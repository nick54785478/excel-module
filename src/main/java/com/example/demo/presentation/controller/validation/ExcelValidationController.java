package com.example.demo.presentation.controller.validation;

import com.example.demo.application.annotation.validation.ValidateExcel;
import com.example.demo.application.annotation.validation.ValidatedExcelData;
import com.example.demo.application.port.in.validation.ValidateExcelUseCase;
import com.example.demo.application.port.out.core.ExcelReaderPort;
import com.example.demo.application.shared.dto.ExcelGottenResult;
import com.example.demo.application.shared.dto.ExcelSheetGottenResult;
import com.example.demo.application.shared.dto.ExcelValidationGottenResult;
import com.example.demo.application.shared.model.OrderModel;
import com.example.demo.config.ExcelTemplateConfiguration;
import com.example.demo.infrastructure.template.ExcelTemplateRegistry;
import com.example.demo.infrastructure.template.ExcelTemplateRegistry.TemplateDescriptor;
import com.example.demo.presentation.assembler.ExcelValidationResourceAssembler;
import com.example.demo.presentation.resource.out.ExcelValidatedResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Excel 客製驗證與「錯誤回標檔」生成 REST 控制器 (Excel Validation & Error Feedback Presentation Controller)
 * <p>
 * <b>【架構定位與職責規範】</b><br>
 * 位於 Clean Architecture 表現層 ({@code presentation/controller/})，遵循 {@code AGENTS.md} 規範。<br>
 * 作為外部 HTTP 批次檔案上傳檢核的入口點，調度 {@link ValidateExcelUseCase} 應用層業務案例，
 * 並透過 {@link ExcelValidationResourceAssembler} 進行防腐層 (ACL) 轉換。
 * </p>
 * <p>
 * <b>【開放封閉原則 (OCP) 與精簡接口架構】</b><br>
 * 控制器本身不針對特定業務模型（如訂單、商品、物料）硬編碼獨立方法，
 * 而是全面委託 {@link ExcelTemplateRegistry} 集中管理模型元數據與路由對照，
 * 僅收斂為 <b>5 隻核心精簡 API</b>：
 * <ol>
 *   <li>{@code GET /templates}：範本探索，取得所有已註冊之單據元數據</li>
 *   <li>{@code POST /{templateKey}/feedback}：OCP 動態範本校驗並生成「錯誤回標檔」</li>
 *   <li>{@code POST /{templateKey}}：OCP 動態範本校驗並回傳結構化 JSON 統計報告</li>
 *   <li>{@code POST /feedback}：Zero-Config 依上傳 Excel 工作表名稱 (Sheet Name) 智慧自動探測與回標</li>
 *   <li>{@code POST /declarative/orders}：宣告式 AOP 切面攔截防守與合法資料自動注入最佳實踐示範</li>
 * </ol>
 * </p>
 */
@Tag(name = "Excel 客製驗證與錯誤回標介面", description = "提供基於 EasyRules 與多層次規則管線之 Excel 上傳校驗、全量錯誤收集與視覺化錯誤回標檔生成 REST API")
@Validated
@RestController
@RequestMapping("/api/v1/excel/validation")
public class ExcelValidationController {

    private static final String EXCEL_MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ValidateExcelUseCase validateExcelUseCase;
    private final ExcelValidationResourceAssembler resourceAssembler;
    private final ExcelTemplateRegistry templateRegistry;
    private final ExcelReaderPort excelReaderPort;

    /**
     * Spring 容器自動裝配建構子：注入 UseCase、組裝器、範本註冊表與讀取埠
     *
     * @param validateExcelUseCase 檢核業務案例 (Application Inbound Port)
     * @param resourceAssembler    表現層資源組裝器 (Presentation Assembler)
     * @param templateRegistry     範本模型註冊表 (Infrastructure Template Registry)
     * @param excelReaderPort      試算表讀取輸出埠 (用於工作表名稱自動探測，可為空)
     */
    @Autowired
    public ExcelValidationController(
        ValidateExcelUseCase validateExcelUseCase,
        ExcelValidationResourceAssembler resourceAssembler,
        ExcelTemplateRegistry templateRegistry,
        @Autowired(required = false) ExcelReaderPort excelReaderPort) {
        this.validateExcelUseCase = Objects.requireNonNull(validateExcelUseCase, "validateExcelUseCase must not be null");
        this.resourceAssembler = Objects.requireNonNull(resourceAssembler, "resourceAssembler must not be null");
        this.templateRegistry = templateRegistry != null ? templateRegistry : ExcelTemplateConfiguration.createDefaultRegistry();
        this.excelReaderPort = excelReaderPort;
    }

    /**
     * 便捷建構子：用於單元測試環境快速建立實例 (維持回溯相容)
     *
     * @param validateExcelUseCase 檢核業務案例
     * @param resourceAssembler    表現層資源組裝器
     */
    public ExcelValidationController(ValidateExcelUseCase validateExcelUseCase, ExcelValidationResourceAssembler resourceAssembler) {
        this(validateExcelUseCase, resourceAssembler, ExcelTemplateConfiguration.createDefaultRegistry(), null);
    }

    // =========================================================================
    // 0. 內部共享處理核心邏輯 (遵循 DRY 原則)
    // =========================================================================

    /**
     * 統一核心檢核回標檔生成邏輯 (Unified Feedback Processing Core)
     * <p>
     * 負責調度 {@link ValidateExcelUseCase#validateAndGenerateFeedback(java.io.InputStream, Class)}，
     * 並執行全流程的分流判定、HTTP 標頭建構與錯誤回標檔之二進位輸出。
     * </p>
     *
     * @param file       上傳之 Excel 檔案 (MultipartFile)
     * @param descriptor 範本元數據描述符 (包含目標模型 Class 與回標檔名)
     * @return HTTP 回應實體：
     * <ul>
     *   <li>若檔案有違規資料：回傳帶有淡紅底深紅字之 Excel 二進位檔案下載 (HTTP 200 attachment)</li>
     *   <li>若檔案完全合格：智能降級回傳 HTTP 200 JSON 統計報告 (避免回傳空的或無意義之試算表)</li>
     *   <li>若檔案為空：回傳 HTTP 400 Bad Request</li>
     * </ul>
     * @throws IOException 當檔案讀取串流發生 I/O 異常時拋出
     */
    private ResponseEntity<?> processValidationWithFeedback(MultipartFile file, TemplateDescriptor descriptor) throws IOException {
        // ---------------------------------------------------------------------
        // 步驟 1：邊界防禦檢核 (Input Guard Clause)
        // ---------------------------------------------------------------------
        // 檢查 Multipart 上傳實體是否存在且內容長度大於 0。
        // 若前端未附加檔案或上傳 0 位元組空檔案，立即回傳 HTTP 400 Bad Request 阻斷無效請求。
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // ---------------------------------------------------------------------
        // 步驟 2：解析目標資料模型型別 (Model Class Resolution)
        // ---------------------------------------------------------------------
        // 從 TemplateDescriptor 提取綁定的實體模型型別 (例如 OrderModel.class 或 ProductRecordModel.class)，
        // 該型別將傳入 UseCase，供 ExcelBeanAssembler 動態反射解析表頭映射與啟動對應 EasyRules 檢核規則。
        Class<?> modelClass = descriptor.modelClass();

        // ---------------------------------------------------------------------
        // 步驟 3：調度應用層業務案例進行全量檢核與回標檔產製
        // ---------------------------------------------------------------------
        // 呼叫 ValidateExcelUseCase 執行多層次檢核管線：
        // 1. 底層讀取適配器解析二進位 Excel 串流。
        // 2. 應用層 ExcelBeanAssembler 將列資料轉化為 JavaBean / Record 模型。
        // 3. EasyRules 規則引擎累積行級與表級錯誤 (非短路模式，收集全量錯誤)。
        // 4. 若有錯誤，呼叫 ExcelErrorMarkerPort (POI) 針對出錯儲存格套用淡紅底深紅字樣式，並於最右側追加錯誤說明欄。
        byte[] feedbackFile = validateExcelUseCase.validateAndGenerateFeedback(file.getInputStream(), modelClass);

        // ---------------------------------------------------------------------
        // 步驟 4：分流判定與智慧降級 (Intelligent Fallback Mechanism)
        // ---------------------------------------------------------------------
        // 若 feedbackFile 為空或位元組長度為 0，代表上傳之資料 100% 合法，無須產製錯誤回標檔。
        // 此時執行業務降級，改為呼叫 validate() 取得統計結果，並經由 ACL 組裝器轉為 JSON 報告回應給前端。
        if (feedbackFile == null || feedbackFile.length == 0) {
            ExcelValidationGottenResult<?> result = validateExcelUseCase.validate(file.getInputStream(), modelClass);
            return ResponseEntity.ok(resourceAssembler.toResource(result));
        }

        // ---------------------------------------------------------------------
        // 步驟 5：建構二進位檔案下載 HTTP 標頭 (RFC 5987 UTF-8 檔名安全編碼)
        // ---------------------------------------------------------------------
        // 避免中文或特殊符號在不同瀏覽器 (如 Chrome, Firefox, Safari) 產生亂碼，
        // 依據 RFC 5987 規範同時輸出標準 filename 與 UTF-8 編碼之 filename*=UTF-8'' 格式。
        String encodedFilename = URLEncoder.encode(descriptor.feedbackFilename(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFilename + "\"; filename*=UTF-8''" + encodedFilename)
            .contentType(MediaType.parseMediaType(EXCEL_MIME_TYPE))
            .contentLength(feedbackFile.length)
            .body(feedbackFile);
    }

    /**
     * 統一核心 JSON 檢核邏輯 (Unified JSON Validation Processing Core)
     * <p>
     * 負責調度 {@link ValidateExcelUseCase#validate(java.io.InputStream, Class)} 進行資料檢核，
     * 並經由防腐層組裝器 {@link ExcelValidationResourceAssembler} 將應用層結果轉換為表現層資源物件。
     * </p>
     *
     * @param file       上傳之 Excel 檔案 (MultipartFile)
     * @param descriptor 範本元數據描述符
     * @return HTTP 回應實體：
     * <ul>
     *   <li>若全數合格：回傳 HTTP 200 OK 與成功 JSON 統計報告</li>
     *   <li>若發現違規資料：回傳 HTTP 422 Unprocessable Entity 與包含錯誤詳情的 JSON 報告</li>
     *   <li>若檔案為空：回傳 HTTP 400 Bad Request</li>
     * </ul>
     * @throws IOException 當檔案讀取串流發生 I/O 異常時拋出
     */
    private ResponseEntity<ExcelValidatedResource> processValidationJson(MultipartFile file, TemplateDescriptor descriptor) throws IOException {
        // ---------------------------------------------------------------------
        // 步驟 1：邊界防禦檢核 (Input Guard Clause)
        // ---------------------------------------------------------------------
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // ---------------------------------------------------------------------
        // 步驟 2：調度應用層檢核案例 (Invoke UseCase)
        // ---------------------------------------------------------------------
        Class<?> modelClass = descriptor.modelClass();
        ExcelValidationGottenResult<?> result = validateExcelUseCase.validate(file.getInputStream(), modelClass);

        // ---------------------------------------------------------------------
        // 步驟 3：防腐層 (ACL) 資源轉譯
        // ---------------------------------------------------------------------
        // 透過 ExcelValidationResourceAssembler 將應用層純結果 DTO 轉化為表現層純資料載體 ExcelValidatedResource。
        ExcelValidatedResource resource = resourceAssembler.toResource(result);

        // ---------------------------------------------------------------------
        // 步驟 4：REST 語意化狀態碼判定 (Semantic HTTP Status)
        // ---------------------------------------------------------------------
        // 若資料內含任何違規項目，依據 RFC 9110 規範回傳 HTTP 422 Unprocessable Entity；
        // 若全數合格，則回傳標準 HTTP 200 OK。
        HttpStatus status = result.hasErrors() ? HttpStatus.UNPROCESSABLE_CONTENT : HttpStatus.OK;
        return ResponseEntity.status(status).body(resource);
    }

    /**
     * 從上傳之 Excel 檔案首個工作表名稱反查已註冊之範本規格 (Zero-Config 自動偵測)
     * <p>
     * 透過 {@link ExcelReaderPort} 輕量讀取試算表第 1 個工作表的名稱 (Sheet Name)，
     * 並向 {@link ExcelTemplateRegistry#findBySheetName(String)} 進行反查匹配。
     * </p>
     *
     * @param file 上傳之 Excel 檔案
     * @return 匹配之範本描述符
     * @throws IOException              檔案讀取失敗
     * @throws IllegalArgumentException 若無法自動探測或查無匹配模型時拋出
     */
    private TemplateDescriptor resolveDescriptorFromExcel(MultipartFile file) throws IOException {
        // ---------------------------------------------------------------------
        // 步驟 1：確認 ExcelReaderPort 讀取埠是否已注入
        // ---------------------------------------------------------------------
        if (excelReaderPort != null) {
            // -----------------------------------------------------------------
            // 步驟 2：低開銷預覽工作表名稱 (Sheet Name Peek)
            // -----------------------------------------------------------------
            // 將上傳檔案轉為字節串流，僅解析試算表元數據與首個工作表名稱
            byte[] bytes = file.getBytes();
            ExcelGottenResult preview = excelReaderPort.read(new ByteArrayInputStream(bytes));
            String sheetName = preview.getFirstSheet().map(ExcelSheetGottenResult::sheetName).orElse("");

            // -----------------------------------------------------------------
            // 步驟 3：透過註冊表依工作表名稱反查 TemplateDescriptor
            // -----------------------------------------------------------------
            Optional<TemplateDescriptor> opt = templateRegistry.findBySheetName(sheetName);
            if (opt.isPresent()) {
                return opt.get();
            }
        }

        // ---------------------------------------------------------------------
        // 步驟 4：探測失敗之防禦與引導提示
        // ---------------------------------------------------------------------
        throw new IllegalArgumentException(
            "無法從試算表工作表名稱自動辨識相符之範本，請使用 /{templateKey}/feedback 明確指定範本。目前可用範本列表: " + templateRegistry.getAvailableKeys()
        );
    }

    // =========================================================================
    // 1. 精簡後的 5 隻核心對外 REST API
    // =========================================================================

    /**
     * API : 查詢所有已註冊之可用範本列表 (Template Discovery)
     * <p>
     * 回傳目前系統集中註冊的所有 Excel 業務範本資訊，包含標準鍵值、資料模型類別名稱、
     * 對應工作表名稱與預設錯誤回標檔名。便於前端動態呈現單據下拉選單。
     * </p>
     *
     * @return HTTP 200 與包含所有已註冊範本描述符之清單
     */
    @Operation(
        summary = "查詢所有已註冊之可用範本列表 (Template Discovery)",
        description = "回傳目前系統已註冊之所有 Excel 業務範本資訊 (鍵值、名稱、工作表名稱與預設回標檔名)，便於前端動態呈現或選單配置。"
    )
    @GetMapping("/templates")
    public ResponseEntity<List<TemplateDescriptor>> getRegisteredTemplates() {
        // ---------------------------------------------------------------------
        // 步驟 1：自共享註冊表查詢全量已註冊範本清單
        // ---------------------------------------------------------------------
        // 包含內建的 orders, products, materials, boms 以及執行期動態外掛之所有模型規格
        List<TemplateDescriptor> templates = templateRegistry.getAllTemplates();

        // ---------------------------------------------------------------------
        // 步驟 2：封裝為標準 HTTP 200 JSON 陣列回傳
        // ---------------------------------------------------------------------
        return ResponseEntity.ok(templates);
    }

    /**
     * API : 統一動態範本校驗並生成「錯誤回標檔」(OCP 核心統一接口)
     * <p>
     * <b>【開放封閉原則核心體現】</b><br>
     * 呼叫端透過路徑變數 {@code {templateKey}} 明確指定範本鍵值（例如 {@code orders}, {@code products},
     * {@code materials}, {@code boms} 等）。後端自動路由至對應模型執行檢核。<br>
     * 若資料包含錯誤，自動生成標註淡紅底深紅字之 Excel 檔案供下載排查；若全數合格則智能降級回傳 JSON 統計報告。
     * </p>
     *
     * @param templateKey 範本鍵值 (如 "orders", "products", "materials" 或 Model 類別名稱)
     * @param file        待檢核之 Excel 檔案 (.xlsx)
     * @return HTTP 200 下載回標檔案 (.xlsx) 或 HTTP 200 JSON 統計報告 (全數合格時)
     * @throws IOException 檔案處理或網路串流異常
     */
    @Operation(
        summary = "統一動態範本校驗並生成「錯誤回標檔」(OCP 核心統一接口)",
        description = "支援任意已註冊之業務範本 (如 orders, products, materials, boms 或自訂 templateKey)。若包含錯誤自動生成標註淡紅底深紅字之 Excel 回標檔供下載；全數合格則回傳 JSON 統計報告。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "檢核完全合格 (回傳成功 JSON) 或成功生成錯誤回標檔 (.xlsx) 下載", content = @Content(mediaType = EXCEL_MIME_TYPE)),
        @ApiResponse(responseCode = "400", description = "無效請求、檔案為空或未註冊之範本鍵值")
    })
    @PostMapping(value = "/{templateKey}/feedback", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> validateWithFeedback(
        @Parameter(description = "範本鍵值 (如 orders, products, materials, boms 或 Model 類別名稱)", required = true)
        @PathVariable("templateKey") String templateKey,
        @Parameter(description = "待檢核之 Excel 檔案 (.xlsx)", required = true)
        @RequestParam("file") MultipartFile file) throws IOException {

        // ---------------------------------------------------------------------
        // 步驟 1：依據路徑變數 templateKey 向註冊表取得範本元數據描述符
        // ---------------------------------------------------------------------
        // 支援別名、類別名與不分大小寫匹配 (若查無範本將拋出 IllegalArgumentException，由全域例外處理器轉為 400)
        TemplateDescriptor descriptor = templateRegistry.getRequired(templateKey);

        // ---------------------------------------------------------------------
        // 步驟 2：委託共用回標處理核心執行檢核、標註與 HTTP 串流封裝
        // ---------------------------------------------------------------------
        return processValidationWithFeedback(file, descriptor);
    }

    /**
     * API : 統一動態範本校驗 (回傳 JSON 統計結果 - OCP 核心統一接口)
     * <p>
     * 供前端在執行匯入前進行純資料檢核與預覽。執行行級規則 (EasyRules) 與表級防重複比對，
     * 回傳全量檢核錯誤清單、違規欄位名稱、錯誤原因與行數統計。
     * </p>
     *
     * @param templateKey 範本鍵值 (如 orders, products, materials, boms)
     * @param file        待檢核之 Excel 檔案 (.xlsx)
     * @return HTTP 200 (合格) 或 HTTP 422 (違規) 之結構化 JSON 統計報告
     * @throws IOException 檔案讀取異常
     */
    @Operation(
        summary = "統一動態範本校驗 (回傳 JSON 統計結果 - OCP 核心統一接口)",
        description = "支援任意已註冊之業務範本 (如 orders, products, materials, boms)。執行行級規則 (EasyRules) 檢核，回傳全量檢核錯誤清單與統計數據。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "完全合格或檢核完成報告", content = @Content(schema = @Schema(implementation = ExcelValidatedResource.class))),
        @ApiResponse(responseCode = "422", description = "檢核發現不合格資料 (包含錯誤細節清單)", content = @Content(schema = @Schema(implementation = ExcelValidatedResource.class))),
        @ApiResponse(responseCode = "400", description = "無效請求、檔案為空或未註冊之範本鍵值")
    })
    @PostMapping(value = "/{templateKey}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ExcelValidatedResource> validate(
        @Parameter(description = "範本鍵值 (如 orders, products, materials, boms 或 Model 類別名稱)", required = true)
        @PathVariable("templateKey") String templateKey,
        @Parameter(description = "待檢核之 Excel 檔案 (.xlsx)", required = true)
        @RequestParam("file") MultipartFile file) throws IOException {

        // ---------------------------------------------------------------------
        // 步驟 1：解析範本鍵值對應之描述符
        // ---------------------------------------------------------------------
        TemplateDescriptor descriptor = templateRegistry.getRequired(templateKey);

        // ---------------------------------------------------------------------
        // 步驟 2：委託共用核心執行純 JSON 檢核流程與 200 / 422 語意狀態碼判定
        // ---------------------------------------------------------------------
        return processValidationJson(file, descriptor);
    }

    /**
     * API : 智慧自動偵測範本並生成「錯誤回標檔」(Zero-Config 自動路由接口)
     * <p>
     * <b>【零配置極簡體驗】</b><br>
     * 呼叫端無需於 URL 中指定任何 {@code templateKey}，後端自動解析上傳試算表第 1 個工作表名稱 (Sheet Name)，
     * 動態匹配註冊表中對應之模型規格並完成檢核與回標檔生成。
     * </p>
     *
     * @param file 待檢核之 Excel 檔案 (.xlsx)
     * @return HTTP 200 下載回標檔案 (.xlsx) 或 HTTP 200 JSON 統計報告 (全數合格時)
     * @throws IOException 檔案讀取異常
     */
    @Operation(
        summary = "智慧自動偵測範本並生成「錯誤回標檔」(Zero-Config 自動路由接口)",
        description = "無須指定 templateKey，後端自動依據上傳 Excel 第一個工作表名稱 (Sheet Name) 自動匹配對應之模型規範並校驗產製回標檔。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "檢核完全合格 (回傳成功 JSON) 或成功生成錯誤回標檔 (.xlsx) 下載", content = @Content(mediaType = EXCEL_MIME_TYPE)),
        @ApiResponse(responseCode = "400", description = "無效請求、檔案為空或查無匹配工作表之範本")
    })
    @PostMapping(value = "/feedback", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> validateAutoDetectWithFeedback(
        @Parameter(description = "待檢核之 Excel 檔案 (.xlsx)", required = true)
        @RequestParam("file") MultipartFile file) throws IOException {

        // ---------------------------------------------------------------------
        // 步驟 1：前置檔案防禦檢查
        // ---------------------------------------------------------------------
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // ---------------------------------------------------------------------
        // 步驟 2：自上傳 Excel 檔案之第一個 Sheet 名稱動態探測範本描述符
        // ---------------------------------------------------------------------
        // 達成零配置 (Zero-Config)，呼叫端無需知道具體 templateKey 即可完成自動匹配
        TemplateDescriptor descriptor = resolveDescriptorFromExcel(file);

        // ---------------------------------------------------------------------
        // 步驟 3：委託核心回標方法執行檢核與檔案產製
        // ---------------------------------------------------------------------
        return processValidationWithFeedback(file, descriptor);
    }

    /**
     * API : 宣告式訂單匯入 (AOP 自動回標短路攔截最佳實踐示範)
     * <p>
     * <b>【AOP 註解驅動架構示範】</b><br>
     * 展示業務 Controller 如何透過標註 {@link ValidateExcel} 切面防守髒資料：
     * <ul>
     *   <li>若有任何違規，AOP 切面於最外層直接短路阻斷，直接回傳錯誤回標 Excel 下載，業務方法本體絕不執行。</li>
     *   <li>若資料 100% 合格，切面自動放行，並由參數解析器將合規資料自動注入至 {@code validOrders} 參數中！</li>
     * </ul>
     * </p>
     *
     * @param file        待檢核之訂單 Excel 檔案 (.xlsx)
     * @param validOrders 由切面與 ArgumentResolver 自動解析並注入的合格訂單清單 (保證 100% 合規)
     * @return HTTP 200 與匯入結果報告
     */
    @Operation(
        summary = "宣告式訂單匯入 (AOP 自動回標短路攔截最佳實踐示範)",
        description = "透過 @ValidateExcel 切面攔截，若有錯誤直接短路阻斷並下載錯誤回標 Excel；若合格則自動注入乾淨的 List<OrderModel>"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "完全合格回傳成功報告，或違規時直接下載錯誤回標檔 (.xlsx)", content = @Content(mediaType = EXCEL_MIME_TYPE)),
        @ApiResponse(responseCode = "400", description = "無效請求或檔案為空")
    })
    @PostMapping(value = "/declarative/orders", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ValidateExcel(
        model = OrderModel.class,
        action = ValidateExcel.OnErrorAction.AUTO_FEEDBACK,
        feedbackFilename = "AOP訂單檢核錯誤回標檔.xlsx"
    )
    public ResponseEntity<ExcelValidatedResource> declarativeValidateOrders(
        @Parameter(description = "待檢核之訂單 Excel 檔案 (.xlsx)", required = true)
        @RequestParam("file") MultipartFile file,
        @ValidatedExcelData List<OrderModel> validOrders) {

        // ---------------------------------------------------------------------
        // 步驟 1：[AOP 切面守護保證] 當程式執行抵達此方法時，代表資料已 100% 通過 EasyRules 檢核！
        // ---------------------------------------------------------------------
        // 若上傳檔案中存在任何違規資料，ExcelValidationAspect 已在最外層直接阻斷並觸發回標檔下載，
        // 絕不會進入此業務本體，徹底消除業務代碼對髒資料的防禦負擔。
        // 此外，validOrders 參數已由切面與 ArgumentResolver 自動注入反序列化後的合格清單！
        int totalCount = validOrders != null ? validOrders.size() : 0;

        // ---------------------------------------------------------------------
        // 步驟 2：直接針對 100% 合規之資料進行業務持久化或處理 (此處封裝為成功結果回傳)
        // ---------------------------------------------------------------------
        ExcelValidatedResource resource = new ExcelValidatedResource(
            true,
            totalCount,
            totalCount,
            0,
            Collections.emptyList(),
            validOrders
        );
        return ResponseEntity.ok(resource);
    }
}
