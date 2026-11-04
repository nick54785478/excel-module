package com.example.demo.presentation.exception;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import com.example.demo.application.shared.exception.ExcelReadException;
import com.example.demo.application.shared.exception.ExcelTaskNotFoundException;
import com.example.demo.application.shared.exception.ExcelWriteException;
import com.example.demo.domain.material.exception.MaterialNotFoundException;
import com.example.demo.presentation.resource.out.ValidationFailedResource;
import com.example.demo.presentation.resource.out.ValidationFailedResource.FieldErrorResource;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

/**
 * 表現層全域例外處理器 (Presentation Global Exception Handler)
 * <p>
 * 遵循 Clean Architecture 表現層職責，集中攔截 HTTP 請求與業務處理過程中拋出的例外。
 * 特別處理 Spring Validation 檢核失敗與業務例外，統一封裝為結構化的 {@link ValidationFailedResource}
 * 回應。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * 攔截 {@code @Valid} 請求主體 (RequestBody) 參數校驗失敗例外
	 *
	 * @param ex 參數校驗例外
	 * @return 400 Bad Request 結構化錯誤回應
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ValidationFailedResource> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
		List<FieldErrorResource> fieldErrors = new ArrayList<>();
		for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
			fieldErrors.add(new FieldErrorResource(
					fieldError.getField(),
					fieldError.getRejectedValue(),
					fieldError.getDefaultMessage()));
		}

		ValidationFailedResource response = new ValidationFailedResource(
				LocalDateTime.now(),
				HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				"請求主體資料校驗失敗，請檢查輸入參數",
				fieldErrors);

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}

	/**
	 * 攔截 Spring 6+ / Spring Boot 3+ 方法參數級別校驗例外 (HandlerMethodValidationException)
	 *
	 * @param ex 方法參數校驗例外
	 * @return 400 Bad Request 結構化錯誤回應
	 */
	@ExceptionHandler(HandlerMethodValidationException.class)
	public ResponseEntity<ValidationFailedResource> handleHandlerMethodValidation(HandlerMethodValidationException ex) {
		List<FieldErrorResource> fieldErrors = new ArrayList<>();
		ex.getParameterValidationResults().forEach(result -> {
			String paramName = result.getMethodParameter().getParameterName();
			result.getResolvableErrors().forEach(error -> {
				fieldErrors.add(new FieldErrorResource(
						paramName != null ? paramName : "parameter",
						null,
						error.getDefaultMessage()));
			});
		});

		ValidationFailedResource response = new ValidationFailedResource(
				LocalDateTime.now(),
				HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				"請求參數校驗失敗",
				fieldErrors);

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}

	/**
	 * 攔截 Jakarta Bean Validation 約束違反例外 (ConstraintViolationException)
	 *
	 * @param ex 約束違反例外
	 * @return 400 Bad Request 結構化錯誤回應
	 */
	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ValidationFailedResource> handleConstraintViolation(ConstraintViolationException ex) {
		List<FieldErrorResource> fieldErrors = new ArrayList<>();
		for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
			fieldErrors.add(new FieldErrorResource(
					violation.getPropertyPath().toString(),
					violation.getInvalidValue(),
					violation.getMessage()));
		}

		ValidationFailedResource response = new ValidationFailedResource(
				LocalDateTime.now(),
				HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				"約束規則校驗失敗",
				fieldErrors);

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}

	/**
	 * 攔截非法參數例外 (IllegalArgumentException)
	 *
	 * @param ex 非法參數例外
	 * @return 400 Bad Request 結構化錯誤回應
	 */
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ValidationFailedResource> handleIllegalArgument(IllegalArgumentException ex) {
		List<FieldErrorResource> fieldErrors = List.of(
				new FieldErrorResource("request", null, ex.getMessage()));

		ValidationFailedResource response = new ValidationFailedResource(
				LocalDateTime.now(),
				HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				ex.getMessage(),
				fieldErrors);

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}

	/**
	 * 攔截 Excel 讀取業務例外
	 *
	 * @param ex Excel 讀取例外
	 * @return 400 Bad Request 結構化錯誤回應
	 */
	@ExceptionHandler(ExcelReadException.class)
	public ResponseEntity<ValidationFailedResource> handleExcelRead(ExcelReadException ex) {
		List<FieldErrorResource> fieldErrors = List.of(
				new FieldErrorResource("excelRead", null, ex.getMessage()));

		ValidationFailedResource response = new ValidationFailedResource(
				LocalDateTime.now(),
				HttpStatus.BAD_REQUEST.value(),
				"Excel Read Error",
				ex.getMessage(),
				fieldErrors);

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}

	/**
	 * 攔截 Excel 寫入業務例外
	 *
	 * @param ex Excel 寫入例外
	 * @return 400 Bad Request 結構化錯誤回應
	 */
	@ExceptionHandler(ExcelWriteException.class)
	public ResponseEntity<ValidationFailedResource> handleExcelWrite(ExcelWriteException ex) {
		List<FieldErrorResource> fieldErrors = List.of(
				new FieldErrorResource("excelWrite", null, ex.getMessage()));

		ValidationFailedResource response = new ValidationFailedResource(
				LocalDateTime.now(),
				HttpStatus.BAD_REQUEST.value(),
				"Excel Write Error",
				ex.getMessage(),
				fieldErrors);

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}

	/**
	 * 攔截查無物料主檔之領域例外
	 *
	 * @param ex 物料不存在例外
	 * @return 404 Not Found 結構化錯誤回應
	 */
	@ExceptionHandler(MaterialNotFoundException.class)
	public ResponseEntity<ValidationFailedResource> handleMaterialNotFound(
			MaterialNotFoundException ex
	) {
		List<FieldErrorResource> fieldErrors = List.of(
				new FieldErrorResource("parentMaterialCode", null, ex.getMessage()));

		ValidationFailedResource response = new ValidationFailedResource(
				LocalDateTime.now(),
				HttpStatus.NOT_FOUND.value(),
				HttpStatus.NOT_FOUND.getReasonPhrase(),
				ex.getMessage(),
				fieldErrors);

		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
	}

	/**
	 * 攔截查無 Excel 長任務之業務例外
	 *
	 * @param ex 任務不存在例外
	 * @return 404 Not Found 結構化錯誤回應
	 */
	@ExceptionHandler(ExcelTaskNotFoundException.class)
	public ResponseEntity<ValidationFailedResource> handleExcelTaskNotFound(
			ExcelTaskNotFoundException ex
	) {
		List<FieldErrorResource> fieldErrors = List.of(
				new FieldErrorResource("taskId", null, ex.getMessage()));

		ValidationFailedResource response = new ValidationFailedResource(
				LocalDateTime.now(),
				HttpStatus.NOT_FOUND.value(),
				HttpStatus.NOT_FOUND.getReasonPhrase(),
				ex.getMessage(),
				fieldErrors);

		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
	}
}
