/**
 * Excel 驗證與錯誤回饋輸出適配器 (Excel Validation & Error Feedback Outbound Adapters)
 * <p>
 * <b>【架構定位與職責規範】</b><br>
 * 位於 Clean Architecture 基礎設施層 ({@code infrastructure/adapter/validation/})，
 * 遵循 {@code AGENTS.md} 規則 30 與 3-9 (以 {@code Adapter} 結尾)。<br>
 * 實作應用層驗證合約：
 * <ul>
 *     <li>{@link com.example.demo.application.port.out.validation.ExcelRowValidatorPort}：EasyRules 單列驗證引擎適配器</li>
 *     <li>{@link com.example.demo.application.port.out.validation.ExcelSheetValidatorPort}：EasyRules 工作表驗證引擎適配器</li>
 *     <li>{@link com.example.demo.application.port.out.validation.ExcelErrorMarkerPort}：Apache POI 錯誤標註與批註適配器</li>
 * </ul>
 * </p>
 */
package com.example.demo.infrastructure.adapter.validation;

