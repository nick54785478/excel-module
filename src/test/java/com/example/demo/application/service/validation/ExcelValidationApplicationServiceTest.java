package com.example.demo.application.service.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.demo.application.port.out.validation.ExcelErrorMarkerPort;
import com.example.demo.application.port.out.core.ExcelReaderPort;
import com.example.demo.application.port.out.validation.ExcelRowValidatorPort;
import com.example.demo.application.port.out.validation.ExcelSheetValidatorPort;
import com.example.demo.application.shared.dto.ExcelGottenResult;
import com.example.demo.application.shared.dto.ExcelSheetGottenResult;
import com.example.demo.application.shared.dto.ExcelValidationGottenResult;
import com.example.demo.application.shared.model.ExcelRowError;
import com.example.demo.application.shared.model.OrderModel;

/**
 * Excel 資料檢核應用服務業務案例測試 (ExcelValidationApplicationService Test)
 * <p>
 * 遵循 BDD 與 Gherkin 規範，驗證 {@link ExcelValidationApplicationService} 核心調度邏輯：
 * 包括行級驗證器、整表約束驗證器、全量錯誤收集以及錯誤標註回標檔產製決策。
 * </p>
 */
@DisplayName("ExcelValidationApplicationService 資料檢核應用服務測試")
class ExcelValidationApplicationServiceTest {

	private ExcelReaderPort excelReaderPort;
	private ExcelErrorMarkerPort errorMarkerPort;
	private ExcelRowValidatorPort<OrderModel> rowValidator;
	private ExcelSheetValidatorPort<OrderModel> sheetValidator;
	private ExcelValidationApplicationService service;

	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUp() {
		excelReaderPort = mock(ExcelReaderPort.class);
		errorMarkerPort = mock(ExcelErrorMarkerPort.class);
		rowValidator = mock(ExcelRowValidatorPort.class);
		sheetValidator = mock(ExcelSheetValidatorPort.class);

		when(rowValidator.supports(OrderModel.class)).thenReturn(true);
		when(sheetValidator.supports(OrderModel.class)).thenReturn(true);

		service = new ExcelValidationApplicationService(
				excelReaderPort,
				List.of(rowValidator),
				List.of(sheetValidator),
				errorMarkerPort
		);
	}

	/**
	 * Feature: Excel 業務資料全量合格檢核
	 * 
	 * Scenario: 當所有資料列均通過驗證時，服務應回傳合格狀態且合格數等於總列數
	 *   Given 記憶體中包含 2 筆合法的 OrderModel 資料
	 *     And rowValidator 與 sheetValidator 皆未回傳任何錯誤
	 *    When 呼叫 validateRows 進行檢核
	 *    Then 結果 DTO 的 valid 屬性應為 true
	 *     And validCount 應為 2，errorCount 應為 0
	 */
	@Test
	@DisplayName("當所有資料完全合格時，應回傳成功結果")
	void shouldValidateAndReturnSuccessWhenAllRowsAreValid() {
		// Given
		OrderModel o1 = new OrderModel("ORD-1", "客戶A", new BigDecimal("100.00"), 1, LocalDate.now(), "PAID");
		OrderModel o2 = new OrderModel("ORD-2", "客戶B", new BigDecimal("200.00"), 2, LocalDate.now(), "PAID");
		List<OrderModel> rows = List.of(o1, o2);

		when(rowValidator.validateRow(any(), any(Integer.class), any())).thenReturn(Collections.emptyList());
		when(sheetValidator.validateSheet(any())).thenReturn(Collections.emptyList());

		// When
		ExcelValidationGottenResult<OrderModel> result = service.validateRows("訂單表", rows, OrderModel.class);

		// Then
		assertTrue(result.valid());
		assertFalse(result.hasErrors());
		assertEquals(2, result.totalRows());
		assertEquals(2, result.validCount());
		assertEquals(0, result.errorCount());
	}

	/**
	 * Feature: Excel 行級與整表錯誤全量收集
	 * 
	 * Scenario: 當第 1 筆資料違反行級規則且整表包含重複編號時，系統應累積所有錯誤
	 *   Given 包含 2 筆 OrderModel 資料
	 *     And 第 1 筆觸發行級金額錯誤
	 *     And 整表觸發編號重複錯誤
	 *    When 呼叫 validateRows 進行檢核
	 *    Then 結果 DTO 的 valid 屬性應為 false
	 *     And 應累積回傳 2 筆錯誤事實 (包含行級與整表)
	 *     And 合格資料數應為 1
	 */
	@Test
	@DisplayName("當包含行級與整表錯誤時，應全量累積且精確統計合格數")
	void shouldValidateAndCollectAllErrorsWhenRowsContainInvalidData() {
		// Given
		OrderModel o1 = new OrderModel("ORD-DUP", "客戶A", new BigDecimal("-50.00"), 1, LocalDate.now(), "PAID");
		OrderModel o2 = new OrderModel("ORD-DUP", "客戶B", new BigDecimal("200.00"), 2, LocalDate.now(), "PAID");
		List<OrderModel> rows = List.of(o1, o2);

		ExcelRowError rowErr = ExcelRowError.ofCell(2, 2, "金額", "-50.00", "金額必須大於0", "INVALID_AMOUNT");
		ExcelRowError sheetErr = ExcelRowError.ofRow(3, "訂單編號不可重複", "DUPLICATE_ORDER_NO");

		when(rowValidator.validateRow(any(), any(Integer.class), any())).thenAnswer(inv -> {
			int rIdx = inv.getArgument(1);
			return rIdx == 2 ? List.of(rowErr) : Collections.emptyList();
		});
		when(sheetValidator.validateSheet(any())).thenReturn(List.of(sheetErr));

		// When
		ExcelValidationGottenResult<OrderModel> result = service.validateRows("訂單表", rows, OrderModel.class);

		// Then
		assertFalse(result.valid());
		assertTrue(result.hasErrors());
		assertEquals(2, result.totalRows());
		assertEquals(1, result.validCount());
		assertEquals(2, result.errorCount());
		assertEquals(2, result.errors().size());
	}

	/**
	 * Feature: 錯誤回標檔產製決策
	 * 
	 * Scenario: 當輸入之試算表含有錯誤時，validateAndGenerateFeedback 應調用回標適配器產製二進位檔
	 *   Given 透過 ExcelReaderPort 解析出包含 1 筆錯誤的資料
	 *     And errorFeedbackPort 模擬回傳產出的 Excel byte 陣列
	 *    When 呼叫 validateAndGenerateFeedback
	 *    Then 應回傳非空之 byte 陣列
	 *     And 應驗證調用了 errorFeedbackPort.generateErrorMarkedWorkbook
	 */
	@Test
	@DisplayName("檢核發現錯誤時，應調用錯誤回標適配器產製檔案")
	void shouldGenerateFeedbackBytesWhenErrorsExist() {
		// Given
		List<String> headers = List.of("訂單編號", "訂購人", "購買金額", "數量");
		List<List<Object>> rows = List.of(
				List.of("ORD-1", "測試者", "-100.00", 1)
		);
		List<List<String>> textRows = List.of(
				List.of("ORD-1", "測試者", "-100.00", "1")
		);
		ExcelSheetGottenResult sheetResult = new ExcelSheetGottenResult(0, "訂單匯入清單", headers, rows, textRows);
		ExcelGottenResult gottenResult = new ExcelGottenResult(List.of(sheetResult));

		when(excelReaderPort.read(any(InputStream.class))).thenReturn(gottenResult);

		ExcelRowError err = ExcelRowError.ofCell(2, 2, "購買金額", "-100.00", "金額必須大於0", "INVALID_AMOUNT");
		when(rowValidator.validateRow(any(), any(Integer.class), any())).thenReturn(List.of(err));
		when(sheetValidator.validateSheet(any())).thenReturn(Collections.emptyList());

		byte[] mockFeedbackBytes = new byte[]{1, 2, 3, 4};
		when(errorMarkerPort.generateErrorMarkedWorkbook(any(), any())).thenReturn(mockFeedbackBytes);

		// When
		byte[] resultBytes = service.validateAndGenerateFeedback(new ByteArrayInputStream(new byte[]{0}), OrderModel.class);

		// Then
		assertNotNull(resultBytes);
		assertEquals(4, resultBytes.length);
		verify(errorMarkerPort).generateErrorMarkedWorkbook(any(), any());
	}

	/**
	 * Feature: 全量合格時跳過錯誤回標檔產製
	 * 
	 * Scenario: 當輸入之試算表完全無錯誤時，validateAndGenerateFeedback 應回傳空 byte 陣列
	 *   Given ExcelReaderPort 解析出合格資料且驗證無誤
	 *    When 呼叫 validateAndGenerateFeedback
	 *    Then 回傳之 byte 陣列長度應為 0
	 */
	@Test
	@DisplayName("資料完全合格時，應回傳長度為 0 之 byte 陣列")
	void shouldReturnEmptyFeedbackBytesWhenDataIsFullyValid() {
		// Given
		List<String> headers = List.of("訂單編號", "訂購人", "購買金額", "數量");
		List<List<Object>> rows = List.of(
				List.of("ORD-1", "測試者", "100.00", 1)
		);
		List<List<String>> textRows = List.of(
				List.of("ORD-1", "測試者", "100.00", "1")
		);
		ExcelSheetGottenResult sheetResult = new ExcelSheetGottenResult(0, "訂單匯入清單", headers, rows, textRows);
		ExcelGottenResult gottenResult = new ExcelGottenResult(List.of(sheetResult));

		when(excelReaderPort.read(any(InputStream.class))).thenReturn(gottenResult);
		when(rowValidator.validateRow(any(), any(Integer.class), any())).thenReturn(Collections.emptyList());
		when(sheetValidator.validateSheet(any())).thenReturn(Collections.emptyList());

		// When
		byte[] resultBytes = service.validateAndGenerateFeedback(new ByteArrayInputStream(new byte[]{0}), OrderModel.class);

		// Then
		assertNotNull(resultBytes);
		assertEquals(0, resultBytes.length, "全數合格時不應產製錯誤回標檔");
	}
}
