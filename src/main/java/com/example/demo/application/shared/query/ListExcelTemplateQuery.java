package com.example.demo.application.shared.query;

/**
 * 查詢 Excel 範本清單查詢載體 (Query)。
 * <p>
 * 遵循 AGENTS.md 規則 3-5，以 {@code V + N + Query} 命名。
 * 應用層純資料載體 (Java 21 Record)，支援業務分類篩選與是否僅列出啟用中範本。
 * </p>
 *
 * @param category   業務分類篩選 (可為 null，代表不分分類)
 * @param activeOnly 是否僅查詢啟用中範本 (預設 true)
 */
public record ListExcelTemplateQuery(
        String category,
        Boolean activeOnly
) {
    public ListExcelTemplateQuery {
        if (activeOnly == null) {
            activeOnly = true;
        }
    }

    public static ListExcelTemplateQuery all() {
        return new ListExcelTemplateQuery(null, true);
    }
}

