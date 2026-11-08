package com.example.demo.application.shared.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.example.demo.application.shared.pipeline.ExcelChunkProvider;

/**
 * 微觀管線生產者-消費者 Excel 匯出命令 (Export Pipeline Excel Command)
 * <p>
 * 遵循 AGENTS.md 規則 3-4 (動詞 + 名詞 + Command)，採用 Java Record 作為應用層不可變資料載體。<br>
 * 封裝「模式 A：平行查 DB + 單緒循序串流寫入」所需的所有參數與函數式資料提供者，
 * 透過有界阻斷佇列 (ArrayBlockingQueue) 達成背壓防護，杜絕記憶體暴增與執行緒安全衝突。
 *
 * @param applicationName   產製應用程式名稱
 * @param sheetName         工作表名稱
 * @param headers           標題列名稱清單
 * @param totalRows         預估總列數
 * @param chunkSize         單一資料區塊拉取大小 (預設 1,000)
 * @param queueCapacity     背壓有界佇列容量 (預設 30)
 * @param parallelProducers 平行查詢生產者執行緒數 (預設 4)
 * @param columnWidths      自訂欄寬清單 (選填)
 * @param freezeHeader      是否凍結首列
 * @param chunkProvider     函數式區塊資料提供者 (不得為 null)
 */
public record ExportPipelineExcelCommand(
		String applicationName,
		String sheetName,
		List<String> headers,
		int totalRows,
		int chunkSize,
		int queueCapacity,
		int parallelProducers,
		List<Double> columnWidths,
		boolean freezeHeader,
		ExcelChunkProvider chunkProvider
) {

	/**
	 * 緊湊建構子 (Compact Constructor)，填補預設值並執行集合不可變防禦
	 */
	public ExportPipelineExcelCommand {
		applicationName = applicationName == null || applicationName.isBlank() ? "ExcelModule" : applicationName.trim();
		sheetName = sheetName == null || sheetName.isBlank() ? "Sheet1" : sheetName.trim();
		headers = headers == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(headers));
		totalRows = Math.max(0, totalRows);
		chunkSize = chunkSize > 0 ? chunkSize : 1000;
		queueCapacity = queueCapacity > 0 ? queueCapacity : 30;
		parallelProducers = parallelProducers > 0 ? parallelProducers : 4;
		columnWidths = columnWidths == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(columnWidths));
		Objects.requireNonNull(chunkProvider, "chunkProvider must not be null");
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private String applicationName = "ExcelModule";
		private String sheetName = "Sheet1";
		private List<String> headers = new ArrayList<>();
		private int totalRows = 0;
		private int chunkSize = 1000;
		private int queueCapacity = 30;
		private int parallelProducers = 4;
		private List<Double> columnWidths = new ArrayList<>();
		private boolean freezeHeader = true;
		private ExcelChunkProvider chunkProvider;

		public Builder applicationName(String applicationName) {
			this.applicationName = applicationName;
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

		public Builder chunkSize(int chunkSize) {
			this.chunkSize = chunkSize;
			return this;
		}

		public Builder queueCapacity(int queueCapacity) {
			this.queueCapacity = queueCapacity;
			return this;
		}

		public Builder parallelProducers(int parallelProducers) {
			this.parallelProducers = parallelProducers;
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

		public ExportPipelineExcelCommand build() {
			return new ExportPipelineExcelCommand(
					applicationName,
					sheetName,
					headers,
					totalRows,
					chunkSize,
					queueCapacity,
					parallelProducers,
					columnWidths,
					freezeHeader,
					chunkProvider
			);
		}
	}
}

