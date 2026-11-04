package com.example.demo.application.shared.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Excel 活頁簿讀取結果物件 (Excel Gotten Result DTO)
 * <p>
 * 遵循 AGENTS.md 3-6 DTO 命名規範 ({@code N + Gotten/Searched + Result})，採用 Java Record 作為應用層不可變純資料載體。
 * 封裝整份 Excel 檔案內所有工作表的讀取結果，嚴禁於內部實作領域轉換邏輯。
 *
 * @param sheets 工作表讀取結果清單 (不可變清單)
 */
public record ExcelGottenResult(
		List<ExcelSheetGottenResult> sheets
) {

	/**
	 * 緊湊建構子 (Compact Constructor)，執行集合不可變安全防禦
	 */
	public ExcelGottenResult {
		sheets = sheets != null ? Collections.unmodifiableList(new ArrayList<>(sheets)) : Collections.emptyList();
	}

	// ==================== 向後相容與便利查詢方法 ====================

	public List<ExcelSheetGottenResult> getSheets() {
		return sheets();
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
				.filter(s -> s.sheetName().equalsIgnoreCase(sheetName))
				.findFirst();
	}
}
