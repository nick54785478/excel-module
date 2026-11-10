package com.example.demo.infrastructure.adapter.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.demo.application.shared.model.ExcelRowError;
import com.example.demo.application.shared.model.ExcelValidationContext;
import com.example.demo.application.shared.model.OrderModel;
import com.example.demo.application.shared.model.ProductRecordModel;
import com.example.demo.infrastructure.validator.rules.OrderAmountValidationRule;
import com.example.demo.infrastructure.validator.rules.OrderQuantityValidationRule;
import com.example.demo.infrastructure.validator.rules.ProductPriceValidationRule;

/**
 * EasyRules Excel 驗證適配器測試 (EasyRulesExcelValidatorAdapter Test)
 * <p>
 * 遵循 BDD 與 Gherkin 規範，驗證基於 EasyRules 規則引擎的行級客製驗證適配器。
 * 確保引擎遵循非短路全量錯誤累積 (Accumulate All Errors)，並正確處理 JavaBean 與 Java 21 Record 模型。
 * </p>
 */
@DisplayName("EasyRulesExcelValidatorAdapter 規則引擎適配器測試")
class EasyRulesExcelValidatorAdapterTest {

	private EasyRulesExcelValidatorAdapter<Object> adapter;

	@BeforeEach
	void setUp() {
		adapter = new EasyRulesExcelValidatorAdapter<>(List.of(
				new OrderAmountValidationRule(),
				new OrderQuantityValidationRule(),
				new ProductPriceValidationRule()
		));
	}

	/**
	 * Feature: EasyRules 行級資料檢核
	 * 
	 * Scenario: 當訂單資料金額與數量皆符合業務規範時，應完全通過且無任何錯誤
	 *   Given 一筆金額為 500.00 且數量為 5 的合法 OrderModel
	 *    When 呼叫 EasyRulesExcelValidatorAdapter 執行行級檢核
	 *    Then 檢核結果應為空清單，代表全部驗證通過
	 */
	@Test
	@DisplayName("場景：合法訂單資料應順利通過檢核且無任何錯誤")
	void shouldPassValidationWhenOrderDataIsValid() {
		// Given
		OrderModel validOrder = new OrderModel("ORD-001", "王小明", new BigDecimal("500.00"), 5, LocalDate.now(), "PAID");
		ExcelValidationContext<Object> context = new ExcelValidationContext<>("訂單清單", List.of(validOrder));

		// When
		List<ExcelRowError> errors = adapter.validateRow(context, 2, validOrder);

		// Then
		assertTrue(errors.isEmpty(), "合規訂單不應產生任何錯誤");
	}

	/**
	 * Feature: EasyRules 全量錯誤累積機制 (Accumulate All Errors)
	 * 
	 * Scenario: 當訂單金額為負數且數量超過上限時，系統應累積所有違規事實而不會中途短路中斷
	 *   Given 一筆金額為 -100.00 (負數) 且數量為 15000 (超過上限 9999 違規) 的 OrderModel
	 *    When 呼叫 EasyRulesExcelValidatorAdapter 執行第 3 行檢核
	 *    Then 引擎不應短路，應累積回傳 2 筆錯誤事實
	 *     And 包含購買金額錯誤 (第 2 欄)
	 *     And 包含數量錯誤 (第 3 欄)
	 */
	@Test
	@DisplayName("場景：金額與數量同時違規時，應全量累積兩者錯誤事實")
	void shouldAccumulateMultipleErrorsWithoutShortCircuit() {
		// Given
		OrderModel invalidOrder = new OrderModel("ORD-002", "李大華", new BigDecimal("-100.00"), 15000, LocalDate.now(), "PENDING");
		ExcelValidationContext<Object> context = new ExcelValidationContext<>("訂單清單", List.of(invalidOrder));

		// When
		List<ExcelRowError> errors = adapter.validateRow(context, 3, invalidOrder);

		// Then
		assertFalse(errors.isEmpty());
		assertEquals(2, errors.size(), "應全量收集 2 筆違規錯誤事實，非短路中斷");

		ExcelRowError amountError = errors.stream()
				.filter(e -> "購買金額".equals(e.fieldName()))
				.findFirst()
				.orElseThrow();
		assertEquals(3, amountError.rowIndex());
		assertEquals(2, amountError.columnIndex());
		assertTrue(amountError.errorMessage().contains("大於 0.00"));

		ExcelRowError quantityError = errors.stream()
				.filter(e -> "數量".equals(e.fieldName()))
				.findFirst()
				.orElseThrow();
		assertEquals(3, quantityError.rowIndex());
		assertEquals(3, quantityError.columnIndex());
		assertTrue(quantityError.errorMessage().contains("不可超過 9,999"));
	}

	/**
	 * Feature: Java 21 Record 領域模型客製檢核
	 * 
	 * Scenario: 商品售價為 0 或負數時應被 EasyRules 檢出違規
	 *   Given 一筆售價為 0.00 的違規 ProductRecordModel
	 *    When 呼叫 EasyRulesExcelValidatorAdapter 執行第 4 行檢核
	 *    Then 應回傳商品售價必須大於零的錯誤事實
	 */
	@Test
	@DisplayName("場景：Record 商品售價違規時應正確檢出")
	void shouldDetectPriceViolationOnProductRecordModel() {
		// Given
		ProductRecordModel invalidProduct = new ProductRecordModel("PROD-001", "測試商品", BigDecimal.ZERO, true);
		ExcelValidationContext<Object> context = new ExcelValidationContext<>("商品清單", List.of(invalidProduct));

		// When
		List<ExcelRowError> errors = adapter.validateRow(context, 4, invalidProduct);

		// Then
		assertEquals(1, errors.size());
		ExcelRowError priceError = errors.getFirst();
		assertEquals(4, priceError.rowIndex());
		assertEquals(2, priceError.columnIndex());
		assertEquals("商品售價", priceError.fieldName());
		assertTrue(priceError.errorMessage().contains("必須大於 0"));
	}
}
