package com.example.demo.application.shared.vo;

/**
 * 工作表安全防護與鎖定配置 (Sheet Protection Configuration)
 * <p>
 * 遵循 AGENTS.md 規範，採用 Java 21 Record 作為應用層不可變純資料載體。
 * 封裝工作表防護密碼與使用者的操作權限開關（排序、篩選、選取鎖定儲存格等）。
 *
 * @param password                 解鎖密碼 (為空字串或 null 表示不設密碼但啟動保護)
 * @param allowSorting             是否允許使用者進行資料排序 (預設 true)
 * @param allowFiltering           是否允許使用者使用自動篩選 (AutoFilter, 預設 true)
 * @param allowSelectLockedCells   是否允許使用者選取已鎖定的儲存格 (預設 true)
 * @param allowSelectUnlockedCells 是否允許使用者選取未鎖定的儲存格 (預設 true)
 */
public record SheetProtectionConfig(
		String password,
		boolean allowSorting,
		boolean allowFiltering,
		boolean allowSelectLockedCells,
		boolean allowSelectUnlockedCells
) {

	/**
	 * 便捷工廠方法：透過指定密碼建立標準保護設定 (允許排序與自動篩選)
	 *
	 * @param password 解鎖密碼
	 * @return 工作表防護設定實例
	 */
	public static SheetProtectionConfig ofPassword(String password) {
		return new SheetProtectionConfig(password, true, true, true, true);
	}

	/**
	 * 便捷工廠方法：建立完全唯讀保護設定 (禁止排序與篩選)
	 *
	 * @param password 解鎖密碼
	 * @return 完全唯讀工作表防護設定實例
	 */
	public static SheetProtectionConfig readOnly(String password) {
		return new SheetProtectionConfig(password, false, false, true, true);
	}
}

