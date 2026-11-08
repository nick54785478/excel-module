package com.example.demo.infrastructure.adapter.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.demo.application.port.out.ExcelWriterPort;
import com.example.demo.application.shared.command.WriteExcelCommand;
import com.example.demo.application.shared.command.WriteSheetCommand;
import com.example.demo.application.shared.command.WriteStreamExcelCommand;
import com.example.demo.application.shared.command.WriteStreamSheetCommand;
import com.example.demo.application.shared.model.CellRange;
import com.example.demo.application.shared.model.SheetHeaderFooterConfig;
import com.example.demo.application.shared.model.SheetProtectionConfig;

/**
 * PoiSxssfWriterAdapter 單元與整合測試 (Unit & Integration Tests)
 * <p>
 * 針對 {@link PoiSxssfWriterAdapter} 進行 SXSSF 串流寫入、二進位生成、多工作表與格式驗證。
 * 所有測試方法嚴格遵循 BDD / Gherkin (Feature, Scenario, Given-When-Then) 語法註解規範。
 */
@DisplayName("PoiSxssfWriterAdapter 寫入適配器測試")
class PoiSxssfWriterAdapterTest {

	private ExcelWriterPort excelWriterPort;

	@BeforeEach
	void setUp() {
		this.excelWriterPort = new PoiSxssfWriterAdapter();
	}

	/**
	 * Feature: 單一工作表二進位寫入
	 * Scenario: 使用 Apache POI SXSSF 寫入包含標題與不同型別資料之單一工作表
	 * Given: 建立包含欄位名稱與不同型別資料 (String, BigDecimal, Integer, LocalDate, Boolean)
	 * When: 呼叫 excelWriterPort.writeSingleSheet 寫入為 byte 陣列
	 * Then: 產出之 byte 陣列長度大於 0，且符合標準 ZIP/OOXML 魔術字元 0x50 0x4B
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
	void shouldWriteMultiSheetWorkbookSuccessfully() throws Exception {
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

		try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(result))) {
			assertThat(wb.getNumberOfSheets()).isEqualTo(2);
			assertThat(wb.getSheetAt(0).getSheetName()).isEqualTo("銷售總表");
			assertThat(wb.getSheetAt(1).getSheetName()).isEqualTo("明細數據");
		}
	}

	/**
	 * Feature: 大數據串流 writeStream 寫入
	 * Scenario: 傳入串流命令 WriteStreamExcelCommand，以分批沖刷機制產生 Excel 檔案
	 * Given: 建立包含 50 筆資料且設定 flushInterval=10 的 WriteStreamSheetCommand
	 * When: 呼叫 excelWriterPort.writeStream
	 * Then: 成功產生合法 Excel 二進位陣列，驗證工作表與行數
	 */
	@Test
	@DisplayName("情境：成功透過 writeStream 執行大數據串流輸出")
	void shouldWriteStreamSuccessfully() throws Exception {
		// Given
		List<List<Object>> streamRows = List.of(
				List.of("CODE-01", "物料 A", 10.5),
				List.of("CODE-02", "物料 B", 20.0),
				List.of("CODE-03", "物料 C", 30.25)
		);

		WriteStreamSheetCommand sheet = WriteStreamSheetCommand.builder()
				.sheetName("串流測試表")
				.headers(List.of("代碼", "名稱", "單價"))
				.rowIterable(streamRows)
				.flushInterval(2)
				.freezeHeader(true)
				.headerFillColor("1F4E78")
				.headerFontColor("FFFFFF")
				.build();

		WriteStreamExcelCommand command = WriteStreamExcelCommand.builder()
				.applicationName("StreamApp")
				.applicationVersion("1.0")
				.addSheet(sheet)
				.build();

		// When
		byte[] bytes = excelWriterPort.writeStream(command);

		// Then
		assertThat(bytes).isNotEmpty();
		try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
			assertThat(wb.getNumberOfSheets()).isEqualTo(1);
			assertThat(wb.getSheetAt(0).getSheetName()).isEqualTo("串流測試表");
			assertThat(wb.getSheetAt(0).getLastRowNum()).isEqualTo(3);
		}
	}

	/**
	 * Feature: 維度五企業級功能：合併儲存格、動態公式、工作表防護與頁首頁尾
	 * Scenario: 匯出包含跨欄合併合計、自訂公式與密碼保護之正式會計報表
	 * Given: 建立具備 CellRange、cellFormulas、SheetProtectionConfig 與 SheetHeaderFooterConfig 之命令
	 * When: 呼叫 excelWriterPort.write 產製 Excel
	 * Then: 驗證產出活頁簿包含合併區間、儲存格類型為 FORMULA 且公式文字正確、工作表處於保護狀態、頁首頁尾包含機密設定
	 */
	@Test
	@DisplayName("情境：成功產製包含合併儲存格、動態公式、工作表密碼保護與頁首頁尾之試算表")
	void shouldWriteSheetWithAdvancedDecorationsSuccessfully() throws Exception {
		// Given
		WriteSheetCommand sheet = WriteSheetCommand.builder()
				.sheetName("損益分析表")
				.headers(List.of("物料代碼", "單價", "數量", "小計"))
				.rows(List.of(
						List.of("M-01", 100, 2, "=B2*C2"),
						List.of("M-02", 50, 4, "=B3*C3"),
						java.util.Arrays.asList("合計", null, null, null)
				))
				.addMergedRegion(CellRange.ofRow(3, 0, 2)) // 合併第 4 列的 col 0 到 2 ("合計")
				.addFormula("D4", "SUM(D2:D3)")            // 合計欄位公式
				.protectionConfig(SheetProtectionConfig.ofPassword("AuditPass2026"))
				.headerFooterConfig(SheetHeaderFooterConfig.enterpriseStandard("物料採購損益分析月報", "財務機密"))
				.build();

		WriteExcelCommand command = WriteExcelCommand.of(sheet);

		// When
		byte[] bytes = excelWriterPort.write(command);

		// Then
		assertThat(bytes).isNotEmpty();
		try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
			Sheet resultSheet = wb.getSheet("損益分析表");
			assertThat(resultSheet).isNotNull();

			// 驗證合併區間
			assertThat(resultSheet.getNumMergedRegions()).isGreaterThanOrEqualTo(1);
			org.apache.poi.ss.util.CellRangeAddress merged = resultSheet.getMergedRegion(0);
			assertThat(merged.getFirstRow()).isEqualTo(3);
			assertThat(merged.getLastRow()).isEqualTo(3);
			assertThat(merged.getFirstColumn()).isEqualTo(0);
			assertThat(merged.getLastColumn()).isEqualTo(2);

			// 驗證儲存格公式
			Row dataRow1 = resultSheet.getRow(1);
			Cell formulaCell1 = dataRow1.getCell(3);
			assertThat(formulaCell1.getCellType()).isEqualTo(CellType.FORMULA);
			assertThat(formulaCell1.getCellFormula()).isEqualTo("B2*C2");

			Row summaryRow = resultSheet.getRow(3);
			Cell summaryFormulaCell = summaryRow.getCell(3);
			assertThat(summaryFormulaCell.getCellType()).isEqualTo(CellType.FORMULA);
			assertThat(summaryFormulaCell.getCellFormula()).isEqualTo("SUM(D2:D3)");

			// 驗證工作表防護
			assertThat(resultSheet.getProtect()).isTrue();

			// 驗證頁首頁尾
			assertThat(resultSheet.getHeader().getCenter()).isEqualTo("物料採購損益分析月報");
			assertThat(resultSheet.getFooter().getLeft()).contains("財務機密");
		}
	}
}

