package com.example.demo.infrastructure.template.customizer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BOM 成本分析單專屬範本資料模型自定義策略單元測試 (BomCostAnalysisTemplateModelCustomizerTest)。
 * <p>
 * 遵循 AGENTS.md 規則 5 測試規範，具備 Gherkin 語法註解。
 * </p>
 */
@DisplayName("基礎設施層: BomCostAnalysisTemplateModelCustomizer 測試")
class BomCostAnalysisTemplateModelCustomizerTest {

    private BomCostAnalysisTemplateModelCustomizer customizer;

    @BeforeEach
    void setUp() {
        customizer = new BomCostAnalysisTemplateModelCustomizer();
    }

    /**
     * Feature: 檢查綁定之範本唯一識別鍵
     * Scenario: 驗證 getTemplateKey 正確回傳 "BOM_COST_ANALYSIS" 且 supports 支援該鍵
     *   Given 建立 BomCostAnalysisTemplateModelCustomizer
     *   When 檢查 getTemplateKey 與調用 supports
     *   Then 應正確回傳 BOM_COST_ANALYSIS 且大小寫皆能支援
     */
    @Test
    @DisplayName("Gherkin: 範本識別鍵綁定與支援判定情境")
    void shouldBindCorrectTemplateKey() {
        assertThat(customizer.getTemplateKey()).isEqualTo("BOM_COST_ANALYSIS");
        assertThat(customizer.supports("BOM_COST_ANALYSIS")).isTrue();
        assertThat(customizer.supports("bom_cost_analysis")).isTrue();
        assertThat(customizer.supports("OTHER_KEY")).isFalse();
    }

    /**
     * Feature: BOM 成本分析模型正規化與子件別名對齊
     * Scenario: 傳入 materials 清單，自動對齊 items 並補強 childMaterialCode 與 standardCost 別名
     *   Given 原始模型包含 materials 清單
     *   When 調用 customize 執行轉換
     *   Then items 與 materials 應雙向對齊
     *   And 子件項目應補齊 childMaterialCode, specification 與 standardCost 別名
     */
    @Test
    @DisplayName("Gherkin: BOM 資料模型欄位別名擴充情境")
    @SuppressWarnings("unchecked")
    void shouldCustomizeBomDataModelSuccessfully() {
        Map<String, Object> sourceModel = Map.of(
            "parentMaterialCode", "DRONE-X01",
            "materials", List.of(
                Map.of(
                    "materialCode", "MAT-SUB-01",
                    "materialName", "碳管機臂",
                    "specification", "25mm 碳管",
                    "quantity", 4,
                    "unitPrice", 450.0
                )
            )
        );

        Map<String, Object> customized = customizer.customize(sourceModel);

        assertThat(customized).isNotNull();
        assertThat(customized.get("items")).isNotNull();

        List<Map<String, Object>> items = (List<Map<String, Object>>) customized.get("items");
        assertThat(items).hasSize(1);

        Map<String, Object> firstItem = items.get(0);
        assertThat(firstItem.get("childMaterialCode")).isEqualTo("MAT-SUB-01");
        assertThat(firstItem.get("childMaterialName")).isEqualTo("碳管機臂");
        assertThat(firstItem.get("spec")).isEqualTo("25mm 碳管");
        assertThat(firstItem.get("requiredQuantity")).isEqualTo(4);
        assertThat(firstItem.get("standardCost")).isEqualTo(450.0);
    }
}
