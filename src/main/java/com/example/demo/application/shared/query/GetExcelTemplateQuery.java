package com.example.demo.application.shared.query;

import java.util.Objects;

/**
 * 查詢單一 Excel 範本查詢載體 (Query)。
 * <p>
 * 遵循 AGENTS.md 規則 3-5，以 {@code V + N + Query} 命名。
 * 應用層純資料載體 (Java 21 Record)。
 * </p>
 *
 * @param templateKey 欲查詢之範本唯一識別鍵
 */
public record GetExcelTemplateQuery(
        String templateKey
) {
    public GetExcelTemplateQuery {
        Objects.requireNonNull(templateKey, "templateKey 不得為空");
    }
}

