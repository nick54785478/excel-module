# Excel 測試範本清單與使用說明 (Test Templates Guide)

> 📌 **目錄位置**：`doc/template/`  
> 🎯 **用途說明**：本目錄存放各類業務模型在進行 **宣告式物件映射 (Bean Mapping)**、**EasyRules 客製資料檢核** 與 **視覺化「錯誤回標檔」生成** 時所需的標準實體 Excel 測試檔案。

---

## 範本清單總覽 (Template Overview)

| 檔案名稱 | 適用模型 (Model) | 預設工作表名稱 | 測試情境與目的 | 包含列數 | 樣式特色 |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **[`BOM_IMPORT.xlsx`](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/template/BOM_IMPORT.xlsx)** | `BomItem` / `Material` | 多階層BOM展開清單<br>單階BOM匯入清單<br>物料主檔對照表 | 製造業多階 BOM 結構遞迴展開與整批匯入驗證 | 3 工作表<br>(11項料件) | 藍/橙/綠三色專業標題、凍結首列、多層階層縮排標註 |
| **[`ORDER_IMPORT_SUCCESS.xlsx`](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/template/ORDER_IMPORT_SUCCESS.xlsx)** | `OrderModel` (JavaBean) | 訂單匯入清單 | 驗證標準 JavaBean 宣告式匯入與型別映射（全部資料皆合法） | 5 列資料 | 經典深藍表頭 (`#1F4E78`)、千分位金額、白字粗體、凍結首列 |
| **[`ORDER_IMPORT_WITH_ERRORS.xlsx`](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/template/ORDER_IMPORT_WITH_ERRORS.xlsx)** | `OrderModel` (JavaBean) | 訂單匯入清單 | 包含負數金額、超標數量等違規資料，專門用於驗證 EasyRules 檢核與**「錯誤回標檔」生成** | 5 列資料<br>(3 列含違規) | 警戒磚橘表頭 (`#C65911`)、出錯時會被系統套用淡紅底深紅字回標 |
| **[`PRODUCT_IMPORT_SUCCESS.xlsx`](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/template/PRODUCT_IMPORT_SUCCESS.xlsx)** | `ProductRecordModel` (Record) | 商品主檔清單 | 驗證 Java 21 原生不可變 Record 之規範建構子 (Canonical Constructor) 宣告式匯入 | 5 列資料 | 商務深藍表頭 (`#2F5597`)、布林在售狀態、凍結首列 |
| **[`PRODUCT_IMPORT_WITH_ERRORS.xlsx`](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/template/PRODUCT_IMPORT_WITH_ERRORS.xlsx)** | `ProductRecordModel` (Record) | 商品主檔清單 | 包含售價為 0.00 及負數金額之違規資料，測試 Java 21 Record 客製規則檢核 | 4 列資料<br>(2 列含違規) | 警戒磚橘表頭 (`#C65911`)、專門觸發 `INVALID_PRODUCT_PRICE` |
| **[`PO_IMPORT.xlsx`](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/template/PO_IMPORT.xlsx)** | 採購明細 (Purchase Order) | 採購明細表 | 企業標準採購明細匯入（單號、供應商、料號、數量、單價、金額與狀態） | 3 列資料 | 藍底白字、金額千分位、凍結窗格 |

---

## 各範本欄位結構與測試詳情

### 1. `ORDER_IMPORT_SUCCESS.xlsx` (訂單合格匯入範本)
* **對應模型**：[`OrderModel.java`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/application/shared/model/demo/OrderModel.java)
* **欄位結構**：
  | 欄位索引 (0-based) | 欄位名稱 | 範例值 | 型別 | 說明 |
  | :---: | :--- | :--- | :--- | :--- |
  | `0` | 訂單編號 | `ORD-2026-1001` | String | 唯一訂單流水號 |
  | `1` | 訂購人 | `王小明` | String | 客戶姓名 (預設值：匿名訪客) |
  | `2` | 購買金額 | `12500.00` | BigDecimal | 支援千分位格式與貨幣符號容錯 |
  | `3` | 數量 | `2` | Integer | 正整數購買數量 |
  | `4` | 下單日期 | `2026-11-01` | LocalDate | 支援 `yyyy-MM-dd` 格式 |
  | `5` | 訂單狀態 | `PAID` | String | PAID / SHIPPED / PENDING |
* **建議測試 API**：
  * `POST /api/v1/excel/bean-mapping/orders/import`（宣告式解析為 JavaBean）
  * `POST /api/v1/excel/validation/orders`（預檢回傳 `valid: true`, HTTP 200）

---

### 2. `ORDER_IMPORT_WITH_ERRORS.xlsx` (訂單含違規資料範本)
* **測試情境**：驗證 EasyRules 行級規則引擎是否全量捕捉違規事實，並成功生成標註淡紅底深紅字之錯誤回標檔。
* **資料列違規明細**：
  * **第 2 列** (`ORD-2026-E001`)：正常合格資料。
  * **第 3 列** (`ORD-2026-E002`)：`購買金額 = -500.00`（違反 `OrderAmountValidationRule`，金額必須大於 0）。
  * **第 4 列** (`ORD-2026-E003`)：`數量 = 15000`（違反 `OrderQuantityValidationRule`，數量上限 9999）。
  * **第 5 列** (`ORD-2026-E004`)：`購買金額 = 0.00` 且 `數量 = 0`（雙重違規，同時觸發金額與數量規則）。
  * **第 6 列** (`ORD-2026-E005`)：正常合格資料。
* **建議測試 API**：
  * **取得 JSON 錯誤清單**：
    ```bash
    curl -X POST "http://localhost:8080/api/v1/excel/validation/orders" \
         -F "file=@doc/template/ORDER_IMPORT_WITH_ERRORS.xlsx"
    ```
    *回應 HTTP 422 Unprocessable Entity，包含 4 筆錯誤事實。*
  * **下載視覺化「錯誤回標檔」**：
    ```bash
    curl -X POST "http://localhost:8080/api/v1/excel/validation/orders/feedback" \
         -F "file=@doc/template/ORDER_IMPORT_WITH_ERRORS.xlsx" \
         --output "訂單錯誤標註檔.xlsx"
    ```
    *開啟輸出的 `訂單錯誤標註檔.xlsx`，可直接看到出錯儲存格皆已被套用淡紅底深紅字，最右側追加了錯誤原因說明欄！*

---

### 3. `PRODUCT_IMPORT_SUCCESS.xlsx` (商品主檔合格範本)
* **對應模型**：[`ProductRecordModel.java`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/application/shared/model/demo/ProductRecordModel.java)
* **欄位結構**：
  | 欄位索引 | 欄位名稱 | 範例值 | 型別 | 說明 |
  | :---: | :--- | :--- | :--- | :--- |
  | `0` | 商品代碼 | `PROD-A001` | String | 唯一商品料號 |
  | `1` | 商品名稱 | `4K 工業級空拍機` | String | 商品中文品名 |
  | `2` | 商品售價 | `48900.00` | BigDecimal | 售價 (必須大於 0.00) |
  | `3` | 在售狀態 | `true` | Boolean | true / false |
* **建議測試 API**：
  * `POST /api/v1/excel/bean-mapping/products/import`（宣告式解析為 Java 21 Record）
  * `POST /api/v1/excel/validation/products`（預檢通過，HTTP 200）

---

### 4. `PRODUCT_IMPORT_WITH_ERRORS.xlsx` (商品含違規資料範本)
* **資料列違規明細**：
  * **第 3 列** (`PROD-E002`)：`商品售價 = 0.00`（違反 `ProductPriceValidationRule`）。
  * **第 4 列** (`PROD-E003`)：`商品售價 = -1200.00`（違反 `ProductPriceValidationRule`）。
* **建議測試 API**：
  * `POST /api/v1/excel/validation/products`（回傳 HTTP 422 錯誤 JSON）
  * `POST /api/v1/excel/validation/products/feedback`（下載標紅之商品錯誤回標檔）

---

### 5. `BOM_IMPORT.xlsx` (多工作表 BOM 樹狀配方範本)
* **包含工作表**：
  1. `多階層BOM展開清單`：樹狀多階（Level 0 整機 $\to$ Level 1 半成品 $\to$ Level 2 零件），包含單機用量、損耗率、有效用量、小計成本與安裝位號。
  2. `單階BOM匯入清單`：扁平化匯入結構。
  3. `物料主檔對照表`：成品、半成品與原物料之類別、單位與標準單價。
* **建議測試 API**：
  * `POST /api/v1/excel/bom/import`（解析多階樹狀結構並儲存至資料庫）
  * `GET /api/v1/excel/bom/export/MAT-FG-001`（匯出產製對應 BOM 活頁簿）

---

## 相關指引文件
* 客製驗證與錯誤回標測試手冊：[`doc/test/custom_validation_testing_guide.md`](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/test/custom_validation_testing_guide.md)
* 宣告式映射測試實務操作手冊：[`doc/test/declarative_bean_mapping_guide.md`](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/test/declarative_bean_mapping_guide.md)
* 驗證與錯誤回標維護手冊：[`doc/maintenance/custom-validation-and-error-feedback-manual.md`](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/maintenance/custom-validation-and-error-feedback-manual.md)
* 物件映射架構指南：[`doc/design/declarative-bean-mapping.md`](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/design/declarative-bean-mapping.md)

