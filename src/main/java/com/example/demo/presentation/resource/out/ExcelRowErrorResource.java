package com.example.demo.presentation.resource.out;

import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 試算表檢核單項錯誤回應載體 (Excel Row Error Resource)
 * <p>
 * 位於表現層回應模型 ({@code presentation/resource/out/})，遵循 {@code AGENTS.md} 規範 3-2。<br>
 * 嚴格作為純資料載體，不包含任何轉換邏輯。
 * </p>
 *
 * @param rowIndex     發生錯誤列號 (1-based)
 * @param columnIndex  發生錯誤欄位索引 (0-based)
 * @param columnName   欄位名稱
 * @param invalidValue 使用者輸入之原始無效值
 * @param errorMessage 詳細檢核錯誤原因說明
 * @param ruleCode     觸發之規則唯一識別碼
 */
@Schema(description = "Excel 檢核單筆錯誤事實")
public record ExcelRowErrorResource(
		@Schema(description = "發生錯誤之資料列索引 (1-based)", example = "2")
		int rowIndex,

		@Schema(description = "發生錯誤之欄位索引 (0-based)", example = "2")
		int columnIndex,

		@Schema(description = "發生錯誤之欄位名稱", example = "購買金額")
		String columnName,

		@Schema(description = "使用者輸入之原始無效值", example = "-100.00")
		Object invalidValue,

		@Schema(description = "詳細檢核錯誤原因說明", example = "購買金額必須大於 0.00")
		String errorMessage,

		@Schema(description = "觸發之規則代碼", example = "INVALID_ORDER_AMOUNT")
		String ruleCode
) implements Serializable {
}

