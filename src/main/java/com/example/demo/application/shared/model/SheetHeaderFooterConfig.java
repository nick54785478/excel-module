package com.example.demo.application.shared.model;

/**
 * 試算表列印頁首頁尾與合規中繼設定 (Sheet Header & Footer Configuration)
 * <p>
 * 遵循 AGENTS.md 規範，採用 Java 21 Record 作為應用層不可變純資料載體。
 * 支援 Excel 內建巨集標籤：
 * <ul>
 *   <li>{@code &P}：當前頁碼 (Current Page Number)</li>
 *   <li>{@code &N}：總頁數 (Total Page Count)</li>
 *   <li>{@code &D}：目前日期 (Current Date)</li>
 *   <li>{@code &T}：目前時間 (Current Time)</li>
 *   <li>{@code &F}：活頁簿檔案名稱 (File Name)</li>
 *   <li>{@code &A}：工作表名稱 (Sheet Tab Name)</li>
 * </ul>
 *
 * @param leftHeader   頁首左側文字
 * @param centerHeader 頁首中間文字 (常置放報表大標題)
 * @param rightHeader  頁首右側文字
 * @param leftFooter   頁尾左側文字 (常置放「機密等級」)
 * @param centerFooter 頁尾中間文字 (常置放「第 &P 頁 / 共 &N 頁」)
 * @param rightFooter  頁尾右側文字 (常置放「製表時間：&D &T」)
 */
public record SheetHeaderFooterConfig(
		String leftHeader,
		String centerHeader,
		String rightHeader,
		String leftFooter,
		String centerFooter,
		String rightFooter
) {

	/**
	 * 便捷工廠方法：建立企業標準機密審計頁首頁尾範本
	 *
	 * @param reportTitle    報表大標題
	 * @param classification 機密等級 (例如 "機密資料・內部流轉・嚴禁外流")
	 * @return 頁首頁尾設定物件
	 */
	public static SheetHeaderFooterConfig enterpriseStandard(String reportTitle, String classification) {
		return new SheetHeaderFooterConfig(
				"系統產製",
				reportTitle,
				null,
				classification != null && !classification.isBlank() ? "【" + classification + "】" : "【機密資料】",
				"第 &P 頁 / 共 &N 頁",
				"產製時間：&D &T"
		);
	}
}

