package com.example.demo.domain.material.aggregate.vo;

import java.util.regex.Pattern;
import com.example.demo.domain.material.exception.InvalidMaterialCodeException;

/**
 * 物料料號實質物件 (Material Code Value Object)
 * <p>
 * 企業內部物料之標準編碼，遵循統一編碼格式規範（大寫英文字母、數字、減號與底線，長度介於 3 至 50 字元）。
 */
public record MaterialCode(String value) {

	private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z0-9_-]{3,50}$");

	/**
	 * 建構並檢核物料料號。
	 *
	 * @param value 料號字串
	 * @throws InvalidMaterialCodeException 當格式不符時拋出
	 */
	public MaterialCode {
		if (value == null || value.trim().isEmpty()) {
			throw new InvalidMaterialCodeException("物料料號不得為空");
		}
		value = value.trim().toUpperCase();
		if (!CODE_PATTERN.matcher(value).matches()) {
			throw new InvalidMaterialCodeException(
					"物料料號格式無效 [" + value + "]，必須為 3 至 50 字元之大寫英文、數字、減號或底線組合"
			);
		}
	}

	/**
	 * 依據指定字串建立物料料號。
	 *
	 * @param value 料號字串
	 * @return 物料料號實質物件
	 */
	public static MaterialCode of(String value) {
		return new MaterialCode(value);
	}

	@Override
	public String toString() {
		return value;
	}
}

