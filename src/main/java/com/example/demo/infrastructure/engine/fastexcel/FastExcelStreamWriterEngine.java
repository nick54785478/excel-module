package com.example.demo.infrastructure.engine.fastexcel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.dhatim.fastexcel.SheetProtectionOption;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;

import com.example.demo.application.shared.command.WriteStreamExcelCommand;
import com.example.demo.application.shared.command.WriteStreamSheetCommand;
import com.example.demo.application.shared.context.ExcelTaskContextHolder;
import com.example.demo.application.shared.exception.ExcelWriteException;
import com.example.demo.application.shared.model.CellCoordinate;
import com.example.demo.application.shared.model.CellRange;
import com.example.demo.application.shared.model.SheetProtectionConfig;
import com.example.demo.infrastructure.engine.ExcelStreamWriterEngine;

/**
 * FastExcel 串流寫入共用引擎實作 (FastExcel Stream Writer Engine)
 * <p>
 * <b>【架構定位與職責說明】</b><br>
 * 座落於 Clean Architecture 基礎設施層 (Infrastructure Layer)，實作內部共用引擎合約 {@link ExcelStreamWriterEngine}。<br>
 * 封裝第三方極速串流套件 {@code org.dhatim:fastexcel}，提供低記憶體佔用、毫秒級高吞吐量之 Excel (.xlsx) 檔案生成服務。<br>
 * 本組件供外層適配器內部共用，杜絕適配器之間互相依賴或跨層依賴應用層 Port 的壞味道。
 */
public class FastExcelStreamWriterEngine implements ExcelStreamWriterEngine {

	@Override
	public void writeStream(OutputStream outputStream, WriteStreamExcelCommand command) {
		Objects.requireNonNull(outputStream, "OutputStream must not be null");
		Objects.requireNonNull(command, "WriteStreamExcelCommand must not be null");

		try (Workbook workbook = new Workbook(outputStream, command.getApplicationName(), command.getApplicationVersion())) {
			List<WriteStreamSheetCommand> sheets = command.getSheets();
			if (sheets.isEmpty()) {
				workbook.newWorksheet("Sheet1");
			} else {
				for (WriteStreamSheetCommand sheetCommand : sheets) {
					writeStreamWorksheet(workbook, sheetCommand);
				}
			}
			workbook.finish();
		} catch (IOException e) {
			throw new ExcelWriteException("Failed to stream write Excel document via FastExcel: " + e.getMessage(), e);
		} catch (Exception e) {
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
	 * 串流寫入單一工作表，每達 flushInterval 即執行 worksheet.flush()
	 */
	private void writeStreamWorksheet(Workbook workbook, WriteStreamSheetCommand sheetCommand) throws IOException {
		Worksheet worksheet = workbook.newWorksheet(sheetCommand.getSheetName());
		List<String> headers = sheetCommand.getHeaders();

		// 1. 設定欄寬
		if (sheetCommand.getColumnWidths() != null && !sheetCommand.getColumnWidths().isEmpty()) {
			for (int col = 0; col < sheetCommand.getColumnWidths().size(); col++) {
				Double width = sheetCommand.getColumnWidths().get(col);
				if (width != null && width > 0) {
					worksheet.width(col, width);
				}
			}
		} else if (!headers.isEmpty()) {
			for (int col = 0; col < headers.size(); col++) {
				String header = headers.get(col);
				double estimatedWidth = Math.max(14.0, (header != null ? header.length() * 2.2 : 14.0));
				worksheet.width(col, estimatedWidth);
			}
		}

		// 2. 寫入標題
		if (!headers.isEmpty()) {
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
		if (sheetCommand.isFreezeHeader() && !headers.isEmpty()) {
			worksheet.freezePane(0, 1);
		}

		// 4. 串流迭代寫入資料列，定期執行 flush 釋放記憶體
		int startRow = headers.isEmpty() ? 0 : 1;
		Iterable<List<Object>> rowIterable = sheetCommand.getRowIterable();
		int currentRowIndex = startRow;
		int flushInterval = sheetCommand.getFlushInterval();

		if (rowIterable != null) {
			for (List<Object> row : rowIterable) {
				if (row != null) {
					for (int c = 0; c < row.size(); c++) {
						String cellRef = CellCoordinate.format(c, currentRowIndex);
						String formula = sheetCommand.cellFormulas().get(cellRef);
						if (formula != null && !formula.isBlank()) {
							String cleanFormula = formula.startsWith("=") ? formula.substring(1).trim() : formula.trim();
							worksheet.formula(currentRowIndex, c, cleanFormula);
						} else {
							writeCellValue(worksheet, currentRowIndex, c, row.get(c));
						}
					}
					currentRowIndex++;
					if ((currentRowIndex - startRow) % flushInterval == 0) {
						worksheet.flush();
						if (ExcelTaskContextHolder.isActive()) {
							ExcelTaskContextHolder.updateProgress(currentRowIndex - startRow, ExcelTaskContextHolder.getTotalRows());
						}
					}
				}
			}
		}

		// 5. 套用裝飾：合併儲存格與工作表防護
		applySheetDecorations(worksheet, sheetCommand.mergedRegions(), sheetCommand.protectionConfig());

		// 串流結束後結束本工作表寫入並關閉 ZIP Entry，確保後續工作表串流不衝突
		worksheet.finish();
		if (ExcelTaskContextHolder.isActive()) {
			ExcelTaskContextHolder.updateProgress(currentRowIndex - startRow, ExcelTaskContextHolder.getTotalRows());
		}
	}

	/**
	 * 套用工作表進階裝飾：儲存格合併區間與工作表防護密碼
	 */
	private void applySheetDecorations(Worksheet worksheet, List<CellRange> mergedRegions, SheetProtectionConfig protectionConfig) {
		if (mergedRegions != null && !mergedRegions.isEmpty()) {
			for (CellRange range : mergedRegions) {
				worksheet.range(range.firstRow(), range.firstCol(), range.lastRow(), range.lastCol()).merge();
			}
		}

		if (protectionConfig != null) {
			String pwd = protectionConfig.password() != null ? protectionConfig.password() : "";
			Set<SheetProtectionOption> options = new HashSet<>(SheetProtectionOption.DEFAULT_OPTIONS);
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
	 * 寫入單一儲存格數值，依據物件型別調用對應的 FastExcel value 重載方法
	 */
	private void writeCellValue(Worksheet worksheet, int row, int col, Object val) {
		if (val == null) {
			return;
		}

		if (val instanceof Number num) {
			worksheet.value(row, col, num);
		} else if (val instanceof String str) {
			if (str.startsWith("=") && str.length() > 1) {
				worksheet.formula(row, col, str.substring(1).trim());
			} else {
				worksheet.value(row, col, str);
			}
		} else if (val instanceof Boolean bool) {
			worksheet.value(row, col, bool);
		} else if (val instanceof LocalDate localDate) {
			worksheet.value(row, col, localDate);
		} else if (val instanceof LocalDateTime localDateTime) {
			worksheet.value(row, col, localDateTime);
		} else if (val instanceof ZonedDateTime zonedDateTime) {
			worksheet.value(row, col, zonedDateTime);
		} else if (val instanceof Instant instant) {
			worksheet.value(row, col, instant);
		} else if (val instanceof Date date) {
			worksheet.value(row, col, date);
		} else {
			worksheet.value(row, col, val.toString());
		}
	}
}

