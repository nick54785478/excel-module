# 維度四 Part 2：JPA 持久化、REST 端點與前端非同步閉環 (JPA Persistence, REST API & Frontend Lifecycle)

> 📌 本文件座落於 [`doc/design/annotation-driven-task-tracking/02-persistence-api-and-frontend-lifecycle.md`](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/design/annotation-driven-task-tracking/02-persistence-api-and-frontend-lifecycle.md)，屬於「維度四：非同步長任務與進度追蹤」之【Part 2：JPA 持久化、REST 端點與前端非同步閉環】。  
> 詳述基礎設施層 JPA 技術實體、獨立事務儲存庫適配器 (`Propagation.REQUIRES_NEW`)、REST 表現層輪詢與檔案下載 API、前端閉環整合流程以及單元與整合測試驗證。  
> 
> 🔗 **模組導讀與關聯文件**：  
> • 模組入口主文件：👉 [維度四：非同步長任務與進度追蹤架構總覽 (annotation-driven-task-tracking.md)](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/design/annotation-driven-task-tracking.md)  
> • AOP 與上下文規範：👉 [維度四 Part 1：宣告式註解、AOP 切面生命週期與執行緒上下文保護 (01-aop-lifecycle-and-context-protection.md)](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/design/annotation-driven-task-tracking/01-aop-lifecycle-and-context-protection.md)

---

## 1. 基礎設施層技術實體與儲存庫 (Persistence Layer)

依據 Clean Architecture 與工作區規範，長任務追蹤純屬**技術性基礎設施職責**，嚴格封裝於 `infrastructure/persistence/task/`，不污染核心業務 Domain Layer。

### 1.1 JPA ORM 技術實體：`ExcelTaskJpaEntity`
座落於 [`com.example.demo.infrastructure.persistence.task.entity.ExcelTaskJpaEntity`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/infrastructure/persistence/task/entity/ExcelTaskJpaEntity.java)：

```java
@Entity
@Table(name = "excel_task")
public class ExcelTaskJpaEntity {

    @Id
    @Column(name = "task_id", length = 36, nullable = false)
    private String taskId;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", length = 20, nullable = false)
    private ExcelTaskType taskType;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_status", length = 20, nullable = false)
    private ExcelTaskStatus taskStatus;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "total_rows")
    private Long totalRows;

    @Column(name = "processed_rows")
    private Long processedRows;

    @Column(name = "progress_percentage")
    private Double progressPercentage;

    @Column(name = "file_url", length = 500)
    private String fileUrl;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

### 1.2 Outbound Adapter 與獨立事務隔離：`ExcelTaskRepositoryAdapter`
座落於 [`com.example.demo.infrastructure.adapter.task.ExcelTaskRepositoryAdapter`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/infrastructure/adapter/task/ExcelTaskRepositoryAdapter.java)：
* **關鍵技術設計**：所有進度更新與狀態寫入方法均標註 `@Transactional(propagation = Propagation.REQUIRES_NEW)`。
* **效益**：確保即使外層業務事務回滾，長任務的進度更新與錯誤狀態（`FAILED`）依然能即時寫入資料庫，防止狀態卡死。

---

## 2. 應用層合約與 CQRS (Application Layer)

* **Inbound Ports (Use Cases)**：
  * [`GetExcelTaskUseCase`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/application/port/in/task/GetExcelTaskUseCase.java)：依 `taskId` 查詢進度 DTO。
  * [`DownloadExcelTaskFileUseCase`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/application/port/in/task/DownloadExcelTaskFileUseCase.java)：取得完成產物二進位串流。
  * [`SubmitAsyncExcelExportUseCase`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/application/port/in/task/SubmitAsyncExcelExportUseCase.java)
  * [`SubmitAsyncExcelImportUseCase`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/application/port/in/task/SubmitAsyncExcelImportUseCase.java)
* **Outbound Port**：
  * [`ExcelTaskRepositoryPort`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/application/port/out/task/ExcelTaskRepositoryPort.java)：純資料契約，定義 `createTask`, `updateProgress`, `markCompleted`, `markFailed`, `findTaskById`。

---

## 3. 表現層端點與前端閉環 (Frontend Integration)

座落於 [`com.example.demo.presentation.controller.ExcelTaskController`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/presentation/controller/task/ExcelTaskController.java)：

```
[前端 Client]                         [後端 Excel 模組]
      │                                       │
      ├───── 1. 提交業務請求 (例如匯出報表) ────┤
      │                                       │ (切面生成 UUID taskId，背景執行)
      │<──── 返回 HTTP 202 (帶有 taskId) ─────┤
      │                                       │
      │   ┌── 2. 定期輪詢進度 (Polling) ───┐   │
      ├───┼─> GET /api/v1/excel/tasks/{id}/progress
      │   └── <── 狀態: PROCESSING, 進度: 65%  │
      │                                       │ (背景完成，標記 COMPLETED)
      │                                       │
      ├───── 3. 偵測到 COMPLETED ────────────┤
      │                                       │
      ├───── 4. 下載檔案 ─────────────────────┤
      │      GET /api/v1/excel/tasks/{id}/download
      │<──── 回傳 Content-Disposition 二進位流 ┤
```

### 3.1 進度查詢 API
* **端點**：`GET /api/v1/excel/tasks/{taskId}/progress`
* **回應範例** (`HTTP 200 OK`)：
```json
{
  "taskId": "c4b9d038-e6d2-43bb-a5a4-03a557b7f520",
  "taskType": "EXPORT",
  "taskStatus": "PROCESSING",
  "totalRows": 50000,
  "processedRows": 25000,
  "progressPercentage": 50.0,
  "fileName": "BigDataReport.xlsx",
  "fileUrl": null,
  "errorMessage": null,
  "createdAt": "2026-11-04T00:30:00",
  "updatedAt": "2026-11-04T00:30:15"
}
```

### 3.2 產物下載 API
* **端點**：`GET /api/v1/excel/tasks/{taskId}/download`
* **回應**：`HTTP 200 OK`，`Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`，支援檔名 UTF-8 URL 編碼。

---

## 4. 單元與整合測試驗證 (Testing & Verification)

* [`ExcelTaskTrackingAspectTest`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/test/java/com/example/demo/infrastructure/aop/ExcelTaskTrackingAspectTest.java)：驗證同步任務自動建檔、狀態變更、進度記錄、例外標記 `FAILED`、`async = true` 立即返回以及 FastExcel / POI 串流沖刷進度自動回報。
* [`ExcelTaskControllerTest`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/test/java/com/example/demo/presentation/controller/task/ExcelTaskControllerTest.java)：驗證進度輪詢正常回傳、`404 Not Found` 查無任務防護以及二進位檔案串流下載。

