/**
 * 資料庫持久化輸出適配器 (Persistence Outbound Adapters)
 * <p>
 * <b>【架構定位與職責規範】</b><br>
 * 位於 Clean Architecture 基礎設施層 ({@code infrastructure/adapter/persistence/})，
 * 遵循 {@code AGENTS.md} 規則 30 與 3-9 (以 {@code Adapter} 結尾)。<br>
 * 作為領域模型與 Spring Data JPA / 資料庫持久化儲存之間的防腐適配器，
 * 協調呼叫對應之 JPA Repository 與 Assembler 轉換：
 * <ul>
 *     <li>{@link com.example.demo.infrastructure.adapter.persistence.BomRepositoryAdapter}：實作 {@link com.example.demo.application.port.out.BomRepositoryPort}</li>
 *     <li>{@link com.example.demo.infrastructure.adapter.persistence.MaterialRepositoryAdapter}：實作 {@link com.example.demo.application.port.out.MaterialRepositoryPort}</li>
 *     <li>{@link com.example.demo.infrastructure.adapter.persistence.ExcelTaskRepositoryAdapter}：實作 {@link com.example.demo.application.port.out.ExcelTaskRepositoryPort}</li>
 * </ul>
 * </p>
 */
package com.example.demo.infrastructure.adapter.persistence;

