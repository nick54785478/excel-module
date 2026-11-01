package com.example.demo.domain.bom.aggregate.vo;

/**
 * 物料清單生命週期狀態枚舉 (BOM Status Enumeration)
 * <p>
 * 定義 BOM 清單之發行與審核狀態：
 * <ul>
 *     <li>{@link #DRAFT}：草稿，允許新增、移除與修改子件。</li>
 *     <li>{@link #APPROVED}：已審核生效，結構進入凍結（Frozen）狀態，不得再進行子件異動。</li>
 *     <li>{@link #OBSOLETE}：已作廢，舊版或已停用配方。</li>
 * </ul>
 */
public enum BomStatus {

	/**
	 * 草稿 (Draft)
	 */
	DRAFT("草稿"),

	/**
	 * 已審核生效 (Approved)
	 */
	APPROVED("已審核"),

	/**
	 * 已廢止 (Obsolete)
	 */
	OBSOLETE("已廢止");

	private final String displayName;

	BomStatus(String displayName) {
		this.displayName = displayName;
	}

	/**
	 * 取得狀態顯示名稱。
	 *
	 * @return 中文名稱
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * 是否為草稿編輯中狀態。
	 *
	 * @return 若為草稿回傳 {@code true}
	 */
	public boolean isDraft() {
		return this == DRAFT;
	}

	/**
	 * 是否為已審核生效狀態。
	 *
	 * @return 若已審核回傳 {@code true}
	 */
	public boolean isApproved() {
		return this == APPROVED;
	}

	/**
	 * 是否為已作廢狀態。
	 *
	 * @return 若已作廢回傳 {@code true}
	 */
	public boolean isObsolete() {
		return this == OBSOLETE;
	}
}

