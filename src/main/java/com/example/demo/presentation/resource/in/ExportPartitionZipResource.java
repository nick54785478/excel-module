package com.example.demo.presentation.resource.in;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 模式 B / 模式 A+B：宏觀多活頁簿 ZIP 分片匯出請求資源 (Export Partition ZIP Request Resource)
 * <p>
 * 遵循 AGENTS.md 規則 3-1 (動詞 + 名詞 + Resource) 與規則 33~36 規範。<br>
 * 作為表現層純資料載體 (Pure Data Carrier)，<b>嚴禁於內部撰寫任何業務轉換或 toCommand 邏輯</b>。<br>
 * 資料組裝一律由 {@link com.example.demo.presentation.assembler.ExcelParallelResourceAssembler} 負責。
 *
 * @param zipFileName                 ZIP 壓縮封裝檔名
 * @param baseFileName                各分片 Excel 檔名前綴
 * @param sheetName                   工作表名稱
 * @param totalRows                   欲模擬產製之總列數 (如 50,000)
 * @param maxRowsPerFile              單一 Excel 活頁簿最大列數 (如 20,000，將自動切分為 3 個分片)
 * @param chunkSize                   單一區塊批次大小 (預設 1,000)
 * @param parallelProducersPerPartition 每個分片內部查詢之平行執行緒數 (預設 4)
 * @param usePipelinePerPartition     每個分片內部是否啟用模式 A 生產者-消費者管線
 * @param headers                     自訂欄位表頭清單 (選填)
 */
@Schema(
		description = "多活頁簿分片平行產製並壓縮打包為 ZIP 請求資源 (模式 B / A+B)",
		example = """
		{
		  "zipFileName": "orders_multi_partition_archive.zip",
		  "baseFileName": "orders_part",
		  "sheetName": "訂單明細",
		  "totalRows": 50000,
		  "maxRowsPerFile": 20000,
		  "chunkSize": 1000,
		  "parallelProducersPerPartition": 4,
		  "usePipelinePerPartition": true,
		  "headers": ["序號", "訂單流水號", "料件編號", "出貨數量", "標準售價", "交易總額", "歸檔時間"]
		}
		"""
)
public record ExportPartitionZipResource(
		@Schema(description = "ZIP 封裝下載檔名", example = "orders_multi_partition_archive.zip")
		String zipFileName,

		@Schema(description = "各分片 Excel 檔名前綴", example = "orders_part")
		String baseFileName,

		@Schema(description = "活頁簿工作表名稱", example = "訂單明細")
		String sheetName,

		@Schema(description = "預計產製總列數", example = "50000")
		Integer totalRows,

		@Schema(description = "單一活頁簿上限列數 (超出自動分片)", example = "20000")
		Integer maxRowsPerFile,

		@Schema(description = "單一區塊批次大小", example = "1000")
		Integer chunkSize,

		@Schema(description = "各分片內部平行查詢執行緒數", example = "4")
		Integer parallelProducersPerPartition,

		@Schema(description = "各分片內部是否啟用模式 A 管線加速", example = "true")
		Boolean usePipelinePerPartition,

		@Schema(description = "自訂表頭清單 (選填)")
		List<String> headers
) {
}

