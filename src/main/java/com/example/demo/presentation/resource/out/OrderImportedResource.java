package com.example.demo.presentation.resource.out;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 訂單匯入解析完成回應結構載體 (Response Data Carrier)
 * <p>
 * 遵循 {@code N + Ved + Resource} 命名規範 (3-2 規範)。
 * 作為純資料載體，嚴禁於內部實作任何轉換邏輯。
 *
 * @param totalCount 匯入解析成功的訂單總筆數
 * @param sheetName  工作表名稱
 * @param items      解析出的訂單明細資料清單
 */
@Schema(description = "訂單匯入解析完成回應結構")
public record OrderImportedResource(
		@Schema(description = "匯入成功筆數", example = "3")
		int totalCount,

		@Schema(description = "工作表名稱", example = "訂單匯入清單")
		String sheetName,

		@Schema(description = "解析成功的訂單明細清單")
		List<OrderRetrievedResource> items
) {

	/**
	 * 單筆解析成功之訂單資料項
	 *
	 * @param orderNo      訂單編號
	 * @param customerName 訂購人姓名
	 * @param amount       購買金額
	 * @param quantity     數量
	 * @param orderDate    下單日期
	 * @param status       訂單狀態
	 */
	@Schema(description = "已解析訂單項目")
	public record OrderRetrievedResource(
			@Schema(description = "訂單編號", example = "ORD-2026-001")
			String orderNo,

			@Schema(description = "訂購人姓名", example = "王小明")
			String customerName,

			@Schema(description = "購買金額", example = "12500.50")
			BigDecimal amount,

			@Schema(description = "數量", example = "2")
			Integer quantity,

			@Schema(description = "下單日期", example = "2026-11-01")
			LocalDate orderDate,

			@Schema(description = "訂單狀態", example = "PAID")
			String status
	) {
	}
}

