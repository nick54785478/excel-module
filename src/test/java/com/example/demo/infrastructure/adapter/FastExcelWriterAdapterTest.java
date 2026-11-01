package com.example.demo.infrastructure.adapter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;
import org.dhatim.fastexcel.reader.Sheet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.demo.application.port.out.ExcelWriterPort;
import com.example.demo.application.shared.command.WriteExcelCommand;
import com.example.demo.application.shared.command.WriteSheetCommand;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * FastExcelWriterAdapter 單元與整合測試 (Unit & Integration Tests)
 * <p>
 * 針對 {@link FastExcelWriterAdapter} 進行串流寫入、二進位生成與多型別儲存格輸出驗證。
 * 所有測試方法嚴格遵循 BDD / Gherkin (Feature, Scenario, Given-When-Then) 語法註解規範。
 */
@DisplayName("FastExcelWriterAdapter 寫入適配器測試")
class FastExcelWriterAdapterTest {

	private ExcelWriterPort excelWriterPort;

	@BeforeEach
	void setUp() {
		this.excelWriterPort = new FastExcelWriterAdapter();
	}

	/**
	 * Feature: 單一工作表二進位寫入
	 * Scenario: 寫入包含標題與多種型別資料的單一工作表，驗證產生的 byte 陣列符合標準 OOXML ZIP 結構
	 * Given: 建立包含欄位名稱與不同型別資料 (String, BigDecimal, Integer, LocalDate, Boolean, null) 的工作表
	 * When: 呼叫 excelWriterPort.writeSingleSheet 寫入為 byte 陣列
	 * Then: 產出之 byte 陣列長度大於 0，且開頭兩位元組為 ZIP 魔術字元 0x50 0x4B ('PK')
	 */
	@Test
	@DisplayName("情境：成功將單一工作表寫入為 byte[] 並驗證 ZIP 魔術字元")
	void shouldWriteSingleSheetToByteArraySuccessfully() {
		// Given
		String sheetName = "訂單報表";
		List<String> headers = List.of("訂單編號", "商品名稱", "單價", "數量", "訂購日期", "是否已出貨", "備註");
		List<List<Object>> rows = List.of(
				List.of("ORD-001", "MacBook Pro", new BigDecimal("72900.00"), 1, LocalDate.of(2026, 10, 31), true, "急單"),
				List.of("ORD-002", "Magic Mouse", new BigDecimal("2490.50"), 2, LocalDate.of(2026, 10, 30), false, "")
		);

		// When
		byte[] bytes = excelWriterPort.writeSingleSheet(sheetName, headers, rows);

		// Then
		assertThat(bytes).isNotNull();
		assertThat(bytes.length).isGreaterThan(0);
		// 驗證 OOXML / ZIP 魔術字元 (0x50, 0x4B 即 ASCII 'P', 'K')
		assertThat(bytes[0]).isEqualTo((byte) 0x50);
		assertThat(bytes[1]).isEqualTo((byte) 0x4B);
	}

	/**
	 * Feature: 多工作表活頁簿串流寫入
	 * Scenario: 寫入包含多個不同工作表名稱之活頁簿至 OutputStream
	 * Given: 建立包含兩個工作表 ("銷售總表", "明細數據") 的 WriteExcelCommand
	 * When: 呼叫 excelWriterPort.write(outputStream, command)
	 * Then: OutputStream 應順利接收完整的 Excel 資料且不拋出任何異常
	 */
	@Test
	@DisplayName("情境：成功將多工作表活頁簿串流寫入指定的 OutputStream")
	void shouldWriteMultiSheetWorkbookSuccessfully() {
		// Given
		WriteSheetCommand sheet1 = WriteSheetCommand.builder()
				.sheetName("銷售總表")
				.headers(List.of("月份", "總營收"))
				.rows(List.of(
						List.of("10月", 1500000),
						List.of("11月", 1850000)
				))
				.build();

		WriteSheetCommand sheet2 = WriteSheetCommand.builder()
				.sheetName("明細數據")
				.headers(List.of("項目代碼", "狀態", "更新時間"))
				.rows(List.of(
						List.of("ITEM-A", "已審核", LocalDateTime.of(2026, 10, 31, 10, 0, 0)),
						List.of("ITEM-B", "待處理", LocalDateTime.of(2026, 10, 31, 11, 30, 0))
				))
				.build();

		WriteExcelCommand command = WriteExcelCommand.builder()
				.applicationName("OrderSystem")
				.applicationVersion("2.0")
				.addSheet(sheet1)
				.addSheet(sheet2)
				.build();

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		// When & Then
		assertDoesNotThrow(() -> excelWriterPort.write(baos, command));
		byte[] result = baos.toByteArray();
		assertThat(result).isNotNull();
		assertThat(result.length).isGreaterThan(0);
	}

	/**
	 * Feature: 往返讀寫一致性驗證 (Round-trip Verification)
	 * Scenario: 使用 FastExcelWriterAdapter 產製 Excel 後，以 FastExcel Reader 讀取並檢驗內容數值
	 * Given: 寫入包含自訂欄寬、凍結標題與特定儲存格數值之 Excel 檔案
	 * When: 使用 fastexcel-reader ReadableWorkbook 讀取產生之二進位串流
	 * Then: 工作表名稱、標題列與各資料列的值應完全符合原始輸入內容
	 */
	@Test
	@DisplayName("情境：產製之 Excel 檔案經 fastexcel-reader 讀取後數值與標題完全一致")
	void shouldVerifyGeneratedExcelUsingFastExcelReader() throws IOException {
		// Given
		String sheetName = "會員名單";
		List<String> headers = List.of("ID", "姓名", "點數", "VIP");
		List<List<Object>> rows = List.of(
				List.of(1001, "Alice", 2500, true),
				List.of(1002, "Bob", 120, false)
		);

		WriteSheetCommand sheetCommand = WriteSheetCommand.builder()
				.sheetName(sheetName)
				.headers(headers)
				.rows(rows)
				.columnWidths(List.of(10.0, 20.0, 15.0, 10.0))
				.freezeHeader(true)
				.headerFillColor("D9E1F2")
				.headerFontColor("000000")
				.build();

		WriteExcelCommand command = WriteExcelCommand.of(sheetCommand);

		// When
		byte[] excelBytes = excelWriterPort.write(command);

		// Then
		try (ReadableWorkbook reader = new ReadableWorkbook(new ByteArrayInputStream(excelBytes))) {
			Sheet readSheet = reader.getFirstSheet();
			assertThat(readSheet.getName()).isEqualTo(sheetName);

			List<Row> excelRows = readSheet.read();
			assertThat(excelRows).hasSize(3); // 1 header + 2 data rows

			// 驗證標題列
			Row headerRow = excelRows.get(0);
			assertThat(headerRow.getCellText(0)).isEqualTo("ID");
			assertThat(headerRow.getCellText(1)).isEqualTo("姓名");
			assertThat(headerRow.getCellText(2)).isEqualTo("點數");
			assertThat(headerRow.getCellText(3)).isEqualTo("VIP");

			// 驗證第 1 筆資料
			Row dataRow1 = excelRows.get(1);
			assertThat(dataRow1.getCellText(0)).isEqualTo("1001");
			assertThat(dataRow1.getCellText(1)).isEqualTo("Alice");
			assertThat(dataRow1.getCellText(2)).isEqualTo("2500");
			assertThat(dataRow1.getCellText(3)).isEqualToIgnoringCase("true");

			// 驗證第 2 筆資料
			Row dataRow2 = excelRows.get(2);
			assertThat(dataRow2.getCellText(0)).isEqualTo("1002");
			assertThat(dataRow2.getCellText(1)).isEqualTo("Bob");
			assertThat(dataRow2.getCellText(2)).isEqualTo("120");
			assertThat(dataRow2.getCellText(3)).isEqualToIgnoringCase("false");
		}
	}

	/**
	 * Feature: 空命令容錯機制
	 * Scenario: 當傳入空 sheets 清單時，應自動建立預設空工作表，不拋出異常
	 * Given: 建立 sheets 為空的 WriteExcelCommand
	 * When: 呼叫 excelWriterPort.write(command)
	 * Then: 應成功生成符合規範之預設活頁簿，不發生異常
	 */
	@Test
	@DisplayName("情境：傳入無工作表之命令時，應容錯生成預設工作表")
	void shouldHandleEmptySheetsGracefully() {
		// Given
		WriteExcelCommand emptyCommand = WriteExcelCommand.builder().build();

		// When
		byte[] bytes = excelWriterPort.write(emptyCommand);

		// Then
		assertThat(bytes).isNotNull();
		assertThat(bytes.length).isGreaterThan(0);
	}
}
