/**
 * 物料聚合領域事件切片 (Material Domain Events)
 * <p>
 * 遵循 DDD (領域驅動設計) 與專案架構規範 (AGENTS.md 3-3 規範：{@code N + Ved + Event})。
 * 定義物料生命週期狀態異動時所產生之不可變領域事件載體 (Domain Events)。
 *
 * <h3>一、包含之領域事件</h3>
 * <ul>
 *   <li>{@link com.example.demo.domain.material.event.MaterialCreatedEvent}：新物料主檔建立事件。</li>
 *   <li>{@link com.example.demo.domain.material.event.MaterialUpdatedEvent}：物料品名或規格異動事件。</li>
 *   <li>{@link com.example.demo.domain.material.event.MaterialCostChangedEvent}：物料標準單價成本調整事件。</li>
 *   <li>{@link com.example.demo.domain.material.event.MaterialDeactivatedEvent}：物料停用/淘汰事件。</li>
 * </ul>
 *
 * <h3>二、現況架構評估 (Architecture Evaluation)</h3>
 * <ul>
 *   <li><b>領域層內部 (Domain Layer) - 正常運作：</b>
 *       聚合根 {@link com.example.demo.domain.material.aggregate.root.Material} 於執行生命週期異動行為
 *       ({@code create}, {@code updateBasicInfo}, {@code updateCost}, {@code discontinue}) 時，
 *       會自主記錄並暫存於內部之 {@code domainEvents} 佇列，並於單元測試中由 {@code MaterialTest} 完成 100% 覆蓋率驗證。</li>
 *   <li><b>外層應用與基礎設施 (Application & Infrastructure Layer) - 目前尚未對外發布：</b>
 *       當前專案模組主要定位為 Excel 處理引擎 (excel-module)，物料主要作為多階 BOM 與對照報表匯出、串流及範本映射之資料來源 (Read/Export Side)，
 *       目前應用層尚未提供物料後台寫入管理之 Use Case，故外層尚未實裝外部訊息發布器 (如 Spring ApplicationEventPublisher 或 Message Broker)。</li>
 * </ul>
 *
 * <h3>三、設計建議與擴充評估 (Recommendations & Future Extensibility)</h3>
 * <ul>
 *   <li><b>符合規範並建議完整保留：</b>
 *       嚴格恪守 DDD 聚合根自給自足 (Self-contained) 原則與事件累積模式 (Event Accumulation)，
 *       領域層維持技術無關性 (Pure Java)，不因外層暫未消費而破壞領域模型的完整性。</li>
 *   <li><b>未來擴充零重構：</b>
 *       未來若需要實作微服務跨模組連動（例如：當標準成本調整觸發 {@link com.example.demo.domain.material.event.MaterialCostChangedEvent} 時，
 *       自動連動重新計算下游所有 BOM 總成本；或對外發送 MQ 訊息同步外部 ERP/MES 系統），
 *       僅需在應用層加裝 Outbound Port 與 Publisher 適配器，領域模型核心邏輯完全零改動。</li>
 * </ul>
 */
package com.example.demo.domain.material.event;

