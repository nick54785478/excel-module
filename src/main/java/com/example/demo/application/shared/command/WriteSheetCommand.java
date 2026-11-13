package com.example.demo.application.shared.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.example.demo.application.shared.vo.CellRange;
import com.example.demo.application.shared.vo.SheetHeaderFooterConfig;
import com.example.demo.application.shared.vo.SheetProtectionConfig;

/**
 * Excel 工作表寫入命令 (Write Sheet Command)
 * <p>
 * 遵循 AGENTS.md 3-4 命名規範 ({@code V + N + Command})，採用 Java Record 作為應用層不可變純資料載體。
 * 封裝單一工作表之名稱、標題欄位、資料列、欄寬設定與標題列凍結/顏色樣式，
 * 並於維度五擴充支援：多階表頭合併區間 (CellRange)、動態計算公式對照 (Cell Formulas)、
 * 工作表安全防護 (Sheet Protection) 與列印頁首頁尾合規標記 (Header & Footer)。
 * 作為純資料載體傳遞至 {@link com.example.demo.application.port.out.ExcelWriterPort}。
 *
 * @param sheetName          工作表名稱 (預設 Sheet1)
 * @param headers            標題欄位清單 (不可變清單)
 * @param rows               二維資料列清單 (不可變清單)
 * @param columnWidths       自訂欄寬清單 (不可變清單)
 * @param freezeHeader       是否凍結標題列首行
 * @param headerFillColor    標題列背景填色 HEX 碼 (如 "F2F2F2")
 * @param headerFontColor    標題列字體顏色 HEX 碼 (如 "000000")
 * @param mergedRegions      儲存格合併區間清單 (不可變清單)
 * @param cellFormulas       動態公式對照表 (Key: 儲存格座標如 "D10", Value: 公式字串如 "SUM(D2:D9)")
 * @param protectionConfig   工作表安全防護與鎖定配置 (可為 null)
 * @param headerFooterConfig 列印頁首頁尾與機密標記配置 (可為 null)
 */
public record WriteSheetCommand(
		String sheetName,
		List<String> headers,
		List<List<Object>> rows,
		List<Double> columnWidths,
		boolean freezeHeader,
		String headerFillColor,
		String headerFontColor,
		List<CellRange> mergedRegions,
		Map<String, String> cellFormulas,
		SheetProtectionConfig protectionConfig,
		SheetHeaderFooterConfig headerFooterConfig
) {

	/**
	 * 緊湊建構子 (Compact Constructor)，執行預設值填補與集合不可變安全防禦
	 */
	public WriteSheetCommand {
		sheetName = sheetName == null || sheetName.isBlank() ? "Sheet1" : sheetName;
		headers = headers == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(headers));
		rows = rows == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(rows));
		columnWidths = columnWidths == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(columnWidths));
		headerFillColor = headerFillColor == null || headerFillColor.isBlank() ? "F2F2F2" : headerFillColor;
		headerFontColor = headerFontColor == null || headerFontColor.isBlank() ? "000000" : headerFontColor;
		mergedRegions = mergedRegions == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(mergedRegions));
		cellFormulas = cellFormulas == null ? Collections.emptyMap() : Collections.unmodifiableMap(new LinkedHashMap<>(cellFormulas));
	}

	/**
	 * 向後相容建構子：保留原有基礎 7 個參數，其餘進階屬性預設為空
	 */
	public WriteSheetCommand(
			String sheetName,
			List<String> headers,
			List<List<Object>> rows,
			List<Double> columnWidths,
			boolean freezeHeader,
			String headerFillColor,
			String headerFontColor
	) {
		this(sheetName, headers, rows, columnWidths, freezeHeader, headerFillColor, headerFontColor,
				Collections.emptyList(), Collections.emptyMap(), null, null);
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

	public List<List<Object>> getRows() {
		return rows();
	}

	public List<Double> getColumnWidths() {
		return columnWidths();
	}

	public boolean isFreezeHeader() {
		return freezeHeader();
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
		private List<List<Object>> rows = new ArrayList<>();
		private List<Double> columnWidths = new ArrayList<>();
		private boolean freezeHeader = true;
		private String headerFillColor = "F2F2F2";
		private String headerFontColor = "000000";
		private List<CellRange> mergedRegions = new ArrayList<>();
		private Map<String, String> cellFormulas = new LinkedHashMap<>();
		private SheetProtectionConfig protectionConfig;
		private SheetHeaderFooterConfig headerFooterConfig;

		public Builder from(WriteSheetCommand command) {
			if (command != null) {
				this.sheetName = command.sheetName();
				this.headers = new ArrayList<>(command.headers());
				this.rows = new ArrayList<>(command.rows());
				this.columnWidths = new ArrayList<>(command.columnWidths());
				this.freezeHeader = command.freezeHeader();
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

		public Builder rows(List<List<Object>> rows) {
			this.rows = rows != null ? rows : new ArrayList<>();
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

		public WriteSheetCommand build() {
			return new WriteSheetCommand(
					sheetName,
					headers,
					rows,
					columnWidths,
					freezeHeader,
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
