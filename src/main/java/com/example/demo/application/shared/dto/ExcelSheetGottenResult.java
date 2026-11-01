package com.example.demo.application.shared.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Excel 工作表讀取結果物件 (Excel Sheet Gotten Result)
 * <p>
 * 遵循 {@code N + Gotten/Searched + Result} 命名規範 (3-6 規範)，作為純資料載體封裝單一工作表解析之資料。
 * 內部禁止寫入 {@code fromDomain} 等領域轉換邏輯。
 */
public class ExcelSheetGottenResult {

	private final int sheetIndex;
	private final String sheetName;
	private final List<String> headers;
	private final List<List<Object>> rows;
	private final List<List<String>> textRows;

	public ExcelSheetGottenResult(int sheetIndex, String sheetName, List<String> headers,
			List<List<Object>> rows, List<List<String>> textRows) {
		this.sheetIndex = sheetIndex;
		this.sheetName = sheetName != null ? sheetName : "Sheet" + sheetIndex;
		this.headers = headers != null ? Collections.unmodifiableList(new ArrayList<>(headers)) : Collections.emptyList();
		this.rows = rows != null ? Collections.unmodifiableList(new ArrayList<>(rows)) : Collections.emptyList();
		this.textRows = textRows != null ? Collections.unmodifiableList(new ArrayList<>(textRows)) : Collections.emptyList();
	}

	public int getSheetIndex() {
		return sheetIndex;
	}

	public String getSheetName() {
		return sheetName;
	}

	public List<String> getHeaders() {
		return headers;
	}

	public List<List<Object>> getRows() {
		return rows;
	}

	public List<List<String>> getTextRows() {
		return textRows;
	}

	public int getRowCount() {
		return rows.size();
	}

	public boolean isEmpty() {
		return rows.isEmpty();
	}

	/**
	 * 便捷取用：將各資料列依標題名稱映射為鍵值字典集合
	 *
	 * @return 鍵值映射資料列清單 (Key 為 Header 名稱，Value 為儲存格 Object)
	 */
	public List<Map<String, Object>> getDataRowsAsMaps() {
		if (headers.isEmpty() || rows.isEmpty()) {
			return Collections.emptyList();
		}
		List<Map<String, Object>> mappedRows = new ArrayList<>(rows.size());
		for (List<Object> row : rows) {
			Map<String, Object> rowMap = new LinkedHashMap<>();
			for (int i = 0; i < headers.size(); i++) {
				String headerName = headers.get(i);
				Object cellValue = (row != null && i < row.size()) ? row.get(i) : null;
				rowMap.put(headerName, cellValue);
			}
			mappedRows.add(rowMap);
		}
		return Collections.unmodifiableList(mappedRows);
	}
}
