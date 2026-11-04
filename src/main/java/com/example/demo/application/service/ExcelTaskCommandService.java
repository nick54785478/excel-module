package com.example.demo.application.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.example.demo.application.assembler.BomDtoAssembler;
import com.example.demo.application.port.in.SubmitAsyncExcelExportUseCase;
import com.example.demo.application.port.in.SubmitAsyncExcelImportUseCase;
import com.example.demo.application.port.out.ExcelReaderPort;
import com.example.demo.application.port.out.ExcelTaskRepositoryPort;
import com.example.demo.application.port.out.ExcelWriterPort;
import com.example.demo.application.shared.command.CreateExcelTaskCommand;
import com.example.demo.application.shared.command.ExportBomCommand;
import com.example.demo.application.shared.command.ExportExcelCommand;
import com.example.demo.application.shared.command.WriteExcelCommand;
import com.example.demo.application.shared.dto.BomExcelGottenResult;
import com.example.demo.application.shared.dto.ExcelTaskGottenResult;
import com.example.demo.application.shared.vo.ExcelTaskType;
import com.example.demo.application.shared.exception.ExcelReadException;
import com.example.demo.application.shared.query.ReadExcelQuery;

/**
 * Excel 長任務命令應用服務 (Excel Task Command Service / Inbound Adapter)
 * <p>
 * 遵循 Clean Architecture 六角形架構與 CQRS 規範，作為命令端 (Command Side) 的 Inbound Adapter。
 * 命名規範符合 AGENTS.md 3-9 規範 (以 {@code CommandService} 結尾)。
 * 實作應用層 {@link SubmitAsyncExcelExportUseCase} 與
 * {@link SubmitAsyncExcelImportUseCase} 業務合約。
 * <p>
 * 負責調度
 * {@link ExcelTaskRepositoryPort}、{@link ExcelWriterPort}、{@link ExcelReaderPort}
 * 與 {@link BomCommandService}，
 * 並將實際繁重的 I/O 與解析任務委派予專屬執行緒池 (Thread Pool) 執行，以達成防禦連線逾時 (HTTP 202 Accepted)
 * 與進度追蹤。
 * <p>
 * 涵蓋技術模組：
 * <ul>
 * <li><b>【非同步長任務調度模組 (Async Long-Running Task Dispatcher)】</b>：
 * 在接收前端請求後立即生成全域唯一 {@code taskId} 並於資料庫持久化 {@code PENDING} 狀態任務記錄，
 * 隨即將解析/匯出邏輯拋入背景工作緒，即刻回傳任務初值以防禦 Gateway 或 HTTP 連線逾時。</li>
 * <li><b>【大數據串流進度回呼模組 (Streaming Progress Callback)】</b>：
 * 深度結合 {@link ExcelReaderPort#readChunked} 分塊串流機制，每當累積指定批次列數 (如 500 筆)
 * 即觸發回呼更新 {@link ExcelTaskRepositoryPort#updateProgress}，達成 O(1)
 * 記憶體常數開銷與即時百分比狀態同步。</li>
 * </ul>
 */
@Service
public class ExcelTaskCommandService implements SubmitAsyncExcelExportUseCase, SubmitAsyncExcelImportUseCase {

	private final ExcelTaskRepositoryPort excelTaskRepositoryPort;
	private final ExcelWriterPort excelWriterPort;
	private final ExcelReaderPort excelReaderPort;
	private final BomCommandService bomCommandService;
	private final Executor taskExecutor;

	/**
	 * 記憶體中暫存已完成任務產出檔案之二進位內容 (供 /download 端點取用)
	 */
	private final ConcurrentMap<String, byte[]> completedTaskFiles = new ConcurrentHashMap<>();

	public ExcelTaskCommandService(
			ExcelTaskRepositoryPort excelTaskRepositoryPort,
			ExcelWriterPort excelWriterPort,
			ExcelReaderPort excelReaderPort,
			BomCommandService bomCommandService,
			@Qualifier("excelTaskExecutor") Executor taskExecutor) {
		this.excelTaskRepositoryPort = Objects.requireNonNull(excelTaskRepositoryPort, "excelTaskRepositoryPort 不得為空");
		this.excelWriterPort = Objects.requireNonNull(excelWriterPort, "excelWriterPort 不得為空");
		this.excelReaderPort = Objects.requireNonNull(excelReaderPort, "excelReaderPort 不得為空");
		this.bomCommandService = Objects.requireNonNull(bomCommandService, "bomCommandService 不得為空");
		this.taskExecutor = Objects.requireNonNull(taskExecutor, "taskExecutor 不得為空");
	}

	// =========================================================================
	// 1. 【非同步長任務調度模組 (Async Long-Running Task Dispatcher)】
	// =========================================================================

	/**
	 * 【非同步長任務調度模組 (Async Long-Running Task Dispatcher)】
	 * <p>
	 * 提交標準通用多工作表結構之非同步 Excel 匯出長任務。
	 * 立即持久化任務並指派背景工作緒進行活頁簿產製與進度更新。
	 *
	 * @param command 標準匯出命令載體
	 * @return 初始長任務結果 DTO (狀態為 PENDING)
	 */
	@Override
	public ExcelTaskGottenResult submitAsyncExport(ExportExcelCommand command) {
		Objects.requireNonNull(command, "ExportExcelCommand 不得為空");

		long totalRows = command.sheets() != null
				? command.sheets().stream().mapToLong(s -> s.getRows() != null ? s.getRows().size() : 0).sum()
				: 0;

		String taskId = UUID.randomUUID().toString();
		String fileName = command.fileName() != null && !command.fileName().isBlank()
				? command.fileName()
				: "export.xlsx";

		CreateExcelTaskCommand createCommand = new CreateExcelTaskCommand(taskId, ExcelTaskType.EXPORT, fileName,
				totalRows);
		ExcelTaskGottenResult initialResult = excelTaskRepositoryPort.createTask(createCommand);

		// 指派背景執行緒執行匯出作業
		taskExecutor.execute(() -> executeExportTask(taskId, command, totalRows));

		return initialResult;
	}

	/**
	 * 【非同步長任務調度模組 (Async Long-Running Task Dispatcher)】
	 * <p>
	 * 提交 BOM 物料清單結構之非同步 Excel 匯出長任務。
	 * 委託 {@link BomCommandService} 於背景展開多階樹狀或單階資料，並即時回報進度與完成狀態。
	 *
	 * @param command BOM 匯出命令載體
	 * @return 初始長任務結果 DTO (狀態為 PENDING)
	 */
	@Override
	public ExcelTaskGottenResult submitAsyncBomExport(ExportBomCommand command) {
		Objects.requireNonNull(command, "ExportBomCommand 不得為空");

		String taskId = UUID.randomUUID().toString();
		String fileName = BomDtoAssembler.resolveFileName(command);

		CreateExcelTaskCommand createCommand = new CreateExcelTaskCommand(taskId, ExcelTaskType.EXPORT, fileName, 0L);
		ExcelTaskGottenResult initialResult = excelTaskRepositoryPort.createTask(createCommand);

		// 指派背景執行緒執行 BOM 匯出作業
		taskExecutor.execute(() -> executeBomExportTask(taskId, command));

		return initialResult;
	}

	// =========================================================================
	// 2. 【大數據串流進度回呼模組 (Streaming Progress Callback)】
	// =========================================================================

	/**
	 * 【大數據串流進度回呼模組 (Streaming Progress Callback)】
	 * <p>
	 * 提交 Excel 檔案二進位串流之非同步解析長任務。
	 * 先行讀取串流位元組避免連線關閉導致 Stream Closed，接著於背景執行緒啟動分塊串流解析 (Chunked Stream)，
	 * 每累積 500 列即時回呼持久化進度，避免龐大檔案造成記憶體溢位 (OOM) 與連線中斷。
	 *
	 * @param inputStream 檔案二進位輸入串流
	 * @param fileName    原始上傳檔案名稱
	 * @param query       讀取查詢條件 (若為 null 則套用預設條件)
	 * @return 初始長任務結果 DTO (狀態為 PENDING)
	 */
	@Override
	public ExcelTaskGottenResult submitAsyncImport(InputStream inputStream, String fileName, ReadExcelQuery query) {
		Objects.requireNonNull(inputStream, "inputStream 不得為空");

		byte[] excelBytes;
		try {
			excelBytes = inputStream.readAllBytes();
		} catch (IOException e) {
			throw new ExcelReadException("讀取上傳檔案輸入流位元組失敗: " + e.getMessage(), e);
		}

		String taskId = UUID.randomUUID().toString();
		String effectiveFileName = fileName != null && !fileName.isBlank() ? fileName : "uploaded.xlsx";
		ReadExcelQuery effectiveQuery = query != null ? query : ReadExcelQuery.defaultQuery();

		CreateExcelTaskCommand createCommand = new CreateExcelTaskCommand(taskId, ExcelTaskType.IMPORT,
				effectiveFileName, 0L);
		ExcelTaskGottenResult initialResult = excelTaskRepositoryPort.createTask(createCommand);

		// 指派背景執行緒啟動分塊串流讀取
		taskExecutor.execute(() -> executeImportTask(taskId, excelBytes, effectiveQuery));

		return initialResult;
	}

	/**
	 * 取得指定已完成匯出長任務產出之檔案二進位內容
	 *
	 * @param taskId 任務識別碼
	 * @return 檔案二進位位元組陣列 (若未完成或不存在則回傳 null)
	 */
	public byte[] getExportedFile(String taskId) {
		Objects.requireNonNull(taskId, "taskId 不得為空");
		return completedTaskFiles.get(taskId);
	}

	// =========================================================================
	// 私有背景執行工作方法
	// =========================================================================

	private void executeExportTask(String taskId, ExportExcelCommand command, long totalRows) {
		try {
			excelTaskRepositoryPort.markProcessing(taskId);

			long processed = 0;
			if (command.sheets() != null) {
				for (var sheet : command.sheets()) {
					int sheetRows = sheet.getRows() != null ? sheet.getRows().size() : 0;
					processed += sheetRows;
					excelTaskRepositoryPort.updateProgress(taskId, processed, totalRows);
				}
			}

			WriteExcelCommand writeCommand = WriteExcelCommand.of(command.sheets());
			byte[] fileBytes = excelWriterPort.write(writeCommand);

			completedTaskFiles.put(taskId, fileBytes);
			String fileUrl = "/api/v1/excel/tasks/" + taskId + "/download";
			excelTaskRepositoryPort.markCompleted(taskId, fileUrl, totalRows > 0 ? totalRows : processed);
		} catch (Exception e) {
			excelTaskRepositoryPort.markFailed(taskId, e.getMessage());
		}
	}

	private void executeBomExportTask(String taskId, ExportBomCommand command) {
		try {
			excelTaskRepositoryPort.markProcessing(taskId);

			BomExcelGottenResult bomResult = bomCommandService.exportBom(command);
			long rowCount = bomResult.totalRows();

			excelTaskRepositoryPort.updateProgress(taskId, rowCount, rowCount);
			completedTaskFiles.put(taskId, bomResult.fileBytes());

			String fileUrl = "/api/v1/excel/tasks/" + taskId + "/download";
			excelTaskRepositoryPort.markCompleted(taskId, fileUrl, rowCount);
		} catch (Exception e) {
			excelTaskRepositoryPort.markFailed(taskId, e.getMessage());
		}
	}

	private void executeImportTask(String taskId, byte[] excelBytes, ReadExcelQuery query) {
		try {
			excelTaskRepositoryPort.markProcessing(taskId);

			AtomicLong processedCount = new AtomicLong(0);
			int chunkSize = 500;

			excelReaderPort.readChunked(excelBytes, query, chunkSize, (chunk, context) -> {
				long current = processedCount.addAndGet(chunk.size());
				excelTaskRepositoryPort.updateProgress(taskId, current, current);
			});

			long finalTotal = processedCount.get();
			excelTaskRepositoryPort.markCompleted(taskId, null, finalTotal);
		} catch (Exception e) {
			excelTaskRepositoryPort.markFailed(taskId, e.getMessage());
		}
	}
}
