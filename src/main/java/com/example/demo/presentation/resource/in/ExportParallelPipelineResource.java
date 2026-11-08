package com.example.demo.presentation.resource.in;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 模式 A：微觀管線生產者-消費者平行匯出請求資源 (Export Parallel Pipeline Request Resource)
 * <p>
 * 遵循 AGENTS.md 規則 3-1 (動詞 + 名詞 + Resource) 與規則 33~36 規範。<br>
 * 作為表現層純資料載體 (Pure Data Carrier)，<b>嚴禁於內部撰寫任何業務轉換或 toCommand 邏輯</b>。<br>
 * 資料組裝一律由 {@link com.example.demo.presentation.assembler.ExcelParallelResourceAssembler} 負責。
 *
 * @param sheetName         自訂工作表名稱
 * @param totalRows         欲模擬產製之總列數 (如 10,000)
 * @param chunkSize         單一資料區塊大小 (預設 1,000)
 * @param queueCapacity     背壓有界佇列容量 (預設 30)
 * @param parallelProducers 平行查詢生產者執行緒數 (預設 4)
 * @param headers           自訂欄位表頭清單 (選填)
 */
@Schema(
		description = "微觀生產者-消費者管線平行匯出請求資源 (模式 A)",
		example = """
		{
		  "sheetName": "高並發管線匯出明細",
		  "totalRows": 20000,
		  "chunkSize": 1000,
		  "queueCapacity": 30,
		  "parallelProducers": 4,
		  "headers": ["序號", "訂單編號", "物料代碼", "數量", "單價", "總金額", "建立時間"]
		}
		"""
)
public record ExportParallelPipelineResource(
		@Schema(description = "自訂工作表名稱", example = "高並發管線匯出明細")
		String sheetName,

		@Schema(description = "預計產製總列數", example = "20000")
		Integer totalRows,

		@Schema(description = "單一區塊批次大小", example = "1000")
		Integer chunkSize,

		@Schema(description = "背壓有界佇列容量", example = "30")
		Integer queueCapacity,

		@Schema(description = "平行查詢生產者執行緒數", example = "4")
		Integer parallelProducers,

		@Schema(description = "自訂表頭清單 (選填)")
		List<String> headers
) {
}

