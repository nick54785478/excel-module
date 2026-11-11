package com.example.demo.presentation.resource.in;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * 建立 Excel 範本請求資源載體 (Request Resource)。
 * <p>
 * 遵循 AGENTS.md 規則 3-1，請求物件以 {@code V + N + Resource} 命名。
 * 純資料載體 (Pure Data Carrier)，嚴禁在內部實作 toCommand 轉換。
 * </p>
 *
 * @param templateKey        範本唯一識別鍵 (例如 "PROCUREMENT_COMPARISON")
 * @param templateName       範本顯示名稱 (例如 "採購詢價比價單")
 * @param category           範本業務分類 (例如 "PROCUREMENT")
 * @param description        範本設計與用途描述
 * @param requiredVariables  必備變數清單
 * @param storageType        檔案儲存類型 (預設 "CLASSPATH", 亦支援 "FILE_SYSTEM", "S3")
 * @param filePath           實體檔案路徑
 * @param defaultMappingJson 預設動態欄位映射 JSON
 */
@Schema(description = "建立 Excel 範本請求資源載體")
public record CreateExcelTemplateResource(

        @NotBlank(message = "範本識別鍵 (templateKey) 不得為空")
        @Schema(description = "範本唯一識別鍵", example = "EQUIPMENT_MAINTENANCE")
        String templateKey,

        @NotBlank(message = "範本中文名稱 (templateName) 不得為空")
        @Schema(description = "範本中文名稱", example = "設備定檢維護月報表")
        String templateName,

        @NotBlank(message = "業務分類 (category) 不得為空")
        @Schema(description = "業務分類代碼", example = "MAINTENANCE")
        String category,

        @Schema(description = "範本用途與設計說明", example = "包含設備點檢紀錄、故障工時分析與備件耗損自適應公式")
        String description,

        @Schema(description = "JXLS 必備上下文變數清單", example = "[\"equipments\", \"department\", \"month\"]")
        List<String> requiredVariables,

        @Schema(description = "實體範本儲存媒介類型", example = "CLASSPATH", defaultValue = "CLASSPATH")
        String storageType,

        @Schema(description = "範本實體資源儲存路徑", example = "templates/excel/equipment_maintenance.xlsx")
        String filePath,

        @Schema(description = "預設動態欄位映射定義 JSON 規格")
        String defaultMappingJson
) {}

