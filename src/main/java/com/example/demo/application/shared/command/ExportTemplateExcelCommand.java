package com.example.demo.application.shared.command;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * 範本 Excel 匯出命令物件 (Export Template Excel Command)。
 * <p>
 * 遵循 AGENTS.md 規則 3-4，命令以 {@code V + N + Command} 命名。
 * 以 Java 21 Record 實作，確保不可變性與純資料載體特性。
 * 負責封裝調用範本匯出時所需的範本識別鍵、資料上下文模型與建議輸出檔名。
 * </p>
 *
 * @param templateKey    範本唯一識別鍵 (必填，例如 "PROCUREMENT_COMPARISON")
 * @param dataModel      注入範本之業務資料上下文 (必填，鍵值對字典或模型集合)
 * @param outputFileName 建議之輸出檔案名稱 (可為 null，預設自動結合 timestamp 命名)
 */
public record ExportTemplateExcelCommand(
    String templateKey,
    Map<String, Object> dataModel,
    String outputFileName
) {
    /**
     * 規範化建構子 (Compact Constructor)，執行參數防呆檢核與預設值賦予。
     */
    public ExportTemplateExcelCommand {
        Objects.requireNonNull(templateKey, "範本識別鍵 (templateKey) 不得為空");
        if (templateKey.isBlank()) {
            throw new IllegalArgumentException("範本識別鍵 (templateKey) 不得為空白字串");
        }
        Objects.requireNonNull(dataModel, "資料模型 (dataModel) 不得為空");
        dataModel = Collections.unmodifiableMap(dataModel);
        if (outputFileName == null || outputFileName.isBlank()) {
            outputFileName = templateKey.toLowerCase() + "_" + System.currentTimeMillis() + ".xlsx";
        }
    }

    /**
     * 便捷工廠方法：建立預設檔名之範本匯出命令。
     *
     * @param templateKey 範本唯一識別鍵
     * @param dataModel   業務資料上下文
     * @return 範本匯出命令實例
     */
    public static ExportTemplateExcelCommand of(String templateKey, Map<String, Object> dataModel) {
        return new ExportTemplateExcelCommand(templateKey, dataModel, null);
    }

    /**
     * 便捷工廠方法：建立指定檔名之範本匯出命令。
     *
     * @param templateKey    範本唯一識別鍵
     * @param dataModel      業務資料上下文
     * @param outputFileName 自訂輸出檔案名稱
     * @return 範本匯出命令實例
     */
    public static ExportTemplateExcelCommand of(String templateKey, Map<String, Object> dataModel, String outputFileName) {
        return new ExportTemplateExcelCommand(templateKey, dataModel, outputFileName);
    }
}

