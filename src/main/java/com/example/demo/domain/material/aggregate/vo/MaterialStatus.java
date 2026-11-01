package com.example.demo.domain.material.aggregate.vo;

/**
 * 物料生命週期狀態枚舉 (Material Status Enumeration)
 * <p>
 * 定義物料之啟用與停用狀態。
 */
public enum MaterialStatus {

	/**
	 * 正常使用中 (Active)
	 */
	ACTIVE("正常使用中"),

	/**
	 * 已停用 / 已報廢 (Discontinued)
	 */
	DISCONTINUED("已停用");

	private final String displayName;

	MaterialStatus(String displayName) {
		this.displayName = displayName;
	}

	/**
	 * 取得狀態顯示名稱。
	 *
	 * @return 中文顯示名稱
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * 檢查物料是否為可用狀態。
	 *
	 * @return 若正常啟用回傳 {@code true}，否則回傳 {@code false}
	 */
	public boolean isActive() {
		return this == ACTIVE;
	}
}

