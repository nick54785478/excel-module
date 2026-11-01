package com.example.demo.domain.bom.exception;

/**
 * 母件物料分類無效領域例外 (Invalid BOM Parent Category Domain Exception)
 * <p>
 * 遵循命名規範：{@code ... + Exception}。
 * 當嘗試為非成品或非半成品（例如原物料 RM）建立 BOM 配方清單時拋出。
 */
public class InvalidBomParentCategoryException extends RuntimeException {

	/**
	 * 建構母件物料分類無效領域例外。
	 *
	 * @param message 異常說明訊息
	 */
	public InvalidBomParentCategoryException(String message) {
		super(message);
	}
}

