/**
 * 應用層組裝器 (Application Assemblers / Mappers)
 * <p>
 * 專門負責領域模型 (Domain Model) 與 DTO/View 之間的雙向轉換，維持純資料載體的單一職責。
 * 嚴禁在 DTO/View 內部實作 {@code fromDomain} 等轉換邏輯。
 * 命名規範：以 {@code DtoAssembler}、{@code ViewAssembler} 結尾 (例如：{@code ExcelDtoAssembler})。
 */
package com.example.demo.application.assembler;
