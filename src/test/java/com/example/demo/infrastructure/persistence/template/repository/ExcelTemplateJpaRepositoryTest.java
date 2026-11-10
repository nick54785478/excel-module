package com.example.demo.infrastructure.persistence.template.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.application.shared.dto.ExcelTemplateGottenResult;
import com.example.demo.infrastructure.persistence.template.entity.ExcelTemplateJpaEntity;

/**
 * Excel 範本 Spring Data JPA 儲存庫整合測試 (ExcelTemplateJpaRepository Integration Test)
 * <p>
 * 遵循 AGENTS.md 規則 5，所有測試方法均以 BDD Gherkin 語法規範註解，
 * 驗證持久化實體映射、CRUD 操作、條件衍生查詢、狀態過濾與 DTO 轉換功能。
 * </p>
 */
@SpringBootTest
@Transactional
@DisplayName("ExcelTemplateJpaRepository 範本持久化儲存庫整合測試")
class ExcelTemplateJpaRepositoryTest {

    @Autowired
    private ExcelTemplateJpaRepository templateRepository;

    /**
     * Feature: Excel 範本識別鍵檢索
     * Scenario: 成功查詢初始化 SQL 預載之採購比價單範本 (PROCUREMENT_COMPARISON)
     * Given 初始化資料庫已預載 3 筆內建範本資料 (包含 PROCUREMENT_COMPARISON)
     * When 調用 findByTemplateKey("PROCUREMENT_COMPARISON")
     * Then 應取得正確之 ExcelTemplateJpaEntity，且分類、名稱、必備變數解析正確
     */
    @Test
    @DisplayName("場景：依範本識別鍵查詢既有範本實體與變數解析")
    void shouldFindTemplateByKeySuccessfully() {
        // Given
        String templateKey = "PROCUREMENT_COMPARISON";

        // When
        Optional<ExcelTemplateJpaEntity> opt = templateRepository.findByTemplateKey(templateKey);

        // Then
        assertThat(opt).isPresent();
        ExcelTemplateJpaEntity entity = opt.get();
        assertThat(entity.getTemplateKey()).isEqualTo("PROCUREMENT_COMPARISON");
        assertThat(entity.getTemplateName()).isEqualTo("採購詢價比價單");
        assertThat(entity.getCategory()).isEqualTo("PROCUREMENT");
        assertThat(entity.getStorageType()).isEqualTo("CLASSPATH");
        assertThat(entity.getFilePath()).isEqualTo("templates/excel/procurement_comparison.xlsx");
        assertThat(entity.isActive()).isTrue();

        List<String> requiredVars = entity.getRequiredVariablesList();
        assertThat(requiredVars).containsExactlyInAnyOrder(
                "inquiryNo", "inquiryDate", "quotationItems", "vendorNames"
        );
    }

    /**
     * Feature: 範本業務分類檢索
     * Scenario: 依據業務分類查詢啟用中之範本清單
     * Given 資料庫已存在 MATERIAL 分類之範本
     * When 調用 findByCategoryAndActiveTrue("MATERIAL")
     * Then 應回傳包含 MATERIAL_MASTER 之清單
     */
    @Test
    @DisplayName("場景：依據業務分類查詢啟用中之範本清單")
    void shouldFindByCategoryAndActiveTrue() {
        // When
        List<ExcelTemplateJpaEntity> templates = templateRepository.findByCategoryAndActiveTrue("MATERIAL");

        // Then
        assertThat(templates).isNotEmpty();
        assertThat(templates).anyMatch(t -> "MATERIAL_MASTER".equals(t.getTemplateKey()));
    }

    /**
     * Feature: 全域有效啟用範本清單查詢
     * Scenario: 查詢所有標記為 active 之範本
     * Given 資料庫初始化已載入 3 筆啟用範本
     * When 調用 findByActiveTrue()
     * Then 回傳清單大小應至少為 3 筆，且皆為啟用狀態
     */
    @Test
    @DisplayName("場景：查詢全系統所有啟用中之範本")
    void shouldFindAllActiveTemplates() {
        // When
        List<ExcelTemplateJpaEntity> activeTemplates = templateRepository.findByActiveTrue();

        // Then
        assertThat(activeTemplates).hasSizeGreaterThanOrEqualTo(3);
        assertThat(activeTemplates).allMatch(ExcelTemplateJpaEntity::isActive);
    }

    /**
     * Feature: 新增客製化動態映射範本
     * Scenario: 建立新範本並設定預設欄位映射 JSON，驗證自動生成主鍵與 DTO 雙向轉換
     * Given 一個自訂之銷售統計範本實體，包含客製化欄位映射 JSON
     * When 呼叫 save 進行持久化，並以 findByTemplateKey 查詢
     * Then 資料庫應成功儲存，UUID 主鍵自動生成，且 toGottenResult 轉換資料無損
     */
    @Test
    @DisplayName("場景：新增自訂範本並驗證自動審計與 DTO 轉換")
    void shouldSaveAndConvertCustomTemplateSuccessfully() {
        // Given
        ExcelTemplateJpaEntity newTemplate = new ExcelTemplateJpaEntity();
        newTemplate.setTemplateKey("SALES_SUMMARY_2026");
        newTemplate.setTemplateName("2026 年度銷售總體績效表");
        newTemplate.setCategory("SALES");
        newTemplate.setDescription("涵蓋多區域業務銷售額、達成率與毛利分析");
        newTemplate.setRequiredVariablesList(List.of("salesOrders", "fiscalYear", "totalAmount"));
        newTemplate.setStorageType("CLASSPATH");
        newTemplate.setFilePath("templates/excel/sales_summary.xlsx");
        newTemplate.setDefaultMappingJson("{\"mappings\":[{\"sourceField\":\"amount\",\"targetField\":\"salesAmount\"}]}");
        newTemplate.setTenantId("TENANT_001");
        newTemplate.setActive(true);

        // When
        ExcelTemplateJpaEntity saved = templateRepository.save(newTemplate);

        // Then
        assertThat(saved.getId()).isNotNull().isNotBlank();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        Optional<ExcelTemplateJpaEntity> foundOpt = templateRepository.findByTemplateKey("SALES_SUMMARY_2026");
        assertThat(foundOpt).isPresent();
        ExcelTemplateJpaEntity found = foundOpt.get();
        assertThat(found.getDefaultMappingJson()).contains("salesAmount");

        // 驗證 toGottenResult 轉換
        ExcelTemplateGottenResult gottenResult = found.toGottenResult();
        assertThat(gottenResult.templateKey()).isEqualTo("SALES_SUMMARY_2026");
        assertThat(gottenResult.templateName()).isEqualTo("2026 年度銷售總體績效表");
        assertThat(gottenResult.category()).isEqualTo("SALES");
        assertThat(gottenResult.requiredVariables()).containsExactly("salesOrders", "fiscalYear", "totalAmount");
    }

    /**
     * Feature: 範本存在性檢查業務
     * Scenario: 驗證既有識別鍵與不存在識別鍵之檢查結果
     * Given 資料庫已存在 MATERIAL_MASTER 範本
     * When 分別調用 existsByTemplateKey 傳入既有與未註冊鍵值
     * Then 既有鍵應回傳 true，未註冊鍵應回傳 false
     */
    @Test
    @DisplayName("場景：檢查範本識別鍵是否存在")
    void shouldCheckTemplateExistence() {
        // When & Then
        assertThat(templateRepository.existsByTemplateKey("MATERIAL_MASTER")).isTrue();
        assertThat(templateRepository.existsByTemplateKey("NON_EXISTING_KEY")).isFalse();
        assertThat(templateRepository.existsByTemplateKeyAndActiveTrue("MATERIAL_MASTER")).isTrue();
    }

    /**
     * Feature: 範本停用與狀態過濾
     * Scenario: 停用範本後，findByTemplateKeyAndActiveTrue 不可再查獲該範本
     * Given 既有啟用中之 BOM_COST_ANALYSIS 範本
     * When 將其 active 設為 false 並存檔
     * Then findByTemplateKeyAndActiveTrue 應查無結果，但 findByTemplateKey 仍可查得實體
     */
    @Test
    @DisplayName("場景：停用範本後驗證有效狀態過濾")
    void shouldFilterInactiveTemplatesProperly() {
        // Given
        ExcelTemplateJpaEntity bomTemplate = templateRepository.findByTemplateKey("BOM_COST_ANALYSIS")
                .orElseThrow(() -> new IllegalStateException("找不到測試範本"));

        // When
        bomTemplate.setActive(false);
        templateRepository.save(bomTemplate);

        // Then
        Optional<ExcelTemplateJpaEntity> activeOpt = templateRepository.findByTemplateKeyAndActiveTrue("BOM_COST_ANALYSIS");
        assertThat(activeOpt).isEmpty();

        Optional<ExcelTemplateJpaEntity> allOpt = templateRepository.findByTemplateKey("BOM_COST_ANALYSIS");
        assertThat(allOpt).isPresent();
        assertThat(allOpt.get().isActive()).isFalse();
    }

    /**
     * Feature: 租戶隔離檢索業務
     * Scenario: 依據租戶代碼與範本鍵查詢多租戶隔離範本
     * Given 預載範本租戶識別碼為 DEFAULT
     * When 調用 findByTemplateKeyAndTenantIdAndActiveTrue 傳入 DEFAULT 與非該租戶代碼
     * Then 正確租戶應可查得實體，不同租戶則為 empty
     */
    @Test
    @DisplayName("場景：多租戶隔離條件查詢範本")
    void shouldFindTemplateByTenantIdSuccessfully() {
        // When & Then
        Optional<ExcelTemplateJpaEntity> defaultTenantOpt = templateRepository
                .findByTemplateKeyAndTenantIdAndActiveTrue("PROCUREMENT_COMPARISON", "DEFAULT");
        assertThat(defaultTenantOpt).isPresent();

        Optional<ExcelTemplateJpaEntity> otherTenantOpt = templateRepository
                .findByTemplateKeyAndTenantIdAndActiveTrue("PROCUREMENT_COMPARISON", "ANOTHER_TENANT");
        assertThat(otherTenantOpt).isEmpty();
    }
}

