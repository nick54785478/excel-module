package com.example.demo.application.shared.model;

import java.math.BigDecimal;

import com.example.demo.application.annotation.mapping.ExcelColumn;
import com.example.demo.application.annotation.mapping.ExcelSheet;

/**
 * 商品主檔模型 (Java 21 Record Product Model)
 * <p>
 * 遵循維度一「宣告式物件映射」設計，以註解標註工作表與各欄位屬性。
 * 支援 Java 21 原生不可變 Record 物件（Canonical Constructor 自動綁定）的雙向 Excel 解析與匯出。
 *
 * @param productCode 商品代碼
 * @param productName 商品名稱
 * @param price       商品售價
 * @param onSale      在售狀態
 */
@ExcelSheet(name = "商品主檔清單")
public record ProductRecordModel(
		@ExcelColumn(name = "商品代碼", index = 0) String productCode,

		@ExcelColumn(name = "商品名稱", index = 1) String productName,

		@ExcelColumn(name = "商品售價", index = 2, format = "#,##0.00") BigDecimal price,

		@ExcelColumn(name = "在售狀態", index = 3) Boolean onSale) {
}
