package com.example.demo.infrastructure.engine.fastexcel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.dhatim.fastexcel.SheetProtectionOption;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;

import com.example.demo.application.shared.command.WriteStreamExcelCommand;
import com.example.demo.application.shared.command.WriteStreamSheetCommand;
import com.example.demo.application.shared.context.ExcelTaskContextHolder;
import com.example.demo.application.shared.exception.ExcelWriteException;
import com.example.demo.application.shared.vo.CellCoordinate;
import com.example.demo.application.shared.vo.CellRange;
import com.example.demo.application.shared.vo.SheetProtectionConfig;
import com.example.demo.infrastructure.engine.ExcelStreamWriterEngine;

/**
 * FastExcel 串流寫入共用引擎實作 (FastExcel Stream Writer Engine)
 * <p>
 * <b>【架構定位與職責說明】</b><br>
 * 座落於 Clean Architecture 基礎設施層 (Infrastructure Layer)，實作內部共用引擎合約 {@link ExcelStreamWriterEngine}。<br>
 * 封裝第三方極速串流套件 {@code org.dhatim:fastexcel}，提供低記憶體佔用、毫秒級高吞吐量之 Excel (.xlsx) 檔案生成服務。<br>
 * 本組件供外層適配器內部共用，杜絕適配器之間互相依賴或跨層依賴應用層 Port 的壞味道。
 * </p>
 */
public class FastExcelStreamWriterEngine implements ExcelStreamWriterEngine {

	private static final double DEFAULT_ESTIMATED_WIDTH = 14.0;
	private static final double CHAR_WIDTH_MULTIPLIER = 2.2;
	private static final int DEFAULT_FLUSH_INTERVAL = 1000;

	@Override
	public void writeStream(OutputStream outputStream, WriteStreamExcelCommand command) {
		Objects.requireNonNull(outputStream, "OutputStream must not be null");
		Objects.requireNonNull(command, "WriteStreamExcelCommand must not be null");

		try (Workbook workbook = new Workbook(outputStream, command.getApplicationName(), command.getApplicationVersion())) {
			List<WriteStreamSheetCommand> sheets = command.getSheets();
			if (sheets == null || sheets.isEmpty()) {
				workbook.newWorksheet("Sheet1");
			} else {
				for (WriteStreamSheetCommand sheetCommand : sheets) {
					writeStreamWorksheet(workbook, sheetCommand);
				}
			}
			workbook.finish();
		} catch (IOException e) {
			throw new ExcelWriteException("Failed to stream write Excel document via FastExcel: " + e.getMessage(), e);
		} catch (RuntimeException e) {
			throw new ExcelWriteException("Unexpected error during FastExcel stream generation: " + e.getMessage(), e);
		}
	}

	@Override
	public byte[] writeStream(WriteStreamExcelCommand command) {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			writeStream(baos, command);
			return baos.toByteArray();
		} catch (IOException e) {
			throw new ExcelWriteException("Failed to stream write Excel to byte array: " + e.getMessage(), e);
		}
	}

	/**
	 * 串流寫入單一工作表，每達 flushInterval 即執行 worksheet.flush() 釋放記憶體
	 *
	 * @param workbook     FastExcel 活頁簿實例
	 * @param sheetCommand 工作表寫入命令
	 * @throws IOException 當底層串流寫入失敗時拋出
	 */
	private void writeStreamWorksheet(Workbook workbook, WriteStreamSheetCommand sheetCommand) throws IOException {
		Worksheet worksheet = workbook.newWorksheet(sheetCommand.getSheetName());
		List<String> headers = sheetCommand.getHeaders();

		// 1. 設定欄寬
		configureColumnWidths(worksheet, sheetCommand.getColumnWidths(), headers);

		// 2. 寫入標題
		if (headers != null && !headers.isEmpty()) {
			for (int col = 0; col < headers.size(); col++) {
				worksheet.value(0, col, headers.get(col));
				var styleSetter = worksheet.style(0, col).bold();
				if (sheetCommand.getHeaderFillColor() != null && !sheetCommand.getHeaderFillColor().isBlank()) {
					styleSetter.fillColor(sheetCommand.getHeaderFillColor());
				}
				if (sheetCommand.getHeaderFontColor() != null && !sheetCommand.getHeaderFontColor().isBlank()) {
					styleSetter.fontColor(sheetCommand.getHeaderFontColor());
				}
				styleSetter.set();
			}
		}

		// 3. 凍結首列
		if (sheetCommand.isFreezeHeader() && headers != null && !headers.isEmpty()) {
			worksheet.freezePane(0, 1);
		}

		// 4. 串流迭代寫入資料列，定期執行 flush 釋放記憶體
		int startRow = (headers == null || headers.isEmpty()) ? 0 : 1;
		Iterable<List<Object>> rowIterable = sheetCommand.getRowIterable();
		int currentRowIndex = startRow;
		int flushInterval = sheetCommand.getFlushInterval() > 0 ? sheetCommand.getFlushInterval() : DEFAULT_FLUSH_INTERVAL;
		Map<String, String> formulas = sheetCommand.cellFormulas();

		if (rowIterable != null) {
			for (List<Object> row : rowIterable) {
				if (row != null) {
					for (int c = 0; c < row.size(); c++) {
						String cellRef = CellCoordinate.format(c, currentRowIndex);
						String formula = formulas != null ? formulas.get(cellRef) : null;
						if (formula != null && !formula.isBlank()) {
							worksheet.formula(currentRowIndex, c, cleanFormula(formula));
						} else {
							writeCellValue(worksheet, currentRowIndex, c, row.get(c));
						}
					}
					currentRowIndex++;
					if ((currentRowIndex - startRow) % flushInterval == 0) {
						worksheet.flush();
						updateProgressIfActive(currentRowIndex - startRow);
					}
				}
			}
		}

		// 5. 套用裝飾：合併儲存格與工作表防護
		applySheetDecorations(worksheet, sheetCommand.mergedRegions(), sheetCommand.protectionConfig());

		// 串流結束後結束本工作表寫入並關閉 ZIP Entry，確保後續工作表串流不衝突
		worksheet.finish();
		updateProgressIfActive(currentRowIndex - startRow);
	}

	/**
	 * 設定工作表欄位寬度
	 *
	 * @param worksheet    FastExcel 工作表
	 * @param customWidths 自訂欄寬清單
	 * @param headers      標題欄位清單
	 */
	private void configureColumnWidths(Worksheet worksheet, List<Double> customWidths, List<String> headers) {
		if (customWidths != null && !customWidths.isEmpty()) {
			for (int col = 0; col < customWidths.size(); col++) {
				Double width = customWidths.get(col);
				if (width != null && width > 0) {
					worksheet.width(col, width);
				}
			}
		} else if (headers != null && !headers.isEmpty()) {
			for (int col = 0; col < headers.size(); col++) {
				String header = headers.get(col);
				double estimatedWidth = Math.max(DEFAULT_ESTIMATED_WIDTH, (header != null ? header.length() * CHAR_WIDTH_MULTIPLIER : DEFAULT_ESTIMATED_WIDTH));
				worksheet.width(col, estimatedWidth);
			}
		}
	}

	/**
	 * 若任務上下文處於啟用狀態，則同步更新任務進度
	 *
	 * @param processedRows 目前已處理之列數
	 */
	private void updateProgressIfActive(int processedRows) {
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
	 * 套用工作表進階裝飾：儲存格合併區間與工作表防護密碼
	 *
	 * @param worksheet        FastExcel 工作表
	 * @param mergedRegions    合併儲存格區間清單
	 * @param protectionConfig 工作表防護設定
	 */
	private void applySheetDecorations(Worksheet worksheet, List<CellRange> mergedRegions, SheetProtectionConfig protectionConfig) {
		if (mergedRegions != null && !mergedRegions.isEmpty()) {
			for (CellRange range : mergedRegions) {
				worksheet.range(range.firstRow(), range.firstCol(), range.lastRow(), range.lastCol()).merge();
			}
		}

		if (protectionConfig != null) {
			String pwd = protectionConfig.password() != null ? protectionConfig.password() : "";
			Set<SheetProtectionOption> options = EnumSet.noneOf(SheetProtectionOption.class);
			options.addAll(SheetProtectionOption.DEFAULT_OPTIONS);
			options.add(SheetProtectionOption.SHEET);
			if (protectionConfig.allowSorting()) {
				options.add(SheetProtectionOption.SORT);
			}
			if (protectionConfig.allowFiltering()) {
				options.add(SheetProtectionOption.AUTO_FILTER);
			}
			if (protectionConfig.allowSelectLockedCells()) {
				options.add(SheetProtectionOption.SELECT_LOCKED_CELLS);
			}
			if (protectionConfig.allowSelectUnlockedCells()) {
				options.add(SheetProtectionOption.SELECT_UNLOCKED_CELLS);
			}
			worksheet.protect(pwd, options);
		}
	}

	/**
	 * 寫入單一儲存格數值，依據物件型別調用對應的 FastExcel value 重載方法 (採用 Java 21 Pattern Matching Switch)
	 *
	 * @param worksheet FastExcel 工作表
	 * @param row       列索引
	 * @param col       行索引
	 * @param val       欲寫入之數值物件
	 */
	private void writeCellValue(Worksheet worksheet, int row, int col, Object val) {
		if (val == null) {
			return;
		}

		switch (val) {
			case Number num -> worksheet.value(row, col, num);
			case String str -> {
				if (str.startsWith("=") && str.length() > 1) {
					worksheet.formula(row, col, cleanFormula(str));
				} else {
					worksheet.value(row, col, str);
				}
			}
			case Boolean bool -> worksheet.value(row, col, bool);
			case LocalDate localDate -> worksheet.value(row, col, localDate);
			case LocalDateTime localDateTime -> worksheet.value(row, col, localDateTime);
			case ZonedDateTime zonedDateTime -> worksheet.value(row, col, zonedDateTime);
			case Instant instant -> worksheet.value(row, col, instant);
			case Date date -> worksheet.value(row, col, date);
			default -> worksheet.value(row, col, val.toString());
		}
	}
}
