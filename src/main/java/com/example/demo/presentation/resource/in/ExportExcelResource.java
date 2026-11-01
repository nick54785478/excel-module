package com.example.demo.presentation.resource.in;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Excel 檔案匯出請求資源物件 (Export Excel Request Resource)
 * <p>
 * 遵循命名規範：{@code V + N + Resource} (3-1 規範)。
 * 透過 Jakarta Bean Validation 標註進行請求參數邊界檢核。
 * 作為純資料載體 (Pure Data Carrier)，內部嚴禁撰寫任何防腐或業務轉換邏輯。
 *
 * @param fileName 匯出檔案名稱，不可為空且必須以 .xlsx 或 .xls 為結尾
 * @param sheets   工作表列表，至少需包含一個工作表
 */
@Schema(description = "Excel 檔案標準批次匯出請求載體")
public record ExportExcelResource(
		@Schema(description = "匯出檔案名稱 (必須以 .xlsx 或 .xls 結尾)", example = "使用者名冊.xlsx", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotBlank(message = "匯出檔案名稱不能為空")
		@Pattern(regexp = "^[\\w\\-.\\u4e00-\\u9fa5 ]+\\.(xlsx|xls)$", message = "檔案名稱必須包含合法副檔名 (.xlsx 或 .xls)")
		String fileName,

		@Schema(description = "工作表清單 (至少包含一個工作表)", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotEmpty(message = "匯出內容至少需包含一個工作表 (sheet)")
		List<@Valid ExportSheetResource> sheets
) {

	/**
	 * 工作表請求載體
	 *
	 * @param sheetName 工作表名稱，不可為空
	 * @param headers   工作表表頭，至少需包含一個欄位名稱
	 * @param rows      資料列清單，不可為 null
	 */
	@Schema(description = "單一工作表結構與資料載體")
	public record ExportSheetResource(
			@Schema(description = "工作表名稱", example = "員工資料表", requiredMode = Schema.RequiredMode.REQUIRED)
			@NotBlank(message = "工作表名稱不能為空")
			String sheetName,

			@Schema(description = "工作表表頭清單", example = "[\"工號\", \"姓名\", \"部門\"]", requiredMode = Schema.RequiredMode.REQUIRED)
			@NotEmpty(message = "工作表表頭 (headers) 不能為空")
			List<String> headers,

			@Schema(description = "各列資料二維陣列", example = "[[\"E001\", \"王小明\", \"資訊部\"], [\"E002\", \"李小華\", \"財務部\"]]", requiredMode = Schema.RequiredMode.REQUIRED)
			@NotNull(message = "資料列清單 (rows) 不能為 null")
			List<List<Object>> rows
	) {
	}
}
