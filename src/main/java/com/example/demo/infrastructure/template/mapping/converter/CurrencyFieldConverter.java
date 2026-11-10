package com.example.demo.infrastructure.template.mapping.converter;

import com.example.demo.application.shared.mapping.FieldConverterType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.DecimalFormat;

/**
 * 貨幣與數值格式化轉換策略。
 * <p>
 * 支援將 {@link Number}、{@link BigDecimal} 或數值字串格式化為具備千分位與指定小數位之格式。
 * </p>
 */
@Component
public class CurrencyFieldConverter implements FieldConverter {

    public static final String DEFAULT_PATTERN = "#,##0.00";

    @Override
    public String getConverterKey() {
        return FieldConverterType.CURRENCY;
    }

    @Override
    public Object convert(Object sourceValue, String formatPattern) {
        if (sourceValue == null) {
            return null;
        }

        BigDecimal number;
        if (sourceValue instanceof BigDecimal bd) {
            number = bd;
        } else if (sourceValue instanceof Number num) {
            number = new BigDecimal(num.toString());
        } else {
            try {
                number = new BigDecimal(sourceValue.toString().trim().replace(",", ""));
            } catch (Exception e) {
                return sourceValue;
            }
        }

        String pattern = (formatPattern != null && !formatPattern.isBlank()) ? formatPattern : DEFAULT_PATTERN;
        DecimalFormat df = new DecimalFormat(pattern);
        return df.format(number);
    }
}

