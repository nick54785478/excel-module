package com.example.demo.domain.material.aggregate.vo;

import java.util.Objects;
import java.util.UUID;

/**
 * 物料唯一識別碼實質物件 (Material Identifier Value Object)
 * <p>
 * 封裝物料之全域唯一 UUID，具備不變性與自主檢核邏輯。
 *
 * @param value 物料全域唯一識別碼字串 (UUID 格式)
 */
public record MaterialId(String value) {

	/**
	 * 建構並檢核物料識別碼。
	 *
	 * @param value 識別碼字串，不得為 null 或空白
	 * @throws IllegalArgumentException 當識別碼為空或無效時拋出
	 */
	public MaterialId {
		if (value == null || value.trim().isEmpty()) {
			throw new IllegalArgumentException("物料識別碼不得為空");
		}
		value = value.trim();
	}

	/**
	 * 生成隨機全域唯一物料識別碼。
	 *
	 * @return 新建之物料識別碼實質物件
	 */
	public static MaterialId generate() {
		return new MaterialId(UUID.randomUUID().toString());
	}

	/**
	 * 依據指定字串建立物料識別碼。
	 *
	 * @param value 識別碼字串
	 * @return 物料識別碼實質物件
	 */
	public static MaterialId of(String value) {
		return new MaterialId(value);
	}

	/**
	 * 取得識別碼字串表達形式。
	 *
	 * @return 識別碼原始字串
	 */
	@Override
	public String toString() {
		return value;
	}
}

