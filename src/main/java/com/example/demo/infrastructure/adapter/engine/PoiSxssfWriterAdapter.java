package com.example.demo.infrastructure.adapter.engine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;

import com.example.demo.application.annotation.task.TrackExcelTask;
import com.example.demo.application.port.out.ExcelWriterPort;
import com.example.demo.application.shared.command.WriteExcelCommand;
import com.example.demo.application.shared.command.WriteSheetCommand;
import com.example.demo.application.shared.command.WriteStreamExcelCommand;
import com.example.demo.application.shared.command.WriteStreamSheetCommand;
import com.example.demo.application.shared.context.ExcelTaskContextHolder;
import com.example.demo.application.shared.exception.ExcelWriteException;
import com.example.demo.application.shared.model.CellCoordinate;
import com.example.demo.application.shared.model.CellRange;
import com.example.demo.application.shared.model.SheetHeaderFooterConfig;
import com.example.demo.application.shared.model.SheetProtectionConfig;
import com.example.demo.application.shared.vo.ExcelTaskType;
import com.example.demo.infrastructure.engine.ExcelStreamWriterEngine;
import com.example.demo.infrastructure.engine.poi.PoiSxssfStreamWriterEngine;

/**
 * Apache POI SXSSF 寫入適配器 (Apache POI SXSSF Writer Outbound Adapter)
 * <p>
 * 遵循 Clean Architecture 六角形架構規範，實作應用層 {@link ExcelWriterPort} 介面。
 * 封裝 Apache POI 之 {@link SXSSFWorkbook}，提供功能豐富、支援複雜格式與硬碟暫存沖刷機制之 Excel (.xlsx) 檔案產製服務。
 * 底層大數據串流輸出協調共用之 {@link ExcelStreamWriterEngine}。
 * 命名規範：以 {@code Adapter} 結尾 (3-9 規範)。
 */
public class PoiSxssfWriterAdapter implements ExcelWriterPort {

	private static final int DEFAULT_WINDOW_SIZE = 100;
	private final ExcelStreamWriterEngine streamWriterEngine;

	public PoiSxssfWriterAdapter() {
		this(new PoiSxssfStreamWriterEngine());
	}

	public PoiSxssfWriterAdapter(ExcelStreamWriterEngine streamWriterEngine) {
		this.streamWriterEngine = Objects.requireNonNull(streamWriterEngine, "ExcelStreamWriterEngine must not be null");
	}

	@TrackExcelTask(
			type = ExcelTaskType.EXPORT,
			fileName = "#command != null && #command.applicationName != null ? #command.applicationName + '.xlsx' : 'export.xlsx'",
			description = "Apache POI 批次資料匯出長任務"
	)
	@Override
	public void write(OutputStream outputStream, WriteExcelCommand command) {
		Objects.requireNonNull(outputStream, "OutputStream must not be null");
		Objects.requireNonNull(command, "WriteExcelCommand must not be null");

		SXSSFWorkbook workbook = new SXSSFWorkbook(DEFAULT_WINDOW_SIZE);
		workbook.setCompressTempFiles(true);

		try {
			List<WriteSheetCommand> sheets = command.getSheets();
			if (sheets.isEmpty()) {
				workbook.createSheet("Sheet1");
			} else {
				for (WriteSheetCommand sheetCommand : sheets) {
					writeWorksheet(workbook, sheetCommand);
				}
			}

			workbook.write(outputStream);
			outputStream.flush();
		} catch (IOException e) {
			throw new ExcelWriteException("Failed to write Excel document via Apache POI: " + e.getMessage(), e);
		} catch (Exception e) {
			throw new ExcelWriteException("Unexpected error during Apache POI generation: " + e.getMessage(), e);
		} finally {
			// 清理 SXSSF 於系統產生的硬碟暫存檔案，杜絕磁碟洩漏
			workbook.dispose();
			try {
				workbook.close();
			} catch (IOException ignored) {
			}
		}
	}

	@TrackExcelTask(
			type = ExcelTaskType.EXPORT,
			fileName = "#command != null && #command.applicationName != null ? #command.applicationName + '.xlsx' : 'export.xlsx'",
			description = "Apache POI 批次資料匯出長任務"
	)
	@Override
	public byte[] write(WriteExcelCommand command) {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			write(baos, command);
			return baos.toByteArray();
		} catch (IOException e) {
			throw new ExcelWriteException("Failed to write Excel document to byte array via Apache POI: " + e.getMessage(), e);
		}
	}

	/**
	 * 串流寫入巨量資料活頁簿至指定的 OutputStream (支援非同步長任務追蹤)
	 * <p>
	 * 標註 {@link TrackExcelTask} 宣告式任務監控註解：
	 * <ul>
	 *     <li>當在非同步背景工作緒或長任務情境中執行大數據串流輸出時，自動於持久化儲存庫建立任務記錄並維護其生命週期狀態</li>
	 *     <li>串流迭代過程中依據 {@code flushInterval} 定期沖刷硬碟暫存檔案，並自動將寫入進度回報至 {@link ExcelTaskContextHolder}</li>
	 *     <li>若外層業務呼叫方已建立作用中的任務上下文，則直接沿用現有上下文並持續更新進度，避免重複建檔</li>
	 * </ul>
	 *
	 * @param outputStream 輸出串流
	 * @param command      大數據串流寫入命令
	 */
	@TrackExcelTask(
			type = ExcelTaskType.EXPORT,
			fileName = "#command != null && #command.applicationName != null ? #command.applicationName + '.xlsx' : 'stream_export.xlsx'",
			description = "Apache POI SXSSF 串流大量資料匯出長任務"
	)
	@Override
	public void writeStream(OutputStream outputStream, WriteStreamExcelCommand command) {
		Objects.requireNonNull(outputStream, "OutputStream must not be null");
		Objects.requireNonNull(command, "WriteStreamExcelCommand must not be null");
		streamWriterEngine.writeStream(outputStream, command);
	}

	/**
	 * 串流寫入巨量資料活頁簿並產出 byte 陣列 (支援非同步長任務追蹤)
	 *
	 * @param command 大數據串流寫入命令
	 * @return Excel 二進位檔案 byte 陣列
	 */
	@TrackExcelTask(
			type = ExcelTaskType.EXPORT,
			fileName = "#command != null && #command.applicationName != null ? #command.applicationName + '.xlsx' : 'stream_export.xlsx'",
			description = "Apache POI SXSSF 串流大量資料匯出長任務"
	)
	@Override
	public byte[] writeStream(WriteStreamExcelCommand command) {
		Objects.requireNonNull(command, "WriteStreamExcelCommand must not be null");
		return streamWriterEngine.writeStream(command);
	}

	/**
	 * 寫入單一工作表資料與設定
	 */
	private void writeWorksheet(SXSSFWorkbook workbook, WriteSheetCommand sheetCommand) {
		SXSSFSheet sheet = workbook.createSheet(sheetCommand.getSheetName());
		List<String> headers = sheetCommand.getHeaders();
		List<List<Object>> rows = sheetCommand.getRows();

		// 1. 設定欄寬
		configureColumnWidths(sheet, sheetCommand.getColumnWidths(), headers);

		// 2. 寫入標題列
		if (!headers.isEmpty()) {
			writeHeaders(workbook, sheet, headers, sheetCommand.getHeaderFillColor(), sheetCommand.getHeaderFontColor());
		}

		// 3. 凍結首列
		if (sheetCommand.isFreezeHeader() && !headers.isEmpty()) {
			sheet.createFreezePane(0, 1);
		}

		// 4. 寫入資料列
		int startRow = headers.isEmpty() ? 0 : 1;
		if (rows != null) {
			for (int r = 0; r < rows.size(); r++) {
				List<Object> rowData = rows.get(r);
				if (rowData == null) {
					continue;
				}
				int rowIndex = startRow + r;
				Row row = sheet.createRow(rowIndex);
				for (int c = 0; c < rowData.size(); c++) {
					Cell cell = row.createCell(c);
					String cellRef = CellCoordinate.format(c, rowIndex);
					String formula = sheetCommand.cellFormulas().get(cellRef);
					if (formula != null && !formula.isBlank()) {
						String cleanFormula = formula.startsWith("=") ? formula.substring(1).trim() : formula.trim();
						cell.setCellFormula(cleanFormula);
					} else {
						writeCellValue(cell, rowData.get(c));
					}
				}
			}
		}

		// 5. 套用未被 rows 包含的額外儲存格公式 (如彙總列)
		if (sheetCommand.cellFormulas() != null && !sheetCommand.cellFormulas().isEmpty()) {
			sheetCommand.cellFormulas().forEach((cellRef, formula) -> {
				if (cellRef != null && formula != null && !formula.isBlank()) {
					CellCoordinate coord = CellCoordinate.parse(cellRef);
					Row row = sheet.getRow(coord.row());
					if (row == null) {
						row = sheet.createRow(coord.row());
					}
					Cell cell = row.getCell(coord.col());
					if (cell == null) {
						cell = row.createCell(coord.col());
					}
					String cleanFormula = formula.startsWith("=") ? formula.substring(1).trim() : formula.trim();
					cell.setCellFormula(cleanFormula);
				}
			});
		}

		// 6. 套用維度五企業級功能：合併儲存格、工作表防護與列印頁首頁尾
		applySheetDecorations(sheet, sheetCommand.mergedRegions(), sheetCommand.protectionConfig(), sheetCommand.headerFooterConfig());
	}

	/**
	 * 串流寫入單一工作表，每達 flushInterval 即執行 sheet.flushRows(0)
	 */
	private void writeStreamWorksheet(SXSSFWorkbook workbook, WriteStreamSheetCommand sheetCommand) throws IOException {
		SXSSFSheet sheet = workbook.createSheet(sheetCommand.getSheetName());
		List<String> headers = sheetCommand.getHeaders();

		// 1. 設定欄寬
		configureColumnWidths(sheet, sheetCommand.getColumnWidths(), headers);

		// 2. 寫入標題列
		if (!headers.isEmpty()) {
			writeHeaders(workbook, sheet, headers, sheetCommand.getHeaderFillColor(), sheetCommand.getHeaderFontColor());
		}

		// 3. 凍結首列
		if (sheetCommand.isFreezeHeader() && !headers.isEmpty()) {
			sheet.createFreezePane(0, 1);
		}

		// 4. 串流迭代寫入資料列並定期沖刷
		int startRow = headers.isEmpty() ? 0 : 1;
		Iterable<List<Object>> rowIterable = sheetCommand.getRowIterable();
		int currentRowIndex = startRow;
		int flushInterval = sheetCommand.getFlushInterval() > 0 ? sheetCommand.getFlushInterval() : DEFAULT_WINDOW_SIZE;

		if (rowIterable != null) {
			for (List<Object> rowData : rowIterable) {
				if (rowData != null) {
					Row row = sheet.createRow(currentRowIndex);
					for (int c = 0; c < rowData.size(); c++) {
						Cell cell = row.createCell(c);
						String cellRef = CellCoordinate.format(c, currentRowIndex);
						String formula = sheetCommand.cellFormulas().get(cellRef);
						if (formula != null && !formula.isBlank()) {
							String cleanFormula = formula.startsWith("=") ? formula.substring(1).trim() : formula.trim();
							cell.setCellFormula(cleanFormula);
						} else {
							writeCellValue(cell, rowData.get(c));
						}
					}
					currentRowIndex++;
					if ((currentRowIndex - startRow) % flushInterval == 0) {
						sheet.flushRows(0);
						if (ExcelTaskContextHolder.isActive()) {
							ExcelTaskContextHolder.updateProgress(currentRowIndex - startRow, ExcelTaskContextHolder.getTotalRows());
						}
					}
				}
			}
		}

		sheet.flushRows(0);
		if (ExcelTaskContextHolder.isActive()) {
			ExcelTaskContextHolder.updateProgress(currentRowIndex - startRow, ExcelTaskContextHolder.getTotalRows());
		}

		// 5. 套用維度五企業級功能：合併儲存格、工作表防護與列印頁首頁尾
		applySheetDecorations(sheet, sheetCommand.mergedRegions(), sheetCommand.protectionConfig(), sheetCommand.headerFooterConfig());
	}

	/**
	 * 套用工作表進階裝飾：儲存格合併區間、工作表防護密碼與列印頁首頁尾
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
			org.apache.poi.ss.usermodel.Header header = sheet.getHeader();
			if (headerFooterConfig.leftHeader() != null) header.setLeft(headerFooterConfig.leftHeader());
			if (headerFooterConfig.centerHeader() != null) header.setCenter(headerFooterConfig.centerHeader());
			if (headerFooterConfig.rightHeader() != null) header.setRight(headerFooterConfig.rightHeader());

			org.apache.poi.ss.usermodel.Footer footer = sheet.getFooter();
			if (headerFooterConfig.leftFooter() != null) footer.setLeft(headerFooterConfig.leftFooter());
			if (headerFooterConfig.centerFooter() != null) footer.setCenter(headerFooterConfig.centerFooter());
			if (headerFooterConfig.rightFooter() != null) footer.setRight(headerFooterConfig.rightFooter());
		}
	}

	/**
	 * 設定工作表欄位寬度 (POI 寬度單位為 1/256 字元寬)
	 */
	private void configureColumnWidths(SXSSFSheet sheet, List<Double> customWidths, List<String> headers) {
		if (customWidths != null && !customWidths.isEmpty()) {
			for (int col = 0; col < customWidths.size(); col++) {
				Double width = customWidths.get(col);
				if (width != null && width > 0) {
					sheet.setColumnWidth(col, Math.min(255 * 256, (int) (width * 256)));
				}
			}
		} else if (!headers.isEmpty()) {
			for (int col = 0; col < headers.size(); col++) {
				String header = headers.get(col);
				double estimatedWidth = Math.max(14.0, (header != null ? header.length() * 2.2 : 14.0));
				sheet.setColumnWidth(col, Math.min(255 * 256, (int) (estimatedWidth * 256)));
			}
		}
	}

	/**
	 * 寫入標題列與樣式
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
	 */
	private CellStyle createHeaderStyle(SXSSFWorkbook workbook, String fillColorHex, String fontColorHex) {
		CellStyle style = workbook.createCellStyle();
		Font font = workbook.createFont();
		font.setBold(true);

		if (fontColorHex != null && !fontColorHex.isBlank() && font instanceof XSSFFont xssfFont) {
			byte[] rgb = parseHexRgb(fontColorHex);
			xssfFont.setColor(new XSSFColor(rgb, null));
		}
		style.setFont(font);

		if (fillColorHex != null && !fillColorHex.isBlank() && style instanceof XSSFCellStyle xssfStyle) {
			byte[] rgb = parseHexRgb(fillColorHex);
			xssfStyle.setFillForegroundColor(new XSSFColor(rgb, null));
			xssfStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		}

		return style;
	}

	/**
	 * 解析十六進位色碼為 RGB byte 陣列
	 */
	private byte[] parseHexRgb(String hex) {
		String clean = hex.startsWith("#") ? hex.substring(1) : hex;
		if (clean.length() == 6) {
			int r = Integer.parseInt(clean.substring(0, 2), 16);
			int g = Integer.parseInt(clean.substring(2, 4), 16);
			int b = Integer.parseInt(clean.substring(4, 6), 16);
			return new byte[]{(byte) r, (byte) g, (byte) b};
		}
		return new byte[]{0, 0, 0};
	}

	/**
	 * 依據 Java 物件型別寫入儲存格
	 */
	private void writeCellValue(Cell cell, Object val) {
		if (val == null) {
			return;
		}

		if (val instanceof Number num) {
			cell.setCellValue(num.doubleValue());
		} else if (val instanceof String str) {
			if (str.startsWith("=") && str.length() > 1) {
				cell.setCellFormula(str.substring(1).trim());
			} else {
				cell.setCellValue(str);
			}
		} else if (val instanceof Boolean bool) {
			cell.setCellValue(bool);
		} else if (val instanceof LocalDate localDate) {
			cell.setCellValue(localDate);
		} else if (val instanceof LocalDateTime localDateTime) {
			cell.setCellValue(localDateTime);
		} else if (val instanceof Date date) {
			cell.setCellValue(date);
		} else if (val instanceof ZonedDateTime zonedDateTime) {
			cell.setCellValue(zonedDateTime.toLocalDateTime());
		} else if (val instanceof Instant instant) {
			cell.setCellValue(instant.toString());
		} else {
			cell.setCellValue(val.toString());
		}
	}
}
