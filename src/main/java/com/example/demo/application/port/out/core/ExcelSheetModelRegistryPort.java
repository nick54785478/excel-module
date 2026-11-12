package com.example.demo.application.port.out.core;

import java.util.List;
import java.util.Optional;

/**
 * 宣告式 Excel 模型註冊與工作表名稱路由查詢輸出埠 (Outbound Port)
 * <p>
 * <b>【架構定位與職責規範】</b><br>
 * 位於 Clean Architecture 應用層 ({@code application/port/out/core/})。<br>
 * 遵循 {@code AGENTS.md} 規範：
 * <ul>
 *   <li>命名規範：以 {@code Port} 結尾 (3-8 規範)。</li>
 *   <li>方法參數與回傳型別嚴禁使用任何技術類或環境相依物件 (規則 18)，僅使用 Java 原生型別 ({@link String}, {@link Class}, {@link Optional}, {@link List})。</li>
 * </ul>
 * 負責提供外部表現層 (Controller) 或應用層依據 Excel 工作表名稱 (SheetName) 或別名 (Alias)
 * 反查對應之宣告式 Java 模型類別 ({@code Class<?>})，徹底避免表現層硬編碼字串比對與 if-else 分支。
 * </p>
 */
public interface ExcelSheetModelRegistryPort {

	/**
	 * 依據工作表名稱或別名查找對應的 Excel 宣告式資料模型 Class
	 *
	 * @param sheetName 欲查找之工作表名稱或別名 (不區分大小寫、自動去除前後空白)
	 * @return 匹配之模型類別 {@link Optional}；若查無對應模型則回傳 empty
	 */
	Optional<Class<?>> findModelClass(String sheetName);

	/**
	 * 依據工作表名稱或別名查找對應的 Excel 宣告式資料模型 Class；若查無則拋出例外
	 *
	 * @param sheetName 欲查找之工作表名稱或別名
	 * @return 匹配之模型類別
	 * @throws IllegalArgumentException 當找不到對應之已註冊模型時拋出
	 */
	default Class<?> getRequiredModelClass(String sheetName) {
		return findModelClass(sheetName).orElseThrow(() -> new IllegalArgumentException(
				"未支援或未註冊之 Excel 工作表名稱: [" + sheetName + "]。請確認該工作表名稱或 @ExcelSheet 別名是否已正確配置。"
		));
	}

	/**
	 * 取得系統目前已註冊之所有 Excel 宣告式資料模型 Class 清單 (去除重複)
	 *
	 * @return 已註冊之模型 Class 唯讀清單
	 */
	List<Class<?>> getAllModelClasses();

	/**
	 * 檢查指定之工作表名稱或別名是否已被系統支援與註冊
	 *
	 * @param sheetName 欲檢查之工作表名稱或別名
	 * @return {@code true} 若存在匹配之模型；否則 {@code false}
	 */
	default boolean supports(String sheetName) {
		return findModelClass(sheetName).isPresent();
	}
}

