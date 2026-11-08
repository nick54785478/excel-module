# 維度一 Part 2：應用層 UseCase、CQRS 服務調度與 REST 端點 (UseCases, CQRS Services & REST Endpoints)

> 📌 本文件座落於 [`doc/design/declarative-bean-mapping/02-ports-usecases-and-rest-binding.md`](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/design/declarative-bean-mapping/02-ports-usecases-and-rest-binding.md)，屬於「維度一：宣告式物件映射」之【Part 2：應用層 UseCase、CQRS 服務調度與 REST 端點】。  
> 詳述六角形架構 Inbound Ports、CQRS 分離的 Application Services、純淨 Outbound Ports、REST 表現層控制器與單元測試。  
> 
> 🔗 **模組導讀與關聯文件**：  
> • 模組入口主文件：👉 [維度一：宣告式物件映射架構總覽 (declarative-bean-mapping.md)](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/design/declarative-bean-mapping.md)  
> • 註解與反射快取：👉 [維度一 Part 1：宣告式註解、雙層反射快取與型別容錯轉換 (01-reflection-cache-and-converters.md)](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/design/declarative-bean-mapping/01-reflection-cache-and-converters.md)

---

## 1. 應用層 Inbound Ports 與 CQRS Services

座落於 [`com.example.demo.application`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/application)：

### 1.1 Inbound Ports (Use Cases)
* [`ExportExcelUseCase`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/application/port/in/ExportExcelUseCase.java)：`<T> byte[] exportBeans(List<T> dataList, Class<T> modelClass)`
* [`StreamExportExcelUseCase`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/application/port/in/StreamExportExcelUseCase.java)：`<T> void streamExportBeans(OutputStream os, List<T> dataList, Class<T> modelClass)`
* [`ImportExcelUseCase`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/application/port/in/ImportExcelUseCase.java)：`<T> List<T> importBeans(InputStream is, Class<T> modelClass)`
* [`StreamImportExcelUseCase`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/application/port/in/StreamImportExcelUseCase.java)：`<T> void streamImportBeans(InputStream is, ReadExcelQuery query, Class<T> modelClass, int chunkSize, Consumer<List<T>> chunkConsumer)`

### 1.2 Application Services (CQRS 分離)
* [`ExcelExportApplicationService`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/application/service/ExcelExportApplicationService.java)：呼叫 `ExcelBeanAssembler` 將 Model 清單轉為 `WriteExcelCommand`，再委派 `ExcelWriterPort`。
* [`ExcelImportApplicationService`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/application/service/ExcelImportApplicationService.java)：呼叫 `ExcelReaderPort` 讀取字典資料，再委派 `ExcelBeanAssembler` 反序列化為強型別 Model 清單。

---

## 2. Outbound Ports 純淨隔離

* [`ExcelWriterPort`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/application/port/out/ExcelWriterPort.java)：維持純粹 Command/I/O 技術契約，**嚴禁依賴任何 Assembler 實作**。
* [`ExcelReaderPort`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/application/port/out/ExcelReaderPort.java)：維持純粹讀取與串流回呼契約。

---

## 3. 表現層端點：`ExcelBeanMappingController`

座落於 [`com.example.demo.presentation.controller.ExcelBeanMappingController`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/presentation/controller/ExcelBeanMappingController.java)：

| HTTP 方法 | 端點路徑 | 說明與用途 |
| :--- | :--- | :--- |
| `POST` | `/api/v1/excel/beans/orders/export` | 傳入訂單 JSON，宣告式匯出為訂單 Excel (JavaBean) |
| `POST` | `/api/v1/excel/beans/orders/stream-export` | 串流匯出訂單 Excel，直沖 Servlet Response |
| `POST` | `/api/v1/excel/beans/orders/import` | 上傳訂單 Excel，宣告式純解析為 OrderModel 清單 |
| `POST` | `/api/v1/excel/beans/products/export` | 傳入商品清單，宣告式匯出商品 Excel (Java 21 Record) |
| `POST` | `/api/v1/excel/beans/products/import` | 上傳商品 Excel，宣告式純解析為 ProductRecordModel 清單 |

---

## 4. 單元與整合測試驗證 (Testing & Verification)

* [`ExcelBeanAssemblerTest`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/test/java/com/example/demo/application/assembler/ExcelBeanAssemblerTest.java)：驗證反射快取、POJO 與 Record 雙軌轉換、千分位容錯、公式與標題解析。
* [`ExcelBeanMappingControllerTest`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/test/java/com/example/demo/presentation/controller/ExcelBeanMappingControllerTest.java)：驗證 5 隻端點之 JSON 匯出、串流產製與 Multipart 上傳反序列化。

