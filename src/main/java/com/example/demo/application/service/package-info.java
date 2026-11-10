/**
 * 應用層服務根套件 (Application Services / UseCase Implementations)
 * <p>
 * 遵循六角形架構與 Clean Architecture 規範 (AGENTS.md 規則 25 與 56)。
 * 實作 {@code port/in/} 之 UseCase 業務合約介面，協調領域模型（Domain Model）、
 * 聚合根（Aggregate Root）與外部基礎設施輸出埠（{@code port/out/}）。
 * </p>
 * <p>
 * <b>【模組子套件劃分 (Sub-module Packages)】</b>
 * <ul>
 *   <li>{@code bom}：物料與產品結構清單業務命令/查詢服務 ({@link com.example.demo.application.service.bom.BomCommandService}, {@link com.example.demo.application.service.bom.BomQueryService})</li>
 *   <li>{@code core}：核心通用 Excel 檔案匯出入處理服務 ({@link com.example.demo.application.service.core.ExcelCommandService}, {@link com.example.demo.application.service.core.ExcelQueryService})</li>
 *   <li>{@code template}：JXLS 3.x 宣告式範本渲染與動態映射應用服務 ({@link com.example.demo.application.service.template.ExcelTemplateCommandService})</li>
 *   <li>{@code report}：企業級複合裝飾報表應用服務 ({@link com.example.demo.application.service.report.ExcelReportApplicationService})</li>
 *   <li>{@code parallel}：大數據高並發平行加速管線與分片 ZIP 匯出應用服務 ({@link com.example.demo.application.service.parallel.ExcelParallelCommandService})</li>
 *   <li>{@code validation}：EasyRules 宣告式業務規則檢核與錯誤回標應用服務 ({@link com.example.demo.application.service.validation.ExcelValidationApplicationService})</li>
 *   <li>{@code task}：非同步長任務命令調度與進度查詢服務 ({@link com.example.demo.application.service.task.ExcelTaskCommandService}, {@link com.example.demo.application.service.task.ExcelTaskQueryService})</li>
 * </ul>
 * </p>
 */
package com.example.demo.application.service;
