package com.example.demo.application.annotation.mapping;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Excel 欄位宣告式標註註解 (Declarative Excel Column Annotation)
 * <p>
 * <b>【架構定位與職責說明】</b><br>
 * 位於 Clean Architecture 應用層宣告式映射註解套件 ({@code application/annotation/mapping/})。<br>
 * 標註於 Java Model 欄位 (Field)、Getter 方法或 Record 元件 (Record Component) 上，
 * 定義儲存格與物件屬性之映射關係、欄位索引、日期/數字格式與預設值。
 * </p>
 *
 * */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.RECORD_COMPONENT})
public @interface ExcelColumn {

	/**
	 * 欄位標題名稱 (Header Name)。
	 * <p>
	 * 若未指定，預設回退至 {@link #name()}，若皆為空則使用屬性欄位名稱。
	 *
	 * @return 欄位標題名稱字串
	 */
	String value() default "";

	/**
	 * 欄位標題名稱別名 (等同於 {@link #value()})。
	 *
	 * @return 欄位標題名稱字串
	 */
	String name() default "";

	/**
	 * 欄位索引 (從 0 起算)。
	 * <p>
	 * <ul>
	 *   <li>匯出時：若大於等於 0，將嚴格依照此 index 進行欄位排列排序；若為 -1 (預設值)，則依類別宣告順序排列。</li>
	 *   <li>匯入時：若標題列無法完全匹配，將依據此 index 索引直接提取儲存格數值。</li>
	 * </ul>
	 *
	 * @return 欄位索引值 (預設 -1)
	 */
	int index() default -1;

	/**
	 * 日期或數值格式化樣式 (如 {@code "yyyy-MM-dd"}, {@code "yyyy-MM-dd HH:mm:ss"}, {@code "#,##0.00"})。
	 *
	 * @return 格式化樣式字串
	 */
	String format() default "";

	/**
	 * 當儲存格數值為空字串或 null 時，所套用之預設值字串。
	 *
	 * @return 預設值字串
	 */
	String defaultValue() default "";

	/**
	 * 是否忽略此欄位。
	 * <p>
	 * 若為 {@code true}，則在 Excel 匯入與匯出時皆不參與資料繫結。
	 *
	 * @return 若忽略回傳 {@code true}，否則回傳 {@code false}
	 */
	boolean ignore() default false;
}

