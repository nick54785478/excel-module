package com.example.demo.presentation.controller.template;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.application.service.template.ExcelTemplateCommandService;
import com.example.demo.application.service.template.ExcelTemplateQueryService;
import com.example.demo.application.shared.command.CreateExcelTemplateCommand;
import com.example.demo.application.shared.command.DeleteExcelTemplateCommand;
import com.example.demo.application.shared.command.ExportTemplateExcelCommand;
import com.example.demo.application.shared.command.ExportTemplateWithMappingCommand;
import com.example.demo.application.shared.command.UpdateExcelTemplateCommand;
import com.example.demo.application.shared.dto.ExcelTemplateGottenResult;
import com.example.demo.application.shared.exception.ExcelTemplateNotFoundException;
import com.example.demo.presentation.assembler.ExcelTemplateResourceAssembler;
import com.example.demo.presentation.resource.in.CreateExcelTemplateResource;
import com.example.demo.presentation.resource.in.ExportTemplateExcelResource;
import com.example.demo.presentation.resource.in.ExportTemplateWithMappingResource;
import com.example.demo.presentation.resource.in.UpdateExcelTemplateResource;
import com.example.demo.presentation.resource.out.ExcelTemplateCreatedResource;
import com.example.demo.presentation.resource.out.ExcelTemplateRetrievedResource;
import com.example.demo.presentation.resource.out.ExcelTemplateUpdatedResource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import static com.example.demo.presentation.controller.template.ExcelTemplateOpenApiExamples.BOM_COST_ANALYSIS_EXAMPLE_JSON;
import static com.example.demo.presentation.controller.template.ExcelTemplateOpenApiExamples.MAPPED_CUSTOM_SOURCE_EXAMPLE_JSON;
import static com.example.demo.presentation.controller.template.ExcelTemplateOpenApiExamples.MAPPED_MATERIAL_MASTER_EXAMPLE_JSON;
import static com.example.demo.presentation.controller.template.ExcelTemplateOpenApiExamples.MAPPED_PROCUREMENT_COMPARISON_EXAMPLE_JSON;
import static com.example.demo.presentation.controller.template.ExcelTemplateOpenApiExamples.MATERIAL_MASTER_EXAMPLE_JSON;
import static com.example.demo.presentation.controller.template.ExcelTemplateOpenApiExamples.PROCUREMENT_COMPARISON_EXAMPLE_JSON;

/**
 * 宣告式範本 Excel 匯出與維護管理 REST API 控制器 (Presentation Layer - Inbound Adapter)。
 * <p>
 * 遵循 Clean Architecture 職責劃分，作為 HTTP 入口端點，
 * 接收 Request Resource，透過 {@link ExcelTemplateResourceAssembler} 轉為應用層 Command / Query，
 * 調度各項 UseCase 執行範本的 CRUD 維護、資料渲染與匯出，並將產物封裝為 HTTP 回應。
 * 同步支援純資料模型注入模式與動態欄位映射規格 (Dynamic Field Mapping) 模式。
 * </p>
 */
@Tag(name = "Excel 宣告式範本管理與匯出 API", description = "提供基於 JXLS 3.x 範本定義之 Excel 報表匯出、串流直通下載、動態欄位映射適配與範本規格 CRUD 維護端點")
@Validated
@RestController
@RequestMapping("/api/v1/excel/templates")
public class ExcelTemplateExportController {

    private static final String EXCEL_MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ExcelTemplateCommandService templateCommandService;
    private final ExcelTemplateQueryService templateQueryService;
    private final ExcelTemplateResourceAssembler resourceAssembler;

    /**
     * 建立 Excel 宣告式範本管理與匯出 REST API 控制器。
     * <p>
     * 依據 CQRS 架構職責收斂依賴注入元件：
     * <ul>
     *   <li>{@link ExcelTemplateCommandService}：負責範本匯出、串流直通、動態欄位映射與範本規格之新增/修改/刪除</li>
     *   <li>{@link ExcelTemplateQueryService}：負責範本清單檢索與單一規格查詢</li>
     *   <li>{@link ExcelTemplateResourceAssembler}：負責表現層 Resource 與應用層 Command/Query/Dto 相互轉換</li>
     * </ul>
     * </p>
     *
     * @param templateCommandService 範本命令應用層服務 (CQRS Command Service)
     * @param templateQueryService   範本查詢應用層服務 (CQRS Query Service)
     * @param resourceAssembler      表現層資料轉換組裝器
     */
    @Autowired
    public ExcelTemplateExportController(
            ExcelTemplateCommandService templateCommandService,
            ExcelTemplateQueryService templateQueryService,
            ExcelTemplateResourceAssembler resourceAssembler) {
        this.templateCommandService = Objects.requireNonNull(templateCommandService, "templateCommandService 不得為空");
        this.templateQueryService = Objects.requireNonNull(templateQueryService, "templateQueryService 不得為空");
        this.resourceAssembler = Objects.requireNonNull(resourceAssembler, "resourceAssembler 不得為空");
    }

    /**
     * 建立並註冊新的 Excel 範本規格 (Create)。
     *
     * @param resource 建立範本請求資源
     * @return 201 Created 與建立完成之範本回應資源
     */
    @Operation(summary = "建立並註冊新的 Excel 範本", description = "註冊全新範本識別鍵、名稱、分類、描述與必備變數清單")
    @ApiResponse(responseCode = "201", description = "成功建立並註冊範本")
    @ApiResponse(responseCode = "400", description = "請求參數格式不合法或範本鍵值重複")
    @PostMapping
    public ResponseEntity<ExcelTemplateCreatedResource> createTemplate(
            @Valid @RequestBody CreateExcelTemplateResource resource) {
        CreateExcelTemplateCommand command = resourceAssembler.toCommand(resource);
        ExcelTemplateGottenResult result = templateCommandService.createTemplate(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(resourceAssembler.toCreatedResource(result));
    }

    /**
     * 更新既有 Excel 範本中繼資訊 (Update)。
     *
     * @param templateKey 範本唯一識別鍵
     * @param resource    更新範本請求資源
     * @return 200 OK 與更新完成之範本回應資源
     */
    @Operation(summary = "更新既有 Excel 範本規格", description = "異動範本顯示名稱、分類、用途說明、必備變數清單或啟用狀態")
    @ApiResponse(responseCode = "200", description = "成功更新範本規格")
    @ApiResponse(responseCode = "404", description = "找不到指定範本")
    @PutMapping("/{templateKey}")
    public ResponseEntity<ExcelTemplateUpdatedResource> updateTemplate(
            @Parameter(description = "範本識別鍵", example = "PROCUREMENT_COMPARISON")
            @PathVariable String templateKey,
            @Valid @RequestBody UpdateExcelTemplateResource resource) {
        UpdateExcelTemplateCommand command = resourceAssembler.toCommand(templateKey, resource);
        ExcelTemplateGottenResult result = templateCommandService.updateTemplate(command);
        return ResponseEntity.ok(resourceAssembler.toUpdatedResource(result));
    }

    /**
     * 刪除或下架指定 Excel 範本 (Delete)。
     *
     * @param templateKey 範本唯一識別鍵
     * @return 204 No Content
     */
    @Operation(summary = "刪除指定 Excel 範本", description = "自系統移除指定識別鍵之範本持久化記錄並自動更新快取")
    @ApiResponse(responseCode = "204", description = "成功刪除指定範本")
    @ApiResponse(responseCode = "404", description = "找不到指定範本")
    @DeleteMapping("/{templateKey}")
    public ResponseEntity<Void> deleteTemplate(
            @Parameter(description = "範本識別鍵", example = "PROCUREMENT_COMPARISON")
            @PathVariable String templateKey) {
        DeleteExcelTemplateCommand command = resourceAssembler.toDeleteCommand(templateKey);
        templateCommandService.deleteTemplate(command);
        return ResponseEntity.noContent().build();
    }

    /**
     * 查詢所有已註冊之 Excel 範本清單與中繼規格 (Read - List)。
     *
     * @param category   業務分類篩選 (可選)
     * @param activeOnly 是否僅回傳啟用中範本 (可選)
     * @return 範本中繼資訊清單
     */
    @Operation(summary = "查詢所有已註冊之 Excel 範本清單", description = "取得系統已註冊之範本識別鍵、名稱、業務分類與必備變數清單，支援依分類篩選")
    @ApiResponse(responseCode = "200", description = "成功取得範本清單")
    @GetMapping
    public ResponseEntity<List<ExcelTemplateRetrievedResource>> listTemplates(
            @Parameter(description = "業務分類篩選代碼 (例如 PROCUREMENT, BOM, MATERIAL)")
            @RequestParam(required = false) String category,
            @Parameter(description = "是否僅查詢啟用中範本")
            @RequestParam(required = false, defaultValue = "true") Boolean activeOnly) {
        List<ExcelTemplateGottenResult> dtos = templateQueryService.listTemplates(
                resourceAssembler.toListQuery(category, activeOnly));
        List<ExcelTemplateRetrievedResource> list = dtos.stream()
            .map(resourceAssembler::toResource)
            .toList();
        return ResponseEntity.ok(list);
    }

    /**
     * 查詢單一範本之中繼規格資訊 (Read - Get)。
     *
     * @param templateKey 範本唯一識別鍵
     * @return 範本中繼規格回應資源
     */
    @Operation(summary = "查詢單一範本中繼規格", description = "依據範本鍵值獲取該範本之詳細欄位規格與描述")
    @ApiResponse(responseCode = "200", description = "成功取得範本規格")
    @ApiResponse(responseCode = "404", description = "找不到指定範本")
    @GetMapping("/{templateKey}")
    public ResponseEntity<ExcelTemplateRetrievedResource> getTemplateInfo(
            @Parameter(description = "範本識別鍵", example = "PROCUREMENT_COMPARISON")
            @PathVariable String templateKey) {
        try {
            ExcelTemplateGottenResult dto = templateQueryService.getTemplate(resourceAssembler.toQuery(templateKey));
            return ResponseEntity.ok(resourceAssembler.toResource(dto));
        } catch (ExcelTemplateNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 依據範本與動態資料模型匯出 Excel 檔案 (記憶體緩衝 Buffered 模式)。
     *
     * @param templateKey 範本識別鍵
     * @param resource    匯出請求資源載體
     * @return Excel (.xlsx) 二進位位元組附件下載
     */
    @Operation(
            summary = "依據範本匯出 Excel 檔案 (Buffered 模式)",
            description = "接收資料上下文，以 JXLS 範本渲染並一次性回傳二進位位元組檔案下載",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "範本 Excel 匯出請求載體，包含 dataModel 資料模型與自訂輸出檔名",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ExportTemplateExcelResource.class),
                            examples = {
                                    @ExampleObject(
                                            name = "採購詢價比價單範例 (PROCUREMENT_COMPARISON)",
                                            summary = "符合 templateKey 為 PROCUREMENT_COMPARISON 之完整資料模型範例",
                                            value = PROCUREMENT_COMPARISON_EXAMPLE_JSON
                                    ),
                                    @ExampleObject(
                                            name = "物料主檔清單範例 (MATERIAL_MASTER)",
                                            summary = "符合 templateKey 為 MATERIAL_MASTER 之完整資料模型範例",
                                            value = MATERIAL_MASTER_EXAMPLE_JSON
                                    ),
                                    @ExampleObject(
                                            name = "產品結構成本分析範例 (BOM_COST_ANALYSIS)",
                                            summary = "符合 templateKey 為 BOM_COST_ANALYSIS 之完整資料模型範例",
                                            value = BOM_COST_ANALYSIS_EXAMPLE_JSON
                                    )
                            }
                    )
            )
    )
    @ApiResponse(responseCode = "200", description = "成功產製並回傳檔案二進位資料")
    @ApiResponse(responseCode = "400", description = "請求參數格式不合法")
    @ApiResponse(responseCode = "404", description = "指定範本不存在")
    @ApiResponse(responseCode = "500", description = "伺服器內部產生異常")
    @PostMapping("/{templateKey}/export")
    public ResponseEntity<byte[]> exportFromTemplate(
            @Parameter(description = "範本識別鍵", example = "PROCUREMENT_COMPARISON")
            @PathVariable String templateKey,
            @Valid @RequestBody ExportTemplateExcelResource resource) {

        ExportTemplateExcelCommand command = resourceAssembler.toCommand(templateKey, resource);
        byte[] excelBytes = templateCommandService.exportFromTemplate(command);

        return createAttachmentResponse(excelBytes, command.outputFileName());
    }

    /**
     * 依據範本與動態資料模型串流匯出 Excel 檔案 (Zero-Copy HTTP 串流模式)。
     *
     * @param templateKey 範本識別鍵
     * @param resource    匯出請求資源載體
     * @param response    HTTP Servlet 回應物件
     * @throws IOException 串流寫入 I/O 異常
     */
    @Operation(
            summary = "依據範本串流匯出 Excel 檔案 (Zero-Copy 串流模式)",
            description = "以低記憶體串流方式將 JXLS 產物直通 HTTP Response OutputStream",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "範本 Excel 串流匯出請求載體，包含 dataModel 資料模型與自訂輸出檔名",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ExportTemplateExcelResource.class),
                            examples = {
                                    @ExampleObject(
                                            name = "採購詢價比價單範例 (PROCUREMENT_COMPARISON)",
                                             summary = "符合 templateKey 為 PROCUREMENT_COMPARISON 之完整資料模型範例",
                                             value = PROCUREMENT_COMPARISON_EXAMPLE_JSON
                                     ),
                                     @ExampleObject(
                                             name = "物料主檔清單範例 (MATERIAL_MASTER)",
                                             summary = "符合 templateKey 為 MATERIAL_MASTER 之完整資料模型範例",
                                             value = MATERIAL_MASTER_EXAMPLE_JSON
                                     ),
                                     @ExampleObject(
                                             name = "產品結構成本分析範例 (BOM_COST_ANALYSIS)",
                                             summary = "符合 templateKey 為 BOM_COST_ANALYSIS 之完整資料模型範例",
                                             value = BOM_COST_ANALYSIS_EXAMPLE_JSON
                                     )
                             }
                     )
             )
     )
    @ApiResponse(responseCode = "200", description = "成功建立串流傳輸")
    @ApiResponse(responseCode = "404", description = "指定範本不存在")
    @ApiResponse(responseCode = "500", description = "伺服器串流輸出異常")
    @PostMapping("/{templateKey}/stream")
    public void streamExportFromTemplate(
            @Parameter(description = "範本識別鍵", example = "PROCUREMENT_COMPARISON")
            @PathVariable String templateKey,
            @Valid @RequestBody ExportTemplateExcelResource resource,
            HttpServletResponse response) throws IOException {

        ExportTemplateExcelCommand command = resourceAssembler.toCommand(templateKey, resource);

        response.setContentType(EXCEL_MIME_TYPE);
        String encodedFileName = URLEncoder.encode(command.outputFileName(), StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName);

        templateCommandService.streamExportFromTemplate(response.getOutputStream(), command);
        response.flushBuffer();
    }

    /**
     * 依據範本與動態欄位映射匯出 Excel (Buffered 模式)。
     *
     * @param templateKey 範本識別鍵
     * @param resource    自訂映射請求本文
     * @return Excel (.xlsx) 二進位位元組附件下載
     */
    @Operation(
            summary = "依據自訂欄位映射匯出 Excel 範本報表",
            description = "接收來源資料物件清單與宣告式欄位映射規格 (JSON)，動態適配轉換後執行 JXLS 渲染",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "動態欄位映射匯出請求本文，包含 mappingDefinition 規則集合與自訂 sourceData (選填)",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ExportTemplateWithMappingResource.class),
                            examples = {
                                    @ExampleObject(
                                            name = "標準物料主檔領域開箱映射範例 (MATERIAL_MASTER)",
                                            summary = "免傳 sourceData，自動開箱 Material 領域聚合根並執行千分位貨幣格式化與列舉字典轉譯",
                                            value = MAPPED_MATERIAL_MASTER_EXAMPLE_JSON
                                    ),
                                    @ExampleObject(
                                            name = "自訂來源業務資料多租戶映射範例 (Custom Source Data)",
                                            summary = "前端傳入自訂 sourceData 陣列與自訂欄位路徑動態適配",
                                            value = MAPPED_CUSTOM_SOURCE_EXAMPLE_JSON
                                    ),
                                    @ExampleObject(
                                            name = "採購比價單欄位動態映射範例 (PROCUREMENT_COMPARISON)",
                                            summary = "PROCUREMENT_COMPARISON 範本之欄位映射與全域中繼資料注入範例",
                                            value = MAPPED_PROCUREMENT_COMPARISON_EXAMPLE_JSON
                                    )
                            }
                    )
            )
    )
    @ApiResponse(responseCode = "200", description = "成功動態映射並產製檔案")
    @ApiResponse(responseCode = "400", description = "映射規則無效或資料格式錯誤")
    @ApiResponse(responseCode = "404", description = "指定範本不存在")
    @PostMapping("/{templateKey}/export-mapped")
    public ResponseEntity<byte[]> exportFromTemplateWithMapping(
            @Parameter(description = "範本識別鍵", example = "MATERIAL_MASTER")
            @PathVariable String templateKey,
            @Valid @RequestBody ExportTemplateWithMappingResource resource) {

        ExportTemplateWithMappingCommand command = resourceAssembler.toCommand(templateKey, resource);
        byte[] excelBytes = templateCommandService.exportFromTemplateWithMapping(command);

        return createAttachmentResponse(excelBytes, command.outputFileName());
    }

    /**
     * 依據範本與動態欄位映射串流匯出 Excel (Zero-Copy 串流模式)。
     *
     * @param templateKey 範本識別鍵
     * @param resource    自訂映射請求本文
     * @param response    HTTP Servlet 回應物件
     * @throws IOException 串流寫入 I/O 異常
     */
    @Operation(
            summary = "依據自訂欄位映射串流匯出 Excel (Zero-Copy 模式)",
            description = "接收來源資料與宣告式欄位映射規格，動態映射後以串流模式直接輸出至 HTTP 客戶端",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "動態欄位映射串流匯出請求本文，包含 mappingDefinition 規則集合與自訂 sourceData (選填)",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ExportTemplateWithMappingResource.class),
                            examples = {
                                    @ExampleObject(
                                            name = "標準物料主檔領域開箱映射範例 (MATERIAL_MASTER)",
                                            summary = "免傳 sourceData，自動開箱 Material 領域聚合根並執行千分位貨幣格式化與列舉字典轉譯",
                                            value = MAPPED_MATERIAL_MASTER_EXAMPLE_JSON
                                    ),
                                    @ExampleObject(
                                            name = "自訂來源業務資料多租戶映射範例 (Custom Source Data)",
                                            summary = "前端傳入自訂 sourceData 陣列與自訂欄位路徑動態適配",
                                            value = MAPPED_CUSTOM_SOURCE_EXAMPLE_JSON
                                    ),
                                    @ExampleObject(
                                            name = "採購比價單欄位動態映射範例 (PROCUREMENT_COMPARISON)",
                                            summary = "PROCUREMENT_COMPARISON 範本之欄位映射與全域中繼資料注入範例",
                                            value = MAPPED_PROCUREMENT_COMPARISON_EXAMPLE_JSON
                                    )
                            }
                    )
            )
    )
    @ApiResponse(responseCode = "200", description = "成功建立串流傳輸")
    @ApiResponse(responseCode = "404", description = "指定範本不存在")
    @PostMapping("/{templateKey}/stream-mapped")
    public void streamExportFromTemplateWithMapping(
            @Parameter(description = "範本識別鍵", example = "MATERIAL_MASTER")
            @PathVariable String templateKey,
            @Valid @RequestBody ExportTemplateWithMappingResource resource,
            HttpServletResponse response) throws IOException {

        ExportTemplateWithMappingCommand command = resourceAssembler.toCommand(templateKey, resource);

        response.setContentType(EXCEL_MIME_TYPE);
        String encodedFileName = URLEncoder.encode(command.outputFileName(), StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName);

        templateCommandService.streamExportFromTemplateWithMapping(response.getOutputStream(), command);
        response.flushBuffer();
    }

    /**
     * 預覽動態欄位映射轉換結果。
     *
     * @param resource 自訂映射請求本文
     * @return 映射後的資料上下文 Map
     */
    @Operation(
            summary = "預覽動態欄位映射轉換結果 (除錯與校驗用)",
            description = "不執行實體 Excel 檔案渲染，僅回傳資料模型經由 Field Mapping 引擎轉換後的結構化 JSON",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "動態欄位映射預覽請求本文，包含 mappingDefinition 規則集合與自訂 sourceData (選填)",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ExportTemplateWithMappingResource.class),
                            examples = {
                                    @ExampleObject(
                                            name = "標準物料主檔領域開箱映射範例 (MATERIAL_MASTER)",
                                            summary = "免傳 sourceData，自動開箱 Material 領域聚合根並執行千分位貨幣格式化與列舉字典轉譯",
                                            value = MAPPED_MATERIAL_MASTER_EXAMPLE_JSON
                                    ),
                                    @ExampleObject(
                                            name = "自訂來源業務資料多租戶映射範例 (Custom Source Data)",
                                            summary = "前端傳入自訂 sourceData 陣列與自訂欄位路徑動態適配",
                                            value = MAPPED_CUSTOM_SOURCE_EXAMPLE_JSON
                                    ),
                                    @ExampleObject(
                                            name = "採購比價單欄位動態映射範例 (PROCUREMENT_COMPARISON)",
                                            summary = "PROCUREMENT_COMPARISON 範本之欄位映射與全域中繼資料注入範例",
                                            value = MAPPED_PROCUREMENT_COMPARISON_EXAMPLE_JSON
                                    )
                            }
                    )
            )
    )
    @ApiResponse(responseCode = "200", description = "成功預覽映射結果")
    @ApiResponse(responseCode = "400", description = "映射規則無效")
    @PostMapping("/mapping/preview")
    public ResponseEntity<Map<String, Object>> previewMapping(
            @Valid @RequestBody ExportTemplateWithMappingResource resource) {

        String templateKey = (resource.mappingDefinition() != null && resource.mappingDefinition().templateKey() != null)
            ? resource.mappingDefinition().templateKey()
            : "DEFAULT_TEMPLATE";
        ExportTemplateWithMappingCommand command = resourceAssembler.toCommand(templateKey, resource);
        Map<String, Object> previewMap = templateCommandService.previewTemplateMapping(command);

        return ResponseEntity.ok(previewMap);
    }

    private ResponseEntity<byte[]> createAttachmentResponse(byte[] content, String fileName) {
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(EXCEL_MIME_TYPE))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
            .body(content);
    }
}
