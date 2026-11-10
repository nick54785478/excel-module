package com.example.demo.infrastructure.template.mapping.converter;

import com.example.demo.application.shared.mapping.FieldConverterType;
import org.springframework.stereotype.Component;

/**
 * 敏感資料去識別化遮罩轉換策略。
 * <p>
 * 支援機密資訊（如採購成本、機密物料碼、個人資訊）之自動動態遮罩處理。
 * </p>
 */
@Component
public class MaskingFieldConverter implements FieldConverter {

    @Override
    public String getConverterKey() {
        return FieldConverterType.MASKING;
    }

    @Override
    public Object convert(Object sourceValue, String formatPattern) {
        if (sourceValue == null) {
            return null;
        }

        String str = sourceValue.toString();
        if (str.isBlank()) {
            return str;
        }

        String pattern = (formatPattern != null) ? formatPattern.toUpperCase() : "FULL";

        return switch (pattern) {
            case "MIDDLE" -> maskMiddle(str);
            case "TAIL" -> maskTail(str);
            default -> "***"; // 預設全遮罩
        };
    }

    private String maskMiddle(String str) {
        int len = str.length();
        if (len <= 2) {
            return "**";
        }
        int start = len / 3;
        int end = len - start;
        return str.substring(0, start) + "*".repeat(end - start) + str.substring(end);
    }

    private String maskTail(String str) {
        int len = str.length();
        if (len <= 3) {
            return "*".repeat(len);
        }
        return str.substring(0, len - 3) + "***";
    }
}

