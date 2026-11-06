package com.example.demo.infrastructure.aop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.application.annotation.validation.ValidateExcel;
import com.example.demo.application.annotation.validation.ValidatedExcelData;
import com.example.demo.application.port.in.ValidateExcelUseCase;
import com.example.demo.application.shared.context.ExcelValidationContextHolder;
import com.example.demo.application.shared.dto.ExcelValidationGottenResult;
import com.example.demo.application.shared.exception.ExcelValidationFailedException;
import com.example.demo.application.shared.model.ExcelRowError;
import com.example.demo.application.shared.model.OrderModel;

/**
 * Excel 宣告式資料檢核切面單元測試 (ExcelValidationAspect Test)
 * <p>
 * 使用 {@link AspectJProxyFactory} 驗證 {@link ExcelValidationAspect} 切面攔截行為：
 * 包含空檔案阻斷 (400)、違規自動回標短路下載 (200 attachment)、合格自動注入 (@ValidatedExcelData)
 * 與 THROW_EXCEPTION 模式下拋出業務例外。
 * 遵循 BDD / Gherkin (Feature, Scenario, Given-When-Then) 測試註解規範。
 * </p>
 */
@DisplayName("ExcelValidationAspect 宣告式檢核切面測試")
class ExcelValidationAspectTest {

	private ValidateExcelUseCase validateExcelUseCase;
	private SampleUploadController proxy;

	/**
	 * 用於切面攔截驗證的範例 Controller
	 */
	static class SampleUploadController {

		@ValidateExcel(
				model = OrderModel.class,
				action = ValidateExcel.OnErrorAction.AUTO_FEEDBACK,
				feedbackFilename = "AOP回標測試.xlsx"
		)
		public ResponseEntity<?> uploadWithAutoFeedback(
				MultipartFile file,
				@ValidatedExcelData List<OrderModel> validOrders
		) {
			// 若切面阻斷，此方法主體不應被執行；若全數合格，validOrders 應由切面注入
			int count = validOrders != null ? validOrders.size() : 0;
			return ResponseEntity.ok("PROCEEDED_" + count);
		}

		@ValidateExcel(
				model = OrderModel.class,
				action = ValidateExcel.OnErrorAction.THROW_EXCEPTION
		)
		public ResponseEntity<?> uploadWithThrowException(
				MultipartFile file,
				@ValidatedExcelData List<OrderModel> validOrders
		) {
			int count = validOrders != null ? validOrders.size() : 0;
			return ResponseEntity.ok("EXCEPTION_MODE_SUCCESS_" + count);
		}
	}

	@BeforeEach
	void setUp() {
		validateExcelUseCase = mock(ValidateExcelUseCase.class);
		ExcelValidationAspect aspect = new ExcelValidationAspect(validateExcelUseCase);

		// 使用 AspectJProxyFactory 構建切面代理
		AspectJProxyFactory factory = new AspectJProxyFactory(new SampleUploadController());
		factory.addAspect(aspect);
		proxy = factory.getProxy();
	}

	/**
	 * Feature: 上傳空檔案防呆
	 * 
	 * Scenario: 上傳 0 位元組之檔案時，切面直接阻斷並回傳 400 Bad Request
	 *   Given 空的 MockMultipartFile
	 *    When 呼叫被切面攔截的目標方法
	 *    Then 應直接回傳 HTTP 400 Bad Request，不觸發 UseCase 檢核
	 */
	@Test
	@DisplayName("場景：上傳檔案為空時切面應直接回傳 400 Bad Request")
	void shouldReturnBadRequestWhenFileIsEmpty() {
		// Given
		MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.xlsx", "application/vnd.ms-excel", new byte[0]);

		// When
		ResponseEntity<?> response = proxy.uploadWithAutoFeedback(emptyFile, null);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	/**
	 * Feature: AUTO_FEEDBACK 模式下存在錯誤之短路阻斷
	 * 
	 * Scenario: 當資料違規時，切面產製回標 Excel 並直接以二進位下載形式回傳，阻斷 Controller 主體
	 *   Given 含有違規資料之 Excel 檔案
	 *     And UseCase 回傳非空之回標位元組陣列
	 *    When 呼叫被切面攔截的目標方法
	 *    Then 回應狀態碼應為 200 OK
	 *     And Content-Disposition 應包含 "AOP回標測試.xlsx"
	 *     And 目標 Controller 方法主體不應執行 (不應回傳 PROCEEDED_*)
	 */
	@Test
	@DisplayName("場景：AUTO_FEEDBACK 模式違規時切面直接短路回傳錯誤回標 Excel 下載")
	void shouldShortCircuitAndReturnFeedbackFileWhenValidationFails() {
		// Given
		MockMultipartFile file = new MockMultipartFile("file", "orders.xlsx", "application/vnd.ms-excel", new byte[]{1, 2, 3});
		byte[] mockFeedbackExcelBytes = new byte[]{80, 75, 3, 4}; // PK ZIP

		when(validateExcelUseCase.validateAndGenerateFeedback(any(InputStream.class), eq(OrderModel.class)))
				.thenReturn(mockFeedbackExcelBytes);

		// When
		ResponseEntity<?> response = proxy.uploadWithAutoFeedback(file, null);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getHeaders().getContentDisposition().toString()).contains("AOP");
		assertThat(response.getBody()).isEqualTo(mockFeedbackExcelBytes);
	}

	/**
	 * Feature: AUTO_FEEDBACK 模式全數合格之放行與參數自動注入
	 * 
	 * Scenario: 當資料 100% 合格時，切面放行執行 Controller，並自動注入已解析之 List<OrderModel>
	 *   Given 完全合格之 Excel 檔案
	 *     And UseCase 回標方法回傳空 byte 陣列 (無錯誤)
	 *     And UseCase validate 方法回傳包含 2 筆訂單之合格結果
	 *    When 呼叫目標方法
	 *    Then Controller 方法應正常執行並收到切面注入之 2 筆訂單
	 *     And 回應字串應為 "PROCEEDED_2"
	 */
	@Test
	@DisplayName("場景：全數合格時切面放行並自動注入 @ValidatedExcelData 資料清單")
	void shouldProceedAndInjectValidatedDataWhenValidationSucceeds() {
		// Given
		MockMultipartFile file = new MockMultipartFile("file", "valid.xlsx", "application/vnd.ms-excel", new byte[]{1, 2, 3});

		when(validateExcelUseCase.validateAndGenerateFeedback(any(InputStream.class), eq(OrderModel.class)))
				.thenReturn(new byte[0]); // 代表無錯誤

		OrderModel o1 = new OrderModel("ORD-1", "張三", new BigDecimal("100"), 1, LocalDate.now(), "PAID");
		OrderModel o2 = new OrderModel("ORD-2", "李四", new BigDecimal("200"), 2, LocalDate.now(), "PAID");
		ExcelValidationGottenResult<OrderModel> successResult = ExcelValidationGottenResult.success(List.of(o1, o2));

		when(validateExcelUseCase.validate(any(InputStream.class), eq(OrderModel.class)))
				.thenReturn(successResult);

		// When
		ResponseEntity<?> response = proxy.uploadWithAutoFeedback(file, null);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo("PROCEEDED_2");
	}

	/**
	 * Feature: THROW_EXCEPTION 模式下違規拋出例外
	 * 
	 * Scenario: 當設定為 THROW_EXCEPTION 且資料違規時，切面拋出 ExcelValidationFailedException
	 *   Given 含有違規資料之檔案
	 *     And UseCase 回傳包含 1 筆錯誤事實之結果
	 *    When 呼叫目標方法
	 *    Then 切面應拋出 ExcelValidationFailedException，內含全量錯誤清單
	 */
	@Test
	@DisplayName("場景：THROW_EXCEPTION 模式下違規應拋出 ExcelValidationFailedException")
	void shouldThrowExceptionWhenConfiguredAsThrowExceptionAndValidationFails() {
		// Given
		MockMultipartFile file = new MockMultipartFile("file", "invalid.xlsx", "application/vnd.ms-excel", new byte[]{1, 2, 3});
		ExcelRowError error = ExcelRowError.ofCell(2, 2, "金額", "-10", "金額必須大於0", "INVALID_AMOUNT");
		ExcelValidationGottenResult<OrderModel> failedResult = ExcelValidationGottenResult.failed(List.of(), List.of(error), 1);

		when(validateExcelUseCase.validate(any(InputStream.class), eq(OrderModel.class)))
				.thenReturn(failedResult);

		// When & Then
		assertThatThrownBy(() -> proxy.uploadWithThrowException(file, null))
				.isInstanceOf(ExcelValidationFailedException.class)
				.satisfies(ex -> {
					ExcelValidationFailedException validationEx = (ExcelValidationFailedException) ex;
					assertThat(validationEx.getValidationResult().hasErrors()).isTrue();
					assertThat(validationEx.getValidationResult().errorCount()).isEqualTo(1);
				});
	}
}

