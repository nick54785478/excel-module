/**
 * Excel 讀寫引擎輸出適配器 (Excel Engine Outbound Adapters)
 * <p>
 * <b>【架構定位與職責規範】</b><br>
 * 位於 Clean Architecture 基礎設施層 ({@code infrastructure/adapter/engine/})，
 * 遵循 {@code AGENTS.md} 規則 30 與 3-9 (以 {@code Adapter} 結尾)。<br>
 * 實作應用層 {@link com.example.demo.application.port.out.ExcelReaderPort} 與 {@link com.example.demo.application.port.out.ExcelWriterPort} 合約，
 * 封裝隔離底層 FastExcel 與 Apache POI 具體實作技術：
 * <ul>
 *     <li>{@link com.example.demo.infrastructure.adapter.engine.FastExcelReaderAdapter}：FastExcel 串流讀取適配器</li>
 *     <li>{@link com.example.demo.infrastructure.adapter.engine.FastExcelWriterAdapter}：FastExcel 高效串流寫入適配器</li>
 *     <li>{@link com.example.demo.infrastructure.adapter.engine.PoiSaxReaderAdapter}：Apache POI SAX 事件驅動讀取適配器</li>
 *     <li>{@link com.example.demo.infrastructure.adapter.engine.PoiSxssfWriterAdapter}：Apache POI SXSSF 磁碟暫存寫入適配器</li>
 * </ul>
 * </p>
 */
package com.example.demo.infrastructure.adapter.engine;

