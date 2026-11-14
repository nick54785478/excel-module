package com.example.demo.infrastructure.persistence.template.entity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import com.example.demo.application.shared.command.CreateExcelTemplateCommand;
import com.example.demo.application.shared.command.UpdateExcelTemplateCommand;
import com.example.demo.application.shared.dto.ExcelTemplateGottenResult;

/**
 * Excel 範本持久化實體 (Excel Template JPA Entity)
 * <p>
 * 隸屬於基礎設施層 (Infrastructure Layer - Persistence)，映射資料庫資料表 {@code t_excel_template}。
 * 遵循 AGENTS.md 規則 2 與 Clean Architecture 隔離規範，專責管理系統中已註冊之 Excel 範本中繼資訊、
 * 實體檔案儲存媒介 (Classpath、MinIO、S3 等)、必備資料模型變數清單與預設動態欄位映射規格 (JSON)。
 * </p>
 * <p>
 * <b>【架構邊界保護】</b>
 * 本實體嚴禁直接傳遞至 Application Inbound Port 或 Controller 表現層，
 * 須透過內部轉換方法 {@link #toGottenResult()} 封裝為純資料載體 {@link ExcelTemplateGottenResult} 傳遞。
 * </p>
 */
@Entity
@Table(name = "t_excel_template")
public class ExcelTemplateJpaEntity {

    /**
     * 範本持久化唯一主鍵 (Surrogate Primary Key，支援標準 UUID 或業務前綴鍵值)。
     */
    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    /**
     * 範本唯一識別鍵 (Natural/Business Key，例如 "MATERIAL_MASTER", "PROCUREMENT_COMPARISON")。
     */
    @Column(name = "template_key", length = 64, nullable = false, unique = true)
    private String templateKey;

    /**
     * 範本中文顯示名稱 (例如 "採購詢價比價單", "物料主檔清單")。
     */
    @Column(name = "template_name", length = 128, nullable = false)
    private String templateName;

    /**
     * 範本業務分類 (例如 "PROCUREMENT", "MATERIAL", "BOM", "FINANCE")。
     */
    @Column(name = "category", length = 64, nullable = false)
    private String category;

    /**
     * 範本業務用途與報表設計說明。
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * JXLS 報表渲染必備變數清單 (以逗號分隔，例如 "inquiryNo,inquiryDate,quotationItems")。
     */
    @Column(name = "required_variables", length = 1000)
    private String requiredVariables;

    /**
     * 實體範本檔案儲存媒介類型 (預設 "CLASSPATH"，亦支援 "FILE_SYSTEM", "S3", "MINIO")。
     */
    @Column(name = "storage_type", length = 32, nullable = false)
    private String storageType = "CLASSPATH";

    /**
     * 範本實體資源儲存路徑或物件鍵值 (例如 "templates/excel/procurement_comparison.xlsx")。
     */
    @Column(name = "file_path", length = 512, nullable = false)
    private String filePath;

    /**
     * 預設動態欄位映射定義 JSON 格式規格 (對應 TemplateMappingDefinition)。
     */
    @Column(name = "default_mapping_json", columnDefinition = "TEXT")
    private String defaultMappingJson;

    /**
     * 多租戶隔離識別碼 (預設為 "DEFAULT")。
     */
    @Column(name = "tenant_id", length = 64, nullable = false)
    private String tenantId = "DEFAULT";

    /**
     * 範本結構定義版本號 (預設 1，用於樂觀鎖與範本結構演進追蹤)。
     */
    @Column(name = "version", nullable = false)
    private Integer version = 1;

    /**
     * 範本啟用狀態 (true: 啟用中可供匯出, false: 停用下架)。
     */
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    /**
     * 範本初始建立時間。
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 範本最後更新時間。
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * JPA 規範要求之無參建構子。
     */
    public ExcelTemplateJpaEntity() {
    }

    /**
     * 全參建構子。
     *
     * @param id                 範本唯一主鍵 UUID
     * @param templateKey        範本唯一識別鍵
     * @param templateName       範本中文名稱
     * @param category           範本業務分類
     * @param description        範本描述說明
     * @param requiredVariables  必備變數清單 (逗號分隔)
     * @param storageType        儲存媒介類型
     * @param filePath           範本檔案路徑
     * @param defaultMappingJson 預設動態映射 JSON
     * @param tenantId           多租戶識別碼
     * @param version            版本號
     * @param active             啟用狀態
     * @param createdAt          建立時間
     * @param updatedAt          更新時間
     */
    public ExcelTemplateJpaEntity(
            String id,
            String templateKey,
            String templateName,
            String category,
            String description,
            String requiredVariables,
            String storageType,
            String filePath,
            String defaultMappingJson,
            String tenantId,
            Integer version,
            boolean active,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.templateKey = templateKey;
        this.templateName = templateName;
        this.category = category;
        this.description = description;
        this.requiredVariables = requiredVariables;
        this.storageType = storageType;
        this.filePath = filePath;
        this.defaultMappingJson = defaultMappingJson;
        this.tenantId = tenantId;
        this.version = version;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 實體持久化前之自動審計與預設值賦值生命週期鉤子。
     */
    @PrePersist
    protected void onCreate() {
        if (this.id == null || this.id.isBlank()) {
            this.id = UUID.randomUUID().toString();
        }
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.tenantId == null || this.tenantId.isBlank()) {
            this.tenantId = "DEFAULT";
        }
        if (this.storageType == null || this.storageType.isBlank()) {
            this.storageType = "CLASSPATH";
        }
        if (this.version == null) {
            this.version = 1;
        }
    }

    /**
     * 實體更新前之審計時間戳記更新鉤子。
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 取得必備變數清單字串解析後之 List 集合。
     *
     * @return 變數名稱不可變清單
     */
    public List<String> getRequiredVariablesList() {
        if (this.requiredVariables == null || this.requiredVariables.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(this.requiredVariables.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /**
     * 設定必備變數清單，內部自動序列化為逗號分隔字串。
     *
     * @param variables 變數清單
     */
    public void setRequiredVariablesList(List<String> variables) {
        if (variables == null || variables.isEmpty()) {
            this.requiredVariables = "";
        } else {
            this.requiredVariables = String.join(",", variables);
        }
    }

    /**
     * 將 JPA 持久化實體轉換為應用層查詢結果 DTO (Pure Data Carrier)。
     *
     * @return 範本中繼資訊查詢結果 DTO
     */
    public ExcelTemplateGottenResult toGottenResult() {
        Instant lastModified = this.updatedAt != null
                ? this.updatedAt.atZone(ZoneId.systemDefault()).toInstant()
                : Instant.now();
        return new ExcelTemplateGottenResult(
                this.templateKey,
                this.templateName,
                this.category,
                this.description,
                getRequiredVariablesList(),
                lastModified
        );
    }

    /**
     * 從建立範本命令建構 Excel 範本持久化實體 (充血模型工廠方法)。
     * <p>
     * 封裝範本實體的初始狀態與預設屬性指派邏輯，將範本鍵值與分類統一正規化為大寫。
     *
     * @param command 建立 Excel 範本命令載體，不得為 null
     * @return 初始化完成之 Excel 範本持久化實體
     */
    public static ExcelTemplateJpaEntity create(CreateExcelTemplateCommand command) {
        Objects.requireNonNull(command, "CreateExcelTemplateCommand 不得為空");
        String normalizedKey = command.templateKey() != null ? command.templateKey().toUpperCase(Locale.ROOT) : null;
        String normalizedCategory = command.category() != null ? command.category().toUpperCase(Locale.ROOT) : null;

        ExcelTemplateJpaEntity entity = new ExcelTemplateJpaEntity();
        entity.setTemplateKey(normalizedKey);
        entity.setTemplateName(command.templateName());
        entity.setCategory(normalizedCategory);
        entity.setDescription(command.description());
        entity.setRequiredVariablesList(command.requiredVariables());
        entity.setStorageType(command.storageType());
        entity.setFilePath(command.filePath());
        entity.setDefaultMappingJson(command.defaultMappingJson());
        entity.setActive(true);
        return entity;
    }

    /**
     * 依據更新命令更新範本屬性 (充血模型領域行為方法)。
     * <p>
     * 封裝範本內部欄位的選擇性更新邏輯，保持實體狀態的內聚性。
     *
     * @param command 更新 Excel 範本命令載體，不得為 null
     */
    public void updateFrom(UpdateExcelTemplateCommand command) {
        Objects.requireNonNull(command, "UpdateExcelTemplateCommand 不得為空");
        if (command.templateName() != null && !command.templateName().isBlank()) {
            this.templateName = command.templateName();
        }
        if (command.category() != null && !command.category().isBlank()) {
            this.category = command.category().toUpperCase(Locale.ROOT);
        }
        if (command.description() != null) {
            this.description = command.description();
        }
        if (command.requiredVariables() != null) {
            setRequiredVariablesList(command.requiredVariables());
        }
        if (command.storageType() != null && !command.storageType().isBlank()) {
            this.storageType = command.storageType();
        }
        if (command.filePath() != null && !command.filePath().isBlank()) {
            this.filePath = command.filePath();
        }
        if (command.defaultMappingJson() != null) {
            this.defaultMappingJson = command.defaultMappingJson();
        }
        if (command.active() != null) {
            this.active = command.active();
        }
    }

    /**
     * 從應用層範本中繼資訊 DTO 建立 JPA 持久化實體工廠方法。
     *
     * @param gottenResult 應用層範本查詢結果 DTO
     * @param storageType  儲存媒介類型 (若為 null 則預設為 "CLASSPATH")
     * @param filePath     實體檔案路徑 (若為 null 則自動推導)
     * @return JPA 持久化實體
     */
    public static ExcelTemplateJpaEntity fromGottenResult(
            ExcelTemplateGottenResult gottenResult,
            String storageType,
            String filePath
    ) {
        Objects.requireNonNull(gottenResult, "gottenResult 不得為空");
        ExcelTemplateJpaEntity entity = new ExcelTemplateJpaEntity();
        entity.setTemplateKey(gottenResult.templateKey());
        entity.setTemplateName(gottenResult.templateName());
        entity.setCategory(gottenResult.category());
        entity.setDescription(gottenResult.description());
        entity.setRequiredVariablesList(gottenResult.requiredVariables());
        entity.setStorageType(storageType != null ? storageType : "CLASSPATH");
        entity.setFilePath(filePath != null ? filePath : "templates/excel/" + gottenResult.templateKey().toLowerCase() + ".xlsx");
        entity.setActive(true);
        return entity;
    }

    // =========================================================================
    // Getters & Setters
    // =========================================================================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTemplateKey() {
        return templateKey;
    }

    public void setTemplateKey(String templateKey) {
        this.templateKey = templateKey;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRequiredVariables() {
        return requiredVariables;
    }

    public void setRequiredVariables(String requiredVariables) {
        this.requiredVariables = requiredVariables;
    }

    public String getStorageType() {
        return storageType;
    }

    public void setStorageType(String storageType) {
        this.storageType = storageType;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getDefaultMappingJson() {
        return defaultMappingJson;
    }

    public void setDefaultMappingJson(String defaultMappingJson) {
        this.defaultMappingJson = defaultMappingJson;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExcelTemplateJpaEntity that = (ExcelTemplateJpaEntity) o;
        return Objects.equals(id, that.id) || Objects.equals(templateKey, that.templateKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, templateKey);
    }

    @Override
    public String toString() {
        return "ExcelTemplateJpaEntity{" +
                "id='" + id + '\'' +
                ", templateKey='" + templateKey + '\'' +
                ", templateName='" + templateName + '\'' +
                ", category='" + category + '\'' +
                ", storageType='" + storageType + '\'' +
                ", filePath='" + filePath + '\'' +
                ", tenantId='" + tenantId + '\'' +
                ", version=" + version +
                ", active=" + active +
                '}';
    }
}

