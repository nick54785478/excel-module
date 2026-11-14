package com.example.demo.application.shared.template;

import com.example.demo.application.port.out.template.ExcelTemplateModelCustomizerFactoryPort;

import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Excel 範本資料模型自定義策略契約 (Template Model Customizer Strategy / SPI)。
 * <p>
 * <b>【架構定位與職責規範】</b><br>
 * 座落於 Clean Architecture 應用共享層 ({@code application/shared/template/})，作為純業務策略介面。<br>
 * 專責定義「如何針對特定 Excel 匯出範本對傳入之資料模型 (dataModel Context Map) 執行前處理、別名轉換、集合對齊或預設值注入」。<br>
 * 與基礎設施層的註冊表自定義器 ({@code ExcelTemplateRegistryCustomizer}) 相互區隔，
 * 配合 {@link ExcelTemplateModelCustomizerFactoryPort} 工廠輸出埠動態查找與執行。
 * </p>
 */
@FunctionalInterface
public interface ExcelTemplateModelCustomizer {

    /**
     * 保底無操作自定義策略 (No-Op Strategy)，直接回傳原始資料模型。
     */
    ExcelTemplateModelCustomizer NO_OP = dataModel -> (dataModel != null) ? dataModel : Map.of();

    /**
     * 取得此自定義策略所綁定之範本唯一識別鍵 (大小寫不敏感)。
     * <p>
     * 預設回傳 {@code "*"} 代表通用保底自定義器。
     * </p>
     *
     * @return 目標範本識別鍵字串 (例如 "PROCUREMENT_COMPARISON")
     */
    default String getTemplateKey() {
        return "*";
    }

    /**
     * 判斷此自定義策略是否支援指定的範本識別鍵。
     *
     * @param templateKey 欲匯出之範本唯一識別鍵
     * @return 若支援此範本則回傳 true，否則回傳 false
     */
    default boolean supports(String templateKey) {
        if (templateKey == null || getTemplateKey() == null) {
            return false;
        }
        return "*".equals(getTemplateKey()) || getTemplateKey().equalsIgnoreCase(templateKey.trim());
    }

    /**
     * 對資料模型執行自定義客製化或結構擴充。
     *
     * @param dataModel 原始資料上下文 (可為 null 或不可變 Map)
     * @return 客製化與補強後之資料上下文 Map (保證不為 null)
     */
    Map<String, Object> customize(Map<String, Object> dataModel);

    /**
     * 便捷靜態工廠方法：依據指定範本識別鍵與轉換函式快速建立自定義策略實例。
     *
     * @param targetTemplateKey 目標範本唯一識別鍵
     * @param customizerFn      模型轉換函式
     * @return 自定義策略實例
     */
    static ExcelTemplateModelCustomizer of(String targetTemplateKey, UnaryOperator<Map<String, Object>> customizerFn) {
        Objects.requireNonNull(targetTemplateKey, "targetTemplateKey 不得為空");
        Objects.requireNonNull(customizerFn, "customizerFn 不得為空");
        return new ExcelTemplateModelCustomizer() {
            @Override
            public String getTemplateKey() {
                return targetTemplateKey;
            }

            @Override
            public Map<String, Object> customize(Map<String, Object> dataModel) {
                return customizerFn.apply(dataModel);
            }
        };
    }
}
