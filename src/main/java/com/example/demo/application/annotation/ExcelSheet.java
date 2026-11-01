package com.example.demo.application.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Excel 工作表宣告式標註註解 (Declarative Excel Sheet Annotation)
 * <p>
 * 標註於 Java Model 實體類別或 Record 上，指定該資料模型對應之 Excel 工作表名稱與讀寫設定。
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ExcelSheet {

	/**
	 * 工作表名稱 (Sheet Name)。
	 * <p>
	 * 若未指定，預設回退至 {@link #name()}，若兩者皆為空則使用 Model 類別簡稱。
	 *
	 * @return 工作表名稱字串
	 */
	String value() default "";

	/**
	 * 工作表名稱別名 (等同於 {@link #value()})。
	 *
	 * @return 工作表名稱字串
	 */
	String name() default "";

	/**
	 * 標題列索引 (從 0 起算，預設為 0)。
	 *
	 * @return 標題列索引
	 */
	int headerRowIndex() default 0;

	/**
	 * 資料列起始索引 (從 0 起算，預設為 1)。
	 *
	 * @return 資料列起始索引
	 */
	int dataRowStartIndex() default 1;
}

