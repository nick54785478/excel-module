package com.example.demo.application.shared.command;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 更新 Excel 範本規格命令載體 (Command)。
 * <p>
 * 遵循 AGENTS.md 規則 3-4，以 {@code V + N + Command} 命名。
 * 應用層純資料載體 (Java 21 Record)，傳入欲更新之範本識別鍵、名稱、分類、描述、變數規格與啟用狀態。
 * </p>
 *
 * @param templateKey        欲更新之範本唯一識別鍵
 * @param templateName       範本中文顯示名稱
 * @param category           範本業務分類
 * @param description        範本用途與報表設計說明
 * @param requiredVariables  JXLS 渲染必備變數清單
 * @param storageType        實體檔案儲存媒介類型
 * @param filePath           範本實體資源儲存路徑
 * @param defaultMappingJson 預設動態欄位映射定義 JSON 格式字串
 * @param active             範本啟用狀態 (true: 啟用, false: 停用)
 */
public record UpdateExcelTemplateCommand(
        String templateKey,
        String templateName,
        String category,
        String description,
        List<String> requiredVariables,
        String storageType,
        String filePath,
        String defaultMappingJson,
        Boolean active
) {
    public UpdateExcelTemplateCommand {
        Objects.requireNonNull(templateKey, "templateKey 不得為空");
        requiredVariables = requiredVariables != null ? List.copyOf(requiredVariables) : Collections.emptyList();
    }
}

