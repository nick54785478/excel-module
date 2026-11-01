package com.example.demo.application.shared.exception;

/**
 * Excel 讀取與解析例外 (Excel Read Exception)
 * <p>
 * 當底層 FastExcel 讀取、串流解析或檔案結構損毀時拋出此例外，
 * 封裝技術細節，保證應用層不直接暴露第三方 Checked Exception。
 */
public class ExcelReadException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * 建構 Excel 讀取例外
	 *
	 * @param message 錯誤訊息
	 */
	public ExcelReadException(String message) {
		super(message);
	}

	/**
	 * 建構包含底層原因之 Excel 讀取例外
	 *
	 * @param message 錯誤訊息
	 * @param cause   底層異常原因
	 */
	public ExcelReadException(String message, Throwable cause) {
		super(message, cause);
	}
}
