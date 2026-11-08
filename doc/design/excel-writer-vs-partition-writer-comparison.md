# ExcelWriterPort 與 ExcelPartitionWriterPort 設計對比與選型指引 (Writer Ports Comparison & Decision Guide)

> 📌 本文件座落於 [`doc/design/excel-writer-vs-partition-writer-comparison.md`](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/design/excel-writer-vs-partition-writer-comparison.md)，深入比較 `excel-module` 中兩大寫入輸出埠（Outbound Ports）：  
> 1. 底層單活頁簿序列化引擎埠：[`ExcelWriterPort`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/application/port/out/ExcelWriterPort.java)  
> 2. 高並發管線與分片打包輸出埠：[`ExcelPartitionWriterPort`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/application/port/out/ExcelPartitionWriterPort.java)  
> 
> 提供架構分層定位、核心機制差異、以及開發者在不同資料規模與業務場景下的**選型決策樹 (Decision Tree)**。

---

## 1. 核心定位與架構層次剖析 (Architectural Roles & Layering)

在 Clean Architecture 六角形架構中，兩者皆屬於 **Application Layer 的 Outbound Port（輸出埠）**，但其抽象層次與關注點完全不同：

```mermaid
flowchart TD
    subgraph Application_Layer ["Application Layer (應用層業務合約與調度)"]
        direction TB
        UC1["標準匯出 UseCase\n(ExportExcelUseCase / StreamExportExcelUseCase)"]
        UC2["高並發平行與分片 UseCase\n(ExportParallelExcelUseCase)"]
        
        PORT_PART["ExcelPartitionWriterPort\n(高並發管線調度與 ZIP 分片打包埠)"]
        PORT_BASE["ExcelWriterPort\n(單活頁簿二進位序列化埠)"]
        
        UC1 --> PORT_BASE
        UC2 --> PORT_PART
    end

    subgraph Infrastructure_Layer ["Infrastructure Layer (基礎設施層適配器與共用引擎)"]
        direction TB
        subgraph Adapters ["輸出適配器 (Outbound Adapters)"]
            ADAPT_PART["ParallelPartitionExcelWriterAdapter\n(協調執行緒池、背壓佇列與 ZipOutputStream)"]
            ADAPT_POI["PoiSxssfWriterAdapter\n(任務追蹤與 SXSSF 適配)"]
            ADAPT_FAST["FastExcelWriterAdapter\n(任務追蹤與 FastExcel 適配)"]
        end

        subgraph Shared_Engines ["底層共用串流引擎 (Infrastructure Shared Engines)"]
            ENGINE_SPI["ExcelStreamWriterEngine\n(基礎設施共用串流寫入 SPI)"]
            ENGINE_POI["PoiSxssfStreamWriterEngine\n(SXSSF 磁碟暫存沖刷)"]
            ENGINE_FAST["FastExcelStreamWriterEngine\n(StAX 極速串流寫出)"]
            
            ENGINE_SPI -.->|實作| ENGINE_POI
            ENGINE_SPI -.->|實作| ENGINE_FAST
        end
        
        PORT_PART -.->|實作| ADAPT_PART
        PORT_BASE -.->|實作| ADAPT_POI
        PORT_BASE -.->|實作| ADAPT_FAST
        
        ADAPT_PART -->|內部共用調用| ENGINE_SPI
        ADAPT_POI -->|委託底層序列化| ENGINE_POI
        ADAPT_FAST -->|委託底層序列化| ENGINE_FAST
    end

    ADAPT_POI --> OUT_XLSX["單一 .xlsx 檔案串流"]
    ADAPT_FAST --> OUT_XLSX
    ADAPT_PART --> OUT_ZIP["多檔案 .zip 壓縮包 或 單一 .xlsx"]
```

> [!NOTE]
> **嚴格遵循六角形架構 (Hexagonal Architecture) 邊界規範**：  
> 1. `ExcelWriterPort` 與 `ExcelPartitionWriterPort` 為應用層各自獨立之 Outbound Port，由應用層服務（`ApplicationService`）進行業務調度。
> 2. 適配器 [`ParallelPartitionExcelWriterAdapter`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/infrastructure/adapter/parallel/ParallelPartitionExcelWriterAdapter.java) **嚴禁取巧跨層依賴 `ExcelWriterPort`**。
> 3. 基礎設施層於內部落地了專屬共用組件 [`ExcelStreamWriterEngine`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/infrastructure/engine/ExcelStreamWriterEngine.java)，提供單一活頁簿二進位序列化共用能力（支援 POI SXSSF 與 FastExcel 雙實作），由適配器在 Infrastructure 內部調用，達成 100% 程式碼複用與純淨的分層解耦。

---

## 2. 深度特性維度對比矩陣 (Comprehensive Feature Comparison)

| 比較維度 | [`ExcelWriterPort`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/application/port/out/ExcelWriterPort.java) | [`ExcelPartitionWriterPort`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/application/port/out/ExcelPartitionWriterPort.java) |
| :--- | :--- | :--- |
| **核心職責** | 專注於**「單一 Excel 活頁簿 (.xlsx) 的二進位序列化與記憶體沖刷」**。 | 專注於**「多執行緒平行查 DB、背壓佇列管線 (模式 A) 與多活頁簿 ZIP 分片打包 (模式 B)」**。 |
| **資料提供模式** | **被動式提供 (Push / Passive Iterable)**：<br/>呼叫端必須事先備妥資料清單 (`List<List<Object>>`)，或提供被動拉取的 `Iterable<List<Object>>`。 | **主動式拉取 (Pull via SPI)**：<br/>接收函數式提供者 [`ExcelChunkProvider`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/application/shared/pipeline/ExcelChunkProvider.java)，由適配器內部多執行緒主動按 offset/limit 平行查詢。 |
| **執行緒模型** | **純單執行緒循序寫入 (Single-Threaded)**：<br/>實作類別內部不建立、不管理任何執行緒池，完全由調用者所在執行緒執行。 | **多執行緒主動並行 (Multi-Threaded Concurrent)**：<br/>內部管理專用 `ExecutorService`，協調多個生產者執行緒並發查 DB，並由單一專屬消費者執行緒寫入。 |
| **背壓與防 OOM** | 依賴呼叫端自身控制資料產出頻率，適配器僅負責定期 `flush()` 沖刷磁碟。 | 內建 `ArrayBlockingQueue` 有界佇列。當資料庫查詢過快時**自動阻塞生產者**，嚴格保證固定記憶體佔用。 |
| **異常中斷機制** | 傳統單執行緒例外拋出。 | **毒丸模式 (Poison Pill) 與多執行緒異常傳播**：任何查詢緒異常時立即喚醒消費者中止，杜絕死鎖。 |
| **單表列數上限** | 受限於 Excel 物理極限（單 Sheet 最大 **1,048,576 列**），超出必然失敗。 | **自動突破 104 萬列硬限制**：依 `maxRowsPerFile` 自動分片為多個 `.xlsx` 檔，並串流打包為 ZIP。 |
| **輸出產物型態** | 單一 Excel 活頁簿（`application/vnd.openxmlformats-officedocument...`）。 | 單一 Excel 活頁簿（模式 A）或含有多個 Excel 的 **ZIP 壓縮檔**（`application/zip`，模式 B/A+B）。 |
| **適配器實作** | `FastExcelWriterAdapter`<br/>`PoiSxssfWriterAdapter` | `ParallelPartitionExcelWriterAdapter` |

---

## 3. 架構選型決策樹 (Architecture Decision Tree)

面對不同的業務需求、資料規模與架構場景，請依據下方決策樹進行選型：

```mermaid
flowchart TD
    START(["開始：決定 Excel 匯出方案"]) --> Q1{"資料總筆數是否可能超過 100 萬列？\n或業務要求切分多個檔案下載？"}
    
    Q1 -- 是 (突破 Excel 上限 / 需 ZIP 封裝) --> USE_PART_B["選擇【ExcelPartitionWriterPort】\n使用模式 B / 模式 A+B (多活頁簿 ZIP 分片)"]
    
    Q1 -- 否 (單一 Excel 即可容納) --> Q2{"資料庫查詢與業務運算\n是否耗時顯著 (> 3~5 秒)？\n或資料量達 5 萬 ~ 100 萬列？"}
    
    Q2 -- 是 (需要多緒平行查 DB 加速) --> USE_PART_A["選擇【ExcelPartitionWriterPort】\n使用模式 A (微觀生產者-消費者背壓管線)"]
    
    Q2 -- 否 (資料量適中或已在記憶體中) --> Q3{"呼叫方是否已有自己的\n串流游標 (如 MyBatis/JPA Cursor)？"}
    
    Q3 -- 是 (自定義串流消費) --> USE_BASE_STREAM["選擇【ExcelWriterPort.writeStream】\n傳入 WriteStreamExcelCommand 邊讀邊沖刷"]
    
    Q3 -- 否 (一般中小規模報表 < 5 萬列) --> USE_BASE["選擇【ExcelWriterPort.write / writeSingleSheet】\n直接一次性記憶體寫出，極致輕量高效"]
```

---

## 4. 典型場景使用建議 (Practical Usage Scenarios)

### 場景 1：傳統中小型資料匯出（推薦使用 `ExcelWriterPort`）
- **典型情境**：
  - 查詢後台訂單列表、使用者清單、商品主檔匯出（資料量在 10 ~ 30,000 列以內）。
  - 資料庫查詢只需要一次簡單的 SQL（耗時 < 1 秒），且整批資料已由 Service 載入為 `List<T>`。
- **優勢**：
  - 呼叫簡單直覺，不需配置執行緒池、佇列或資料區塊提供者。
  - 記憶體開銷低，直接透過 `write(command)` 或 `writeSingleSheet(...)` 即可秒級產出。

```java
// 典型用法：直接透過 ExcelWriterPort 寫出
WriteExcelCommand command = ExcelBeanAssembler.toWriteCommand(orderList, OrderModel.class);
byte[] excelBytes = excelWriterPort.write(command);
```

---

### 場景 2：單檔大數據但查詢緩慢（推薦使用 `ExcelPartitionWriterPort` - 模式 A）
- **典型情境**：
  - 匯出 10 萬 ~ 80 萬列歷史交易明細，仍需輸出為**「單一 `.xlsx` 檔案」**。
  - 資料庫查詢是最大瓶頸（例如單緒查 50 萬筆需耗時 40 秒，但若分成 5 個執行緒分段查只需 8 秒）。
- **優勢**：
  - 利用 `ExportPipelineExcelCommand` 啟動多個工作緒平行查 DB。
  - 透過 `queueCapacity` 嚴格限制記憶體內的資料量，避免查出幾十萬筆導致 OOM。
  - 專屬寫入緒循序寫入 SXSSF/FastExcel，保證底層 100% 執行緒安全。

```java
// 典型用法：模式 A 生產者-消費者管線加速
ExportPipelineExcelCommand command = ExportPipelineExcelCommand.builder()
        .applicationName("OrderSystem")
        .sheetName("50萬筆訂單明細")
        .headers(List.of("訂單號", "金額", "時間"))
        .totalRows(500000)
        .chunkSize(2000)
        .queueCapacity(25)       // 最多快取 25 個 chunk，超額自動阻塞 DB 查詢
        .parallelProducers(4)   // 4 執行緒平行查 DB
        .chunkProvider((offset, limit) -> orderRepository.findChunk(offset, limit))
        .build();

excelPartitionWriterPort.writePipelineStream(outputStream, command);
```

---

### 場景 3：突破百萬列天花板或多檔案歸檔（推薦使用 `ExcelPartitionWriterPort` - 模式 B / A+B）
- **典型情境**：
  - 匯出全年度 300 萬筆物料進出庫紀錄或電商訂單。
  - 超過單一工作表 $1,048,576$ 列硬上限；且單檔若達 200MB，客戶端開啟必當機。
- **優勢**：
  - 設定 `maxRowsPerFile = 500000`，自動切分成 6 個獨立 Excel 檔案。
  - 每個分片在多核心 CPU 上**完全平行並行產製**，並可開啟內部模式 A 管線加速。
  - 透過 `ZipOutputStream` 直接將各分片串流壓縮打包為單一 `.zip`，瀏覽器一鍵下載。

```java
// 典型用法：模式 B / A+B 多活頁簿分片並壓縮為 ZIP
ExportPartitionZipCommand command = ExportPartitionZipCommand.builder()
        .zipFileName("orders_2026_annual.zip")
        .baseFileName("orders_part")
        .sheetName("訂單資料")
        .headers(List.of("項次", "訂單編號", "金額", "時間"))
        .totalRows(3000000)
        .maxRowsPerFile(500000) // 自動切分為 6 個獨立 .xlsx 活頁簿
        .usePipelinePerPartition(true) // 各分片內部啟用模式 A 多緒查詢加速
        .chunkProvider((offset, limit) -> orderRepository.findChunk(offset, limit))
        .build();

excelPartitionWriterPort.writePartitionZipStream(outputStream, command);
```

---

## 5. 專題探討：低於 5 萬筆（中小型資料）的效能與選型深度解析 (In-Depth Analysis for < 50,000 Rows)

在評估是否採用高並發管線時，開發者常有疑問：**「在小於百萬級（特別是低於 5 萬筆）的中小型資料情境中，使用 `ExcelPartitionWriterPort` 會比 `ExcelWriterPort` 效率差嗎？」**

### 5-1. 核心結論與決定性因素
答案是：**不會變差！在絕大多數需要頻繁分頁查 DB 或業務計算的場景中，`ExcelPartitionWriterPort` 依然快上 2 ~ 3 倍；只有在資料已全在記憶體時，`ExcelWriterPort` 才會有些微優勢。**

關鍵在於**「資料取得方式」**與**「I/O 耗時佔比」**：

| 資料取得方式 | 資料筆數區間 | 哪一個效率更好？ | 效能成因與耗時深度分析 |
| :--- | :---: | :---: | :--- |
| **情境 A：資料已在記憶體**<br>(例如 Service 已載入 `List<T>`) | **全區間**<br>(< 5 萬筆) | **`ExcelWriterPort` 略勝**<br>(快 5% ~ 10%) | 5 萬筆 Java 物件在記憶體約僅佔 **15MB ~ 25MB**，單緒直接序列化僅需 0.3 ~ 0.8 秒。<br>若硬上多執行緒管線，只是把物件塞進 Queue 再倒出來，純屬徒增 CPU 執行緒切換與鎖競爭開銷。 |
| **情境 B：單次 SQL 快速查出**<br>(單次 `SELECT` 0.2 秒完成) | **< 1 萬筆** | **`ExcelWriterPort` 略勝**<br>(體驗毫無差別) | 單次 SQL 查資料只要 0.1~0.2 秒，單緒寫入只需 0.1 秒，總耗時不到 0.3 秒。此時多執行緒管線的固定開銷（約 20ms）佔比相對明顯，但**兩者僅差幾十毫秒，人類肉眼完全無感**。 |
| **情境 C：慢查詢 / 頻繁多次分頁查詢**<br>(例如每次查 1000 筆需 80ms) | **1 萬 ~ 5 萬筆** | **`ExcelPartitionWriterPort` 完勝**<br>(速度快 2 ~ 3 倍) | 5 萬筆若分 50 次查：<br>• 單緒 `ExcelWriterPort` 查 DB 需 $50 \times 80\text{ms} = 4\text{ 秒}$，總耗時約 **5 秒**；<br>• 4 緒 `ExcelPartitionWriterPort` 平行查 DB 只要 **1 秒**，加上寫入重疊掩蓋，總時間縮減至 **1.8 秒**！ |

---

### 5-2. 5 萬筆資料的硬體資源開銷具體量化

為什麼在 5 萬筆以下，`ExcelWriterPort` 通常就足以勝任？

1. **記憶體佔用極小，無 OOM 威脅**：
   - 5 萬筆典型業務資料（約 10 個欄位）：在 JVM 堆積記憶體中約佔 **20MB 左右**。
   - 對於現代伺服器（2GB ~ 8GB Heap）而言，20MB 完全在新生代 Eden 區容忍範圍內，**根本不會引發 Full GC**。
2. **單線程寫入本身已是秒級甚至毫秒級**：
   - **FastExcel 引擎**：寫入 5 萬筆約 **250 ~ 400 毫秒**。
   - **Apache POI SXSSF 引擎**：寫入 5 萬筆約 **800 ~ 1,500 毫秒**（含磁碟暫存沖刷）。
   - 單純序列化資料本身極快，瓶頸幾乎 100% 都在資料庫端。

---

### 5-3. 低於 5 萬筆的黃金決策準則

```mermaid
flowchart TD
    DATA_50K(["資料量低於 5 萬筆 (< 50,000 Rows)"]) --> COND{"這 5 萬筆資料是一次性 SQL 查完\n或已經在 Java 記憶體 List 中了嗎？"}
    
    COND -- 是 (單次查詢 / 已在 List) --> DECIDE_BASE["👉 堅決選擇【ExcelWriterPort】\n• 程式碼最乾淨直覺\n• 零執行緒池排隊與排程開銷\n• 0.5 秒直接下載完成"]
    
    COND -- 否 (多次分頁查詢 / SQL 很慢 / 跨 API 呼叫) --> DECIDE_PART["👉 選擇【ExcelPartitionWriterPort (模式 A)】\n• 利用多執行緒把 I/O 等待時間壓平\n• 依然能顯著省下好幾秒等待時間"]
```

---

## 6. 架構師總結 (Architect's Summary)

> [!IMPORTANT]
> - **`ExcelWriterPort` 是基礎設施層的「引擎基石」**：負責把結構化資料以最乾淨、最嚴格的常數記憶體 Flush 機制轉換為 OpenXML 字節流，嚴守單執行緒安全。
> - **`ExcelPartitionWriterPort` 是高並發業務的「火箭推進器」**：負責調度執行緒池、背壓有界佇列與 ZIP 封裝，解決資料庫查詢 I/O 瓶頸與單表百萬列限制。
> - 兩者職責分明、高度互補，共同建構了本模組從「單筆精緻報表」到「千萬級超大規模大數據」的全場景覆蓋能力。

