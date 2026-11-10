package com.example.demo.infrastructure.adapter.template;

import com.example.demo.application.port.out.template.ExcelTemplateWriterPort;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 基於 Domain Aggregate (Material 與 Bom) 之 JXLS 範本檔案生成器與整合驗證測試。
 * <p>
 * 遵循 AGENTS.md 規則 5 測試規範，具備 Gherkin 語法註解。
 * 負責在 {@code src/main/resources/templates/excel/} 產製正式標準範本：
 * <ol>
 *   <li>{@code material_master.xlsx}：對應 Material 物料聚合根之清單報表範本。</li>
 *   <li>{@code bom_cost_analysis.xlsx}：對應 Bom 聚合根與 BomItem 實體之配方展開與動態計算範本。</li>
 *   <li>{@code procurement_comparison.xlsx}：對應採購比價與詢價單範本。</li>
 * </ol>
 * 同時驗證 JXLS 3.x 引擎對該範本之批註解析、動態公式座標平移展開與資料填充結果。
 * </p>
 */
@DisplayName("基礎設施層: JXLS 聚合範本生成與整合渲染驗證")
public class JxlsAggregateTemplateGeneratorTest {

    private static final String RESOURCE_DIR = "src/main/resources/templates/excel/";
    private static final String TARGET_DIR = "target/classes/templates/excel/";

    private final ExcelTemplateWriterPort writerPort = new JxlsTemplateWriterAdapter();

    @BeforeAll
    static void initDirectories() throws IOException {
        Files.createDirectories(Paths.get(RESOURCE_DIR));
        Files.createDirectories(Paths.get(TARGET_DIR));
    }

    /**
     * Feature: 生成並驗證 Material 聚合根專屬之 JXLS 物料主檔清單範本
     * Scenario: 建立 material_master.xlsx 包含 jx:area, jx:each 與加總公式，並以真實物料資料渲染
     *   Given 在 src/main/resources/templates/excel/ 生成 material_master.xlsx
     *   And 準備 5 筆工業級無人機物料資料 (機臂、電機、飛控主板、電池、螺旋槳)
     *   When 透過 writerPort.writeFromTemplate 執行 JXLS 範本填充
     *   Then 產物應包含 5 筆物料明細列與 1 列合計
     *   And 合計儲存格公式自動改寫為 =SUM(G4:G8)
     */
    @Test
    @DisplayName("Gherkin: Material 聚合根範本生成與 JXLS 填充驗證")
    void shouldGenerateAndVerifyMaterialMasterTemplate() throws Exception {
        byte[] templateBytes = buildMaterialMasterTemplateWorkbook();
        saveTemplateFile("material_master.xlsx", templateBytes);

        List<Map<String, Object>> materials = List.of(
            Map.of("materialCode", "MAT-RM-001", "materialName", "3K 碳纖維管機臂", "specification", "外徑 25mm 碳管",
                "category", "原物料", "unit", "支", "standardCost", new BigDecimal("450.00"), "status", "ACTIVE"),
            Map.of("materialCode", "MAT-RM-002", "materialName", "無刷直流馬達", "specification", "T-Motor U8 II 85KV",
                "category", "原物料", "unit", "顆", "standardCost", new BigDecimal("3200.00"), "status", "ACTIVE"),
            Map.of("materialCode", "MAT-SFG-001", "materialName", "飛控主板電路總成", "specification", "STM32H743 Cortex-M7",
                "category", "半成品", "unit", "套", "standardCost", new BigDecimal("12500.00"), "status", "ACTIVE"),
            Map.of("materialCode", "MAT-RM-003", "materialName", "智慧鋰聚合物動力電池", "specification", "6S 22000mAh 25C",
                "category", "原物料", "unit", "組", "standardCost", new BigDecimal("8900.00"), "status", "ACTIVE"),
            Map.of("materialCode", "MAT-RM-004", "materialName", "高強度碳纖摺疊槳", "specification", "28x9.2 吋折疊碳槳",
                "category", "原物料", "unit", "對", "standardCost", new BigDecimal("1100.00"), "status", "ACTIVE")
        );

        Map<String, Object> dataModel = Map.of(
            "companyName", "神盾航太科技股份有限公司 (Aegis Aerospace)",
            "exportDate", "2026-11-09",
            "materials", materials
        );

        byte[] resultBytes = writerPort.writeFromTemplate(new ByteArrayInputStream(templateBytes), dataModel);
        assertThat(resultBytes).isNotEmpty();

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(resultBytes))) {
            Sheet sheet = wb.getSheetAt(0);
            assertThat(sheet.getSheetName()).isEqualTo("物料主檔");

            // Row 0: Banner
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).contains("物料主檔清單");

            // Row 1: Header metadata
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).contains("神盾航太科技");

            // 資料列應該展開為 5 列 (從 Row 3 到 Row 7)
            Row row1 = sheet.getRow(3);
            assertThat(row1.getCell(1).getStringCellValue()).isEqualTo("MAT-RM-001");
            assertThat(row1.getCell(2).getStringCellValue()).isEqualTo("3K 碳纖維管機臂");
            assertThat(row1.getCell(6).getNumericCellValue()).isEqualTo(450.00);

            Row row5 = sheet.getRow(7);
            assertThat(row5.getCell(1).getStringCellValue()).isEqualTo("MAT-RM-004");
            assertThat(row5.getCell(2).getStringCellValue()).isEqualTo("高強度碳纖摺疊槳");

            // 合計列在 Row 8，公式應由 SUM(G4) 自動平移為 SUM(G4:G8)
            Row sumRow = sheet.getRow(8);
            Cell sumCell = sumRow.getCell(6);
            assertThat(sumCell.getCellFormula()).isEqualTo("SUM(G4:G8)");
        }
    }

    /**
     * Feature: 生成並驗證 Bom 聚合根專屬之 JXLS 配方與成本卷算分析範本
     * Scenario: 建立 bom_cost_analysis.xlsx 包含母件資訊、子項展開、損耗率計算公式與總成本加總
     *   Given 在 src/main/resources/templates/excel/ 生成 bom_cost_analysis.xlsx
     *   And 準備 4 筆 BOM 子件資料 (機身骨架、動力模組、飛控導航模組、光電吊艙)
     *   When 透過 writerPort.writeFromTemplate 執行 JXLS 範本填充
     *   Then 子項資料列正確展開為 4 列
     *   And 有效用量動態公式重寫為 =H5*(1+I5), =H6*(1+I6)...
     *   And 小計公式重寫為 =J5*K5, =J6*K6...
     *   And 總成本合計公式自動擴展為 =SUM(L5:L8)
     */
    @Test
    @DisplayName("Gherkin: Bom 聚合根配方展開與動態計算驗證")
    void shouldGenerateAndVerifyBomCostAnalysisTemplate() throws Exception {
        byte[] templateBytes = buildBomCostAnalysisTemplateWorkbook();
        saveTemplateFile("bom_cost_analysis.xlsx", templateBytes);

        List<Map<String, Object>> items = List.of(
            Map.of("itemSequence", 10, "level", 1, "childMaterialCode", "MAT-SFG-002", "childMaterialName", "六軸碳纖機身結構總成",
                "specification", "軸距 1600mm", "category", "半成品", "unit", "套", "baseQuantity", new BigDecimal("1.0"),
                "scrapRate", new BigDecimal("0.02"), "standardCost", new BigDecimal("18500.00")),
            Map.of("itemSequence", 20, "level", 1, "childMaterialCode", "MAT-SFG-003", "childMaterialName", "高負載動力推進系統模組",
                "specification", "含 6 組馬達與電調", "category", "半成品", "unit", "套", "baseQuantity", new BigDecimal("1.0"),
                "scrapRate", new BigDecimal("0.01"), "standardCost", new BigDecimal("25800.00")),
            Map.of("itemSequence", 30, "level", 1, "childMaterialCode", "MAT-SFG-001", "childMaterialName", "飛控主板電路總成",
                "specification", "三重冗餘 IMU", "category", "半成品", "unit", "套", "baseQuantity", new BigDecimal("1.0"),
                "scrapRate", new BigDecimal("0.00"), "standardCost", new BigDecimal("12500.00")),
            Map.of("itemSequence", 40, "level", 1, "childMaterialCode", "MAT-RM-003", "childMaterialName", "智慧鋰聚合物動力電池",
                "specification", "6S 22000mAh", "category", "原物料", "unit", "組", "baseQuantity", new BigDecimal("2.0"),
                "scrapRate", new BigDecimal("0.00"), "standardCost", new BigDecimal("8900.00"))
        );

        Map<String, Object> dataModel = Map.of(
            "parentMaterialCode", "MAT-FG-001",
            "parentMaterialName", "神鷹號長航時工業級巡檢無人機 (Aegis Falcon)",
            "version", "V1.2.0",
            "status", "APPROVED",
            "baseQuantity", 1,
            "currency", "TWD",
            "items", items
        );

        byte[] resultBytes = writerPort.writeFromTemplate(new ByteArrayInputStream(templateBytes), dataModel);
        assertThat(resultBytes).isNotEmpty();

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(resultBytes))) {
            Sheet sheet = wb.getSheetAt(0);
            assertThat(sheet.getSheetName()).isEqualTo("BOM成本分析");

            // 驗證母件資訊
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).contains("MAT-FG-001");
            assertThat(sheet.getRow(1).getCell(3).getStringCellValue()).contains("神鷹號");

            // 驗證子件展開列 (Row 4 到 Row 7)
            Row item1 = sheet.getRow(4);
            assertThat(item1.getCell(2).getStringCellValue()).isEqualTo("MAT-SFG-002");
            assertThat(item1.getCell(7).getNumericCellValue()).isEqualTo(1.0);
            assertThat(item1.getCell(8).getNumericCellValue()).isEqualTo(0.02);
            // 動態公式有效用量與小計
            assertThat(item1.getCell(9).getCellFormula()).isEqualTo("H5*(1+I5)");
            assertThat(item1.getCell(11).getCellFormula()).isEqualTo("J5*K5");

            Row item4 = sheet.getRow(7);
            assertThat(item4.getCell(2).getStringCellValue()).isEqualTo("MAT-RM-003");
            assertThat(item4.getCell(9).getCellFormula()).isEqualTo("H8*(1+I8)");
            assertThat(item4.getCell(11).getCellFormula()).isEqualTo("J8*K8");

            // 驗證總合計列 (Row 8)
            Row sumRow = sheet.getRow(8);
            Cell sumCell = sumRow.getCell(11);
            assertThat(sumCell.getCellFormula()).isEqualTo("SUM(L5:L8)");
        }
    }

    /**
     * Feature: 生成採購詢價比價單範本 (procurement_comparison.xlsx)
     */
    @Test
    @DisplayName("Gherkin: 採購比價範本生成與驗證")
    void shouldGenerateProcurementComparisonTemplate() throws Exception {
        byte[] templateBytes = buildProcurementComparisonTemplateWorkbook();
        saveTemplateFile("procurement_comparison.xlsx", templateBytes);
        assertThat(templateBytes).isNotEmpty();
    }

    private void saveTemplateFile(String filename, byte[] bytes) throws IOException {
        Path srcPath = Paths.get(RESOURCE_DIR, filename);
        Path targetPath = Paths.get(TARGET_DIR, filename);
        Files.write(srcPath, bytes);
        Files.write(targetPath, bytes);
    }

    /**
     * 建構物料主檔 JXLS 範本工作簿 (Material Master Template)。
     */
    private byte[] buildMaterialMasterTemplateWorkbook() throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("物料主檔");
            Drawing<?> drawing = sheet.createDrawingPatriarch();

            CellStyle bannerStyle = createHeaderBannerStyle(wb, new byte[]{0x1B, 0x36, 0x5D});
            CellStyle infoStyle = createMetaInfoStyle(wb);
            CellStyle colHeaderStyle = createColumnHeaderStyle(wb, new byte[]{0x33, 0x4E, 0x68});
            CellStyle dataCenterStyle = createDataStyle(wb, HorizontalAlignment.CENTER);
            CellStyle dataLeftStyle = createDataStyle(wb, HorizontalAlignment.LEFT);
            CellStyle dataCurrencyStyle = createCurrencyStyle(wb);
            CellStyle sumStyle = createSummaryStyle(wb);

            // Row 0: Banner A1:H1
            Row r0 = sheet.createRow(0);
            r0.setHeightInPoints(34);
            Cell bCell = r0.createCell(0);
            bCell.setCellValue("物料主檔清單 (Material Master Report)");
            bCell.setCellStyle(bannerStyle);
            for (int i = 1; i < 8; i++) {
                r0.createCell(i).setCellStyle(bannerStyle);
            }
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));

            // 在 A1 宣告 jx:area(lastCell="H5")
            addComment(drawing, bCell, "jx:area(lastCell=\"H5\")");

            // Row 1: 報表資訊
            Row r1 = sheet.createRow(1);
            r1.setHeightInPoints(22);
            Cell cA2 = r1.createCell(0);
            cA2.setCellValue("公司名稱: ${companyName}");
            cA2.setCellStyle(infoStyle);

            Cell cD2 = r1.createCell(3);
            cD2.setCellValue("製表日期: ${exportDate}");
            cD2.setCellStyle(infoStyle);

            Cell cG2 = r1.createCell(6);
            cG2.setCellValue("物料總筆數: ${materials.size()}");
            cG2.setCellStyle(infoStyle);

            // Row 2: 表頭 A3:H3
            Row r2 = sheet.createRow(2);
            r2.setHeightInPoints(25);
            String[] headers = {"項次", "物料編號", "物料品名", "規格型號", "物料分類", "計量單位", "標準成本 (TWD)", "物料狀態"};
            for (int i = 0; i < headers.length; i++) {
                Cell h = r2.createCell(i);
                h.setCellValue(headers[i]);
                h.setCellStyle(colHeaderStyle);
            }

            // Row 3: 範本資料列 A4:H4
            Row r3 = sheet.createRow(3);
            r3.setHeightInPoints(20);

            Cell c3_0 = r3.createCell(0);
            c3_0.setCellValue("${idx + 1}");
            c3_0.setCellStyle(dataCenterStyle);

            Cell c3_1 = r3.createCell(1);
            c3_1.setCellValue("${m.materialCode}");
            c3_1.setCellStyle(dataCenterStyle);

            Cell c3_2 = r3.createCell(2);
            c3_2.setCellValue("${m.materialName}");
            c3_2.setCellStyle(dataLeftStyle);

            Cell c3_3 = r3.createCell(3);
            c3_3.setCellValue("${m.specification}");
            c3_3.setCellStyle(dataLeftStyle);

            Cell c3_4 = r3.createCell(4);
            c3_4.setCellValue("${m.category}");
            c3_4.setCellStyle(dataCenterStyle);

            Cell c3_5 = r3.createCell(5);
            c3_5.setCellValue("${m.unit}");
            c3_5.setCellStyle(dataCenterStyle);

            Cell c3_6 = r3.createCell(6);
            c3_6.setCellValue("${m.standardCost}");
            c3_6.setCellStyle(dataCurrencyStyle);

            Cell c3_7 = r3.createCell(7);
            c3_7.setCellValue("${m.status}");
            c3_7.setCellStyle(dataCenterStyle);

            // 在 A4 宣告 jx:each(items="materials" var="m" varIndex="idx" lastCell="H4")
            addComment(drawing, c3_0, "jx:each(items=\"materials\" var=\"m\" varIndex=\"idx\" lastCell=\"H4\")");

            // Row 4: 合計列 A5:H5
            Row r4 = sheet.createRow(4);
            r4.setHeightInPoints(24);
            Cell sumLabel = r4.createCell(0);
            sumLabel.setCellValue("標準單價合計 (Total Standard Cost):");
            sumLabel.setCellStyle(sumStyle);
            for (int i = 1; i <= 5; i++) {
                r4.createCell(i).setCellStyle(sumStyle);
            }
            sheet.addMergedRegion(new CellRangeAddress(4, 4, 0, 5));

            Cell sumFormula = r4.createCell(6);
            sumFormula.setCellValue("$[SUM(G4)]");
            sumFormula.setCellStyle(sumStyle);

            Cell sumEmpty = r4.createCell(7);
            sumEmpty.setCellStyle(sumStyle);

            // 欄寬自適應設定
            sheet.setColumnWidth(0, 8 * 256);
            sheet.setColumnWidth(1, 18 * 256);
            sheet.setColumnWidth(2, 28 * 256);
            sheet.setColumnWidth(3, 26 * 256);
            sheet.setColumnWidth(4, 14 * 256);
            sheet.setColumnWidth(5, 12 * 256);
            sheet.setColumnWidth(6, 18 * 256);
            sheet.setColumnWidth(7, 14 * 256);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            return baos.toByteArray();
        }
    }

    /**
     * 建構 BOM 成本分析 JXLS 範本工作簿 (BOM Cost Analysis Template)。
     */
    private byte[] buildBomCostAnalysisTemplateWorkbook() throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("BOM成本分析");
            Drawing<?> drawing = sheet.createDrawingPatriarch();

            CellStyle bannerStyle = createHeaderBannerStyle(wb, new byte[]{0x1B, 0x36, 0x5D});
            CellStyle infoStyle = createMetaInfoStyle(wb);
            CellStyle colHeaderStyle = createColumnHeaderStyle(wb, new byte[]{0x2B, 0x4C, 0x7E});
            CellStyle dataCenterStyle = createDataStyle(wb, HorizontalAlignment.CENTER);
            CellStyle dataLeftStyle = createDataStyle(wb, HorizontalAlignment.LEFT);
            CellStyle dataRightQty = createQtyStyle(wb);
            CellStyle dataRateStyle = createPercentStyle(wb);
            CellStyle dataCurrencyStyle = createCurrencyStyle(wb);
            CellStyle sumStyle = createSummaryStyle(wb);

            // Row 0: Banner A1:L1
            Row r0 = sheet.createRow(0);
            r0.setHeightInPoints(34);
            Cell bCell = r0.createCell(0);
            bCell.setCellValue("產品結構配方與標準成本分析表 (BOM Structure & Standard Cost Rollup)");
            bCell.setCellStyle(bannerStyle);
            for (int i = 1; i < 12; i++) {
                r0.createCell(i).setCellStyle(bannerStyle);
            }
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 11));

            // 在 A1 宣告 jx:area(lastCell="L6")
            addComment(drawing, bCell, "jx:area(lastCell=\"L6\")");

            // Row 1: 母件資訊列 1
            Row r1 = sheet.createRow(1);
            r1.setHeightInPoints(20);
            Cell c1_0 = r1.createCell(0);
            c1_0.setCellValue("母件料號: ${parentMaterialCode}");
            c1_0.setCellStyle(infoStyle);

            Cell c1_3 = r1.createCell(3);
            c1_3.setCellValue("母件品名: ${parentMaterialName}");
            c1_3.setCellStyle(infoStyle);

            Cell c1_8 = r1.createCell(8);
            c1_8.setCellValue("配方版本: ${version}");
            c1_8.setCellStyle(infoStyle);

            // Row 2: 母件資訊列 2
            Row r2 = sheet.createRow(2);
            r2.setHeightInPoints(20);
            Cell c2_0 = r2.createCell(0);
            c2_0.setCellValue("審核狀態: ${status}");
            c2_0.setCellStyle(infoStyle);

            Cell c2_3 = r2.createCell(3);
            c2_3.setCellValue("基準數量: ${baseQuantity}");
            c2_3.setCellStyle(infoStyle);

            Cell c2_8 = r2.createCell(8);
            c2_8.setCellValue("計價幣別: ${currency}");
            c2_8.setCellStyle(infoStyle);

            // Row 3: 表頭 A4:L4
            Row r3 = sheet.createRow(3);
            r3.setHeightInPoints(25);
            String[] headers = {"項次", "階層", "子件料號", "子件品名", "規格型號", "物料分類", "單位", "基準用量", "損耗率", "有效需求用量", "標準單價", "成本小計"};
            for (int i = 0; i < headers.length; i++) {
                Cell h = r3.createCell(i);
                h.setCellValue(headers[i]);
                h.setCellStyle(colHeaderStyle);
            }

            // Row 4: 範本資料列 A5:L5
            Row r4 = sheet.createRow(4);
            r4.setHeightInPoints(20);

            Cell c4_0 = r4.createCell(0);
            c4_0.setCellValue("${item.itemSequence}");
            c4_0.setCellStyle(dataCenterStyle);

            Cell c4_1 = r4.createCell(1);
            c4_1.setCellValue("${item.level}");
            c4_1.setCellStyle(dataCenterStyle);

            Cell c4_2 = r4.createCell(2);
            c4_2.setCellValue("${item.childMaterialCode}");
            c4_2.setCellStyle(dataCenterStyle);

            Cell c4_3 = r4.createCell(3);
            c4_3.setCellValue("${item.childMaterialName}");
            c4_3.setCellStyle(dataLeftStyle);

            Cell c4_4 = r4.createCell(4);
            c4_4.setCellValue("${item.specification}");
            c4_4.setCellStyle(dataLeftStyle);

            Cell c4_5 = r4.createCell(5);
            c4_5.setCellValue("${item.category}");
            c4_5.setCellStyle(dataCenterStyle);

            Cell c4_6 = r4.createCell(6);
            c4_6.setCellValue("${item.unit}");
            c4_6.setCellStyle(dataCenterStyle);

            Cell c4_7 = r4.createCell(7);
            c4_7.setCellValue("${item.baseQuantity}");
            c4_7.setCellStyle(dataRightQty);

            Cell c4_8 = r4.createCell(8);
            c4_8.setCellValue("${item.scrapRate}");
            c4_8.setCellStyle(dataRateStyle);

            // 動態公式 J5 = H5 * (1 + I5)
            Cell c4_9 = r4.createCell(9);
            c4_9.setCellValue("$[H5*(1+I5)]");
            c4_9.setCellStyle(dataRightQty);

            Cell c4_10 = r4.createCell(10);
            c4_10.setCellValue("${item.standardCost}");
            c4_10.setCellStyle(dataCurrencyStyle);

            // 動態公式 L5 = J5 * K5
            Cell c4_11 = r4.createCell(11);
            c4_11.setCellValue("$[J5*K5]");
            c4_11.setCellStyle(dataCurrencyStyle);

            // 在 A5 宣告 jx:each(items="items" var="item" varIndex="idx" lastCell="L5")
            addComment(drawing, c4_0, "jx:each(items=\"items\" var=\"item\" varIndex=\"idx\" lastCell=\"L5\")");

            // Row 5: 總成本合計列 A6:L6
            Row r5 = sheet.createRow(5);
            r5.setHeightInPoints(24);
            Cell sumLabel = r5.createCell(0);
            sumLabel.setCellValue("單機物料成本總計 (Total BOM Cost):");
            sumLabel.setCellStyle(sumStyle);
            for (int i = 1; i <= 10; i++) {
                r5.createCell(i).setCellStyle(sumStyle);
            }
            sheet.addMergedRegion(new CellRangeAddress(5, 5, 0, 10));

            Cell sumFormula = r5.createCell(11);
            sumFormula.setCellValue("$[SUM(L5)]");
            sumFormula.setCellStyle(sumStyle);

            // 調整欄寬
            sheet.setColumnWidth(0, 8 * 256);
            sheet.setColumnWidth(1, 8 * 256);
            sheet.setColumnWidth(2, 18 * 256);
            sheet.setColumnWidth(3, 26 * 256);
            sheet.setColumnWidth(4, 24 * 256);
            sheet.setColumnWidth(5, 12 * 256);
            sheet.setColumnWidth(6, 10 * 256);
            sheet.setColumnWidth(7, 14 * 256);
            sheet.setColumnWidth(8, 12 * 256);
            sheet.setColumnWidth(9, 16 * 256);
            sheet.setColumnWidth(10, 16 * 256);
            sheet.setColumnWidth(11, 18 * 256);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            return baos.toByteArray();
        }
    }

    /**
     * 建構採購比價 JXLS 範本 (Procurement Comparison Template)。
     */
    private byte[] buildProcurementComparisonTemplateWorkbook() throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("採購比價單");
            Drawing<?> drawing = sheet.createDrawingPatriarch();

            CellStyle bannerStyle = createHeaderBannerStyle(wb, new byte[]{0x1B, 0x36, 0x5D});
            CellStyle infoStyle = createMetaInfoStyle(wb);
            CellStyle colHeaderStyle = createColumnHeaderStyle(wb, new byte[]{0x2B, 0x4C, 0x7E});
            CellStyle dataCenterStyle = createDataStyle(wb, HorizontalAlignment.CENTER);
            CellStyle dataLeftStyle = createDataStyle(wb, HorizontalAlignment.LEFT);
            CellStyle dataCurrencyStyle = createCurrencyStyle(wb);
            CellStyle sumStyle = createSummaryStyle(wb);

            // Banner A1:G1
            Row r0 = sheet.createRow(0);
            r0.setHeightInPoints(34);
            Cell bCell = r0.createCell(0);
            bCell.setCellValue("採購詢價與比價明細單 (Procurement RFQ & Quotation Comparison)");
            bCell.setCellStyle(bannerStyle);
            for (int i = 1; i < 7; i++) {
                r0.createCell(i).setCellStyle(bannerStyle);
            }
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));

            addComment(drawing, bCell, "jx:area(lastCell=\"G5\")");

            // Row 1: 詢價單號與日期
            Row r1 = sheet.createRow(1);
            r1.setHeightInPoints(20);
            Cell cA2 = r1.createCell(0);
            cA2.setCellValue("詢價單號: ${inquiryNo}");
            cA2.setCellStyle(infoStyle);

            Cell cD2 = r1.createCell(3);
            cD2.setCellValue("詢價日期: ${inquiryDate}");
            cD2.setCellStyle(infoStyle);

            // Row 2: 表頭 A3:G3
            Row r2 = sheet.createRow(2);
            r2.setHeightInPoints(25);
            String[] headers = {"項次", "採購料號", "物料品名", "需求數量", "首選供應商", "報價單價 (TWD)", "報價總金額"};
            for (int i = 0; i < headers.length; i++) {
                Cell h = r2.createCell(i);
                h.setCellValue(headers[i]);
                h.setCellStyle(colHeaderStyle);
            }

            // Row 3: 資料循環列 A4:G4
            Row r3 = sheet.createRow(3);
            r3.setHeightInPoints(20);

            Cell c3_0 = r3.createCell(0);
            c3_0.setCellValue("${idx + 1}");
            c3_0.setCellStyle(dataCenterStyle);

            Cell c3_1 = r3.createCell(1);
            c3_1.setCellValue("${item.materialCode ?: item.itemCode}");
            c3_1.setCellStyle(dataCenterStyle);

            Cell c3_2 = r3.createCell(2);
            c3_2.setCellValue("${item.materialName ?: item.itemName}");
            c3_2.setCellStyle(dataLeftStyle);

            Cell c3_3 = r3.createCell(3);
            c3_3.setCellValue("${item.requiredQuantity ?: item.quantity}");
            c3_3.setCellStyle(dataCenterStyle);

            Cell c3_4 = r3.createCell(4);
            c3_4.setCellValue("${item.vendorName ?: '漢翔精密'}");
            c3_4.setCellStyle(dataLeftStyle);

            Cell c3_5 = r3.createCell(5);
            c3_5.setCellValue("${item.quoteUnitPrice ?: item.lowestPrice ?: item.targetPrice}");
            c3_5.setCellStyle(dataCurrencyStyle);

            Cell c3_6 = r3.createCell(6);
            c3_6.setCellValue("$[D4*F4]");
            c3_6.setCellStyle(dataCurrencyStyle);

            addComment(drawing, c3_0, "jx:each(items=\"quotationItems\" var=\"item\" varIndex=\"idx\" lastCell=\"G4\")");

            // Row 4: 合計列 A5:G5
            Row r4 = sheet.createRow(4);
            r4.setHeightInPoints(24);
            Cell sumLabel = r4.createCell(0);
            sumLabel.setCellValue("採購總金額合計 (Total Procurement Amount):");
            sumLabel.setCellStyle(sumStyle);
            for (int i = 1; i <= 5; i++) {
                r4.createCell(i).setCellStyle(sumStyle);
            }
            sheet.addMergedRegion(new CellRangeAddress(4, 4, 0, 5));

            Cell sumFormula = r4.createCell(6);
            sumFormula.setCellValue("$[SUM(G4)]");
            sumFormula.setCellStyle(sumStyle);

            sheet.setColumnWidth(0, 8 * 256);
            sheet.setColumnWidth(1, 18 * 256);
            sheet.setColumnWidth(2, 28 * 256);
            sheet.setColumnWidth(3, 14 * 256);
            sheet.setColumnWidth(4, 24 * 256);
            sheet.setColumnWidth(5, 18 * 256);
            sheet.setColumnWidth(6, 20 * 256);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            return baos.toByteArray();
        }
    }

    private void addComment(Drawing<?> drawing, Cell cell, String commentText) {
        int col = cell.getColumnIndex();
        int row = cell.getRowIndex();
        ClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, col, row, col + 2, row + 2);
        Comment comment = drawing.createCellComment(anchor);
        comment.setString(new XSSFRichTextString(commentText));
        cell.setCellComment(comment);
    }

    private CellStyle createHeaderBannerStyle(Workbook wb, byte[] rgb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontName("Microsoft JhengHei");
        font.setBold(true);
        font.setFontHeightInPoints((short) 15);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        if (style instanceof org.apache.poi.xssf.usermodel.XSSFCellStyle xssfStyle) {
            xssfStyle.setFillForegroundColor(new XSSFColor(rgb, null));
            xssfStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createMetaInfoStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontName("Microsoft JhengHei");
        font.setBold(true);
        font.setFontHeightInPoints((short) 10);
        font.setColor(IndexedColors.GREY_80_PERCENT.getIndex());
        style.setFont(font);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createColumnHeaderStyle(Workbook wb, byte[] rgb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontName("Microsoft JhengHei");
        font.setBold(true);
        font.setFontHeightInPoints((short) 10);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        if (style instanceof org.apache.poi.xssf.usermodel.XSSFCellStyle xssfStyle) {
            xssfStyle.setFillForegroundColor(new XSSFColor(rgb, null));
            xssfStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(style, BorderStyle.THIN, IndexedColors.GREY_40_PERCENT.getIndex());
        return style;
    }

    private CellStyle createDataStyle(Workbook wb, HorizontalAlignment align) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontName("Microsoft JhengHei");
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setAlignment(align);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(style, BorderStyle.THIN, IndexedColors.GREY_25_PERCENT.getIndex());
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook wb) {
        CellStyle style = createDataStyle(wb, HorizontalAlignment.RIGHT);
        style.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
        return style;
    }

    private CellStyle createQtyStyle(Workbook wb) {
        CellStyle style = createDataStyle(wb, HorizontalAlignment.RIGHT);
        style.setDataFormat(wb.createDataFormat().getFormat("#,##0.0000"));
        return style;
    }

    private CellStyle createPercentStyle(Workbook wb) {
        CellStyle style = createDataStyle(wb, HorizontalAlignment.RIGHT);
        style.setDataFormat(wb.createDataFormat().getFormat("0.00%"));
        return style;
    }

    private CellStyle createSummaryStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontName("Microsoft JhengHei");
        font.setBold(true);
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
        if (style instanceof org.apache.poi.xssf.usermodel.XSSFCellStyle xssfStyle) {
            xssfStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 0xF0, (byte) 0xF4, (byte) 0xF8}, null));
            xssfStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        setBorders(style, BorderStyle.THIN, IndexedColors.GREY_40_PERCENT.getIndex());
        style.setBorderBottom(BorderStyle.DOUBLE);
        return style;
    }

    private void setBorders(CellStyle style, BorderStyle border, short color) {
        style.setBorderTop(border);
        style.setBorderBottom(border);
        style.setBorderLeft(border);
        style.setBorderRight(border);
        style.setTopBorderColor(color);
        style.setBottomBorderColor(color);
        style.setLeftBorderColor(color);
        style.setRightBorderColor(color);
    }
}
