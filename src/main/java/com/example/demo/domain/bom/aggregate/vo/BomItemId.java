package com.example.demo.domain.bom.aggregate.vo;

import java.util.UUID;

/**
 * 物料清單子件唯一識別碼實質物件 (BOM Item Identifier Value Object)
 * <p>
 * 封裝 BOM 明細項目之全域唯一 UUID。
 */
public record BomItemId(String value) {

	/**
	 * 建構並檢核 BOM 明細識別碼。
	 *
	 * @param value 識別碼字串
	 * @throws IllegalArgumentException 當識別碼為空或無效時拋出
	 */
	public BomItemId {
		if (value == null || value.trim().isEmpty()) {
			throw new IllegalArgumentException("BOM 明細識別碼不得為空");
		}
		value = value.trim();
	}

	/**
	 * 生成全域唯一 BOM 明細識別碼。
	 *
	 * @return 新建之 BOM 明細識別碼
	 */
	public static BomItemId generate() {
		return new BomItemId(UUID.randomUUID().toString());
	}

	/**
	 * 依據指定字串建立 BOM 明細識別碼。
	 *
	 * @param value 識別碼字串
	 * @return BOM 明細識別碼實質物件
	 */
	public static BomItemId of(String value) {
		return new BomItemId(value);
	}

	@Override
	public String toString() {
		return value;
	}
}

