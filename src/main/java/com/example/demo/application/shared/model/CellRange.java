package com.example.demo.application.shared.model;

/**
 * 儲存格合併區間純資料物件 (Cell Range Value Object)
 * <p>
 * 遵循 AGENTS.md 規範，採用 Java 21 Record 作為應用層不可變純資料載體。
 * 嚴禁依賴 Apache POI 之 CellRangeAddress 或 FastExcel Range 物件，確保領域與應用層的純粹性。
 *
 * @param firstRow 起始列索引 (0-based, 包含)
 * @param lastRow  結束列索引 (0-based, 包含)
 * @param firstCol 起始欄索引 (0-based, 包含)
 * @param lastCol  結束欄索引 (0-based, 包含)
 */
public record CellRange(int firstRow, int lastRow, int firstCol, int lastCol) {

	/**
	 * 緊湊建構子：執行座標合法性與防禦性驗證
	 */
	public CellRange {
		if (firstRow < 0 || firstCol < 0) {
			throw new IllegalArgumentException("Row and column indices must be non-negative: (" + firstRow + ", " + firstCol + ")");
		}
		if (lastRow < firstRow) {
			throw new IllegalArgumentException("lastRow (" + lastRow + ") cannot be less than firstRow (" + firstRow + ")");
		}
		if (lastCol < firstCol) {
			throw new IllegalArgumentException("lastCol (" + lastCol + ") cannot be less than firstCol (" + firstCol + ")");
		}
	}

	/**
	 * 便捷工廠方法：跨欄合併 (同一列中合併多欄)
	 *
	 * @param row      指定列索引 (0-based)
	 * @param firstCol 起始欄索引 (0-based)
	 * @param lastCol  結束欄索引 (0-based)
	 * @return 儲存格合併區間物件
	 */
	public static CellRange ofRow(int row, int firstCol, int lastCol) {
		return new CellRange(row, row, firstCol, lastCol);
	}

	/**
	 * 便捷工廠方法：跨列合併 (同一欄中合併多列)
	 *
	 * @param col      指定欄索引 (0-based)
	 * @param firstRow 起始列索引 (0-based)
	 * @param lastRow  結束列索引 (0-based)
	 * @return 儲存格合併區間物件
	 */
	public static CellRange ofColumn(int col, int firstRow, int lastRow) {
		return new CellRange(firstRow, lastRow, col, col);
	}
}

