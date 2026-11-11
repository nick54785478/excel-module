package com.example.demo.application.service.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.demo.application.port.out.template.ExcelTemplateRegistryPort;
import com.example.demo.application.port.out.template.ExcelTemplateRepositoryPort;
import com.example.demo.application.shared.dto.ExcelTemplateGottenResult;
import com.example.demo.application.shared.exception.ExcelTemplateNotFoundException;
import com.example.demo.application.shared.query.GetExcelTemplateQuery;
import com.example.demo.application.shared.query.ListExcelTemplateQuery;

/**
 * 範本 Excel 查詢應用層服務單元測試 (ExcelTemplateQueryServiceTest)。
 * <p>
 * 遵循 AGENTS.md 規則 5 測試規範，每個測試案例皆標註清晰之 Gherkin 語法註解。
 * 測試覆蓋單一範本查詢、清單條件檢索、分類篩選與查無資料例外。
 * </p>
 */
@DisplayName("應用層: ExcelTemplateQueryService 查詢調度測試")
class ExcelTemplateQueryServiceTest {

    private ExcelTemplateRepositoryPort repositoryPort;
    private ExcelTemplateRegistryPort registryPort;
    private ExcelTemplateQueryService queryService;

    @BeforeEach
    void setUp() {
        repositoryPort = mock(ExcelTemplateRepositoryPort.class);
        registryPort = mock(ExcelTemplateRegistryPort.class);
        queryService = new ExcelTemplateQueryService(repositoryPort, registryPort);
    }

    /**
     * Feature: 依範本識別鍵查詢單一範本
     * Scenario: 傳入合法 templateKey，成功自 Repository 取得中繼資訊
     *   Given 儲存庫存在 "MATERIAL_MASTER" 範本中繼資訊
     *   When 呼叫 getTemplate
     *   Then 應回傳正確之 ExcelTemplateGottenResult
     */
    @Test
    @DisplayName("Gherkin: 成功查詢單一範本中繼規格")
    void shouldGetTemplateSuccessfully() {
        ExcelTemplateGottenResult expected = new ExcelTemplateGottenResult(
            "MATERIAL_MASTER",
            "物料主檔清單",
            "MATERIAL",
            "物料主檔清單",
            List.of("materials"),
            Instant.now()
        );

        when(repositoryPort.findByTemplateKey("MATERIAL_MASTER")).thenReturn(Optional.of(expected));

        ExcelTemplateGottenResult result = queryService.getTemplate(new GetExcelTemplateQuery("MATERIAL_MASTER"));

        assertThat(result).isNotNull();
        assertThat(result.templateKey()).isEqualTo("MATERIAL_MASTER");
        assertThat(result.templateName()).isEqualTo("物料主檔清單");
    }

    /**
     * Feature: 查無範本拋出例外
     * Scenario: 查詢不存在之範本鍵值
     *   Given 儲存庫查無該範本
     *   When 呼叫 getTemplate
     *   Then 應拋出 ExcelTemplateNotFoundException
     */
    @Test
    @DisplayName("Gherkin: 查無範本時拋出領域例外")
    void shouldThrowNotFoundExceptionWhenTemplateDoesNotExist() {
        when(repositoryPort.findByTemplateKey("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> queryService.getTemplate(new GetExcelTemplateQuery("UNKNOWN")))
            .isInstanceOf(ExcelTemplateNotFoundException.class)
            .hasMessageContaining("指定的 Excel 範本不存在");
    }

    /**
     * Feature: 依業務分類篩選範本清單
     * Scenario: 傳入分類 "PROCUREMENT" 進行查詢
     *   Given 儲存庫存在該分類之範本
     *   When 呼叫 listTemplates
     *   Then 應回傳符合該分類之範本清單
     */
    @Test
    @DisplayName("Gherkin: 依分類查詢範本清單")
    void shouldListTemplatesByCategory() {
        ExcelTemplateGottenResult rfq = new ExcelTemplateGottenResult(
            "PROCUREMENT_COMPARISON",
            "採購詢價比價單",
            "PROCUREMENT",
            "說明",
            List.of("items"),
            Instant.now()
        );

        when(repositoryPort.findByCategory("PROCUREMENT")).thenReturn(List.of(rfq));

        List<ExcelTemplateGottenResult> results = queryService.listTemplates(new ListExcelTemplateQuery("PROCUREMENT", true));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).templateKey()).isEqualTo("PROCUREMENT_COMPARISON");
    }
}

