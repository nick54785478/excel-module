package com.example.demo.domain.bom.exception;

/**
 * 物料清單未找到領域例外 (BOM Not Found Domain Exception)
 * <p>
 * 遵循命名規範：{@code ... + Exception}。
 * 當查詢之物料清單識別碼 (BomId) 或母件關聯清單不存在於領域邊界時拋出。
 */
public class BomNotFoundException extends RuntimeException {

	/**
	 * 建構 BOM 未找到領域例外。
	 *
	 * @param message 異常說明訊息
	 */
	public BomNotFoundException(String message) {
		super(message);
	}
}

