package com.example.demo.infrastructure.template.customizer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 範本資料模型客製化內部工具類別 (Template Model Customizer Helper)。
 * <p>
 * 座落於 {@code infrastructure.template.customizer} 套件，
 * 提供集合鍵雙向對齊、巢狀清單展開、Map 鍵值轉換與欄位別名容錯等常用處理方法，
 * 供各專屬範本資料模型自定義策略實作重複利用。
 * </p>
 */
public final class TemplateModelCustomizerHelper {

    private TemplateModelCustomizerHelper() {
        // 私有建構函式避免實例化
    }

    /**
     * 對齊兩個集合鍵（若只有其中一個鍵存在，則將其內容賦予另一個鍵）。
     *
     * @param model 目標資料上下文 Map
     * @param keyA  集合鍵 A (如 "quotationItems")
     * @param keyB  集合鍵 B (如 "items")
     */
    public static void alignCollectionPair(Map<String, Object> model, String keyA, String keyB) {
        Object valA = model.get(keyA);
        Object valB = model.get(keyB);

        if (valA != null && valB == null) {
            model.put(keyB, valA);
        } else if (valB != null && valA == null) {
            model.put(keyA, valB);
        }
    }

    /**
     * 若目標鍵值為空，則依序從候選鍵值清單中取得第一個非空值補入。
     *
     * @param map           資料字典
     * @param targetKey     目標鍵值
     * @param candidateKeys 備用候選鍵值陣列
     */
    public static void aliasIfMissing(Map<String, Object> map, String targetKey, String... candidateKeys) {
        if (map.get(targetKey) == null) {
            for (String candidate : candidateKeys) {
                Object candidateValue = map.get(candidate);
                if (candidateValue != null) {
                    map.put(targetKey, candidateValue);
                    break;
                }
            }
        }
    }

    /**
     * 對目標模型內之特定清單項目進行深層複製與客製化擴充。
     *
     * @param model        資料模型字典
     * @param listKey      清單屬性鍵名 (如 "items" 或 "quotationItems")
     * @param itemConsumer 項目 Map 轉換處理函式
     */
    public static void transformItemList(Map<String, Object> model, String listKey, Consumer<Map<String, Object>> itemConsumer) {
        Object rawList = model.get(listKey);
        if (!(rawList instanceof List<?> list)) {
            return;
        }

        List<Object> normalizedList = new ArrayList<>(list.size());
        for (Object item : list) {
            if (item instanceof Map<?, ?> rawMap) {
                Map<String, Object> itemMap = new LinkedHashMap<>();
                for (Map.Entry<?, ?> e : rawMap.entrySet()) {
                    if (e.getKey() != null) {
                        itemMap.put(e.getKey().toString(), e.getValue());
                    }
                }
                itemConsumer.accept(itemMap);
                normalizedList.add(itemMap);
            } else {
                normalizedList.add(item);
            }
        }
        model.put(listKey, normalizedList);
    }
}
