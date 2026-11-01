package com.example.demo.presentation.resource.in;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 匯出物料清單請求載體 (Export BOM Request Resource)
 * <p>
 * 遵循 3-1 命名規範 (V + N + Resource)，作為表現層對外接收 BOM 匯出條件之純資料載體 (Pure Data Carrier)。
 * 內部嚴格禁止定義領域模型或 Command 轉換邏輯。
 *
 * @param parentMaterialCode    母件物料料號 (選填，若未填寫則匯出系統中所有 BOM 清單)
 * @param version               BOM 版本號 (選填，如 1.0)
 * @param includeMultiLevel     是否包含多階層樹狀展開工作表 (預設為 true)
 * @param includeSingleLevel    是否包含單階匯入平鋪格式工作表 (預設為 true)
 * @param includeMaterialMaster 是否包含物料主檔對照表 (預設為 true)
 * @param flushInterval         串流分批沖刷筆數間隔 (預設為 1000)
 */
@Schema(description = "物料清單 (BOM) Excel 檔案匯出請求結構載體")
public record ExportBomResource(
		@Schema(description = "母件物料料號 (若不填則匯出系統所有 BOM)", example = "MAT-FG-001")
		String parentMaterialCode,

		@Schema(description = "BOM 版本號 (若不填則取全部或最新版本)", example = "1.0")
		String version,

		@Schema(description = "是否包含多階層樹狀展開工作表", defaultValue = "true", example = "true")
		Boolean includeMultiLevel,

		@Schema(description = "是否包含單階匯入格式工作表", defaultValue = "true", example = "true")
		Boolean includeSingleLevel,

		@Schema(description = "是否包含物料主檔對照表", defaultValue = "true", example = "true")
		Boolean includeMaterialMaster,

		@Schema(description = "大數據串流模式分批 Flush 間隔筆數", defaultValue = "1000", example = "1000")
		Integer flushInterval
) {

	public ExportBomResource(
			String parentMaterialCode,
			String version,
			Boolean includeMultiLevel,
			Boolean includeSingleLevel,
			Boolean includeMaterialMaster
	) {
		this(parentMaterialCode, version, includeMultiLevel, includeSingleLevel, includeMaterialMaster, 1000);
	}
}
