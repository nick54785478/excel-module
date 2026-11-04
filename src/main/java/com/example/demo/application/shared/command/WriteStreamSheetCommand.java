package com.example.demo.application.shared.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Excel 串流工作表寫入命令 (Write Stream Sheet Command)
 * <p>
 * 遵循 AGENTS.md 3-4 命名規範 ({@code V + N + Command})，採用 Java Record 作為應用層不可變純資料載體。
 * 封裝串流寫入單一工作表之可迭代資料來源 ({@link Iterable})、標題列、欄寬設定與串流沖刷間隔 (flushInterval)。
 *
 * @param sheetName       工作表名稱 (預設 Sheet1)
 * @param headers         標題欄位清單 (不可變清單)
 * @param rowIterable     可迭代之資料列來源 (支援延遲載入以達成 O(1) 記憶體空間)
 * @param columnWidths    自訂欄寬清單 (不可變清單)
 * @param freezeHeader    是否凍結標題列首行
 * @param flushInterval   每寫入指定筆數自動 Flush 至底層串流 (預設 1000 筆)
 * @param headerFillColor 標題列背景填色 HEX 碼
 * @param headerFontColor 標題列字體顏色 HEX 碼
 */
public record WriteStreamSheetCommand(
		String sheetName,
		List<String> headers,
		Iterable<List<Object>> rowIterable,
		List<Double> columnWidths,
		boolean freezeHeader,
		int flushInterval,
		String headerFillColor,
		String headerFontColor
) {

	/**
	 * 緊湊建構子 (Compact Constructor)，執行預設值填補與安全防禦
	 */
	public WriteStreamSheetCommand {
		sheetName = sheetName == null || sheetName.isBlank() ? "Sheet1" : sheetName;
		headers = headers == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(headers));
		rowIterable = rowIterable != null ? rowIterable : Collections.emptyList();
		columnWidths = columnWidths == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(columnWidths));
		flushInterval = flushInterval > 0 ? flushInterval : 1000;
		headerFillColor = headerFillColor == null || headerFillColor.isBlank() ? "F2F2F2" : headerFillColor;
		headerFontColor = headerFontColor == null || headerFontColor.isBlank() ? "000000" : headerFontColor;
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

	public String getSheetName() {
		return sheetName();
	}

	public List<String> getHeaders() {
		return headers();
	}

	public Iterable<List<Object>> getRowIterable() {
		return rowIterable();
	}

	public List<Double> getColumnWidths() {
		return columnWidths();
	}

	public boolean isFreezeHeader() {
		return freezeHeader();
	}

	public int getFlushInterval() {
		return flushInterval();
	}

	public String getHeaderFillColor() {
		return headerFillColor();
	}

	public String getHeaderFontColor() {
		return headerFontColor();
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
			this.headers = headers != null ? headers : new ArrayList<>();
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
			this.columnWidths = columnWidths != null ? columnWidths : new ArrayList<>();
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
			return new WriteStreamSheetCommand(
					sheetName,
					headers,
					rowIterable,
					columnWidths,
					freezeHeader,
					flushInterval,
					headerFillColor,
					headerFontColor
			);
		}
	}
}
