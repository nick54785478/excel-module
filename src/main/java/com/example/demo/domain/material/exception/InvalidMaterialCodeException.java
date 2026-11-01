package com.example.demo.domain.material.exception;

/**
 * 無效物料料號領域例外 (Invalid Material Code Domain Exception)
 * <p>
 * 遵循命名規範：{@code ... + Exception}。
 * 當傳入之物料料號格式不符合系統規範（如長度不足、含有非法特殊字元或為空值）時拋出。
 */
public class InvalidMaterialCodeException extends RuntimeException {

	/**
	 * 建構無效物料料號領域例外。
	 *
	 * @param message 異常說明訊息
	 */
	public InvalidMaterialCodeException(String message) {
		super(message);
	}
}

