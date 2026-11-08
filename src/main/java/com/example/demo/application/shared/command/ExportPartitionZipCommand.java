package com.example.demo.application.shared.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.example.demo.application.shared.pipeline.ExcelChunkProvider;

/**
 * 宏觀多活頁簿 ZIP 分片匯出命令 (Export Partition ZIP Command)
 * <p>
 * 遵循 AGENTS.md 規則 3-4 (動詞 + 名詞 + Command)，採用 Java Record 作為不可變純資料載體。<br>
 * 封裝「模式 B：多檔案平行產製 + ZIP 串流打包」與「模式 A + B 組合拳」所需之參數配置。
 * 突破 Excel 單表 1,048,576 列硬限制，將超大資料集分片並行產製多個獨立的 .xlsx 檔案，
 * 並串流封裝進單一 ZIP 壓縮檔供使用者高速下載。
 *
 * @param zipFileName                 ZIP 壓縮封裝檔名 (預設 export_archive.zip)
 * @param baseFileName                各分片 Excel 檔名前綴 (如 orders_part)
 * @param sheetName                   各活頁簿內工作表名稱 (預設 Sheet1)
 * @param headers                     標題列名稱清單
 * @param totalRows                   預計匯出之總列數
 * @param maxRowsPerFile              單一 Excel 活頁簿最大列數 (預設 500,000，上限 1,000,000)
 * @param chunkSize                   單一區塊拉取大小 (預設 1,000)
 * @param parallelProducersPerPartition 每個分片內部查詢之平行執行緒數 (預設 4)
 * @param usePipelinePerPartition     每個分片內部是否啟用模式 A 生產者-消費者管線 (預設 true)
 * @param columnWidths                自訂欄寬清單 (選填)
 * @param freezeHeader                是否凍結首列
 * @param chunkProvider               函數式區塊資料提供者 (不得為 null)
 */
public record ExportPartitionZipCommand(
		String zipFileName,
		String baseFileName,
		String sheetName,
		List<String> headers,
		int totalRows,
		int maxRowsPerFile,
		int chunkSize,
		int parallelProducersPerPartition,
		boolean usePipelinePerPartition,
		List<Double> columnWidths,
		boolean freezeHeader,
		ExcelChunkProvider chunkProvider
) {

	public static final int EXCEL_MAX_ROWS_LIMIT = 1048570;

	/**
	 * 緊湊建構子 (Compact Constructor)，預設值填補與安全防護
	 */
	public ExportPartitionZipCommand {
		zipFileName = zipFileName == null || zipFileName.isBlank() ? "export_archive.zip" : zipFileName.trim();
		baseFileName = baseFileName == null || baseFileName.isBlank() ? "export_part" : baseFileName.trim();
		sheetName = sheetName == null || sheetName.isBlank() ? "Sheet1" : sheetName.trim();
		headers = headers == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(headers));
		totalRows = Math.max(0, totalRows);
		maxRowsPerFile = (maxRowsPerFile > 0 && maxRowsPerFile <= EXCEL_MAX_ROWS_LIMIT) ? maxRowsPerFile : 500000;
		chunkSize = chunkSize > 0 ? chunkSize : 1000;
		parallelProducersPerPartition = parallelProducersPerPartition > 0 ? parallelProducersPerPartition : 4;
		columnWidths = columnWidths == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(columnWidths));
		Objects.requireNonNull(chunkProvider, "chunkProvider must not be null");
	}

	/**
	 * 計算分片總數量
	 *
	 * @return 分片總數 (至少為 1)
	 */
	public int calculatePartitionCount() {
		if (totalRows <= 0) {
			return 1;
		}
		return (int) Math.ceil((double) totalRows / maxRowsPerFile);
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private String zipFileName = "export_archive.zip";
		private String baseFileName = "export_part";
		private String sheetName = "Sheet1";
		private List<String> headers = new ArrayList<>();
		private int totalRows = 0;
		private int maxRowsPerFile = 500000;
		private int chunkSize = 1000;
		private int parallelProducersPerPartition = 4;
		private boolean usePipelinePerPartition = true;
		private List<Double> columnWidths = new ArrayList<>();
		private boolean freezeHeader = true;
		private ExcelChunkProvider chunkProvider;

		public Builder zipFileName(String zipFileName) {
			this.zipFileName = zipFileName;
			return this;
		}

		public Builder baseFileName(String baseFileName) {
			this.baseFileName = baseFileName;
			return this;
		}

		public Builder sheetName(String sheetName) {
			this.sheetName = sheetName;
			return this;
		}

		public Builder headers(List<String> headers) {
			this.headers = headers;
			return this;
		}

		public Builder totalRows(int totalRows) {
			this.totalRows = totalRows;
			return this;
		}

		public Builder maxRowsPerFile(int maxRowsPerFile) {
			this.maxRowsPerFile = maxRowsPerFile;
			return this;
		}

		public Builder chunkSize(int chunkSize) {
			this.chunkSize = chunkSize;
			return this;
		}

		public Builder parallelProducersPerPartition(int parallelProducersPerPartition) {
			this.parallelProducersPerPartition = parallelProducersPerPartition;
			return this;
		}

		public Builder usePipelinePerPartition(boolean usePipelinePerPartition) {
			this.usePipelinePerPartition = usePipelinePerPartition;
			return this;
		}

		public Builder columnWidths(List<Double> columnWidths) {
			this.columnWidths = columnWidths;
			return this;
		}

		public Builder freezeHeader(boolean freezeHeader) {
			this.freezeHeader = freezeHeader;
			return this;
		}

		public Builder chunkProvider(ExcelChunkProvider chunkProvider) {
			this.chunkProvider = chunkProvider;
			return this;
		}

		public ExportPartitionZipCommand build() {
			return new ExportPartitionZipCommand(
					zipFileName,
					baseFileName,
					sheetName,
					headers,
					totalRows,
					maxRowsPerFile,
					chunkSize,
					parallelProducersPerPartition,
					usePipelinePerPartition,
					columnWidths,
					freezeHeader,
					chunkProvider
			);
		}
	}
}

