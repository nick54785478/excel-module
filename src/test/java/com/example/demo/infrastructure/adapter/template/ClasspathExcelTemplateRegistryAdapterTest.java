package com.example.demo.infrastructure.adapter.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.demo.application.shared.dto.ExcelTemplateGottenResult;
import com.example.demo.application.shared.exception.ExcelTemplateNotFoundException;
import com.example.demo.infrastructure.persistence.template.entity.ExcelTemplateJpaEntity;
import com.example.demo.infrastructure.persistence.template.repository.ExcelTemplateJpaRepository;

/**
 * ClasspathExcelTemplateRegistryAdapter 儲存庫適配器單元測試。
 * <p>
 * 遵循 AGENTS.md 規則 5 測試規範，每個測試案例皆標註清晰之 Gherkin 語法註解。
 * 測試覆蓋資料庫優先 (DB-First) 範本中繼規格查詢、記憶體降級模式、二進位位元組快取命中、快取刷新與防呆例外。
 * </p>
 */
@DisplayName("基礎設施層: ClasspathExcelTemplateRegistryAdapter 儲存庫測試")
class ClasspathExcelTemplateRegistryAdapterTest {

    private ExcelTemplateJpaRepository templateRepository;
    private ClasspathExcelTemplateRegistryAdapter adapter;

    @BeforeEach
    void setUp() {
        templateRepository = mock(ExcelTemplateJpaRepository.class);
        adapter = new ClasspathExcelTemplateRegistryAdapter("templates/excel/", templateRepository);
    }

    /**
     * Feature: 資料庫優先 (DB-First) 範本中繼資料清單查詢
     * Scenario: 透過注入之 ExcelTemplateJpaRepository 查詢系統範本清單與特定中繼規格
     *   Given 資料庫持久化儲存庫包含 "PROCUREMENT_COMPARISON" 與 "BOM_COST_ANALYSIS" 實體
     *   When 呼叫 listTemplates 與 findTemplateInfo
     *   Then 應優先自資料庫回傳且欄位對齊
     */
    @Test
    @DisplayName("Gherkin: 優先自資料庫查詢範本中繼規格資訊")
    void shouldFindTemplateMetadataFromRepository() {
        ExcelTemplateJpaEntity rfqEntity = new ExcelTemplateJpaEntity(
            "id-1",
            "PROCUREMENT_COMPARISON",
            "採購詢價比價單",
            "PROCUREMENT",
            "包含多供應商比價明細、最佳報價推薦與自適應加總公式",
            "inquiryNo,inquiryDate,quotationItems,vendorNames",
            "CLASSPATH",
            "templates/excel/procurement_comparison.xlsx",
            null,
            "DEFAULT",
            1,
            true,
            LocalDateTime.now(),
            LocalDateTime.now()
        );
        ExcelTemplateJpaEntity bomEntity = new ExcelTemplateJpaEntity(
            "id-2",
            "BOM_COST_ANALYSIS",
            "產品結構配方與成本分析展開表",
            "BOM",
            "產品結構配方與標準成本卷算分析表",
            "parentMaterialCode,items",
            "CLASSPATH",
            "templates/excel/bom_cost_analysis.xlsx",
            null,
            "DEFAULT",
            1,
            true,
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        when(templateRepository.findByActiveTrue()).thenReturn(List.of(rfqEntity, bomEntity));
        when(templateRepository.findByTemplateKeyAndActiveTrue("PROCUREMENT_COMPARISON"))
            .thenReturn(Optional.of(rfqEntity));

        List<ExcelTemplateGottenResult> templates = adapter.listTemplates();
        assertThat(templates).isNotEmpty();
        assertThat(templates).extracting(ExcelTemplateGottenResult::templateKey)
            .contains("PROCUREMENT_COMPARISON", "BOM_COST_ANALYSIS");

        Optional<ExcelTemplateGottenResult> rfq = adapter.findTemplateInfo("procurement_comparison");
        assertThat(rfq).isPresent();
        assertThat(rfq.get().templateName()).isEqualTo("採購詢價比價單");
        assertThat(rfq.get().category()).isEqualTo("PROCUREMENT");
    }

    /**
     * Feature: 無資料庫環境降級支援 (Fallback to In-Memory)
     * Scenario: 在未配置 Repository 時動態註冊中繼資料
     *   Given 適配器在無 Repository 環境下運行
     *   When 手動註冊動態中繼規格
     *   Then findTemplateInfo 與 listTemplates 應正常自記憶體容器回傳
     */
    @Test
    @DisplayName("Gherkin: 無資料庫時優雅降級為純記憶體模式")
    void shouldFallbackToMemoryWhenRepositoryNotAvailable() {
        ClasspathExcelTemplateRegistryAdapter standaloneAdapter =
            new ClasspathExcelTemplateRegistryAdapter("templates/excel/");

        ExcelTemplateGottenResult dynamicDto = new ExcelTemplateGottenResult(
            "DYNAMIC_STANDALONE",
            "獨立動態範本",
            "CUSTOM",
            "無資料庫時之動態範本",
            List.of("var1"),
            Instant.now()
        );
        standaloneAdapter.registerMetadata(dynamicDto);

        Optional<ExcelTemplateGottenResult> result = standaloneAdapter.findTemplateInfo("DYNAMIC_STANDALONE");
        assertThat(result).isPresent();
        assertThat(result.get().templateName()).isEqualTo("獨立動態範本");

        List<ExcelTemplateGottenResult> list = standaloneAdapter.listTemplates();
        assertThat(list).extracting(ExcelTemplateGottenResult::templateKey)
            .contains("DYNAMIC_STANDALONE");
    }

    /**
     * Feature: 動態註冊中繼規格與記憶體位元組
     * Scenario: 註冊自訂範本位元組並透過 openTemplateStream 讀取
     *   Given 自訂範本鍵值 "CUSTOM_REPORT" 與二進位位元組陣列
     *   When 手動註冊並呼叫 exists 與 openTemplateStream
     *   Then exists 應回傳 true 且串流讀取內容與註冊位元組完全相符
     */
    @Test
    @DisplayName("Gherkin: 手動快取二進位範本並成功讀取串流")
    void shouldRegisterAndOpenTemplateStreamSuccessfully() throws Exception {
        String key = "CUSTOM_REPORT";
        byte[] sampleBytes = new byte[]{0x50, 0x4B, 0x03, 0x04}; // ZIP header signature

        adapter.registerMetadata(new ExcelTemplateGottenResult(
            key, "自訂客製報表", "CUSTOM", "用於單元測試之範本", List.of("title"), Instant.now()
        ));
        adapter.registerTemplateBytes(key, sampleBytes);

        assertThat(adapter.exists(key)).isTrue();
        assertThat(adapter.exists("custom_report")).isTrue();

        try (InputStream is = adapter.openTemplateStream(key)) {
            byte[] readBytes = is.readAllBytes();
            assertThat(readBytes).isEqualTo(sampleBytes);
        }
    }

    /**
     * Feature: 快取清除與刷新
     * Scenario: 註冊範本後執行 clearCache 與 refreshCache
     *   Given 已快取之範本資料
     *   When 呼叫 clearCache
     *   Then 再次開啟若磁碟不存在應拋出 ExcelTemplateNotFoundException
     */
    @Test
    @DisplayName("Gherkin: 清空快取後行為驗證")
    void shouldClearCacheProperly() {
        String key = "DYNAMIC_TPL";
        adapter.registerTemplateBytes(key, new byte[]{1, 2, 3});
        assertThat(adapter.exists(key)).isTrue();

        adapter.clearCache();

        assertThatThrownBy(() -> adapter.openTemplateStream(key))
            .isInstanceOf(ExcelTemplateNotFoundException.class)
            .hasMessageContaining("找不到指定的 Excel 範本資源");
    }

    /**
     * Feature: 不存在之範本回傳 false 或拋出例外
     * Scenario: 查詢不存在之範本
     *   Given 未註冊且不存在之範本鍵值 "NON_EXISTENT"
     *   When 呼叫 exists 與 openTemplateStream
     *   Then exists 應回傳 false 且 openTemplateStream 拋出例外
     */
    @Test
    @DisplayName("Gherkin: 不存在範本防呆校驗")
    void shouldHandleNonExistentTemplate() {
        assertThat(adapter.exists("NON_EXISTENT")).isFalse();
        assertThat(adapter.exists(null)).isFalse();
        assertThat(adapter.findTemplateInfo("NON_EXISTENT")).isEmpty();

        assertThatThrownBy(() -> adapter.openTemplateStream("NON_EXISTENT"))
            .isInstanceOf(ExcelTemplateNotFoundException.class);
    }
}

