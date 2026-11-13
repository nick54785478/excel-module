package com.example.demo.infrastructure.adapter.core;

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

import org.dhatim.fastexcel.SheetProtectionOption;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;

import com.example.demo.application.port.out.core.ExcelWriterPort;
import com.example.demo.application.shared.command.WriteExcelCommand;
import com.example.demo.application.shared.command.WriteSheetCommand;
import com.example.demo.application.shared.command.WriteStreamExcelCommand;
import com.example.demo.application.shared.command.WriteStreamSheetCommand;
import com.example.demo.application.shared.context.ExcelTaskContextHolder;
import com.example.demo.application.shared.exception.ExcelWriteException;
import com.example.demo.application.shared.model.CellCoordinate;
import com.example.demo.application.shared.model.CellRange;
import com.example.demo.application.shared.model.SheetProtectionConfig;
import com.example.demo.infrastructure.engine.ExcelStreamWriterEngine;
import com.example.demo.infrastructure.engine.fastexcel.FastExcelStreamWriterEngine;

/**
 * FastExcel 寫入適配器 (FastExcel Writer Outbound Adapter)
 * <p>
 * 遵循 Clean Architecture 六角形架構規範，實作應用層 {@link ExcelWriterPort} 介面。
 * 封裝第三方高效能串流套件 {@code org.dhatim:fastexcel}，提供低記憶體佔用、極速的 Excel (.xlsx) 檔案生成服務。
 * 底層大數據串流輸出協調共用之 {@link ExcelStreamWriterEngine}。
 * 命名規範：以 {@code Adapter} 結尾 (3-9 規範)。
 */
public class FastExcelWriterAdapter implements ExcelWriterPort {

	private final ExcelStreamWriterEngine streamWriterEngine;

	public FastExcelWriterAdapter() {
		this(new FastExcelStreamWriterEngine());
	}

	public FastExcelWriterAdapter(ExcelStreamWriterEngine streamWriterEngine) {
		this.streamWriterEngine = Objects.requireNonNull(streamWriterEngine, "ExcelStreamWriterEngine must not be null");
	}

	@Override
	public void write(OutputStream outputStream, WriteExcelCommand command) {
		Objects.requireNonNull(outputStream, "OutputStream must not be null");
		Objects.requireNonNull(command, "WriteExcelCommand must not be null");

		try (Workbook workbook = new Workbook(outputStream, command.getApplicationName(), command.getApplicationVersion())) {
			List<WriteSheetCommand> sheets = command.getSheets();
			if (sheets.isEmpty()) {
				// Excel 活頁簿規範至少需具備一個工作表
				workbook.newWorksheet("Sheet1");
			} else {
				for (WriteSheetCommand sheetCommand : sheets) {
					writeWorksheet(workbook, sheetCommand);
				}
			}
			workbook.finish();
		} catch (IOException e) {
			throw new ExcelWriteException("Failed to write Excel document via FastExcel: " + e.getMessage(), e);
		} catch (Exception e) {
			throw new ExcelWriteException("Unexpected error during FastExcel generation: " + e.getMessage(), e);
		}
	}

	@Override
	public byte[] write(WriteExcelCommand command) {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			write(baos, command);
			return baos.toByteArray();
		} catch (IOException e) {
			throw new ExcelWriteException("Failed to write Excel document to byte array: " + e.getMessage(), e);
		}
	}

	/**
	 * 串流寫入巨量資料活頁簿至指定的 OutputStream (支援長任務上下文自動推進)
	 * <p>
	 * 遵循策略 B (顯式業務宣告模式)：本底層寫入適配器保持純粹與零開銷，不預設標註任務追蹤；
	 * 當外層業務服務或控制層標註任務註解並啟用 {@link ExcelTaskContextHolder} 上下文時，
	 * 串流迭代沖刷過程將透明推進已處理列數。
	 *
	 * @param outputStream 輸出串流
	 * @param command      大數據串流寫入命令
	 */
	@Override
	public void writeStream(OutputStream outputStream, WriteStreamExcelCommand command) {
		Objects.requireNonNull(outputStream, "OutputStream must not be null");
		Objects.requireNonNull(command, "WriteStreamExcelCommand must not be null");
		streamWriterEngine.writeStream(outputStream, command);
	}

	/**
	 * 串流寫入巨量資料活頁簿並產出 byte 陣列 (支援長任務上下文自動推進)
	 *
	 * @param command 大數據串流寫入命令
	 * @return Excel 二進位檔案 byte 陣列
	 */
	@Override
	public byte[] writeStream(WriteStreamExcelCommand command) {
		Objects.requireNonNull(command, "WriteStreamExcelCommand must not be null");
		return streamWriterEngine.writeStream(command);
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

		// 5. 套用維度五企業級功能：合併儲存格與工作表防護
		applySheetDecorations(worksheet, sheetCommand.mergedRegions(), sheetCommand.protectionConfig());

		// 串流結束後結束本工作表寫入並關閉 ZIP Entry，確保後續工作表串流不衝突
		worksheet.finish();
		if (ExcelTaskContextHolder.isActive()) {
			ExcelTaskContextHolder.updateProgress(currentRowIndex - startRow, ExcelTaskContextHolder.getTotalRows());
		}
	}

	/**
	 * 寫入單一工作表資料與設定
	 *
	 * @param workbook     FastExcel 活頁簿實例
	 * @param sheetCommand 工作表寫入命令
	 */
	private void writeWorksheet(Workbook workbook, WriteSheetCommand sheetCommand) {
		Worksheet worksheet = workbook.newWorksheet(sheetCommand.getSheetName());
		List<String> headers = sheetCommand.getHeaders();
		List<List<Object>> rows = sheetCommand.getRows();

		// 1. 設定欄寬 (Column Widths)
		configureColumnWidths(worksheet, sheetCommand, headers);

		// 2. 寫入標題列 (Headers)
		if (!headers.isEmpty()) {
			writeHeaders(worksheet, sheetCommand, headers);
		}

		// 3. 凍結窗格 (Freeze Panes)
		if (sheetCommand.isFreezeHeader() && !headers.isEmpty()) {
			worksheet.freezePane(0, 1);
		}

		// 4. 寫入資料列 (Data Rows)
		int startRow = headers.isEmpty() ? 0 : 1;
		if (rows != null) {
			for (int r = 0; r < rows.size(); r++) {
				List<Object> row = rows.get(r);
				if (row == null) {
					continue;
				}
				int rowIndex = startRow + r;
				for (int c = 0; c < row.size(); c++) {
					String cellRef = CellCoordinate.format(c, rowIndex);
					String formula = sheetCommand.cellFormulas().get(cellRef);
					if (formula != null && !formula.isBlank()) {
						String cleanFormula = formula.startsWith("=") ? formula.substring(1).trim() : formula.trim();
						worksheet.formula(rowIndex, c, cleanFormula);
					} else {
						writeCellValue(worksheet, rowIndex, c, row.get(c));
					}
				}
			}
		}

		// 5. 套用未被 rows 包含的額外儲存格公式 (如彙總列)
		if (sheetCommand.cellFormulas() != null && !sheetCommand.cellFormulas().isEmpty()) {
			sheetCommand.cellFormulas().forEach((cellRef, formula) -> {
				if (cellRef != null && formula != null && !formula.isBlank() && CellCoordinate.isValid(cellRef)) {
					CellCoordinate coord = CellCoordinate.parse(cellRef);
					String cleanFormula = formula.startsWith("=") ? formula.substring(1).trim() : formula.trim();
					worksheet.formula(coord.row(), coord.col(), cleanFormula);
				}
			});
		}

		// 6. 套用維度五企業級功能：合併儲存格與工作表防護
		applySheetDecorations(worksheet, sheetCommand.mergedRegions(), sheetCommand.protectionConfig());
	}

	/**
	 * 套用工作表進階裝飾：儲存格合併區間與工作表防護密碼
	 */
	private void applySheetDecorations(Worksheet worksheet, List<CellRange> mergedRegions, SheetProtectionConfig protectionConfig) {
		// 1. 合併儲存格區間
		if (mergedRegions != null && !mergedRegions.isEmpty()) {
			for (CellRange range : mergedRegions) {
				worksheet.range(range.firstRow(), range.firstCol(), range.lastRow(), range.lastCol()).merge();
			}
		}

		// 2. 工作表安全防護
		if (protectionConfig != null) {
			String pwd = protectionConfig.password() != null ? protectionConfig.password() : "";
			java.util.Set<SheetProtectionOption> options = new java.util.HashSet<>(SheetProtectionOption.DEFAULT_OPTIONS);
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
	 * 設定工作表欄位寬度
	 */
	private void configureColumnWidths(Worksheet worksheet, WriteSheetCommand sheetCommand, List<String> headers) {
		List<Double> customWidths = sheetCommand.getColumnWidths();
		if (customWidths != null && !customWidths.isEmpty()) {
			for (int col = 0; col < customWidths.size(); col++) {
				Double width = customWidths.get(col);
				if (width != null && width > 0) {
					worksheet.width(col, width);
				}
			}
		} else if (!headers.isEmpty()) {
			// 根據標題長度預估基礎欄寬
			for (int col = 0; col < headers.size(); col++) {
				String header = headers.get(col);
				double estimatedWidth = Math.max(14.0, (header != null ? header.length() * 2.2 : 14.0));
				worksheet.width(col, estimatedWidth);
			}
		}
	}

	/**
	 * 寫入工作表標題列與樣式
	 */
	private void writeHeaders(Worksheet worksheet, WriteSheetCommand sheetCommand, List<String> headers) {
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

	/**
	 * 寫入單一儲存格數值，依據物件型別調用對應的 FastExcel value 重載方法
	 *
	 * @param worksheet 工作表
	 * @param row       列索引 (0-based)
	 * @param col       行索引 (0-based)
	 * @param val       儲存格數值物件
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
