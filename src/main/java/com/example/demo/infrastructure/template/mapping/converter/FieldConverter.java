package com.example.demo.infrastructure.template.mapping.converter;

/**
 * 欄位格式化轉換策略 SPI 介面。
 * <p>
 * 遵循策略模式 (Strategy Pattern)，供基礎設施映射引擎動態路由不同資料型別之格式化邏輯。
 * </p>
 */
public interface FieldConverter {

    /**
     * 取得此轉換器專屬之識別鍵代碼。
     *
     * @return 轉換器代碼 (例如 "CURRENCY", "DATE", "ENUM_DICT", "MASKING")
     */
    String getConverterKey();

    /**
     * 執行欄位值轉換。
     *
     * @param sourceValue 原始欄位值
     * @param formatPattern 格式化樣式參數 (可為格式化字串、字典群組碼等，可為 null)
     * @return 轉換後之值 (字串、格式化數值或遮罩結果)
     */
    Object convert(Object sourceValue, String formatPattern);
}

