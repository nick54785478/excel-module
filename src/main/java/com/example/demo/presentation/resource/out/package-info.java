/**
 * 表現層回應資源 (Response Resources / Outbound Data)
 * <p>
 * 對外回傳的結果載體，嚴格作為純資料載體 (Pure Data Carrier)，
 * <b>不得於 Resource 內部實作 {@code fromResult} 等任何轉換邏輯</b>。
 * 命名規範：{@code N + Ved + Resource} (例如：{@code ExcelExportedResource}, {@code ExcelParsedResource}, {@code ExcelRetrievedResource})。
 */
package com.example.demo.presentation.resource.out;
