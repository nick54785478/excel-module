/**
 * 應用層輸入埠根套件 (Inbound Ports / Use Case Contracts)
 * <p>
 * 遵循六角形架構與 Clean Architecture 應用層規範 (AGENTS.md 規則 22 與 43~57)。
 * 定義系統所有對外暴露之業務使用情境合約介面，以 {@code UseCase} 結尾。
 * 方法參數僅可接收 Command / Query，回傳值僅可為 DTO / View / 領域實質資料，嚴禁使用技術框架物件。
 * </p>
 * <p>
 * <b>【模組子套件劃分 (Sub-module Packages)】</b>
 * <ul>
 *   <li>{@code bom}：物料與產品結構配方業務使用案例 ({@link com.example.demo.application.port.in.bom.ExportBomUseCase}, {@link com.example.demo.application.port.in.bom.QueryBomUseCase} 等)</li>
 *   <li>{@code core}：核心通用 Excel 檔案與 Java Bean 雙向映射使用案例 ({@link com.example.demo.application.port.in.core.ExportExcelUseCase}, {@link com.example.demo.application.port.in.core.ImportExcelUseCase} 等)</li>
 *   <li>{@code template}：JXLS 3.x 宣告式範本渲染與動態欄位映射使用案例 ({@link com.example.demo.application.port.in.template.ExportTemplateExcelUseCase}, {@link com.example.demo.application.port.in.template.PreviewTemplateMappingUseCase} 等)</li>
 *   <li>{@code report}：企業級裝飾複合報表產出使用案例 ({@link com.example.demo.application.port.in.report.ExportFinancialReportUseCase})</li>
 *   <li>{@code parallel}：大數據高並發平行管線與分片 ZIP 串流匯出使用案例 ({@link com.example.demo.application.port.in.parallel.ExportParallelExcelUseCase})</li>
 *   <li>{@code validation}：EasyRules 宣告式業務規則檢核與錯誤回標使用案例 ({@link com.example.demo.application.port.in.validation.ValidateExcelUseCase})</li>
 *   <li>{@code task}：Excel 長任務非同步提交、進度查詢與產檔下載使用案例 ({@link com.example.demo.application.port.in.task.SubmitAsyncExcelExportUseCase}, {@link com.example.demo.application.port.in.task.GetExcelTaskUseCase} 等)</li>
 * </ul>
 * </p>
 */
package com.example.demo.application.port.in;
