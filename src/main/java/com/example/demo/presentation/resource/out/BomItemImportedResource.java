package com.example.demo.presentation.resource.out;

import java.math.BigDecimal;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 單階 BOM 配方匯入解析完成回應結構載體 (Response Data Carrier)
 * <p>
 * 遵循 {@code N + Ved + Resource} 命名規範 (3-2 規範)。
 * 作為純資料載體，嚴禁於內部實作任何轉換邏輯。
 *
 * @param totalCount 匯入解析成功的 BOM 子項總筆數
 * @param sheetName  工作表名稱
 * @param items      解析出的 BOM 子項明細清單
 */
@Schema(description = "單階 BOM 配方匯入解析完成回應結構")
public record BomItemImportedResource(
		@Schema(description = "匯入成功筆數", example = "4")
		int totalCount,

		@Schema(description = "工作表名稱", example = "單階BOM清單")
		String sheetName,

		@Schema(description = "解析成功的 BOM 子項清單")
		List<BomItemRetrievedResource> items
) {

	/**
	 * 單筆解析成功之 BOM 子項
	 *
	 * @param parentMaterialCode 母件料號
	 * @param bomVersion         BOM 版本號
	 * @param itemSequence       項次序號
	 * @param childMaterialCode  子件料號
	 * @param childMaterialName  子件名稱
	 * @param quantity           組裝用量
	 * @param unit               單位
	 * @param scrapRate          損耗率 (%)
	 * @param positionCode       安裝位置代碼
	 * @param remark             備註說明
	 */
	@Schema(description = "已解析 BOM 子項")
	public record BomItemRetrievedResource(
			@Schema(description = "母件料號", example = "MAT-FG-001")
			String parentMaterialCode,

			@Schema(description = "BOM 版本號", example = "1.0")
			String bomVersion,

			@Schema(description = "項次序號", example = "10")
			Integer itemSequence,

			@Schema(description = "子件料號", example = "MAT-SFG-001")
			String childMaterialCode,

			@Schema(description = "子件名稱", example = "4K 雲台相機模組")
			String childMaterialName,

			@Schema(description = "組裝用量", example = "1.0000")
			BigDecimal quantity,

			@Schema(description = "單位", example = "SET")
			String unit,

			@Schema(description = "損耗率 (%)", example = "0.00")
			BigDecimal scrapRate,

			@Schema(description = "安裝位置代碼", example = "GIMBAL-BAY-01")
			String positionCode,

			@Schema(description = "備註說明", example = "光學防手震")
			String remark
	) {
	}
}

