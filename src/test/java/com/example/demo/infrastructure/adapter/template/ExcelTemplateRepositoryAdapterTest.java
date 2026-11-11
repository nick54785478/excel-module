package com.example.demo.infrastructure.adapter.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.demo.application.shared.command.CreateExcelTemplateCommand;
import com.example.demo.application.shared.command.UpdateExcelTemplateCommand;
import com.example.demo.application.shared.dto.ExcelTemplateGottenResult;
import com.example.demo.application.shared.exception.ExcelTemplateNotFoundException;
import com.example.demo.infrastructure.persistence.template.entity.ExcelTemplateJpaEntity;
import com.example.demo.infrastructure.persistence.template.repository.ExcelTemplateJpaRepository;

/**
 * ExcelTemplateRepositoryAdapter 儲存庫輸出適配器單元測試。
 * <p>
 * 遵循 AGENTS.md 規則 5 測試規範，每個測試案例皆標註清晰之 Gherkin 語法註解。
 * 測試覆蓋建立範本 (Save)、更新範本 (Update)、條件查詢 (Find) 與刪除範本 (Delete)。
 * </p>
 */
@DisplayName("基礎設施層: ExcelTemplateRepositoryAdapter 儲存庫輸出適配器測試")
class ExcelTemplateRepositoryAdapterTest {

    private ExcelTemplateJpaRepository jpaRepository;
    private ExcelTemplateRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        jpaRepository = mock(ExcelTemplateJpaRepository.class);
        adapter = new ExcelTemplateRepositoryAdapter(jpaRepository);
    }

    /**
     * Feature: 建立新範本持久化記錄
     * Scenario: 傳入合法 CreateExcelTemplateCommand，成功儲存並回傳 DTO
     *   Given 範本鍵值 "EQUIPMENT_CHECK" 在資料庫中不存在
     *   When 呼叫 save 方法
     *   Then 應成功呼叫 saveAndFlush 並回傳轉換後的 ExcelTemplateGottenResult
     */
    @Test
    @DisplayName("Gherkin: 成功建立並儲存範本")
    void shouldSaveTemplateSuccessfully() {
        CreateExcelTemplateCommand command = new CreateExcelTemplateCommand(
            "EQUIPMENT_CHECK",
            "設備巡檢表",
            "MAINTENANCE",
            "設備巡檢與保養記錄表",
            List.of("equipments", "inspector"),
            "CLASSPATH",
            "templates/excel/equipment_check.xlsx",
            null
        );

        when(jpaRepository.existsByTemplateKey("EQUIPMENT_CHECK")).thenReturn(false);

        ExcelTemplateJpaEntity savedEntity = new ExcelTemplateJpaEntity(
            "uuid-101",
            "EQUIPMENT_CHECK",
            "設備巡檢表",
            "MAINTENANCE",
            "設備巡檢與保養記錄表",
            "equipments,inspector",
            "CLASSPATH",
            "templates/excel/equipment_check.xlsx",
            null,
            "DEFAULT",
            1,
            true,
            LocalDateTime.now(),
            LocalDateTime.now()
        );
        when(jpaRepository.saveAndFlush(any(ExcelTemplateJpaEntity.class))).thenReturn(savedEntity);

        ExcelTemplateGottenResult result = adapter.save(command);

        assertThat(result).isNotNull();
        assertThat(result.templateKey()).isEqualTo("EQUIPMENT_CHECK");
        assertThat(result.templateName()).isEqualTo("設備巡檢表");
        assertThat(result.category()).isEqualTo("MAINTENANCE");
        assertThat(result.requiredVariables()).containsExactly("equipments", "inspector");
    }

    /**
     * Feature: 範本鍵值重複防呆校驗
     * Scenario: 嘗試建立已存在鍵值之範本時拋出 IllegalArgumentException
     *   Given 範本鍵值 "EQUIPMENT_CHECK" 已存在於資料庫
     *   When 呼叫 save 方法
     *   Then 應拋出 IllegalArgumentException
     */
    @Test
    @DisplayName("Gherkin: 範本鍵值重複時拋出異常")
    void shouldThrowExceptionWhenTemplateKeyAlreadyExists() {
        CreateExcelTemplateCommand command = new CreateExcelTemplateCommand(
            "EQUIPMENT_CHECK",
            "設備巡檢表",
            "MAINTENANCE",
            "說明",
            List.of(),
            null,
            null,
            null
        );

        when(jpaRepository.existsByTemplateKey("EQUIPMENT_CHECK")).thenReturn(true);

        assertThatThrownBy(() -> adapter.save(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("範本鍵值已存在");
    }

    /**
     * Feature: 更新既有範本持久化記錄
     * Scenario: 傳入合法 UpdateExcelTemplateCommand 成功異動欄位
     *   Given 資料庫存在 "EQUIPMENT_CHECK" 實體
     *   When 呼叫 update 方法更新名稱與分類
     *   Then 應更新對應實體欄位並回傳更新後的 DTO
     */
    @Test
    @DisplayName("Gherkin: 成功更新範本規格資訊")
    void shouldUpdateTemplateSuccessfully() {
        ExcelTemplateJpaEntity existingEntity = new ExcelTemplateJpaEntity(
            "uuid-101",
            "EQUIPMENT_CHECK",
            "設備巡檢表",
            "MAINTENANCE",
            "原說明",
            "v1",
            "CLASSPATH",
            "path",
            null,
            "DEFAULT",
            1,
            true,
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        when(jpaRepository.findByTemplateKey("EQUIPMENT_CHECK")).thenReturn(Optional.of(existingEntity));
        when(jpaRepository.saveAndFlush(any(ExcelTemplateJpaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateExcelTemplateCommand command = new UpdateExcelTemplateCommand(
            "EQUIPMENT_CHECK",
            "設備巡檢與維護月報 (新版)",
            "OPERATIONS",
            "新說明",
            List.of("items", "date"),
            "CLASSPATH",
            "new/path.xlsx",
            null,
            true
        );

        ExcelTemplateGottenResult result = adapter.update(command);

        assertThat(result.templateName()).isEqualTo("設備巡檢與維護月報 (新版)");
        assertThat(result.category()).isEqualTo("OPERATIONS");
        assertThat(result.requiredVariables()).containsExactly("items", "date");
    }

    /**
     * Feature: 更新不存在範本防呆校驗
     * Scenario: 嘗試更新不存在之範本時拋出 ExcelTemplateNotFoundException
     *   Given 資料庫不存在該範本
     *   When 呼叫 update 方法
     *   Then 應拋出 ExcelTemplateNotFoundException
     */
    @Test
    @DisplayName("Gherkin: 更新不存在範本時拋出例外")
    void shouldThrowNotFoundWhenUpdatingNonExistentTemplate() {
        UpdateExcelTemplateCommand command = new UpdateExcelTemplateCommand(
            "NON_EXISTENT",
            "新名稱",
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );

        when(jpaRepository.findByTemplateKey("NON_EXISTENT")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.update(command))
            .isInstanceOf(ExcelTemplateNotFoundException.class)
            .hasMessageContaining("更新範本失敗");
    }

    /**
     * Feature: 刪除既有範本
     * Scenario: 傳入合法 templateKey，成功刪除資料庫記錄
     *   Given 資料庫存在 "EQUIPMENT_CHECK"
     *   When 呼叫 deleteByTemplateKey
     *   Then 應呼叫 jpaRepository.delete
     */
    @Test
    @DisplayName("Gherkin: 成功刪除指定範本")
    void shouldDeleteTemplateSuccessfully() {
        ExcelTemplateJpaEntity entity = new ExcelTemplateJpaEntity();
        entity.setTemplateKey("EQUIPMENT_CHECK");

        when(jpaRepository.findByTemplateKey("EQUIPMENT_CHECK")).thenReturn(Optional.of(entity));

        adapter.deleteByTemplateKey("EQUIPMENT_CHECK");

        verify(jpaRepository).delete(entity);
    }
}

