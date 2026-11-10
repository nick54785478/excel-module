package com.example.demo.application.shared.command;

import com.example.demo.application.shared.mapping.TemplateMappingDefinition;
import java.util.Objects;

/**
 * 具備自訂欄位映射之範本 Excel 匯出命令物件 (Export Template With Mapping Command)。
 * <p>
 * 遵循 AGENTS.md 規則 3-4，以 {@code V + N + Command} 命名。
 * 以 Java 21 Record 實作，確保不可變性與純資料載體特性。
 * 負責封裝調用動態欄位映射與範本渲染所需之範本識別鍵、來源資料、映射規則與建議輸出檔名。
 * </p>
 *
 * @param templateKey       範本唯一識別鍵 (必填，例如 "material_master")
 * @param sourceData        來源業務資料物件或集合 (選填，可為領域聚合根清單或自訂集合，若為 null 則由服務端預設載入)
 * @param mappingDefinition 範本欄位動態映射整體規格 (必填)
 * @param outputFileName    建議之輸出檔案名稱 (可為 null，預設自動結合 timestamp 命名)
 */
public record ExportTemplateWithMappingCommand(
    String templateKey,
    Object sourceData,
    TemplateMappingDefinition mappingDefinition,
    String outputFileName
) {
    /**
     * 規範化建構子 (Compact Constructor)，執行參數防呆檢核與預設值賦予。
     */
    public ExportTemplateWithMappingCommand {
        Objects.requireNonNull(templateKey, "範本識別鍵 (templateKey) 不得為空");
        if (templateKey.isBlank()) {
            throw new IllegalArgumentException("範本識別鍵 (templateKey) 不得為空白字串");
        }
        Objects.requireNonNull(mappingDefinition, "映射規格定義 (mappingDefinition) 不得為空");
        if (outputFileName == null || outputFileName.isBlank()) {
            outputFileName = templateKey.toLowerCase() + "_mapped_" + System.currentTimeMillis() + ".xlsx";
        }
    }

    /**
     * 便捷工廠方法。
     *
     * @param templateKey       範本識別鍵
     * @param sourceData        來源資料
     * @param mappingDefinition 映射規格定義
     * @return 命令實例
     */
    public static ExportTemplateWithMappingCommand of(
            String templateKey,
            Object sourceData,
            TemplateMappingDefinition mappingDefinition) {
        return new ExportTemplateWithMappingCommand(templateKey, sourceData, mappingDefinition, null);
    }
}

