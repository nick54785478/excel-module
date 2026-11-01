package com.example.demo.application.shared.exception;

/**
 * Excel 寫入操作例外 (Excel Write Exception)
 * <p>
 * 當底層 FastExcel 串流寫入、格式化或 I/O 發生異常時拋出此例外，
 * 封裝底層技術細節，避免底層 Checked Exception 直接侵入業務層。
 */
public class ExcelWriteException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * 建構 Excel 寫入例外
	 *
	 * @param message 錯誤訊息
	 */
	public ExcelWriteException(String message) {
		super(message);
	}

	/**
	 * 建構包含底層原因之 Excel 寫入例外
	 *
	 * @param message 錯誤訊息
	 * @param cause   底層異常原因
	 */
	public ExcelWriteException(String message, Throwable cause) {
		super(message, cause);
	}
}
