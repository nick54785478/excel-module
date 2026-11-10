package com.example.demo.infrastructure.adapter.template;

import com.example.demo.application.shared.exception.ExcelWriteException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JxlsTemplateWriterAdapter 範本寫入適配器整合與單元測試。
 * <p>
 * 遵循 AGENTS.md 規則 5 測試規範，每個測試案例皆標註清晰之 Gherkin 語法註解。
 * 測試覆蓋：
 * 1. 記憶體緩衝模式 (writeFromTemplate) 動態循環資料渲染。
 * 2. 串流直通模式 (streamWriteFromTemplate) 渲染。
 * 3. JEXL 安全沙箱阻斷 CVE 高危代碼執行防禦驗證。
 * </p>
 */
@DisplayName("基礎設施層: JxlsTemplateWriterAdapter JXLS 範本寫入測試")
class JxlsTemplateWriterAdapterTest {

    private JxlsTemplateWriterAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JxlsTemplateWriterAdapter();
    }

    /**
     * Feature: 記憶體緩衝模式下正確以 JXLS 渲染 Excel 範本
     * Scenario: 傳入含 jx:area 與 jx:each 批註指令之範本與物料清單，產出正確內容之 .xlsx
     *   Given 建立包含 A1=jx:area, A2=jx:each 批註之 Excel 範本串流
     *   And 資料模型包含 2 筆物料資料 (電阻, 電容)
     *   When 調用 writeFromTemplate 執行渲染
     *   Then 產出之位元組資料應為合法 Excel 檔案
     *   And 展開後第 1 列為「電阻」、第 2 列為「電容」
     */
    @Test
    @DisplayName("Gherkin: JXLS 正常範本填充與資料展開情境")
    void shouldRenderTemplateSuccessfully() throws Exception {
        byte[] templateBytes = createSampleTemplateWorkbook();
        List<Map<String, Object>> materials = List.of(
            Map.of("name", "貼片電阻 10K", "qty", 1000, "price", 0.05),
            Map.of("name", "陶瓷電容 104", "qty", 500, "price", 0.12)
        );
        Map<String, Object> dataModel = Map.of("materials", materials);

        byte[] resultBytes = adapter.writeFromTemplate(new ByteArrayInputStream(templateBytes), dataModel);

        assertThat(resultBytes).isNotEmpty();

        // 重新讀取產物驗證內容
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(resultBytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getPhysicalNumberOfRows()).isEqualTo(3); // 1 標題 + 2 資料列

            Row row1 = sheet.getRow(1);
            assertThat(row1.getCell(0).getStringCellValue()).isEqualTo("貼片電阻 10K");
            assertThat(row1.getCell(1).getNumericCellValue()).isEqualTo(1000);
            assertThat(row1.getCell(2).getNumericCellValue()).isEqualTo(0.05);

            Row row2 = sheet.getRow(2);
            assertThat(row2.getCell(0).getStringCellValue()).isEqualTo("陶瓷電容 104");
            assertThat(row2.getCell(1).getNumericCellValue()).isEqualTo(500);
            assertThat(row2.getCell(2).getNumericCellValue()).isEqualTo(0.12);
        }
    }

    /**
     * Feature: 串流模式直通輸出目標串流
     * Scenario: 調用 streamWriteFromTemplate 直接寫入 ByteArrayOutputStream
     *   Given 建立標準 Excel 範本串流
     *   When 調用 streamWriteFromTemplate
     *   Then 目標輸出串流應接收到有效之 Excel 二進位內容
     */
    @Test
    @DisplayName("Gherkin: JXLS 串流輸出模式驗證")
    void shouldStreamRenderTemplateSuccessfully() throws Exception {
        byte[] templateBytes = createSampleTemplateWorkbook();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Map<String, Object> dataModel = Map.of(
            "materials", List.of(Map.of("name", "IC 晶片", "qty", 50, "price", 15.0))
        );

        adapter.streamWriteFromTemplate(new ByteArrayInputStream(templateBytes), baos, dataModel);

        byte[] resultBytes = baos.toByteArray();
        assertThat(resultBytes).isNotEmpty();

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(resultBytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getPhysicalNumberOfRows()).isEqualTo(2); // 1 標題 + 1 資料列
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("IC 晶片");
        }
    }

    /**
     * Feature: JEXL 表達式沙箱防禦 (CVE-2020-13973 / CVE-2022-26136 阻斷)
     * Scenario: 範本內植入惡意表達式嘗試透過反射存取 Class/Runtime 或 new() 建立高危物件
     *   Given 建立包含惡意表達式之 Excel 範本
     *   When 調用 writeFromTemplate
     *   Then 系統應透過沙箱阻斷執行並拋出 ExcelWriteException 例外
     */
    /**
     * Feature: JEXL 表達式沙箱防禦 (CVE-2020-13973 / CVE-2022-26136 阻斷)
     * Scenario: 範本內植入惡意表達式嘗試透過反射存取 Class/Runtime 執行高危指令
     *   Given 建立包含惡意反射表達式之 Excel 範本
     *   When 調用 writeFromTemplate 執行渲染
     *   Then 系統沙箱應攔截高危反射調用，安全求值為空值，確保惡意代碼無法執行
     */
    @Test
    @DisplayName("Gherkin: JEXL 安全沙箱阻斷惡意反射攻擊")
    void shouldBlockMaliciousJexlExpressions() throws Exception {
        byte[] maliciousTemplate = createMaliciousTemplateWorkbook();

        byte[] result = adapter.writeFromTemplate(new ByteArrayInputStream(maliciousTemplate), Map.of());
        assertThat(result).isNotEmpty();

        // 驗證惡意表達式被安全沙箱阻斷，儲存格安全求值為空值，未執行任何惡意代碼
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            Sheet sheet = workbook.getSheetAt(0);
            Cell cell = sheet.getRow(1).getCell(0);
            assertThat(cell.getStringCellValue()).isEmpty();
        }
    }

    /**
     * 動態在記憶體建構合法的 JXLS 範本 (.xlsx)。
     */
    private byte[] createSampleTemplateWorkbook() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("物料清單");
            Drawing<?> drawing = sheet.createDrawingPatriarch();

            // Row 0: 表頭
            Row headerRow = sheet.createRow(0);
            Cell h0 = headerRow.createCell(0);
            h0.setCellValue("物料名稱");
            headerRow.createCell(1).setCellValue("數量");
            headerRow.createCell(2).setCellValue("單價");

            // 在 A1 宣告 jx:area(lastCell="C2")
            ClientAnchor anchorA1 = drawing.createAnchor(0, 0, 0, 0, 0, 0, 2, 2);
            Comment commentA1 = drawing.createCellComment(anchorA1);
            commentA1.setString(new XSSFRichTextString("jx:area(lastCell=\"C2\")"));
            h0.setCellComment(commentA1);

            // Row 1: 範本資料列
            Row templateRow = sheet.createRow(1);
            Cell c0 = templateRow.createCell(0);
            c0.setCellValue("${m.name}");
            templateRow.createCell(1).setCellValue("${m.qty}");
            templateRow.createCell(2).setCellValue("${m.price}");

            // 在 A2 宣告 jx:each(items="materials" var="m" lastCell="C2")
            ClientAnchor anchorA2 = drawing.createAnchor(0, 0, 0, 0, 0, 1, 2, 3);
            Comment commentA2 = drawing.createCellComment(anchorA2);
            commentA2.setString(new XSSFRichTextString("jx:each(items=\"materials\" var=\"m\" lastCell=\"C2\")"));
            c0.setCellComment(commentA2);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    /**
     * 動態建構包含惡意 JEXL 反射之攻擊範本。
     */
    private byte[] createMaliciousTemplateWorkbook() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("攻擊測試");
            Drawing<?> drawing = sheet.createDrawingPatriarch();

            Row headerRow = sheet.createRow(0);
            Cell h0 = headerRow.createCell(0);
            h0.setCellValue("標題");

            ClientAnchor anchorA1 = drawing.createAnchor(0, 0, 0, 0, 0, 0, 2, 2);
            Comment commentA1 = drawing.createCellComment(anchorA1);
            commentA1.setString(new XSSFRichTextString("jx:area(lastCell=\"A2\")"));
            h0.setCellComment(commentA1);

            Row attackRow = sheet.createRow(1);
            Cell attackCell = attackRow.createCell(0);
            // 嘗試透過 Class.forName 反射高危 Runtime (CVE 攻擊原型)
            attackCell.setCellValue("${\"\".getClass().forName(\"java.lang.Runtime\").getMethod(\"getRuntime\").invoke(null)}");

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    /**
     * Feature: 智慧欄位別名正規化容錯支援 (Smart Alias Normalization)
     * Scenario: 傳入 itemCode, itemName, quantity, lowestPrice, vendorNames 等常見別名，
     *   渲染依賴 materialCode, materialName, requiredQuantity, quoteUnitPrice, vendorName 之範本
     *   Given 載入實體 procurement_comparison.xlsx 範本
     *   When 傳入使用 itemCode 等別名之 quotationItems 資料
     *   Then 產出之 Excel 內容不再為空，正確填入料號、品名、數量、單價與總價
     */
    @Test
    @DisplayName("Gherkin: 採購比價單欄位別名正規化填充情境 (解決空資料問題)")
    void shouldRenderProcurementComparisonWithNormalizedAliases() throws Exception {
        try (InputStream tplStream = getClass().getResourceAsStream("/templates/excel/procurement_comparison.xlsx")) {
            assertThat(tplStream).isNotNull();

            Map<String, Object> dataModel = Map.of(
                "inquiryNo", "INQ-202611-008",
                "inquiryDate", "2026-11-10",
                "vendorNames", "漢翔精密, 長榮航太, 神盾精密",
                "quotationItems", List.of(
                    Map.of(
                        "itemCode", "MAT-SFG-001",
                        "itemName", "飛控主板電路總成",
                        "quantity", 50,
                        "lowestPrice", 4520.00
                    )
                )
            );

            byte[] resultBytes = adapter.writeFromTemplate(tplStream, dataModel);
            assertThat(resultBytes).isNotEmpty();

            try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(resultBytes))) {
                Sheet sheet = wb.getSheetAt(0);
                Row dataRow = sheet.getRow(3);
                assertThat(dataRow.getCell(1).getStringCellValue()).isEqualTo("MAT-SFG-001");
                assertThat(dataRow.getCell(2).getStringCellValue()).isEqualTo("飛控主板電路總成");
                assertThat(dataRow.getCell(3).getNumericCellValue()).isEqualTo(50.0);
                assertThat(dataRow.getCell(4).getStringCellValue()).isEqualTo("漢翔精密");
                assertThat(dataRow.getCell(5).getNumericCellValue()).isEqualTo(4520.00);
            }
        }
    }
}

