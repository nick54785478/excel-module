/**
 * 物料清單 (BOM) 與物料主檔領域業務使用案例套件 (BOM & Material Use Cases Inbound Ports)。
 * <p>
 * <b>【架構定位與職責】</b><br>
 * 本套件位於 Clean Architecture 應用層 ({@code application/port/in/bom})，定義製造業產品結構樹 (Bill of Materials)
 * 與物料主檔 (Material Master) 之對外業務合約 (Inbound Ports / UseCases)。
 * 嚴格遵循六角形架構 (Hexagonal Architecture) 規範，所有介面均為純 Java (Pure Java)，
 * 嚴禁接發任何技術類或環境相依物件 (如 ORM 實體、Servlet HTTP 物件或資料庫連線)。
 * </p>
 * <p>
 * <b>【CQRS 職責切分】</b>
 * <ul>
 *   <li><b>Command 業務合約 (寫入/產製端點調度)</b>：
 *     <ul>
 *       <li>{@link com.example.demo.application.port.in.bom.ExportBomUseCase}：
 *           記憶體標準批次產製物料清單 Excel 檔案 (多階展開、單階配方、物料主檔)。</li>
 *       <li>{@link com.example.demo.application.port.in.bom.StreamExportBomUseCase}：
 *           百萬級巨量大數據串流輸出 (以分批 Flush 機制串流至 {@link java.io.OutputStream}，維持常數級 O(1) 記憶體)。</li>
 *     </ul>
 *   </li>
 *   <li><b>Query 業務合約 (讀取/查詢端點調度)</b>：
 *     <ul>
 *       <li>{@link com.example.demo.application.port.in.bom.QueryBomUseCase}：
 *           查詢導向之 BOM 報表動態產製與串流讀取合約。</li>
 *       <li>{@link com.example.demo.application.port.in.bom.ExportMaterialBomUseCase}：
 *           宣告式物件映射 (Declarative Bean Mapping) 物料主檔與單階配方清單匯出。</li>
 *     </ul>
 *   </li>
 * </ul>
 * </p>
 */
package com.example.demo.application.port.in.bom;
