package com.example.demo.infrastructure.adapter.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.demo.application.shared.command.WriteSheetCommand;
import com.example.demo.application.shared.model.ReportDecorationContext;
import com.example.demo.application.shared.strategy.ExcelReportDecoratorStrategy;
import com.example.demo.infrastructure.report.DefaultNoOpDecoratorStrategy;
import com.example.demo.infrastructure.report.ProcurementReportDecoratorStrategy;

/**
 * 報表裝飾策略工廠適配器測試 (SpringReportDecoratorFactoryAdapter Test)
 * <p>
 * 遵循 AGENTS.md 規則 63~65 規範，使用 Gherkin (Feature, Scenario, Given-When-Then) 語法進行 BDD 單元測試。
 * 專注驗證工廠的策略動態路由、大小寫不敏感匹配、去除空白與保底無操作策略 (No-Op Strategy) 防禦機制。
 */
@DisplayName("SpringReportDecoratorFactoryAdapter 報表裝飾策略工廠適配器測試")
class SpringReportDecoratorFactoryAdapterTest {

	private SpringReportDecoratorFactoryAdapter factoryAdapter;
	private ProcurementReportDecoratorStrategy procurementStrategy;

	@BeforeEach
	void setUp() {
		this.procurementStrategy = new ProcurementReportDecoratorStrategy();
		this.factoryAdapter = new SpringReportDecoratorFactoryAdapter(List.of(procurementStrategy));
	}

	/**
	 * Feature: 精準與大小寫不敏感鍵值動態路由
	 * Scenario: 當傳入 "PROCUREMENT_ANALYSIS"、小寫 "procurement_analysis" 或帶空白時，正確路由至採購裝飾策略
	 * Given: 已註冊之 ProcurementReportDecoratorStrategy
	 * When: 呼叫 factoryAdapter.getDecorator
	 * Then: 取得 ProcurementReportDecoratorStrategy 實例且 hasDecorator 回傳 true
	 */
	@Test
	@DisplayName("場景：透過標準鍵值與大小寫寬鬆匹配精確取得裝飾策略")
	void shouldRetrieveStrategyWithCaseInsensitiveAndTrimmedKey() {
		// When & Then - 標準大寫
		assertThat(factoryAdapter.hasDecorator("PROCUREMENT_ANALYSIS")).isTrue();
		assertThat(factoryAdapter.getDecorator("PROCUREMENT_ANALYSIS")).isSameAs(procurementStrategy);

		// When & Then - 小寫與空白
		assertThat(factoryAdapter.hasDecorator("  procurement_analysis  ")).isTrue();
		assertThat(factoryAdapter.getDecorator("  procurement_analysis  ")).isSameAs(procurementStrategy);
	}

	/**
	 * Feature: 未匹配策略時之優雅保底機制 (Graceful Fallback)
	 * Scenario: 當傳入未註冊之 Key、null 或空白時，工廠不拋出例外，自動回傳 DefaultNoOpDecoratorStrategy 保底
	 * Given: 隨機未註冊之 Key "UNKNOWN_REPORT" 或 null
	 * When: 呼叫 factoryAdapter.getDecorator
	 * Then: hasDecorator 回傳 false，getDecorator 回傳保底策略，執行 decorate 原樣回傳
	 */
	@Test
	@DisplayName("場景：未註冊或空鍵值時自動退化為保底無操作策略")
	void shouldFallbackToNoOpStrategyWhenKeyNotFoundOrNull() {
		// When & Then - 未知 Key
		assertThat(factoryAdapter.hasDecorator("UNKNOWN_REPORT")).isFalse();
		ExcelReportDecoratorStrategy fallbackStrategy = factoryAdapter.getDecorator("UNKNOWN_REPORT");
		assertThat(fallbackStrategy).isNotNull();
		assertThat(fallbackStrategy.getDecoratorKey()).isEqualTo("DEFAULT");

		// When & Then - null 與空白 Key
		assertThat(factoryAdapter.hasDecorator(null)).isFalse();
		assertThat(factoryAdapter.getDecorator(null)).isNotNull();
		assertThat(factoryAdapter.getDecorator("   ")).isNotNull();

		// 驗證保底策略原樣回傳 baseSheet
		WriteSheetCommand baseSheet = WriteSheetCommand.builder().sheetName("原始工作表").build();
		ReportDecorationContext context = ReportDecorationContext.of("UNKNOWN", "標題", "Pass", "等級", List.of());
		WriteSheetCommand resultSheet = fallbackStrategy.decorate(baseSheet, context);
		assertThat(resultSheet).isSameAs(baseSheet);
	}
}

