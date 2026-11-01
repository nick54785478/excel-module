package com.example.demo.infrastructure.adapter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.demo.application.port.out.ExcelReaderPort;
import com.example.demo.application.port.out.ExcelWriterPort;
import com.example.demo.application.shared.command.WriteStreamExcelCommand;
import com.example.demo.application.shared.command.WriteStreamSheetCommand;
import com.example.demo.application.shared.query.ReadExcelQuery;
import com.example.demo.application.shared.stream.ExcelRowContext;
import com.example.demo.application.shared.stream.ExcelStreamListener;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FastExcel 串流處理模組整合與單元測試 (Streaming & Chunking Tests)
 * <p>
 * 針對 {@link FastExcelWriterAdapter#writeStream} 與 {@link FastExcelReaderAdapter#readStream}、
 * {@link FastExcelReaderAdapter#readChunked} 進行巨量資料串流與分塊處理驗證。
 * 所有測試方法嚴格遵循 BDD / Gherkin (Feature, Scenario, Given-When-Then) 語法註解規範。
 */
@DisplayName("FastExcel 串流與分塊處理模組測試")
class FastExcelStreamingTest {

	private ExcelWriterPort excelWriterPort;
	private ExcelReaderPort excelReaderPort;

	@BeforeEach
	void setUp() {
		this.excelWriterPort = new FastExcelWriterAdapter();
		this.excelReaderPort = new FastExcelReaderAdapter();
	}

	/**
	 * Feature: 串流寫入與逐列串流讀取整合驗證
	 * Scenario: 透過迭代器串流寫入 100 筆訂單資料並定期 flush，再以 readStream 逐列消費還原數據
	 * Given: 建立具備 100 筆延遲生成資料的 WriteStreamExcelCommand (flushInterval = 20)
	 * When: 透過 excelWriterPort.writeStream 產出 Excel，並以 excelReaderPort.readStream 逐列加總
	 * Then: 總消費筆數應為 100 筆，且總金額計算結果完全精確
	 */
	@Test
	@DisplayName("情境：成功串流寫入大數據並逐列串流消費加總")
	void shouldStreamWriteAndStreamReadSuccessfully() {
		// Given
		int totalRows = 100;
		List<String> headers = List.of("序號", "訂單編號", "金額");

		Iterator<List<Object>> dataIterator = new Iterator<>() {
			private int count = 0;

			@Override
			public boolean hasNext() {
				return count < totalRows;
			}

			@Override
			public List<Object> next() {
				count++;
				return List.of(count, "ORD-" + count, new BigDecimal(count * 100));
			}
		};

		WriteStreamSheetCommand sheetCommand = WriteStreamSheetCommand.builder()
				.sheetName("串流訂單總表")
				.headers(headers)
				.rowIterator(dataIterator)
				.flushInterval(20) // 每 20 列 flush 一次，模擬串流寫入磁碟釋放記憶體
				.build();

		WriteStreamExcelCommand command = WriteStreamExcelCommand.of(sheetCommand);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		excelWriterPort.writeStream(baos, command);
		byte[] excelBytes = baos.toByteArray();
		assertThat(excelBytes).isNotEmpty();

		// When
		AtomicInteger rowCounter = new AtomicInteger(0);
		AtomicInteger totalAmount = new AtomicInteger(0);

		excelReaderPort.readStream(new ByteArrayInputStream(excelBytes), ReadExcelQuery.defaultQuery(),
				(rowValues, rowTexts, context) -> {
					rowCounter.incrementAndGet();
					int amount = Integer.parseInt(rowTexts.get(2));
					totalAmount.addAndGet(amount);
				});

		// Then
		assertThat(rowCounter.get()).isEqualTo(totalRows);
		// 等差級數和：(100 + 10000) * 100 / 2 = 505000
		assertThat(totalAmount.get()).isEqualTo(505000);
	}

	/**
	 * Feature: 串流分塊批次讀取 (Chunked Stream Consumer)
	 * Scenario: 將 25 筆資料以 chunkSize=10 進行分塊讀取，驗證分批批次入庫行為
	 * Given: 包含 25 筆學生資料的 Excel 檔案
	 * When: 呼叫 excelReaderPort.readChunked(bytes, query, 10, chunkConsumer)
	 * Then: 應收到 3 個分塊（大小分別為 10, 10, 5），且每個分塊的 Map 鍵值對符合標題映射
	 */
	@Test
	@DisplayName("情境：成功以指定 chunkSize=10 進行串流分塊批次消費")
	void shouldReadChunkedInBatchesSuccessfully() {
		// Given
		int rowCount = 25;
		List<String> headers = List.of("學號", "姓名", "年級");
		List<List<Object>> rows = new ArrayList<>();
		for (int i = 1; i <= rowCount; i++) {
			rows.add(List.of("STU-" + i, "學生" + i, (i % 3) + 1));
		}

		byte[] excelBytes = excelWriterPort.writeSingleSheet("班級名單", headers, rows);

		// When
		List<Integer> chunkSizes = new ArrayList<>();
		AtomicInteger totalRowsProcessed = new AtomicInteger(0);

		excelReaderPort.readChunked(excelBytes, ReadExcelQuery.defaultQuery(), 10, (chunkRows, context) -> {
			chunkSizes.add(chunkRows.size());
			totalRowsProcessed.addAndGet(chunkRows.size());

			// 驗證第一個分塊的第一筆資料映射
			if (chunkSizes.size() == 1) {
				Map<String, Object> firstStudent = chunkRows.get(0);
				assertThat(firstStudent.get("學號")).isEqualTo("STU-1");
				assertThat(firstStudent.get("姓名")).isEqualTo("學生1");
			}
		});

		// Then
		assertThat(totalRowsProcessed.get()).isEqualTo(25);
		assertThat(chunkSizes).containsExactly(10, 10, 5);
	}

	/**
	 * Feature: 串流提前中斷機制 (Early Stream Stop)
	 * Scenario: 讀取包含 50 筆資料的檔案，當處理到第 5 筆時調用 context.stop() 主動終止
	 * Given: 具有 50 筆資料之 Excel 檔案
	 * When: 在逐列消費回呼中，於第 5 筆資料執行 context.stop()
	 * Then: 串流處理器應立刻停止，累計處理筆數剛好為 5 筆
	 */
	@Test
	@DisplayName("情境：當滿足業務終止條件調用 context.stop() 時立即停止後續串流讀取")
	void shouldStopStreamingEarlyWhenContextStopCalled() {
		// Given
		List<String> headers = List.of("編號", "內容");
		List<List<Object>> rows = new ArrayList<>();
		for (int i = 1; i <= 50; i++) {
			rows.add(List.of(i, "內容_" + i));
		}
		byte[] excelBytes = excelWriterPort.writeSingleSheet("測試中斷", headers, rows);

		// When
		AtomicInteger processedCount = new AtomicInteger(0);

		excelReaderPort.readStream(excelBytes, ReadExcelQuery.defaultQuery(), (rowValues, rowTexts, context) -> {
			processedCount.incrementAndGet();
			if (context.getTotalDataRowsRead() == 5) {
				context.stop(); // 觸發中斷
			}
		});

		// Then
		assertThat(processedCount.get()).isEqualTo(5);
	}

	/**
	 * Feature: 全生命週期監聽器事件串接 (Full Lifecycle Stream Listener)
	 * Scenario: 驗證 onHeader、onRow、onComplete 等事件依序完整觸發
	 * Given: 包含標題與 3 筆資料之 Excel
	 * When: 使用 readWithListener 傳入自訂 ExcelStreamListener
	 * Then: 各階段事件旗標皆應為 true，標題與行數正確捕獲
	 */
	@Test
	@DisplayName("情境：全生命週期監聽器之各階段事件正常觸發")
	void shouldTriggerFullLifecycleListenerEvents() {
		// Given
		List<String> headers = List.of("代碼", "描述");
		List<List<Object>> rows = List.of(
				List.of("A", "Alpha"),
				List.of("B", "Beta"),
				List.of("C", "Gamma")
		);
		byte[] excelBytes = excelWriterPort.writeSingleSheet("狀態對照", headers, rows);

		AtomicBoolean headerTriggered = new AtomicBoolean(false);
		AtomicInteger rowCounter = new AtomicInteger(0);
		AtomicBoolean completeTriggered = new AtomicBoolean(false);

		// When
		excelReaderPort.readWithListener(excelBytes, ReadExcelQuery.defaultQuery(), new ExcelStreamListener() {
			@Override
			public void onHeader(List<String> headerList, ExcelRowContext context) {
				headerTriggered.set(true);
				assertThat(headerList).containsExactly("代碼", "描述");
			}

			@Override
			public void onRow(List<Object> rowValues, List<String> rowTexts, ExcelRowContext context) {
				rowCounter.incrementAndGet();
			}

			@Override
			public void onComplete(ExcelRowContext context) {
				completeTriggered.set(true);
				assertThat(context.getTotalDataRowsRead()).isEqualTo(3);
			}
		});

		// Then
		assertThat(headerTriggered.get()).isTrue();
		assertThat(rowCounter.get()).isEqualTo(3);
		assertThat(completeTriggered.get()).isTrue();
	}
}
