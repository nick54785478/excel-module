package com.example.demo.infrastructure.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.demo.application.assembler.ExcelBeanAssembler;
import com.example.demo.application.shared.command.WriteSheetCommand;
import com.example.demo.application.shared.model.BomCostAnalysisReportModel;
import com.example.demo.application.shared.model.CellRange;
import com.example.demo.application.shared.model.ReportDecorationContext;

/**
 * 產品結構配方與標準成本分析裝飾策略測試 (BomCostAnalysisReportDecoratorStrategy Test)
 * <p>
 * 遵循 AGENTS.md 規則 63~65 規範，使用 Gherkin (Feature, Scenario, Given-When-Then) 語法進行 BDD 單元測試。
 * 專注驗證微觀排版算術、合計彙總列追加、動態加總 SUM 公式注入、跨欄合併、工作表防護與企業頁首頁尾。
 */
@DisplayName("BomCostAnalysisReportDecoratorStrategy BOM 成本分析裝飾策略測試")
class BomCostAnalysisReportDecoratorStrategyTest {

	private BomCostAnalysisReportDecoratorStrategy strategy;

	@BeforeEach
	void setUp() {
		this.strategy = new BomCostAnalysisReportDecoratorStrategy();
	}

	/**
	 * Feature: 策略唯一識別鍵驗證
	 * Scenario: 驗證策略綁定之 Key 為 "BOM_COST_ANALYSIS"
	 * When: 呼叫 strategy.getDecoratorKey()
	 * Then: 回傳 "BOM_COST_ANALYSIS"
	 */
	@Test
	@DisplayName("場景：驗證策略綁定之標準識別鍵")
	void shouldReturnCorrectDecoratorKey() {
		assertThat(strategy.getDecoratorKey()).isEqualTo("BOM_COST_ANALYSIS");
	}

	/**
	 * Feature: 產品結構配方與成本分析排版裝飾
	 * Scenario: 傳入 2 筆 BOM 子件明細，驗證合計列自動追加至第 4 列 (0-based index 3)、A~K 欄跨欄合併與 SUM 公式注入
	 * Given: 建立 2 筆 BOM 成本明細模型清單，透過 ExcelBeanAssembler 產出基礎 WriteSheetCommand
	 * When: 呼叫 strategy.decorate(baseSheet, context)
	 * Then:
	 *   1. rows 總列數增加 1 列合計列 (總列數為 3，包含 2 筆明細 + 1 筆合計)
	 *   2. 合計列文字包含母件資訊與「【整機 BOM 標準總成本合計】」
	 *   3. 合計欄位注入動態加總公式 "=SUM(L2:L3)" 且位於 "L4"
	 *   4. 合併區間包含 CellRange(3, 3, 0, 10) (跨 A 到 K 欄)
	 *   5. 工作表防護密碼與列印頁首頁尾設置正確
	 *   6. 表頭背景色為 "1B365D"，文字為白色，啟用凍結首列
	 */
	@Test
	@DisplayName("場景：成功對 2 筆資料的工作表裝飾合計列、SUM公式、A~K跨欄合併與安全防護")
	void shouldDecorateSheetWithSummaryRowFormulasAndMergingSuccessfully() {
		// Given
		List<BomCostAnalysisReportModel> items = List.of(
				new BomCostAnalysisReportModel(1, 10, "MAT-SFG-001", "飛控主板電路總成", "STM32H743", "半成品", "套/組",
						new BigDecimal("1.00"), new BigDecimal("0.01"), null, new BigDecimal("4800.00"), null, "FC-ASM", "核心飛控模組"),
				new BomCostAnalysisReportModel(1, 20, "MAT-SFG-002", "2212 動力推進總成", "920KV 碳纖快拆", "半成品", "套/組",
						new BigDecimal("4.00"), new BigDecimal("0.00"), null, new BigDecimal("680.00"), null, "PROP-1~4", "動力系統")
		);
		WriteSheetCommand baseSheet = ExcelBeanAssembler.toSheetCommand(items, BomCostAnalysisReportModel.class, "BOM成本分析");
		ReportDecorationContext context = new ReportDecorationContext(
				BomCostAnalysisReportDecoratorStrategy.STRATEGY_KEY,
				"2026年Q4產品結構與成本分析表",
				"AuditSecurePass888",
				"機密等級・審計專用",
				items,
				Map.of(
						"parentMaterialCode", "MAT-FG-001",
						"parentMaterialName", "商用巡檢無人機"
				)
		);

		// When
		WriteSheetCommand decoratedSheet = strategy.decorate(baseSheet, context);

		// Then
		assertThat(decoratedSheet).isNotNull();

		// 1. 驗證資料列數：2 筆明細 + 1 筆合計列 = 3 列
		assertThat(decoratedSheet.rows()).hasSize(3);
		List<Object> summaryRow = decoratedSheet.rows().get(2);
		assertThat(summaryRow.get(0).toString()).contains("【整機 BOM 標準總成本合計】");
		assertThat(summaryRow.get(0).toString()).contains("MAT-FG-001");

		// 2. 驗證合計列索引 (0-based 為 3，即第 4 列) 的 SUM 公式
		assertThat(decoratedSheet.cellFormulas()).containsKey("L4");
		assertThat(decoratedSheet.cellFormulas().get("L4")).isEqualTo("SUM(L2:L3)");

		// 3. 驗證合計列跨欄合併 A~K 欄 (col 0 ~ 10)
		assertThat(decoratedSheet.mergedRegions()).contains(new CellRange(3, 3, 0, 10));

		// 4. 驗證工作表防護與頁首頁尾
		assertThat(decoratedSheet.protectionConfig()).isNotNull();
		assertThat(decoratedSheet.protectionConfig().password()).isEqualTo("AuditSecurePass888");
		assertThat(decoratedSheet.headerFooterConfig()).isNotNull();
		assertThat(decoratedSheet.headerFooterConfig().rightFooter()).contains("機密等級・審計專用");

		// 5. 驗證深海軍藍樣式客製與凍結窗格
		assertThat(decoratedSheet.headerFillColor()).isEqualTo("1B365D");
		assertThat(decoratedSheet.headerFontColor()).isEqualTo("FFFFFF");
		assertThat(decoratedSheet.freezeHeader()).isTrue();
	}
}
