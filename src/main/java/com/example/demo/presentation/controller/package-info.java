/**
 * 表現層控制器根套件 (Presentation Layer REST Controllers / Inbound Adapters)
 * <p>
 * 遵循六角形架構與 Clean Architecture 表現層規範 (AGENTS.md 規則 31~38)。
 * 作為 HTTP 請求對外暴露之 Inbound Adapter，接收客戶端 Resource 請求，
 * 委派 {@code assembler} 轉換為 Command/Query 調度 Application UseCase，
 * 並將處理結果透過 Assembler 封裝為 Response Resource 回傳。
 * </p>
 * <p>
 * <b>【模組子套件劃分 (Sub-module Packages)】</b>
 * <ul>
 *   <li>{@code bom}：BOM 與物料領域業務控制器 ({@link com.example.demo.presentation.controller.bom.BomController}, {@link com.example.demo.presentation.controller.bom.MaterialBomExcelController})</li>
 *   <li>{@code core}：核心通用 Excel 檔案串流與 JavaBean 宣告式映射控制器 ({@link com.example.demo.presentation.controller.core.ExcelController}, {@link com.example.demo.presentation.controller.core.ExcelBeanMappingController})</li>
 *   <li>{@code template}：JXLS 3.x 宣告式範本匯出與動態欄位映射控制器 ({@link com.example.demo.presentation.controller.template.ExcelTemplateExportController})</li>
 *   <li>{@code report}：企業級複合報表、公式計算、水印與保護控制器 ({@link com.example.demo.presentation.controller.report.ExcelReportController})</li>
 *   <li>{@code parallel}：大數據高並發平行管線與分片 ZIP 匯出控制器 ({@link com.example.demo.presentation.controller.parallel.ExcelParallelExportController})</li>
 *   <li>{@code validation}：EasyRules 宣告式規則驗證與錯誤回標控制器 ({@link com.example.demo.presentation.controller.validation.ExcelValidationController})</li>
 *   <li>{@code task}：非同步長任務進度追蹤與檔案下載控制器 ({@link com.example.demo.presentation.controller.task.ExcelTaskController})</li>
 * </ul>
 * </p>
 */
package com.example.demo.presentation.controller;
