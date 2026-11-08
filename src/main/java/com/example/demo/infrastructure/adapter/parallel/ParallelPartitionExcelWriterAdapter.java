package com.example.demo.infrastructure.adapter.parallel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.example.demo.application.port.out.ExcelPartitionWriterPort;
import com.example.demo.application.shared.command.ExportPartitionZipCommand;
import com.example.demo.application.shared.command.ExportPipelineExcelCommand;
import com.example.demo.application.shared.command.WriteStreamExcelCommand;
import com.example.demo.application.shared.command.WriteStreamSheetCommand;
import com.example.demo.application.shared.exception.ExcelWriteException;
import com.example.demo.application.shared.pipeline.ExcelChunkProvider;
import com.example.demo.infrastructure.engine.ExcelStreamWriterEngine;

/**
 * 高並發平行管線與分片 ZIP 輸出適配器 (Parallel Partition Excel Writer Outbound Adapter)
 * <p>
 * 遵循 Clean Architecture 六角形架構規範，實作 {@link ExcelPartitionWriterPort}。<br>
 * 命名規範：以 {@code Adapter} 結尾 (3-9 規範)。<br>
 * 協調多執行緒生產者管線、有界背壓佇列與 ZIP 封裝，底層活頁簿串流序列化則直接協作基礎設施層共用之 {@link ExcelStreamWriterEngine}，
 * 嚴禁跨層依賴應用層輸出埠 (Outbound Port)。<br>
 * 實踐高並發企業級加速架構：
 * <ol>
 *     <li><b>模式 A (微觀管線)</b>：平行查詢資料庫 + 有界阻斷佇列 (背壓防護) + 單寫入緒循序寫入 SXSSF/FastExcel。</li>
 *     <li><b>模式 B (宏觀分片)</b>：多活頁簿在多核心上完全並行產製，最後動態串流壓縮寫入 ZIP 輸出流。</li>
 *     <li><b>模式 A + B 終極組合拳</b>：宏觀多檔案並行，各分片內部多緒查詢，徹底釋放多核吞吐量並確保 100% 執行緒安全。</li>
 * </ol>
 */
@Component
public class ParallelPartitionExcelWriterAdapter implements ExcelPartitionWriterPort {

	private final ExcelStreamWriterEngine streamWriterEngine;
	private final ExecutorService executorService;

	public ParallelPartitionExcelWriterAdapter(
			ExcelStreamWriterEngine streamWriterEngine,
			@Qualifier("excelParallelExecutorService") ExecutorService executorService
	) {
		this.streamWriterEngine = Objects.requireNonNull(streamWriterEngine, "ExcelStreamWriterEngine must not be null");
		this.executorService = Objects.requireNonNull(executorService, "ExecutorService must not be null");
	}

	/**
	 * 模式 A：執行微觀生產者-消費者管線串流寫出
	 */
	@Override
	public void writePipelineStream(OutputStream outputStream, ExportPipelineExcelCommand command) {
		Objects.requireNonNull(outputStream, "outputStream must not be null");
		Objects.requireNonNull(command, "ExportPipelineExcelCommand must not be null");

		int totalRows = command.totalRows();
		int chunkSize = command.chunkSize();
		int queueCapacity = command.queueCapacity();
		int totalChunks = totalRows == 0 ? 0 : (int) Math.ceil((double) totalRows / chunkSize);

		BlockingQueue<ChunkEnvelope> queue = new ArrayBlockingQueue<>(queueCapacity);
		AtomicReference<Throwable> firstError = new AtomicReference<>();
		CountDownLatch producerLatch = new CountDownLatch(totalChunks);

		// 1. 提交平行生產者任務 (平行拉取 DB Chunk 並推入有界佇列，具備背壓控制)
		for (int i = 0; i < totalChunks; i++) {
			final int chunkIndex = i;
			executorService.submit(() -> {
				try {
					if (firstError.get() == null) {
						int offset = chunkIndex * chunkSize;
						int limit = Math.min(chunkSize, totalRows - offset);
						List<List<Object>> rows = command.chunkProvider().fetchChunk(offset, limit);
						queue.put(ChunkEnvelope.data(rows != null ? rows : Collections.emptyList()));
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					firstError.compareAndSet(null, e);
				} catch (Throwable t) {
					firstError.compareAndSet(null, t);
					try {
						queue.put(ChunkEnvelope.error(t)); // 提早喚醒消費者中斷
					} catch (InterruptedException ignored) {
					}
				} finally {
					producerLatch.countDown();
				}
			});
		}

		// 2. 獨立監控生產者完成狀態，全部完成後推入 Poison Pill
		CompletableFuture.runAsync(() -> {
			try {
				producerLatch.await();
				queue.put(ChunkEnvelope.poisonPill());
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}, executorService);

		// 3. 消費端包裝為 Iterable 供串流寫入適配器循序消費 (完全單執行緒安全)
		Iterable<List<Object>> streamIterable = () -> new Iterator<>() {
			private List<List<Object>> currentChunk = Collections.emptyList();
			private int indexInChunk = 0;
			private boolean finished = false;

			@Override
			public boolean hasNext() {
				if (indexInChunk < currentChunk.size()) {
					return true;
				}
				if (finished) {
					return false;
				}

				while (true) {
					try {
						ChunkEnvelope envelope = queue.take(); // 阻塞等待下一個 Chunk
						if (envelope.isError()) {
							finished = true;
							throw new ExcelWriteException("Pipeline producer failed: " + envelope.error().getMessage(), envelope.error());
						}
						if (envelope.isPoisonPill()) {
							finished = true;
							return false;
						}
						currentChunk = envelope.data();
						indexInChunk = 0;
						if (!currentChunk.isEmpty()) {
							return true;
						}
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						finished = true;
						throw new ExcelWriteException("Pipeline consumer was interrupted while waiting for data chunk", e);
					}
				}
			}

			@Override
			public List<Object> next() {
				if (!hasNext()) {
					throw new java.util.NoSuchElementException();
				}
				return currentChunk.get(indexInChunk++);
			}
		};

		// 4. 組裝 WriteStreamSheetCommand 並委託底層 Excel 串流引擎循序寫入與定期 flush
		WriteStreamSheetCommand sheetCommand = WriteStreamSheetCommand.builder()
				.sheetName(command.sheetName())
				.headers(command.headers())
				.columnWidths(command.columnWidths())
				.freezeHeader(command.freezeHeader())
				.flushInterval(Math.min(chunkSize, 100))
				.rowIterable(streamIterable)
				.build();

		WriteStreamExcelCommand writeCommand = WriteStreamExcelCommand.builder()
				.applicationName(command.applicationName())
				.sheets(List.of(sheetCommand))
				.build();

		streamWriterEngine.writeStream(outputStream, writeCommand);

		// 檢查是否有生產者異常殘留
		if (firstError.get() != null) {
			Throwable err = firstError.get();
			throw new ExcelWriteException("Pipeline execution encountered an error: " + err.getMessage(), err);
		}
	}

	/**
	 * 模式 A：執行微觀管線寫出並產出 byte 陣列
	 */
	@Override
	public byte[] writePipeline(ExportPipelineExcelCommand command) {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			writePipelineStream(baos, command);
			return baos.toByteArray();
		} catch (IOException e) {
			throw new ExcelWriteException("Failed to write pipeline Excel to byte array: " + e.getMessage(), e);
		}
	}

	/**
	 * 模式 B / 模式 A+B：多活頁簿分片並行產製並動態串流壓縮寫入 ZIP
	 */
	@Override
	public void writePartitionZipStream(OutputStream outputStream, ExportPartitionZipCommand command) {
		Objects.requireNonNull(outputStream, "outputStream must not be null");
		Objects.requireNonNull(command, "ExportPartitionZipCommand must not be null");

		int totalRows = command.totalRows();
		int maxRowsPerFile = command.maxRowsPerFile();
		int partitionCount = command.calculatePartitionCount();

		// 平行任務收集器
		List<CompletableFuture<PartitionResult>> futures = new ArrayList<>(partitionCount);

		for (int p = 0; p < partitionCount; p++) {
			final int partIndex = p;
			final int partStartRow = partIndex * maxRowsPerFile;
			final int partRows = Math.min(maxRowsPerFile, Math.max(0, totalRows - partStartRow));
			final String partFileName = String.format("%s_%03d.xlsx", command.baseFileName(), partIndex + 1);

			// 每個分片由獨立執行緒並行產製各自的 Excel 檔案 (實體隔離，100% 執行緒安全)
			CompletableFuture<PartitionResult> future = CompletableFuture.supplyAsync(() -> {
				byte[] excelBytes;
				if (command.usePipelinePerPartition() && partRows > 0) {
					// 模式 A + B 組合拳：分片內部亦使用多緒查詢管線
					ExcelChunkProvider partProvider = (offset, limit) ->
							command.chunkProvider().fetchChunk(partStartRow + offset, limit);

					ExportPipelineExcelCommand pipelineCommand = ExportPipelineExcelCommand.builder()
							.applicationName("ExcelPartitionModule")
							.sheetName(command.sheetName())
							.headers(command.headers())
							.totalRows(partRows)
							.chunkSize(command.chunkSize())
							.parallelProducers(command.parallelProducersPerPartition())
							.columnWidths(command.columnWidths())
							.freezeHeader(command.freezeHeader())
							.chunkProvider(partProvider)
							.build();

					excelBytes = writePipeline(pipelineCommand);
				} else {
					// 標準串流寫入
					excelBytes = generateStandardPartitionBytes(partStartRow, partRows, command);
				}
				return new PartitionResult(partIndex, partFileName, excelBytes);
			}, executorService);

			futures.add(future);
		}

		// 依序寫入 ZIP 輸出串流 (ZipOutputStream 循序打包)
		try {
			ZipOutputStream zos = new ZipOutputStream(outputStream, StandardCharsets.UTF_8);
			for (CompletableFuture<PartitionResult> future : futures) {
				PartitionResult result = future.join();
				ZipEntry entry = new ZipEntry(result.fileName());
				zos.putNextEntry(entry);
				zos.write(result.excelBytes());
				zos.closeEntry();
			}
			zos.finish();
			zos.flush();
		} catch (IOException e) {
			throw new ExcelWriteException("Failed to stream write partition ZIP: " + e.getMessage(), e);
		}
	}

	/**
	 * 模式 B / 模式 A+B：多活頁簿分片並行產製並回傳 ZIP byte 陣列
	 */
	@Override
	public byte[] writePartitionZip(ExportPartitionZipCommand command) {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			writePartitionZipStream(baos, command);
			return baos.toByteArray();
		} catch (IOException e) {
			throw new ExcelWriteException("Failed to write partition ZIP to byte array: " + e.getMessage(), e);
		}
	}

	/**
	 * 產製單一分片之標準串流位元組
	 */
	private byte[] generateStandardPartitionBytes(int startRow, int rows, ExportPartitionZipCommand command) {
		Iterable<List<Object>> iterable = () -> new Iterator<>() {
			private int currentRow = 0;

			@Override
			public boolean hasNext() {
				return currentRow < rows;
			}

			@Override
			public List<Object> next() {
				if (!hasNext()) {
					throw new java.util.NoSuchElementException();
				}
				List<List<Object>> chunk = command.chunkProvider().fetchChunk(startRow + currentRow, 1);
				currentRow++;
				return (chunk != null && !chunk.isEmpty()) ? chunk.get(0) : Collections.emptyList();
			}
		};

		WriteStreamSheetCommand sheet = WriteStreamSheetCommand.builder()
				.sheetName(command.sheetName())
				.headers(command.headers())
				.columnWidths(command.columnWidths())
				.freezeHeader(command.freezeHeader())
				.rowIterable(iterable)
				.build();

		return streamWriterEngine.writeStream(WriteStreamExcelCommand.builder().sheets(List.of(sheet)).build());
	}

	/**
	 * 分片產製內部結果封裝
	 */
	private record PartitionResult(int partIndex, String fileName, byte[] excelBytes) {
	}

	/**
	 * 佇列封裝信封 (包含資料、毒丸與錯誤訊息)
	 */
	private record ChunkEnvelope(List<List<Object>> data, boolean isPoisonPill, Throwable error) {
		public static ChunkEnvelope data(List<List<Object>> data) {
			return new ChunkEnvelope(data, false, null);
		}

		public static ChunkEnvelope poisonPill() {
			return new ChunkEnvelope(Collections.emptyList(), true, null);
		}

		public static ChunkEnvelope error(Throwable error) {
			return new ChunkEnvelope(Collections.emptyList(), false, error);
		}

		public boolean isError() {
			return error != null;
		}
	}
}

