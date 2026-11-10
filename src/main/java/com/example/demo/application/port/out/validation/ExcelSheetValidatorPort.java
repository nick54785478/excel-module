package com.example.demo.application.port.out.validation;

import java.util.List;

import com.example.demo.application.shared.model.ExcelRowError;
import com.example.demo.application.shared.model.ExcelValidationContext;

/**
 * Excel 整表級別約束驗證輸出埠 (Excel Sheet Validator Port)
 * <p>
 * <b>【架構定位與職責規範】</b><br>
 * 位於 Clean Architecture 應用層 Outbound Port 套件 ({@code application/port/out/})，
 * 遵循 {@code AGENTS.md} 規則 18 與 3-8 (以 {@code Port} 結尾)。<br>
 * 專門負責跨列唯一性防重複 (如特定單號或物料編號不可重複)、表頭表身加總平衡等集合層級業務約束。
 * 傳參及回傳型別嚴格使用純 Java 領域與值物件。
 * </p>
 *
 * @param <T> 目標資料模型型別
 */
public interface ExcelSheetValidatorPort<T> {

	/**
	 * 針對整張工作表執行跨列或集合層級檢核
	 *
	 * @param context 包含整表快照與共享屬性之檢核上下文 (不得為 null)
	 * @return 檢核錯誤清單 (若完全合格則回傳空清單，嚴禁回傳 null)
	 */
	List<ExcelRowError> validateSheet(ExcelValidationContext<T> context);

	/**
	 * 判斷本整表驗證器是否支援特定目標模型類別 (預設為支援)
	 *
	 * @param targetClass 目標模型 Class
	 * @return {@code true} 代表支援該型別
	 */
	default boolean supports(Class<?> targetClass) {
		return true;
	}
}

