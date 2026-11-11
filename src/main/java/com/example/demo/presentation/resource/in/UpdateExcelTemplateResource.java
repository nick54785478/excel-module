package com.example.demo.presentation.resource.in;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 更新 Excel 範本請求資源載體 (Request Resource)。
 * <p>
 * 遵循 AGENTS.md 規則 3-1，請求物件以 {@code V + N + Resource} 命名。
 * 純資料載體 (Pure Data Carrier)，內部不含任何轉換邏輯。
 * </p>
 *
 * @param templateName       範本中文名稱
 * @param category           範本業務分類
 * @param description        範本描述說明
 * @param requiredVariables  必備變數清單
 * @param storageType        檔案儲存媒介類型
 * @param filePath           範本檔案路徑
 * @param defaultMappingJson 預設動態映射 JSON
 * @param active             啟用狀態
 */
@Schema(description = "更新 Excel 範本請求資源載體")
public record UpdateExcelTemplateResource(

        @Schema(description = "範本中文名稱", example = "採購詢價比價單 (修訂版)")
        String templateName,

        @Schema(description = "業務分類代碼", example = "PROCUREMENT")
        String category,

        @Schema(description = "範本設計與用途說明")
        String description,

        @Schema(description = "JXLS 必備上下文變數清單")
        List<String> requiredVariables,

        @Schema(description = "實體範本儲存媒介類型", example = "CLASSPATH")
        String storageType,

        @Schema(description = "範本實體資源儲存路徑")
        String filePath,

        @Schema(description = "預設動態欄位映射定義 JSON 規格")
        String defaultMappingJson,

        @Schema(description = "是否啟用該範本", example = "true")
        Boolean active
) {}
