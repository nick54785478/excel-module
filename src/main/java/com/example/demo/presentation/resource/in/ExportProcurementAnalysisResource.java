package com.example.demo.presentation.resource.in;

import java.math.BigDecimal;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 物料採購損益分析報表匯出請求資源載體 (Export Procurement Analysis Request Resource)
 * <p>
 * 遵循 AGENTS.md 3-1 命名規範 ({@code V + N + Resource})。
 * 作為表現層 HTTP 請求輸入純資料載體，接收前端傳送之物料採購清單、報表標題、解鎖密碼與機密標記。
 * 嚴禁於此 Resource 內部實作 {@code toCommand} 等轉換邏輯。
 *
 * @param reportTitle    自訂列印頁首報表標題 (選填，若未填則預設為 "物料採購損益分析月報")
 * @param password       工作表解鎖密碼 (選填，鎖定公式防篡改，預設為 "AuditSecure2026")
 * @param classification 列印頁尾機密等級標籤 (選填，預設為 "機密資料・內部流轉")
 * @param items          物料採購明細項目清單 (選填，若為空則使用系統擬真示範資料)
 */
@Schema(description = "物料採購損益分析報表匯出請求資源載體")
public record ExportProcurementAnalysisResource(
		@Schema(description = "自訂列印頁首報表標題", example = "2026年度第三季無人機採購損益報表")
		String reportTitle,

		@Schema(description = "工作表解鎖密碼 (鎖定公式防篡改)", example = "AuditSecure2026")
		String password,

		@Schema(description = "列印頁尾機密等級標籤", example = "機密資料・內部流轉")
		String classification,

		@Schema(description = "物料採購明細項目清單 (若未傳入則自動使用系統預設示範資料)")
		List<@Valid ProcurementItemResource> items
) {

	/**
	 * 採購明細單列項目資源載體 (Procurement Item Resource Carrier)
	 *
	 * @param itemCode       物料代碼 (如 MAT-001)
	 * @param itemName       物料名稱 (如 碳纖維主樑結構)
	 * @param quantity       採購數量 (必須大於 0)
	 * @param unitPrice      採購單價 (必須大於 0)
	 * @param estimatedPrice 預估銷售單價 (必須大於 0)
	 */
	@Schema(description = "採購明細項目資源載體")
	public record ProcurementItemResource(
			@Schema(description = "物料代碼", requiredMode = Schema.RequiredMode.REQUIRED, example = "MAT-001")
			@NotBlank(message = "物料代碼不得為空")
			String itemCode,

			@Schema(description = "物料名稱", requiredMode = Schema.RequiredMode.REQUIRED, example = "高轉速無刷電機馬達")
			@NotBlank(message = "物料名稱不得為空")
			String itemName,

			@Schema(description = "採購數量", requiredMode = Schema.RequiredMode.REQUIRED, example = "20")
			@NotNull(message = "採購數量不得為空")
			@Min(value = 1, message = "採購數量必須大於 0")
			Integer quantity,

			@Schema(description = "採購單價", requiredMode = Schema.RequiredMode.REQUIRED, example = "850.00")
			@NotNull(message = "採購單價不得為空")
			@DecimalMin(value = "0.0", inclusive = false, message = "採購單價必須大於 0")
			BigDecimal unitPrice,

			@Schema(description = "預估售價", requiredMode = Schema.RequiredMode.REQUIRED, example = "1350.00")
			@NotNull(message = "預估售價不得為空")
			@DecimalMin(value = "0.0", inclusive = false, message = "預估售價必須大於 0")
			BigDecimal estimatedPrice
	) {}
}

