package com.example.demo.infrastructure.engine.poi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Footer;
import org.apache.poi.ss.usermodel.Header;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;

import com.example.demo.application.shared.command.WriteStreamExcelCommand;
import com.example.demo.application.shared.command.WriteStreamSheetCommand;
import com.example.demo.application.shared.context.ExcelTaskContextHolder;
import com.example.demo.application.shared.exception.ExcelWriteException;
import com.example.demo.application.shared.model.CellCoordinate;
import com.example.demo.application.shared.model.CellRange;
import com.example.demo.application.shared.model.SheetHeaderFooterConfig;
import com.example.demo.application.shared.model.SheetProtectionConfig;
import com.example.demo.infrastructure.engine.ExcelStreamWriterEngine;

/**
 * Apache POI SXSSF 串流寫入共用引擎實作 (Apache POI SXSSF Stream Writer Engine)
 * <p>
 * <b>【架構定位與職責說明】</b><br>
 * 座落於 Clean Architecture 基礎設施層 (Infrastructure Layer)，實作內部共用引擎合約 {@link ExcelStreamWriterEngine}。<br>
 * 封裝 Apache POI 之 {@link SXSSFWorkbook}，提供自動磁碟分頁沖刷（預設滑動視窗 100 列）、低記憶體佔用與完整儲存格格式支援。<br>
 * 本組件供外層適配器內部共用，杜絕適配器之間互相依賴或跨層依賴應用層 Port 的壞味道。
 * </p>
 */
public class PoiSxssfStreamWriterEngine implements ExcelStreamWriterEngine {

	private static final int DEFAULT_WINDOW_SIZE = 100;
	private static final int POI_WIDTH_UNIT = 256;
	private static final int MAX_COLUMN_WIDTH = 255 * POI_WIDTH_UNIT;
	private static final double DEFAULT_ESTIMATED_WIDTH = 14.0;
	private static final double CHAR_WIDTH_MULTIPLIER = 2.2;
	private static final byte[] DEFAULT_BLACK_RGB = new byte[]{0, 0, 0};
	private static final DefaultIndexedColorMap DEFAULT_COLOR_MAP = new DefaultIndexedColorMap();

	@Override
	public void writeStream(OutputStream outputStream, WriteStreamExcelCommand command) {
		Objects.requireNonNull(outputStream, "OutputStream must not be null");
		Objects.requireNonNull(command, "WriteStreamExcelCommand must not be null");

		try (SXSSFWorkbook workbook = new SXSSFWorkbook(DEFAULT_WINDOW_SIZE)) {
			workbook.setCompressTempFiles(true);
			try {
				List<WriteStreamSheetCommand> sheets = command.getSheets();
				if (sheets == null || sheets.isEmpty()) {
					workbook.createSheet("Sheet1");
				} else {
					for (WriteStreamSheetCommand sheetCommand : sheets) {
						writeStreamWorksheet(workbook, sheetCommand);
					}
				}

				workbook.write(outputStream);
				outputStream.flush();
			} finally {
				workbook.dispose();
			}
		} catch (IOException e) {
			throw new ExcelWriteException("Failed to stream write Excel document via Apache POI: " + e.getMessage(), e);
		} catch (RuntimeException e) {
			throw new ExcelWriteException("Unexpected error during Apache POI stream generation: " + e.getMessage(), e);
		}
	}

	@Override
	public byte[] writeStream(WriteStreamExcelCommand command) {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			writeStream(baos, command);
			return baos.toByteArray();
		} catch (IOException e) {
			throw new ExcelWriteException("Failed to stream write Excel to byte array via Apache POI: " + e.getMessage(), e);
		}
	}

	/**
	 * 串流寫入單一工作表，每達 flushInterval 即執行 sheet.flushRows(0) 沖刷至硬碟暫存檔
	 *
	 * @param workbook     SXSSF 活頁簿實例
	 * @param sheetCommand 工作表寫入命令
	 * @throws IOException 當串流沖刷失敗時拋出
	 */
	private void writeStreamWorksheet(SXSSFWorkbook workbook, WriteStreamSheetCommand sheetCommand) throws IOException {
		SXSSFSheet sheet = workbook.createSheet(sheetCommand.getSheetName());
		List<String> headers = sheetCommand.getHeaders();

		// 1. 設定欄寬
		configureColumnWidths(sheet, sheetCommand.getColumnWidths(), headers);

		// 2. 寫入標題列
		if (headers != null && !headers.isEmpty()) {
			writeHeaders(workbook, sheet, headers, sheetCommand.getHeaderFillColor(), sheetCommand.getHeaderFontColor());
		}

		// 3. 凍結首列
		if (sheetCommand.isFreezeHeader() && headers != null && !headers.isEmpty()) {
			sheet.createFreezePane(0, 1);
		}

		// 4. 串流迭代寫入資料列並定期沖刷
		int startRow = (headers == null || headers.isEmpty()) ? 0 : 1;
		Iterable<List<Object>> rowIterable = sheetCommand.getRowIterable();
		int currentRowIndex = startRow;
		int flushInterval = sheetCommand.getFlushInterval() > 0 ? sheetCommand.getFlushInterval() : DEFAULT_WINDOW_SIZE;
		Map<String, String> formulas = sheetCommand.cellFormulas();

		if (rowIterable != null) {
			for (List<Object> rowData : rowIterable) {
				if (rowData != null) {
					Row row = sheet.createRow(currentRowIndex);
					for (int c = 0; c < rowData.size(); c++) {
						Cell cell = row.createCell(c);
						String cellRef = CellCoordinate.format(c, currentRowIndex);
						String formula = formulas != null ? formulas.get(cellRef) : null;
						if (formula != null && !formula.isBlank()) {
							cell.setCellFormula(cleanFormula(formula));
						} else {
							writeCellValue(cell, rowData.get(c));
						}
					}
					currentRowIndex++;
					if ((currentRowIndex - startRow) % flushInterval == 0) {
						flushProgress(sheet, currentRowIndex - startRow);
					}
				}
			}
		}

		flushProgress(sheet, currentRowIndex - startRow);

		// 5. 套用未被 rows 包含的額外儲存格公式 (如彙總列)
		if (formulas != null && !formulas.isEmpty()) {
			formulas.forEach((cellRef, form) -> {
				if (cellRef != null && form != null && !form.isBlank()) {
					CellCoordinate coord = CellCoordinate.parse(cellRef);
					Row row = sheet.getRow(coord.row());
					if (row == null) {
						row = sheet.createRow(coord.row());
					}
					Cell cell = row.getCell(coord.col());
					if (cell == null) {
						cell = row.createCell(coord.col());
					}
					cell.setCellFormula(cleanFormula(form));
				}
			});
		}

		// 6. 套用裝飾：合併儲存格、工作表防護與列印頁首頁尾
		applySheetDecorations(sheet, sheetCommand.mergedRegions(), sheetCommand.protectionConfig(), sheetCommand.headerFooterConfig());
	}

	/**
	 * 沖刷工作表資料列至硬碟暫存檔並同步更新進度指示器
	 *
	 * @param sheet         SXSSF 工作表
	 * @param processedRows 目前已處理之資料列數
	 * @throws IOException 當檔案磁碟沖刷異常時拋出
	 */
	private void flushProgress(SXSSFSheet sheet, int processedRows) throws IOException {
		sheet.flushRows(0);
		if (ExcelTaskContextHolder.isActive()) {
			ExcelTaskContextHolder.updateProgress(processedRows, ExcelTaskContextHolder.getTotalRows());
		}
	}

	/**
	 * 正規化並修剪公式字串（自動移除開頭的等號與前後多餘空白）
	 *
	 * @param formula 原始公式字串
	 * @return 正規化後的公式表示式
	 */
	private String cleanFormula(String formula) {
		if (formula == null) {
			return "";
		}
		String trimmed = formula.trim();
		return trimmed.startsWith("=") ? trimmed.substring(1).trim() : trimmed;
	}

	/**
	 * 套用工作表進階裝飾：儲存格合併區間、工作表防護密碼與列印頁首頁尾
	 *
	 * @param sheet              SXSSF 工作表
	 * @param mergedRegions      合併儲存格區間清單
	 * @param protectionConfig   工作表防護設定
	 * @param headerFooterConfig 列印頁首頁尾設定
	 */
	private void applySheetDecorations(
			SXSSFSheet sheet,
			List<CellRange> mergedRegions,
			SheetProtectionConfig protectionConfig,
			SheetHeaderFooterConfig headerFooterConfig
	) {
		// 1. 合併儲存格區間
		if (mergedRegions != null && !mergedRegions.isEmpty()) {
			for (CellRange range : mergedRegions) {
				sheet.addMergedRegion(new CellRangeAddress(range.firstRow(), range.lastRow(), range.firstCol(), range.lastCol()));
			}
		}

		// 2. 工作表安全防護
		if (protectionConfig != null) {
			sheet.protectSheet(protectionConfig.password() != null ? protectionConfig.password() : "");
			sheet.enableLocking();
		}

		// 3. 列印頁首頁尾
		if (headerFooterConfig != null) {
			Header header = sheet.getHeader();
			if (headerFooterConfig.leftHeader() != null) {
				header.setLeft(headerFooterConfig.leftHeader());
			}
			if (headerFooterConfig.centerHeader() != null) {
				header.setCenter(headerFooterConfig.centerHeader());
			}
			if (headerFooterConfig.rightHeader() != null) {
				header.setRight(headerFooterConfig.rightHeader());
			}

			Footer footer = sheet.getFooter();
			if (headerFooterConfig.leftFooter() != null) {
				footer.setLeft(headerFooterConfig.leftFooter());
			}
			if (headerFooterConfig.centerFooter() != null) {
				footer.setCenter(headerFooterConfig.centerFooter());
			}
			if (headerFooterConfig.rightFooter() != null) {
				footer.setRight(headerFooterConfig.rightFooter());
			}
		}
	}

	/**
	 * 設定工作表欄位寬度 (POI 寬度單位為 1/256 字元寬)
	 *
	 * @param sheet        SXSSF 工作表
	 * @param customWidths 自訂欄寬清單
	 * @param headers      標題欄位清單
	 */
	private void configureColumnWidths(SXSSFSheet sheet, List<Double> customWidths, List<String> headers) {
		if (customWidths != null && !customWidths.isEmpty()) {
			for (int col = 0; col < customWidths.size(); col++) {
				Double width = customWidths.get(col);
				if (width != null && width > 0) {
					sheet.setColumnWidth(col, Math.min(MAX_COLUMN_WIDTH, (int) (width * POI_WIDTH_UNIT)));
				}
			}
		} else if (headers != null && !headers.isEmpty()) {
			for (int col = 0; col < headers.size(); col++) {
				String header = headers.get(col);
				double estimatedWidth = Math.max(DEFAULT_ESTIMATED_WIDTH, (header != null ? header.length() * CHAR_WIDTH_MULTIPLIER : DEFAULT_ESTIMATED_WIDTH));
				sheet.setColumnWidth(col, Math.min(MAX_COLUMN_WIDTH, (int) (estimatedWidth * POI_WIDTH_UNIT)));
			}
		}
	}

	/**
	 * 寫入標題列與樣式
	 *
	 * @param workbook     SXSSF 活頁簿
	 * @param sheet        SXSSF 工作表
	 * @param headers      標題欄位清單
	 * @param fillColorHex 標題列背景 HEX 填色
	 * @param fontColorHex 標題文字 HEX 顏色
	 */
	private void writeHeaders(SXSSFWorkbook workbook, SXSSFSheet sheet, List<String> headers, String fillColorHex, String fontColorHex) {
		Row headerRow = sheet.createRow(0);
		CellStyle headerStyle = createHeaderStyle(workbook, fillColorHex, fontColorHex);

		for (int col = 0; col < headers.size(); col++) {
			Cell cell = headerRow.createCell(col);
			cell.setCellValue(headers.get(col));
			cell.setCellStyle(headerStyle);
		}
	}

	/**
	 * 建立標題列樣式
	 *
	 * @param workbook     SXSSF 活頁簿
	 * @param fillColorHex 標題列背景 HEX 填色
	 * @param fontColorHex 標題文字 HEX 顏色
	 * @return 建立完成之儲存格樣式
	 */
	private CellStyle createHeaderStyle(SXSSFWorkbook workbook, String fillColorHex, String fontColorHex) {
		CellStyle style = workbook.createCellStyle();
		Font font = workbook.createFont();
		font.setBold(true);

		if (fontColorHex != null && !fontColorHex.isBlank() && font instanceof XSSFFont xssfFont) {
			byte[] rgb = parseHexRgb(fontColorHex);
			xssfFont.setColor(new XSSFColor(rgb, DEFAULT_COLOR_MAP));
		}
		style.setFont(font);

		if (fillColorHex != null && !fillColorHex.isBlank() && style instanceof XSSFCellStyle xssfStyle) {
			byte[] rgb = parseHexRgb(fillColorHex);
			xssfStyle.setFillForegroundColor(new XSSFColor(rgb, DEFAULT_COLOR_MAP));
			xssfStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		}

		return style;
	}

	/**
	 * 解析十六進位色碼為 RGB byte 陣列（支援防禦性例外處理與預設黑色回退）
	 *
	 * @param hex HEX 色碼字串 (如 "#FFFFFF" 或 "FFFFFF")
	 * @return RGB 3 位元組陣列
	 */
	private byte[] parseHexRgb(String hex) {
		if (hex == null || hex.isBlank()) {
			return DEFAULT_BLACK_RGB.clone();
		}
		String clean = hex.startsWith("#") ? hex.substring(1) : hex;
		if (clean.length() == 6) {
			try {
				int r = Integer.parseInt(clean.substring(0, 2), 16);
				int g = Integer.parseInt(clean.substring(2, 4), 16);
				int b = Integer.parseInt(clean.substring(4, 6), 16);
				return new byte[]{(byte) r, (byte) g, (byte) b};
			} catch (NumberFormatException ignored) {
				return DEFAULT_BLACK_RGB.clone();
			}
		}
		return DEFAULT_BLACK_RGB.clone();
	}

	/**
	 * 依據 Java 物件型別寫入儲存格（採用 Java 21 Pattern Matching Switch）
	 *
	 * @param cell 目標儲存格
	 * @param val  欲寫入之數值物件
	 */
	private void writeCellValue(Cell cell, Object val) {
		if (val == null) {
			return;
		}

		switch (val) {
			case Number num -> cell.setCellValue(num.doubleValue());
			case String str -> {
				if (str.startsWith("=") && str.length() > 1) {
					cell.setCellFormula(cleanFormula(str));
				} else {
					cell.setCellValue(str);
				}
			}
			case Boolean bool -> cell.setCellValue(bool);
			case LocalDate localDate -> cell.setCellValue(localDate);
			case LocalDateTime localDateTime -> cell.setCellValue(localDateTime);
			case Date date -> cell.setCellValue(date);
			case ZonedDateTime zonedDateTime -> cell.setCellValue(zonedDateTime.toLocalDateTime());
			case Instant instant -> cell.setCellValue(instant.toString());
			default -> cell.setCellValue(val.toString());
		}
	}
}
