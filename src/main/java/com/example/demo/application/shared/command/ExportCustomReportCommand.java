package com.example.demo.application.shared.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.example.demo.application.shared.model.CellRange;

/**
 * 匯出自訂樣式與防護報表命令 (Export Custom Report Command)
 * <p>
 * 遵循 AGENTS.md 3-4 命名規範 ({@code V + N + Command})，採用 Java Record 作為應用層不可變純資料載體。
 * 封裝維度五之進階報表樣式、自訂合併儲存格區間、動態公式與工作表防護參數。
 *
 * @param sheetName       工作表名稱 (預設 "客製化報表")
 * @param headers         標題欄位清單
 * @param rows            二維資料列清單
 * @param mergedRegions   儲存格合併區間清單
 * @param cellFormulas    動態公式對照 (Key: 座標如 "D10", Value: 公式如 "SUM(D2:D9)")
 * @param password        工作表防護密碼 (可為空或 null)
 * @param reportTitle     列印頁首報表大標題
 * @param classification  列印頁尾機密等級標籤
 * @param headerFillColor 標題列背景顏色 HEX 碼 (如 "1F4E78")
 * @param headerFontColor 標題列字體顏色 HEX 碼 (如 "FFFFFF")
 */
public record ExportCustomReportCommand(
		String sheetName,
		List<String> headers,
		List<List<Object>> rows,
		List<CellRange> mergedRegions,
		Map<String, String> cellFormulas,
		String password,
		String reportTitle,
		String classification,
		String headerFillColor,
		String headerFontColor
) {

	/**
	 * 緊湊建構子：執行不可變集合防禦與預設值防禦
	 */
	public ExportCustomReportCommand {
		sheetName = sheetName == null || sheetName.isBlank() ? "客製化報表" : sheetName.trim();
		headers = headers == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(headers));
		rows = rows == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(rows));
		mergedRegions = mergedRegions == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(mergedRegions));
		cellFormulas = cellFormulas == null ? Collections.emptyMap() : Collections.unmodifiableMap(new LinkedHashMap<>(cellFormulas));
		headerFillColor = headerFillColor == null || headerFillColor.isBlank() ? "1F4E78" : headerFillColor.trim();
		headerFontColor = headerFontColor == null || headerFontColor.isBlank() ? "FFFFFF" : headerFontColor.trim();
	}

	/**
	 * 取得建造者實例
	 *
	 * @return 新建的 {@link Builder}
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * 建造者模式
	 */
	public static class Builder {
		private String sheetName = "客製化報表";
		private List<String> headers = new ArrayList<>();
		private List<List<Object>> rows = new ArrayList<>();
		private List<CellRange> mergedRegions = new ArrayList<>();
		private Map<String, String> cellFormulas = new LinkedHashMap<>();
		private String password;
		private String reportTitle;
		private String classification;
		private String headerFillColor = "1F4E78";
		private String headerFontColor = "FFFFFF";

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

		public Builder mergedRegions(List<CellRange> mergedRegions) {
			this.mergedRegions = mergedRegions != null ? mergedRegions : new ArrayList<>();
			return this;
		}

		public Builder cellFormulas(Map<String, String> cellFormulas) {
			this.cellFormulas = cellFormulas != null ? cellFormulas : new LinkedHashMap<>();
			return this;
		}

		public Builder password(String password) {
			this.password = password;
			return this;
		}

		public Builder reportTitle(String reportTitle) {
			this.reportTitle = reportTitle;
			return this;
		}

		public Builder classification(String classification) {
			this.classification = classification;
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

		public ExportCustomReportCommand build() {
			return new ExportCustomReportCommand(
					sheetName,
					headers,
					rows,
					mergedRegions,
					cellFormulas,
					password,
					reportTitle,
					classification,
					headerFillColor,
					headerFontColor
			);
		}
	}
}

