package com.example.demo.application.shared.vo;

/**
 * Excel 非同步長任務執行狀態實質物件 (Excel Task Execution Status Value Object)
 * <p>
 * 遵循 DDD / Clean Architecture 規範，作為不可變之 Value Object (VO)。
 * 定義長任務於生命週期中的各個演進狀態，無唯一識別碼，以值本身為核心。
 */
public enum ExcelTaskStatus {

	/**
	 * 任務已建立，排隊等待背景執行緒提取
	 */
	PENDING("排隊中"),

	/**
	 * 背景執行緒已啟動，正在處理串流讀寫
	 */
	PROCESSING("處理中"),

	/**
	 * 任務已成功處理完畢
	 */
	COMPLETED("已完成"),

	/**
	 * 任務於處理過程中發生異常中斷
	 */
	FAILED("失敗"),

	/**
	 * 任務已由呼叫方或系統主動取消
	 */
	CANCELLED("已取消");

	private final String displayName;

	ExcelTaskStatus(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}

