package com.example.demo.application.shared.strategy;

/**
 * 企業級報表裝飾策略識別鍵常數 (Excel Report Decorator Strategy Keys)
 * <p>
 * <b>【架構定位與職責規範】</b><br>
 * 位於 Clean Architecture 應用層共享策略套件 ({@code application/shared/strategy/})。<br>
 * 統一管理所有系統支援之報表裝飾策略識別鍵值，作為表現層、應用層與策略工廠之間的標準合約鍵。<br>
 * 消除應用層對基礎設施層具體策略類別之直接編譯期依賴，維持極致純淨的依賴倒置原則 (DIP)。
 */
public final class ExcelReportStrategyKeys {

	/**
	 * 物料採購損益分析月報策略識別鍵
	 */
	public static final String PROCUREMENT_ANALYSIS = "PROCUREMENT_ANALYSIS";

	/**
	 * 產品結構配方與標準成本卷算分析策略識別鍵
	 */
	public static final String BOM_COST_ANALYSIS = "BOM_COST_ANALYSIS";

	/**
	 * 預設無操作保底策略識別鍵
	 */
	public static final String DEFAULT = "DEFAULT";

	private ExcelReportStrategyKeys() {
		// 常數工具類別禁止實例化
	}
}

