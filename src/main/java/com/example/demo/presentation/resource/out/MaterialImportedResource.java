package com.example.demo.presentation.resource.out;

import java.math.BigDecimal;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 物料主檔匯入解析完成回應結構載體 (Response Data Carrier)
 * <p>
 * 遵循 {@code N + Ved + Resource} 命名規範 (3-2 規範)。
 * 作為純資料載體，嚴禁於內部實作任何轉換邏輯。
 *
 * @param totalCount 匯入解析成功的物料總筆數
 * @param sheetName  工作表名稱
 * @param items      解析出的物料明細資料清單
 */
@Schema(description = "物料主檔匯入解析完成回應結構")
public record MaterialImportedResource(
		@Schema(description = "匯入成功筆數", example = "3")
		int totalCount,

		@Schema(description = "工作表名稱", example = "物料主檔清單")
		String sheetName,

		@Schema(description = "解析成功的物料明細清單")
		List<MaterialRetrievedResource> items
) {

	/**
	 * 單筆解析成功之物料項目
	 *
	 * @param materialCode  物料料號
	 * @param materialName  物料品名
	 * @param specification 規格型號
	 * @param category      物料類別
	 * @param unit          計量單位
	 * @param standardCost  標準成本 (TWD)
	 * @param status        狀態
	 */
	@Schema(description = "已解析物料項目")
	public record MaterialRetrievedResource(
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

			@Schema(description = "狀態", example = "ACTIVE")
			String status
	) {
	}
}

