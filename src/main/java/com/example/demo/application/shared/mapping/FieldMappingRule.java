package com.example.demo.application.shared.mapping;

import java.util.Objects;

/**
 * 單一欄位動態映射規則 (Java 21 Record)。
 * <p>
 * 遵循 Clean Architecture 規格，作為不可變純資料載體。
 * 定義來源物件屬性路徑、目標 JXLS 範本變數鍵值、轉換器識別碼與格式化樣式。
 * </p>
 *
 * @param sourcePath 來源屬性路徑 (支援巢狀導航，例如 "code.value", "standardCost.amount"，可為 null 當使用 virtualExpression 時)
 * @param targetKey 目標範本接收變數鍵值 (例如 "materialCode", "cost")
 * @param converterKey 格式轉換器識別鍵 (如 {@link FieldConverterType#CURRENCY}，可為 null)
 * @param formatPattern 格式化樣式 (如 "#,##0.00", "yyyy-MM-dd"，可為 null)
 * @param defaultValue 來源值為 null 時之預設替換值 (可為 null)
 * @param virtualExpression 跨欄位動態求值表達式 (支援 SpEL，例如 "#{m.name} + ' (' + #{m.specification} + ')'"，可為 null)
 */
public record FieldMappingRule(
    String sourcePath,
    String targetKey,
    String converterKey,
    String formatPattern,
    Object defaultValue,
    String virtualExpression
) {

    /**
     * 規範建構函式：校驗必要屬性不變條件。
     */
    public FieldMappingRule {
        Objects.requireNonNull(targetKey, "targetKey 目標範本變數鍵值不得為空");
        if ((sourcePath == null || sourcePath.isBlank()) && (virtualExpression == null || virtualExpression.isBlank())) {
            throw new IllegalArgumentException("sourcePath 與 virtualExpression 至少必須提供一者");
        }
    }

    /**
     * 建立最簡易之直接屬性對應規則。
     *
     * @param sourcePath 來源屬性路徑
     * @param targetKey 目標變數鍵值
     * @return 映射規則實例
     */
    public static FieldMappingRule of(String sourcePath, String targetKey) {
        return new FieldMappingRule(sourcePath, targetKey, null, null, null, null);
    }

    /**
     * 建立帶轉換器與樣式之屬性對應規則。
     *
     * @param sourcePath 來源屬性路徑
     * @param targetKey 目標變數鍵值
     * @param converterKey 轉換器代碼
     * @param formatPattern 格式化樣式
     * @return 映射規則實例
     */
    public static FieldMappingRule of(String sourcePath, String targetKey, String converterKey, String formatPattern) {
        return new FieldMappingRule(sourcePath, targetKey, converterKey, formatPattern, null, null);
    }

    /**
     * 建立虛擬運算欄位規則。
     *
     * @param targetKey 目標變數鍵值
     * @param virtualExpression 虛擬表達式
     * @return 映射規則實例
     */
    public static FieldMappingRule virtual(String targetKey, String virtualExpression) {
        return new FieldMappingRule(null, targetKey, null, null, null, virtualExpression);
    }
}

