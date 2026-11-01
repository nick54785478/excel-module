package com.example.demo.domain.material.exception;

/**
 * 物料未找到領域例外 (Material Not Found Domain Exception)
 * <p>
 * 遵循命名規範：{@code ... + Exception}。
 * 當查詢之物料識別碼或料號不存在於領域邊界內時拋出。
 */
public class MaterialNotFoundException extends RuntimeException {

	public MaterialNotFoundException(String message) {
		super(message);
	}
}

