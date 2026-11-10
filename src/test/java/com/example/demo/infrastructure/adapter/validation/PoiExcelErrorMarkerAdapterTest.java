package com.example.demo.infrastructure.adapter.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.demo.application.shared.model.ExcelRowError;

/**
 * Apache POI Excel 錯誤標註適配器測試 (PoiExcelErrorMarkerAdapter Test)
 * <p>
 * 遵循 BDD 與 Gherkin 規範，驗證錯誤標註適配器正確標註儲存格樣式 (ROSE 淡紅底 + DARK_RED 深紅字)
 * 並在工作表最右側追加「檢核結果與錯誤原因」說明欄位。
 * </p>
 */
@DisplayName("PoiExcelErrorMarkerAdapter 錯誤標註適配器測試")
class PoiExcelErrorMarkerAdapterTest {

	private PoiExcelErrorMarkerAdapter adapter;

	@BeforeEach
	void setUp() {
		adapter = new PoiExcelErrorMarkerAdapter();
	}

	/**
	 * Feature: Excel 錯誤標註檔樣式渲染與錯誤原因欄位追加
	 * 
	 * Scenario: 傳入含有錯誤事實之試算表，系統應高亮出錯儲存格並於最右側生成詳細錯誤說明
	 * Given 一份包含 3 欄 (訂單編號、訂購人、金額) 與 2 筆資料的原始 Excel 檔案
	 * And 第 2 列金額欄位 (索引 2) 發生數值錯誤
	 * And 第 3 列訂購人欄位 (索引 1) 發生缺失錯誤
	 * When 呼叫 PoiExcelErrorMarkerAdapter 產製錯誤回標檔
	 * Then 產出之試算表表頭最右側 (第 3 欄) 應追加「檢核結果與錯誤原因」
	 * And 第 2 列金額儲存格應渲染為 ROSE 背景
	 * And 第 3 列訂購人儲存格應渲染為 ROSE 背景
	 * And 錯誤說明欄應依序寫入對應的錯誤原因文字
	 */
	@Test
	@DisplayName("場景：出錯試算表應正確渲染淡紅底樣式並追加錯誤原因說明欄")
	void shouldHighlightErrorCellsAndAppendErrorExplanationColumn() throws IOException {
		// Given
		byte[] originalExcelBytes = createSampleExcel();
		List<ExcelRowError> errors = List.of(
				new ExcelRowError(2, 2, "金額", "購買金額不可為負數"),
				new ExcelRowError(3, 1, "訂購人", "訂購人姓名不可為空"));

		ByteArrayOutputStream targetOutputStream = new ByteArrayOutputStream();

		// When
		adapter.writeErrorMarkedWorkbook(new ByteArrayInputStream(originalExcelBytes), errors, targetOutputStream);

		// Then
		byte[] resultBytes = targetOutputStream.toByteArray();
		assertTrue(resultBytes.length > 0, "回標 Excel 檔案不應為空");

		try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(resultBytes))) {
			Sheet sheet = workbook.getSheetAt(0);
			assertNotNull(sheet);

			// 驗證表頭追加
			Row headerRow = sheet.getRow(0);
			Cell feedbackHeaderCell = headerRow.getCell(3);
			assertNotNull(feedbackHeaderCell, "表頭應追加錯誤回標欄");
			assertEquals("檢核結果與錯誤原因", feedbackHeaderCell.getStringCellValue());

			// 驗證第 2 列 (Excel 行號 2 -> row index 1)
			Row dataRow1 = sheet.getRow(1);
			Cell errorCellRow1 = dataRow1.getCell(2);
			assertEquals(IndexedColors.ROSE.getIndex(), errorCellRow1.getCellStyle().getFillForegroundColor());
			assertEquals(FillPatternType.SOLID_FOREGROUND, errorCellRow1.getCellStyle().getFillPattern());
			Cell feedbackCellRow1 = dataRow1.getCell(3);
			assertEquals("購買金額不可為負數", feedbackCellRow1.getStringCellValue());

			// 驗證第 3 列 (Excel 行號 3 -> row index 2)
			Row dataRow2 = sheet.getRow(2);
			Cell errorCellRow2 = dataRow2.getCell(1);
			assertEquals(IndexedColors.ROSE.getIndex(), errorCellRow2.getCellStyle().getFillForegroundColor());
			assertEquals(FillPatternType.SOLID_FOREGROUND, errorCellRow2.getCellStyle().getFillPattern());
			Cell feedbackCellRow2 = dataRow2.getCell(3);
			assertEquals("訂購人姓名不可為空", feedbackCellRow2.getStringCellValue());
		}
	}

	private byte[] createSampleExcel() throws IOException {
		try (Workbook workbook = new XSSFWorkbook()) {
			Sheet sheet = workbook.createSheet("測試資料");
			Row header = sheet.createRow(0);
			header.createCell(0).setCellValue("訂單編號");
			header.createCell(1).setCellValue("訂購人");
			header.createCell(2).setCellValue("金額");

			Row row1 = sheet.createRow(1);
			row1.createCell(0).setCellValue("ORD-001");
			row1.createCell(1).setCellValue("王小明");
			row1.createCell(2).setCellValue(-50.0);

			Row row2 = sheet.createRow(2);
			row2.createCell(0).setCellValue("ORD-002");
			row2.createCell(1).setCellValue("");
			row2.createCell(2).setCellValue(100.0);

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			workbook.write(baos);
			return baos.toByteArray();
		}
	}
}
