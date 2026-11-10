/**
 * 應用層輸出埠根套件 (Outbound Ports / Infrastructure Contracts)
 * <p>
 * 遵循六角形架構與 Clean Architecture 應用層規範 (AGENTS.md 規則 22 與 43~57)。
 * 定義應用層調度外部基礎設施（資料庫持久化、Excel 讀寫引擎、訊息佇列、快取等）之契約介面，以 {@code Port} 結尾。
 * 方法參數與回傳型別嚴禁接發任何技術類或 ORM/HTTP 環境相依物件，保證純淨與技術無關性。
 * </p>
 * <p>
 * <b>【模組子套件劃分 (Sub-module Packages)】</b>
 * <ul>
 *   <li>{@code bom}：物料與產品結構聚合根持久化輸出埠 ({@link com.example.demo.application.port.out.bom.BomRepositoryPort}, {@link com.example.demo.application.port.out.bom.MaterialRepositoryPort})</li>
 *   <li>{@code core}：核心 Excel 讀寫底層引擎輸出埠 ({@link com.example.demo.application.port.out.core.ExcelReaderPort}, {@link com.example.demo.application.port.out.core.ExcelWriterPort})</li>
 *   <li>{@code template}：JXLS 3.x 宣告式範本渲染、中繼註冊表與欄位映射輸出埠 ({@link com.example.demo.application.port.out.template.ExcelTemplateWriterPort}, {@link com.example.demo.application.port.out.template.ExcelTemplateRegistryPort}, {@link com.example.demo.application.port.out.template.ExcelTemplateFieldMapperPort})</li>
 *   <li>{@code report}：企業級複合報表裝飾策略工廠輸出埠 ({@link com.example.demo.application.port.out.report.ExcelReportDecoratorFactoryPort})</li>
 *   <li>{@code parallel}：大數據分片與並行管線寫入器輸出埠 ({@link com.example.demo.application.port.out.parallel.ExcelPartitionWriterPort})</li>
 *   <li>{@code validation}：資料校驗器與錯誤回標產物生成輸出埠 ({@link com.example.demo.application.port.out.validation.ExcelRowValidatorPort}, {@link com.example.demo.application.port.out.validation.ExcelSheetValidatorPort}, {@link com.example.demo.application.port.out.validation.ExcelErrorMarkerPort})</li>
 *   <li>{@code task}：Excel 長任務狀態持久化儲存庫輸出埠 ({@link com.example.demo.application.port.out.task.ExcelTaskRepositoryPort})</li>
 * </ul>
 * </p>
 */
package com.example.demo.application.port.out;
