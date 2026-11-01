package com.example.demo.application.shared.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Excel 串流工作表寫入命令 (Write Stream Sheet Command)
 * <p>
 * 遵循 {@code V + N + Command} 命名規範，封裝串流寫入單一工作表之迭代器或可迭代資料源。
 * 支援設定 {@code flushInterval}，每寫入指定筆數自動將記憶體快取刷入輸出流，達成常數級 O(1) 記憶體開銷。
 */
public class WriteStreamSheetCommand {

	private final String sheetName;
	private final List<String> headers;
	private final Iterable<List<Object>> rowIterable;
	private final List<Double> columnWidths;
	private final boolean freezeHeader;
	private final int flushInterval;
	private final String headerFillColor;
	private final String headerFontColor;

	private WriteStreamSheetCommand(Builder builder) {
		this.sheetName = builder.sheetName == null || builder.sheetName.isBlank() ? "Sheet1" : builder.sheetName;
		this.headers = builder.headers == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(builder.headers));
		this.rowIterable = builder.rowIterable != null ? builder.rowIterable : Collections.emptyList();
		this.columnWidths = builder.columnWidths == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(builder.columnWidths));
		this.freezeHeader = builder.freezeHeader;
		this.flushInterval = builder.flushInterval > 0 ? builder.flushInterval : 1000;
		this.headerFillColor = builder.headerFillColor;
		this.headerFontColor = builder.headerFontColor;
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

	public Iterable<List<Object>> getRowIterable() {
		return rowIterable;
	}

	public List<Double> getColumnWidths() {
		return columnWidths;
	}

	public boolean isFreezeHeader() {
		return freezeHeader;
	}

	public int getFlushInterval() {
		return flushInterval;
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
		private Iterable<List<Object>> rowIterable;
		private List<Double> columnWidths = new ArrayList<>();
		private boolean freezeHeader = true;
		private int flushInterval = 1000;
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

		public Builder rowIterable(Iterable<List<Object>> rowIterable) {
			this.rowIterable = rowIterable;
			return this;
		}

		public Builder rowIterator(Iterator<List<Object>> iterator) {
			this.rowIterable = () -> iterator;
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

		public Builder flushInterval(int flushInterval) {
			this.flushInterval = flushInterval;
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

		public WriteStreamSheetCommand build() {
			return new WriteStreamSheetCommand(this);
		}
	}
}
