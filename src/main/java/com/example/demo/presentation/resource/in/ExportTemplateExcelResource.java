package com.example.demo.presentation.resource.in;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

/**
 * 範本 Excel 匯出請求資源載體 (Request Resource)。
 * <p>
 * 遵循 AGENTS.md 規則 3-1，請求物件以 {@code V + N + Resource} 命名。
 * 純資料載體 (Pure Data Carrier)，內部不得實作任何 toCommand 轉換邏輯。
 * 負責接收客戶端傳入之動態資料模型與自訂輸出檔名。
 * </p>
 */
@Schema(description = "範本 Excel 匯出請求資源載體")
public record ExportTemplateExcelResource(

    @Schema(description = "注入範本之業務資料上下文 (支援任意鍵值對字典或巢狀清單)", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "資料模型 (dataModel) 不得為空")
    Map<String, Object> dataModel,

    @Schema(description = "自訂輸出檔案名稱 (選填，若未填則系統依範本鍵值自動命名)", example = "quotation_comparison.xlsx")
    String outputFileName
) {}

