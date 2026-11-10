package com.example.demo.infrastructure.template.mapping.converter;

import com.example.demo.domain.material.aggregate.vo.MaterialCategory;
import com.example.demo.domain.material.aggregate.vo.MaterialStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 格式轉換策略庫 (FieldConverter SPI) 單元測試。
 * <p>
 * 遵循 AGENTS.md 規則 5 測試規範，具備 Gherkin 語法註解。
 * 驗證貨幣、日期時間、列舉字典與機密遮罩轉換之正確性。
 * </p>
 */
@DisplayName("基礎設施層: 格式轉換策略庫 (FieldConverter SPI) 測試")
public class FieldConverterTest {

    private final CurrencyFieldConverter currencyConverter = new CurrencyFieldConverter();
    private final DateFormatFieldConverter dateConverter = new DateFormatFieldConverter();
    private final EnumDictionaryConverter enumConverter = new EnumDictionaryConverter();
    private final MaskingFieldConverter maskingConverter = new MaskingFieldConverter();

    /**
     * Feature: 貨幣與數值格式化轉換
     * Scenario: 驗證不同數值型別套用千分位與自訂小數樣式
     *   Given 輸入各類數值 (BigDecimal, Double, String)
     *   When 呼叫 CurrencyFieldConverter.convert
     *   Then 應正確轉換為包含千分位與指定小數之格式字串
     */
    @Test
    @DisplayName("Gherkin: 貨幣轉換器數值千分位與小數格式化測試")
    void shouldConvertCurrencyCorrectly() {
        // Given
        BigDecimal value1 = new BigDecimal("1234567.89");
        Double value2 = 987654.3;

        // When
        Object result1 = currencyConverter.convert(value1, "#,##0.00");
        Object result2 = currencyConverter.convert(value2, "$#,##0.00");
        Object resultNull = currencyConverter.convert(null, null);

        // Then
        assertThat(result1).isEqualTo("1,234,567.89");
        assertThat(result2).isEqualTo("$987,654.30");
        assertThat(resultNull).isNull();
    }

    /**
     * Feature: 日期時間格式化轉換
     * Scenario: 驗證 LocalDate 與 LocalDateTime 轉為自訂字串格式
     *   Given 輸入 LocalDate 與 LocalDateTime
     *   When 呼叫 DateFormatFieldConverter.convert
     *   Then 應轉換為指定 pattern 之時間字串
     */
    @Test
    @DisplayName("Gherkin: 日期時間轉換器格式化測試")
    void shouldConvertDateCorrectly() {
        // Given
        LocalDate date = LocalDate.of(2026, 11, 10);
        LocalDateTime dateTime = LocalDateTime.of(2026, 11, 10, 14, 30, 45);

        // When
        Object dateStr = dateConverter.convert(date, "yyyy/MM/dd");
        Object dateTimeStr = dateConverter.convert(dateTime, "yyyy-MM-dd HH:mm:ss");

        // Then
        assertThat(dateStr).isEqualTo("2026/11/10");
        assertThat(dateTimeStr).isEqualTo("2026-11-10 14:30:45");
    }

    /**
     * Feature: 業務列舉轉多國語系文字
     * Scenario: 驗證 MaterialCategory 與自訂字典轉譯
     *   Given 領域列舉值 MaterialCategory.RAW_MATERIAL 與 MaterialStatus.ACTIVE
     *   When 呼叫 EnumDictionaryConverter.convert
     *   Then 應正確轉譯為中文業務標籤
     */
    @Test
    @DisplayName("Gherkin: 業務列舉字典轉譯測試")
    void shouldConvertEnumDictionaryCorrectly() {
        // Given
        MaterialCategory category = MaterialCategory.RAW_MATERIAL;
        MaterialStatus status = MaterialStatus.ACTIVE;

        // When
        Object catText = enumConverter.convert(category, "MATERIAL_CATEGORY");
        Object statusText = enumConverter.convert(status, "MATERIAL_STATUS");

        // 測試動態註冊自訂字典
        enumConverter.registerDictionary("CUSTOM_PRIORITY", Map.of("URGENT", "特急件", "NORMAL", "一般"));
        Object customText = enumConverter.convert("URGENT", "CUSTOM_PRIORITY");

        // Then
        assertThat(catText).isEqualTo("原物料");
        assertThat(statusText).isEqualTo("啟用");
        assertThat(customText).isEqualTo("特急件");
    }

    /**
     * Feature: 敏感資料去識別化遮罩
     * Scenario: 驗證全文遮罩與中間遮罩
     *   Given 機密字串
     *   When 呼叫 MaskingFieldConverter.convert
     *   Then 應套用遮罩字元
     */
    @Test
    @DisplayName("Gherkin: 機密資料遮罩測試")
    void shouldMaskSensitiveDataCorrectly() {
        // Given
        String cost = "18500.00";
        String code = "MAT-RM-001";

        // When
        Object fullMask = maskingConverter.convert(cost, "FULL");
        Object middleMask = maskingConverter.convert(code, "MIDDLE");

        // Then
        assertThat(fullMask).isEqualTo("***");
        assertThat(middleMask.toString()).contains("*");
    }
}

