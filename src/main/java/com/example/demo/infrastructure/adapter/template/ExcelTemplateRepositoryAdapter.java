package com.example.demo.infrastructure.adapter.template;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.application.port.out.template.ExcelTemplateRepositoryPort;
import com.example.demo.application.shared.command.CreateExcelTemplateCommand;
import com.example.demo.application.shared.command.UpdateExcelTemplateCommand;
import com.example.demo.application.shared.dto.ExcelTemplateGottenResult;
import com.example.demo.application.shared.exception.ExcelTemplateNotFoundException;
import com.example.demo.infrastructure.persistence.template.entity.ExcelTemplateJpaEntity;
import com.example.demo.infrastructure.persistence.template.repository.ExcelTemplateJpaRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Excel 範本持久化儲存庫輸出適配器 (Excel Template Repository Outbound Adapter)。
 * <p>
 * 遵循 Clean Architecture 六角形架構規範與 AGENTS.md 規則 3-9，以 {@code Adapter} 結尾。
 * 實作應用層 {@link ExcelTemplateRepositoryPort}，協調 Spring Data JPA {@link ExcelTemplateJpaRepository}
 * 執行範本持久化實體的建立、更新、查詢與刪除操作。
 * <b>嚴守防腐原則，禁止對外暴露 ORM 實體，僅接收與回傳純 Command / DTO</b>。
 * </p>
 */
@Slf4j
@Component
public class ExcelTemplateRepositoryAdapter implements ExcelTemplateRepositoryPort {

    private final ExcelTemplateJpaRepository templateJpaRepository;

    public ExcelTemplateRepositoryAdapter(ExcelTemplateJpaRepository templateJpaRepository) {
        this.templateJpaRepository = Objects.requireNonNull(templateJpaRepository, "ExcelTemplateJpaRepository 不得為空");
    }

    @Override
    @Transactional
    public ExcelTemplateGottenResult save(CreateExcelTemplateCommand command) {
        Objects.requireNonNull(command, "CreateExcelTemplateCommand 不得為空");
        String normalizedKey = command.templateKey().toUpperCase(Locale.ROOT);

        if (templateJpaRepository.existsByTemplateKey(normalizedKey)) {
            throw new IllegalArgumentException("建立範本失敗，範本鍵值已存在: " + normalizedKey);
        }

        ExcelTemplateJpaEntity entity = new ExcelTemplateJpaEntity();
        entity.setTemplateKey(normalizedKey);
        entity.setTemplateName(command.templateName());
        entity.setCategory(command.category().toUpperCase(Locale.ROOT));
        entity.setDescription(command.description());
        entity.setRequiredVariablesList(command.requiredVariables());
        entity.setStorageType(command.storageType());
        entity.setFilePath(command.filePath());
        entity.setDefaultMappingJson(command.defaultMappingJson());
        entity.setActive(true);

        ExcelTemplateJpaEntity saved = templateJpaRepository.saveAndFlush(entity);
        log.info("成功建立 Excel 範本持久化記錄: key={}, name={}", saved.getTemplateKey(), saved.getTemplateName());
        return saved.toGottenResult();
    }

    @Override
    @Transactional
    public ExcelTemplateGottenResult update(UpdateExcelTemplateCommand command) {
        Objects.requireNonNull(command, "UpdateExcelTemplateCommand 不得為空");
        String normalizedKey = command.templateKey().toUpperCase(Locale.ROOT);

        ExcelTemplateJpaEntity entity = templateJpaRepository.findByTemplateKey(normalizedKey)
                .orElseThrow(() -> new ExcelTemplateNotFoundException("更新範本失敗，查無該範本鍵值: " + normalizedKey));

        if (command.templateName() != null && !command.templateName().isBlank()) {
            entity.setTemplateName(command.templateName());
        }
        if (command.category() != null && !command.category().isBlank()) {
            entity.setCategory(command.category().toUpperCase(Locale.ROOT));
        }
        if (command.description() != null) {
            entity.setDescription(command.description());
        }
        if (command.requiredVariables() != null) {
            entity.setRequiredVariablesList(command.requiredVariables());
        }
        if (command.storageType() != null && !command.storageType().isBlank()) {
            entity.setStorageType(command.storageType());
        }
        if (command.filePath() != null && !command.filePath().isBlank()) {
            entity.setFilePath(command.filePath());
        }
        if (command.defaultMappingJson() != null) {
            entity.setDefaultMappingJson(command.defaultMappingJson());
        }
        if (command.active() != null) {
            entity.setActive(command.active());
        }

        ExcelTemplateJpaEntity saved = templateJpaRepository.saveAndFlush(entity);
        log.info("成功更新 Excel 範本持久化記錄: key={}, active={}", saved.getTemplateKey(), saved.isActive());
        return saved.toGottenResult();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ExcelTemplateGottenResult> findByTemplateKey(String templateKey) {
        if (templateKey == null || templateKey.isBlank()) {
            return Optional.empty();
        }
        return templateJpaRepository.findByTemplateKey(templateKey.toUpperCase(Locale.ROOT))
                .map(ExcelTemplateJpaEntity::toGottenResult);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExcelTemplateGottenResult> findAll() {
        return templateJpaRepository.findAll().stream()
                .map(ExcelTemplateJpaEntity::toGottenResult)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExcelTemplateGottenResult> findByActiveTrue() {
        return templateJpaRepository.findByActiveTrue().stream()
                .map(ExcelTemplateJpaEntity::toGottenResult)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExcelTemplateGottenResult> findByCategory(String category) {
        if (category == null || category.isBlank()) {
            return findByActiveTrue();
        }
        return templateJpaRepository.findByCategoryAndActiveTrue(category.toUpperCase(Locale.ROOT)).stream()
                .map(ExcelTemplateJpaEntity::toGottenResult)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByTemplateKey(String templateKey) {
        if (templateKey == null || templateKey.isBlank()) {
            return false;
        }
        return templateJpaRepository.existsByTemplateKey(templateKey.toUpperCase(Locale.ROOT));
    }

    @Override
    @Transactional
    public void deleteByTemplateKey(String templateKey) {
        Objects.requireNonNull(templateKey, "templateKey 不得為空");
        String normalizedKey = templateKey.toUpperCase(Locale.ROOT);

        ExcelTemplateJpaEntity entity = templateJpaRepository.findByTemplateKey(normalizedKey)
                .orElseThrow(() -> new ExcelTemplateNotFoundException("刪除範本失敗，查無該範本鍵值: " + normalizedKey));

        templateJpaRepository.delete(entity);
        log.info("成功自資料庫刪除 Excel 範本記錄: key={}", normalizedKey);
    }
}

