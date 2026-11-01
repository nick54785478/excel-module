package com.example.demo.infrastructure.adapter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.dhatim.fastexcel.reader.Cell;
import org.dhatim.fastexcel.reader.CellType;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;
import org.dhatim.fastexcel.reader.Sheet;
import com.example.demo.application.port.out.ExcelReaderPort;
import com.example.demo.application.shared.dto.ExcelGottenResult;
import com.example.demo.application.shared.dto.ExcelSheetGottenResult;
import com.example.demo.application.shared.exception.ExcelReadException;
import com.example.demo.application.shared.query.ReadExcelQuery;
import com.example.demo.application.shared.stream.ExcelChunkConsumer;
import com.example.demo.application.shared.stream.ExcelRowConsumer;
import com.example.demo.application.shared.stream.ExcelRowContext;
import com.example.demo.application.shared.stream.ExcelStreamListener;

/**
 * FastExcel 讀取適配器 (FastExcel Reader Outbound Adapter)
 * <p>
 * 遵循 Clean Architecture 六角形架構規範，實作應用層 {@link ExcelReaderPort} 介面。
 * 封裝第三方高效能套件 {@code org.dhatim:fastexcel-reader}，提供低延遲、串流式 Excel (.xlsx) 解析與資料擷取服務。
 * 命名規範：以 {@code Adapter} 結尾 (3-9 規範)。
 */
public class FastExcelReaderAdapter implements ExcelReaderPort {

	@Override
	public ExcelGottenResult read(InputStream inputStream, ReadExcelQuery query) {
		Objects.requireNonNull(inputStream, "InputStream must not be null");
		ReadExcelQuery effectiveQuery = query != null ? query : ReadExcelQuery.defaultQuery();

		try (ReadableWorkbook workbook = new ReadableWorkbook(inputStream)) {
			List<ExcelSheetGottenResult> sheetResults = new ArrayList<>();

			if (effectiveQuery.getSheetIndex() != null) {
				// 指定讀取特定索引的工作表
				Optional<Sheet> targetSheet = workbook.getSheet(effectiveQuery.getSheetIndex());
				targetSheet.ifPresent(sheet -> sheetResults.add(parseSheet(sheet, effectiveQuery)));
			} else if (effectiveQuery.getSheetName() != null && !effectiveQuery.getSheetName().isBlank()) {
				// 指定讀取特定名稱的工作表
				Optional<Sheet> targetSheet = workbook.findSheet(effectiveQuery.getSheetName());
				targetSheet.ifPresent(sheet -> sheetResults.add(parseSheet(sheet, effectiveQuery)));
			} else {
				// 讀取活頁簿中的所有工作表
				workbook.getSheets().forEach(sheet -> sheetResults.add(parseSheet(sheet, effectiveQuery)));
			}

			return new ExcelGottenResult(sheetResults);
		} catch (IOException e) {
			throw new ExcelReadException("Failed to read Excel document via FastExcel: " + e.getMessage(), e);
		} catch (Exception e) {
			throw new ExcelReadException("Unexpected error during FastExcel parsing: " + e.getMessage(), e);
		}
	}

	@Override
	public ExcelGottenResult read(byte[] excelBytes, ReadExcelQuery query) {
		Objects.requireNonNull(excelBytes, "excelBytes must not be null");
		try (ByteArrayInputStream bais = new ByteArrayInputStream(excelBytes)) {
			return read(bais, query);
		} catch (IOException e) {
			throw new ExcelReadException("Failed to read Excel from byte array: " + e.getMessage(), e);
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
			throw new ExcelReadException("Failed to stream read Excel from byte array: " + e.getMessage(), e);
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

		try (ReadableWorkbook workbook = new ReadableWorkbook(inputStream)) {
			if (effectiveQuery.getSheetIndex() != null) {
				Optional<Sheet> targetSheet = workbook.getSheet(effectiveQuery.getSheetIndex());
				targetSheet.ifPresent(sheet -> streamSheet(sheet, effectiveQuery, new ExcelStreamListener() {}, chunkSize, chunkConsumer));
			} else if (effectiveQuery.getSheetName() != null && !effectiveQuery.getSheetName().isBlank()) {
				Optional<Sheet> targetSheet = workbook.findSheet(effectiveQuery.getSheetName());
				targetSheet.ifPresent(sheet -> streamSheet(sheet, effectiveQuery, new ExcelStreamListener() {}, chunkSize, chunkConsumer));
			} else {
				workbook.getSheets().forEach(sheet -> streamSheet(sheet, effectiveQuery, new ExcelStreamListener() {}, chunkSize, chunkConsumer));
			}
		} catch (IOException e) {
			throw new ExcelReadException("Failed to chunk read Excel document via FastExcel: " + e.getMessage(), e);
		} catch (Exception e) {
			throw new ExcelReadException("Unexpected error during FastExcel chunk parsing: " + e.getMessage(), e);
		}
	}

	@Override
	public void readChunked(byte[] excelBytes, ReadExcelQuery query, int chunkSize, ExcelChunkConsumer chunkConsumer) {
		Objects.requireNonNull(excelBytes, "excelBytes must not be null");
		try (ByteArrayInputStream bais = new ByteArrayInputStream(excelBytes)) {
			readChunked(bais, query, chunkSize, chunkConsumer);
		} catch (IOException e) {
			throw new ExcelReadException("Failed to chunk read Excel from byte array: " + e.getMessage(), e);
		}
	}

	@Override
	public void readWithListener(InputStream inputStream, ReadExcelQuery query, ExcelStreamListener listener) {
		Objects.requireNonNull(inputStream, "InputStream must not be null");
		Objects.requireNonNull(listener, "ExcelStreamListener must not be null");
		ReadExcelQuery effectiveQuery = query != null ? query : ReadExcelQuery.defaultQuery();

		try (ReadableWorkbook workbook = new ReadableWorkbook(inputStream)) {
			if (effectiveQuery.getSheetIndex() != null) {
				Optional<Sheet> targetSheet = workbook.getSheet(effectiveQuery.getSheetIndex());
				targetSheet.ifPresent(sheet -> streamSheet(sheet, effectiveQuery, listener, 0, null));
			} else if (effectiveQuery.getSheetName() != null && !effectiveQuery.getSheetName().isBlank()) {
				Optional<Sheet> targetSheet = workbook.findSheet(effectiveQuery.getSheetName());
				targetSheet.ifPresent(sheet -> streamSheet(sheet, effectiveQuery, listener, 0, null));
			} else {
				workbook.getSheets().forEach(sheet -> streamSheet(sheet, effectiveQuery, listener, 0, null));
			}
		} catch (IOException e) {
			ExcelRowContext errContext = new ExcelRowContext(0, "", 0, Collections.emptyList(), false, 0);
			listener.onError(e, errContext);
			throw new ExcelReadException("Failed to stream read Excel document via FastExcel: " + e.getMessage(), e);
		} catch (Exception e) {
			ExcelRowContext errContext = new ExcelRowContext(0, "", 0, Collections.emptyList(), false, 0);
			listener.onError(e, errContext);
			throw new ExcelReadException("Unexpected error during FastExcel stream parsing: " + e.getMessage(), e);
		}
	}

	@Override
	public void readWithListener(byte[] excelBytes, ReadExcelQuery query, ExcelStreamListener listener) {
		Objects.requireNonNull(excelBytes, "excelBytes must not be null");
		try (ByteArrayInputStream bais = new ByteArrayInputStream(excelBytes)) {
			readWithListener(bais, query, listener);
		} catch (IOException e) {
			throw new ExcelReadException("Failed to read Excel with listener from byte array: " + e.getMessage(), e);
		}
	}

	/**
	 * 使用 FastExcel StAX 串流以 O(1) 記憶體讀取並驅動監聽器與分塊回呼
	 */
	private void streamSheet(Sheet sheet, ReadExcelQuery query, ExcelStreamListener listener, int chunkSize, ExcelChunkConsumer chunkConsumer) {
		try (var rowStream = sheet.openStream()) {
			Iterator<Row> rowIterator = rowStream.iterator();
			List<String> headers = new ArrayList<>();
			int headerRowIndex = query.getHeaderRowIndex();
			int startDataRow = query.isHasHeader() ? Math.max(query.getStartDataRowIndex(), headerRowIndex + 1) : 0;
			int currentRowIndex = 0;
			long totalDataRowsRead = 0;

			List<Map<String, Object>> chunkBuffer = (chunkSize > 0 || chunkConsumer != null) ? new ArrayList<>(Math.max(chunkSize, 100)) : null;
			ExcelRowContext lastContext = null;

			while (rowIterator.hasNext()) {
				Row row = rowIterator.next();

				// 1. 處理標題列
				if (query.isHasHeader() && currentRowIndex == headerRowIndex) {
					int cellCount = row.getCellCount();
					for (int c = 0; c < cellCount; c++) {
						String text = row.getCellText(c);
						if (query.isTrimWhitespace() && text != null) {
							text = text.trim();
						}
						headers.add(text != null ? text : "");
					}
					ExcelRowContext headerContext = new ExcelRowContext(sheet.getIndex(), sheet.getName(), currentRowIndex, headers, true, 0);
					listener.onHeader(headers, headerContext);
					currentRowIndex++;
					continue;
				}

				// 2. 跳過非資料列
				if (currentRowIndex < startDataRow) {
					currentRowIndex++;
					continue;
				}

				// 3. 處理資料列
				totalDataRowsRead++;
				int effectiveCols = Math.max(headers.size(), row.getCellCount());
				List<Object> rowValues = new ArrayList<>(effectiveCols);
				List<String> rowTexts = new ArrayList<>(effectiveCols);

				for (int c = 0; c < effectiveCols; c++) {
					Cell cell = c < row.getCellCount() ? row.getCell(c) : null;
					rowValues.add(extractCellValue(cell, query.isTrimWhitespace()));
					rowTexts.add(extractCellText(cell, query.isTrimWhitespace()));
				}

				lastContext = new ExcelRowContext(sheet.getIndex(), sheet.getName(), currentRowIndex, headers, false, totalDataRowsRead);

				// 逐列通知 listener
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
						if (chunkConsumer != null) {
							chunkConsumer.accept(Collections.unmodifiableList(new ArrayList<>(chunkBuffer)), lastContext);
						}
						listener.onChunk(Collections.unmodifiableList(new ArrayList<>(chunkBuffer)), lastContext);
						chunkBuffer.clear();
					}
				}

				currentRowIndex++;

				// 檢查中斷或筆數上限
				if (lastContext.isStopped()) {
					break;
				}
				if (query.getMaxRows() != null && totalDataRowsRead >= query.getMaxRows()) {
					break;
				}
			}

			// 處理分塊緩衝區剩餘資料
			if (chunkBuffer != null && !chunkBuffer.isEmpty()) {
				if (lastContext == null) {
					lastContext = new ExcelRowContext(sheet.getIndex(), sheet.getName(), currentRowIndex, headers, false, totalDataRowsRead);
				}
				if (chunkConsumer != null) {
					chunkConsumer.accept(Collections.unmodifiableList(chunkBuffer), lastContext);
				}
				listener.onChunk(Collections.unmodifiableList(chunkBuffer), lastContext);
				chunkBuffer.clear();
			}

			if (lastContext == null) {
				lastContext = new ExcelRowContext(sheet.getIndex(), sheet.getName(), currentRowIndex, headers, false, totalDataRowsRead);
			}
			listener.onComplete(lastContext);

		} catch (IOException e) {
			ExcelRowContext errContext = new ExcelRowContext(sheet.getIndex(), sheet.getName(), 0, Collections.emptyList(), false, 0);
			listener.onError(e, errContext);
			throw new ExcelReadException("Failed to stream read sheet [" + sheet.getName() + "]: " + e.getMessage(), e);
		}
	}

	/**
	 * 解析單一工作表內容
	 *
	 * @param sheet 工作表物件
	 * @param query 讀取條件
	 * @return 工作表解析結果 DTO
	 */
	private ExcelSheetGottenResult parseSheet(Sheet sheet, ReadExcelQuery query) {
		try {
			List<Row> rows = sheet.read();
			if (rows == null || rows.isEmpty()) {
				return new ExcelSheetGottenResult(sheet.getIndex(), sheet.getName(), Collections.emptyList(),
						Collections.emptyList(), Collections.emptyList());
			}

			// 1. 解析標題列 (Headers)
			List<String> headers = new ArrayList<>();
			int headerRowIndex = query.getHeaderRowIndex();
			if (query.isHasHeader() && headerRowIndex >= 0 && headerRowIndex < rows.size()) {
				Row headerRow = rows.get(headerRowIndex);
				int cellCount = headerRow.getCellCount();
				for (int col = 0; col < cellCount; col++) {
					String headerText = headerRow.getCellText(col);
					if (query.isTrimWhitespace() && headerText != null) {
						headerText = headerText.trim();
					}
					headers.add(headerText != null ? headerText : "");
				}
			}

			// 2. 解析資料列 (Data Rows)
			int startRow = query.isHasHeader() ? Math.max(query.getStartDataRowIndex(), headerRowIndex + 1) : 0;
			int maxColumns = headers.size();

			List<List<Object>> dataRows = new ArrayList<>();
			List<List<String>> textRows = new ArrayList<>();

			for (int r = startRow; r < rows.size(); r++) {
				if (query.getMaxRows() != null && dataRows.size() >= query.getMaxRows()) {
					break;
				}

				Row row = rows.get(r);
				int rowCells = row.getCellCount();
				int effectiveColCount = Math.max(maxColumns, rowCells);

				List<Object> rowValues = new ArrayList<>(effectiveColCount);
				List<String> rowTexts = new ArrayList<>(effectiveColCount);

				for (int c = 0; c < effectiveColCount; c++) {
					Cell cell = c < rowCells ? row.getCell(c) : null;
					Object cellValue = extractCellValue(cell, query.isTrimWhitespace());
					String cellText = extractCellText(cell, query.isTrimWhitespace());

					rowValues.add(cellValue);
					rowTexts.add(cellText);
				}

				dataRows.add(Collections.unmodifiableList(rowValues));
				textRows.add(Collections.unmodifiableList(rowTexts));
			}

			return new ExcelSheetGottenResult(sheet.getIndex(), sheet.getName(), headers, dataRows, textRows);
		} catch (IOException e) {
			throw new ExcelReadException("Failed to parse sheet [" + sheet.getName() + "]: " + e.getMessage(), e);
		}
	}

	/**
	 * 擷取儲存格原生數值物件 (Number, String, Boolean, LocalDateTime 等)
	 */
	private Object extractCellValue(Cell cell, boolean trimWhitespace) {
		if (cell == null || cell.getType() == CellType.EMPTY) {
			return null;
		}

		Object val = cell.getValue();
		if (val instanceof String str && trimWhitespace) {
			return str.trim();
		}
		return val;
	}

	/**
	 * 擷取儲存格格式化後的文字字串
	 */
	private String extractCellText(Cell cell, boolean trimWhitespace) {
		if (cell == null || cell.getType() == CellType.EMPTY) {
			return "";
		}

		String text = cell.getText();
		if (text == null) {
			return "";
		}
		return trimWhitespace ? text.trim() : text;
	}
}
