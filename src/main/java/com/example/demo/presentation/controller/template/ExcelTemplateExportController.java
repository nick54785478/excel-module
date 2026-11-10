package com.example.demo.presentation.controller.template;

import com.example.demo.application.port.in.template.ExportTemplateExcelUseCase;
import com.example.demo.application.port.in.template.ExportTemplateWithMappingUseCase;
import com.example.demo.application.port.in.template.PreviewTemplateMappingUseCase;
import com.example.demo.application.port.in.template.StreamExportTemplateExcelUseCase;
import com.example.demo.application.port.in.template.StreamExportTemplateWithMappingUseCase;
import com.example.demo.application.port.out.template.ExcelTemplateRegistryPort;
import com.example.demo.application.shared.command.ExportTemplateExcelCommand;
import com.example.demo.application.shared.command.ExportTemplateWithMappingCommand;
import com.example.demo.presentation.assembler.ExcelTemplateResourceAssembler;
import com.example.demo.presentation.resource.in.ExportTemplateExcelResource;
import com.example.demo.presentation.resource.in.ExportTemplateWithMappingResource;
import com.example.demo.presentation.resource.out.ExcelTemplateRetrievedResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 宣告式範本 Excel 匯出 REST API 控制器 (Presentation Layer - Inbound Adapter)。
 * <p>
 * 遵循 Clean Architecture 職責劃分，作為 HTTP 入口端點，
 * 接收 Request Resource，透過 {@link ExcelTemplateResourceAssembler} 轉為應用層 Command，
 * 調度各項 UseCase 執行渲染，並將產物二進位資料或串流封裝為 HTTP 附件回應 (Content-Disposition: attachment)。
 * 同步支援純資料模型注入模式與動態欄位映射規格 (Dynamic Field Mapping) 模式。
 * </p>
 */
@Tag(name = "Excel 宣告式範本匯出 API", description = "提供基於 JXLS 3.x 範本定義之 Excel 報表匯出、串流直通下載、動態欄位映射適配與範本清單探索端點")
@Validated
@RestController
@RequestMapping("/api/v1/excel/templates")
public class ExcelTemplateExportController {

    private static final String EXCEL_MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ExportTemplateExcelUseCase exportTemplateExcelUseCase;
    private final StreamExportTemplateExcelUseCase streamExportTemplateExcelUseCase;
    private final ExcelTemplateRegistryPort templateRegistryPort;
    private final ExcelTemplateResourceAssembler resourceAssembler;

    private final ExportTemplateWithMappingUseCase exportTemplateWithMappingUseCase;
    private final StreamExportTemplateWithMappingUseCase streamExportTemplateWithMappingUseCase;
    private final PreviewTemplateMappingUseCase previewTemplateMappingUseCase;

    /**
     * 相容輕量建構函式。
     */
    public ExcelTemplateExportController(
            ExportTemplateExcelUseCase exportTemplateExcelUseCase,
            StreamExportTemplateExcelUseCase streamExportTemplateExcelUseCase,
            ExcelTemplateRegistryPort templateRegistryPort,
            ExcelTemplateResourceAssembler resourceAssembler) {
        this(exportTemplateExcelUseCase, streamExportTemplateExcelUseCase, templateRegistryPort, resourceAssembler, null, null, null);
    }

    /**
     * 完整功能建構函式：支援自訂欄位映射與預覽用例。
     */
    @Autowired
    public ExcelTemplateExportController(
            ExportTemplateExcelUseCase exportTemplateExcelUseCase,
            StreamExportTemplateExcelUseCase streamExportTemplateExcelUseCase,
            ExcelTemplateRegistryPort templateRegistryPort,
            ExcelTemplateResourceAssembler resourceAssembler,
            @Autowired(required = false) ExportTemplateWithMappingUseCase exportTemplateWithMappingUseCase,
            @Autowired(required = false) StreamExportTemplateWithMappingUseCase streamExportTemplateWithMappingUseCase,
            @Autowired(required = false) PreviewTemplateMappingUseCase previewTemplateMappingUseCase) {
        this.exportTemplateExcelUseCase = Objects.requireNonNull(exportTemplateExcelUseCase, "exportTemplateExcelUseCase 不得為空");
        this.streamExportTemplateExcelUseCase = Objects.requireNonNull(streamExportTemplateExcelUseCase, "streamExportTemplateExcelUseCase 不得為空");
        this.templateRegistryPort = Objects.requireNonNull(templateRegistryPort, "templateRegistryPort 不得為空");
        this.resourceAssembler = Objects.requireNonNull(resourceAssembler, "resourceAssembler 不得為空");
        this.exportTemplateWithMappingUseCase = exportTemplateWithMappingUseCase;
        this.streamExportTemplateWithMappingUseCase = streamExportTemplateWithMappingUseCase;
        this.previewTemplateMappingUseCase = previewTemplateMappingUseCase;
    }

    /**
     * 查詢所有已註冊之 Excel 範本清單與中繼規格。
     *
     * @return 範本中繼資訊清單
     */
    @Operation(summary = "查詢所有已註冊之 Excel 範本清單", description = "取得系統已註冊之範本識別鍵、名稱、業務分類與必備變數清單")
    @ApiResponse(responseCode = "200", description = "成功取得範本清單")
    @GetMapping
    public ResponseEntity<List<ExcelTemplateRetrievedResource>> listTemplates() {
        List<ExcelTemplateRetrievedResource> list = templateRegistryPort.listTemplates()
            .stream()
            .map(resourceAssembler::toResource)
            .toList();
        return ResponseEntity.ok(list);
    }

    /**
     * 查詢單一範本之中繼規格資訊。
     *
     * @param templateKey 範本唯一識別鍵
     * @return 範本中繼規格回應資源
     */
    @Operation(summary = "查詢單一範本中繼規格", description = "依據範本鍵值獲取該範本之詳細欄位規格與描述")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "成功取得範本規格"),
        @ApiResponse(responseCode = "404", description = "找不到指定範本")
    })
    @GetMapping("/{templateKey}")
    public ResponseEntity<ExcelTemplateRetrievedResource> getTemplateInfo(
            @Parameter(description = "範本識別鍵", example = "PROCUREMENT_COMPARISON")
            @PathVariable String templateKey) {
        return templateRegistryPort.findTemplateInfo(templateKey)
            .map(resourceAssembler::toResource)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * 依據範本與動態資料模型匯出 Excel 檔案 (記憶體緩衝 Buffered 模式)。
     *
     * @param templateKey 範本識別鍵
     * @param resource    匯出請求資源載體
     * @return Excel (.xlsx) 二進位位元組附件下載
     */
    @Operation(summary = "依據範本匯出 Excel 檔案 (Buffered 模式)", description = "接收資料上下文，以 JXLS 範本渲染並一次性回傳二進位位元組檔案下載")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "成功產製並回傳檔案二進位資料"),
        @ApiResponse(responseCode = "400", description = "請求參數格式不合法"),
        @ApiResponse(responseCode = "404", description = "指定範本不存在"),
        @ApiResponse(responseCode = "500", description = "伺服器內部產生異常")
    })
    @PostMapping("/{templateKey}/export")
    public ResponseEntity<byte[]> exportFromTemplate(
            @Parameter(description = "範本識別鍵", example = "PROCUREMENT_COMPARISON")
            @PathVariable String templateKey,
            @Valid @RequestBody ExportTemplateExcelResource resource) {

        ExportTemplateExcelCommand command = resourceAssembler.toCommand(templateKey, resource);
        byte[] excelBytes = exportTemplateExcelUseCase.exportFromTemplate(command);

        return createAttachmentResponse(excelBytes, command.outputFileName());
    }

    /**
     * 依據範本與動態資料模型串流匯出 Excel 檔案 (Zero-Copy HTTP 串流模式)。
     *
     * @param templateKey 範本識別鍵
     * @param resource    匯出請求資源載體
     * @param response    HTTP Servlet 回應物件
     * @throws IOException 當網路串流寫出發生 I/O 錯誤時拋出
     */
    @Operation(summary = "依據範本串流匯出 Excel 檔案 (Streaming 模式)", description = "直通輸出串流，產物直接寫入 HTTP Response，適用於較大資料量以節省記憶體堆疊")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "成功串流輸出檔案"),
        @ApiResponse(responseCode = "400", description = "請求參數格式不合法"),
        @ApiResponse(responseCode = "404", description = "指定範本不存在"),
        @ApiResponse(responseCode = "500", description = "伺服器內部產生異常")
    })
    @PostMapping("/{templateKey}/stream")
    public void streamExportFromTemplate(
            @Parameter(description = "範本識別鍵", example = "PROCUREMENT_COMPARISON")
            @PathVariable String templateKey,
            @Valid @RequestBody ExportTemplateExcelResource resource,
            HttpServletResponse response) throws IOException {

        ExportTemplateExcelCommand command = resourceAssembler.toCommand(templateKey, resource);
        String encodedFileName = URLEncoder.encode(command.outputFileName(), StandardCharsets.UTF_8).replace("+", "%20");

        response.setContentType(EXCEL_MIME_TYPE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName);

        streamExportTemplateExcelUseCase.streamExportFromTemplate(response.getOutputStream(), command);
        response.flushBuffer();
    }

    /**
     * 依據自訂動態欄位映射規格匯出 Excel 檔案 (Buffered 模式)。
     *
     * @param templateKey 範本識別鍵
     * @param resource    具自訂映射規格與來源資料之請求資源載體
     * @return Excel (.xlsx) 二進位位元組附件下載
     */
    @Operation(summary = "依據自訂欄位映射匯出 Excel 檔案 (Buffered 模式)", description = "接收自訂欄位映射規格 (Field Mapping Definition)，自動開箱 Value Object 並執行各類轉換器與 SpEL 運算後以 JXLS 範本渲染下載")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "成功產製並回傳檔案二進位資料"),
        @ApiResponse(responseCode = "400", description = "請求參數格式不合法"),
        @ApiResponse(responseCode = "404", description = "指定範本不存在"),
        @ApiResponse(responseCode = "500", description = "伺服器內部產生異常")
    })
    @PostMapping("/{templateKey}/export-mapped")
    public ResponseEntity<byte[]> exportFromTemplateWithMapping(
            @Parameter(description = "範本識別鍵", example = "material_master")
            @PathVariable String templateKey,
            @Valid @RequestBody ExportTemplateWithMappingResource resource) {

        if (exportTemplateWithMappingUseCase == null) {
            throw new UnsupportedOperationException("未啟用動態欄位映射功能");
        }
        ExportTemplateWithMappingCommand command = resourceAssembler.toCommand(templateKey, resource);
        byte[] excelBytes = exportTemplateWithMappingUseCase.exportFromTemplateWithMapping(command);

        return createAttachmentResponse(excelBytes, command.outputFileName());
    }

    /**
     * 依據自訂動態欄位映射規格串流匯出 Excel 檔案 (Streaming 模式)。
     *
     * @param templateKey 範本識別鍵
     * @param resource    具自訂映射規格與來源資料之請求資源載體
     * @param response    HTTP Servlet 回應物件
     * @throws IOException 當網路串流寫出發生 I/O 錯誤時拋出
     */
    @Operation(summary = "依據自訂欄位映射串流匯出 Excel 檔案 (Streaming 模式)", description = "接收自訂欄位映射規格，邊渲染邊直通輸出串流，適用於較大資料量以節省記憶體堆疊")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "成功串流輸出檔案"),
        @ApiResponse(responseCode = "400", description = "請求參數格式不合法"),
        @ApiResponse(responseCode = "404", description = "指定範本不存在"),
        @ApiResponse(responseCode = "500", description = "伺服器內部產生異常")
    })
    @PostMapping("/{templateKey}/stream-mapped")
    public void streamExportFromTemplateWithMapping(
            @Parameter(description = "範本識別鍵", example = "material_master")
            @PathVariable String templateKey,
            @Valid @RequestBody ExportTemplateWithMappingResource resource,
            HttpServletResponse response) throws IOException {

        if (streamExportTemplateWithMappingUseCase == null) {
            throw new UnsupportedOperationException("未啟用動態欄位映射串流功能");
        }
        ExportTemplateWithMappingCommand command = resourceAssembler.toCommand(templateKey, resource);
        String encodedFileName = URLEncoder.encode(command.outputFileName(), StandardCharsets.UTF_8).replace("+", "%20");

        response.setContentType(EXCEL_MIME_TYPE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName);

        streamExportTemplateWithMappingUseCase.streamExportFromTemplateWithMapping(response.getOutputStream(), command);
        response.flushBuffer();
    }

    /**
     * 預覽動態欄位映射後之 JXLS 資料模型 Context (Dry-Run / Preview API)。
     *
     * @param resource 具自訂映射規格與來源資料之請求資源載體
     * @return 供 JXLS 渲染消費之資料模型 JSON 結構
     */
    @Operation(summary = "預覽欄位動態映射後之 JXLS 資料模型 Context (Dry-Run / Preview)", description = "接收映射規格與來源資料，執行屬性導航、開箱、轉換器與虛擬表達式，回傳渲染前的資料上下文 JSON，便於除錯與驗證")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "成功求值並回傳 JXLS Context Map"),
        @ApiResponse(responseCode = "400", description = "請求參數格式不合法"),
        @ApiResponse(responseCode = "500", description = "伺服器內部產生異常")
    })
    @PostMapping("/mapping/preview")
    public ResponseEntity<Map<String, Object>> previewMapping(
            @Valid @RequestBody ExportTemplateWithMappingResource resource) {

        if (previewTemplateMappingUseCase == null) {
            throw new UnsupportedOperationException("未啟用動態欄位映射預覽功能");
        }
        String templateKey = (resource.mappingDefinition() != null && resource.mappingDefinition().templateKey() != null)
            ? resource.mappingDefinition().templateKey()
            : "DEFAULT_TEMPLATE";
        ExportTemplateWithMappingCommand command = resourceAssembler.toCommand(templateKey, resource);
        Map<String, Object> previewMap = previewTemplateMappingUseCase.previewTemplateMapping(command);

        return ResponseEntity.ok(previewMap);
    }

    /**
     * 建構標準 HTTP 附件下載回應 (Content-Disposition: attachment)。
     */
    private ResponseEntity<byte[]> createAttachmentResponse(byte[] fileBytes, String fileName) {
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(EXCEL_MIME_TYPE));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName);
        headers.setContentLength(fileBytes.length);
        return ResponseEntity.ok()
            .headers(headers)
            .body(fileBytes);
    }
}
