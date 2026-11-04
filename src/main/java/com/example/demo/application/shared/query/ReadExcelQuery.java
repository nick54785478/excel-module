package com.example.demo.application.shared.query;

/**
 * Excel 檔案讀取查詢參數物件 (Read Excel Query)
 * <p>
 * 遵循 AGENTS.md 3-5 命名規範 ({@code V + N + Query})，採用 Java Record 作為應用層不可變純資料載體。
 * 封裝讀取活頁簿時之目標工作表、標題列位置、資料列起始行、最大讀取筆數與字串去除空白等條件選項。
 *
 * @param sheetIndex        目標工作表索引 (0-based，可為 null 代表依名稱或預設第 0 個)
 * @param sheetName         目標工作表名稱 (可為 null 代表依索引或讀取全部)
 * @param hasHeader         是否有標題列 (預設 true)
 * @param headerRowIndex    標題列所在列索引 (0-based，預設 0)
 * @param startDataRowIndex 資料列起始列索引 (0-based，預設 1)
 * @param maxRows           最大讀取資料筆數 (null 代表無限制)
 * @param trimWhitespace    字串欄位是否自動去除首尾空白 (預設 true)
 */
public record ReadExcelQuery(
		Integer sheetIndex,
		String sheetName,
		boolean hasHeader,
		int headerRowIndex,
		int startDataRowIndex,
		Integer maxRows,
		boolean trimWhitespace
) {

	/**
	 * 緊湊建構子 (Compact Constructor)，執行安全防禦與預設值補正
	 */
	public ReadExcelQuery {
		if (headerRowIndex < 0) {
			headerRowIndex = 0;
		}
		if (startDataRowIndex < 0) {
			startDataRowIndex = hasHeader ? headerRowIndex + 1 : 0;
		}
	}

	/**
	 * 預設查詢條件：讀取第 0 列為標題、第 1 列為資料列開始、去除字串前後空白
	 *
	 * @return 預設查詢物件
	 */
	public static ReadExcelQuery defaultQuery() {
		return builder().build();
	}

	/**
	 * 指定讀取特定工作表索引 (0-based)
	 *
	 * @param sheetIndex 工作表索引
	 * @return 查詢物件
	 */
	public static ReadExcelQuery ofSheet(int sheetIndex) {
		return builder().sheetIndex(sheetIndex).build();
	}

	/**
	 * 指定讀取特定工作表名稱
	 *
	 * @param sheetName 工作表名稱
	 * @return 查詢物件
	 */
	public static ReadExcelQuery ofSheet(String sheetName) {
		return builder().sheetName(sheetName).build();
	}

	/**
	 * 指定標題列位置 (0-based) 與資料起始列
	 *
	 * @param headerRowIndex 標題列索引
	 * @return 查詢物件
	 */
	public static ReadExcelQuery ofHeaderRow(int headerRowIndex) {
		return builder()
				.headerRowIndex(headerRowIndex)
				.startDataRowIndex(headerRowIndex + 1)
				.build();
	}

	/**
	 * 取得建造者物件
	 *
	 * @return 新建的 {@link Builder} 實例
	 */
	public static Builder builder() {
		return new Builder();
	}

	// ==================== 向後相容別名方法 (Backward Compatible Accessors) ====================

	public Integer getSheetIndex() {
		return sheetIndex();
	}

	public String getSheetName() {
		return sheetName();
	}

	public boolean isHasHeader() {
		return hasHeader();
	}

	public int getHeaderRowIndex() {
		return headerRowIndex();
	}

	public int getStartDataRowIndex() {
		return startDataRowIndex();
	}

	public Integer getMaxRows() {
		return maxRows();
	}

	public boolean isTrimWhitespace() {
		return trimWhitespace();
	}

	/**
	 * 建造者模式 (Builder)
	 */
	public static class Builder {
		private Integer sheetIndex;
		private String sheetName;
		private boolean hasHeader = true;
		private int headerRowIndex = 0;
		private int startDataRowIndex = 1;
		private Integer maxRows;
		private boolean trimWhitespace = true;

		public Builder sheetIndex(Integer sheetIndex) {
			this.sheetIndex = sheetIndex;
			return this;
		}

		public Builder sheetName(String sheetName) {
			this.sheetName = sheetName;
			return this;
		}

		public Builder hasHeader(boolean hasHeader) {
			this.hasHeader = hasHeader;
			if (!hasHeader) {
				this.startDataRowIndex = 0;
			}
			return this;
		}

		public Builder headerRowIndex(int headerRowIndex) {
			this.headerRowIndex = headerRowIndex;
			return this;
		}

		public Builder startDataRowIndex(int startDataRowIndex) {
			this.startDataRowIndex = startDataRowIndex;
			return this;
		}

		public Builder maxRows(Integer maxRows) {
			this.maxRows = maxRows;
			return this;
		}

		public Builder trimWhitespace(boolean trimWhitespace) {
			this.trimWhitespace = trimWhitespace;
			return this;
		}

		public ReadExcelQuery build() {
			return new ReadExcelQuery(
					sheetIndex,
					sheetName,
					hasHeader,
					headerRowIndex,
					startDataRowIndex,
					maxRows,
					trimWhitespace
			);
		}
	}
}
