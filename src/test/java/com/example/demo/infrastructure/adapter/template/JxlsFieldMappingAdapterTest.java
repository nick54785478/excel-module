package com.example.demo.infrastructure.adapter.template;

import com.example.demo.application.port.out.template.ExcelTemplateFieldMapperPort;
import com.example.demo.application.shared.mapping.TemplateMappingDefinition;
import com.example.demo.domain.material.aggregate.root.Material;
import com.example.demo.domain.material.aggregate.vo.*;
import com.example.demo.infrastructure.template.mapping.converter.CurrencyFieldConverter;
import com.example.demo.infrastructure.template.mapping.converter.DateFormatFieldConverter;
import com.example.demo.infrastructure.template.mapping.converter.EnumDictionaryConverter;
import com.example.demo.infrastructure.template.mapping.converter.MaskingFieldConverter;
import com.example.demo.infrastructure.template.mapping.engine.JxlsFieldMappingEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JxlsFieldMappingAdapter 適配器整合測試。
 * <p>
 * 遵循 AGENTS.md 規則 5 測試規範，具備 Gherkin 語法註解。
 * 驗證應用層輸出埠 {@link ExcelTemplateFieldMapperPort} 經由適配器調用基礎設施引擎之端到端流程。
 * </p>
 */
@DisplayName("基礎設施層: JXLS 欄位映射適配器 (JxlsFieldMappingAdapter) 測試")
public class JxlsFieldMappingAdapterTest {

    private ExcelTemplateFieldMapperPort mapperPort;

    @BeforeEach
    void setUp() {
        JxlsFieldMappingEngine engine = new JxlsFieldMappingEngine(List.of(
            new CurrencyFieldConverter(),
            new DateFormatFieldConverter(),
            new EnumDictionaryConverter(),
            new MaskingFieldConverter()
        ));
        mapperPort = new JxlsFieldMappingAdapter(engine);
    }

    /**
     * Feature: 透過 Port/Adapter 調用完成物料聚合根映射
     * Scenario: 驗證 Inbound/Outbound Port 解耦下之資料正確流轉
     *   Given 建立物料實體並定義 mapping 規格
     *   When 呼叫 mapperPort.mapToTemplateContext
     *   Then 應正確產出 JXLS Context Map
     */
    @Test
    @DisplayName("Gherkin: ExcelTemplateFieldMapperPort 端到端合約調用測試")
    void shouldMapThroughPortSuccessfully() {
        // Given
        Material mat = Material.create(
            MaterialCode.of("MAT-RM-001"),
            "碳管",
            "25mm",
            MaterialCategory.RAW_MATERIAL,
            UnitOfMeasure.PCS,
            Money.of(new BigDecimal("450.00"), "TWD")
        );

        TemplateMappingDefinition def = TemplateMappingDefinition.builder("mat_tpl")
            .rootCollection("materials", "m")
            .mapField("code.value", "materialCode")
            .mapField("name", "materialName")
            .mapField("standardCost.amount", "standardCost")
            .addMeta("exportDate", "2026-11-10")
            .build();

        // When
        Map<String, Object> context = mapperPort.mapToTemplateContext(List.of(mat), def);

        // Then
        assertThat(context).containsKey("materials").containsKey("exportDate");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) context.get("materials");
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("materialCode")).isEqualTo("MAT-RM-001");
        assertThat(rows.get(0).get("materialName")).isEqualTo("碳管");
        assertThat((BigDecimal) rows.get(0).get("standardCost")).isEqualByComparingTo("450.00");
    }
}

