package com.example.demo.application.shared.dto;

/**
 * Excel 檔案串流匯入解析結果 (Excel Stream Import Gotten Result)
 * <p>
 * 遵循命名規範：{@code N + Gotten/Searched + Result} (3-6 規範)。
 * 作為串流匯入完成後的統計傳輸載體，封裝工作表、處理行數、分塊數與執行耗時。
 */
public record ExcelStreamImportGottenResult(
		String sheetName,
		long totalRowsProcessed,
		int totalChunks,
		long durationMs,
		String status,
		String message
) {
}

