package com.example.demo.infrastructure.persistence.task.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.infrastructure.persistence.task.entity.ExcelTaskJpaEntity;

/**
 * Excel 長任務 Spring Data JPA 儲存庫 (Excel Task JPA Repository)
 * <p>
 * 遵循 Clean Architecture 基礎設施層規範 (AGENTS.md 規則 28，位於 {@code persistence/task/repository/})。
 * 提供針對 {@link ExcelTaskJpaEntity} 之持久化與 CRUD 查詢能力。
 */
@Repository
public interface ExcelTaskJpaRepository extends JpaRepository<ExcelTaskJpaEntity, String> {
}

