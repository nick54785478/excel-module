package com.example.demo.application.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.example.demo.application.shared.vo.ExcelTaskType;

/**
 * Excel 長任務監控與追蹤宣告式註解 (Track Excel Task Annotation)
 * <p>
 * 可標註於 Inbound Adapter (如 Application Service / Controller) 或 Outbound Adapter 之業務方法上。
 * 啟用宣告式任務生命週期監控，AOP 切面將自動完成：
 * <ul>
 *     <li>任務初始實體建立 (PENDING) 與開始執行標記 (PROCESSING)</li>
 *     <li>將 taskId 綁定至執行緒上下文 {@link com.example.demo.application.shared.context.ExcelTaskContextHolder}</li>
 *     <li>串流讀寫時自動回報進度與計算百分比</li>
 *     <li>方法正常返回時自動標記 COMPLETED 與產出下載識別</li>
 *     <li>方法拋出異常時自動標記 FAILED 與記錄錯誤摘要</li>
 * </ul>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface TrackExcelTask {

	/**
	 * 任務類型：匯入 (IMPORT) 或 匯出 (EXPORT)，預設為 EXPORT
	 *
	 * @return 任務類型
	 */
	ExcelTaskType type() default ExcelTaskType.EXPORT;

	/**
	 * 目標檔案名稱，支援靜態字串或 Spring EL 動態表達式 (如 "#command.fileName" 或 "#fileName")。
	 *
	 * @return 檔案名稱表達式
	 */
	String fileName() default "";

	/**
	 * 預估總列數，支援靜態數值或 Spring EL 動態表達式 (如 "#command.totalRows")，未知則預設為 "0"。
	 *
	 * @return 總列數表達式
	 */
	String totalRows() default "0";

	/**
	 * 是否於獨立背景工作緒中非同步執行 (預設 false)
	 *
	 * @return 是否非同步執行
	 */
	boolean async() default false;

	/**
	 * 任務說明或業務備註 (可選)
	 *
	 * @return 說明文字
	 */
	String description() default "";
}

