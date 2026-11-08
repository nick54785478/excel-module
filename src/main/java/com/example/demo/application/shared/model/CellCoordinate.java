package com.example.demo.application.shared.model;

/**
 * Excel 儲存格二維座標純資料物件 (Cell Coordinate Value Object)
 * <p>
 * 遵循 AGENTS.md 規範，採用 Java 21 Record 作為應用層不可變純資料載體。
 * 提供 Excel 標準儲存格文字參照代碼（如 "A1", "BC23"）與 0-based 陣列索引（row, col）之雙向轉譯。
 *
 * @param row 0-based 列索引 (例如 0 對應 Excel 第 1 列)
 * @param col 0-based 欄索引 (例如 0 對應 Excel 'A' 欄)
 */
public record CellCoordinate(int row, int col) {

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
	 * @throws IllegalArgumentException 若輸入為空或格式無法解析
	 */
	public static CellCoordinate parse(String cellReference) {
		if (cellReference == null || cellReference.isBlank()) {
			throw new IllegalArgumentException("Cell reference must not be null or blank");
		}
		String ref = cellReference.trim().toUpperCase();
		int splitIdx = 0;
		while (splitIdx < ref.length() && Character.isLetter(ref.charAt(splitIdx))) {
			splitIdx++;
		}
		if (splitIdx == 0 || splitIdx == ref.length()) {
			throw new IllegalArgumentException("Invalid Excel cell reference: " + cellReference);
		}
		String colPart = ref.substring(0, splitIdx);
		String rowPart = ref.substring(splitIdx);

		int col = 0;
		for (int i = 0; i < colPart.length(); i++) {
			col = col * 26 + (colPart.charAt(i) - 'A' + 1);
		}
		int rowIndex;
		try {
			rowIndex = Integer.parseInt(rowPart) - 1;
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid row number in cell reference: " + cellReference, e);
		}
		return new CellCoordinate(rowIndex, col - 1);
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

