package com.example.demo.domain.material.aggregate.vo;

/**
 * 計量單位枚舉 (Unit Of Measure Enumeration)
 * <p>
 * 定義物料之標準計量單位。
 */
public enum UnitOfMeasure {

	/**
	 * 個 / 件
	 */
	PCS("件/個"),

	/**
	 * 套 / 組
	 */
	SET("套/組"),

	/**
	 * 公斤 (Kilogram)
	 */
	KG("公斤"),

	/**
	 * 公尺 (Meter)
	 */
	M("公尺"),

	/**
	 * 公升 (Liter)
	 */
	L("公升"),

	/**
	 * 卷 (Roll)
	 */
	ROLL("卷"),

	/**
	 * 箱 / 盒 (Box)
	 */
	BOX("箱/盒");

	private final String displayName;

	UnitOfMeasure(String displayName) {
		this.displayName = displayName;
	}

	/**
	 * 取得單位中文顯示名稱。
	 *
	 * @return 顯示名稱
	 */
	public String getDisplayName() {
		return displayName;
	}
}

