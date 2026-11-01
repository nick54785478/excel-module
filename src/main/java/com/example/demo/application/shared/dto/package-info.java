/**
 * 應用層資料傳輸物件 (Application DTOs)
 * <p>
 * 嚴格作為純資料載體 (Pure Data Carrier)，禁止在內部撰寫 {@code fromDomain} 等任何轉換邏輯。
 * 命名規範：{@code N + Gotten/Searched + Result} (例如：{@code ExcelGottenResult}, {@code ExcelSearchedResult})。
 */
package com.example.demo.application.shared.dto;
