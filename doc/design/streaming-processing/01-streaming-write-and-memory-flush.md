# 維度三 Part 1：大數據串流寫出與常數記憶體沖刷 (Streaming Write & Memory Flush)

> 📌 本文件座落於 [`doc/design/streaming-processing/01-streaming-write-and-memory-flush.md`](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/design/streaming-processing/01-streaming-write-and-memory-flush.md)，屬於「維度三：大數據分塊串流消費」之【Part 1：大數據串流寫出與常數記憶體沖刷】。  
> 詳述 `WriteStreamExcelCommand` 契約、常數級空間複雜度 $O(1)$ 記憶體原理、FastExcel `worksheet.flush()` 與 POI SXSSF `flushRows(0)` 定期沖刷機制以及百萬級數據產出實踐。  
> 
> 🔗 **模組導讀與關聯文件**：  
> • 模組入口主文件：👉 [維度三：大數據分塊串流消費架構總覽 (streaming-processing.md)](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/design/streaming-processing.md)  
> • 串流讀取與消費者：👉 [維度三 Part 2：StAX/SAX 惰性拉取式串流讀取與分塊消費 (02-streaming-read-and-chunk-consumer.md)](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/design/streaming-processing/02-streaming-read-and-chunk-consumer.md)

---

## 1. 傳統批次寫入的 OOM 瓶頸

傳統 Apache POI 的 DOM 模式（`XSSFWorkbook`）會在 JVM 堆積記憶體中完整建置整份活頁簿的 XML DOM 物件樹：
* **記憶體膨脹率極高**：每一筆 Excel 資料載入記憶體後放大 10~100 倍。當檔案達數萬至數十萬列時，頻繁觸發 Full GC 與 `OutOfMemoryError: Java heap space`。
* **併發阻塞**：多位使用者同時下載大型檔案時，伺服器記憶體迅速耗盡。

---

## 2. 常數空間複雜度 ($O(1)$) 串流寫出機制

FastExcel 與 POI SXSSF 的核心架構理念是將 Excel 工作表直接對應至 ZIP Entry 的 XML 串流直沖 `OutputStream`：

```
  [Data Producer Stream] 
           │ (row by row)
           ▼
┌─────────────────────────┐
│ FastExcel Worksheet     │ ──(達到 1,000 列)──> [worksheet.flush()]
│ Internal Row Buffer     │                          │ (寫入並釋放記憶體)
└─────────────────────────┘                          ▼
                                             ┌──────────────────┐
                                             │ ZipOutputStream  │ ──> (Disk / HTTP Response)
                                             └──────────────────┘
```

### 核心運作步驟：
1. 傳入 `Stream<List<Object>>` 或 `Iterator<List<Object>>` 惰性資料流。
2. 每當寫入列數達到 `batchFlushSize`（預設 1,000 列）時，主動呼叫 `worksheet.flush()` 或 POI `((SXSSFSheet) sheet).flushRows(0)`。
3. `flush()` 會將該千列 XML 內容壓縮寫入 `OutputStream`，並**完全清空內部儲存格緩衝區**。
4. 記憶體中始終僅維持約 1,000 列的輕量資料，無論寫入 1 萬列、50 萬列還是 100 萬列，堆積記憶體維持平穩常數。

---

## 3. 串流寫出指令模型 (Pure Java 21 Record)

座落於 [`com.example.demo.application.shared.command`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/application/shared/command)：

### 3.1 `WriteStreamExcelCommand`
```java
public record WriteStreamExcelCommand(
        String applicationName,
        List<WriteStreamSheetCommand> sheets,
        int defaultBatchFlushSize
) {
    public static final int DEFAULT_BATCH_FLUSH_SIZE = 1000;
}
```

### 3.2 `WriteStreamSheetCommand`
```java
public record WriteStreamSheetCommand(
        String sheetName,
        List<String> headers,
        Stream<List<Object>> rowStream,
        Iterator<List<Object>> rowIterator,
        int batchFlushSize
) {}
```

---

## 4. 基礎設施寫出適配器實作

* [`FastExcelWriterAdapter.writeStream`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/infrastructure/adapter/engine/FastExcelWriterAdapter.java)：利用 FastExcel 高速 StAX 直沖。
* [`PoiSxssfWriterAdapter.writeStream`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/infrastructure/adapter/engine/PoiSxssfWriterAdapter.java)：利用 Apache POI SXSSF 硬碟暫存檔機制防爆記憶體。

