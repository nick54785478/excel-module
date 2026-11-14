package com.example.demo.domain.material.aggregate.vo;

/**
 * 物料分類枚舉 (Material Category Enumeration)
 * <p>
 * 定義物料在製造與供應鏈系統中的層級類型：
 * <ul>
 *     <li>{@link #FINISHED_GOODS}：成品 (FG)，可對外出貨之終端產品，允許建立 BOM。</li>
 *     <li>{@link #SEMI_FINISHED}：半成品 (SFG)，可由多個原料組裝且可進一步組裝為成品，允許建立 BOM。</li>
 *     <li>{@link #RAW_MATERIAL}：原物料 (RM)，採購之基礎零件，不得作為母件建立 BOM。</li>
 * </ul>
 */
public enum MaterialCategory {

	/**
	 * 成品 (Finished Goods, FG)
	 */
	FINISHED_GOODS("成品", "FG"),

	/**
	 * 半成品 (Semi-Finished Goods, SFG)
	 */
	SEMI_FINISHED("半成品", "SFG"),

	/**
	 * 原物料與零件 (Raw Material / Component, RM)
	 */
	RAW_MATERIAL("原物料", "RM");

	/**
	 * 分類中文顯示名稱 (如 "成品", "半成品", "原物料")
	 */
	private final String displayName;

	/**
	 * 分類代碼縮寫 (如 "FG", "SFG", "RM")
	 */
	private final String code;

	/**
	 * 建構物料分類枚舉項。
	 *
	 * @param displayName 中文顯示名稱
	 * @param code 英文縮寫代碼
	 */
	MaterialCategory(String displayName, String code) {
		this.displayName = displayName;
		this.code = code;
	}

	/**
	 * 取得分類顯示名稱。
	 *
	 * @return 中文顯示名稱
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * 取得分類代碼。
	 *
	 * @return 縮寫代碼
	 */
	public String getCode() {
		return code;
	}

	/**
	 * 判斷該物料分類是否允許作為 BOM 母件。
	 * <p>
	 * 依據業務領域規則，僅成品與半成品具備產品結構並允許建立物料清單。
	 *
	 * @return 若允許建立 BOM 回傳 {@code true}，否則回傳 {@code false}
	 */
	public boolean canHaveBom() {
		return this == FINISHED_GOODS || this == SEMI_FINISHED;
	}
}

