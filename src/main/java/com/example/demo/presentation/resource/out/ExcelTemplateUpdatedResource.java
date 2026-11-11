package com.example.demo.presentation.resource.out;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * 範本更新回應資源載體 (Response Resource)。
 * <p>
 * 遵循 AGENTS.md 規則 3-2，回應物件以 {@code N + Ved + Resource} 命名。
 * 純資料載體 (Pure Data Carrier)。
 * </p>
 *
 * @param templateKey       範本唯一識別鍵
 * @param templateName      範本中文名稱
 * @param category          範本業務分類
 * @param description       範本用途說明
 * @param requiredVariables 必備變數清單
 * @param updatedAt         更新時間戳記
 */
@Schema(description = "範本更新成功回應資源載體")
public record ExcelTemplateUpdatedResource(

        @Schema(description = "範本唯一識別鍵", example = "EQUIPMENT_MAINTENANCE")
        String templateKey,

        @Schema(description = "範本顯示名稱", example = "設備定檢維護月報表")
        String templateName,

        @Schema(description = "範本業務分類", example = "MAINTENANCE")
        String category,

        @Schema(description = "範本業務用途說明")
        String description,

        @Schema(description = "必備上下文變數清單")
        List<String> requiredVariables,

        @Schema(description = "最後更新時間戳記")
        Instant updatedAt
) {}

