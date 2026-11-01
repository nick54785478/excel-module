/**
 * 基礎設施輸出適配器 (Outbound Adapters)
 * <p>
 * 作為所有基礎設施模組的匯聚點與對外出口，實作 Application 層的 Outbound Port。
 * 由其協調並呼叫 {@code persistence/}、{@code messaging/} 或第三方工具 (如 FastExcel)。
 * 命名規範：以 {@code Adapter} 結尾 (例如：{@code ExcelRepositoryAdapter}, {@code ExcelMessagePublisherAdapter}, {@code FastExcelProcessorAdapter})。
 */
package com.example.demo.infrastructure.adapter;
