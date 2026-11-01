package com.example.demo.presentation.resource.in;

import java.math.BigDecimal;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 物料主檔匯出請求資源載體 (Request Data Carrier)
 * <p>
 * 遵循 {@code V + N + Resource} 命名規範 (3-1 規範)。
 * 作為純資料載體，嚴禁於內部實作任何轉換邏輯。
 *
 * @param sheetName 自訂工作表名稱 (選填)
 * @param materials 欲匯出的物料清單 (選填，若為空則由展示端生成範例資料)
 */
@Schema(description = "物料主檔匯出請求資源載體")
public record ExportMaterialResource(
		@Schema(description = "自訂工作表名稱 (若無則使用模型預設名稱)", example = "物料主檔清單")
		String sheetName,

		@Schema(description = "欲匯出的物料資料清單")
		List<MaterialItemResource> materials
) {

	/**
	 * 單筆物料請求資料項目
	 *
	 * @param materialCode  物料料號
	 * @param materialName  物料品名
	 * @param specification 規格型號
	 * @param category      物料類別 (FINISHED_GOODS / SEMI_FINISHED_GOODS / RAW_MATERIAL)
	 * @param unit          計量單位 (PCS / SET / KG)
	 * @param standardCost  標準成本單價
	 * @param status        物料狀態 (ACTIVE / OBSOLETE / PENDING)
	 */
	@Schema(description = "單筆物料請求資料項")
	public record MaterialItemResource(
			@Schema(description = "物料料號", example = "MAT-FG-001")
			String materialCode,

			@Schema(description = "物料品名", example = "4K 專業航拍無人機")
			String materialName,

			@Schema(description = "規格型號", example = "DRONE-4K-PRO")
			String specification,

			@Schema(description = "物料類別", example = "FINISHED_GOODS")
			String category,

			@Schema(description = "計量單位", example = "PCS")
			String unit,

			@Schema(description = "標準成本 (TWD)", example = "35000.00")
			BigDecimal standardCost,

			@Schema(description = "物料狀態", example = "ACTIVE")
			String status
	) {
	}
}

