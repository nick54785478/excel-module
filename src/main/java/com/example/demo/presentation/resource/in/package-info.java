/**
 * 表現層請求資源 (Request Resources / Inbound Data)
 * <p>
 * 對外接收的請求載體，嚴格作為純資料載體 (Pure Data Carrier)，
 * <b>不得於 Resource 內部實作 {@code toCommand} 等任何轉換邏輯</b>。
 * 命名規範：{@code V + N + Resource} (例如：{@code ExportExcelResource}, {@code ImportExcelResource})。
 */
package com.example.demo.presentation.resource.in;
