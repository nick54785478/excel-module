/**
 * Excel 核心讀寫引擎輸出適配器 (Excel Core Engine Outbound Adapters)
 * <p>
 * <b>【架構定位與職責規範】</b><br>
 * 位於 Clean Architecture 基礎設施層 ({@code infrastructure/adapter/core/})，
 * 遵循 {@code AGENTS.md} 規範與六角形架構 Outbound Adapter 規則。<br>
 * 實作應用層 {@link com.example.demo.application.port.out.core.ExcelReaderPort} 與 {@link com.example.demo.application.port.out.core.ExcelWriterPort} 合約，
 * 封裝隔離底層 FastExcel 與 Apache POI 具體實作技術：
 * <ul>
 *     <li>{@link com.example.demo.infrastructure.adapter.core.FastExcelReaderAdapter}：FastExcel 串流讀取適配器</li>
 *     <li>{@link com.example.demo.infrastructure.adapter.core.FastExcelWriterAdapter}：FastExcel 高效串流寫入適配器</li>
 *     <li>{@link com.example.demo.infrastructure.adapter.core.PoiSaxReaderAdapter}：Apache POI SAX 事件驅動讀取適配器</li>
 *     <li>{@link com.example.demo.infrastructure.adapter.core.PoiSxssfWriterAdapter}：Apache POI SXSSF 磁碟暫存寫入適配器</li>
 * </ul>
 * </p>
 */
package com.example.demo.infrastructure.adapter.core;
