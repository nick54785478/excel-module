package com.example.demo.application.shared.query;

/**
 * Excel 檔案讀取查詢參數物件 (Read Excel Query)
 * <p>
 * 遵循 {@code V + N + Query} 命名規範 (3-5 規範)，封裝讀取活頁簿時之目標工作表、標題列位置、
 * 資料列起始行、最大讀取筆數與字串去除空白等條件選項。
 */
public class ReadExcelQuery {

	private final Integer sheetIndex;
	private final String sheetName;
	private final boolean hasHeader;
	private final int headerRowIndex;
	private final int startDataRowIndex;
	private final Integer maxRows;
	private final boolean trimWhitespace;

	private ReadExcelQuery(Builder builder) {
		this.sheetIndex = builder.sheetIndex;
		this.sheetName = builder.sheetName;
		this.hasHeader = builder.hasHeader;
		this.headerRowIndex = builder.headerRowIndex;
		this.startDataRowIndex = builder.startDataRowIndex;
		this.maxRows = builder.maxRows;
		this.trimWhitespace = builder.trimWhitespace;
	}

	/**
	 * 預設查詢條件：讀取所有工作表、第 0 列為標題、第 1 列為資料列開始、去除字串前後空白
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

	public static Builder builder() {
		return new Builder();
	}

	public Integer getSheetIndex() {
		return sheetIndex;
	}

	public String getSheetName() {
		return sheetName;
	}

	public boolean isHasHeader() {
		return hasHeader;
	}

	public int getHeaderRowIndex() {
		return headerRowIndex;
	}

	public int getStartDataRowIndex() {
		return startDataRowIndex;
	}

	public Integer getMaxRows() {
		return maxRows;
	}

	public boolean isTrimWhitespace() {
		return trimWhitespace;
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
			return new ReadExcelQuery(this);
		}
	}
}
