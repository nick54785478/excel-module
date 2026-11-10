package com.example.demo.application.shared.mapping;

/**
 * 預設欄位轉換器類型識別常數。
 * <p>
 * 提供給 {@link FieldMappingRule#converterKey()} 使用之標準鍵值定義。
 * </p>
 */
public final class FieldConverterType {

    /**
     * 貨幣數值轉換器：支援千分位、小數精確度與貨幣符號格式化。
     */
    public static final String CURRENCY = "CURRENCY";

    /**
     * 日期時間轉換器：支援 LocalDate, LocalDateTime, Instant, Date 等格式化。
     */
    public static final String DATE = "DATE";

    /**
     * 業務列舉字典轉換器：支援將 Enum 轉換為多國語言或業務中文名稱。
     */
    public static final String ENUM_DICT = "ENUM_DICT";

    /**
     * 敏感資料遮罩轉換器：支援成本、身分證、電話、機密字串去識別化。
     */
    public static final String MASKING = "MASKING";

    private FieldConverterType() {
        // 私有建構函式防止實例化
    }
}

