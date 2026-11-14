package com.example.demo.infrastructure.template.customizer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 通用智慧欄位別名範本資料模型自定義策略單元測試 (SmartAliasTemplateModelCustomizerTest)。
 * <p>
 * 遵循 AGENTS.md 規則 5 測試規範，具備 Gherkin 語法註解。
 * </p>
 */
@DisplayName("基礎設施層: SmartAliasTemplateModelCustomizer 測試")
class SmartAliasTemplateModelCustomizerTest {

    private SmartAliasTemplateModelCustomizer customizer;

    @BeforeEach
    void setUp() {
        customizer = new SmartAliasTemplateModelCustomizer();
    }

    /**
     * Feature: 通用保底策略支援所有範本識別鍵
     * Scenario: 傳入任何範本鍵與資料模型，supports 皆應回傳 true，getTemplateKey 回傳 "*"
     *   Given 任意範本識別鍵
     *   When 調用 supports 判定
     *   Then 應恆為 true
     */
    @Test
    @DisplayName("Gherkin: supports 全域支援驗證")
    void shouldSupportAllTemplates() {
        assertThat(customizer.getTemplateKey()).isEqualTo("*");
        assertThat(customizer.supports("ANY_CUSTOM_TEMPLATE")).isTrue();
        assertThat(customizer.supports("PROCUREMENT_COMPARISON")).isTrue();
        assertThat(customizer.supports(null)).isFalse();
    }

    /**
     * Feature: 通用 List 欄位別名正規化
     * Scenario: 傳入自訂集合內含 itemCode、quantity、price 等常見別名，自動擴充同義詞
     *   Given 含有自訂列表之資料模型
     *   When 調用 customize 執行轉換
     *   Then 集合內各 Map 應補齊 materialCode, requiredQuantity, quoteUnitPrice 等別名
     */
    @Test
    @DisplayName("Gherkin: 通用模型別名補強情境")
    @SuppressWarnings("unchecked")
    void shouldNormalizeCommonAliasesInAnyCollection() {
        Map<String, Object> sourceModel = Map.of(
            "customList", List.of(
                Map.of(
                    "itemCode", "IC-009",
                    "quantity", 500,
                    "price", 12.5,
                    "supplier", "聯發科"
                )
            )
        );

        Map<String, Object> customized = customizer.customize(sourceModel);

        assertThat(customized).isNotNull();
        List<Map<String, Object>> list = (List<Map<String, Object>>) customized.get("customList");
        assertThat(list).hasSize(1);

        Map<String, Object> item = list.get(0);
        assertThat(item.get("materialCode")).isEqualTo("IC-009");
        assertThat(item.get("requiredQuantity")).isEqualTo(500);
        assertThat(item.get("standardCost")).isEqualTo(12.5);
        assertThat(item.get("vendorName")).isEqualTo("聯發科");
    }
}
