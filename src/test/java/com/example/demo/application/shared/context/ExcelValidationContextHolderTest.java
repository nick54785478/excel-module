package com.example.demo.application.shared.context;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.demo.application.shared.dto.ExcelValidationGottenResult;
import com.example.demo.application.shared.model.demo.OrderModel;

/**
 * Excel 資料檢核執行緒上下文保持器單元測試 (ExcelValidationContextHolder Test)
 * <p>
 * 遵循 BDD / Gherkin (Feature, Scenario, Given-When-Then) 測試註解規範。
 * </p>
 */
@DisplayName("ExcelValidationContextHolder 執行緒上下文保持器測試")
class ExcelValidationContextHolderTest {

	@AfterEach
	void tearDown() {
		ExcelValidationContextHolder.clear();
	}

	/**
	 * Feature: 檢核結果上下文綁定與獲取
	 * 
	 * Scenario: 成功綁定檢核結果至當前執行緒，並正確讀取合格模型清單
	 *   Given 一筆包含 1 筆訂單之合格檢核結果
	 *    When 調用 ExcelValidationContextHolder.bind(mockResult)
	 *    Then get() 應回傳非空 Optional，且 getValidItems(OrderModel.class) 包含該筆訂單
	 */
	@Test
	@DisplayName("場景：成功綁定檢核結果並可透過型別讀取合格項目清單")
	void shouldBindAndRetrieveValidationResultSuccessfully() {
		// Given
		OrderModel order = new OrderModel("ORD-001", "張三", null, 1, null, "PAID");
		ExcelValidationGottenResult<OrderModel> result = ExcelValidationGottenResult.success(List.of(order));

		// When
		ExcelValidationContextHolder.bind(result);

		// Then
		assertThat(ExcelValidationContextHolder.get()).isPresent();
		assertThat(ExcelValidationContextHolder.get().get().valid()).isTrue();

		List<OrderModel> validItems = ExcelValidationContextHolder.getValidItems(OrderModel.class);
		assertThat(validItems).hasSize(1);
		assertThat(validItems.get(0).getOrderNo()).isEqualTo("ORD-001");
	}

	/**
	 * Feature: 上下文清理機制
	 * 
	 * Scenario: 呼叫 clear() 後，ThreadLocal 內容被安全釋放
	 *   Given 已綁定之檢核結果
	 *    When 呼叫 ExcelValidationContextHolder.clear()
	 *    Then get() 應回傳空 Optional，getValidItems 應回傳空清單
	 */
	@Test
	@DisplayName("場景：呼叫 clear 後應安全清除上下文")
	void shouldClearContextSafely() {
		// Given
		ExcelValidationGottenResult<OrderModel> result = ExcelValidationGottenResult.success(Collections.emptyList());
		ExcelValidationContextHolder.bind(result);
		assertThat(ExcelValidationContextHolder.get()).isPresent();

		// When
		ExcelValidationContextHolder.clear();

		// Then
		assertThat(ExcelValidationContextHolder.get()).isEmpty();
		assertThat(ExcelValidationContextHolder.getValidItems(OrderModel.class)).isEmpty();
	}
}

