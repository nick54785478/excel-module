package com.example.demo.domain.bom.aggregate.vo;

import java.util.UUID;

/**
 * 物料清單唯一識別碼實質物件 (BOM Identifier Value Object)
 * <p>
 * 封裝 BOM 之全域唯一 UUID，具備不變性與自主檢核邏輯。
 */
public record BomId(String value) {

	/**
	 * 建構並檢核 BOM 識別碼。
	 *
	 * @param value 識別碼字串
	 * @throws IllegalArgumentException 當識別碼為空或無效時拋出
	 */
	public BomId {
		if (value == null || value.trim().isEmpty()) {
			throw new IllegalArgumentException("BOM 識別碼不得為空");
		}
		value = value.trim();
	}

	/**
	 * 生成全域唯一 BOM 識別碼。
	 *
	 * @return 新建之 BOM 識別碼
	 */
	public static BomId generate() {
		return new BomId(UUID.randomUUID().toString());
	}

	/**
	 * 依據指定字串建立 BOM 識別碼。
	 *
	 * @param value 識別碼字串
	 * @return BOM 識別碼實質物件
	 */
	public static BomId of(String value) {
		return new BomId(value);
	}

	@Override
	public String toString() {
		return value;
	}
}

