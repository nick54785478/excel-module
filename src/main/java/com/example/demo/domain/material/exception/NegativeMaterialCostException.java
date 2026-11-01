package com.example.demo.domain.material.exception;

/**
 * 負數物料成本領域例外 (Negative Material Cost Domain Exception)
 * <p>
 * 遵循命名規範：{@code ... + Exception}。
 * 當傳入之物料標準成本小於零時拋出，確保領域實質物件數值之完整性與有效性。
 */
public class NegativeMaterialCostException extends RuntimeException {

	/**
	 * 建構負數物料成本領域例外。
	 *
	 * @param message 異常說明訊息
	 */
	public NegativeMaterialCostException(String message) {
		super(message);
	}
}

