# 維度二 Part 1：EasyRules 檢核管線與全量錯誤診斷模型 (EasyRules Validation Pipeline & Error Diagnostics)

> 📌 本文件座落於 [`doc/design/custom-validation-and-error-feedback/01-easyrules-validation-pipeline.md`](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/design/custom-validation-and-error-feedback/01-easyrules-validation-pipeline.md)，屬於「維度二：宣告式資料校驗與錯誤回標」之【Part 1：EasyRules 檢核管線與全量錯誤診斷模型】。  
> 詳述 5 層多維檢核架構、EasyRules 4.1.0 宣告式規則引擎組件庫、非短路全量錯誤收集機制、以及不可變 Java 21 Record 錯誤事實模型。  
> 
> 🔗 **模組導讀與關聯文件**：  
> • 模組入口主文件：👉 [維度二：宣告式資料校驗與錯誤回標架構總覽 (custom-validation-and-error-feedback.md)](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/design/custom-validation-and-error-feedback.md)  
> • 錯誤回標與切面守衛：👉 [維度二 Part 2：高傳真錯誤回標檔、前置切面守衛與 OCP 範本註冊 (02-error-marked-excel-and-guard-aspect.md)](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/design/custom-validation-and-error-feedback/02-error-marked-excel-and-guard-aspect.md)

---

## 1. 五層多維檢核管線全貌 (Multi-Tier Validation Pipeline)

傳統批次匯入往往在遇到第一個錯誤即中斷流程（Fail-Fast），使用者必須反覆重傳十幾次。本設計採用**全量錯誤累積模式 (Accumulate All Errors)**，掃描整份試算表：

| 層級 | 檢核管線 | 負責技術 / 組件 | 說明與檢核重點 |
| :--- | :--- | :--- | :--- |
| **Level 1** | **檔案結構檢核** | Controller / Spring Validation | 驗證副檔名（`.xlsx`/`.xls`）、檔案不可為空、工作表是否存在。 |
| **Level 2** | **表頭與型別映射** | `ExcelBeanAssembler` | 檢查 Excel 必要欄位表頭是否存在，並將儲存格二維資料自動對齊轉型為強型別 Java 模型（支援 JavaBean 與 Java 21 Record）。 |
| **Level 3** | **行級客製業務檢核** | `EasyRulesExcelValidatorAdapter`<br/>+ EasyRules POJO Rules | **核心業務檢核**：逐列評估金額是否大於 0、數量區間、折扣率聯動、狀態值是否合法等。透過非短路模式收集全量違規事實。 |
| **Level 4** | **整表跨列約束檢核** | `ExcelSheetValidatorPort` | **集合級約束**：如「整張工作表內項目編號嚴禁重複」、「明細總金額與表頭加總對帳」等跨列比對。 |
| **Level 5** | **視覺化錯誤回標檔** | `PoiExcelErrorMarkerAdapter` | 若檢核存在錯誤，在原始試算表上**原樣保留格式**，將出錯儲存格標記為**淡紅底深紅字**，並在最右側追加**錯誤原因說明欄**。 |

---

## 2. 應用層純資料契約與錯誤模型 (Pure Java 21 Records)

所有契約與模型座落於 [`com.example.demo.application.shared`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/application/shared)，純 Java 21 Record，零外部框架侵入：

### 2.1 儲存格錯誤詳細描述：`ExcelRowError`
```java
package com.example.demo.application.shared.model.validation;

/**
 * 試算表單一儲存格錯誤事實純資料載體 (Excel Row Error Fact)
 *
 * @param rowIndex     Excel 物理行號 (1-based, 包含表頭)
 * @param columnIndex  欄位索引 (0-based)
 * @param columnName   欄位標題名稱 (如 "購買金額")
 * @param invalidValue 使用者輸入之原始違規值字串
 * @param errorMessage 具體錯誤原因說明 (如 "購買金額必須大於 0")
 */
public record ExcelRowError(
        int rowIndex,
        int columnIndex,
        String columnName,
        String invalidValue,
        String errorMessage
) {}
```

### 2.2 檢核結果匯總純資料載體：`ExcelValidationGottenResult<T>`
```java
package com.example.demo.application.shared.dto;

import java.util.List;

public record ExcelValidationGottenResult<T>(
        boolean valid,
        int totalRows,
        int errorRowCount,
        List<T> validRows,
        List<ExcelRowError> errors,
        byte[] errorMarkedFileBytes
) {
    public boolean hasErrors() {
        return !valid || (errors != null && !errors.isEmpty());
    }
}
```

---

## 3. 基礎設施層 EasyRules 規則引擎組件庫

座落於 [`com.example.demo.infrastructure.adapter.validator`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/infrastructure/adapter/validation)：

### 3.1 規則契約與抽象基類
* [`ExcelRowValidationRule<T>`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/infrastructure/adapter/validation/rules/ExcelRowValidationRule.java)：定義 `evaluate(T item, int rowIndex)` 與 `int priority()`。
* [`AbstractExcelRowValidationRule<T>`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/infrastructure/adapter/validation/rules/AbstractExcelRowValidationRule.java)：封裝 EasyRules 的 `@Rule`, `@Condition`, `@Action` 註解。

### 3.2 具體商業規則實作範例
```java
@Component
public class OrderAmountValidationRule extends AbstractExcelRowValidationRule<OrderImportModel> {

    @Override
    public boolean when(OrderImportModel order, int rowIndex) {
        return order.getAmount() == null || order.getAmount().compareTo(BigDecimal.ZERO) <= 0;
    }

    @Override
    public ExcelRowError then(OrderImportModel order, int rowIndex) {
        return new ExcelRowError(
                rowIndex,
                2,
                "購買金額",
                String.valueOf(order.getAmount()),
                "購買金額必須大於 0 元"
        );
    }
}
```

### 3.3 規則排程適配器：`EasyRulesExcelValidatorAdapter`
座落於 [`com.example.demo.infrastructure.adapter.validation.EasyRulesExcelValidatorAdapter`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/infrastructure/adapter/validation/EasyRulesExcelValidatorAdapter.java)：
* 實作應用層 Outbound Port [`ExcelRowValidatorPort`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/application/port/out/validation/ExcelRowValidatorPort.java)。
* 建立 `RulesEngineParameters().skipOnFirstAppliedRule(false).skipOnFirstFailedRule(false)`，確保整張工作表的所有規則完整掃描，全量非短路累積錯誤。

