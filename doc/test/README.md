# Excel 模組與物料清單 (BOM) API 測試手冊 (API Testing Guide)

本目錄存放本專案所有 API 測試資產，支援 **Postman** 與 **OpenAPI 3.0 (Swagger UI)** 兩種主流測試方式。

---

## 一、測試資產檔案清單

| 檔案名稱 | 格式 | 說明與用途 |
| :--- | :---: | :--- |
| [`declarative_bean_mapping_guide.md`](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/test/declarative_bean_mapping_guide.md) | Markdown | **維度一「宣告式物件映射 (Declarative Bean Mapping)」完整測試與使用指引** |
| [`annotation_driven_task_testing_guide.md`](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/test/annotation_driven_task_testing_guide.md) | Markdown | **宣告式長任務監控模組 (@TrackExcelTask) 觸發、進度輪詢與下載完整測試指引** |
| [`custom_validation_testing_guide.md`](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/test/custom_validation_testing_guide.md) | Markdown | **維度三「客製檢核與視覺化錯誤回標檔 (Custom Validation & Error Feedback)」完整測試操作手冊** |
| [`excel_bom_module_postman_collection.json`](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/test/excel_bom_module_postman_collection.json) | JSON (v2.1) | 完整 Postman 測試集合，內建自動斷言腳本（Status Code、Header、檔名、錯誤結構等） |
| [`openapi_specification.yaml`](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/test/openapi_specification.yaml) | YAML (v3.0.3) | OpenAPI 3.0 標準規範檔，可直接匯入 Swagger Editor、Postman 或 APIDog |
| [`README.md`](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/test/README.md) | Markdown | 測試指引與測試矩陣操作文件 |

---

## 二、使用 Postman 進行測試

### 1. 匯入 Collection
1. 開啟 **Postman** 桌面應用程式或 Web 版。
2. 點擊左上角 **Import** 按鈕。
3. 將 [`doc/test/excel_bom_module_postman_collection.json`](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/test/excel_bom_module_postman_collection.json) 拖入或選取匯入。
4. 匯入完成後，集合名稱顯示為 **`Excel & BOM Module API Test Collection`**。

### 2. 環境變數設定
集合內部已內建變數 `baseUrl`：
* **`baseUrl`** 預設值為 `http://localhost:8080`
* 若服務運行於不同埠號或遠端伺服器，可在 Postman Collection 的 **Variables** 標籤頁直接修改。

### 3. 測試集合目錄結構
* **`1. BOM 物料清單模組 (BOM Module)`**：
  1. `GET 下載指定母件多階層 BOM (MAT-FG-001)`：驗證指定成品產製 3 工作表 Excel。
  2. `GET 下載指定母件特定版本 BOM (MAT-FG-001 V1.0)`：驗證帶版本號篩選產製。
  3. `GET 下載全系統所有 BOM 清單 (未指定料號)`：驗證匯出 25 筆 BOM 與 2000 筆料件主檔。
  4. `POST 結構化條件匯出 BOM (僅匯出單階工作表)`：驗證 RequestBody 欄位開關 (`includeMultiLevel=false`)。
  5. `GET 大數據串流下載 BOM Excel (邊產製邊傳輸)`：邊產製邊以 `flushInterval` 沖刷二進位串流，常數級極低記憶體消耗。
  6. `POST 結構化條件大數據串流下載 BOM (帶入 flushInterval)`：透過 POST JSON RequestBody 設定篩選與沖刷間隔。
  7. `GET 異常測試 - 查無母件料號 (404 Not Found)`：驗證不存在料號時的錯誤結構回應。
* **`2. Excel 標準處理模組 (Excel Core Module)`**：
  1. `POST 標準批次匯出 Excel (In-Memory)`：傳入自訂行列資料，即時產製 `.xlsx`。
  2. `POST 大數據百萬級串流匯出 (Streaming Flush)`：串流邊寫入邊 Flush，維持極低記憶體消耗。
  3. `POST 檔案上傳並完整解析 Excel (Multipart)`：上傳 `.xlsx` 並解析為 JSON。
  4. `POST 異常測試 - 匯出空白檔名 (400 Bad Request)`：驗證 Spring Validation 參數攔截。
* **`3. Excel 宣告式物件映射模組 (Declarative Bean Mapping)`**：
  * 詳細指引參閱：[`declarative_bean_mapping_guide.md`](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/test/declarative_bean_mapping_guide.md)
  1. `POST 自訂 JSON 匯出訂單 Excel (JavaBean)`：驗證 RequestBody 轉 OrderModel 匯出。
  2. `POST 大數據串流匯出訂單 Excel (JavaBean)`：驗證常數記憶體串流直沖 Servlet Response。
  3. `POST 上傳 Excel 並宣告式解析為 OrderModel 清單`：驗證 Multipart 上傳自動映射 JavaBean。
  4. `POST 自訂 JSON 匯出商品主檔 (Java 21 Record)`：驗證 Record 批次資料匯出。
  5. `POST 上傳 Excel 並宣告式解析為 Record 清單`：驗證 Multipart 自動映射 Java 21 Record。
* **`4. Excel 宣告式長任務監控模組 (Annotation-Driven Task Tracking)`**：
  * 詳細指引參閱：[`annotation_driven_task_testing_guide.md`](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/test/annotation_driven_task_testing_guide.md)
  1. `POST 結構化條件大數據串流下載 BOM (觸發長任務與 flushInterval)`：驗證 `@TrackExcelTask` 切面自動攔截、建檔並同步串流沖刷進度。
  2. `GET 查詢 Excel 長任務進度與狀態 (/progress)`：驗證任務執行階段即時輪詢 (`PENDING` $\to$ `PROCESSING` $\to$ `COMPLETED`)、百分比與已處理列數。
  3. `GET 下載已完成長任務之產出 Excel 檔案 (/download)`：驗證任務完成後二進位產物安全下載。
  4. `GET 異常驗證 - 查詢不存在之任務 (404 Not Found)`：驗證查無任務時之全域錯誤回應。
* **`5. Excel 客製驗證與錯誤回標模組 (Custom Validation & Error Feedback)`**：
  * 詳細指引參閱：[`custom_validation_testing_guide.md`](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/test/custom_validation_testing_guide.md)
  1. `GET 查詢已註冊可用範本清單 (/templates)`：驗證系統註冊之所有單據元數據。
  2. `POST 統一動態範本錯誤回標檔生成 (/{templateKey}/feedback)`：支援 `orders`, `products`, `materials` 等，驗證淡紅底深紅字回標檔下載。
  3. `POST 統一動態範本 JSON 檢核統計 (/{templateKey})`：驗證全量非短路累積錯誤事實 (HTTP 200 / 422)。
  4. `POST Zero-Config 智慧自動探測回標檔 (/feedback)`：免傳 Key，依 Sheet Name 自動探測並回標。
  5. `POST 宣告式 AOP 自動回標攔截匯入 (/declarative/orders)`：驗證 `@ValidateExcel` 切面短路防守與合規清單自動注入。
* **`6. 物料與 BOM 聚合宣告式 Excel 映射模組 (Material & BOM Mapping)`**：
  * 控制器：[`MaterialBomExcelController`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/presentation/controller/MaterialBomExcelController.java) (`/api/v1/excel/material-bom`)
  1. `GET 匯出系統現有物料主檔 Excel (/materials/export)`：直接查詢 Material 聚合根，一鍵下載 `materials_master.xlsx`。
  2. `POST 上傳物料 Excel 宣告式純解析 (/materials/import)`：反射反序列化為 `MaterialExcelModel` JSON。
  3. `GET 匯出系統現有單階 BOM 配方 Excel (/bom-items/export)`：直接查詢 BOM 聚合根，一鍵下載 `bom_items_master.xlsx`。
  4. `POST 上傳 BOM 配方 Excel 宣告式純解析 (/bom-items/import)`：反射反序列化為 `BomItemExcelModel` JSON。

### 4. 執行自動化測試 (Collection Runner)
點選集合右側的 **`Run collection`**，可一鍵自動執行全部 20 個請求，所有測試腳本（`pm.test`）將自動驗證狀態碼、標頭與回應內容。

---

## 三、使用 OpenAPI / Swagger UI 進行測試

Spring Boot 服務啟動後，已自動整合 SpringDoc OpenAPI 3.0，可直接透過瀏覽器開啟視覺化互動介面：

* **Swagger UI 介面 URL**：
  ```
  http://localhost:8080/swagger-ui.html
  ```
* **OpenAPI 3.0 JSON 規格 URL**：
  ```
  http://localhost:8080/v3/api-docs
  ```

### 介面操作步驟：
1. 瀏覽器開啟 `http://localhost:8080/swagger-ui.html`。
2. 展開 **`BOM 物料清單介面`** $\to$ 點選 `GET /api/v1/bom/excel`。
3. 點擊右上角 **`Try it out`**。
4. 在 `parentMaterialCode` 輸入 `MAT-FG-001`，點擊 **`Execute`**。
5. 下方 Response 將顯示 `200 OK`，並出現 **`Download file`** 藍色超連結，點擊即可將產製之 Excel 下載至電腦檢視！

---

## 四、使用 cURL 指令快速測試

### 1. 下載 MAT-FG-001 多階層 BOM Excel
```bash
curl -i -X GET "http://localhost:8080/api/v1/bom/excel?parentMaterialCode=MAT-FG-001" -O -J
```
> 註：`-O -J` 參數會自動依照後端 `Content-Disposition` 標頭儲存為 `BOM_MAT-FG-001.xlsx`。

### 2. 下載全系統所有 BOM 清單
```bash
curl -i -X GET "http://localhost:8080/api/v1/bom/excel" -O -J
```

### 3. POST 結構化條件下載 (只產製單階工作表)
```bash
curl -i -X POST "http://localhost:8080/api/v1/bom/excel/export" \
     -H "Content-Type: application/json" \
     -d '{
       "parentMaterialCode": "MAT-FG-001",
       "version": "V1.0",
       "includeMultiLevel": false,
       "includeSingleLevel": true,
       "includeMaterialMaster": false
     }' \
     -O -J
```

### 4. GET 大數據邊讀邊沖刷串流下載 (GET /stream)
```bash
curl -i -X GET "http://localhost:8080/api/v1/bom/excel/stream?parentMaterialCode=MAT-FG-001&flushInterval=500" -O -J
```

### 5. POST 大數據結構化串流下載 (POST /stream)
```bash
curl -i -X POST "http://localhost:8080/api/v1/bom/excel/stream" \
     -H "Content-Type: application/json" \
     -d '{
       "parentMaterialCode": "MAT-FG-001",
       "version": "1.0",
       "includeMultiLevel": true,
       "includeSingleLevel": true,
       "includeMaterialMaster": true,
       "flushInterval": 500
     }' \
     -O -J
```

### 6. 異常驗證：查無母件料號 (404 Not Found)
```bash
curl -i -X GET "http://localhost:8080/api/v1/bom/excel?parentMaterialCode=NON_EXISTENT_MATERIAL"
```
**預期回應**：
```json
{
  "timestamp": "2026-10-31T19:25:00",
  "status": 404,
  "error": "Not Found",
  "message": "查無指定料號之物料主檔: NON_EXISTENT_MATERIAL",
  "errors": [
    {
      "field": "parentMaterialCode",
      "rejectedValue": null,
      "message": "查無指定料號之物料主檔: NON_EXISTENT_MATERIAL"
    }
  ]
}
```

---

## 五、預載測試資料料號對照表

本模組於資料庫初始化時已自動注入 2000 筆物料主檔與 25 筆 BOM 配方（定義於 [`material_bom_test_data.sql`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/resources/sql/material_bom_test_data.sql)），測試時可使用下列料號：

| 母件物料料號 | 物料品名 | 規格型號 | 階層結構深度 | 子項料件數 |
| :--- | :--- | :--- | :---: | :---: |
| **`MAT-FG-001`** | 工業級四軸無人機旗艦套件 | UAV-450-PRO | 3 階（FG $\to$ SA $\to$ RM） | 4 個主要模組總成，共展開 11 項料件 |
| **`MAT-FG-002`** | 智慧農業高壓植保無人機總成 | AGRI-DRONE-V2 | 3 階（FG $\to$ SA $\to$ RM） | 包含動力總成、大容量藥箱與高壓噴頭 |
| **`MAT-FG-003`** | 專業航拍雙光熱成像雲台無人機 | THERMAL-UAV-X8 | 3 階（FG $\to$ SA $\to$ RM） | 包含熱成像與 4K 雙光雲台 |
| **`MAT-SA-001`** | 無人機飛控核心運算主板模組 | FC-STM32H7-REV2 | 2 階（SA $\to$ RM） | 包含 STM32H7 晶片、六軸陀螺儀與螺絲 |
| **`MAT-SA-002`** | 4K 雙軸光學防手震雲台航拍相機總成 | GIMBAL-4K-OIS | 2 階（SA $\to$ RM） | 包含 SONY CMOS 感光元件與步進馬達 |
| **`MAT-SA-003`** | 450 級高性能動力總成模組 (馬達+電變+槳) | POWER-SYS-450KV | 2 階（SA $\to$ RM） | 包含無刷馬達、電子調速器與螺旋槳 |

---

## 六、底層 Excel 引擎動態切換指南 (FastExcel vs Apache POI)

本系統支援六角形架構動態適配器切換，可透過 `application.properties` 或環境變數自由切換底層 Excel 處理引擎：

### 1. 設定檔配置項 (`application.properties`)
```properties
# Excel 底層處理引擎配置 (可選值: FAST_EXCEL / APACHE_POI)
# 預設值為 FAST_EXCEL (若未設定則自動回退為 FAST_EXCEL)
excel.engine=FAST_EXCEL
```

### 2. 引擎特性與使用情境比較
| 引擎選項 | 底層技術實現 | 核心優勢 | 適用情境 |
| :--- | :--- | :--- | :--- |
| **`FAST_EXCEL`**（預設） | FastExcel (StAX 串流) | 極致吞吐量、毫秒級純記憶體產製、超低延遲 | 百萬級大數據高併發產製、大資料量排程匯出 |
| **`APACHE_POI`** | Apache POI SXSSF / SAX | 工業級豐富樣式支援、凍結窗格、磁碟快取緩衝 | 複雜商業樣式、大型試算表格式相容、企業級標準格式 |

### 3. 動態注入機制
* 表現層 Controller 與應用層 UseCase 完全依賴抽象連接埠 `ExcelWriterPort` 與 `ExcelReaderPort`。
* 配置層 `ExcelEngineConfiguration` 依據 `@ConditionalOnProperty(name = "excel.engine", ...)` 在 Spring Boot 啟動時動態注入對應的 Outbound Adapter（`FastExcelWriterAdapter`/`PoiSxssfWriterAdapter` 與 `FastExcelReaderAdapter`/`PoiSaxReaderAdapter`），業務程式碼 100% 零修改。


