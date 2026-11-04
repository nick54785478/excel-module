package com.example.demo.application.shared.vo;

/**
 * Excel 非同步長任務類型列舉實質物件 (Excel Task Type Value Object)
 * <p>
 * 遵循 DDD / Clean Architecture 規範，作為不可變之 Value Object (VO)。
 * 定義長任務之業務分類（匯入解析或匯出產製），無唯一識別碼，以值本身為核心。
 */
public enum ExcelTaskType {

	/**
	 * 匯入解析長任務
	 */
	IMPORT("Excel 匯入解析"),

	/**
	 * 匯出產製長任務
	 */
	EXPORT("Excel 匯出產製");

	private final String displayName;

	ExcelTaskType(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}

