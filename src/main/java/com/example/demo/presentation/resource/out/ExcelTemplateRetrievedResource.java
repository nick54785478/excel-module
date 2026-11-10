package com.example.demo.presentation.resource.out;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * 範本規格檢索回應資源載體 (Response Resource)。
 * <p>
 * 遵循 AGENTS.md 規則 3-2，回應物件以 {@code N + Ved + Resource} 命名。
 * 純資料載體 (Pure Data Carrier)，內部不得實作任何 fromResult 轉換邏輯。
 * 負責向客戶端呈現可用範本之鍵值、名稱、業務分類、必備變數與時間戳記。
 * </p>
 */
@Schema(description = "範本規格檢索回應資源載體")
public record ExcelTemplateRetrievedResource(

    @Schema(description = "範本唯一識別鍵", example = "PROCUREMENT_COMPARISON")
    String templateKey,

    @Schema(description = "範本顯示名稱", example = "採購詢價比價單")
    String templateName,

    @Schema(description = "範本業務分類", example = "PROCUREMENT")
    String category,

    @Schema(description = "範本業務用途說明", example = "包含多供應商比價明細、最佳報價推薦與自適應加總公式")
    String description,

    @Schema(description = "必備上下文變數清單", example = "[\"inquiryNo\", \"quotationItems\"]")
    List<String> requiredVariables,

    @Schema(description = "最後異動或註冊時間戳記")
    Instant lastModifiedAt
) {}

