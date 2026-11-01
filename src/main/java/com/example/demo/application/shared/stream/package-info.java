/**
 * Excel 串流與分塊處理模組 (Excel Streaming & Chunking Module)
 * <p>
 * 遵循 Clean Architecture 應用層規範，提供高效能、低記憶體佔用之巨量資料串流處理抽象合約：
 * <ul>
 *   <li>{@link com.example.demo.application.shared.stream.ExcelRowContext}：儲存格與資料列上下文環境。</li>
 *   <li>{@link com.example.demo.application.shared.stream.ExcelRowConsumer}：逐列串流式回呼介面 ($O(1)$ 記憶體)。</li>
 *   <li>{@link com.example.demo.application.shared.stream.ExcelChunkConsumer}：分塊批次串流回呼介面 (專門對接資料庫 Batch Insert)。</li>
 *   <li>{@link com.example.demo.application.shared.stream.ExcelStreamListener}：全生命週期監聽器合約。</li>
 * </ul>
 */
package com.example.demo.application.shared.stream;
