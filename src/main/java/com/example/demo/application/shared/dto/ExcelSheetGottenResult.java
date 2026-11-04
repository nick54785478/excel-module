package com.example.demo.application.shared.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Excel 工作表讀取結果物件 (Excel Sheet Gotten Result DTO)
 * <p>
 * 遵循 AGENTS.md 3-6 DTO 命名規範 ({@code N + Gotten/Searched + Result})，採用 Java Record 作為應用層不可變純資料載體。
 * 封裝單一工作表之索引、名稱、標題欄位、原始物件列與純文字資料列。
 * 內部嚴格遵守單一職責，禁止在 DTO 內部寫入領域轉換邏輯。
 *
 * @param sheetIndex 工作表索引 (0-based)
 * @param sheetName  工作表名稱
 * @param headers    標題欄位清單 (不可變清單)
 * @param rows       二維儲存格物件資料列 (不可變清單)
 * @param textRows   二維純文字格式化資料列 (不可變清單)
 */
public record ExcelSheetGottenResult(
		int sheetIndex,
		String sheetName,
		List<String> headers,
		List<List<Object>> rows,
		List<List<String>> textRows
) {

	/**
	 * 緊湊建構子 (Compact Constructor)，執行名稱預設補正與所有巢狀集合不可變防禦
	 */
	public ExcelSheetGottenResult {
		sheetName = sheetName != null && !sheetName.isBlank() ? sheetName : "Sheet" + sheetIndex;
		headers = headers != null ? Collections.unmodifiableList(new ArrayList<>(headers)) : Collections.emptyList();
		rows = rows != null ? Collections.unmodifiableList(new ArrayList<>(rows)) : Collections.emptyList();
		textRows = textRows != null ? Collections.unmodifiableList(new ArrayList<>(textRows)) : Collections.emptyList();
	}

	/**
	 * 建立空工作表讀取結果物件
	 *
	 * @return 空工作表讀取結果
	 */
	public static ExcelSheetGottenResult empty() {
		return empty(0, "Sheet1");
	}

	/**
	 * 建立指定工作表索引與名稱之空結果物件
	 *
	 * @param sheetIndex 工作表索引
	 * @param sheetName  工作表名稱
	 * @return 空工作表讀取結果
	 */
	public static ExcelSheetGottenResult empty(int sheetIndex, String sheetName) {
		return new ExcelSheetGottenResult(sheetIndex, sheetName, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
	}

	// ==================== 向後相容別名方法 (Backward Compatible Accessors) ====================

	public int getSheetIndex() {
		return sheetIndex();
	}

	public String getSheetName() {
		return sheetName();
	}

	public List<String> getHeaders() {
		return headers();
	}

	public List<List<Object>> getRows() {
		return rows();
	}

	public List<List<String>> getTextRows() {
		return textRows();
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
