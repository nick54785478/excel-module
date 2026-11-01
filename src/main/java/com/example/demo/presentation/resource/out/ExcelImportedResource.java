package com.example.demo.presentation.resource.out;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Excel 檔案匯入解析回應資源物件 (Excel Imported Response Resource)
 * <p>
 * 遵循命名規範：{@code N + Ved + Resource} (3-2 規範)。
 * 作為表現層對外回傳之純資料載體，不包含任何轉換邏輯。
 *
 * @param sheetCount 解析出的工作表總數
 * @param sheets     各工作表結構與資料明細清單
 */
@Schema(description = "Excel 檔案匯入解析完成回應載體")
public record ExcelImportedResource(
		@Schema(description = "解析出的工作表總數", example = "1")
		int sheetCount,

		@Schema(description = "各工作表結構與資料明細清單")
		List<ExcelSheetImportedResource> sheets
) {

	/**
	 * 工作表解析結果載體
	 *
	 * @param sheetIndex 工作表索引 (0-based)
	 * @param sheetName  工作表名稱
	 * @param rowCount   資料列總數 (不含表頭)
	 * @param headers    工作表表頭清單
	 * @param dataRows   每列資料 (鍵值映射表)
	 */
	@Schema(description = "單一工作表解析結構明細")
	public record ExcelSheetImportedResource(
			@Schema(description = "工作表索引 (0-based)", example = "0")
			int sheetIndex,

			@Schema(description = "工作表名稱", example = "員工資料表")
			String sheetName,

			@Schema(description = "資料列總數 (不含表頭)", example = "2")
			int rowCount,

			@Schema(description = "表頭欄位清單", example = "[\"ID\", \"姓名\", \"部門\"]")
			List<String> headers,

			@Schema(description = "資料列物件清單 (欄位鍵值對)")
			List<Map<String, Object>> dataRows
	) {
	}
}
