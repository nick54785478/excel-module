package com.example.demo.infrastructure.adapter.template;

import com.example.demo.application.port.out.template.ExcelTemplateFieldMapperPort;
import com.example.demo.application.port.out.template.ExcelTemplateWriterPort;
import com.example.demo.application.shared.mapping.FieldConverterType;
import com.example.demo.application.shared.mapping.TemplateMappingDefinition;
import com.example.demo.domain.material.aggregate.root.Material;
import com.example.demo.domain.material.aggregate.vo.*;
import com.example.demo.infrastructure.template.mapping.converter.CurrencyFieldConverter;
import com.example.demo.infrastructure.template.mapping.converter.DateFormatFieldConverter;
import com.example.demo.infrastructure.template.mapping.converter.EnumDictionaryConverter;
import com.example.demo.infrastructure.template.mapping.converter.MaskingFieldConverter;
import com.example.demo.infrastructure.template.mapping.engine.JxlsFieldMappingEngine;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JXLS 欄位動態映射與實體範本渲染端到端整合測試 (End-to-End Integration Test)。
 * <p>
 * 遵循 AGENTS.md 規則 5 測試規範，具備 Gherkin 語法註解。
 * 完整貫通：
 * <ol>
 *   <li>DDD 純領域聚合根 ({@link Material})。</li>
 *   <li>應用層欄位映射輸出埠 ({@link ExcelTemplateFieldMapperPort}) 與流暢 DSL 規格。</li>
 *   <li>基礎設施層映射引擎 ({@link JxlsFieldMappingEngine}) 屬性萃取與開箱。</li>
 *   <li>物理 JXLS 範本 ({@code material_master.xlsx})。</li>
 *   <li>JXLS 3.x 範本填充適配器 ({@link JxlsTemplateWriterAdapter}) 與 POI 渲染。</li>
 * </ol>
 * </p>
 */
@DisplayName("基礎設施層: JXLS 欄位動態映射與實體範本渲染整合測試")
public class JxlsFieldMappingIntegrationTest {

    private ExcelTemplateFieldMapperPort mapperPort;
    private final ExcelTemplateWriterPort writerPort = new JxlsTemplateWriterAdapter();

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
     * Feature: Material 聚合根經由動態 Mapping 規格填充至 material_master.xlsx 實體範本
     * Scenario: 驗證 3 筆物料聚合根經由非侵入式開箱與轉換後，成功驅動 JXLS 展開與動態公式計算
     *   Given 準備 3 筆純 Java Material 聚合根 (無任何 DTO 或轉換邏輯)
     *   And 定義 TemplateMappingDefinition 包含 code.value, category 中文轉譯, standardCost 開箱
     *   When 透過 mapperPort.mapToTemplateContext 產生上下文
     *   And 透過 writerPort.writeFromTemplate 結合 material_master.xlsx 渲染
     *   Then 產出之 Excel 活頁簿資料列應正確展開為 3 列 (Row 3 到 Row 5)
     *   And 表尾加總公式自動擴展平移為 =SUM(G4:G6)
     */
    @Test
    @DisplayName("Gherkin: Material 聚合根經由動態映射至 material_master.xlsx 渲染驗證")
    void shouldRenderMaterialMasterFromDynamicMappedAggregateRoots() throws Exception {
        // Given: 純領域聚合根 (Pure Java Domain Aggregates)
        Material m1 = Material.create(
            MaterialCode.of("MAT-RM-001"),
            "3K 碳纖維管機臂",
            "外徑 25mm 碳管",
            MaterialCategory.RAW_MATERIAL,
            UnitOfMeasure.PCS,
            Money.of(new BigDecimal("450.00"), "TWD")
        );

        Material m2 = Material.create(
            MaterialCode.of("MAT-RM-002"),
            "無刷直流馬達",
            "T-Motor U8 II 85KV",
            MaterialCategory.RAW_MATERIAL,
            UnitOfMeasure.PCS,
            Money.of(new BigDecimal("3200.00"), "TWD")
        );

        Material m3 = Material.create(
            MaterialCode.of("MAT-SFG-001"),
            "飛控主板電路總成",
            "STM32H743 Cortex-M7",
            MaterialCategory.SEMI_FINISHED,
            UnitOfMeasure.SET,
            Money.of(new BigDecimal("12500.00"), "TWD")
        );

        // 定義對應至 material_master.xlsx 範本變數之 Mapping 規格
        TemplateMappingDefinition mappingDef = TemplateMappingDefinition.builder("material_master")
            .rootCollection("materials", "m")
            .mapField("code.value", "materialCode")
            .mapField("name", "materialName")
            .mapField("specification", "specification")
            .mapField("category", "category", FieldConverterType.ENUM_DICT, "MATERIAL_CATEGORY")
            .mapField("unit.displayName", "unit") // Enum displayName -> "件/個", "套/組"
            .mapField("standardCost.amount", "standardCost") // 數值交由 Excel 貨幣格式顯示
            .mapField("status.name", "status")
            .addMeta("companyName", "神盾航太科技股份有限公司")
            .addMeta("exportDate", "2026-11-10")
            .build();

        // When: 1. 執行動態欄位映射
        Map<String, Object> contextModel = mapperPort.mapToTemplateContext(List.of(m1, m2, m3), mappingDef);

        // When: 2. 載入實體範本檔案流並執行 JXLS 填充
        byte[] resultExcelBytes;
        try (InputStream templateIs = getClass().getResourceAsStream("/templates/excel/material_master.xlsx")) {
            assertThat(templateIs).isNotNull();
            resultExcelBytes = writerPort.writeFromTemplate(templateIs, contextModel);
        }

        assertThat(resultExcelBytes).isNotEmpty();

        // Then: 3. 解析產出活頁簿斷言驗證
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(resultExcelBytes))) {
            Sheet sheet = wb.getSheetAt(0);
            assertThat(sheet.getSheetName()).isEqualTo("物料主檔");

            // Row 0: Banner
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).contains("物料主檔清單");

            // Row 1: Header metadata
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).contains("神盾航太科技");

            // Row 3 (第 1 筆)
            Row row1 = sheet.getRow(3);
            assertThat(row1.getCell(1).getStringCellValue()).isEqualTo("MAT-RM-001");
            assertThat(row1.getCell(2).getStringCellValue()).isEqualTo("3K 碳纖維管機臂");
            assertThat(row1.getCell(4).getStringCellValue()).isEqualTo("原物料");
            assertThat(row1.getCell(5).getStringCellValue()).isEqualTo("件/個");
            assertThat(row1.getCell(6).getNumericCellValue()).isEqualTo(450.00);

            // Row 5 (第 3 筆)
            Row row3 = sheet.getRow(5);
            assertThat(row3.getCell(1).getStringCellValue()).isEqualTo("MAT-SFG-001");
            assertThat(row3.getCell(2).getStringCellValue()).isEqualTo("飛控主板電路總成");
            assertThat(row3.getCell(4).getStringCellValue()).isEqualTo("半成品");
            assertThat(row3.getCell(5).getStringCellValue()).isEqualTo("套/組");
            assertThat(row3.getCell(6).getNumericCellValue()).isEqualTo(12500.00);

            // Row 6 (表尾加總列)
            Row sumRow = sheet.getRow(6);
            Cell sumCell = sumRow.getCell(6);
            // 驗證公式已自 $[SUM(G4)] 動態擴展平移為 =SUM(G4:G6)
            assertThat(sumCell.getCellFormula()).isEqualTo("SUM(G4:G6)");
        }
    }
}

