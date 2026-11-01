# Excel 宣告式物件映射測試介面使用手冊 (Declarative Bean Mapping Guide)

本手冊專門說明如何使用 **「Excel 宣告式物件映射 (Declarative Bean Mapping)」** REST 測試介面，涵蓋從 **標準 JavaBean (POJO)** 與 **Java 21 原生不可變 Record** 兩種維度的匯入、匯出與大數據串流實測指南。

---

## 一、核心特色與設計概念

* **宣告式註解標註**：透過在 Java 類別或 Record 上標記 [`@ExcelSheet`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/application/annotation/ExcelSheet.java) 與 [`@ExcelColumn`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/application/annotation/ExcelColumn.java)，完全擺脫以往需要手動解析二維矩陣 `List<List<Object>>` 或鍵值字典 `Map<String, Object>` 的重複負擔。
* **雙軌相容**：
  * **JavaBean 模式**：支援標準無參建構子與 Getter/Setter 存取（以 [`OrderModel`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/application/shared/model/OrderModel.java) 為例）。
  * **Java 21 Record 模式**：支援原生不可變 Record 之規範建構子 (Canonical Constructor) 自動反射綁定（以 [`ProductRecordModel`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/application/shared/model/ProductRecordModel.java) 為例）。
* **智慧型別轉換**：內建反射快取與格式化解析，自動支援千分位數值（如 `"12,500.50"` 自動轉為 `BigDecimal`）、貨幣符號、自訂日期格式（`yyyy-MM-dd`）、預設值填補（`defaultValue`）及列索引顯式指定。
* **架構嚴謹**：嚴格遵循 Clean Architecture 六角形架構，Controller 調用應用層 Inbound UseCase，Outbound Port 維持純粹 SPI 契約。

---

## 二、REST API 端點總覽

所有端點基礎路徑 (Base URL)：
```
http://localhost:8080/api/v1/excel/bean-mapping
```

| HTTP 方法 | 端點路徑 (Endpoint) | 功能說明 | 請求載體 (Request) | 回應內容 (Response) |
| :---: | :--- | :--- | :--- | :--- |
| **GET** | `/orders/export` | 快速下載訂單展示 Excel (JavaBean) | 無 (自動載入展示資料) | `.xlsx` 檔案二進位下載 |
| **POST** | `/orders/export` | 自訂 JSON 資料宣告式匯出 (JavaBean) | `ExportOrderResource` (JSON) | `.xlsx` 檔案二進位下載 |
| **POST** | `/orders/stream-export` | 訂單大數據串流直接沖刷匯出 (JavaBean) | `ExportOrderResource` (JSON) | `.xlsx` HTTP 輸出流直沖 |
| **POST** | `/orders/import` | 上傳 Excel 並宣告式解析為 JavaBean | `file` (MultipartFile) | `OrderImportedResource` (JSON) |
| **GET** | `/products/export` | 快速下載商品展示 Excel (Java 21 Record) | 無 (自動載入展示資料) | `.xlsx` 檔案二進位下載 |
| **POST** | `/products/export` | 自訂 JSON 資料宣告式匯出 (Java 21 Record) | `ExportProductResource` (JSON) | `.xlsx` 檔案二進位下載 |
| **POST** | `/products/import` | 上傳 Excel 並宣告式解析為 Record | `file` (MultipartFile) | `ProductImportedResource` (JSON) |

---

## 三、詳細操作與 cURL 測試範例

### 情境 1：標準 JavaBean 模式 (訂單管理 - OrderModel)

#### 1-1. 快速 GET 下載訂單展示 Excel
直接透過瀏覽器或 cURL 發送 GET 請求，系統會自動生成 3 筆預設展示資料並依照註解設定格式化產製 Excel：

```bash
curl -X GET "http://localhost:8080/api/v1/excel/bean-mapping/orders/export" \
     -o "orders_sample.xlsx"
```
* **產出檔案特性**：
  * 工作表名稱：`訂單匯入清單`（取自 `@ExcelSheet`）。
  * 標題列：`訂單編號`、`訂購人`、`購買金額`、`數量`、`下單日期`、`訂單狀態`。
  * 數值格式：`購買金額` 自動套用 `#,##0.00` 格式，`下單日期` 套用 `yyyy-MM-dd`。

---

#### 1-2. 自訂 JSON 資料 POST 匯出訂單 Excel
若前端或調用端欲自行指定工作表名稱與訂單明細資料，可透過 POST 傳入 JSON：

```bash
curl -X POST "http://localhost:8080/api/v1/excel/bean-mapping/orders/export" \
     -H "Content-Type: application/json" \
     -d '{
       "sheetName": "2026年11月特約訂單",
       "orders": [
         {
           "orderNo": "ORD-2026-VIP01",
           "customerName": "張大千",
           "amount": 88000.50,
           "quantity": 3,
           "orderDate": "2026-11-01",
           "status": "PAID"
         },
         {
           "orderNo": "ORD-2026-VIP02",
           "customerName": "林小芳",
           "amount": 16500.00,
           "quantity": 1,
           "orderDate": "2026-11-02",
           "status": "SHIPPED"
         }
       ]
     }' \
     -o "vip_orders.xlsx"
```

---

#### 1-3. 檔案上傳並宣告式解析為 JavaBean (POST /orders/import)
使用者將編輯完成的 Excel 上傳，後端自動將 Excel 活頁簿內容**直接綁定並轉換為 `List<OrderModel>`**：

```bash
curl -X POST "http://localhost:8080/api/v1/excel/bean-mapping/orders/import" \
     -H "Content-Type: multipart/form-data" \
     -F "file=@orders_sample.xlsx"
```

* **回應 JSON 範例 (`OrderImportedResource`)**：
```json
{
  "totalCount": 3,
  "sheetName": "訂單匯入清單",
  "items": [
    {
      "orderNo": "ORD-2026-001",
      "customerName": "陳大文",
      "amount": 12500.50,
      "quantity": 2,
      "orderDate": "2026-11-01",
      "status": "PAID"
    },
    {
      "orderNo": "ORD-2026-002",
      "customerName": "李曉芳",
      "amount": 3200.00,
      "quantity": 1,
      "orderDate": "2026-11-02",
      "status": "SHIPPED"
    },
    {
      "orderNo": "ORD-2026-003",
      "customerName": "張志豪",
      "amount": 580.00,
      "quantity": 5,
      "orderDate": "2026-11-03",
      "status": "PENDING"
    }
  ]
}
```

---

#### 1-4. 大數據串流沖刷匯出 (POST /orders/stream-export)
若資料筆數龐大，調用串流端點可直接透過 Servlet Response 輸出流進行寫入並定時 Flush，保持極低常數級記憶體消耗：

```bash
curl -X POST "http://localhost:8080/api/v1/excel/bean-mapping/orders/stream-export" \
     -o "orders_stream.xlsx"
```

---

### 情境 2：Java 21 原生 Record 模式 (商品主檔 - ProductRecordModel)

#### 2-1. 快速 GET 下載商品主檔展示 Excel
```bash
curl -X GET "http://localhost:8080/api/v1/excel/bean-mapping/products/export" \
     -o "products_sample.xlsx"
```
* **產出特點**：驗證 Java 21 原生不可變 Record 自動依標記屬性產製 Excel。

---

#### 2-2. 自訂 JSON 匯出商品主檔
```bash
curl -X POST "http://localhost:8080/api/v1/excel/bean-mapping/products/export" \
     -H "Content-Type: application/json" \
     -d '{
       "sheetName": "旗艦商品主檔",
       "products": [
         {
           "productCode": "PROD-9001",
           "productName": "頂級雙鏡頭空拍機",
           "price": 48900.00,
           "onSale": true
         },
         {
           "productCode": "PROD-9002",
           "productName": "長續航智慧型防爆電池",
           "price": 6200.00,
           "onSale": false
         }
       ]
     }' \
     -o "flagship_products.xlsx"
```

---

#### 2-3. 上傳 Excel 並宣告式解析為 Java 21 Record (POST /products/import)
```bash
curl -X POST "http://localhost:8080/api/v1/excel/bean-mapping/products/import" \
     -H "Content-Type: multipart/form-data" \
     -F "file=@products_sample.xlsx"
```

* **回應 JSON 範例 (`ProductImportedResource`)**：
```json
{
  "totalCount": 3,
  "sheetName": "商品主檔清單",
  "items": [
    {
      "productCode": "PROD-001",
      "productName": "4K 專業航拍無人機",
      "price": 35000.00,
      "onSale": true
    },
    {
      "productCode": "PROD-002",
      "productName": "高容量鋰電池模組",
      "price": 4500.00,
      "onSale": false
    },
    {
      "productCode": "PROD-003",
      "productName": "多功能收納背包",
      "price": 1280.00,
      "onSale": true
    }
  ]
}
```

---

## 四、使用 Swagger UI 進行視覺化測試

啟動 Spring Boot 服務後，打開瀏覽器訪問：
```
http://localhost:8080/swagger-ui.html
```

### 操作步驟：
1. 展開 **`Excel 宣告式物件映射測試介面`** 群組標籤。
2. **測試匯出**：
   * 點選 `GET /api/v1/excel/bean-mapping/orders/export` $\to$ 點擊 **`Try it out`** $\to$ **`Execute`**。
   * 下方點擊出現的藍色 **`Download file`** 連結，即可下載 Excel 檔案並以 Excel 軟體開啟檢視。
3. **測試匯入**：
   * 點選 `POST /api/v1/excel/bean-mapping/orders/import` $\to$ 點擊 **`Try it out`**。
   * 在 `file` 欄位選取剛剛下載的 `orders_sample.xlsx` 檔案 $\to$ 點擊 **`Execute`**。
   * 檢視下方 Response Body，確認 Excel 內的資料列已被正確解析為強型別 JSON 結構！

---

## 五、如何自訂並宣告自己的 Model

### 1. 宣告式註解規格

| 註解 | 屬性 | 型別 | 預設值 | 說明 |
| :--- | :--- | :--- | :--- | :--- |
| **`@ExcelSheet`** | `name` / `value` | `String` | `"Sheet1"` | Excel 工作表名稱 |
| | `headerRowIndex` | `int` | `0` | 標題列之行號 (0-based) |
| | `dataRowStartIndex` | `int` | `1` | 資料內容起始之行號 (0-based) |
| **`@ExcelColumn`** | `name` / `value` | `String` | `""` | 欄位標題文字（比對工作表表頭） |
| | `index` | `int` | `-1` | 顯式指定欄位索引 (`-1` 代表依宣告順序) |
| | `format` | `String` | `""` | 格式化規則 (如 `#,##0.00` 或 `yyyy-MM-dd`) |
| | `defaultValue` | `String` | `""` | 當儲存格為空或缺漏時的預設字串值 |
| | `ignore` | `boolean` | `false` | 若為 `true` 則在讀寫時忽略該屬性 |

### 2. 範例：定義自訂客戶資料模型 (CustomerModel)

```java
package com.example.demo.application.shared.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import com.example.demo.application.annotation.ExcelColumn;
import com.example.demo.application.annotation.ExcelSheet;

@ExcelSheet(name = "會員資料表", headerRowIndex = 0, dataRowStartIndex = 1)
public class CustomerModel {

    @ExcelColumn(name = "會員代號", index = 0)
    private String customerId;

    @ExcelColumn(name = "客戶名稱", index = 1, defaultValue = "訪客")
    private String name;

    @ExcelColumn(name = "儲值餘額", index = 2, format = "#,##0.00")
    private BigDecimal balance;

    @ExcelColumn(name = "註冊日期", index = 3, format = "yyyy-MM-dd")
    private LocalDate registeredDate;

    public CustomerModel() {} // 必須具備無參建構子

    // Getter & Setter 略...
}
```

在任何 Service 或 Controller 中即可直接透過 UseCase 調用：
```java
// 匯入
List<CustomerModel> list = importExcelUseCase.importBeans(inputStream, CustomerModel.class);

// 匯出
byte[] excelBytes = exportExcelUseCase.exportBeans(list, CustomerModel.class);
```

