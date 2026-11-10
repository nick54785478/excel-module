package com.example.demo.infrastructure.adapter.validation;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import com.example.demo.application.port.out.validation.ExcelErrorMarkerPort;
import com.example.demo.application.shared.model.ExcelRowError;

/**
 * Apache POI Excel 錯誤標註適配器 (Poi Excel Error Marker Adapter - Outbound Adapter)
 * <p>
 * <b>【架構定位與職責規範】</b><br>
 * 位於 Clean Architecture 基礎設施層 ({@code infrastructure/adapter/feedback/})，
 * 遵循 {@code AGENTS.md} 規則 30 與 3-9 (以 {@code Adapter} 結尾)。<br>
 * 實作應用層 {@link ExcelErrorMarkerPort} 合約，隔離底層 Apache POI 樣式繪製技術。
 * 負責保留使用者上傳之原始資料排版，在出錯儲存格套用國際標準淡紅色背景 (ROSE) 與深紅色文字 (DARK_RED) 標註，
 * 並於工作表最右側動態追加「檢核結果與錯誤原因」欄位，生成完整的視覺化錯誤回標檔。
 * </p>
 */
@Component
public class PoiExcelErrorMarkerAdapter implements ExcelErrorMarkerPort {

	@Override
	public void writeErrorMarkedWorkbook(InputStream originalExcelStream, List<ExcelRowError> errors, OutputStream targetOutputStream) {
		Objects.requireNonNull(originalExcelStream, "originalExcelStream must not be null");
		Objects.requireNonNull(targetOutputStream, "targetOutputStream must not be null");
		List<ExcelRowError> safeErrors = errors != null ? errors : Collections.emptyList();

		try (Workbook workbook = new XSSFWorkbook(originalExcelStream)) {
			Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
			if (sheet == null) {
				workbook.write(targetOutputStream);
				return;
			}

			// 準備高亮樣式
			CellStyle errorCellStyle = workbook.createCellStyle();
			errorCellStyle.setFillForegroundColor(IndexedColors.ROSE.getIndex());
			errorCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
			Font errorFont = workbook.createFont();
			errorFont.setColor(IndexedColors.DARK_RED.getIndex());
			errorFont.setBold(true);
			errorCellStyle.setFont(errorFont);

			CellStyle headerCellStyle = workbook.createCellStyle();
			headerCellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
			headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
			Font headerFont = workbook.createFont();
			headerFont.setBold(true);
			headerCellStyle.setFont(headerFont);

			// 依據 rowIndex 分組錯誤
			Map<Integer, List<ExcelRowError>> errorsByRow = safeErrors.stream()
					.collect(Collectors.groupingBy(ExcelRowError::rowIndex));

			// 決定錯誤欄位所在的欄位索引 (最大欄數)
			int maxColIndex = 0;
			for (int r = 0; r <= sheet.getLastRowNum(); r++) {
				Row row = sheet.getRow(r);
				if (row != null && row.getLastCellNum() > maxColIndex) {
					maxColIndex = row.getLastCellNum();
				}
			}

			// 表頭追加錯誤說明欄
			Row headerRow = sheet.getRow(0);
			if (headerRow != null) {
				Cell headerErrorCell = headerRow.createCell(maxColIndex);
				headerErrorCell.setCellValue("檢核結果與錯誤原因");
				headerErrorCell.setCellStyle(headerCellStyle);
			}

			// 遍歷所有含有錯誤的行
			for (Map.Entry<Integer, List<ExcelRowError>> entry : errorsByRow.entrySet()) {
				int rowIdx = entry.getKey() - 1; // 轉為 0-based
				List<ExcelRowError> rowErrors = entry.getValue();
				Row row = sheet.getRow(rowIdx);
				if (row == null) {
					row = sheet.createRow(rowIdx);
				}

				// 高亮出錯欄位
				for (ExcelRowError err : rowErrors) {
					if (err.columnIndex() >= 0) {
						Cell cell = row.getCell(err.columnIndex());
						if (cell == null) {
							cell = row.createCell(err.columnIndex());
						}
						cell.setCellStyle(errorCellStyle);
					}
				}

				// 最右側追加錯誤原因彙總
				String combinedMessages = rowErrors.stream()
						.map(ExcelRowError::errorMessage)
						.collect(Collectors.joining("; "));
				Cell errorFeedbackCell = row.createCell(maxColIndex);
				errorFeedbackCell.setCellValue(combinedMessages);
				errorFeedbackCell.setCellStyle(errorCellStyle);
			}

			// 自動調整錯誤說明欄寬
			sheet.autoSizeColumn(maxColIndex);

			workbook.write(targetOutputStream);
		} catch (Exception e) {
			throw new RuntimeException("Failed to write error marked Excel workbook: " + e.getMessage(), e);
		}
	}

	@Override
	public byte[] generateErrorMarkedWorkbook(InputStream originalExcelStream, List<ExcelRowError> errors) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		writeErrorMarkedWorkbook(originalExcelStream, errors, baos);
		return baos.toByteArray();
	}
}
