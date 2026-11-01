package com.example.demo.presentation.resource.out;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Excel 檔案串流匯入解析回應資源物件 (Excel Stream Imported Response Resource)
 * <p>
 * 遵循命名規範：{@code N + Ved + Resource} (3-2 規範)。
 * 作為表現層對外回傳之純資料載體，呈現大數據串流分塊處理之統計指標。
 *
 * @param sheetName          處理之工作表名稱
 * @param totalRowsProcessed 累計成功解析之總列數
 * @param totalChunks        分塊 (Chunk) 批次總數
 * @param durationMs         串流處理耗時 (毫秒)
 * @param status             執行狀態代碼 (例如 "SUCCESS")
 * @param message            處理結果說明摘要
 */
@Schema(description = "Excel 檔案大數據串流分塊匯入解析統計回應載體")
public record ExcelStreamImportedResource(
		@Schema(description = "處理之工作表名稱", example = "銷售紀錄")
		String sheetName,

		@Schema(description = "累計成功解析之總列數", example = "50000")
		long totalRowsProcessed,

		@Schema(description = "分塊 (Chunk) 批次總數", example = "50")
		int totalChunks,

		@Schema(description = "串流處理耗時 (毫秒)", example = "342")
		long durationMs,

		@Schema(description = "執行狀態代碼", example = "SUCCESS")
		String status,

		@Schema(description = "處理結果說明摘要", example = "串流處理成功完成，共處理 50000 行資料（50 個批次）")
		String message
) {
}
