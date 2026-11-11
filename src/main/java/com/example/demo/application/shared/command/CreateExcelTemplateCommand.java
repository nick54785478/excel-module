package com.example.demo.application.shared.command;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 建立 Excel 範本命令載體 (Command)。
 * <p>
 * 遵循 AGENTS.md 規則 3-4，以 {@code V + N + Command} 命名。
 * 應用層純資料載體 (Java 21 Record)，傳入範本識別鍵、名稱、分類、描述與必備變數清單，
 * 嚴禁依賴任何 ORM 或基礎設施技術類別。
 * </p>
 *
 * @param templateKey        範本唯一識別鍵 (例如 "MATERIAL_MASTER", "PURCHASE_ORDER")
 * @param templateName       範本中文顯示名稱 (例如 "物料主檔清單")
 * @param category           範本業務分類 (例如 "MATERIAL", "BOM", "PROCUREMENT")
 * @param description        範本用途與報表設計說明
 * @param requiredVariables  JXLS 渲染必備變數清單
 * @param storageType        實體檔案儲存媒介類型 (預設 "CLASSPATH", 亦支援 "FILE_SYSTEM", "S3")
 * @param filePath           範本實體資源儲存路徑 (若為 null 則自動推導)
 * @param defaultMappingJson 預設動態欄位映射定義 JSON 格式字串
 */
public record CreateExcelTemplateCommand(
        String templateKey,
        String templateName,
        String category,
        String description,
        List<String> requiredVariables,
        String storageType,
        String filePath,
        String defaultMappingJson
) {
    public CreateExcelTemplateCommand {
        Objects.requireNonNull(templateKey, "templateKey 不得為空");
        Objects.requireNonNull(templateName, "templateName 不得為空");
        Objects.requireNonNull(category, "category 不得為空");
        requiredVariables = requiredVariables != null ? List.copyOf(requiredVariables) : Collections.emptyList();
        storageType = (storageType != null && !storageType.isBlank()) ? storageType : "CLASSPATH";
        filePath = (filePath != null && !filePath.isBlank()) ? filePath : "templates/excel/" + templateKey.toLowerCase() + ".xlsx";
    }
}
