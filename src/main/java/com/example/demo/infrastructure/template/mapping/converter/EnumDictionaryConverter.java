package com.example.demo.infrastructure.template.mapping.converter;

import com.example.demo.application.shared.mapping.FieldConverterType;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 業務列舉與多國語系字典轉譯策略。
 * <p>
 * 支援將系統標準業務列舉（例如 MaterialCategory, MaterialStatus, BomStatus）
 * 自動轉譯為繁體中文業務描述，並支援動態擴充自訂字典。
 * </p>
 */
@Component
public class EnumDictionaryConverter implements FieldConverter {

    private final Map<String, Map<String, String>> dictionaryCache = new ConcurrentHashMap<>();

    public EnumDictionaryConverter() {
        initDefaultDictionaries();
    }

    @Override
    public String getConverterKey() {
        return FieldConverterType.ENUM_DICT;
    }

    @Override
    public Object convert(Object sourceValue, String formatPattern) {
        if (sourceValue == null) {
            return null;
        }

        String enumName;
        if (sourceValue instanceof Enum<?> e) {
            enumName = e.name();
        } else {
            enumName = sourceValue.toString().trim();
        }

        if (formatPattern != null && !formatPattern.isBlank()) {
            Map<String, String> dict = dictionaryCache.get(formatPattern.toUpperCase());
            if (dict != null && dict.containsKey(enumName)) {
                return dict.get(enumName);
            }
        }

        // 全域通用備援比對
        for (Map<String, String> dict : dictionaryCache.values()) {
            if (dict.containsKey(enumName)) {
                return dict.get(enumName);
            }
        }

        // 檢查 Enum 是否本身帶有 getDisplayName() 方法
        if (sourceValue instanceof Enum<?> e) {
            try {
                java.lang.reflect.Method m = e.getClass().getMethod("getDisplayName");
                Object display = m.invoke(e);
                if (display != null) {
                    return display.toString();
                }
            } catch (Exception ignored) {
            }
        }

        return enumName;
    }

    /**
     * 註冊自訂業務字典映射表。
     *
     * @param dictionaryKey 字典識別碼 (例如 "MATERIAL_CATEGORY")
     * @param mapping 字典鍵值對
     */
    public void registerDictionary(String dictionaryKey, Map<String, String> mapping) {
        if (dictionaryKey != null && mapping != null) {
            dictionaryCache.put(dictionaryKey.toUpperCase(), new ConcurrentHashMap<>(mapping));
        }
    }

    private void initDefaultDictionaries() {
        // MaterialCategory
        Map<String, String> materialCategory = Map.of(
            "RAW_MATERIAL", "原物料",
            "SEMI_FINISHED", "半成品",
            "FINISHED_PRODUCT", "成品"
        );
        registerDictionary("MATERIAL_CATEGORY", materialCategory);

        // MaterialStatus
        Map<String, String> materialStatus = Map.of(
            "ACTIVE", "啟用",
            "INACTIVE", "停用",
            "OBSOLETE", "廢止"
        );
        registerDictionary("MATERIAL_STATUS", materialStatus);

        // BomStatus
        Map<String, String> bomStatus = Map.of(
            "DRAFT", "草稿",
            "SUBMITTED", "已送審",
            "APPROVED", "已核准",
            "OBSOLETE", "廢止"
        );
        registerDictionary("BOM_STATUS", bomStatus);
    }
}

