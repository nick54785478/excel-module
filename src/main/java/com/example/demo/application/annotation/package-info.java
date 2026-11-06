/**
 * Excel 宣告式註解頂層套件 (Excel Declarative Annotations)
 * <p>
 * 依據業務職責垂直切片為三大模組子套件：
 * <ul>
 *     <li>{@code mapping}：宣告式 JavaBean / Record 試算表與欄位映射註解 ({@link com.example.demo.application.annotation.mapping.ExcelSheet}, {@link com.example.demo.application.annotation.mapping.ExcelColumn})</li>
 *     <li>{@code task}：宣告式長任務生命週期與串流監控切面註解 ({@link com.example.demo.application.annotation.task.TrackExcelTask})</li>
 *     <li>{@code validation}：宣告式 AOP 資料校驗、自動錯誤回標短路與乾淨資料注入註解 ({@link com.example.demo.application.annotation.validation.ValidateExcel}, {@link com.example.demo.application.annotation.validation.ValidatedExcelData})</li>
 * </ul>
 * </p>
 */
package com.example.demo.application.annotation;
