package com.example.demo.presentation.resource.in;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

/**
 * 訂單匯出請求資源載體 (Request Data Carrier)
 * <p>
 * 遵循 {@code V + N + Resource} 命名規範 (3-1 規範)。
 * 作為純資料載體，嚴禁於內部實作任何轉換邏輯。
 *
 * @param sheetName 自訂工作表名稱 (選填，若為空則套用預設值)
 * @param orders    欲匯出的訂單清單，不可為空
 */
@Schema(description = "訂單匯出請求資源載體")
public record ExportOrderResource(
		@Schema(description = "自訂工作表名稱 (若無則使用模型預設名稱)", example = "11月份訂單清單")
		String sheetName,

		@Schema(description = "欲匯出的訂單資料清單", requiredMode = Schema.RequiredMode.REQUIRED)
		@Valid
		@NotEmpty(message = "訂單資料清單不能為空")
		List<OrderItemResource> orders
) {

	/**
	 * 單筆訂單請求資料項目
	 *
	 * @param orderNo      訂單編號
	 * @param customerName 訂購人姓名
	 * @param amount       購買金額
	 * @param quantity     數量
	 * @param orderDate    下單日期
	 * @param status       訂單狀態
	 */
	@Schema(description = "單筆訂單請求資料項")
	public record OrderItemResource(
			@Schema(description = "訂單編號", example = "ORD-2026-001")
			String orderNo,

			@Schema(description = "訂購人姓名", example = "王小明")
			String customerName,

			@Schema(description = "購買金額", example = "12500.50")
			BigDecimal amount,

			@Schema(description = "購買數量", example = "2")
			Integer quantity,

			@Schema(description = "下單日期 (格式: yyyy-MM-dd)", example = "2026-11-01")
			LocalDate orderDate,

			@Schema(description = "訂單狀態 (PAID, SHIPPED, PENDING)", example = "PAID")
			String status
	) {
	}
}
