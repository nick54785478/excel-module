package com.example.demo.presentation.resource.in;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Excel 檔案大數據串流匯出請求資源物件 (Stream Export Excel Request Resource)
 * <p>
 * 遵循命名規範：{@code V + N + Resource} (3-1 規範)。
 * 透過 Jakarta Bean Validation 標註進行串流請求參數檢核。
 * 作為純資料載體，提供呼叫端指定串流測試資料筆數、標題與 flush 批次大小。
 *
 * @param fileName       串流匯出檔案名稱，不可為空且必須以 .xlsx 或 .xls 為結尾
 * @param sheetName      工作表名稱，不可為空
 * @param headers        表頭欄位清單，不可為空
 * @param rowCount       串流生成筆數，至少需為 1 筆
 * @param batchFlushSize 批次沖刷記憶體筆數，至少需大於等於 1
 */
@Schema(description = "Excel 檔案百萬級大數據串流匯出請求載體")
public record StreamExportExcelResource(
		@Schema(description = "串流匯出檔案名稱 (必須以 .xlsx 或 .xls 結尾)", example = "大數據銷售報表.xlsx", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotBlank(message = "串流匯出檔案名稱不能為空")
		@Pattern(regexp = "^[\\w\\-.\\u4e00-\\u9fa5 ]+\\.(xlsx|xls)$", message = "檔案名稱必須包含合法副檔名 (.xlsx 或 .xls)")
		String fileName,

		@Schema(description = "工作表名稱", example = "銷售紀錄", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotBlank(message = "工作表名稱不能為空")
		String sheetName,

		@Schema(description = "工作表表頭清單", example = "[\"交易序號\", \"商品\", \"金額\"]", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotEmpty(message = "表頭定義 (headers) 不能為空")
		List<String> headers,

		@Schema(description = "欲生成之串流資料列數", example = "10000", requiredMode = Schema.RequiredMode.REQUIRED)
		@Min(value = 1, message = "串流生成筆數 (rowCount) 至少需為 1 筆")
		int rowCount,

		@Schema(description = "批次沖刷筆數 (Flush Interval，每滿指定筆數即 Flush 至客戶端串流)", example = "500", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull(message = "批次沖刷筆數 (batchFlushSize) 不能為 null")
		@Min(value = 1, message = "批次沖刷筆數 (batchFlushSize) 必須大於等於 1")
		Integer batchFlushSize
) {
}
