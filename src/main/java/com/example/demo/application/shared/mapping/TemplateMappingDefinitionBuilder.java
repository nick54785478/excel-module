package com.example.demo.application.shared.mapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * {@link TemplateMappingDefinition} 之流暢建構器 (Fluent Java DSL)。
 * <p>
 * 提供直觀、流暢的鏈式 API，供微服務內部或單元測試快速定義範本欄位對齊規則。
 * </p>
 */
public class TemplateMappingDefinitionBuilder {

    private final String templateKey;
    private String rootCollectionVar = "items";
    private String itemAlias = "item";
    private final List<FieldMappingRule> rules = new ArrayList<>();
    private final Map<String, Object> globalMeta = new HashMap<>();

    public TemplateMappingDefinitionBuilder(String templateKey) {
        this.templateKey = Objects.requireNonNull(templateKey, "templateKey 不得為空");
    }

    /**
     * 設定 JXLS 迭代集合識別名與項目別名。
     *
     * @param rootCollectionVar 集合變數名稱 (例如 "materials")
     * @param itemAlias 單一項目別名 (例如 "m")
     * @return this
     */
    public TemplateMappingDefinitionBuilder rootCollection(String rootCollectionVar, String itemAlias) {
        this.rootCollectionVar = Objects.requireNonNull(rootCollectionVar, "rootCollectionVar 不得為空");
        this.itemAlias = (itemAlias != null && !itemAlias.isBlank()) ? itemAlias : "item";
        return this;
    }

    /**
     * 新增簡易欄位映射規則。
     *
     * @param sourcePath 來源屬性路徑 (例如 "code.value")
     * @param targetKey 目標範本鍵值 (例如 "materialCode")
     * @return this
     */
    public TemplateMappingDefinitionBuilder mapField(String sourcePath, String targetKey) {
        rules.add(FieldMappingRule.of(sourcePath, targetKey));
        return this;
    }

    /**
     * 新增帶預設值之欄位映射規則。
     *
     * @param sourcePath 來源屬性路徑
     * @param targetKey 目標範本鍵值
     * @param defaultValue 預設替換值
     * @return this
     */
    public TemplateMappingDefinitionBuilder mapFieldWithDefault(String sourcePath, String targetKey, Object defaultValue) {
        rules.add(new FieldMappingRule(sourcePath, targetKey, null, null, defaultValue, null));
        return this;
    }

    /**
     * 新增帶格式轉換器之欄位映射規則。
     *
     * @param sourcePath 來源屬性路徑
     * @param targetKey 目標範本鍵值
     * @param converterKey 轉換器類型代碼 (例如 "CURRENCY")
     * @param formatPattern 格式化樣式 (例如 "#,##0.00")
     * @return this
     */
    public TemplateMappingDefinitionBuilder mapField(String sourcePath, String targetKey, String converterKey, String formatPattern) {
        rules.add(FieldMappingRule.of(sourcePath, targetKey, converterKey, formatPattern));
        return this;
    }

    /**
     * 新增完整設定之欄位映射規則。
     *
     * @param sourcePath 來源屬性路徑
     * @param targetKey 目標範本鍵值
     * @param converterKey 轉換器代碼
     * @param formatPattern 格式化樣式
     * @param defaultValue 預設值
     * @return this
     */
    public TemplateMappingDefinitionBuilder mapField(String sourcePath, String targetKey, String converterKey, String formatPattern, Object defaultValue) {
        rules.add(new FieldMappingRule(sourcePath, targetKey, converterKey, formatPattern, defaultValue, null));
        return this;
    }

    /**
     * 新增動態虛擬運算欄位規則 (SpEL 表達式)。
     *
     * @param targetKey 目標範本鍵值
     * @param virtualExpression 虛擬運算式 (例如 "#{m.name} + ' (' + #{m.specification} + ')'")
     * @return this
     */
    public TemplateMappingDefinitionBuilder virtualField(String targetKey, String virtualExpression) {
        rules.add(FieldMappingRule.virtual(targetKey, virtualExpression));
        return this;
    }

    /**
     * 加入全域元數據鍵值對。
     *
     * @param key 元數據鍵名 (例如 "companyName")
     * @param value 元數據值 (例如 "神盾航太科技股份有限公司")
     * @return this
     */
    public TemplateMappingDefinitionBuilder addMeta(String key, Object value) {
        if (key != null) {
            this.globalMeta.put(key, value);
        }
        return this;
    }

    /**
     * 批次加入全域元數據。
     *
     * @param metaMap 元數據 Map
     * @return this
     */
    public TemplateMappingDefinitionBuilder addMetaAll(Map<String, Object> metaMap) {
        if (metaMap != null) {
            this.globalMeta.putAll(metaMap);
        }
        return this;
    }

    /**
     * 建構不可變之 {@link TemplateMappingDefinition} 實例。
     *
     * @return 映射規格定義
     */
    public TemplateMappingDefinition build() {
        return new TemplateMappingDefinition(templateKey, rootCollectionVar, itemAlias, rules, globalMeta);
    }
}

