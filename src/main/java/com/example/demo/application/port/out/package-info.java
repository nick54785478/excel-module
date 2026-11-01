/**
 * 輸出端口 (Outbound Ports)
 * <p>
 * 代表應用層對外要求基礎設施層提供的合約 (包含 JPA 資料庫、MQ 訊息佇列、快取、Excel 解析引擎等)。
 * 命名規範：以 {@code Port} 結尾 (例如：{@code ExcelRepositoryPort}, {@code ExcelMessagePublisherPort}, {@code ExcelParserPort})。
 * 嚴禁在此介面方法中使用任何技術類或環境相依物件 (如 JPA Entity、Connection 等)。
 */
package com.example.demo.application.port.out;
