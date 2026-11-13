package com.example.demo.infrastructure.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.demo.application.assembler.ExcelBeanAssembler;
import com.example.demo.application.shared.command.WriteSheetCommand;
import com.example.demo.application.shared.vo.CellRange;
import com.example.demo.application.shared.model.report.ProcurementAnalysisReportModel;
import com.example.demo.application.shared.context.ReportDecorationContext;

/**
 * 物料採購損益月報裝飾策略測試 (ProcurementReportDecoratorStrategy Test)
 * <p>
 * 遵循 AGENTS.md 規則 63~65 規範，使用 Gherkin (Feature, Scenario, Given-When-Then) 語法進行 BDD 單元測試。
 * 專注驗證微觀排版算術、合計彙總列追加、動態加總 SUM 公式注入、跨欄合併與安全防護配置。
 */
@DisplayName("ProcurementReportDecoratorStrategy 物料採購損益裝飾策略測試")
class ProcurementReportDecoratorStrategyTest {

	private ProcurementReportDecoratorStrategy strategy;

	@BeforeEach
	void setUp() {
		this.strategy = new ProcurementReportDecoratorStrategy();
	}

	/**
	 * Feature: 策略唯一識別鍵驗證
	 * Scenario: 驗證策略綁定之 Key 為 "PROCUREMENT_ANALYSIS"
	 * When: 呼叫 strategy.getDecoratorKey()
	 * Then: 回傳 "PROCUREMENT_ANALYSIS"
	 */
	@Test
	@DisplayName("場景：驗證策略綁定之標準識別鍵")
	void shouldReturnCorrectDecoratorKey() {
		assertThat(strategy.getDecoratorKey()).isEqualTo("PROCUREMENT_ANALYSIS");
	}

	/**
	 * Feature: 物料採購損益報表排版裝飾
	 * Scenario: 傳入 2 筆物料明細，驗證合計列自動追加至第 4 列 (0-based index 3)、A~D 欄跨欄合併與 SUM 公式注入
	 * Given: 建立 2 筆物料明細模型清單，透過 ExcelBeanAssembler 產出基礎 WriteSheetCommand
	 * When: 呼叫 strategy.decorate(baseSheet, context)
	 * Then: 
	 *   1. rows 總列數增加 1 列合計列 (總列數為 3)
	 *   2. 合計列文字為 "合計彙總"
	 *   3. 合計欄位注入動態公式 "=SUM(E2:E3)" 且位於 "E4"
	 *   4. 合併區間包含 CellRange(3, 3, 0, 3)
	 *   5. 工作表防護密碼與列印頁首頁尾設置正確
	 */
	@Test
	@DisplayName("場景：成功對 2 筆資料的工作表裝飾合計列、SUM公式、跨欄合併與安全防護")
	void shouldDecorateSheetWithSummaryRowFormulasAndMergingSuccessfully() {
		// Given
		List<ProcurementAnalysisReportModel> items = List.of(
				new ProcurementAnalysisReportModel("MAT-01", "碳纖機臂", 10, new BigDecimal("1200.00"), null, new BigDecimal("1800.00"), null),
				new ProcurementAnalysisReportModel("MAT-02", "無刷電調", 20, new BigDecimal("450.00"), null, new BigDecimal("700.00"), null)
		);
		WriteSheetCommand baseSheet = ExcelBeanAssembler.toSheetCommand(items, ProcurementAnalysisReportModel.class, "採購損益分析");
		ReportDecorationContext context = ReportDecorationContext.of(
				ProcurementReportDecoratorStrategy.STRATEGY_KEY,
				"2026年Q4採購損益表",
				"SecurePass123",
				"內部絕密",
				items
		);

		// When
		WriteSheetCommand decoratedSheet = strategy.decorate(baseSheet, context);

		// Then
		assertThat(decoratedSheet).isNotNull();

		// 1. 驗證資料列數：2 筆明細 + 1 筆合計列 = 3 列
		assertThat(decoratedSheet.rows()).hasSize(3);
		List<Object> summaryRow = decoratedSheet.rows().get(2);
		assertThat(summaryRow.get(0)).isEqualTo("合計彙總");

		// 2. 驗證合計列索引 (0-based 為 3，即第 4 列) 的 SUM 公式
		assertThat(decoratedSheet.cellFormulas()).containsKey("E4");
		assertThat(decoratedSheet.cellFormulas().get("E4")).isEqualTo("SUM(E2:E3)");

		// 3. 驗證合計列跨欄合併 A~D 欄 (col 0 ~ 3)
		assertThat(decoratedSheet.mergedRegions()).contains(new CellRange(3, 3, 0, 3));

		// 4. 驗證工作表安全防護與密碼
		assertThat(decoratedSheet.protectionConfig()).isNotNull();
		assertThat(decoratedSheet.protectionConfig().password()).isEqualTo("SecurePass123");

		// 5. 驗證列印頁首頁尾標記
		assertThat(decoratedSheet.headerFooterConfig()).isNotNull();
		assertThat(decoratedSheet.headerFooterConfig().centerHeader()).isEqualTo("2026年Q4採購損益表");
		assertThat(decoratedSheet.headerFooterConfig().leftFooter()).contains("內部絕密");
	}
}

