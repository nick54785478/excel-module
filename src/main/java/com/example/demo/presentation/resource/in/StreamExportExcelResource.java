package com.example.demo.presentation.resource.in;

import java.util.List;

import com.example.demo.presentation.resource.in.ExportExcelResource.ExportSheetResource;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
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
 * 作為純資料載體，提供呼叫端傳入真實工作表資料、表頭與 flush 批次沖刷大小。
 *
 * @param fileName       串流匯出檔案名稱，不可為空且必須以 .xlsx 或 .xls 為結尾
 * @param batchFlushSize 批次沖刷記憶體筆數，至少需大於等於 1
 * @param sheets         欲串流匯出之工作表清單，至少需包含一個工作表
 */
@Schema(description = "Excel 檔案百萬級大數據串流匯出請求載體")
public record StreamExportExcelResource(
		@Schema(description = "串流匯出檔案名稱 (必須以 .xlsx 或 .xls 結尾)", example = "大數據銷售報表.xlsx", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotBlank(message = "串流匯出檔案名稱不能為空")
		@Pattern(regexp = "^[\\w\\-.\\u4e00-\\u9fa5 ]+\\.(xlsx|xls)$", message = "檔案名稱必須包含合法副檔名 (.xlsx 或 .xls)")
		String fileName,

		@Schema(description = "批次沖刷筆數 (Flush Interval，每滿指定筆數即 Flush 至客戶端串流)", example = "500", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull(message = "批次沖刷筆數 (batchFlushSize) 不能為 null")
		@Min(value = 1, message = "批次沖刷筆數 (batchFlushSize) 必須大於等於 1")
		Integer batchFlushSize,

		@Schema(description = "工作表清單", requiredMode = Schema.RequiredMode.REQUIRED)
		@Valid
		@NotEmpty(message = "工作表清單不能為空")
		List<ExportSheetResource> sheets
) {

	public StreamExportExcelResource {
		if (batchFlushSize == null) {
			batchFlushSize = 1000;
		}
	}
}
