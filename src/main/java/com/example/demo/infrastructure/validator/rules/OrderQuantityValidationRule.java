package com.example.demo.infrastructure.validator.rules;

import org.jeasy.rules.annotation.Rule;
import org.springframework.stereotype.Component;

import com.example.demo.application.shared.model.ExcelRowError;
import com.example.demo.application.shared.model.OrderModel;

/**
 * 訂單購買數量合法性檢核規則 (Order Quantity Validation Rule)
 * <p>
 * 繼承 {@link AbstractExcelRowValidationRule}，以宣告式語法驗證訂單數量必須介於 1 至 9999 之間。
 * </p>
 */
@Component
@Rule(name = "OrderQuantityValidationRule", description = "驗證購買數量必須介於 1 至 9999", priority = 2)
public class OrderQuantityValidationRule extends AbstractExcelRowValidationRule<OrderModel> {

	public OrderQuantityValidationRule() {
		super(OrderModel.class);
	}

	@Override
	public boolean isInvalid(OrderModel order) {
		Integer quantity = order.getQuantity();
		return quantity == null || quantity <= 0 || quantity > 9999;
	}

	@Override
	public ExcelRowError createError(OrderModel order, int rowIndex) {
		return errorOfCell(
				rowIndex,
				3, // D 欄 (數量)
				"數量",
				order.getQuantity(),
				"購買數量必須為正整數且不可超過 9,999",
				"INVALID_ORDER_QUANTITY"
		);
	}
}

