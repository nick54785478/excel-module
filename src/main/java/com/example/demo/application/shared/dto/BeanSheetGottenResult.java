package com.example.demo.application.shared.dto;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 宣告式工作表物件解析結果純資料載體 (Bean Sheet Gotten Result DTO)
 * <p>
 * 遵循 AGENTS.md 3-6 DTO 命名規範 ({@code N + Gotten/Searched + Result})，
 * 採用 Java 21 Record 作為應用層不可變純資料載體。
 * 負責同時封裝活頁簿中真實匹配或指定讀取之工作表名稱 ({@code sheetName}) 與解析後之強型別模型清單 ({@code items})。
 * </p>
 *
 * @param <T>       模型型別泛型
 * @param sheetName 活頁簿中真實解析之工作表名稱
 * @param items     解析完成之不可變模型清單
 */
public record BeanSheetGottenResult<T>(
		String sheetName,
		List<T> items
) {

	/**
	 * 緊湊建構子 (Compact Constructor)，執行欄位非空與不可變安全防禦
	 */
	public BeanSheetGottenResult {
		Objects.requireNonNull(sheetName, "sheetName must not be null");
		items = items != null ? Collections.unmodifiableList(items) : Collections.emptyList();
	}

	/**
	 * 取得解析所得之資料項目筆數
	 *
	 * @return 資料項目筆數
	 */
	public int totalCount() {
		return items.size();
	}

	/**
	 * 判斷模型清單是否為空
	 *
	 * @return 若清單為空則為 {@code true}，否則為 {@code false}
	 */
	public boolean isEmpty() {
		return items.isEmpty();
	}
}

