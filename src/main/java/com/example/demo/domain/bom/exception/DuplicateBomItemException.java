package com.example.demo.domain.bom.exception;

/**
 * 重複子料件領域例外 (Duplicate BOM Item Domain Exception)
 * <p>
 * 遵循命名規範：{@code ... + Exception}。
 * 當在同一個 BOM 結構中嘗試加入已存在之子料件時拋出，確保子件唯一性。
 */
public class DuplicateBomItemException extends RuntimeException {

	/**
	 * 建構重複子料件領域例外。
	 *
	 * @param message 異常說明訊息
	 */
	public DuplicateBomItemException(String message) {
		super(message);
	}
}

