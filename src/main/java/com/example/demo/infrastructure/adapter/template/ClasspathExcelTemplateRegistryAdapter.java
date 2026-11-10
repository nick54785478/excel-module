package com.example.demo.infrastructure.adapter.template;

import com.example.demo.application.port.out.template.ExcelTemplateRegistryPort;
import com.example.demo.application.shared.dto.ExcelTemplateGottenResult;
import com.example.demo.application.shared.exception.ExcelTemplateNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Classpath 範本儲存庫適配器 (Outbound Adapter)。
 * <p>
 * 遵循 AGENTS.md 規則 3-9，適配器一律以 {@code Adapter} 結尾。
 * 實作應用層 {@link ExcelTemplateRegistryPort}，
 * 自 {@code classpath:/templates/excel/} 載入 Excel 範本檔案，
 * 並透過 {@link ConcurrentHashMap} 提供記憶體位元組快取與零停機熱刷新支援。
 * </p>
 */
@Slf4j
@Component
public class ClasspathExcelTemplateRegistryAdapter implements ExcelTemplateRegistryPort {

    /**
     * 預設的 Classpath 實體 Excel 範本目錄前綴。
     * <p>
     * 在 Spring Boot 應用中，實體 .xlsx 範本檔案（如 material_master.xlsx、bom_cost_analysis.xlsx）
     * 統一存放於 {@code src/main/resources/templates/excel/} 路徑下。
     * </p>
     */
    private static final String DEFAULT_TEMPLATE_PATH_PREFIX = "templates/excel/";

    /**
     * Excel 實體範本檔案的標準副檔名 (OOXML 格式)。
     * <p>
     * 當透過範本識別鍵 (templateKey) 尋找實體檔案時，引擎會自動串接此副檔名組裝完整資源路徑。
     * </p>
     */
    private static final String FILE_EXTENSION = ".xlsx";

    /**
     * 當前適配器實例實際使用的範本資源路徑前綴。
     * <p>
     * 支援透過多載建構函式由外部抽換（例如在單元測試中指向 mock 路徑，或於多環境中指定客製目錄），
     * 預設為 {@link #DEFAULT_TEMPLATE_PATH_PREFIX}。
     * </p>
     */
    private final String templatePrefix;

    /**
     * 實體 Excel 範本檔案二進位位元組內容的執行緒安全快取池 (In-Memory Byte Array Cache)。
     * <p>
     * <b>【設計目的與架構效益】</b>
     * <ul>
     *   <li><b>極致 I/O 效能</b>：Key 為標準化範本鍵值 (大寫)，Value 為該檔案之完整 {@code byte[]} 內容。
     *       避免每次執行報表渲染時重複自硬碟或 JAR 包進行耗時的 Classpath I/O 讀取，達到 $O(1)$ 的極速常數開銷。</li>
     *   <li><b>零停機熱刷新 (Hot-Reload)</b>：支援於執行期間透過 {@code refreshCache()} 或手動註冊動態替換範本位元組，
     *       無需重新重啟服務或重新發版。</li>
     * </ul>
     * </p>
     */
    private final Map<String, byte[]> templateCache = new ConcurrentHashMap<>();

    /**
     * 範本中繼資訊與變數結構規格的執行緒安全註冊表 (Template Metadata Registry)。
     * <p>
     * <b>【設計目的與架構效益】</b>
     * <ul>
     *   <li><b>範本規格探索 (Discovery API)</b>：Key 為範本識別鍵 (大寫)，Value 為 {@link ExcelTemplateGottenResult}。
     *       提供對外 REST API (如 {@code GET /api/v1/excel/templates}) 快速查詢目前系統已註冊之範本名稱、業務分類、
     *       描述以及 JXLS 所需之必備變數清單 (requiredVariables)。</li>
     *   <li><b>防呆與前後端契約校驗</b>：讓調用端（前端或第三方微服務）在呼叫匯出前，能先行確認該範本所需的資料模型結構，
     *       避免因資料欄位缺失導致報表渲染空白。</li>
     * </ul>
     * </p>
     */
    private final Map<String, ExcelTemplateGottenResult> metadataRegistry = new ConcurrentHashMap<>();

    /**
     * 預設建構函式，指向預設 Classpath 路徑 {@code templates/excel/}。
     */
    public ClasspathExcelTemplateRegistryAdapter() {
        this(DEFAULT_TEMPLATE_PATH_PREFIX);
    }

    /**
     * 建構函式：指定自訂範本路徑前綴。
     *
     * @param templatePrefix 範本路徑前綴
     */
    public ClasspathExcelTemplateRegistryAdapter(String templatePrefix) {
        this.templatePrefix = Objects.requireNonNull(templatePrefix, "templatePrefix 不得為空");
        registerBuiltInMetadata();
    }

    /**
     * 預先註冊內建模組之核心範本中繼規格。
     */
    private void registerBuiltInMetadata() {
        registerMetadata(new ExcelTemplateGottenResult(
            "MATERIAL_MASTER",
            "物料主檔清單",
            "MATERIAL",
            "物料主檔資訊清單，基於 Material 聚合根，包含料號、品名、規格、分類、計量單位與標準成本",
            List.of("materials", "companyName", "exportDate"),
            Instant.now()
        ));
        registerMetadata(new ExcelTemplateGottenResult(
            "BOM_COST_ANALYSIS",
            "產品結構配方與成本分析展開表",
            "BOM",
            "產品結構配方與標準成本卷算分析表，基於 Bom 聚合根與 BomItem 實體，支援有效用量與成本加總動態公式",
            List.of("parentMaterialCode", "parentMaterialName", "version", "status", "baseQuantity", "currency", "items"),
            Instant.now()
        ));
        registerMetadata(new ExcelTemplateGottenResult(
            "PROCUREMENT_COMPARISON",
            "採購詢價比價單",
            "PROCUREMENT",
            "包含多供應商比價明細、最佳報價推薦與自適應加總公式",
            List.of("inquiryNo", "inquiryDate", "quotationItems", "vendorNames"),
            Instant.now()
        ));
    }

    /**
     * 動態註冊或更新範本中繼資訊。
     *
     * @param metadata 範本中繼規格 DTO
     */
    public void registerMetadata(ExcelTemplateGottenResult metadata) {
        Objects.requireNonNull(metadata, "metadata 不得為空");
        metadataRegistry.put(metadata.templateKey().toUpperCase(Locale.ROOT), metadata);
        log.debug("註冊 Excel 範本中繼資訊: key={}", metadata.templateKey());
    }

    /**
     * 手動向記憶體註冊範本二進位檔案內容 (例如用於測試或動態上傳情境)。
     *
     * @param templateKey   範本唯一識別鍵
     * @param templateBytes 範本二進位位元組陣列
     */
    public void registerTemplateBytes(String templateKey, byte[] templateBytes) {
        Objects.requireNonNull(templateKey, "templateKey 不得為空");
        Objects.requireNonNull(templateBytes, "templateBytes 不得為空");
        templateCache.put(templateKey.toUpperCase(Locale.ROOT), templateBytes.clone());
        log.info("手動快取 Excel 範本二進位資料: key={}, size={} bytes", templateKey, templateBytes.length);
    }

    @Override
    public InputStream openTemplateStream(String templateKey) {
        Objects.requireNonNull(templateKey, "templateKey 不得為空");
        String normalizedKey = templateKey.toUpperCase(Locale.ROOT);
        byte[] bytes = templateCache.computeIfAbsent(normalizedKey, this::loadTemplateBytes);
        return new ByteArrayInputStream(bytes);
    }

    @Override
    public Optional<ExcelTemplateGottenResult> findTemplateInfo(String templateKey) {
        if (templateKey == null || templateKey.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(metadataRegistry.get(templateKey.toUpperCase(Locale.ROOT)));
    }

    @Override
    public List<ExcelTemplateGottenResult> listTemplates() {
        return List.copyOf(metadataRegistry.values());
    }

    @Override
    public boolean exists(String templateKey) {
        if (templateKey == null || templateKey.isBlank()) {
            return false;
        }
        String normalizedKey = templateKey.toUpperCase(Locale.ROOT);
        if (templateCache.containsKey(normalizedKey)) {
            return true;
        }
        Resource resource = resolveClasspathResource(normalizedKey);
        return resource.exists();
    }

    /**
     * 清空記憶體範本快取，強制下次讀取時重新自磁碟載入。
     */
    public void clearCache() {
        templateCache.clear();
        log.info("已清空 Excel 範本快取容器");
    }

    /**
     * 刷新指定範本鍵值之快取。
     *
     * @param templateKey 範本唯一識別鍵
     */
    public void refreshCache(String templateKey) {
        if (templateKey != null) {
            String normalizedKey = templateKey.toUpperCase(Locale.ROOT);
            templateCache.remove(normalizedKey);
            loadTemplateBytes(normalizedKey);
            log.info("已刷新 Excel 範本快取: key={}", normalizedKey);
        }
    }

    private byte[] loadTemplateBytes(String normalizedKey) {
        Resource resource = resolveClasspathResource(normalizedKey);
        if (!resource.exists()) {
            throw new ExcelTemplateNotFoundException("找不到指定的 Excel 範本資源: " + resource.getDescription());
        }
        try (InputStream is = resource.getInputStream()) {
            byte[] bytes = is.readAllBytes();
            log.info("成功自 Classpath 載入並快取範本: key={}, path={}, size={} bytes",
                normalizedKey, resource.getDescription(), bytes.length);
            return bytes;
        } catch (Exception e) {
            throw new ExcelTemplateNotFoundException("讀取 Excel 範本檔案串流遭遇異常: " + normalizedKey, e);
        }
    }

    private Resource resolveClasspathResource(String normalizedKey) {
        String path = templatePrefix + normalizedKey.toLowerCase(Locale.ROOT) + FILE_EXTENSION;
        return new ClassPathResource(path);
    }
}

