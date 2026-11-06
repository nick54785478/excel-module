package com.example.demo.application.annotation.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Excel 宣告式資料檢核與上傳攔截註解 (Validate Excel Annotation)
 * <p>
 * <b>【架構定位與職責說明】</b><br>
 * 位於 Clean Architecture 應用層資料校驗註解套件 ({@code application/annotation/validation/})，遵循六角形架構規範。<br>
 * 可標註於表現層 Controller 或應用層 Service 的批次上傳方法上。啟用後由基礎設施層切面
 * {@code com.example.demo.infrastructure.aop.ExcelValidationAspect} 進行前置攔截，
 * 自動調度多層次規則管線 (含 EasyRules 規則引擎) 進行非短路全量錯誤累積檢核。
 * </p>
 * <p>
 * <b>【核心特性】</b>
 * <ul>
 *     <li><b>自動錯誤回標短路 (Auto Feedback Short-Circuit)</b>：若資料存在違規，可直接阻斷方法執行，產製並下載淡紅底深紅字錯誤回標檔。</li>
 *     <li><b>全域例外轉譯 (Exception-Driven Mode)</b>：檢核失敗時可拋出業務例外，由全域例外處理器轉譯為 HTTP 422 JSON 結構。</li>
 *     <li><b>乾淨資料自動注入 (Clean Data Injection)</b>：檢核通過後，自動將已反序列化之資料清單注入標註 {@link ValidatedExcelData} 之入參。</li>
 * </ul>
 * </p>
 *
 * @see ValidatedExcelData
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface ValidateExcel {

	/**
	 * 目標資料模型類別 (必填)
	 * <p>
	 * 支援標準 JavaBean (如 {@code OrderModel.class}) 或 Java 21 Record (如 {@code ProductRecordModel.class})。
	 * </p>
	 *
	 * @return 目標模型 Class 物件
	 */
	Class<?> model();

	/**
	 * 檢核違規時之攔截處理策略 (預設為自動產製並下載錯誤回標 Excel)
	 *
	 * @return 違規處理動作列舉
	 */
	OnErrorAction action() default OnErrorAction.AUTO_FEEDBACK;

	/**
	 * 錯誤回標 Excel 檔案下載之預設檔名 (僅在 {@link OnErrorAction#AUTO_FEEDBACK} 策略下生效)
	 *
	 * @return 回標檔案名稱 (需帶 .xlsx 副檔名)
	 */
	String feedbackFilename() default "檢核錯誤回標檔.xlsx";

	/**
	 * 是否將檢核通過之資料綁定至執行緒上下文 {@code ExcelValidationContextHolder}
	 *
	 * @return 是否啟用執行緒上下文綁定 (預設 true)
	 */
	boolean bindValidData() default true;

	/**
	 * 檢核違規時之處置動作列舉
	 */
	enum OnErrorAction {
		/**
		 * 自動短路阻斷：直接回傳帶有淡紅底深紅字高亮標註與錯誤原因說明之 Excel 檔案二進位下載
		 */
		AUTO_FEEDBACK,

		/**
		 * 拋出例外：拋出 {@code ExcelValidationFailedException}，由全域例外處理器轉為 HTTP 422 JSON
		 */
		THROW_EXCEPTION
	}
}

