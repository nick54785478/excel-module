# Excel 宣告式物件映射測試指引與操作手冊 (Declarative Bean Mapping Test Guide)

> 📌 **文件位置**：`doc/test/declarative_bean_mapping_guide.md`  
> 🎯 **適用對象**：測試工程師 (QA)、前端開發人員、第三方系統整合人員  
> 📖 **相關文件**：
> * 業務開發與模型定義手冊：[`doc/maintenance/declarative-bean-mapping-manual.md`](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/maintenance/declarative-bean-mapping-manual.md)
> * 底層架構與設計指南：[`doc/design/declarative-bean-mapping.md`](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/design/declarative-bean-mapping.md)

---

## 目錄 (Table of Contents)
- [1. REST API 測試端點總覽](#1-rest-api-測試端點總覽)
- [2. 端到端測試情境與 cURL 操作範例](#2-端到端測試情境與-curl-操作範例)
  - [情境 1：標準 JavaBean 模式 (訂單管理 - OrderModel)](#情境-1標準-javabean-模式-訂單管理---ordermodel)
  - [情境 2：Java 21 Record 模式 (商品主檔 - ProductRecordModel)](#情境-2java-21-record-模式-商品主檔---productrecordmodel)
- [3. 使用 Swagger UI 進行視覺化介面實測](#3-使用-swagger-ui-進行視覺化介面實測)
- [4. 自動化測試驗證 (JUnit 5 & MockMvc)](#4-自動化測試驗證-junit-5--mockmvc)

---

## 1. REST API 測試端點總覽

本模組提供專職測試控制器 [`ExcelBeanMappingController`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/presentation/controller/core/ExcelBeanMappingController.java)，支援透過 HTTP 呼叫驗證雙軌宣告式映射機制。

* **基礎路徑 (Base URL)**：`http://localhost:8080/api/v1/excel/bean-mapping`

| HTTP 方法 | 端點路徑 (Endpoint) | 功能說明 | 請求資料 (Request) | 回應內容 (Response) |
| :---: | :--- | :--- | :--- | :--- |
| **POST** | `/orders/export` | 自訂 JSON 資料宣告式匯出 (JavaBean) | `ExportOrderResource` (JSON) | `.xlsx` 檔案二進位下載 |
| **POST** | `/orders/stream-export` | 訂單大數據串流直接沖刷匯出 (JavaBean) | `ExportOrderResource` (JSON) | `.xlsx` HTTP 輸出流直沖 |
| **POST** | `/orders/import` | 上傳 Excel 並宣告式解析為 JavaBean | `file` (MultipartFile) | `OrderImportedResource` (JSON) |
| **POST** | `/products/export` | 自訂 JSON 資料宣告式匯出 (Java 21 Record) | `ExportProductResource` (JSON) | `.xlsx` 檔案二進位下載 |
| **POST** | `/products/import` | 上傳 Excel 並宣告式解析為 Record | `file` (MultipartFile) | `ProductImportedResource` (JSON) |

---

## 2. 端到端測試情境與 cURL 操作範例

### 情境 1：標準 JavaBean 模式 (訂單管理 - OrderModel)

#### 1-1. 自訂 JSON 匯出訂單 Excel
調用端指定工作表名稱與訂單明細資料，驗證數值格式化（`#,##0.00`）與日期格式化：

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
* **驗證重點**：
  * 下載之 `vip_orders.xlsx` 是否包含工作表 `2026年11月特約訂單`。
  * 第一列表頭應為：`訂單編號`、`客戶名稱`、`購買金額`、`購買數量`、`訂單日期`、`訂單狀態`。
  * 購買金額欄位應帶有千分位格式。

---

#### 1-2. 檔案上傳並宣告式解析為 JavaBean (POST /orders/import)
上傳剛才生成的 `vip_orders.xlsx`，驗證後端是否正確反序列化為 `OrderModel` 物件：

```bash
curl -X POST "http://localhost:8080/api/v1/excel/bean-mapping/orders/import" \
     -H "Content-Type: multipart/form-data" \
     -F "file=@vip_orders.xlsx"
```

* **預期回應 (HTTP 200 OK)**：
```json
{
  "totalCount": 2,
  "sheetName": "2026年11月特約訂單",
  "items": [
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
}
```

---

#### 1-3. 訂單大數據串流沖刷匯出 (POST /orders/stream-export)
驗證大數據串流匯出模式下之 HTTP 分塊傳輸與常數級記憶體沖刷機制：

```bash
curl -X POST "http://localhost:8080/api/v1/excel/bean-mapping/orders/stream-export" \
     -H "Content-Type: application/json" \
     -d '{
       "sheetName": "串流訂單清單",
       "orders": [
         {
           "orderNo": "ORD-STREAM-001",
           "customerName": "王小明",
           "amount": 520.00,
           "quantity": 10,
           "orderDate": "2026-11-05",
           "status": "COMPLETED"
         }
       ]
     }' \
     -o "orders_stream.xlsx"
```

---

### 情境 2：Java 21 Record 模式 (商品主檔 - ProductRecordModel)

#### 2-1. 自訂 JSON 匯出商品主檔
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

#### 2-2. 上傳 Excel 並宣告式解析為 Java 21 Record (POST /products/import)
```bash
curl -X POST "http://localhost:8080/api/v1/excel/bean-mapping/products/import" \
     -H "Content-Type: multipart/form-data" \
     -F "file=@flagship_products.xlsx"
```

* **預期回應 (HTTP 200 OK)**：
```json
{
  "totalCount": 2,
  "sheetName": "旗艦商品主檔",
  "items": [
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
}
```

---

## 3. 使用 Swagger UI 進行視覺化介面實測

啟動 Spring Boot 應用後，打開瀏覽器訪問：
```
http://localhost:8080/swagger-ui.html
```

### 測試步驟：
1. 展開 **`Excel 宣告式物件映射測試介面`** 標籤。
2. **測試匯出**：
   * 點選 `POST /api/v1/excel/bean-mapping/orders/export` $\to$ 點擊 **`Try it out`**。
   * 輸入 JSON 請求主體 $\to$ 點擊 **`Execute`**。
   * 下方點擊藍色 **`Download file`** 連結，下載產生之 Excel 檔案並確認欄位排版。
3. **測試匯入**：
   * 點選 `POST /api/v1/excel/bean-mapping/orders/import` $\to$ 點擊 **`Try it out`**。
   * 在 `file` 欄位選取剛下載的 Excel 檔案 $\to$ 點擊 **`Execute`**。
   * 檢視下方 Response Body，確認二維儲存格已被精準反序列化為強型別 JSON 結構！

---

## 4. 自動化測試驗證 (JUnit 5 & MockMvc)

本模組已涵蓋完整的端到端與組裝器單元測試，執行以下指令驗證：

```bash
./mvnw test -Dtest=ExcelBeanMappingControllerTest,ExcelBeanAssemblerTest
```

### 核心測試案例清單：
1. **[`ExcelBeanAssemblerTest`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/test/java/com/example/demo/application/assembler/ExcelBeanAssemblerTest.java) (14 項單元測試)**：
   * 驗證 JavaBean 模式下反射讀寫與 Getter/Setter 存取。
   * 驗證 Java 21 Record 模式下規範建構子 Canonical Constructor 自動依序組裝。
   * 驗證千分位數值（如 `"12,500.50"`）與特殊貨幣符號容錯解析。
   * 驗證日期自訂格式（`yyyy-MM-dd`）、布林多格式容錯與預設值自動補齊。
2. **[`ExcelBeanMappingControllerTest`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/test/java/com/example/demo/presentation/controller/core/ExcelBeanMappingControllerTest.java) (8 項整合測試)**：
   * 驗證匯入與匯出 API 之 HTTP 200 狀態碼、MIME Type 與 Content-Disposition。
   * 驗證 Spring Validation 參數校驗（檔案為空或訂單清單為空時回傳 400 Bad Request）。
