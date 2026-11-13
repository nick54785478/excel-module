package com.example.demo.application.shared.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Excel 儲存格二維座標純資料物件 (Cell Coordinate Value Object)
 * <p>
 * 遵循 AGENTS.md 規範，採用 Java 21 Record 作為應用層不可變純資料載體。
 * 提供 Excel 標準儲存格文字參照代碼（如 "A1", "BC23"）與 0-based 陣列索引（row, col）之雙向轉譯。
 * 遵循 OpenXML 規格：欄代碼為 1~3 位英文字母 (最高支援至 XFD，共 16,384 欄)，列號為 1~1,048,576。
 *
 * @param row 0-based 列索引 (例如 0 對應 Excel 第 1 列)
 * @param col 0-based 欄索引 (例如 0 對應 Excel 'A' 欄)
 */
public record CellCoordinate(int row, int col) {

	/**
	 * 標準 Excel 儲存格參照格式正規表示式：1~3 位 ASCII 英文字母 + 1 位以上數字 (不允許前導 0)
	 */
	private static final Pattern CELL_REF_PATTERN = Pattern.compile("^([A-Za-z]{1,3})([1-9][0-9]{0,6})$");

	/**
	 * OpenXML (.xlsx) 最大欄數 (對應欄標籤 XFD)
	 */
	public static final int MAX_EXCEL_COLUMNS = 16384;

	/**
	 * OpenXML (.xlsx) 最大列數
	 */
	public static final int MAX_EXCEL_ROWS = 1048576;

	/**
	 * 緊湊建構子：座標合法性防禦性檢驗
	 */
	public CellCoordinate {
		if (row < 0 || col < 0) {
			throw new IllegalArgumentException("Row and column indices must be non-negative: (" + row + ", " + col + ")");
		}
	}

	/**
	 * 便捷靜態建構工廠方法
	 *
	 * @param row 0-based 列索引
	 * @param col 0-based 欄索引
	 * @return 儲存格座標物件
	 */
	public static CellCoordinate of(int row, int col) {
		return new CellCoordinate(row, col);
	}

	/**
	 * 解析 Excel 標準參照字串代碼 (如 "A1", "D15", "AA100") 為座標物件
	 *
	 * @param cellReference 儲存格參照字串 (不區分大小寫)
	 * @return 解析後之 {@link CellCoordinate} 物件
	 * @throws IllegalArgumentException 若輸入為空、格式無法解析或超出 Excel 範圍上限 (XFD / 1048576)
	 */
	public static CellCoordinate parse(String cellReference) {
		if (cellReference == null || cellReference.isBlank()) {
			throw new IllegalArgumentException("Cell reference must not be null or blank");
		}
		String ref = cellReference.trim();
		Matcher matcher = CELL_REF_PATTERN.matcher(ref);
		if (!matcher.matches()) {
			throw new IllegalArgumentException("Invalid Excel cell reference: '" + cellReference + "'. Expected format: [A-Z]{1,3}[1-9][0-9]* (e.g. 'A1', 'D10', max 'XFD1048576')");
		}

		String colPart = matcher.group(1).toUpperCase();
		String rowPart = matcher.group(2);

		int col = 0;
		for (int i = 0; i < colPart.length(); i++) {
			col = col * 26 + (colPart.charAt(i) - 'A' + 1);
		}
		if (col > MAX_EXCEL_COLUMNS) {
			throw new IllegalArgumentException("Column index exceeds Excel maximum limit ('XFD' / 16384): " + cellReference);
		}

		long rowIndexLong;
		try {
			rowIndexLong = Long.parseLong(rowPart) - 1;
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid row number in cell reference: " + cellReference, e);
		}
		if (rowIndexLong >= MAX_EXCEL_ROWS) {
			throw new IllegalArgumentException("Row index exceeds Excel maximum limit (1048576): " + cellReference);
		}

		return new CellCoordinate((int) rowIndexLong, col - 1);
	}

	/**
	 * 檢驗指定字串是否為合法的 Excel 儲存格參照代碼 (如 "A1", "D4", "XFD1048576")
	 *
	 * @param cellReference 欲檢驗之參照字串
	 * @return 若為合法 Excel 座標則回傳 true，否則回傳 false
	 */
	public static boolean isValid(String cellReference) {
		if (cellReference == null || cellReference.isBlank()) {
			return false;
		}
		String ref = cellReference.trim();
		Matcher matcher = CELL_REF_PATTERN.matcher(ref);
		if (!matcher.matches()) {
			return false;
		}

		String colPart = matcher.group(1).toUpperCase();
		int col = 0;
		for (int i = 0; i < colPart.length(); i++) {
			col = col * 26 + (colPart.charAt(i) - 'A' + 1);
		}
		if (col > MAX_EXCEL_COLUMNS) {
			return false;
		}

		try {
			long rowIndexLong = Long.parseLong(matcher.group(2)) - 1;
			return rowIndexLong < MAX_EXCEL_ROWS;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	/**
	 * 轉為標準 Excel 參照文字 (例如 row=0, col=0 $\to$ "A1", row=14, col=3 $\to$ "D15")
	 *
	 * @return Excel 參照字串
	 */
	public String toCellReference() {
		return format(col, row);
	}

	/**
	 * 依據欄列索引格式化為 Excel 參照文字
	 *
	 * @param col 0-based 欄索引
	 * @param row 0-based 列索引
	 * @return Excel 參照文字 (如 "C2")
	 */
	public static String format(int col, int row) {
		StringBuilder sb = new StringBuilder();
		int tempCol = col + 1;
		while (tempCol > 0) {
			int rem = (tempCol - 1) % 26;
			sb.insert(0, (char) ('A' + rem));
			tempCol = (tempCol - 1) / 26;
		}
		sb.append(row + 1);
		return sb.toString();
	}
}
