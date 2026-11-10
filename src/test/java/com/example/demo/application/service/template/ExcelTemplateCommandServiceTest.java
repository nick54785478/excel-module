package com.example.demo.application.service.template;

import com.example.demo.application.port.out.template.ExcelTemplateFieldMapperPort;
import com.example.demo.application.port.out.template.ExcelTemplateRegistryPort;
import com.example.demo.application.port.out.template.ExcelTemplateWriterPort;
import com.example.demo.application.port.out.bom.MaterialRepositoryPort;
import com.example.demo.application.shared.command.ExportTemplateExcelCommand;
import com.example.demo.application.shared.command.ExportTemplateWithMappingCommand;
import com.example.demo.application.shared.mapping.TemplateMappingDefinition;
import com.example.demo.application.shared.exception.ExcelTemplateNotFoundException;
import com.example.demo.application.shared.exception.ExcelWriteException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 範本 Excel 匯出應用層服務單元測試 (ExcelTemplateCommandServiceTest)。
 * <p>
 * 遵循 AGENTS.md 規則 5 測試規範，每個測試案例皆標註清晰之 Gherkin 語法註解。
 * 測試覆蓋正常業務流程、範本不存在異常分支與底層寫入錯誤處理。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("應用層: ExcelTemplateCommandService 業務調度測試")
class ExcelTemplateCommandServiceTest {

    @Mock
    private ExcelTemplateRegistryPort templateRegistryPort;

    @Mock
    private ExcelTemplateWriterPort templateWriterPort;

    @Mock
    private ExcelTemplateFieldMapperPort templateFieldMapperPort;

    @Mock
    private MaterialRepositoryPort materialRepositoryPort;

    private ExcelTemplateCommandService service;

    @BeforeEach
    void setUp() {
        service = new ExcelTemplateCommandService(templateRegistryPort, templateWriterPort, templateFieldMapperPort, materialRepositoryPort);
    }

    /**
     * Feature: 範本 Excel 記憶體緩衝匯出 (Buffered Mode)
     * Scenario: 傳入合法之範本識別鍵與資料模型，成功協調儲存庫與寫入埠回傳檔案二進位位元組陣列
     *   Given 範本 "PROCUREMENT_COMPARISON" 存在於儲存庫中
     *   And 模擬寫入埠回傳有效之 byte[]
     *   When 調用 exportFromTemplate 業務方法
     *   Then 應正確回傳預期之 byte[] 資料
     *   And 應調用 registryPort 開啟串流並委派 writerPort 執行填充
     */
    @Test
    @DisplayName("Gherkin: 正常緩衝匯出成功情境")
    void shouldExportFromTemplateSuccessfully() {
        String templateKey = "PROCUREMENT_COMPARISON";
        byte[] expectedBytes = new byte[]{1, 2, 3, 4, 5};
        ExportTemplateExcelCommand command = new ExportTemplateExcelCommand(
            templateKey,
            Map.of("inquiryNo", "RFQ-20261109-001"),
            "rfq.xlsx"
        );

        when(templateRegistryPort.exists(templateKey)).thenReturn(true);
        when(templateRegistryPort.openTemplateStream(templateKey)).thenReturn(new ByteArrayInputStream(new byte[]{9, 9}));
        when(templateWriterPort.writeFromTemplate(any(InputStream.class), eq(command.dataModel()))).thenReturn(expectedBytes);

        byte[] actual = service.exportFromTemplate(command);

        assertThat(actual).isEqualTo(expectedBytes);
        verify(templateRegistryPort).exists(templateKey);
        verify(templateRegistryPort).openTemplateStream(templateKey);
        verify(templateWriterPort).writeFromTemplate(any(InputStream.class), eq(command.dataModel()));
    }

    /**
     * Feature: 範本 Excel 串流直接匯出 (Streaming Mode)
     * Scenario: 傳入合法之範本識別鍵與目標輸出串流，成功協調儲存庫與寫入埠直通串流
     *   Given 範本 "BOM_COST_ANALYSIS" 存在於儲存庫中
     *   When 調用 streamExportFromTemplate 業務方法
     *   Then 應調用 writerPort.streamWriteFromTemplate 並寫入目標串流
     */
    @Test
    @DisplayName("Gherkin: 正常串流匯出成功情境")
    void shouldStreamExportFromTemplateSuccessfully() {
        String templateKey = "BOM_COST_ANALYSIS";
        ByteArrayOutputStream targetStream = new ByteArrayOutputStream();
        ExportTemplateExcelCommand command = new ExportTemplateExcelCommand(
            templateKey,
            Map.of("bomCode", "BOM-001"),
            "bom.xlsx"
        );

        when(templateRegistryPort.exists(templateKey)).thenReturn(true);
        when(templateRegistryPort.openTemplateStream(templateKey)).thenReturn(new ByteArrayInputStream(new byte[]{8, 8}));

        service.streamExportFromTemplate(targetStream, command);

        verify(templateRegistryPort).exists(templateKey);
        verify(templateRegistryPort).openTemplateStream(templateKey);
        verify(templateWriterPort).streamWriteFromTemplate(any(InputStream.class), eq(targetStream), eq(command.dataModel()));
    }

    /**
     * Feature: 範本不存在時拋出領域例外
     * Scenario: 傳入未註冊之範本鍵值，系統應阻斷執行並拋出 ExcelTemplateNotFoundException
     *   Given 範本 "UNKNOWN_TEMPLATE" 不存在於儲存庫
     *   When 調用 exportFromTemplate 業務方法
     *   Then 應拋出 ExcelTemplateNotFoundException 例外
     *   And 不得調用 openTemplateStream 或寫入輸出埠
     */
    @Test
    @DisplayName("Gherkin: 範本不存在拋出例外情境")
    void shouldThrowExceptionWhenTemplateNotFound() {
        String templateKey = "UNKNOWN_TEMPLATE";
        ExportTemplateExcelCommand command = new ExportTemplateExcelCommand(
            templateKey,
            Map.of(),
            "unknown.xlsx"
        );

        when(templateRegistryPort.exists(templateKey)).thenReturn(false);

        assertThatThrownBy(() -> service.exportFromTemplate(command))
            .isInstanceOf(ExcelTemplateNotFoundException.class)
            .hasMessageContaining("未註冊: " + templateKey);

        verify(templateRegistryPort, never()).openTemplateStream(any());
        verify(templateWriterPort, never()).writeFromTemplate(any(), any());
    }

    /**
     * Feature: 串流模式下範本不存在時拋出領域例外
     * Scenario: 傳入未註冊之範本鍵值調用串流匯出，系統應拋出 ExcelTemplateNotFoundException
     *   Given 範本 "NON_EXIST" 不存在於儲存庫
     *   When 調用 streamExportFromTemplate 業務方法
     *   Then 應拋出 ExcelTemplateNotFoundException 例外
     */
    @Test
    @DisplayName("Gherkin: 串流模式範本不存在拋出例外情境")
    void shouldThrowExceptionWhenTemplateNotFoundInStreaming() {
        String templateKey = "NON_EXIST";
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ExportTemplateExcelCommand command = new ExportTemplateExcelCommand(
            templateKey,
            Map.of(),
            "non_exist.xlsx"
        );

        when(templateRegistryPort.exists(templateKey)).thenReturn(false);

        assertThatThrownBy(() -> service.streamExportFromTemplate(os, command))
            .isInstanceOf(ExcelTemplateNotFoundException.class)
            .hasMessageContaining("未註冊: " + templateKey);

        verify(templateWriterPort, never()).streamWriteFromTemplate(any(), any(), any());
    }

    /**
     * Feature: 底層寫入埠拋出異常時轉譯為 ExcelWriteException
     * Scenario: 範本存在但底層寫入埠因格式損毀或渲染異常拋出 RuntimeException
     *   Given 範本存在於儲存庫
     *   And writerPort 拋出 RuntimeException("Template corrupted")
     *   When 調用 exportFromTemplate 業務方法
     *   Then 應捕捉並轉譯為 ExcelWriteException
     */
    @Test
    @DisplayName("Gherkin: 底層寫入異常轉譯情境")
    void shouldWrapUnderlyingExceptionToExcelWriteException() {
        String templateKey = "PROCUREMENT_COMPARISON";
        ExportTemplateExcelCommand command = new ExportTemplateExcelCommand(
            templateKey,
            Map.of(),
            "err.xlsx"
        );

        when(templateRegistryPort.exists(templateKey)).thenReturn(true);
        when(templateRegistryPort.openTemplateStream(templateKey)).thenReturn(new ByteArrayInputStream(new byte[]{1}));
        when(templateWriterPort.writeFromTemplate(any(), any())).thenThrow(new RuntimeException("Simulated I/O failure"));

        assertThatThrownBy(() -> service.exportFromTemplate(command))
            .isInstanceOf(ExcelWriteException.class)
            .hasMessageContaining("範本 Excel 匯出執行遭遇異常: " + templateKey);
    }

    /**
     * Feature: 具備自訂欄位映射之範本 Excel 匯出 (Buffered Mode)
     * Scenario: 傳入合法之映射規格與來源資料，委派映射埠組裝資料模型並經由寫入埠渲染下載
     *   Given 範本 "material_master" 存在
     *   And templateFieldMapperPort 成功映射出資料上下文
     *   And templateWriterPort 成功渲染 byte[]
     *   When 調用 exportFromTemplateWithMapping
     *   Then 應回傳預期之 byte[] 資料
     */
    @Test
    @DisplayName("Gherkin: 自訂欄位映射 Buffered 模式匯出成功情境")
    void shouldExportFromTemplateWithMappingSuccessfully() {
        String templateKey = "material_master";
        TemplateMappingDefinition mappingDef = TemplateMappingDefinition.builder(templateKey)
            .rootCollection("materials", "m")
            .mapField("name", "materialName")
            .build();
        ExportTemplateWithMappingCommand command = new ExportTemplateWithMappingCommand(
            templateKey, null, mappingDef, "output.xlsx"
        );
        byte[] expectedBytes = new byte[]{1, 2, 3};

        when(templateRegistryPort.exists(templateKey)).thenReturn(true);
        when(templateRegistryPort.openTemplateStream(templateKey)).thenReturn(new ByteArrayInputStream(new byte[]{9}));
        when(templateFieldMapperPort.mapToTemplateContext(any(), eq(mappingDef))).thenReturn(Map.of("materials", java.util.List.of()));
        when(templateWriterPort.writeFromTemplate(any(), any())).thenReturn(expectedBytes);

        byte[] actual = service.exportFromTemplateWithMapping(command);

        assertThat(actual).isEqualTo(expectedBytes);
        verify(templateFieldMapperPort).mapToTemplateContext(any(), eq(mappingDef));
        verify(templateWriterPort).writeFromTemplate(any(), any());
    }

    /**
     * Feature: 具備自訂欄位映射之範本 Excel 串流匯出 (Streaming Mode)
     * Scenario: 傳入合法映射規格，委派映射埠組裝資料並直接直寫輸出串流
     *   Given 範本 "material_master" 存在
     *   When 調用 streamExportFromTemplateWithMapping
     *   Then 應正確呼叫 templateWriterPort.streamWriteFromTemplate
     */
    @Test
    @DisplayName("Gherkin: 自訂欄位映射 Streaming 模式串流直出成功情境")
    void shouldStreamExportFromTemplateWithMappingSuccessfully() {
        String templateKey = "material_master";
        TemplateMappingDefinition mappingDef = TemplateMappingDefinition.builder(templateKey)
            .rootCollection("materials", "m")
            .mapField("name", "materialName")
            .build();
        ExportTemplateWithMappingCommand command = new ExportTemplateWithMappingCommand(
            templateKey, java.util.List.of(), mappingDef, "stream.xlsx"
        );
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        when(templateRegistryPort.exists(templateKey)).thenReturn(true);
        when(templateRegistryPort.openTemplateStream(templateKey)).thenReturn(new ByteArrayInputStream(new byte[]{9}));
        when(templateFieldMapperPort.mapToTemplateContext(any(), eq(mappingDef))).thenReturn(Map.of("materials", java.util.List.of()));

        service.streamExportFromTemplateWithMapping(baos, command);

        verify(templateFieldMapperPort).mapToTemplateContext(any(), eq(mappingDef));
        verify(templateWriterPort).streamWriteFromTemplate(any(), eq(baos), any());
    }

    /**
     * Feature: 預覽欄位動態映射轉換後之 JXLS 上下文資料模型 (Preview / Dry-Run)
     * Scenario: 呼叫 previewTemplateMapping 僅執行映射轉換並回傳資料模型，不觸發實體 Excel 渲染
     *   Given templateFieldMapperPort 成功回傳預覽資料模型
     *   When 調用 previewTemplateMapping
     *   Then 應回傳預覽 Map 且不調用 templateWriterPort
     */
    @Test
    @DisplayName("Gherkin: 欄位映射預覽 Preview 成功情境")
    void shouldPreviewTemplateMappingSuccessfully() {
        String templateKey = "material_master";
        TemplateMappingDefinition mappingDef = TemplateMappingDefinition.builder(templateKey)
            .rootCollection("materials", "m")
            .mapField("name", "materialName")
            .build();
        ExportTemplateWithMappingCommand command = new ExportTemplateWithMappingCommand(
            templateKey, java.util.List.of(), mappingDef, "preview.xlsx"
        );
        Map<String, Object> expectedContext = Map.of("materials", java.util.List.of(Map.of("materialName", "螺絲")));

        when(templateFieldMapperPort.mapToTemplateContext(any(), eq(mappingDef))).thenReturn(expectedContext);

        Map<String, Object> result = service.previewTemplateMapping(command);

        assertThat(result).isEqualTo(expectedContext);
        verify(templateFieldMapperPort).mapToTemplateContext(any(), eq(mappingDef));
        verifyNoInteractions(templateWriterPort);
    }
}

