package com.example.demo.domain.bom.exception;

/**
 * 循環引用領域例外 (Circular BOM Reference Domain Exception)
 * <p>
 * 遵循命名規範：{@code ... + Exception}。
 * 當 BOM 結構中檢測到母件與子件循環依賴（例如 A -> B -> A，或母件將自身加入子件清單）時拋出，
 * 防止造成展開遞迴計算無限迴圈與系統崩潰。
 */
public class CircularBomReferenceException extends RuntimeException {

	/**
	 * 建構循環引用領域例外。
	 *
	 * @param message 異常說明訊息
	 */
	public CircularBomReferenceException(String message) {
		super(message);
	}
}

