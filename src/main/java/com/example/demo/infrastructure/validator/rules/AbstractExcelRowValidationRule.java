package com.example.demo.infrastructure.validator.rules;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

import com.example.demo.application.shared.model.validation.ExcelRowError;
import com.example.demo.application.shared.context.ExcelValidationContext;

/**
 * Excel 行級自訂檢核規則抽象基底類別 (Abstract Excel Row Validation Rule Base)
 * <p>
 * <b>【架構定位與職責規範】</b><br>
 * 位於 Clean Architecture 基礎設施層 ({@code infrastructure/validator/rules/})，
 * 實作 {@link ExcelRowValidationRule} 介面並無縫橋接 <b>EasyRules 規則引擎</b>。<br>
 * 封裝了 EasyRules 的 {@link Condition} 與 {@link Action} 核心反射調度機制，
 * 自動處理模型型別檢查與安全轉型，大幅降低開發者編寫樣板代碼（Boilerplate）的負擔。
 * </p>
 * <p>
 * <b>【客製擴充指南】</b><br>
 * 後續新增任何業務規則時，開發者僅需：
 * <ol>
 *   <li>繼承本基類並標註 {@code @Component} 與 {@code @Rule(name = "...", priority = ...)}</li>
 *   <li>覆寫 {@link #isInvalid(Object)}：定義何種條件視為不合法 (回傳 true)</li>
 *   <li>覆寫 {@link #createError(Object, int)}：使用內建輔助方法 {@link #errorOfCell} 建立錯誤資訊</li>
 * </ol>
 * </p>
 *
 * @param <T> 目標資料模型型別 (例如 {@code OrderModel}, {@code ProductRecordModel})
 */
@Rule(name = "AbstractExcelRowValidationRule", description = "Excel 行級客製檢核規則基礎抽象類別")
public abstract class AbstractExcelRowValidationRule<T> implements ExcelRowValidationRule<T> {

	private final Class<T> targetClass;

	/**
	 * 明確指定目標模型類別的建構子
	 *
	 * @param targetClass 目標模型 Class
	 */
	protected AbstractExcelRowValidationRule(Class<T> targetClass) {
		this.targetClass = Objects.requireNonNull(targetClass, "targetClass must not be null");
	}

	/**
	 * 自動探測泛型型別的預設建構子
	 */
	@SuppressWarnings("unchecked")
	protected AbstractExcelRowValidationRule() {
		Type superClass = getClass().getGenericSuperclass();
		if (superClass instanceof ParameterizedType parameterizedType) {
			Type[] typeArgs = parameterizedType.getActualTypeArguments();
			if (typeArgs.length > 0 && typeArgs[0] instanceof Class<?> clazz) {
				this.targetClass = (Class<T>) clazz;
				return;
			}
		}
		this.targetClass = (Class<T>) Object.class;
	}

	@Override
	public Class<T> getTargetClass() {
		return targetClass;
	}

	/**
	 * EasyRules 條件評估觸發點 (Condition Method)
	 *
	 * @param row      當前資料列物件 (來自 EasyRules Facts)
	 * @param rowIndex 資料列索引 (1-based)
	 * @param context  檢核執行上下文
	 * @return {@code true} 代表規則觸發 (資料不符合業務要求，需生成錯誤)
	 */
	@Condition
	@SuppressWarnings("unchecked")
	public boolean when(
			@Fact("row") Object row,
			@Fact("rowIndex") int rowIndex,
			@Fact("context") ExcelValidationContext<?> context) {
		if (row != null && targetClass.isInstance(row)) {
			return isInvalid((T) row, context, rowIndex);
		}
		return false;
	}

	/**
	 * EasyRules 動作執行觸發點 (Action Method)
	 *
	 * @param row      當前資料列物件
	 * @param rowIndex 資料列索引 (1-based)
	 * @param context  檢核執行上下文
	 * @param errors   全量錯誤累積容器
	 */
	@Action
	@SuppressWarnings("unchecked")
	public void then(
			@Fact("row") Object row,
			@Fact("rowIndex") int rowIndex,
			@Fact("context") ExcelValidationContext<?> context,
			@Fact("errors") List<ExcelRowError> errors) {
		if (row != null && targetClass.isInstance(row) && errors != null) {
			List<ExcelRowError> generatedErrors = createErrors((T) row, context, rowIndex);
			if (generatedErrors != null && !generatedErrors.isEmpty()) {
				errors.addAll(generatedErrors);
			}
		}
	}

	// ==================== 多層次業務檢核覆寫掛鉤 (Template Methods) ====================

	/**
	 * 完整檢核掛鉤 (含上下文與列號)
	 *
	 * @param item     強型別模型實體
	 * @param context  檢核執行上下文
	 * @param rowIndex 當前資料列索引 (1-based)
	 * @return 是否不合格
	 */
	@Override
	public boolean isInvalid(T item, ExcelValidationContext<?> context, int rowIndex) {
		return isInvalid(item, rowIndex);
	}

	/**
	 * 簡化檢核掛鉤 (含資料列號)
	 *
	 * @param item     強型別模型實體
	 * @param rowIndex 當前資料列索引 (1-based)
	 * @return 是否不合格
	 */
	public boolean isInvalid(T item, int rowIndex) {
		return isInvalid(item);
	}

	/**
	 * 最簡檢核掛鉤 (僅需檢核資料實體本身)
	 *
	 * @param item 強型別模型實體
	 * @return 是否不合格 (預設為 false)
	 */
	public boolean isInvalid(T item) {
		return false;
	}

	/**
	 * 完整錯誤事實生成掛鉤 (含上下文與列號)
	 *
	 * @param item     強型別模型實體
	 * @param context  檢核執行上下文
	 * @param rowIndex 當前資料列索引 (1-based)
	 * @return 單項錯誤事實物件
	 */
	@Override
	public ExcelRowError createError(T item, ExcelValidationContext<?> context, int rowIndex) {
		return createError(item, rowIndex);
	}

	/**
	 * 簡化錯誤事實生成掛鉤 (含資料列號)
	 *
	 * @param item     強型別模型實體
	 * @param rowIndex 當前資料列索引 (1-based)
	 * @return 單項錯誤事實物件
	 */
	public ExcelRowError createError(T item, int rowIndex) {
		return null;
	}

	/**
	 * 多項錯誤事實生成擴充點 (適用於單一規則需產出多筆欄位錯誤的情境)
	 *
	 * @param item     強型別模型實體
	 * @param context  檢核執行上下文
	 * @param rowIndex 當前資料列索引 (1-based)
	 * @return 錯誤事實清單
	 */
	public List<ExcelRowError> createErrors(T item, ExcelValidationContext<?> context, int rowIndex) {
		ExcelRowError single = createError(item, context, rowIndex);
		return single != null ? List.of(single) : Collections.emptyList();
	}

	// ==================== 便捷建立錯誤事實輔助方法 (Helpers) ====================

	/**
	 * 便捷建立儲存格欄位錯誤
	 *
	 * @param rowIndex     發生錯誤列號 (1-based)
	 * @param columnIndex  發生錯誤欄位索引 (0-based)
	 * @param columnName   欄位名稱 (例如 "單價")
	 * @param invalidValue 使用者輸入之原始無效值
	 * @param errorMessage 詳細檢核錯誤原因
	 * @param ruleCode     規則代碼
	 * @return 錯誤事實快照物件
	 */
	protected ExcelRowError errorOfCell(int rowIndex, int columnIndex, String columnName,
	                                    Object invalidValue, String errorMessage, String ruleCode) {
		return ExcelRowError.ofCell(rowIndex, columnIndex, columnName, invalidValue, errorMessage, ruleCode);
	}

	/**
	 * 便捷建立整列層級錯誤
	 *
	 * @param rowIndex     發生錯誤列號 (1-based)
	 * @param errorMessage 詳細檢核錯誤原因
	 * @param ruleCode     規則代碼
	 * @return 錯誤事實快照物件
	 */
	protected ExcelRowError errorOfRow(int rowIndex, String errorMessage, String ruleCode) {
		return ExcelRowError.ofRow(rowIndex, errorMessage, ruleCode);
	}
}

