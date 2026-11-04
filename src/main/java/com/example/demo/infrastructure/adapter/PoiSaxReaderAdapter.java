package com.example.demo.infrastructure.adapter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.example.demo.application.port.out.ExcelReaderPort;
import com.example.demo.application.shared.context.ExcelTaskContextHolder;
import com.example.demo.application.shared.dto.ExcelGottenResult;
import com.example.demo.application.shared.dto.ExcelSheetGottenResult;
import com.example.demo.application.shared.exception.ExcelReadException;
import com.example.demo.application.shared.query.ReadExcelQuery;
import com.example.demo.application.shared.stream.ExcelChunkConsumer;
import com.example.demo.application.shared.stream.ExcelRowConsumer;
import com.example.demo.application.shared.stream.ExcelRowContext;
import com.example.demo.application.shared.stream.ExcelStreamListener;

/**
 * Apache POI 讀取適配器 (Apache POI Reader Outbound Adapter)
 * <p>
 * 遵循 Clean Architecture 六角形架構規範，實作應用層 {@link ExcelReaderPort} 介面。
 * 封裝 Apache POI 套件，支援全格式 Excel (.xlsx, .xls) 解析、公式計算、型別轉換與串流監聽讀取服務。
 * 命名規範：以 {@code Adapter} 結尾 (3-9 規範)。
 */
public class PoiSaxReaderAdapter implements ExcelReaderPort {

	@Override
	public ExcelGottenResult read(InputStream inputStream, ReadExcelQuery query) {
		Objects.requireNonNull(inputStream, "InputStream must not be null");
		ReadExcelQuery effectiveQuery = query != null ? query : ReadExcelQuery.defaultQuery();

		try (Workbook workbook = WorkbookFactory.create(inputStream)) {
			DataFormatter dataFormatter = new DataFormatter();
			FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
			List<ExcelSheetGottenResult> sheetResults = new ArrayList<>();

			if (effectiveQuery.getSheetIndex() != null) {
				int idx = effectiveQuery.getSheetIndex();
				if (idx >= 0 && idx < workbook.getNumberOfSheets()) {
					sheetResults.add(parseSheet(workbook.getSheetAt(idx), idx, effectiveQuery, dataFormatter, evaluator));
				}
			} else if (effectiveQuery.getSheetName() != null && !effectiveQuery.getSheetName().isBlank()) {
				Sheet sheet = workbook.getSheet(effectiveQuery.getSheetName());
				if (sheet != null) {
					sheetResults.add(parseSheet(sheet, workbook.getSheetIndex(sheet), effectiveQuery, dataFormatter, evaluator));
				}
			} else {
				for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
					sheetResults.add(parseSheet(workbook.getSheetAt(i), i, effectiveQuery, dataFormatter, evaluator));
				}
			}

			return new ExcelGottenResult(sheetResults);
		} catch (IOException e) {
			throw new ExcelReadException("Failed to read Excel document via Apache POI: " + e.getMessage(), e);
		} catch (Exception e) {
			throw new ExcelReadException("Unexpected error during Apache POI parsing: " + e.getMessage(), e);
		}
	}

	@Override
	public ExcelGottenResult read(byte[] excelBytes, ReadExcelQuery query) {
		Objects.requireNonNull(excelBytes, "excelBytes must not be null");
		try (ByteArrayInputStream bais = new ByteArrayInputStream(excelBytes)) {
			return read(bais, query);
		} catch (IOException e) {
			throw new ExcelReadException("Failed to read Excel from byte array via Apache POI: " + e.getMessage(), e);
		}
	}

	@Override
	public void readStream(InputStream inputStream, ReadExcelQuery query, ExcelRowConsumer rowConsumer) {
		Objects.requireNonNull(rowConsumer, "ExcelRowConsumer must not be null");
		readWithListener(inputStream, query, new ExcelStreamListener() {
			@Override
			public void onRow(List<Object> rowValues, List<String> rowTexts, ExcelRowContext context) {
				rowConsumer.accept(rowValues, rowTexts, context);
			}
		});
	}

	@Override
	public void readStream(byte[] excelBytes, ReadExcelQuery query, ExcelRowConsumer rowConsumer) {
		Objects.requireNonNull(excelBytes, "excelBytes must not be null");
		try (ByteArrayInputStream bais = new ByteArrayInputStream(excelBytes)) {
			readStream(bais, query, rowConsumer);
		} catch (IOException e) {
			throw new ExcelReadException("Failed to stream read Excel from byte array via Apache POI: " + e.getMessage(), e);
		}
	}

	@Override
	public void readChunked(InputStream inputStream, ReadExcelQuery query, int chunkSize, ExcelChunkConsumer chunkConsumer) {
		Objects.requireNonNull(inputStream, "InputStream must not be null");
		Objects.requireNonNull(chunkConsumer, "ExcelChunkConsumer must not be null");
		if (chunkSize <= 0) {
			throw new IllegalArgumentException("chunkSize must be greater than 0");
		}
		ReadExcelQuery effectiveQuery = query != null ? query : ReadExcelQuery.defaultQuery();

		try (Workbook workbook = WorkbookFactory.create(inputStream)) {
			DataFormatter dataFormatter = new DataFormatter();
			FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

			if (effectiveQuery.getSheetIndex() != null) {
				int idx = effectiveQuery.getSheetIndex();
				if (idx >= 0 && idx < workbook.getNumberOfSheets()) {
					streamSheet(workbook.getSheetAt(idx), idx, effectiveQuery, new ExcelStreamListener() {}, chunkSize, chunkConsumer, dataFormatter, evaluator);
				}
			} else if (effectiveQuery.getSheetName() != null && !effectiveQuery.getSheetName().isBlank()) {
				Sheet sheet = workbook.getSheet(effectiveQuery.getSheetName());
				if (sheet != null) {
					streamSheet(sheet, workbook.getSheetIndex(sheet), effectiveQuery, new ExcelStreamListener() {}, chunkSize, chunkConsumer, dataFormatter, evaluator);
				}
			} else {
				for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
					streamSheet(workbook.getSheetAt(i), i, effectiveQuery, new ExcelStreamListener() {}, chunkSize, chunkConsumer, dataFormatter, evaluator);
				}
			}
		} catch (IOException e) {
			throw new ExcelReadException("Failed to chunk read Excel document via Apache POI: " + e.getMessage(), e);
		} catch (Exception e) {
			throw new ExcelReadException("Unexpected error during Apache POI chunk parsing: " + e.getMessage(), e);
		}
	}

	@Override
	public void readChunked(byte[] excelBytes, ReadExcelQuery query, int chunkSize, ExcelChunkConsumer chunkConsumer) {
		Objects.requireNonNull(excelBytes, "excelBytes must not be null");
		try (ByteArrayInputStream bais = new ByteArrayInputStream(excelBytes)) {
			readChunked(bais, query, chunkSize, chunkConsumer);
		} catch (IOException e) {
			throw new ExcelReadException("Failed to chunk read Excel from byte array via Apache POI: " + e.getMessage(), e);
		}
	}

	@Override
	public void readWithListener(InputStream inputStream, ReadExcelQuery query, ExcelStreamListener listener) {
		Objects.requireNonNull(inputStream, "InputStream must not be null");
		Objects.requireNonNull(listener, "ExcelStreamListener must not be null");
		ReadExcelQuery effectiveQuery = query != null ? query : ReadExcelQuery.defaultQuery();

		try (Workbook workbook = WorkbookFactory.create(inputStream)) {
			DataFormatter dataFormatter = new DataFormatter();
			FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

			if (effectiveQuery.getSheetIndex() != null) {
				int idx = effectiveQuery.getSheetIndex();
				if (idx >= 0 && idx < workbook.getNumberOfSheets()) {
					streamSheet(workbook.getSheetAt(idx), idx, effectiveQuery, listener, 0, null, dataFormatter, evaluator);
				}
			} else if (effectiveQuery.getSheetName() != null && !effectiveQuery.getSheetName().isBlank()) {
				Sheet sheet = workbook.getSheet(effectiveQuery.getSheetName());
				if (sheet != null) {
					streamSheet(sheet, workbook.getSheetIndex(sheet), effectiveQuery, listener, 0, null, dataFormatter, evaluator);
				}
			} else {
				for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
					streamSheet(workbook.getSheetAt(i), i, effectiveQuery, listener, 0, null, dataFormatter, evaluator);
				}
			}
		} catch (IOException e) {
			ExcelRowContext errContext = new ExcelRowContext(0, "", 0, Collections.emptyList(), false, 0);
			listener.onError(e, errContext);
			throw new ExcelReadException("Failed to stream read Excel document via Apache POI: " + e.getMessage(), e);
		} catch (Exception e) {
			ExcelRowContext errContext = new ExcelRowContext(0, "", 0, Collections.emptyList(), false, 0);
			listener.onError(e, errContext);
			throw new ExcelReadException("Unexpected error during Apache POI stream parsing: " + e.getMessage(), e);
		}
	}

	@Override
	public void readWithListener(byte[] excelBytes, ReadExcelQuery query, ExcelStreamListener listener) {
		Objects.requireNonNull(excelBytes, "excelBytes must not be null");
		try (ByteArrayInputStream bais = new ByteArrayInputStream(excelBytes)) {
			readWithListener(bais, query, listener);
		} catch (IOException e) {
			throw new ExcelReadException("Failed to read Excel with listener from byte array via Apache POI: " + e.getMessage(), e);
		}
	}

	/**
	 * 逐行迭代工作表以驅動生命週期監聽器與批次回呼
	 */
	private void streamSheet(Sheet sheet, int sheetIndex, ReadExcelQuery query, ExcelStreamListener listener,
	                         int chunkSize, ExcelChunkConsumer chunkConsumer, DataFormatter dataFormatter, FormulaEvaluator evaluator) {
		List<String> headers = new ArrayList<>();
		int headerRowIndex = query.getHeaderRowIndex();
		int lastRowNum = sheet.getLastRowNum();
		int startDataRow = query.isHasHeader() ? Math.max(query.getStartDataRowIndex(), headerRowIndex + 1) : 0;
		long totalDataRowsRead = 0;

		List<Map<String, Object>> chunkBuffer = (chunkSize > 0 || chunkConsumer != null) ? new ArrayList<>(Math.max(chunkSize, 100)) : null;
		ExcelRowContext lastContext = null;

		for (int r = 0; r <= lastRowNum; r++) {
			Row row = sheet.getRow(r);

			// 1. 處理標題列
			if (query.isHasHeader() && r == headerRowIndex) {
				if (row != null) {
					short lastCell = row.getLastCellNum();
					for (int c = 0; c < lastCell; c++) {
						Cell cell = row.getCell(c);
						String text = cell != null ? dataFormatter.formatCellValue(cell, evaluator) : "";
						if (query.isTrimWhitespace() && text != null) {
							text = text.trim();
						}
						headers.add(text != null ? text : "");
					}
				}
				ExcelRowContext headerContext = new ExcelRowContext(sheetIndex, sheet.getSheetName(), r, headers, true, 0);
				listener.onHeader(headers, headerContext);
				continue;
			}

			// 2. 跳過非資料列
			if (r < startDataRow) {
				continue;
			}

			// 3. 處理資料列
			totalDataRowsRead++;
			int rowCells = row != null ? row.getLastCellNum() : 0;
			int effectiveCols = Math.max(headers.size(), Math.max(0, rowCells));

			List<Object> rowValues = new ArrayList<>(effectiveCols);
			List<String> rowTexts = new ArrayList<>(effectiveCols);

			for (int c = 0; c < effectiveCols; c++) {
				Cell cell = row != null ? row.getCell(c) : null;
				rowValues.add(extractCellValue(cell, evaluator, query.isTrimWhitespace()));
				rowTexts.add(extractCellText(cell, dataFormatter, evaluator, query.isTrimWhitespace()));
			}

			lastContext = new ExcelRowContext(sheetIndex, sheet.getSheetName(), r, headers, false, totalDataRowsRead);

			// 逐列通知
			listener.onRow(rowValues, rowTexts, lastContext);

			// 分塊收集與回呼
			if (chunkBuffer != null) {
				Map<String, Object> rowMap = new LinkedHashMap<>(headers.size());
				for (int i = 0; i < headers.size(); i++) {
					String h = headers.get(i);
					Object val = i < rowValues.size() ? rowValues.get(i) : null;
					rowMap.put(h, val);
				}
				chunkBuffer.add(rowMap);

				if (chunkBuffer.size() >= chunkSize) {
					if (ExcelTaskContextHolder.isActive()) {
						ExcelTaskContextHolder.incrementProgress(chunkBuffer.size());
					}
					if (chunkConsumer != null) {
						chunkConsumer.accept(Collections.unmodifiableList(new ArrayList<>(chunkBuffer)), lastContext);
					}
					listener.onChunk(Collections.unmodifiableList(new ArrayList<>(chunkBuffer)), lastContext);
					chunkBuffer.clear();
				}
			}

			// 檢查中斷或筆數上限
			if (lastContext.isStopped()) {
				break;
			}
			if (query.getMaxRows() != null && totalDataRowsRead >= query.getMaxRows()) {
				break;
			}
		}

		// 處理剩餘分塊緩衝
		if (chunkBuffer != null && !chunkBuffer.isEmpty()) {
			if (lastContext == null) {
				lastContext = new ExcelRowContext(sheetIndex, sheet.getSheetName(), 0, headers, false, totalDataRowsRead);
			}
			List<Map<String, Object>> remainingChunk = Collections.unmodifiableList(new ArrayList<>(chunkBuffer));
			if (ExcelTaskContextHolder.isActive()) {
				ExcelTaskContextHolder.incrementProgress(remainingChunk.size());
			}
			if (chunkConsumer != null) {
				chunkConsumer.accept(remainingChunk, lastContext);
			}
			listener.onChunk(remainingChunk, lastContext);
			chunkBuffer.clear();
		}

		if (lastContext == null) {
			lastContext = new ExcelRowContext(sheetIndex, sheet.getSheetName(), 0, headers, false, totalDataRowsRead);
		}
		listener.onComplete(lastContext);
	}

	/**
	 * 解析單一工作表內容 (In-Memory)
	 */
	private ExcelSheetGottenResult parseSheet(Sheet sheet, int sheetIndex, ReadExcelQuery query,
	                                          DataFormatter dataFormatter, FormulaEvaluator evaluator) {
		List<String> headers = new ArrayList<>();
		int headerRowIndex = query.getHeaderRowIndex();
		int lastRowNum = sheet.getLastRowNum();

		if (query.isHasHeader() && headerRowIndex >= 0 && headerRowIndex <= lastRowNum) {
			Row headerRow = sheet.getRow(headerRowIndex);
			if (headerRow != null) {
				short lastCell = headerRow.getLastCellNum();
				for (int c = 0; c < lastCell; c++) {
					Cell cell = headerRow.getCell(c);
					String text = cell != null ? dataFormatter.formatCellValue(cell, evaluator) : "";
					if (query.isTrimWhitespace() && text != null) {
						text = text.trim();
					}
					headers.add(text != null ? text : "");
				}
			}
		}

		int startRow = query.isHasHeader() ? Math.max(query.getStartDataRowIndex(), headerRowIndex + 1) : 0;
		int maxCols = headers.size();

		List<List<Object>> dataRows = new ArrayList<>();
		List<List<String>> textRows = new ArrayList<>();

		for (int r = startRow; r <= lastRowNum; r++) {
			if (query.getMaxRows() != null && dataRows.size() >= query.getMaxRows()) {
				break;
			}
			Row row = sheet.getRow(r);
			int rowCells = row != null ? row.getLastCellNum() : 0;
			int effectiveCols = Math.max(maxCols, Math.max(0, rowCells));

			List<Object> rowValues = new ArrayList<>(effectiveCols);
			List<String> rowTexts = new ArrayList<>(effectiveCols);

			for (int c = 0; c < effectiveCols; c++) {
				Cell cell = row != null ? row.getCell(c) : null;
				rowValues.add(extractCellValue(cell, evaluator, query.isTrimWhitespace()));
				rowTexts.add(extractCellText(cell, dataFormatter, evaluator, query.isTrimWhitespace()));
			}

			dataRows.add(Collections.unmodifiableList(rowValues));
			textRows.add(Collections.unmodifiableList(rowTexts));
		}

		return new ExcelSheetGottenResult(sheetIndex, sheet.getSheetName(), headers, dataRows, textRows);
	}

	/**
	 * 擷取儲存格原生數值 (Number, String, Boolean, LocalDateTime 等)
	 */
	private Object extractCellValue(Cell cell, FormulaEvaluator evaluator, boolean trimWhitespace) {
		if (cell == null || cell.getCellType() == CellType.BLANK) {
			return null;
		}

		CellType type = cell.getCellType();
		if (type == CellType.FORMULA && evaluator != null) {
			try {
				type = evaluator.evaluateFormulaCell(cell);
			} catch (Exception ignored) {
				// 公式求值失敗時回退為原公式文字或快取字串
			}
		}

		return switch (type) {
			case NUMERIC -> {
				if (DateUtil.isCellDateFormatted(cell)) {
					yield cell.getLocalDateTimeCellValue();
				}
				double d = cell.getNumericCellValue();
				if (d == Math.floor(d) && !Double.isInfinite(d)) {
					yield (long) d;
				}
				yield d;
			}
			case STRING -> {
				String str = cell.getStringCellValue();
				yield (trimWhitespace && str != null) ? str.trim() : str;
			}
			case BOOLEAN -> cell.getBooleanCellValue();
			case ERROR -> cell.getErrorCellValue();
			default -> null;
		};
	}

	/**
	 * 擷取儲存格格式化後的文字字串
	 */
	private String extractCellText(Cell cell, DataFormatter dataFormatter, FormulaEvaluator evaluator, boolean trimWhitespace) {
		if (cell == null || cell.getCellType() == CellType.BLANK) {
			return "";
		}
		String text = dataFormatter.formatCellValue(cell, evaluator);
		if (text == null) {
			return "";
		}
		return trimWhitespace ? text.trim() : text;
	}
}

