package com.example.demo.infrastructure.adapter.template;

import com.example.demo.application.shared.template.ExcelTemplateModelCustomizer;
import com.example.demo.infrastructure.template.customizer.BomCostAnalysisTemplateModelCustomizer;
import com.example.demo.infrastructure.template.customizer.ProcurementComparisonTemplateModelCustomizer;
import com.example.demo.infrastructure.template.customizer.SmartAliasTemplateModelCustomizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Excel 範本資料模型自定義器工廠適配器單元測試 (ExcelTemplateModelCustomizerFactoryAdapterTest)。
 * <p>
 * 遵循 AGENTS.md 規則 5 測試規範，每個測試案例皆標註清晰之 Gherkin 語法註解。
 * </p>
 */
@DisplayName("基礎設施層: ExcelTemplateModelCustomizerFactoryAdapter 工廠適配器測試")
class ExcelTemplateModelCustomizerFactoryAdapterTest {

    private ExcelTemplateModelCustomizerFactoryAdapter factory;
    private ProcurementComparisonTemplateModelCustomizer procurementCustomizer;
    private BomCostAnalysisTemplateModelCustomizer bomCustomizer;
    private SmartAliasTemplateModelCustomizer smartAliasCustomizer;

    @BeforeEach
    void setUp() {
        procurementCustomizer = new ProcurementComparisonTemplateModelCustomizer();
        bomCustomizer = new BomCostAnalysisTemplateModelCustomizer();
        smartAliasCustomizer = new SmartAliasTemplateModelCustomizer();

        factory = new ExcelTemplateModelCustomizerFactoryAdapter(List.of(
            procurementCustomizer,
            bomCustomizer,
            smartAliasCustomizer
        ));
    }

    /**
     * Feature: 依範本唯一識別鍵精確獲取對應之自定義策略
     * Scenario: 傳入 "PROCUREMENT_COMPARISON" 或 "BOM_COST_ANALYSIS"，成功取得專屬 Customizer
     *   Given 工廠已註冊採購比價與 BOM 成本自定義策略
     *   When 調用 getCustomizer 傳入對應 templateKey (大小寫不敏感)
     *   Then 應取得對應之自定義策略實例
     *   And hasCustomizer 應回傳 true
     */
    @Test
    @DisplayName("Gherkin: 依 templateKey 正確取得專屬 Customizer 實例情境")
    void shouldGetExactCustomizerSuccessfully() {
        ExcelTemplateModelCustomizer customizer1 = factory.getCustomizer("PROCUREMENT_COMPARISON");
        assertThat(customizer1).isSameAs(procurementCustomizer);
        assertThat(factory.hasCustomizer("procurement_comparison")).isTrue();

        ExcelTemplateModelCustomizer customizer2 = factory.getCustomizer("bom_cost_analysis");
        assertThat(customizer2).isSameAs(bomCustomizer);
        assertThat(factory.hasCustomizer("BOM_COST_ANALYSIS")).isTrue();
    }

    /**
     * Feature: 未註冊專屬策略時自動降級至通用保底策略
     * Scenario: 傳入未註冊之 "UNKNOWN_TEMPLATE" 或 null/空白鍵，工廠應自動回傳 SmartAliasTemplateModelCustomizer
     *   Given 工廠已註冊通用保底自定義策略 (*)
     *   When 調用 getCustomizer 傳入未知名稱或空值
     *   Then 應回傳 SmartAliasTemplateModelCustomizer 作為保底，保證不為 null
     *   And hasCustomizer 應回傳 false
     */
    @Test
    @DisplayName("Gherkin: 未註冊範本自動降級至通用保底 Customizer 情境")
    void shouldFallbackToSmartAliasCustomizerWhenTemplateNotFound() {
        ExcelTemplateModelCustomizer customizer = factory.getCustomizer("UNKNOWN_TEMPLATE");
        assertThat(customizer).isSameAs(smartAliasCustomizer);
        assertThat(factory.hasCustomizer("UNKNOWN_TEMPLATE")).isFalse();

        assertThat(factory.getCustomizer(null)).isSameAs(smartAliasCustomizer);
        assertThat(factory.getCustomizer("   ")).isSameAs(smartAliasCustomizer);
    }

    /**
     * Feature: 無任何自定義器注入時安全降級
     * Scenario: 建立無任何 Customizer 之工廠，調用 getCustomizer 應安全回傳 NO_OP
     *   Given 建立空策略清單之 factory
     *   When 調用 getCustomizer
     *   Then 應回傳 ExcelTemplateModelCustomizer.NO_OP
     */
    @Test
    @DisplayName("Gherkin: 空配置環境下安全降級至 NO_OP 情境")
    void shouldFallbackToNoOpWhenNoCustomizersRegistered() {
        ExcelTemplateModelCustomizerFactoryAdapter emptyFactory = new ExcelTemplateModelCustomizerFactoryAdapter(null);

        ExcelTemplateModelCustomizer customizer = emptyFactory.getCustomizer("ANY_KEY");
        assertThat(customizer).isNotNull();

        Map<String, Object> input = Map.of("key", "val");
        Map<String, Object> output = customizer.customize(input);
        assertThat(output).isEqualTo(input);
    }
}
