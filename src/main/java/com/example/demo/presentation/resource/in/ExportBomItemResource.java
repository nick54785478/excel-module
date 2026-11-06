package com.example.demo.presentation.resource.in;

import java.math.BigDecimal;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 單階 BOM 配方匯出請求資源載體 (Request Data Carrier)
 * <p>
 * 遵循 {@code V + N + Resource} 命名規範 (3-1 規範)。
 * 作為純資料載體，嚴禁於內部實作任何轉換邏輯。
 *
 * @param sheetName 自訂工作表名稱 (選填)
 * @param items     欲匯出的 BOM 子項清單 (選填)
 */
@Schema(description = "單階 BOM 配方匯出請求資源載體")
public record ExportBomItemResource(
		@Schema(description = "自訂工作表名稱 (若無則使用模型預設名稱)", example = "單階BOM清單")
		String sheetName,

		@Schema(description = "欲匯出的 BOM 配方資料清單")
		List<BomItemDetailResource> items
) {

	/**
	 * 單筆 BOM 子項請求資料項目
	 *
	 * @param parentMaterialCode 母件料號
	 * @param bomVersion         BOM 版本號
	 * @param itemSequence       項次序號
	 * @param childMaterialCode  子件料號
	 * @param childMaterialName  子件名稱
	 * @param quantity           組裝用量
	 * @param unit               計量單位
	 * @param scrapRate          損耗率 (%)
	 * @param positionCode       安裝位置代碼
	 * @param remark             備註說明
	 */
	@Schema(description = "單筆 BOM 子項請求資料項")
	public record BomItemDetailResource(
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

			@Schema(description = "計量單位", example = "SET")
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

