package com.example.demo.infrastructure.validator.rules;

import java.math.BigDecimal;

import org.jeasy.rules.annotation.Rule;
import org.springframework.stereotype.Component;

import com.example.demo.application.shared.model.ExcelRowError;
import com.example.demo.application.shared.model.ProductRecordModel;

/**
 * 商品售價檢核規則 (Product Price Validation Rule)
 * <p>
 * 繼承 {@link AbstractExcelRowValidationRule}，以宣告式語法驗證 Java 21 Record {@link ProductRecordModel} 之售價必須大於 0。
 * </p>
 */
@Component
@Rule(name = "ProductPriceValidationRule", description = "驗證商品售價必須大於零", priority = 1)
public class ProductPriceValidationRule extends AbstractExcelRowValidationRule<ProductRecordModel> {

	public ProductPriceValidationRule() {
		super(ProductRecordModel.class);
	}

	@Override
	public boolean isInvalid(ProductRecordModel product) {
		BigDecimal price = product.price();
		return price == null || price.compareTo(BigDecimal.ZERO) <= 0;
	}

	@Override
	public ExcelRowError createError(ProductRecordModel product, int rowIndex) {
		return errorOfCell(
				rowIndex,
				2, // C 欄 (商品售價)
				"商品售價",
				product.price(),
				"商品售價必須大於 0.00",
				"INVALID_PRODUCT_PRICE"
		);
	}
}

