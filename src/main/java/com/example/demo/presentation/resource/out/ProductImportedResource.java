package com.example.demo.presentation.resource.out;

import java.math.BigDecimal;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 商品匯入解析完成回應結構載體 (Response Data Carrier)
 * <p>
 * 遵循 {@code N + Ved + Resource} 命名規範 (3-2 規範)。
 * 作為純資料載體，嚴禁於內部實作任何轉換邏輯。
 *
 * @param totalCount 匯入解析成功的商品總筆數
 * @param sheetName  工作表名稱
 * @param items      解析出的商品明細資料清單
 */
@Schema(description = "商品匯入解析完成回應結構")
public record ProductImportedResource(
		@Schema(description = "匯入成功筆數", example = "2")
		int totalCount,

		@Schema(description = "工作表名稱", example = "商品主檔清單")
		String sheetName,

		@Schema(description = "解析成功的商品明細清單")
		List<ProductRetrievedResource> items
) {

	/**
	 * 單筆解析成功之商品資料項
	 *
	 * @param productCode 商品代碼
	 * @param productName 商品名稱
	 * @param price       商品售價
	 * @param onSale      在售狀態
	 */
	@Schema(description = "已解析商品項目")
	public record ProductRetrievedResource(
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

