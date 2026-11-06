/**
 * 基礎設施適配器整合測試頂層套件 (Adapter Integration Tests)
 * <p>
 * 遵循 Clean Architecture 與 BDD/Gherkin 測試規範，內部依據適配器模組垂直分類：
 * <ul>
 *     <li>{@code engine}：Excel 讀寫核心引擎適配器整合測試 (FastExcel / Apache POI / 測試範本產製)</li>
 *     <li>{@code persistence}：資料庫持久化倉儲適配器整合測試 (BOM / Material / Task Repositories)</li>
 *     <li>{@code validator}：EasyRules 規則引擎驗證適配器測試</li>
 *     <li>{@code feedback}：Apache POI 視覺化錯誤回標適配器測試</li>
 * </ul>
 * </p>
 */
package com.example.demo.infrastructure.adapter;
