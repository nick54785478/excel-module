# 維度三 Part 2：StAX/SAX 惰性拉取式串流讀取與分塊消費 (Streaming Read, Chunk Consumer & Listener)

> 📌 本文件座落於 [`doc/design/streaming-processing/02-streaming-read-and-chunk-consumer.md`](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/design/streaming-processing/02-streaming-read-and-chunk-consumer.md)，屬於「維度三：大數據分塊串流消費」之【Part 2：StAX/SAX 惰性拉取式串流讀取與分塊消費】。  
> 詳述基於 StAX / SAX 之大數據惰性解析、逐列消費 (`ExcelRowConsumer`)、分塊批次消費 (`ExcelChunkConsumer`)、全生命週期事件監聽 (`ExcelStreamListener`) 以及主動中斷控制 (`stop()`)。  
> 
> 🔗 **模組導讀與關聯文件**：  
> • 模組入口主文件：👉 [維度三：大數據分塊串流消費架構總覽 (streaming-processing.md)](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/design/streaming-processing.md)  
> • 串流寫出與 Flush：👉 [維度三 Part 1：大數據串流寫出與常數記憶體沖刷 (01-streaming-write-and-memory-flush.md)](file:///d:/桌面/ModuleWorkSpace/excel-module/doc/design/streaming-processing/01-streaming-write-and-memory-flush.md)

---

## 1. 串流解析模式：StAX / SAX 惰性拉取

傳統解析方式會將整份活頁簿載入記憶體。串流讀取採用事件驅動或拉取式（Pull-Parsing）解析：
* **FastExcel**：使用底層 StAX `sheet.openStream()` 進行拉取式逐列讀取。
* **Apache POI**：使用 `SAX eventusermodel` 進行輕量事件驅動讀取。
* 記憶體中僅保留單一行的 XML 標籤，讀完立即丟棄，徹底杜絕巨檔解析 OOM。

---

## 2. 三大消費模式與回呼契約 (Pure Functional SPI)

座落於 [`com.example.demo.application.shared.stream`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/main/java/com/example/demo/application/shared/stream)：

### 2.1 逐列即時消費：`ExcelRowConsumer`
適用於逐列檢核、即時轉送訊息佇列 (MQ)：
```java
@FunctionalInterface
public interface ExcelRowConsumer {
    void accept(Map<String, Object> rowData, ExcelRowContext context) throws Exception;
}
```

### 2.2 分塊批次消費：`ExcelChunkConsumer`
適用於批次資料庫寫入（如 JDBC `batchInsert`，每次 1,000 筆）：
```java
@FunctionalInterface
public interface ExcelChunkConsumer {
    void accept(List<Map<String, Object>> chunkRows, ExcelRowContext context) throws Exception;
}
```

### 2.3 全生命週期事件監聽器：`ExcelStreamListener`
提供 7 個關鍵節點回呼：
```java
public interface ExcelStreamListener {
    default void onStart(ExcelRowContext context) {}
    default void onSheetStart(String sheetName, int sheetIndex) {}
    default void onHeader(List<String> headers, ExcelRowContext context) {}
    default void onRow(Map<String, Object> rowData, ExcelRowContext context) {}
    default void onChunk(List<Map<String, Object>> chunk, ExcelRowContext context) {}
    default void onSheetEnd(String sheetName, int sheetIndex, long rowCount) {}
    default void onComplete(long totalRowCount) {}
    default void onError(Exception exception, ExcelRowContext context) {}
}
```

---

## 3. 主動中斷控制機制 (Early Termination)

在消費過程中，若已達到抽樣筆數、或偵測到重大資料錯誤需提早終止 I/O 解析：

```java
public class ExcelRowContext {
    private boolean stopped = false;

    public void stop() {
        this.stopped = true;
    }

    public boolean isStopped() {
        return stopped;
    }
}
```
* **使用範例**：
```java
excelReaderPort.readChunked(inputStream, query, 500, (chunk, ctx) -> {
    if (ctx.getPhysicalRowNumber() > 10000) {
        ctx.stop(); // 立即中斷底層 StAX / SAX 串流讀取，釋放檔案控制代碼
    }
});
```

---

## 4. 單元與整合測試驗證 (Testing & Verification)

* [`FastExcelReaderAdapterTest`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/test/java/com/example/demo/infrastructure/adapter/engine/FastExcelReaderAdapterTest.java)：驗證逐列串流讀取、分塊消費批次組裝、`stop()` 中斷與標頭自適應。
* [`PoiSaxReaderAdapterTest`](file:///d:/桌面/ModuleWorkSpace/excel-module/src/test/java/com/example/demo/infrastructure/adapter/engine/PoiSaxReaderAdapterTest.java)：驗證 POI SAX 串流解析在大檔案下的記憶體平穩度與型別還原。

