/**
 * 視覺化錯誤回標輸出適配器套件 (Feedback Outbound Adapters)
 * <p>
 * 遵循六角形架構規範，實作應用層 {@link com.example.demo.application.port.out.ExcelErrorMarkerPort}：
 * <ul>
 *     <li>{@link com.example.demo.infrastructure.adapter.feedback.PoiExcelErrorMarkerAdapter}：基於 Apache POI 之儲存格淡紅底深紅字渲染與錯誤欄位追加適配器</li>
 * </ul>
 * </p>
 */
package com.example.demo.infrastructure.adapter.feedback;

