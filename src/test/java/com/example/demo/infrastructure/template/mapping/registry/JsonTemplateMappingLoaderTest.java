package com.example.demo.infrastructure.template.mapping.registry;

import com.example.demo.application.shared.mapping.FieldConverterType;
import com.example.demo.application.shared.mapping.TemplateMappingDefinition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JsonTemplateMappingLoader JSON 規格載入器單元測試。
 * <p>
 * 遵循 AGENTS.md 規則 5 測試規範，具備 Gherkin 語法註解。
 * 驗證 JSON 規格字串能準確反序列化為不可變 TemplateMappingDefinition Record，並支援雙向序列化。
 * </p>
 */
@DisplayName("基礎設施層: JSON 範本映射規格載入器測試")
public class JsonTemplateMappingLoaderTest {

    private final JsonTemplateMappingLoader loader = new JsonTemplateMappingLoader();

    /**
     * Feature: 從外部 JSON 反序列化範本映射規格
     * Scenario: 驗證多欄位規則、轉換器與全域元數據之完整還原
     *   Given 符合規格之 JSON 字串
     *   When 呼叫 loader.fromJson
     *   Then 應產出正確之 TemplateMappingDefinition 實例
     */
    @Test
    @DisplayName("Gherkin: JSON 字串反序列化為 TemplateMappingDefinition Record")
    void shouldDeserializeJsonToTemplateMappingDefinition() {
        // Given
        String json = """
            {
              "templateKey": "material_master",
              "rootCollectionVar": "materials",
              "itemAlias": "m",
              "globalMeta": {
                "companyName": "神盾航太科技股份有限公司",
                "exportYear": 2026
              },
              "rules": [
                {
                  "sourcePath": "code.value",
                  "targetKey": "materialCode",
                  "defaultValue": "N/A"
                },
                {
                  "sourcePath": "standardCost",
                  "targetKey": "formattedCost",
                  "converterKey": "CURRENCY",
                  "formatPattern": "$#,##0.00"
                },
                {
                  "targetKey": "fullSpec",
                  "virtualExpression": "#{m.name} + ' / ' + #{m.specification}"
                }
              ]
            }
            """;

        // When
        TemplateMappingDefinition def = loader.fromJson(json);

        // Then
        assertThat(def.templateKey()).isEqualTo("material_master");
        assertThat(def.rootCollectionVar()).isEqualTo("materials");
        assertThat(def.itemAlias()).isEqualTo("m");
        assertThat(def.globalMeta()).containsEntry("companyName", "神盾航太科技股份有限公司");
        assertThat(def.rules()).hasSize(3);

        assertThat(def.rules().get(0).targetKey()).isEqualTo("materialCode");
        assertThat(def.rules().get(0).defaultValue()).isEqualTo("N/A");

        assertThat(def.rules().get(1).converterKey()).isEqualTo(FieldConverterType.CURRENCY);
        assertThat(def.rules().get(1).formatPattern()).isEqualTo("$#,##0.00");

        assertThat(def.rules().get(2).virtualExpression()).isEqualTo("#{m.name} + ' / ' + #{m.specification}");
    }

    /**
     * Feature: 雙向序列化回環測試 (Round-trip)
     * Scenario: 將 Java DSL 建構之定義轉為 JSON 再反序列化，內容應完全一致
     */
    @Test
    @DisplayName("Gherkin: 映射定義雙向 JSON 序列化與反序列化一致性驗證")
    void shouldPerformRoundTripSerialization() {
        // Given
        TemplateMappingDefinition original = TemplateMappingDefinition.builder("bom_analysis")
            .rootCollection("items", "item")
            .mapField("childMaterialCode", "matCode")
            .mapField("standardCost", "cost", FieldConverterType.CURRENCY, "#,##0.00")
            .addMeta("currency", "TWD")
            .build();

        // When
        String json = loader.toJson(original);
        TemplateMappingDefinition restored = loader.fromJson(json);

        // Then
        assertThat(restored.templateKey()).isEqualTo(original.templateKey());
        assertThat(restored.rootCollectionVar()).isEqualTo(original.rootCollectionVar());
        assertThat(restored.rules()).hasSize(2);
        assertThat(restored.globalMeta()).containsEntry("currency", "TWD");
    }
}

