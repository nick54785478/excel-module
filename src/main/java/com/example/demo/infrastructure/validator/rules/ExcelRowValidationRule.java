package com.example.demo.infrastructure.validator.rules;

import com.example.demo.application.shared.model.validation.ExcelRowError;
import com.example.demo.application.shared.context.ExcelValidationContext;

/**
 * Excel 行級自訂檢核規則合約介面 (Excel Row Validation Rule Contract)
 * <p>
 * <b>【架構定位與職責規範】</b><br>
 * 位於 Clean Architecture 基礎設施層驗證規則套件 ({@code infrastructure/validator/rules/})，
 * 供開發者擴充自訂業務規則，定義對特定 Excel 資料模型（JavaBean 或 Java 21 Record）的違規評估與錯誤事實生成契約。
 * </p>
 *
 * @param <T> 目標資料模型型別 (例如 {@code OrderModel}, {@code ProductRecordModel})
 */
public interface ExcelRowValidationRule<T> {

	/**
	 * 取得本規則所支援之目標資料模型 Class
	 *
	 * @return 目標模型類別 Class
	 */
	Class<T> getTargetClass();

	/**
	 * 評估目標資料列是否違反業務檢核條件
	 *
	 * @param item     強型別模型資料實體 (不得為 null)
	 * @param context  檢核上下文 (包含全表快照與共享屬性)
	 * @param rowIndex 目前資料列索引 (1-based，對齊 Excel 介面列號)
	 * @return {@code true} 代表資料違規不合格 (觸發錯誤生成)；{@code false} 代表合格
	 */
	boolean isInvalid(T item, ExcelValidationContext<?> context, int rowIndex);

	/**
	 * 當資料違規時，建立對應之詳細錯誤事實物件
	 *
	 * @param item     強型別模型資料實體 (不得為 null)
	 * @param context  檢核上下文
	 * @param rowIndex 目前資料列索引 (1-based)
	 * @return 錯誤事實快照物件 {@link ExcelRowError}
	 */
	ExcelRowError createError(T item, ExcelValidationContext<?> context, int rowIndex);
}

