# Excel 宣告式物件映射開發與維護手冊 (Declarative Bean Mapping Manual)

> 📌 **文件位置**：`doc/maintenance/declarative-bean-mapping-manual.md`  
> 🎯 **適用對象**：後端開發人員、架構維運工程師  
> 🏷️ **核心技術**：Java 21 Record、JavaBean 反射快取、元數據註解系統、Clean Architecture

---

## 目錄 (Table of Contents)
- [1. 模組簡介與設計理念](#1-模組簡介與設計理念)
- [2. 宣告式註解規格總覽](#2-宣告式註解規格總覽)
- [3. 雙軌模型定義教學 (POJO vs Record)](#3-雙軌模型定義教學-pojo-vs-record)
- [4. 應用層調用指南 (Assembler API)](#4-應用層調用指南-assembler-api)
- [5. 智慧型別轉換與容錯機制](#5-智慧型別轉換與容錯機制)
- [6. 常見問題與維運排查 (FAQ)](#6-常見問題與維運排查-faq)

---

## 1. 模組簡介與設計理念

在傳統試算表處理中，開發者常被迫直接操作二維矩陣 `List<List<Object>>` 或字串字典 `Map<String, Object>`。這帶來了顯著缺點：缺少編譯期型別安全、容易因欄位拼寫錯誤引發 `NullPointerException`，且需反覆撰寫字串轉日期、數值等樣板代碼。

### 本機制核心特色：
* **宣告式標註**：透過 [`@ExcelSheet`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/application/annotation/mapping/ExcelSheet.java) 與 [`@ExcelColumn`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/application/annotation/mapping/ExcelColumn.java) 標記業務實體，全自動完成活頁簿結構對齊。
* **雙軌原生支援**：
  * **JavaBean 模式**：標準 POJO（無參建構子 + Getter/Setter 反射）。
  * **Java 21 Record 模式**：不可變 Record（透過規範建構子 Canonical Constructor 自動依序組裝）。
* **極致效能快取**：以靜態 `ConcurrentHashMap` 預熱屬性元數據、存取器與建構子句柄，消除重複反射開銷，吞吐量逼近硬編碼。
* **智慧寬鬆匹配**：自動過濾千分位逗號（`"12,500.50"`）、貨幣符號、自訂日期格式，並支援欄位未匹配時的預設值自動填補。

---

## 2. 宣告式註解規格總覽

### 2.1 `@ExcelSheet` (工作表級別配置)
標註於 Class 或 Record 宣告之上：

| 屬性名稱 | 資料型別 | 預設值 | 說明 |
| :--- | :--- | :--- | :--- |
| `name` / `value` | `String` | `"Sheet1"` | 匯出或匹配之工作表名稱 |
| `headerRowIndex` | `int` | `0` | 標題列之列號 (0-based，預設第 1 列為表頭) |
| `dataRowStartIndex`| `int` | `1` | 資料內容起始列號 (0-based，預設第 2 列起為資料) |

### 2.2 `@ExcelColumn` (欄位級別配置)
標註於屬性欄位 (Field)、Getter 方法或 Record 元件 (Record Component) 上：

| 屬性名稱 | 資料型別 | 預設值 | 說明 |
| :--- | :--- | :--- | :--- |
| `name` / `value` | `String` | `""` | 欄位標題文字（比對工作表表頭文字） |
| `index` | `int` | `-1` | 顯式指定欄位索引 (0-based，`-1` 代表依宣告順序) |
| `format` | `String` | `""` | 格式化規則（如 `#,##0.00` 或 `yyyy-MM-dd`） |
| `defaultValue` | `String` | `""` | 當儲存格為空或資料缺漏時的預設字串值 |
| `ignore` | `boolean` | `false` | 若為 `true` 則在讀寫時忽略此屬性 |

---

## 3. 雙軌模型定義教學 (POJO vs Record)

### 3.1 模式 A：標準 JavaBean (POJO) 定義
適用於既有業務實體或需要可變狀態（Setter）的場景：

```java
package com.example.demo.application.shared.model.demo;

import java.math.BigDecimal;
import java.time.LocalDate;
import com.example.demo.application.annotation.mapping.ExcelColumn;
import com.example.demo.application.annotation.mapping.ExcelSheet;

@ExcelSheet(name = "訂單匯入清單", headerRowIndex = 0, dataRowStartIndex = 1)
public class OrderModel {

    @ExcelColumn(name = "訂單編號", index = 0)
    private String orderNo;

    @ExcelColumn(name = "客戶名稱", index = 1, defaultValue = "一般客戶")
    private String customerName;

    @ExcelColumn(name = "購買金額", index = 2, format = "#,##0.00")
    private BigDecimal amount;

    @ExcelColumn(name = "購買數量", index = 3, defaultValue = "1")
    private Integer quantity;

    @ExcelColumn(name = "訂單日期", index = 4, format = "yyyy-MM-dd")
    private LocalDate orderDate;

    @ExcelColumn(name = "訂單狀態", index = 5)
    private String status;

    // 必須包含公開無參建構子
    public OrderModel() {}

    // Getter & Setter 略...
}
```

### 3.2 模式 B：Java 21 不可變 Record 定義 (推薦)
適用於純資料載體（Pure Data Carrier），具備線程安全與不可變特性：

```java
package com.example.demo.application.shared.model.demo;

import java.math.BigDecimal;
import com.example.demo.application.annotation.mapping.ExcelColumn;
import com.example.demo.application.annotation.mapping.ExcelSheet;

@ExcelSheet(name = "商品主檔清單", headerRowIndex = 0, dataRowStartIndex = 1)
public record ProductRecordModel(
    @ExcelColumn(name = "商品料號", index = 0)
    String productCode,

    @ExcelColumn(name = "商品名稱", index = 1)
    String productName,

    @ExcelColumn(name = "商品售價", index = 2, format = "#,##0.00")
    BigDecimal price,

    @ExcelColumn(name = "是否上架", index = 3, defaultValue = "true")
    Boolean onSale
) {
}
```

---

## 4. 應用層調用指南 (Assembler API)

模組提供了靜態無狀態工具類 [`ExcelBeanAssembler`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/application/assembler/ExcelBeanAssembler.java)，負責在應用層進行雙向資料轉換。

### 4.1 匯入解析：將 Excel 讀取結果轉換為強型別清單
```java
// 1. 透過 ExcelReaderPort 讀取試算表二進位資料
ExcelGottenResult gottenResult = excelReaderPort.read(inputStream);

// 2. 宣告式反序列化為 Model 清單 (支援 JavaBean 或 Record)
List<OrderModel> orders = ExcelBeanAssembler.toBeanList(gottenResult, OrderModel.class);
List<ProductRecordModel> products = ExcelBeanAssembler.toBeanList(gottenResult, ProductRecordModel.class);
```

### 4.2 匯出組裝：將強型別清單轉換為工作表寫入命令
```java
List<OrderModel> orderList = getOrderListFromService();

// 1. 提取 Model 定義的工作表名稱與標題列
String sheetName = ExcelBeanAssembler.extractSheetName(OrderModel.class);
List<String> headers = ExcelBeanAssembler.extractHeaders(OrderModel.class);

// 2. 將 Model 清單轉為二維資料列陣列
List<List<Object>> rows = ExcelBeanAssembler.toRowDataList(orderList, OrderModel.class);

// 3. 組裝寫入命令並調用 ExcelWriterPort
WriteSheetCommand sheetCommand = WriteSheetCommand.builder()
        .sheetName(sheetName)
        .headers(headers)
        .rows(rows)
        .build();
byte[] excelBytes = excelWriterPort.write(new ExportExcelCommand("orders.xlsx", List.of(sheetCommand)));
```

---

## 5. 智慧型別轉換與容錯機制

[`ExcelBeanAssembler`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/application/assembler/ExcelBeanAssembler.java) 內建容錯轉換器，支援各種非標準字串自動轉型：

1. **數值與金額容錯**：
   * 自動去除千分位逗號（`"1,234,567.89"` $\to$ `1234567.89`）。
   * 自動過濾常見貨幣符號（`"$"`、`"NT$"`、`"¥"`、`"€"`）。
2. **多種日期時間格式**：
   * 原生支援 `LocalDate`、`LocalDateTime`、`Date`。
   * 優先依據 `@ExcelColumn(format = "...")` 解析；若未指定則依序嘗試 `yyyy-MM-dd`、`yyyy/MM/dd`、`yyyyMMdd`、ISO-8601。
3. **布林值容錯**：
   * 支援字串 `"true"`、`"1"`、`"Y"`、`"YES"`、`"是"` 自動轉為 `Boolean.TRUE`。
4. **預設值補齊機制**：
   * 若儲存格內容為空字串或 null，且標記了 `@ExcelColumn(defaultValue = "100")`，系統將自動填補該預設值。

---

## 6. 常見問題與維運排查 (FAQ)

### Q1：讀取時回傳的物件屬性全為 null？
* **檢查 1**：確認 Excel 第一列表頭名稱是否與 `@ExcelColumn(name = "...")` 完全相符（前後是否有隱形空白字元）。
* **檢查 2**：若使用 JavaBean，確認是否具備 `public` 無參建構子與標準 `get/set` 方法。

### Q2：欄位未依照期望順序匯出？
`@ExcelColumn` 若未設定 `index`，預設會依據反射欄位聲明順序。若需確保跨環境與 JVM 編譯順序一致，**強烈建議顯式設定 `index = 0`, `index = 1`...**。

### Q3：如何忽略某些特定輔助屬性？
在該屬性上方標記 `@ExcelColumn(ignore = true)`，或使用 Java `transient` 關鍵字，解析器會自動跳過。

