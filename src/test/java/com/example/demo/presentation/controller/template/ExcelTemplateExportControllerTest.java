package com.example.demo.presentation.controller.template;

import com.example.demo.application.port.in.template.ExportTemplateExcelUseCase;
import com.example.demo.application.port.in.template.ExportTemplateWithMappingUseCase;
import com.example.demo.application.port.in.template.PreviewTemplateMappingUseCase;
import com.example.demo.application.port.in.template.StreamExportTemplateExcelUseCase;
import com.example.demo.application.port.in.template.StreamExportTemplateWithMappingUseCase;
import com.example.demo.application.port.out.template.ExcelTemplateRegistryPort;
import com.example.demo.application.shared.dto.ExcelTemplateGottenResult;
import com.example.demo.application.shared.exception.ExcelTemplateNotFoundException;
import com.example.demo.presentation.assembler.ExcelTemplateResourceAssembler;
import com.example.demo.presentation.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ExcelTemplateExportController REST API 表現層單元測試。
 * <p>
 * 遵循 AGENTS.md 規則 5 測試規範，每個測試案例皆標註清晰之 Gherkin 語法註解。
 * 測試覆蓋範本清單檢索、單一範本查詢、二進位匯出下載、串流匯出下載與不存在之 404 處置。
 * </p>
 */
@DisplayName("表現層: ExcelTemplateExportController REST 控制器測試")
class ExcelTemplateExportControllerTest {

    private MockMvc mockMvc;
    private ExportTemplateExcelUseCase exportTemplateExcelUseCase;
    private StreamExportTemplateExcelUseCase streamExportTemplateExcelUseCase;
    private ExcelTemplateRegistryPort templateRegistryPort;
    private ExportTemplateWithMappingUseCase exportTemplateWithMappingUseCase;
    private StreamExportTemplateWithMappingUseCase streamExportTemplateWithMappingUseCase;
    private PreviewTemplateMappingUseCase previewTemplateMappingUseCase;

    @BeforeEach
    void setUp() {
        exportTemplateExcelUseCase = mock(ExportTemplateExcelUseCase.class);
        streamExportTemplateExcelUseCase = mock(StreamExportTemplateExcelUseCase.class);
        templateRegistryPort = mock(ExcelTemplateRegistryPort.class);
        exportTemplateWithMappingUseCase = mock(ExportTemplateWithMappingUseCase.class);
        streamExportTemplateWithMappingUseCase = mock(StreamExportTemplateWithMappingUseCase.class);
        previewTemplateMappingUseCase = mock(PreviewTemplateMappingUseCase.class);
        ExcelTemplateResourceAssembler resourceAssembler = new ExcelTemplateResourceAssembler();

        ExcelTemplateExportController controller = new ExcelTemplateExportController(
            exportTemplateExcelUseCase,
            streamExportTemplateExcelUseCase,
            templateRegistryPort,
            resourceAssembler,
            exportTemplateWithMappingUseCase,
            streamExportTemplateWithMappingUseCase,
            previewTemplateMappingUseCase
        );

        this.mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    /**
     * Feature: 查詢所有已註冊之範本規格清單
     * Scenario: 發送 GET /api/v1/excel/templates 取得範本清單
     *   Given 儲存庫內存在 1 筆範本中繼資訊
     *   When 呼叫 GET /api/v1/excel/templates
     *   Then 回傳 200 OK 且 JSON 包含範本名稱與鍵值
     */
    @Test
    @DisplayName("Gherkin: 查詢所有可用範本清單情境")
    void shouldListTemplatesSuccessfully() throws Exception {
        when(templateRegistryPort.listTemplates()).thenReturn(List.of(
            new ExcelTemplateGottenResult(
                "PROCUREMENT_COMPARISON", "採購詢價比價單", "PROCUREMENT", "比價說明",
                List.of("inquiryNo"), Instant.now()
            )
        ));

        mockMvc.perform(get("/api/v1/excel/templates"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[0].templateKey").value("PROCUREMENT_COMPARISON"))
            .andExpect(jsonPath("$[0].templateName").value("採購詢價比價單"));
    }

    /**
     * Feature: 查詢單一範本詳細中繼規格
     * Scenario: 發送 GET /api/v1/excel/templates/{templateKey}
     *   Given 範本存在於儲存庫中
     *   When 發送 GET 請求
     *   Then 回傳 200 OK 且 JSON 結構完整
     */
    @Test
    @DisplayName("Gherkin: 查詢單一範本規格情境")
    void shouldGetTemplateInfoSuccessfully() throws Exception {
        String key = "PROCUREMENT_COMPARISON";
        when(templateRegistryPort.findTemplateInfo(key)).thenReturn(Optional.of(
            new ExcelTemplateGottenResult(
                key, "採購詢價比價單", "PROCUREMENT", "比價單說明", List.of("inquiryNo"), Instant.now()
            )
        ));

        mockMvc.perform(get("/api/v1/excel/templates/{templateKey}", key))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.templateKey").value(key))
            .andExpect(jsonPath("$.templateName").value("採購詢價比價單"));
    }

    /**
     * Feature: 查詢不存在之單一範本回傳 404 Not Found
     */
    @Test
    @DisplayName("Gherkin: 查詢不存在範本規格回傳 404")
    void shouldReturn404WhenTemplateInfoNotFound() throws Exception {
        when(templateRegistryPort.findTemplateInfo("NOT_EXIST")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/excel/templates/NOT_EXIST"))
            .andExpect(status().isNotFound());
    }

    /**
     * Feature: POST /api/v1/excel/templates/{templateKey}/export 匯出下載 Excel
     * Scenario: 傳入合法之 JSON 資料模型，成功下載 Excel 檔案
     *   Given 應用層匯出成功回傳二進位 byte[]
     *   When 發送 POST 請求
     *   Then 回傳 200 OK，包含 Content-Disposition: attachment 與檔案位元組
     */
    @Test
    @DisplayName("Gherkin: 緩衝模式範本匯出下載情境")
    void shouldExportFromTemplateViaPost() throws Exception {
        String key = "PROCUREMENT_COMPARISON";
        byte[] fakeExcel = new byte[]{0x50, 0x4B, 0x03, 0x04};
        when(exportTemplateExcelUseCase.exportFromTemplate(any())).thenReturn(fakeExcel);

        String jsonRequest = """
            {
                "dataModel": {
                    "inquiryNo": "RFQ-2026-001",
                    "items": []
                },
                "outputFileName": "rfq_download.xlsx"
            }
            """;

        mockMvc.perform(post("/api/v1/excel/templates/{templateKey}/export", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("rfq_download.xlsx")))
            .andExpect(content().bytes(fakeExcel));

        verify(exportTemplateExcelUseCase).exportFromTemplate(any());
    }

    /**
     * Feature: POST /api/v1/excel/templates/{templateKey}/stream 串流直通下載 Excel
     * Scenario: 傳入合法資料模型，調用 streamExportFromTemplate 直通輸出
     *   When 發送 POST 串流請求
     *   Then 回傳 200 OK 且調用 StreamExportTemplateExcelUseCase
     */
    @Test
    @DisplayName("Gherkin: 串流模式範本直通下載情境")
    void shouldStreamExportFromTemplateViaPost() throws Exception {
        String key = "PROCUREMENT_COMPARISON";

        String jsonRequest = """
            {
                "dataModel": {
                    "inquiryNo": "RFQ-STREAM-001"
                },
                "outputFileName": "rfq_stream.xlsx"
            }
            """;

        mockMvc.perform(post("/api/v1/excel/templates/{templateKey}/stream", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("rfq_stream.xlsx")));

        verify(streamExportTemplateExcelUseCase).streamExportFromTemplate(any(), any());
    }

    /**
     * Feature: 匯出不存在之範本時回傳 404 Not Found
     * Scenario: 傳入未註冊之範本鍵值，UseCase 拋出 ExcelTemplateNotFoundException
     *   Given exportTemplateExcelUseCase 拋出 ExcelTemplateNotFoundException
     *   When 發送 POST 匯出請求
     *   Then 全域例外處理器攔截並回傳 404 Not Found 結構化錯誤
     */
    @Test
    @DisplayName("Gherkin: 匯出不存在範本回傳 404 情境")
    void shouldReturn404WhenExportingNonExistentTemplate() throws Exception {
        when(exportTemplateExcelUseCase.exportFromTemplate(any()))
            .thenThrow(new ExcelTemplateNotFoundException("指定的 Excel 範本不存在或未註冊: UNKNOWN"));

        String jsonRequest = """
            {
                "dataModel": {}
            }
            """;

        mockMvc.perform(post("/api/v1/excel/templates/UNKNOWN/export")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("指定的 Excel 範本不存在或未註冊: UNKNOWN")));
    }

    /**
     * Feature: 依據自訂欄位映射規則匯出範本 Excel 檔案 (Buffered 模式)
     * Scenario: 發送 POST /api/v1/excel/templates/material_master/export-mapped 成功回傳二進位下載
     *   Given exportTemplateWithMappingUseCase 成功回傳有效的 Excel 二進位陣列
     *   When 發送 POST /export-mapped 帶入自訂欄位映射 JSON
     *   Then 回傳 200 OK 且包含 Content-Disposition attachment 下載標頭
     */
    @Test
    @DisplayName("Gherkin: 自訂欄位映射 Buffered 匯出成功情境")
    void shouldExportFromTemplateWithMappingSuccessfully() throws Exception {
        byte[] fakeExcelBytes = new byte[]{0x50, 0x4B, 0x03, 0x04};
        when(exportTemplateWithMappingUseCase.exportFromTemplateWithMapping(any())).thenReturn(fakeExcelBytes);

        String jsonRequest = """
            {
                "outputFileName": "my_materials.xlsx",
                "mappingDefinition": {
                    "templateKey": "material_master",
                    "rootCollectionVar": "materials",
                    "itemAlias": "m",
                    "rules": [
                        { "sourcePath": "code.value", "targetKey": "materialCode" }
                    ]
                }
            }
            """;

        mockMvc.perform(post("/api/v1/excel/templates/material_master/export-mapped")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("my_materials.xlsx")))
            .andExpect(content().bytes(fakeExcelBytes));

        verify(exportTemplateWithMappingUseCase).exportFromTemplateWithMapping(any());
    }

    /**
     * Feature: 依據自訂欄位映射規則串流匯出範本 Excel 檔案 (Streaming 模式)
     * Scenario: 發送 POST /api/v1/excel/templates/material_master/stream-mapped 成功直通串流寫出
     *   Given streamExportTemplateWithMappingUseCase 模擬成功串流寫入
     *   When 發送 POST /stream-mapped 請求
     *   Then 回傳 200 OK 且正確呼叫串流 UseCase
     */
    @Test
    @DisplayName("Gherkin: 自訂欄位映射 Streaming 串流匯出成功情境")
    void shouldStreamExportFromTemplateWithMappingSuccessfully() throws Exception {
        doNothing().when(streamExportTemplateWithMappingUseCase).streamExportFromTemplateWithMapping(any(), any());

        String jsonRequest = """
            {
                "outputFileName": "stream_materials.xlsx",
                "mappingDefinition": {
                    "templateKey": "material_master",
                    "rootCollectionVar": "materials",
                    "rules": [
                        { "sourcePath": "name", "targetKey": "materialName" }
                    ]
                }
            }
            """;

        mockMvc.perform(post("/api/v1/excel/templates/material_master/stream-mapped")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("stream_materials.xlsx")));

        verify(streamExportTemplateWithMappingUseCase).streamExportFromTemplateWithMapping(any(), any());
    }

    /**
     * Feature: 預覽欄位動態映射轉換後之 JXLS 上下文資料模型 (Dry-Run / Preview)
     * Scenario: 發送 POST /api/v1/excel/templates/mapping/preview 回傳 JSON Context Map
     *   Given previewTemplateMappingUseCase 回傳組裝後之 Map 上下文
     *   When 發送 POST /mapping/preview 帶入映射規格與來源資料
     *   Then 回傳 200 OK 且回傳求值後之 JSON 結構
     */
    @Test
    @DisplayName("Gherkin: 欄位映射預覽 Dry-Run 成功情境")
    void shouldPreviewTemplateMappingSuccessfully() throws Exception {
        when(previewTemplateMappingUseCase.previewTemplateMapping(any())).thenReturn(
            Map.of("materials", List.of(Map.of("materialCode", "MAT-001", "materialName", "無人機零件")))
        );

        String jsonRequest = """
            {
                "mappingDefinition": {
                    "templateKey": "material_master",
                    "rootCollectionVar": "materials",
                    "itemAlias": "m",
                    "rules": [
                        { "sourcePath": "code.value", "targetKey": "materialCode" },
                        { "sourcePath": "name", "targetKey": "materialName" }
                    ]
                }
            }
            """;

        mockMvc.perform(post("/api/v1/excel/templates/mapping/preview")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.materials[0].materialCode").value("MAT-001"))
            .andExpect(jsonPath("$.materials[0].materialName").value("無人機零件"));

        verify(previewTemplateMappingUseCase).previewTemplateMapping(any());
    }
}

