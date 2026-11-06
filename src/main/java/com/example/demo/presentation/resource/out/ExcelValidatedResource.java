package com.example.demo.presentation.resource.out;

import java.io.Serializable;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Excel 宣告式檢核結果回應資源載體 (Excel Validated Resource - Response Data)
 * <p>
 * 位於表現層回應模型 ({@code presentation/resource/out/})，遵循 {@code AGENTS.md} 規範 3-2 (以 {@code N + Ved + Resource} 命名)。<br>
 * 嚴守單一職責，作為純資料載體 (Pure Data Carrier)，內部不實作任何轉換邏輯，轉換統一由 Assembler 負責。
 * </p>
 *
 * @param success    是否完全通過檢核
 * @param totalRows  總檢核資料列數
 * @param validCount 合格資料列數
 * @param errorCount 錯誤項次總數
 * @param errors     錯誤細節清單
 * @param validData  合格資料物件清單
 */
@Schema(description = "Excel 宣告式檢核結果回應載體")
public record ExcelValidatedResource(
		@Schema(description = "是否完全通過檢核", example = "false")
		boolean success,

		@Schema(description = "總檢核資料列數", example = "10")
		int totalRows,

		@Schema(description = "合格資料列數", example = "8")
		int validCount,

		@Schema(description = "錯誤項次總數", example = "2")
		int errorCount,

		@Schema(description = "錯誤事實清單")
		List<ExcelRowErrorResource> errors,

		@Schema(description = "通過檢核之合格資料清單")
		List<?> validData
) implements Serializable {
}

