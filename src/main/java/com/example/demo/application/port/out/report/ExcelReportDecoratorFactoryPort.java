package com.example.demo.application.port.out.report;

import com.example.demo.application.shared.strategy.ExcelReportDecoratorStrategy;

/**
 * 企業級報表裝飾策略工廠輸出埠 (Excel Report Decorator Factory Outbound Port)
 * <p>
 * 遵循 AGENTS.md 3-8 規範（Outbound Port 一律以 {@code Port} 結尾）。
 * 屬於 Clean Architecture 六角形架構應用層對外宣告之工廠合約。
 * 負責依據報表識別鍵 (reportKey / styleKey) 動態獲取對應的 {@link ExcelReportDecoratorStrategy} 裝飾策略實例。
 */
public interface ExcelReportDecoratorFactoryPort {

	/**
	 * 依據報表識別鍵獲取對應之裝飾策略實例
	 * <p>
	 * 若未找到精確匹配之策略，應自動回傳保底無操作策略 (No-Op Strategy)，確保系統高可用性且不拋出未捕獲例外。
	 *
	 * @param reportKey 報表標識鍵 (如 "PROCUREMENT_ANALYSIS")
	 * @return 對應之裝飾策略實例 (保證不為 null)
	 */
	ExcelReportDecoratorStrategy getDecorator(String reportKey);

	/**
	 * 檢查系統是否已註冊特定 Key 之專屬裝飾策略
	 *
	 * @param reportKey 報表標識鍵
	 * @return 若已註冊專屬策略回傳 true，否則回傳 false
	 */
	boolean hasDecorator(String reportKey);
}

