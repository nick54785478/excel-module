package com.example.demo.application.annotation.mapping;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Excel 工作表宣告式標註註解 (Declarative Excel Sheet Annotation)
 * <p>
 * <b>【架構定位與職責說明】</b><br>
 * 位於 Clean Architecture 應用層宣告式映射註解套件 ({@code application/annotation/mapping/})。<br>
 * 標註於 Java Model 實體類別或 Record 上，指定該資料模型對應之 Excel 工作表名稱與讀寫設定。
 * </p>
 *
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

	/**
	 * 是否啟動工作表安全防護 (預設 false)。
	 *
	 * @return 若啟動工作表防護回傳 true
	 */
	boolean protect() default false;

	/**
	 * 工作表防護解鎖密碼。
	 * <p>
	 * 若設定非空字串，將自動啟動工作表防護並鎖定。
	 *
	 * @return 防護密碼字串 (預設空字串)
	 */
	String protectPassword() default "";

	/**
	 * 列印頁首左側文字 (支援巨集代碼如 &P, &D, &T)。
	 *
	 * @return 頁首左側字串
	 */
	String leftHeader() default "";

	/**
	 * 列印頁首中間文字 (常置放報表大標題)。
	 *
	 * @return 頁首中間字串
	 */
	String centerHeader() default "";

	/**
	 * 列印頁首右側文字。
	 *
	 * @return 頁首右側字串
	 */
	String rightHeader() default "";

	/**
	 * 列印頁尾左側文字 (常置放機密等級如 "【機密資料】")。
	 *
	 * @return 頁尾左側字串
	 */
	String leftFooter() default "";

	/**
	 * 列印頁尾中間文字 (如 "第 &P 頁 / 共 &N 頁")。
	 *
	 * @return 頁尾中間字串
	 */
	String centerFooter() default "";

	/**
	 * 列印頁尾右側文字 (如 "產製時間：&D &T")。
	 *
	 * @return 頁尾右側字串
	 */
	String rightFooter() default "";
}

