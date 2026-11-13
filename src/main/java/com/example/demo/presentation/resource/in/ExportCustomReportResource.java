package com.example.demo.presentation.resource.in;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

/**
 * 客製化進階報表匯出請求資源載體 (Export Custom Report Request Resource)
 * <p>
 * 遵循 AGENTS.md 3-1 命名規範 ({@code V + N + Resource})。
 * 作為表現層 HTTP 請求輸入純資料載體，嚴禁於內部實作轉換邏輯。
 *
 * @param sheetName       工作表名稱
 * @param headers         表頭標題欄位清單
 * @param rows            二維資料列清單 (支援純文字、數值或以 "=" 開頭之公式)
 * @param mergedRegions   儲存格合併區間清單
 * @param cellFormulas    額外儲存格動態公式 (Key: 座標如 "D5", Value: 公式如 "SUM(D2:D4)")
 * @param password        工作表安全防護密碼
 * @param reportTitle     列印頁首報表標題
 * @param classification  列印頁尾機密等級 (如 "財務機密")
 * @param headerFillColor 標題列背景填色 HEX 碼 (如 "1F4E78")
 * @param headerFontColor 標題列文字顏色 HEX 碼 (如 "FFFFFF")
 */
@Schema(description = "客製化進階報表匯出請求資源載體")
public record ExportCustomReportResource(
		@Schema(description = "工作表名稱", example = "客製損益表")
		String sheetName,

		@Schema(description = "表頭標題欄位清單", requiredMode = Schema.RequiredMode.REQUIRED, example = "[\"物料名稱\", \"標準單價\", \"採購用量\", \"採購小計\"]")
		@NotEmpty(message = "表頭標題不能為空")
		List<String> headers,

		@Schema(
				description = "二維資料列清單 (支援普通數值與 '=' 開頭之公式)",
				requiredMode = Schema.RequiredMode.REQUIRED,
				example = "[[\"高精度無刷馬達\", 850.0, 10, \"=B2*C2\"], [\"碳纖維螺旋槳\", 120.0, 20, \"=B3*C3\"], [\"智慧調速器\", 450.0, 15, \"=B4*C4\"], [\"合計\", null, null, \"=SUM(D2:D4)\"]]"
		)
		@NotEmpty(message = "資料列不能為空")
		List<List<Object>> rows,

		@Schema(description = "儲存格合併區間清單")
		List<MergedRegionResource> mergedRegions,

		@Schema(
				description = "特定座標動態公式對照表 (Key: 座標代碼如 D5, Value: 公式文字如 SUM(D2:D4))",
				example = "{\"D5\": \"SUM(D2:D4)\"}"
		)
		Map<String, String> cellFormulas,

		@Schema(description = "工作表防篡改保護解鎖密碼", example = "AuditPass2026")
		String password,

		@Schema(description = "列印頁首報表大標題", example = "物料採購損益分析月報")
		String reportTitle,

		@Schema(description = "列印頁尾機密等級標籤", example = "機密等級・內部流轉")
		String classification,

		@Schema(description = "標題列背景填色 HEX 碼", example = "1F4E78")
		String headerFillColor,

		@Schema(description = "標題列字體顏色 HEX 碼", example = "FFFFFF")
		String headerFontColor
) {

	/**
	 * 儲存格合併區間請求資料項
	 *
	 * @param firstRow 起始列索引 (0-based)
	 * @param lastRow  結束列索引 (0-based)
	 * @param firstCol 起始欄索引 (0-based)
	 * @param lastCol  結束欄索引 (0-based)
	 */
	@Schema(description = "儲存格合併區間 (0-based)")
	public record MergedRegionResource(
			@Schema(description = "起始列 (0-based)", example = "4")
			int firstRow,
			@Schema(description = "結束列 (0-based)", example = "4")
			int lastRow,
			@Schema(description = "起始欄 (0-based)", example = "0")
			int firstCol,
			@Schema(description = "結束欄 (0-based)", example = "2")
			int lastCol
	) {}
}
