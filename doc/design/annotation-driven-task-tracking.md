# 維度四：非同步長任務與進度追蹤架構總覽 (Dimension 4: Annotation-Driven Task Tracking Hub)

> 📌 本文件座落於 [`doc/design/annotation-driven-task-tracking.md`](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/design/annotation-driven-task-tracking.md)，作為 `excel-module` 企業級擴充藍圖 **「維度四：非同步長任務與進度追蹤」** 的核心入口與子模組總覽導讀。  
> 總體架構請參閱：👉 [Excel 模組架構設計與演進藍圖 (architecture-design.md)](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/design/architecture-design.md)。

---

## 1. 子模組定位與設計哲學 (Sub-Module Vision & Architecture)

在處理數十萬至百萬筆大數據的 Excel 匯入與匯出時，傳統將狀態管理硬編碼於業務服務的方式，常造成重複樣板代碼、狀態卡死（Zombie Task）以及技術實體污染 Domain 層等嚴重缺陷。

維度四基於 **Clean Architecture 六角形架構**、**Spring AOP** 與 **宣告式註解驅動 (`@TrackExcelTask`)**，打造全自動化的非同步長任務生命週期追蹤機制：
1. **極致非侵入 (Zero-Boilerplate)**：標註 `@TrackExcelTask` 即自動接管建檔、狀態轉換、成功標記與全域例外兜底。
2. **架構合規與層級隔離**：長任務技術實體封裝於 Infrastructure Layer (`infrastructure/persistence/task/`)，不污染核心業務 Domain Layer。
3. **智慧上下文傳遞 (Task Context Joining)**：透過 `ThreadLocal` 提供巢狀防覆蓋保護，內外層透明協同。
4. **串流沖刷自動同步**：FastExcel / POI SXSSF 執行 `flush()` 沖刷記憶體時，自動向上下文累積已處理列數。
5. **完整前端輪詢與下載閉環**：標準化進度查詢與二進位產物下載 REST API。

```mermaid
flowchart TD
    subgraph Presentation_Layer ["Presentation Layer (表現層 - Inbound Adapter)"]
        ETC["ExcelTaskController<br/>(GET /progress, GET /download)"]
        ETRA["ExcelTaskResourceAssembler"]
    end

    subgraph Application_Layer ["Application Layer (應用層 - 契約、註解與上下文)"]
        direction TB
        subgraph Annotations ["Annotation"]
            TET["@TrackExcelTask"]
        end
        subgraph VOs ["Value Objects"]
            ETT["ExcelTaskType (IMPORT / EXPORT)"]
            ETS["ExcelTaskStatus (PENDING / PROCESSING / COMPLETED / FAILED)"]
        end
        subgraph Context ["Context Holder"]
            ETCH["ExcelTaskContextHolder (ThreadLocal)"]
        end
        subgraph Ports_In ["Inbound Ports"]
            GETU["GetExcelTaskUseCase"]
        end
        subgraph Ports_Out ["Outbound Ports"]
            ETRP["ExcelTaskRepositoryPort"]
        end
    end

    subgraph Infrastructure_Layer ["Infrastructure Layer (基礎設施層 - 切面與持久化)"]
        direction TB
        subgraph AOP ["AOP 切面"]
            ETTA["ExcelTaskTrackingAspect"]
        end
        subgraph Adapters_Out ["Outbound Adapters"]
            ETRA_Impl["ExcelTaskRepositoryAdapter<br/>(Propagation.REQUIRES_NEW)"]
            FEWA["FastExcelWriterAdapter"]
            PSWA["PoiSxssfWriterAdapter"]
        end
        subgraph Persistence ["JPA 持久化"]
            ETJE["ExcelTaskJpaEntity"]
            ETJR["ExcelTaskJpaRepository"]
        end
    end

    ETC -->|調用| GETU
    ETC -->|DTO轉譯| ETRA
    ETTA -.->|攔截| TET
    ETTA -->|建檔/狀態維護| ETRP
    ETTA -->|綁定/釋放| ETCH
    ETRP -.->|實作| ETRA_Impl
    ETRA_Impl -->|ORM 操作| ETJR
    ETJR --> ETJE
    FEWA -.->|串流 Flush 時回報| ETCH
    PSWA -.->|硬碟沖刷時回報| ETCH
```

---

## 2. 專題子文件導讀與跳轉索引 (Specialized Documentation Navigation)

本子模組之詳細技術規格已依職責切分收納於專屬目錄 [`doc/design/annotation-driven-task-tracking/`](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/design/annotation-driven-task-tracking/) 中：

| 專題技術文件 | 核心關注點 | 適合閱讀對象 | 涵蓋核心元件 |
| :--- | :--- | :--- | :--- |
| 👉 **[Part 1：宣告式註解、AOP 切面生命週期與執行緒上下文保護 (01-aop-lifecycle-and-context-protection.md)](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/design/annotation-driven-task-tracking/01-aop-lifecycle-and-context-protection.md)** | **切面攔截與上下文機制**<br/>• `@TrackExcelTask` 參數與 SpEL 解析<br/>• 切面環繞通知與全域例外兜底<br/>• `ExcelTaskContextHolder` 防覆蓋保護<br/>• 串流 Flush 自動推進進度 | 架構師、AOP 核心開發者、串流效能調優人員 | `@TrackExcelTask`<br/>`ExcelTaskTrackingAspect`<br/>`ExcelTaskContextHolder`<br/>`FastExcelWriterAdapter`<br/>`PoiSxssfWriterAdapter` |
| 👉 **[Part 2：JPA 持久化、REST 端點與前端非同步閉環 (02-persistence-api-and-frontend-lifecycle.md)](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/design/annotation-driven-task-tracking/02-persistence-api-and-frontend-lifecycle.md)** | **持久化儲存與前端閉環**<br/>• JPA 技術實體與獨立事務適配器<br/>• `Propagation.REQUIRES_NEW` 狀態隔離<br/>• 進度輪詢與檔案下載 API<br/>• 前端非同步兩步驟整合閉環 | 業務應用開發者、前端工程師、API 串接者 | `ExcelTaskJpaEntity`<br/>`ExcelTaskJpaRepository`<br/>`ExcelTaskRepositoryAdapter`<br/>`ExcelTaskController`<br/>`GetExcelTaskUseCase` |

---

## 3. 六角形架構端到端元件矩陣 (Component & Port Matrix)

| 分層 (Layer) | 元件名稱 / 介面 | 角色與職責 | AGENTS.md 命名規範 |
| :--- | :--- | :--- | :--- |
| **Presentation** | [`ExcelTaskController`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/presentation/controller/ExcelTaskController.java) | 對外提供 `GET /{id}/progress` 與 `GET /{id}/download` API | Controller |
| **Presentation** | [`ExcelTaskResourceAssembler`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/presentation/assembler/ExcelTaskResourceAssembler.java) | ACL 防腐層，負責 Task DTO 轉 Resource | Assembler |
| **Application (Port In)** | [`GetExcelTaskUseCase`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/application/port/in/GetExcelTaskUseCase.java)<br>[`DownloadExcelTaskFileUseCase`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/application/port/in/DownloadExcelTaskFileUseCase.java) | 長任務進度查詢與檔案下載 UseCase 合約 | `*UseCase` (Rule 3-8) |
| **Application (Port Out)**| [`ExcelTaskRepositoryPort`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/application/port/out/ExcelTaskRepositoryPort.java) | 應用層定義之資料庫操作純契約，嚴禁洩漏 JPA 實體 | `*Port` (Rule 3-8) |
| **Application (Annotation)**| [`@TrackExcelTask`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/application/annotation/task/TrackExcelTask.java) | 宣告式長任務監控註解，支援 SpEL 動態計算 | Annotation |
| **Application (VO)**      | [`ExcelTaskType`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/application/shared/vo/ExcelTaskType.java)<br>[`ExcelTaskStatus`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/application/shared/vo/ExcelTaskStatus.java) | 任務類型與狀態不可變值物件 | Value Object |
| **Application (Context)** | [`ExcelTaskContextHolder`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/application/shared/context/ExcelTaskContextHolder.java) | ThreadLocal 執行期執行緒上下文管理器 | Context Holder |
| **Infrastructure (AOP)**  | [`ExcelTaskTrackingAspect`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/infrastructure/aop/ExcelTaskTrackingAspect.java) | 切面攔截 `@TrackExcelTask`，自動管理任務生命週期與防覆蓋 | Outbound Aspect |
| **Infrastructure (Persistence)** | [`ExcelTaskJpaEntity`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/infrastructure/persistence/task/entity/ExcelTaskJpaEntity.java)<br>[`ExcelTaskJpaRepository`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/infrastructure/persistence/task/repository/ExcelTaskJpaRepository.java) | 長任務 JPA ORM 技術實體與 Spring Data Repository | Technical Entity |
| **Infrastructure (Adapter)** | [`ExcelTaskRepositoryAdapter`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/infrastructure/adapter/persistence/ExcelTaskRepositoryAdapter.java) | 實作 `ExcelTaskRepositoryPort`，以 `REQUIRES_NEW` 獨立事務保證進度即時入庫 | `*Adapter` (Rule 3-9) |

---

## 4. 端到端調用執行時序 (End-to-End Sequence)

```mermaid
sequenceDiagram
    autonumber
    actor Client as 外部呼叫方 / 前端
    participant Aspect as ExcelTaskTrackingAspect
    participant Context as ExcelTaskContextHolder
    participant Repo as ExcelTaskRepositoryPort
    participant Writer as FastExcelWriterAdapter (writeStream)
    participant Ctrl as ExcelTaskController

    Client->>Aspect: 呼叫標註 @TrackExcelTask 之方法
    Note over Aspect: 1. 解析 SpEL 動態檔名與總列數
    Aspect->>Repo: createTask(PENDING) -> markProcessing(PROCESSING)
    Aspect->>Context: bind(taskId, type, totalRows)
    
    Aspect->>Writer: proceed() 執行實體串流寫入
    loop 每達 batchFlushSize (1,000 列)
        Writer->>Writer: worksheet.flush()
        Writer->>Context: updateProgress(processedRows)
        Context->>Repo: updateProgress(taskId, processed, total)
    end
    Writer-->>Aspect: 寫入完成

    Aspect->>Repo: markCompleted(taskId, fileUrl)
    Aspect->>Context: unbind()
    Aspect-->>Client: 返回處理結果

    Note over Client,Ctrl: 2. 前端即時輪詢進度與下載
    Client->>Ctrl: GET /api/v1/excel/tasks/{taskId}/progress
    Ctrl-->>Client: 200 OK (taskStatus: COMPLETED, progress: 100%)
    Client->>Ctrl: GET /api/v1/excel/tasks/{taskId}/download
    Ctrl-->>Client: 200 OK (二進位 Excel 串流下載)
```

---

## 5. 測試驗證全景 (Testing & Verification Status)

* [`ExcelTaskTrackingAspectTest`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/test/java/com/example/demo/infrastructure/aop/ExcelTaskTrackingAspectTest.java)：驗證同步與非同步任務自動建檔、狀態變更、進度記錄、例外標記 `FAILED` 兜底、ThreadLocal 巢狀保護以及 FastExcel / POI 串流沖刷進度自動回報。
* [`ExcelTaskControllerTest`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/test/java/com/example/demo/presentation/controller/ExcelTaskControllerTest.java)：驗證進度輪詢正常回傳、`404 Not Found` 查無任務防護以及二進位檔案串流下載。
