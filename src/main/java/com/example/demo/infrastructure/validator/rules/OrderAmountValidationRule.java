package com.example.demo.infrastructure.validator.rules;

import java.math.BigDecimal;

import org.jeasy.rules.annotation.Rule;
import org.springframework.stereotype.Component;

import com.example.demo.application.shared.model.ExcelRowError;
import com.example.demo.application.shared.model.OrderModel;

/**
 * 訂單金額合法性檢核規則 (Order Amount Validation Rule)
 * <p>
 * 繼承 {@link AbstractExcelRowValidationRule}，以宣告式語法驗證訂單金額必須大於 0。
 * </p>
 */
@Component
@Rule(name = "OrderAmountValidationRule", description = "驗證訂單購買金額必須大於零", priority = 1)
public class OrderAmountValidationRule extends AbstractExcelRowValidationRule<OrderModel> {

	public OrderAmountValidationRule() {
		super(OrderModel.class);
	}

	@Override
	public boolean isInvalid(OrderModel order) {
		BigDecimal amount = order.getAmount();
		return amount == null || amount.compareTo(BigDecimal.ZERO) <= 0;
	}

	@Override
	public ExcelRowError createError(OrderModel order, int rowIndex) {
		return errorOfCell(
				rowIndex,
				2, // C 欄 (購買金額)
				"購買金額",
				order.getAmount(),
				"購買金額必須大於 0.00",
				"INVALID_ORDER_AMOUNT"
		);
	}
}

