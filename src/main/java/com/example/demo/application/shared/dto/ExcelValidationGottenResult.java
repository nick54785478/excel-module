package com.example.demo.application.shared.dto;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import com.example.demo.application.shared.model.ExcelRowError;

/**
 * Excel 資料檢核結果資料載體 (Excel Validation Gotten Result DTO)
 * <p>
 * <b>【架構定位與職責規範】</b><br>
 * 位於 Clean Architecture 應用層 DTO 套件 ({@code application/shared/dto/})，
 * 遵循 {@code AGENTS.md} 規範 3-6 (命名為 {@code N + Gotten/Searched + Result}) 與規則 21。<br>
 * 嚴格作為純資料載體 (Pure Data Carrier)，以 Java 21 {@code record} 實作不可變性，
 * 內部不負責任何轉換邏輯，封裝驗證是否通過、合格資料清單、全量錯誤清單與統計數據。
 * </p>
 *
 * @param <T>        資料模型型別
 * @param valid      是否完全通過檢核 (無任何錯誤)
 * @param validItems 通過檢核之合格資料清單
 * @param errors     所有檢核錯誤事實清單
 * @param totalRows  總資料列數
 * @param validCount 合格資料列數
 * @param errorCount 錯誤項次總數
 */
public record ExcelValidationGottenResult<T>(
		boolean valid,
		List<T> validItems,
		List<ExcelRowError> errors,
		int totalRows,
		int validCount,
		int errorCount
) implements Serializable {

	/**
	 * 便捷建構子：自動計算統計數量與通過狀態
	 *
	 * @param validItems 合格資料清單
	 * @param errors     錯誤事實清單
	 * @param totalRows  總檢核資料列數
	 */
	public ExcelValidationGottenResult(List<T> validItems, List<ExcelRowError> errors, int totalRows) {
		this(
				errors == null || errors.isEmpty(),
				validItems != null ? List.copyOf(validItems) : Collections.emptyList(),
				errors != null ? List.copyOf(errors) : Collections.emptyList(),
				totalRows,
				validItems != null ? validItems.size() : 0,
				errors != null ? errors.size() : 0
		);
	}

	/**
	 * 成功結果便捷工廠
	 *
	 * @param items 全數合格之資料清單
	 * @param <T>   模型型別
	 * @return 完全檢核通過之結果 DTO
	 */
	public static <T> ExcelValidationGottenResult<T> success(List<T> items) {
		int count = items != null ? items.size() : 0;
		return new ExcelValidationGottenResult<>(items, Collections.emptyList(), count);
	}

	/**
	 * 包含錯誤之結果便捷工廠
	 *
	 * @param validItems 部分合格資料清單
	 * @param errors     檢核錯誤清單
	 * @param totalRows  總資料列數
	 * @param <T>        模型型別
	 * @return 檢核失敗之結果 DTO
	 */
	public static <T> ExcelValidationGottenResult<T> failed(List<T> validItems, List<ExcelRowError> errors, int totalRows) {
		return new ExcelValidationGottenResult<>(validItems, errors, totalRows);
	}

	/**
	 * 是否包含錯誤 (相容輔助方法)
	 *
	 * @return {@code true} 代表包含一筆以上檢核錯誤
	 */
	public boolean hasErrors() {
		return !valid || (errors != null && !errors.isEmpty());
	}
}

