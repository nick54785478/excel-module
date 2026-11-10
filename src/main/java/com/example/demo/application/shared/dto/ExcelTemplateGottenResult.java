package com.example.demo.application.shared.dto;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * 範本中繼資訊查詢結果 DTO (Excel Template Gotten Result)。
 * <p>
 * 遵循 AGENTS.md 規則 3-6，DTO 以 {@code N + Gotten + Result} 命名。
 * 作為純資料載體 (Pure Data Carrier)，不包含任何業務或組裝轉換邏輯。
 * 負責封裝範本識別鍵、名稱、分類、必備變數清單與最後異動時間。
 * </p>
 *
 * @param templateKey       範本唯一識別鍵 (例如 "PROCUREMENT_COMPARISON")
 * @param templateName      範本顯示名稱 (例如 "採購詢價比價單")
 * @param category          範本業務分類 (例如 "PROCUREMENT", "FINANCE", "BOM")
 * @param description       範本業務用途說明
 * @param requiredVariables 必備上下文變數清單 (例如 ["inquiryNo", "quotationItems"])
 * @param lastModifiedAt    最後修改或註冊時間戳記
 */
public record ExcelTemplateGottenResult(
    String templateKey,
    String templateName,
    String category,
    String description,
    List<String> requiredVariables,
    Instant lastModifiedAt
) {
    /**
     * 規範化建構子 (Compact Constructor)，保證集合不可變性。
     */
    public ExcelTemplateGottenResult {
        requiredVariables = (requiredVariables == null)
            ? Collections.emptyList()
            : Collections.unmodifiableList(requiredVariables);
        lastModifiedAt = (lastModifiedAt == null) ? Instant.now() : lastModifiedAt;
    }
}

