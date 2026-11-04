package com.example.demo.application.shared.exception;

/**
 * 查無 Excel 長任務例外 (Excel Task Not Found Exception)
 * <p>
 * 當查詢不存在或無效之 taskId 時拋出此例外。
 */
public class ExcelTaskNotFoundException extends RuntimeException {

	public ExcelTaskNotFoundException(String message) {
		super(message);
	}

	public ExcelTaskNotFoundException(String taskId, Throwable cause) {
		super("查無指定之 Excel 長任務識別碼: " + taskId, cause);
	}

	public static ExcelTaskNotFoundException of(String taskId) {
		return new ExcelTaskNotFoundException("查無指定之 Excel 長任務識別碼: " + taskId);
	}
}

