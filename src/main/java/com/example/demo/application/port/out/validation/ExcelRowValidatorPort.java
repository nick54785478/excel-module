package com.example.demo.application.port.out.validation;

import java.util.List;

import com.example.demo.application.shared.model.ExcelRowError;
import com.example.demo.application.shared.model.ExcelValidationContext;

/**
 * Excel 資料列級別客製驗證輸出埠 (Excel Row Validator Port)
 * <p>
 * <b>【架構定位與職責規範】</b><br>
 * 位於 Clean Architecture 應用層 Outbound Port 套件 ({@code application/port/out/})，
 * 遵循 {@code AGENTS.md} 規則 18 與 3-8 (以 {@code Port} 結尾)。<br>
 * 定義應用層對外要求基礎設施層 (或各業務規則模組) 提供的行級客製驗證合約 (SPI)，
 * 隔離底層規則引擎 (如 EasyRules、Jakarta Validation) 與業務邏輯，
 * 傳參及回傳型別嚴禁使用任何技術類或環境相依物件。
 * </p>
 *
 * @param <T> 目標資料模型型別
 */
public interface ExcelRowValidatorPort<T> {

	/**
	 * 針對單一資料列執行客製業務規則檢核
	 *
	 * @param context  包含整表快照與共享屬性之檢核上下文 (不得為 null)
	 * @param rowIndex 目前資料列索引 (1-based，對齊 Excel 介面顯示列號)
	 * @param item     反序列化後之目標資料實體物件 (不得為 null)
	 * @return 檢核錯誤清單 (若完全合格則回傳空清單，嚴禁回傳 null)
	 */
	List<ExcelRowError> validateRow(ExcelValidationContext<T> context, int rowIndex, T item);

	/**
	 * 判斷本驗證器是否支援特定目標模型類別 (預設為支援)
	 *
	 * @param targetClass 目標模型 Class
	 * @return {@code true} 代表支援該型別
	 */
	default boolean supports(Class<?> targetClass) {
		return true;
	}
}

