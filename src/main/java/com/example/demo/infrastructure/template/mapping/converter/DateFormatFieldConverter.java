package com.example.demo.infrastructure.template.mapping.converter;

import com.example.demo.application.shared.mapping.FieldConverterType;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;

/**
 * 日期時間格式化轉換策略。
 * <p>
 * 支援將 {@link TemporalAccessor}、{@link Instant}、{@link Date} 依指定樣式格式化為字串。
 * </p>
 */
@Component
public class DateFormatFieldConverter implements FieldConverter {

    public static final String DEFAULT_DATE_PATTERN = "yyyy-MM-dd";
    public static final String DEFAULT_DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @Override
    public String getConverterKey() {
        return FieldConverterType.DATE;
    }

    @Override
    public Object convert(Object sourceValue, String formatPattern) {
        if (sourceValue == null) {
            return null;
        }

        String pattern = (formatPattern != null && !formatPattern.isBlank()) ? formatPattern : DEFAULT_DATE_PATTERN;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);

        if (sourceValue instanceof LocalDate ld) {
            return ld.format(formatter);
        } else if (sourceValue instanceof LocalDateTime ldt) {
            return ldt.format(formatter);
        } else if (sourceValue instanceof Instant instant) {
            return LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).format(formatter);
        } else if (sourceValue instanceof Date date) {
            return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()).format(formatter);
        } else if (sourceValue instanceof TemporalAccessor ta) {
            return formatter.format(ta);
        }

        return sourceValue.toString();
    }
}

