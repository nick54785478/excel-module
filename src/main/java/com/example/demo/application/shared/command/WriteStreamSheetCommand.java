package com.example.demo.application.shared.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.example.demo.application.shared.model.CellRange;
import com.example.demo.application.shared.model.SheetHeaderFooterConfig;
import com.example.demo.application.shared.model.SheetProtectionConfig;

/**
 * Excel 串流工作表寫入命令 (Write Stream Sheet Command)
 * <p>
 * 遵循 AGENTS.md 3-4 命名規範 ({@code V + N + Command})，採用 Java Record 作為應用層不可變純資料載體。
 * 封裝串流寫入單一工作表之可迭代資料來源 ({@link Iterable})、標題列、欄寬設定與串流沖刷間隔 (flushInterval)，
 * 並於維度五擴充支援：多階表頭合併區間 (CellRange)、動態計算公式對照 (Cell Formulas)、
 * 工作表安全防護 (Sheet Protection) 與列印頁首頁尾合規標記 (Header & Footer)。
 *
 * @param sheetName          工作表名稱 (預設 Sheet1)
 * @param headers            標題欄位清單 (不可變清單)
 * @param rowIterable        可迭代之資料列來源 (支援延遲載入以達成 O(1) 記憶體空間)
 * @param columnWidths       自訂欄寬清單 (不可變清單)
 * @param freezeHeader       是否凍結標題列首行
 * @param flushInterval      每寫入指定筆數自動 Flush 至底層串流 (預設 1000 筆)
 * @param headerFillColor    標題列背景填色 HEX 碼
 * @param headerFontColor    標題列字體顏色 HEX 碼
 * @param mergedRegions      儲存格合併區間清單 (不可變清單)
 * @param cellFormulas       動態公式對照表 (Key: 儲存格座標如 "D10", Value: 公式字串如 "SUM(D2:D9)")
 * @param protectionConfig   工作表安全防護與鎖定配置 (可為 null)
 * @param headerFooterConfig 列印頁首頁尾與機密標記配置 (可為 null)
 */
public record WriteStreamSheetCommand(
		String sheetName,
		List<String> headers,
		Iterable<List<Object>> rowIterable,
		List<Double> columnWidths,
		boolean freezeHeader,
		int flushInterval,
		String headerFillColor,
		String headerFontColor,
		List<CellRange> mergedRegions,
		Map<String, String> cellFormulas,
		SheetProtectionConfig protectionConfig,
		SheetHeaderFooterConfig headerFooterConfig
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
		mergedRegions = mergedRegions == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(mergedRegions));
		cellFormulas = cellFormulas == null ? Collections.emptyMap() : Collections.unmodifiableMap(new LinkedHashMap<>(cellFormulas));
	}

	/**
	 * 向後相容建構子：保留原有基礎 8 個參數
	 */
	public WriteStreamSheetCommand(
			String sheetName,
			List<String> headers,
			Iterable<List<Object>> rowIterable,
			List<Double> columnWidths,
			boolean freezeHeader,
			int flushInterval,
			String headerFillColor,
			String headerFontColor
	) {
		this(sheetName, headers, rowIterable, columnWidths, freezeHeader, flushInterval,
				headerFillColor, headerFontColor, Collections.emptyList(), Collections.emptyMap(), null, null);
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

	public List<CellRange> getMergedRegions() {
		return mergedRegions();
	}

	public Map<String, String> getCellFormulas() {
		return cellFormulas();
	}

	public SheetProtectionConfig getProtectionConfig() {
		return protectionConfig();
	}

	public SheetHeaderFooterConfig getHeaderFooterConfig() {
		return headerFooterConfig();
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
		private List<CellRange> mergedRegions = new ArrayList<>();
		private Map<String, String> cellFormulas = new LinkedHashMap<>();
		private SheetProtectionConfig protectionConfig;
		private SheetHeaderFooterConfig headerFooterConfig;

		public Builder from(WriteStreamSheetCommand command) {
			if (command != null) {
				this.sheetName = command.sheetName();
				this.headers = new ArrayList<>(command.headers());
				this.rowIterable = command.rowIterable();
				this.columnWidths = new ArrayList<>(command.columnWidths());
				this.freezeHeader = command.freezeHeader();
				this.flushInterval = command.flushInterval();
				this.headerFillColor = command.headerFillColor();
				this.headerFontColor = command.headerFontColor();
				this.mergedRegions = new ArrayList<>(command.mergedRegions());
				this.cellFormulas = new LinkedHashMap<>(command.cellFormulas());
				this.protectionConfig = command.protectionConfig();
				this.headerFooterConfig = command.headerFooterConfig();
			}
			return this;
		}

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

		public Builder mergedRegions(List<CellRange> mergedRegions) {
			this.mergedRegions = mergedRegions != null ? new ArrayList<>(mergedRegions) : new ArrayList<>();
			return this;
		}

		public Builder addMergedRegion(CellRange range) {
			if (range != null) {
				this.mergedRegions.add(range);
			}
			return this;
		}

		public Builder cellFormulas(Map<String, String> cellFormulas) {
			this.cellFormulas = cellFormulas != null ? new LinkedHashMap<>(cellFormulas) : new LinkedHashMap<>();
			return this;
		}

		public Builder addFormula(String cellRef, String formula) {
			if (cellRef != null && formula != null) {
				this.cellFormulas.put(cellRef.trim().toUpperCase(), formula.trim());
			}
			return this;
		}

		public Builder protectionConfig(SheetProtectionConfig protectionConfig) {
			this.protectionConfig = protectionConfig;
			return this;
		}

		public Builder headerFooterConfig(SheetHeaderFooterConfig headerFooterConfig) {
			this.headerFooterConfig = headerFooterConfig;
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
					headerFontColor,
					mergedRegions,
					cellFormulas,
					protectionConfig,
					headerFooterConfig
			);
		}
	}
}
