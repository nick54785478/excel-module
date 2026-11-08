/**
 * 基礎設施層底層 Excel 串流寫入引擎模組 (Infrastructure Excel Stream Writer Engines)
 * <p>
 * <b>【架構定位與職責規範】</b><br>
 * 遵循 Clean Architecture 六角形架構規範，本套件座落於基礎設施層 (Infrastructure Layer)。<br>
 * 提供純粹技術導向、專注於 OpenXML 二進位位元組串流序列化之底層共用引擎。<br>
 * 供外層適配器（如 {@code PoiSxssfWriterAdapter}、{@code FastExcelWriterAdapter} 及 {@code ParallelPartitionExcelWriterAdapter}）內部共用，
 * <b>嚴禁外露至 Application Layer 或做為 Outbound Port 介面使用</b>。
 * </p>
 */
package com.example.demo.infrastructure.engine;

