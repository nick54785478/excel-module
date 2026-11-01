package com.example.demo.infrastructure.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.demo.application.port.out.ExcelReaderPort;
import com.example.demo.application.port.out.ExcelWriterPort;
import com.example.demo.application.shared.command.WriteExcelCommand;
import com.example.demo.application.shared.command.WriteSheetCommand;
import com.example.demo.application.shared.dto.ExcelGottenResult;
import com.example.demo.application.shared.dto.ExcelSheetGottenResult;
import com.example.demo.application.shared.query.ReadExcelQuery;
import com.example.demo.application.shared.stream.ExcelRowContext;
import com.example.demo.application.shared.stream.ExcelStreamListener;

/**
 * PoiSaxReaderAdapter 單元與整合測試 (Unit & Integration Tests)
 * <p>
 * 針對 {@link PoiSaxReaderAdapter} 進行檔案讀取、條件過濾、串流逐行回呼與分塊讀取驗證。
 * 所有測試方法嚴格遵循 BDD / Gherkin (Feature, Scenario, Given-When-Then) 語法註解規範。
 */
@DisplayName("PoiSaxReaderAdapter 讀取適配器測試")
class PoiSaxReaderAdapterTest {

	private ExcelWriterPort excelWriterPort;
	private ExcelReaderPort excelReaderPort;

	@BeforeEach
	void setUp() {
		this.excelWriterPort = new PoiSxssfWriterAdapter();
		this.excelReaderPort = new PoiSaxReaderAdapter();
	}

	/**
	 * Feature: 標準 Excel 檔案讀取
	 * Scenario: 讀取包含標題列與多筆資料之 Excel 二進位內容，驗證結果結構與欄位數值
	 * Given: 透過 PoiSxssfWriterAdapter 產生標準 Excel 二進位位元組陣列
	 * When: 呼叫 excelReaderPort.read 讀取整份活頁簿
	 * Then: 成功解析出工作表名稱、標題列清單與資料列清單
	 */
	@Test
	@DisplayName("情境：成功讀取標準 Excel 活頁簿並驗證標題與資料列")
	void shouldReadExcelByteArraySuccessfully() {
		// Given
		byte[] excelBytes = createSampleExcelBytes();

		// When
		ExcelGottenResult result = excelReaderPort.read(excelBytes);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getSheetCount()).isEqualTo(1);

		ExcelSheetGottenResult sheet = result.getSheet(0).orElseThrow();
		assertThat(sheet.getSheetName()).isEqualTo("物料清單");
		assertThat(sheet.getHeaders()).containsExactly("料號", "品名", "單價", "數量");
		assertThat(sheet.getRows()).hasSize(3);

		List<Object> firstRow = sheet.getRows().get(0);
		assertThat(firstRow.get(0)).isEqualTo("MAT-001");
		assertThat(firstRow.get(1)).isEqualTo("伺服器主板");
	}

	/**
	 * Feature: 串流逐列監聽讀取 (readWithListener)
	 * Scenario: 透過監聽器逐行接收標題與資料列
	 * Given: 建立包含 3 筆資料之 Excel 檔案
	 * When: 呼叫 excelReaderPort.readWithListener
	 * Then: 依序回呼 onHeader、onRow 與 onComplete
	 */
	@Test
	@DisplayName("情境：透過 ExcelStreamListener 進行串流生命週期監聽讀取")
	void shouldReadStreamWithListenerSuccessfully() {
		// Given
		byte[] excelBytes = createSampleExcelBytes();
		List<String> receivedHeaders = new ArrayList<>();
		List<List<Object>> receivedRows = new ArrayList<>();
		boolean[] completed = {false};

		// When
		excelReaderPort.readWithListener(excelBytes, ReadExcelQuery.defaultQuery(), new ExcelStreamListener() {
			@Override
			public void onHeader(List<String> headers, ExcelRowContext context) {
				receivedHeaders.addAll(headers);
			}

			@Override
			public void onRow(List<Object> rowValues, List<String> rowTexts, ExcelRowContext context) {
				receivedRows.add(rowValues);
			}

			@Override
			public void onComplete(ExcelRowContext context) {
				completed[0] = true;
			}
		});

		// Then
		assertThat(receivedHeaders).containsExactly("料號", "品名", "單價", "數量");
		assertThat(receivedRows).hasSize(3);
		assertThat(completed[0]).isTrue();
	}

	/**
	 * Feature: 分塊批次讀取 (readChunked)
	 * Scenario: 設定 chunkSize=2，驗證分批分塊回呼
	 * Given: 建立包含 3 筆資料之 Excel 檔案
	 * When: 呼叫 excelReaderPort.readChunked，chunkSize=2
	 * Then: 應收到 2 次分塊回呼 (分別為 2 筆與 1 筆)
	 */
	@Test
	@DisplayName("情境：透過 readChunked 分塊批次讀取 Excel 資料")
	void shouldReadChunkedSuccessfully() {
		// Given
		byte[] excelBytes = createSampleExcelBytes();
		List<List<Map<String, Object>>> chunks = new ArrayList<>();

		// When
		excelReaderPort.readChunked(excelBytes, ReadExcelQuery.defaultQuery(), 2, (chunk, context) -> {
			chunks.add(chunk);
		});

		// Then
		assertThat(chunks).hasSize(2);
		assertThat(chunks.get(0)).hasSize(2);
		assertThat(chunks.get(1)).hasSize(1);
		assertThat(chunks.get(0).get(0)).containsEntry("料號", "MAT-001");
	}

	private byte[] createSampleExcelBytes() {
		WriteSheetCommand sheet = WriteSheetCommand.builder()
				.sheetName("物料清單")
				.headers(List.of("料號", "品名", "單價", "數量"))
				.rows(List.of(
						List.of("MAT-001", "伺服器主板", 12500.0, 5),
						List.of("MAT-002", "電源供應器", 3200.0, 10),
						List.of("MAT-003", "機箱外殼", 1800.0, 8)
				))
				.build();

		return excelWriterPort.write(WriteExcelCommand.of(sheet));
	}
}

