package com.example.demo.infrastructure.template.customizer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 採購比價單專屬範本資料模型自定義策略單元測試 (ProcurementComparisonTemplateModelCustomizerTest)。
 * <p>
 * 遵循 AGENTS.md 規則 5 測試規範，具備清晰之 Gherkin 語法註解。
 * </p>
 */
@DisplayName("基礎設施層: ProcurementComparisonTemplateModelCustomizer 測試")
class ProcurementComparisonTemplateModelCustomizerTest {

    private ProcurementComparisonTemplateModelCustomizer customizer;

    @BeforeEach
    void setUp() {
        customizer = new ProcurementComparisonTemplateModelCustomizer();
    }

    /**
     * Feature: 檢查綁定之範本唯一識別鍵
     * Scenario: 驗證 getTemplateKey 正確回傳 "PROCUREMENT_COMPARISON" 且 supports 支援該鍵
     *   Given 建立 ProcurementComparisonTemplateModelCustomizer
     *   When 檢查 getTemplateKey 與調用 supports
     *   Then 應正確回傳 PROCUREMENT_COMPARISON 且大小寫皆能支援
     */
    @Test
    @DisplayName("Gherkin: 範本識別鍵綁定與支援判定情境")
    void shouldBindCorrectTemplateKey() {
        assertThat(customizer.getTemplateKey()).isEqualTo("PROCUREMENT_COMPARISON");
        assertThat(customizer.supports("PROCUREMENT_COMPARISON")).isTrue();
        assertThat(customizer.supports("procurement_comparison")).isTrue();
        assertThat(customizer.supports("OTHER_KEY")).isFalse();
    }

    /**
     * Feature: 採購比價單資料模型正規化與別名注入
     * Scenario: 傳入含 vendorNames (逗號分隔) 與 quotationItems (含 itemCode, itemName, quantity, lowestPrice)，
     *           自動解析首家供應商並對齊所有別名欄位
     *   Given 原始模型包含 vendorNames="漢翔精密, 長榮航太" 與 quotationItems 集合
     *   When 調用 customize 執行轉換
     *   Then quotationItems 與 items 應雙向對齊
     *   And 每筆明細應補齊 materialCode, materialName, requiredQuantity, quoteUnitPrice 與 vendorName
     */
    @Test
    @DisplayName("Gherkin: 採購資料模型欄位別名與預設廠商注入情境")
    @SuppressWarnings("unchecked")
    void shouldCustomizeProcurementDataModelSuccessfully() {
        Map<String, Object> sourceModel = Map.of(
            "inquiryNo", "RFQ-202611-001",
            "vendorNames", "漢翔精密, 長榮航太, 神盾精密",
            "quotationItems", List.of(
                Map.of(
                    "itemCode", "MAT-001",
                    "itemName", "無刷電機",
                    "quantity", 100,
                    "lowestPrice", 2500.0
                )
            )
        );

        Map<String, Object> customized = customizer.customize(sourceModel);

        assertThat(customized).isNotNull();
        assertThat(customized.get("items")).isNotNull();
        assertThat(customized.get("quotationItems")).isNotNull();

        List<Map<String, Object>> items = (List<Map<String, Object>>) customized.get("quotationItems");
        assertThat(items).hasSize(1);

        Map<String, Object> firstItem = items.get(0);
        assertThat(firstItem.get("materialCode")).isEqualTo("MAT-001");
        assertThat(firstItem.get("materialName")).isEqualTo("無刷電機");
        assertThat(firstItem.get("requiredQuantity")).isEqualTo(100);
        assertThat(firstItem.get("quoteUnitPrice")).isEqualTo(2500.0);
        assertThat(firstItem.get("vendorName")).isEqualTo("漢翔精密");
    }
}
