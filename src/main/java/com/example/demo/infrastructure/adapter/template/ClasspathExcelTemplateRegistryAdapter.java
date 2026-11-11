package com.example.demo.infrastructure.adapter.template;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.example.demo.application.port.out.template.ExcelTemplateRegistryPort;
import com.example.demo.application.shared.dto.ExcelTemplateGottenResult;
import com.example.demo.application.shared.exception.ExcelTemplateNotFoundException;
import com.example.demo.infrastructure.persistence.template.entity.ExcelTemplateJpaEntity;
import com.example.demo.infrastructure.persistence.template.repository.ExcelTemplateJpaRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Classpath 與持久化資料庫整合之 Excel 範本儲存庫適配器 (Outbound Adapter)。
 * <p>
 * 遵循 AGENTS.md 規則 3-9，適配器一律以 {@code Adapter} 結尾。
 * 實作應用層 {@link ExcelTemplateRegistryPort}，採用「資料庫優先 (DB-First)」架構，
 * 透過 {@link ExcelTemplateJpaRepository} 查詢資料庫資料表 {@code t_excel_template} 中註冊之範本中繼規格與檔案儲存路徑，
 * 徹底消弭硬編碼範本中繼資訊 (DRY / Single Source of Truth 原則)。
 * </p>
 * <p>
 * 同時保留 {@link ConcurrentHashMap} 二進位位元組快取池與記憶體中繼註冊表，
 * 具備極致 I/O 效能、零停機熱刷新 (Hot-Reload) 以及在無資料庫或單元測試環境下的優雅降級能力。
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
     */
    private final String templatePrefix;

    /**
     * Excel 範本 JPA 持久化儲存庫 (可選注入，支援無資料庫環境降級)。
     */
    private final ExcelTemplateJpaRepository templateRepository;

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
     * 範本中繼資訊與變數結構規格的執行緒安全記憶體註冊表 (In-Memory Template Metadata Registry)。
     * <p>
     * 作為資料庫異常或動態手動註冊（例如測試、動態產製）的降級與補充容器。
     * </p>
     */
    private final Map<String, ExcelTemplateGottenResult> metadataRegistry = new ConcurrentHashMap<>();

    /**
     * 預設建構函式，由 Spring 容器依賴注入 {@link ExcelTemplateJpaRepository}。
     *
     * @param templateRepository 範本持久化儲存庫 (可為 null)
     */
    @Autowired
    public ClasspathExcelTemplateRegistryAdapter(
            @Autowired(required = false) ExcelTemplateJpaRepository templateRepository) {
        this(DEFAULT_TEMPLATE_PATH_PREFIX, templateRepository);
    }

    /**
     * 建構函式：指定自訂範本路徑前綴 (純記憶體降級模式)。
     *
     * @param templatePrefix 範本路徑前綴
     */
    public ClasspathExcelTemplateRegistryAdapter(String templatePrefix) {
        this(templatePrefix, null);
    }

    /**
     * 全參建構函式：支援自訂範本路徑前綴與持久化儲存庫注入。
     *
     * @param templatePrefix     範本路徑前綴
     * @param templateRepository 範本持久化儲存庫 (可為 null)
     */
    public ClasspathExcelTemplateRegistryAdapter(
            String templatePrefix,
            ExcelTemplateJpaRepository templateRepository) {
        this.templatePrefix = Objects.requireNonNull(templatePrefix, "templatePrefix 不得為空");
        this.templateRepository = templateRepository;
    }

    /**
     * 動態註冊或更新範本中繼資訊至記憶體快取容器。
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
        String normalizedKey = templateKey.toUpperCase(Locale.ROOT);
        // 1. 優先查詢資料庫 (Single Source of Truth)
        if (templateRepository != null) {
            try {
                Optional<ExcelTemplateGottenResult> dbResult = templateRepository
                        .findByTemplateKeyAndActiveTrue(normalizedKey)
                        .map(ExcelTemplateJpaEntity::toGottenResult);
                if (dbResult.isPresent()) {
                    return dbResult;
                }
            } catch (Exception e) {
                log.warn("自資料庫查詢範本中繼資訊異常，降級嘗試記憶體註冊表: key={}, error={}", normalizedKey, e.getMessage());
            }
        }
        // 2. 降級查詢動態記憶體註冊表
        return Optional.ofNullable(metadataRegistry.get(normalizedKey));
    }

    @Override
    public List<ExcelTemplateGottenResult> listTemplates() {
        Map<String, ExcelTemplateGottenResult> combined = new LinkedHashMap<>();
        // 1. 優先自資料庫載入所有啟用中之範本中繼規格
        if (templateRepository != null) {
            try {
                templateRepository.findByActiveTrue().forEach(entity -> {
                    ExcelTemplateGottenResult dto = entity.toGottenResult();
                    combined.put(dto.templateKey().toUpperCase(Locale.ROOT), dto);
                });
            } catch (Exception e) {
                log.warn("自資料庫載入範本清單異常，降級為純記憶體模式: error={}", e.getMessage());
            }
        }
        // 2. 合併手動或動態註冊於記憶體中之範本 (若資料庫無相同 key 則補入)
        metadataRegistry.forEach((key, dto) -> combined.putIfAbsent(key, dto));
        return List.copyOf(combined.values());
    }

    @Override
    public boolean exists(String templateKey) {
        if (templateKey == null || templateKey.isBlank()) {
            return false;
        }
        String normalizedKey = templateKey.toUpperCase(Locale.ROOT);
        // 1. 檢查記憶體二進位快取池
        if (templateCache.containsKey(normalizedKey)) {
            return true;
        }
        // 2. 檢查記憶體中繼註冊表
        if (metadataRegistry.containsKey(normalizedKey)) {
            return true;
        }
        // 3. 檢查資料庫持久化記錄
        if (templateRepository != null) {
            try {
                if (templateRepository.existsByTemplateKeyAndActiveTrue(normalizedKey)) {
                    return true;
                }
            } catch (Exception e) {
                log.warn("檢查資料庫範本存在性異常，降級檢查 Classpath 資源: key={}, error={}", normalizedKey, e.getMessage());
            }
        }
        // 4. 最後檢查 Classpath 實體檔案是否存在
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
        Resource resource = null;
        // 1. 若資料庫有註冊該範本且標註路徑，依據實體記錄路徑載入
        if (templateRepository != null) {
            try {
                Optional<ExcelTemplateJpaEntity> entityOpt = templateRepository.findByTemplateKeyAndActiveTrue(normalizedKey);
                if (entityOpt.isPresent()) {
                    ExcelTemplateJpaEntity entity = entityOpt.get();
                    if ("CLASSPATH".equalsIgnoreCase(entity.getStorageType()) && entity.getFilePath() != null) {
                        resource = new ClassPathResource(entity.getFilePath());
                    }
                }
            } catch (Exception e) {
                log.warn("自資料庫查詢範本路徑異常，降級使用預設 Classpath 規則: key={}, error={}", normalizedKey, e.getMessage());
            }
        }
        // 2. 若資料庫未指定或非 Classpath，使用預設規則路徑解析
        if (resource == null) {
            resource = resolveClasspathResource(normalizedKey);
        }
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

