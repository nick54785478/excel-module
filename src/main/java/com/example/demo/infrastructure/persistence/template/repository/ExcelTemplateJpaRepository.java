package com.example.demo.infrastructure.persistence.template.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.infrastructure.persistence.template.entity.ExcelTemplateJpaEntity;

/**
 * Excel 範本 Spring Data JPA 儲存庫介面 (Excel Template Spring Data JPA Repository)
 * <p>
 * 隸屬於基礎設施層持久化模組 (Infrastructure Layer - Persistence)，
 * 遵循 AGENTS.md 規則 2 與 Clean Architecture 隔離規範 (位於 {@code persistence/template/repository/})。
 * 提供關聯式資料表 {@code t_excel_template} 之 CRUD 與客製化查詢衍生方法。
 * </p>
 */
@Repository
public interface ExcelTemplateJpaRepository extends JpaRepository<ExcelTemplateJpaEntity, String> {

    /**
     * 依據範本唯一識別鍵查詢範本實體 (包含啟用與停用狀態)。
     *
     * @param templateKey 範本唯一識別鍵 (例如 "PROCUREMENT_COMPARISON")
     * @return 範本 JPA 實體 Optional 包裝
     */
    Optional<ExcelTemplateJpaEntity> findByTemplateKey(String templateKey);

    /**
     * 依據範本唯一識別鍵查詢有效啟用中之範本實體。
     *
     * @param templateKey 範本唯一識別鍵 (例如 "MATERIAL_MASTER")
     * @return 啟用狀態之範本 JPA 實體 Optional 包裝
     */
    Optional<ExcelTemplateJpaEntity> findByTemplateKeyAndActiveTrue(String templateKey);

    /**
     * 依據範本識別鍵與租戶識別碼查詢啟用中之範本實體 (多租戶隔離)。
     *
     * @param templateKey 範本唯一識別鍵
     * @param tenantId    租戶識別碼
     * @return 符合租戶之範本 JPA 實體 Optional 包裝
     */
    Optional<ExcelTemplateJpaEntity> findByTemplateKeyAndTenantIdAndActiveTrue(String templateKey, String tenantId);

    /**
     * 查詢全系統所有啟用中之 Excel 範本清單。
     *
     * @return 啟用中範本 JPA 實體清單
     */
    List<ExcelTemplateJpaEntity> findByActiveTrue();

    /**
     * 依據業務分類查詢啟用中之 Excel 範本清單。
     *
     * @param category 業務分類 (例如 "PROCUREMENT", "BOM", "MATERIAL")
     * @return 符合業務分類之範本 JPA 實體清單
     */
    List<ExcelTemplateJpaEntity> findByCategoryAndActiveTrue(String category);

    /**
     * 依據租戶識別碼查詢該租戶所有啟用中之 Excel 範本清單。
     *
     * @param tenantId 租戶識別碼 (例如 "DEFAULT")
     * @return 符合租戶之範本 JPA 實體清單
     */
    List<ExcelTemplateJpaEntity> findByTenantIdAndActiveTrue(String tenantId);

    /**
     * 檢查指定識別鍵之範本是否存在 (無論啟用狀態)。
     *
     * @param templateKey 範本唯一識別鍵
     * @return 若存在回傳 {@code true}，否則回傳 {@code false}
     */
    boolean existsByTemplateKey(String templateKey);

    /**
     * 檢查指定識別鍵之範本是否存在且為啟用中狀態。
     *
     * @param templateKey 範本唯一識別鍵
     * @return 若存在且啟用回傳 {@code true}，否則回傳 {@code false}
     */
    boolean existsByTemplateKeyAndActiveTrue(String templateKey);
}

