package com.example.demo.application.shared.model;

import java.io.Serializable;

/**
 * Excel 資料列檢核錯誤事實模型 (Excel Row Validation Error)
 * <p>
 * <b>【架構定位與職責】</b><br>
 * 位於 Clean Architecture 應用層共享模型 ({@code application/shared/model/})，以 Java 21 {@code record} 實作不可變之錯誤事實快照 (Immutable Fact)。
 * 封裝錯誤發生之儲存格座標、欄位名稱、無效輸入值與業務錯誤原因，
 * 供錯誤回標適配器精準定位並渲染樣式。
 * </p>
 *
 * @param rowIndex     發生錯誤的資料列索引 (1-based，與 Excel 介面列號完全一致)
 * @param columnIndex  發生錯誤的欄位索引 (0-based，-1 代表整列層級錯誤)
 * @param columnName   發生錯誤的欄位名稱 (例如："採購單價")
 * @param invalidValue 使用者輸入之原始無效值 (例如："-50" 或 "ABC")
 * @param errorMessage 詳細檢核錯誤原因 (例如："單價不可小於合約底價 10.00")
 * @param ruleCode     觸發之規則唯一識別碼 (例如："PRICE_BELOW_CONTRACT_MIN")
 */
public record ExcelRowError(
		int rowIndex,
		int columnIndex,
		String columnName,
		Object invalidValue,
		String errorMessage,
		String ruleCode
) implements Serializable {

	/**
	 * 便捷建構子：傳入行、列、欄位名稱與錯誤訊息
	 *
	 * @param rowIndex     資料列索引 (1-based)
	 * @param columnIndex  欄位索引 (0-based)
	 * @param columnName   欄位名稱
	 * @param errorMessage 詳細錯誤說明
	 */
	public ExcelRowError(int rowIndex, int columnIndex, String columnName, String errorMessage) {
		this(rowIndex, columnIndex, columnName, null, errorMessage, "VALIDATION_FAILED");
	}

	/**
	 * 相容方法：取得欄位名稱 (同 {@link #columnName()})
	 *
	 * @return 欄位名稱
	 */
	public String fieldName() {
		return columnName;
	}

	/**
	 * 便捷工廠方法：建立整列層級錯誤 (無特定單一欄位)
	 *
	 * @param rowIndex     資料列索引 (1-based)
	 * @param errorMessage 詳細錯誤說明
	 * @param ruleCode     規則代碼
	 * @return 錯誤事實物件
	 */
	public static ExcelRowError ofRow(int rowIndex, String errorMessage, String ruleCode) {
		return new ExcelRowError(rowIndex, -1, "整列", null, errorMessage, ruleCode);
	}

	/**
	 * 便捷工廠方法：建立特定儲存格欄位錯誤
	 *
	 * @param rowIndex     資料列索引 (1-based)
	 * @param columnIndex  欄位索引 (0-based)
	 * @param columnName   欄位名稱
	 * @param invalidValue 無效值
	 * @param errorMessage 詳細錯誤說明
	 * @param ruleCode     規則代碼
	 * @return 錯誤事實物件
	 */
	public static ExcelRowError ofCell(int rowIndex, int columnIndex, String columnName,
	                                  Object invalidValue, String errorMessage, String ruleCode) {
		return new ExcelRowError(rowIndex, columnIndex, columnName, invalidValue, errorMessage, ruleCode);
	}
}

