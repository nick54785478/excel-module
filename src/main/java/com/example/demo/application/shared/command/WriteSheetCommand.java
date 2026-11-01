package com.example.demo.application.shared.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Excel 工作表寫入命令物件 (Write Sheet Command)
 * <p>
 * 遵循 {@code V + N + Command} 命名規範，封裝單一工作表之標題、資料列與樣式設定。
 * 作為純資料載體傳遞至 {@link com.example.demo.application.port.out.ExcelWriterPort}。
 */
public class WriteSheetCommand {

	private final String sheetName;
	private final List<String> headers;
	private final List<List<Object>> rows;
	private final List<Double> columnWidths;
	private final boolean freezeHeader;
	private final String headerFillColor;
	private final String headerFontColor;

	private WriteSheetCommand(Builder builder) {
		this.sheetName = builder.sheetName == null || builder.sheetName.isBlank() ? "Sheet1" : builder.sheetName;
		this.headers = builder.headers == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(builder.headers));
		this.rows = builder.rows == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(builder.rows));
		this.columnWidths = builder.columnWidths == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(builder.columnWidths));
		this.freezeHeader = builder.freezeHeader;
		this.headerFillColor = builder.headerFillColor;
		this.headerFontColor = builder.headerFontColor;
	}

	/**
	 * 便捷靜態建立方法
	 *
	 * @param sheetName 工作表名稱
	 * @param headers   標題清單
	 * @param rows      資料列清單
	 * @return 工作表寫入命令
	 */
	public static WriteSheetCommand of(String sheetName, List<String> headers, List<List<Object>> rows) {
		return builder()
				.sheetName(sheetName)
				.headers(headers)
				.rows(rows)
				.build();
	}

	public static Builder builder() {
		return new Builder();
	}

	public String getSheetName() {
		return sheetName;
	}

	public List<String> getHeaders() {
		return headers;
	}

	public List<List<Object>> getRows() {
		return rows;
	}

	public List<Double> getColumnWidths() {
		return columnWidths;
	}

	public boolean isFreezeHeader() {
		return freezeHeader;
	}

	public String getHeaderFillColor() {
		return headerFillColor;
	}

	public String getHeaderFontColor() {
		return headerFontColor;
	}

	/**
	 * 建造者模式 (Builder)
	 */
	public static class Builder {
		private String sheetName = "Sheet1";
		private List<String> headers = new ArrayList<>();
		private List<List<Object>> rows = new ArrayList<>();
		private List<Double> columnWidths = new ArrayList<>();
		private boolean freezeHeader = true;
		private String headerFillColor = "F2F2F2";
		private String headerFontColor = "000000";

		public Builder sheetName(String sheetName) {
			this.sheetName = sheetName;
			return this;
		}

		public Builder headers(List<String> headers) {
			this.headers = headers;
			return this;
		}

		public Builder rows(List<List<Object>> rows) {
			this.rows = rows;
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

		public Builder headerFillColor(String headerFillColor) {
			this.headerFillColor = headerFillColor;
			return this;
		}

		public Builder headerFontColor(String headerFontColor) {
			this.headerFontColor = headerFontColor;
			return this;
		}

		public WriteSheetCommand build() {
			return new WriteSheetCommand(this);
		}
	}
}
