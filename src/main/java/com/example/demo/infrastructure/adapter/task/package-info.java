/**
 * 異步任務持久化輸出適配器 (Excel Task Persistence Outbound Adapter)
 * <p>
 * <b>【架構定位與職責規範】</b><br>
 * 位於 Clean Architecture 基礎設施層 ({@code infrastructure/adapter/task/})，
 * 遵循 {@code AGENTS.md} 規則 30 與 3-9 (以 {@code Adapter} 結尾)。<br>
 * 實作應用層 {@link com.example.demo.application.port.out.task.ExcelTaskRepositoryPort} 合約，
 * 協調呼叫底層 JPA 持久化模組 ({@code persistence/task/})，負責異步任務生命週期與進度記錄之儲存與讀取。
 * </p>
 */
package com.example.demo.infrastructure.adapter.task;

