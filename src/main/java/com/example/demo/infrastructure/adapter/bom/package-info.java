/**
 * 物料與 BOM 持久化輸出適配器 (Material and BOM Persistence Outbound Adapters)
 * <p>
 * <b>【架構定位與職責規範】</b><br>
 * 位於 Clean Architecture 基礎設施層 ({@code infrastructure/adapter/bom/})，
 * 遵循 {@code AGENTS.md} 規則 30 與 3-9 (以 {@code Adapter} 結尾)。<br>
 * 實作應用層 {@link com.example.demo.application.port.out.bom.BomRepositoryPort} 與
 * {@link com.example.demo.application.port.out.bom.MaterialRepositoryPort} 合約，
 * 協調呼叫底層 JPA 持久化模組 ({@code persistence/bom/} 與 {@code persistence/material/})，
 * 負責將資料庫 JPA Entity 與 Domain Aggregate 進行相互轉換與持久化儲存。
 * </p>
 */
package com.example.demo.infrastructure.adapter.bom;

