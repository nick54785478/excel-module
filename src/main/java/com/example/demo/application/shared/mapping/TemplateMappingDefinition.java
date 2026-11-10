package com.example.demo.application.shared.mapping;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * JXLS 範本欄位動態映射整體規格定義 (Java 21 Record)。
 * <p>
 * 遵循 Clean Architecture 規格，封裝單一範本之欄位映射規則集合、
 * 迭代集合識別名與全域報表靜態元數據。
 * </p>
 *
 * @param templateKey 範本唯一識別鍵 (例如 "material_master")
 * @param rootCollectionVar JXLS {@code jx:each} 迭代之集合變數名稱 (例如 "materials")
 * @param itemAlias 迭代集合內單一元素之別名 (例如 "m" 或 "item")
 * @param rules 欄位動態映射規則清單
 * @param globalMeta 全域靜態元數據 (例如 companyName, exportDate 等)
 */
public record TemplateMappingDefinition(
    String templateKey,
    String rootCollectionVar,
    String itemAlias,
    List<FieldMappingRule> rules,
    Map<String, Object> globalMeta
) {

    /**
     * 規範建構函式：防禦性拷貝並確保集合不可變性。
     */
    public TemplateMappingDefinition {
        if (templateKey == null || templateKey.isBlank()) {
            templateKey = "DEFAULT_TEMPLATE";
        }
        Objects.requireNonNull(rootCollectionVar, "rootCollectionVar 不得為空");
        itemAlias = (itemAlias != null && !itemAlias.isBlank()) ? itemAlias : "item";
        rules = (rules != null) ? List.copyOf(rules) : List.of();
        globalMeta = (globalMeta != null) ? Map.copyOf(globalMeta) : Map.of();
    }

    /**
     * 拷貝並替換範本識別鍵。
     *
     * @param newKey 新的範本識別鍵
     * @return 新的規格實例
     */
    public TemplateMappingDefinition withTemplateKey(String newKey) {
        return new TemplateMappingDefinition(newKey, this.rootCollectionVar, this.itemAlias, this.rules, this.globalMeta);
    }

    /**
     * 取得流暢建構器 (Fluent Builder)。
     *
     * @param templateKey 範本識別鍵
     * @return 建構器實例
     */
    public static TemplateMappingDefinitionBuilder builder(String templateKey) {
        return new TemplateMappingDefinitionBuilder(templateKey);
    }
}

