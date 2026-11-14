package com.example.demo.domain.bom.aggregate.vo;

import java.util.regex.Pattern;

/**
 * 物料清單版本號實質物件 (BOM Version Value Object)
 * <p>
 * 定義產品 BOM 之工程版本識別（例如 V1.0、V2.0），格式以 V 開頭並由主次版本號構成。
 *
 * @param value BOM 工程版本號字串 (例如 V1.0, V2.1)
 */
public record BomVersion(String value) {

	/**
	 * 版本號格式驗證正則表示式 (以 V 開頭，後接數字與點分隔次版本號)
	 */
	private static final Pattern VERSION_PATTERN = Pattern.compile("^V[0-9]+(\\.[0-9]+)*$", Pattern.CASE_INSENSITIVE);

	/**
	 * 預設初始版本號 (V1.0)
	 */
	public static final BomVersion DEFAULT = new BomVersion("V1.0");

	/**
	 * 建構並檢核 BOM 版本號。
	 *
	 * @param value 版本號字串，不可為 null 且需符合正規表示式規格
	 * @throws IllegalArgumentException 當版本號為空或格式不符時拋出
	 */
	public BomVersion {
		if (value == null || value.trim().isEmpty()) {
			throw new IllegalArgumentException("BOM 版本號不得為空");
		}
		value = value.trim().toUpperCase();
		if (!VERSION_PATTERN.matcher(value).matches()) {
			throw new IllegalArgumentException("BOM 版本號格式無效 [" + value + "]，必須符合例如 'V1.0' 之格式");
		}
	}

	/**
	 * 依據指定字串建立 BOM 版本號。
	 *
	 * @param value 版本號字串
	 * @return BOM 版本號實質物件
	 */
	public static BomVersion of(String value) {
		return new BomVersion(value);
	}

	/**
	 * 取得版本號字串表達形式。
	 *
	 * @return 版本號原始字串
	 */
	@Override
	public String toString() {
		return value;
	}
}

