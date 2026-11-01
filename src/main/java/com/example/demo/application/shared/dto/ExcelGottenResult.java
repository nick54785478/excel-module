package com.example.demo.application.shared.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Excel 活頁簿讀取結果物件 (Excel Gotten Result)
 * <p>
 * 遵循 {@code N + Gotten/Searched + Result} 命名規範 (3-6 規範)，作為純資料載體封裝整個 Excel 檔案各工作表資料。
 * 內部禁止寫入 {@code fromDomain} 等領域轉換邏輯。
 */
public class ExcelGottenResult {

	private final List<ExcelSheetGottenResult> sheets;

	public ExcelGottenResult(List<ExcelSheetGottenResult> sheets) {
		this.sheets = sheets != null ? Collections.unmodifiableList(new ArrayList<>(sheets)) : Collections.emptyList();
	}

	public List<ExcelSheetGottenResult> getSheets() {
		return sheets;
	}

	public int getSheetCount() {
		return sheets.size();
	}

	public boolean isEmpty() {
		return sheets.isEmpty();
	}

	/**
	 * 取得第一個工作表結果
	 *
	 * @return 第一個工作表 (Optional)
	 */
	public Optional<ExcelSheetGottenResult> getFirstSheet() {
		return sheets.isEmpty() ? Optional.empty() : Optional.of(sheets.get(0));
	}

	/**
	 * 根據工作表索引取得結果
	 *
	 * @param index 工作表索引 (0-based)
	 * @return 工作表結果 (Optional)
	 */
	public Optional<ExcelSheetGottenResult> getSheet(int index) {
		if (index >= 0 && index < sheets.size()) {
			return Optional.of(sheets.get(index));
		}
		return Optional.empty();
	}

	/**
	 * 根據工作表名稱取得結果
	 *
	 * @param sheetName 工作表名稱 (忽略大小寫)
	 * @return 工作表結果 (Optional)
	 */
	public Optional<ExcelSheetGottenResult> getSheet(String sheetName) {
		if (sheetName == null || sheetName.isBlank()) {
			return Optional.empty();
		}
		return sheets.stream()
				.filter(s -> s.getSheetName().equalsIgnoreCase(sheetName))
				.findFirst();
	}
}
