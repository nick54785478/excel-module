package com.example.demo.application.shared.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Excel 工作表寫入命令 (Write Sheet Command)
 * <p>
 * 遵循 AGENTS.md 3-4 命名規範 ({@code V + N + Command})，採用 Java Record 作為應用層不可變純資料載體。
 * 封裝單一工作表之名稱、標題欄位、資料列、欄寬設定與標題列凍結/顏色樣式。
 * 作為純資料載體傳遞至 {@link com.example.demo.application.port.out.ExcelWriterPort}。
 *
 * @param sheetName       工作表名稱 (預設 Sheet1)
 * @param headers         標題欄位清單 (不可變清單)
 * @param rows            二維資料列清單 (不可變清單)
 * @param columnWidths    自訂欄寬清單 (不可變清單)
 * @param freezeHeader    是否凍結標題列首行
 * @param headerFillColor 標題列背景填色 HEX 碼 (如 "F2F2F2")
 * @param headerFontColor 標題列字體顏色 HEX 碼 (如 "000000")
 */
public record WriteSheetCommand(
		String sheetName,
		List<String> headers,
		List<List<Object>> rows,
		List<Double> columnWidths,
		boolean freezeHeader,
		String headerFillColor,
		String headerFontColor
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

		public WriteSheetCommand build() {
			return new WriteSheetCommand(
					sheetName,
					headers,
					rows,
					columnWidths,
					freezeHeader,
					headerFillColor,
					headerFontColor
			);
		}
	}
}
