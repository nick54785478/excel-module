package com.example.demo.infrastructure.template.mapping.engine;

import com.example.demo.application.shared.mapping.FieldConverterType;
import com.example.demo.application.shared.mapping.TemplateMappingDefinition;
import com.example.demo.domain.material.aggregate.root.Material;
import com.example.demo.domain.material.aggregate.vo.*;
import com.example.demo.infrastructure.template.mapping.converter.CurrencyFieldConverter;
import com.example.demo.infrastructure.template.mapping.converter.DateFormatFieldConverter;
import com.example.demo.infrastructure.template.mapping.converter.EnumDictionaryConverter;
import com.example.demo.infrastructure.template.mapping.converter.MaskingFieldConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JxlsFieldMappingEngine 欄位動態映射核心引擎單元測試。
 * <p>
 * 遵循 AGENTS.md 規則 5 測試規範，具備 Gherkin 語法註解。
 * 驗證領域聚合根 (Material) 結合 Fluent DSL 規則進行自動開箱、格式轉換、虛擬求值與 Context 組裝。
 * </p>
 */
@DisplayName("基礎設施層: JXLS 欄位動態映射核心引擎測試")
public class JxlsFieldMappingEngineTest {

    private JxlsFieldMappingEngine engine;

    @BeforeEach
    void setUp() {
        engine = new JxlsFieldMappingEngine(List.of(
            new CurrencyFieldConverter(),
            new DateFormatFieldConverter(),
            new EnumDictionaryConverter(),
            new MaskingFieldConverter()
        ));
    }

    /**
     * Feature: 將 Material 聚合根清單映射為 JXLS 資料模型
     * Scenario: 驗證 Value Object 自動開箱、貨幣格式化、列舉轉譯與虛擬表達式運算
     *   Given 準備 2 筆純領域 Material 聚合根
     *   And 透過 Fluent DSL 建構 TemplateMappingDefinition
     *   When 呼叫 engine.mapToContext
     *   Then 產出之 Map 應包含 rootCollectionVar 且每筆欄位皆正確轉換
     *   And 全域元數據 (globalMeta) 正確合流
     */
    @Test
    @DisplayName("Gherkin: Material 聚合根動態欄位映射與上下文組裝驗證")
    void shouldMapMaterialAggregatesToContextCorrectly() {
        // Given
        Material mat1 = Material.create(
            MaterialCode.of("MAT-RM-001"),
            "碳纖維機臂",
            "外徑 25mm",
            MaterialCategory.RAW_MATERIAL,
            UnitOfMeasure.PCS,
            Money.of(new BigDecimal("450.00"), "TWD")
        );

        Material mat2 = Material.create(
            MaterialCode.of("MAT-SFG-001"),
            "飛控主板電路",
            "Cortex-M7",
            MaterialCategory.SEMI_FINISHED,
            UnitOfMeasure.SET,
            Money.of(new BigDecimal("12500.00"), "TWD")
        );

        TemplateMappingDefinition mappingDef = TemplateMappingDefinition.builder("material_master")
            .rootCollection("materials", "m")
            // 1. 深度屬性路徑
            .mapField("code.value", "materialCode")
            .mapField("name", "materialName")
            .mapField("specification", "spec")
            // 2. Value Object 直接路徑 + 智慧開箱 + 貨幣轉換器
            .mapField("standardCost", "formattedCost", FieldConverterType.CURRENCY, "$#,##0.00")
            // 3. 列舉轉中文
            .mapField("category", "categoryText", FieldConverterType.ENUM_DICT, "MATERIAL_CATEGORY")
            .mapField("status", "statusText", FieldConverterType.ENUM_DICT, "MATERIAL_STATUS")
            // 4. 虛擬跨欄位 SpEL 計算
            .virtualField("fullSpec", "#{m.name} + ' [' + #{m.specification} + ']'")
            // 5. 全域報表元數據
            .addMeta("companyName", "神盾航太科技股份有限公司")
            .addMeta("exportYear", 2026)
            .build();

        // When
        Map<String, Object> context = engine.mapToContext(List.of(mat1, mat2), mappingDef);

        // Then
        assertThat(context).containsKey("companyName").containsKey("materials");
        assertThat(context.get("companyName")).isEqualTo("神盾航太科技股份有限公司");
        assertThat(context.get("exportYear")).isEqualTo(2026);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) context.get("materials");
        assertThat(rows).hasSize(2);

        Map<String, Object> row1 = rows.get(0);
        assertThat(row1.get("materialCode")).isEqualTo("MAT-RM-001");
        assertThat(row1.get("materialName")).isEqualTo("碳纖維機臂");
        assertThat(row1.get("spec")).isEqualTo("外徑 25mm");
        assertThat(row1.get("formattedCost")).isEqualTo("$450.00");
        assertThat(row1.get("categoryText")).isEqualTo("原物料");
        assertThat(row1.get("statusText")).isEqualTo("啟用");
        assertThat(row1.get("fullSpec")).isEqualTo("碳纖維機臂 [外徑 25mm]");

        Map<String, Object> row2 = rows.get(1);
        assertThat(row2.get("materialCode")).isEqualTo("MAT-SFG-001");
        assertThat(row2.get("formattedCost")).isEqualTo("$12,500.00");
        assertThat(row2.get("categoryText")).isEqualTo("半成品");
    }

    /**
     * Feature: 預設值保底機制
     * Scenario: 當來源物件之屬性為 null 時，應正確填入 defaultValue
     */
    @Test
    @DisplayName("Gherkin: 屬性為 null 時套用 defaultValue 保底")
    void shouldApplyDefaultValueWhenPropertyIsNull() {
        // Given
        Map<String, Object> itemWithNull = Map.of("name", "簡易零件");
        TemplateMappingDefinition mappingDef = TemplateMappingDefinition.builder("test_tpl")
            .rootCollection("items", "item")
            .mapField("name", "itemName")
            .mapFieldWithDefault("missingProperty", "fallbackCode", "DEFAULT-CODE-999")
            .build();

        // When
        Map<String, Object> context = engine.mapToContext(List.of(itemWithNull), mappingDef);

        // Then
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) context.get("items");
        assertThat(rows.get(0).get("itemName")).isEqualTo("簡易零件");
        assertThat(rows.get(0).get("fallbackCode")).isEqualTo("DEFAULT-CODE-999");
    }
}

