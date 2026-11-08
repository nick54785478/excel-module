package com.example.demo.infrastructure.adapter.report;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.demo.application.port.out.ExcelReportDecoratorFactoryPort;
import com.example.demo.application.shared.strategy.ExcelReportDecoratorStrategy;
import com.example.demo.infrastructure.report.DefaultNoOpDecoratorStrategy;

/**
 * Spring 容器自動收集型報表裝飾策略工廠適配器 (Spring Report Decorator Factory Adapter)
 * <p>
 * 遵循 AGENTS.md 3-9 規範（Outbound Adapter 一律以 {@code Adapter} 結尾）。
 * 實作應用層 {@link ExcelReportDecoratorFactoryPort} 輸出埠。
 * 於 Spring 容器啟動時，透過建構子自動收集所有註冊之 {@link ExcelReportDecoratorStrategy} 策略 Bean，
 * 建立內部執行緒安全之索引映射表，支援執行期 O(1) 動態鍵值路由。
 */
public class SpringReportDecoratorFactoryAdapter implements ExcelReportDecoratorFactoryPort {

	private static final Logger log = LoggerFactory.getLogger(SpringReportDecoratorFactoryAdapter.class);

	private final Map<String, ExcelReportDecoratorStrategy> strategyMap;
	private final ExcelReportDecoratorStrategy fallbackStrategy;

	/**
	 * 透過 Spring 建構子自動注入容器中所有註冊之裝飾策略 Bean
	 *
	 * @param strategies 容器中所有宣告為 Spring Bean 的裝飾策略實例清單
	 */
	public SpringReportDecoratorFactoryAdapter(List<ExcelReportDecoratorStrategy> strategies) {
		this.fallbackStrategy = new DefaultNoOpDecoratorStrategy();
		this.strategyMap = new ConcurrentHashMap<>();

		if (strategies != null) {
			for (ExcelReportDecoratorStrategy strategy : strategies) {
				if (strategy != null && strategy.getDecoratorKey() != null) {
					String key = normalizeKey(strategy.getDecoratorKey());
					this.strategyMap.put(key, strategy);
					log.info("[ReportDecoratorFactory] Registered report decorator strategy for key: '{}' -> {}",
							key, strategy.getClass().getSimpleName());
				}
			}
		}
	}

	@Override
	public ExcelReportDecoratorStrategy getDecorator(String reportKey) {
		if (reportKey == null || reportKey.isBlank()) {
			return fallbackStrategy;
		}
		return strategyMap.getOrDefault(normalizeKey(reportKey), fallbackStrategy);
	}

	@Override
	public boolean hasDecorator(String reportKey) {
		if (reportKey == null || reportKey.isBlank()) {
			return false;
		}
		return strategyMap.containsKey(normalizeKey(reportKey));
	}

	/**
	 * 統一將鍵值去除首尾空白並轉為大寫，達成大小寫不敏感之寬鬆匹配
	 */
	private String normalizeKey(String key) {
		return key.trim().toUpperCase();
	}
}
