package com.example.demo.domain.bom.exception;

/**
 * 非法 BOM 操作領域例外 (Illegal BOM Operation Domain Exception)
 * <p>
 * 遵循命名規範：{@code ... + Exception}。
 * 當在不允許之 BOM 狀態下執行異動操作（例如已審核 APPROVED 狀態下嘗試新增或刪除子件）時拋出。
 */
public class IllegalBomOperationException extends RuntimeException {

	/**
	 * 建構非法 BOM 操作領域例外。
	 *
	 * @param message 異常說明訊息
	 */
	public IllegalBomOperationException(String message) {
		super(message);
	}
}

