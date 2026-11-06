/**
 * 基礎設施輸出適配器頂層套件 (Infrastructure Outbound Adapters)
 * <p>
 * <b>【架構定位與職責規範】</b><br>
 * 遵循 Clean Architecture 與六角形架構 (Hexagonal Architecture) 規範。<br>
 * 作為所有基礎設施模組的匯聚點與對外出口，實作 Application 層的 Outbound Ports。
 * 內部依據技術功能與輸出埠領域職責劃分為四大模組子套件：
 * <ul>
 *     <li>{@code engine}：Excel 讀寫核心引擎輸出適配器 (FastExcel / Apache POI)</li>
 *     <li>{@code persistence}：資料庫持久化倉儲輸出適配器 (BOM / Material / Task Repositories)</li>
 *     <li>{@code validator}：業務規則資料校驗輸出適配器 (EasyRules Validator)</li>
 *     <li>{@code feedback}：視覺化錯誤回標輸出適配器 (POI Error Marker)</li>
 * </ul>
 * </p>
 */
package com.example.demo.infrastructure.adapter;
