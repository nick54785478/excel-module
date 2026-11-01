package com.example.demo.presentation.resource.in;

import java.math.BigDecimal;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 商品匯出請求資源載體 (Request Data Carrier)
 * <p>
 * 遵循 {@code V + N + Resource} 命名規範 (3-1 規範)。
 * 作為純資料載體，嚴禁於內部實作任何轉換邏輯。
 *
 * @param sheetName 自訂工作表名稱 (選填，若為空則套用預設值)
 * @param products  欲匯出的商品清單 (選填，若為空則由展示端生成範例資料)
 */
@Schema(description = "商品匯出請求資源載體")
public record ExportProductResource(
		@Schema(description = "自訂工作表名稱 (若無則使用模型預設名稱)", example = "熱銷商品主檔")
		String sheetName,

		@Schema(description = "欲匯出的商品資料清單")
		List<ProductItemResource> products
) {

	/**
	 * 單筆商品請求資料項目
	 *
	 * @param productCode 商品代碼
	 * @param productName 商品名稱
	 * @param price       商品售價
	 * @param onSale      在售狀態
	 */
	@Schema(description = "單筆商品請求資料項")
	public record ProductItemResource(
			@Schema(description = "商品代碼", example = "PROD-001")
			String productCode,

			@Schema(description = "商品名稱", example = "4K 專業航拍無人機")
			String productName,

			@Schema(description = "商品售價", example = "35000.00")
			BigDecimal price,

			@Schema(description = "在售狀態", example = "true")
			Boolean onSale
	) {
	}
}

