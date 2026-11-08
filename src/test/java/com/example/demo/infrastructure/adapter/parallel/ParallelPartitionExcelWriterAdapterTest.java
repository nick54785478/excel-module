package com.example.demo.infrastructure.adapter.parallel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.demo.application.shared.command.ExportPartitionZipCommand;
import com.example.demo.application.shared.command.ExportPipelineExcelCommand;
import com.example.demo.application.shared.exception.ExcelWriteException;
import com.example.demo.infrastructure.engine.ExcelStreamWriterEngine;
import com.example.demo.infrastructure.engine.poi.PoiSxssfStreamWriterEngine;

/**
 * 高並發平行管線與分片 ZIP 輸出適配器整合測試 (ParallelPartitionExcelWriterAdapter Test)
 * <p>
 * 遵循 AGENTS.md 規則 5 規範，採用 BDD 與 Gherkin (Feature, Scenario, Given-When-Then) 結構撰寫。
 */
@DisplayName("ParallelPartitionExcelWriterAdapter 平行管線與分片適配器測試")
class ParallelPartitionExcelWriterAdapterTest {

	private static ExecutorService testExecutor;
	private static ExcelStreamWriterEngine streamWriterEngine;
	private static ParallelPartitionExcelWriterAdapter adapter;

	@BeforeAll
	static void init() {
		testExecutor = Executors.newFixedThreadPool(4);
		streamWriterEngine = new PoiSxssfStreamWriterEngine();
		adapter = new ParallelPartitionExcelWriterAdapter(streamWriterEngine, testExecutor);
	}

	@AfterAll
	static void tearDown() {
		if (testExecutor != null) {
			testExecutor.shutdown();
		}
	}

	/**
	 * Feature: 模式 A 微觀生產者-消費者背壓管線加速
	 * Scenario: 多執行緒平行查詢並推入有界佇列，單一消費者循序寫出標準 Excel 檔案
	 * Given: 預計產製 1,500 列資料，設定區塊大小 500、佇列容量 10 與 3 個平行查詢生產者
	 * When: 執行 adapter.writePipeline(command)
	 * Then: 產出有效之 Excel 二進位位元組陣列，開頭包含標準 PK ZIP 魔術字節 (0x50, 0x4B)
	 */
	@Test
	@DisplayName("模式 A 微觀管線：平行查詢生產與循序串流寫出成功")
	void should_export_pipeline_with_producers_and_consumer_successfully() {
		// Given
		int totalRows = 1500;
		int chunkSize = 500;
		List<String> headers = List.of("ID", "OrderNo", "Amount", "Timestamp");

		ExportPipelineExcelCommand command = ExportPipelineExcelCommand.builder()
				.applicationName("PipelineTestApp")
				.sheetName("管線測試")
				.headers(headers)
				.totalRows(totalRows)
				.chunkSize(chunkSize)
				.queueCapacity(10)
				.parallelProducers(3)
				.chunkProvider((offset, limit) -> {
					List<List<Object>> rows = new ArrayList<>(limit);
					for (int i = 0; i < limit; i++) {
						int id = offset + i + 1;
						rows.add(List.of(id, "ORD-" + id, id * 10.5, "2026-11-08 12:00:00"));
					}
					return rows;
				})
				.build();

		// When
		byte[] excelBytes = adapter.writePipeline(command);

		// Then
		assertThat(excelBytes).isNotNull().isNotEmpty();
		// 驗證 Excel (.xlsx) 標準 ZIP 標頭標記 (PK.. -> 0x50, 0x4B, 0x03, 0x04)
		assertThat(excelBytes[0]).isEqualTo((byte) 0x50);
		assertThat(excelBytes[1]).isEqualTo((byte) 0x4B);
	}

	/**
	 * Feature: 模式 A 生產端異常中斷與防禦
	 * Scenario: 當平行查詢工作緒拋出異常時，管線應立即喚醒消費者並傳播 ExcelWriteException
	 * Given: chunkProvider 在特定 offset 拋出模擬之資料庫連線超時例外
	 * When: 執行 adapter.writePipeline(command)
	 * Then: 拋出 ExcelWriteException，且原始根本原因 (Caused by) 為資料庫連線超時
	 */
	@Test
	@DisplayName("模式 A 微觀管線：生產端異常即時傳播至消費者並中斷")
	void should_propagate_exception_when_producer_fails() {
		// Given
		ExportPipelineExcelCommand command = ExportPipelineExcelCommand.builder()
				.applicationName("PipelineErrorTest")
				.sheetName("ErrorSheet")
				.headers(List.of("ID", "Name"))
				.totalRows(1000)
				.chunkSize(200)
				.queueCapacity(5)
				.parallelProducers(2)
				.chunkProvider((offset, limit) -> {
					if (offset >= 200) {
						throw new RuntimeException("Simulated Database connection timeout!");
					}
					return List.of(List.of(1, "Test"));
				})
				.build();

		// When & Then
		assertThatThrownBy(() -> adapter.writePipeline(command))
				.isInstanceOf(ExcelWriteException.class)
				.hasMessageContaining("Simulated Database connection timeout!");
	}

	/**
	 * Feature: 模式 B 多活頁簿 ZIP 分片並行產製
	 * Scenario: 資料超過單檔設定上限時，平行產製多個獨立 Excel 並動態壓縮打包為單一 ZIP
	 * Given: 總列數 4,000 列，單檔上限 2,000 列 (將切分為 2 個分片)，且啟用模式 A+B 組合拳
	 * When: 執行 adapter.writePartitionZip(command)
	 * Then: 產出有效之 ZIP 檔案，且內部恰好包含 orders_part_001.xlsx 與 orders_part_002.xlsx 兩個條目
	 */
	@Test
	@DisplayName("模式 B / A+B：多活頁簿分片平行產製並動態打包 ZIP 成功")
	void should_export_partition_zip_with_multiple_excel_files_successfully() throws Exception {
		// Given
		int totalRows = 4000;
		int maxRowsPerFile = 2000;
		List<String> headers = List.of("序號", "訂單編號", "金額");

		ExportPartitionZipCommand command = ExportPartitionZipCommand.builder()
				.zipFileName("multi_orders.zip")
				.baseFileName("orders_part")
				.sheetName("明細")
				.headers(headers)
				.totalRows(totalRows)
				.maxRowsPerFile(maxRowsPerFile)
				.chunkSize(500)
				.parallelProducersPerPartition(2)
				.usePipelinePerPartition(true)
				.chunkProvider((offset, limit) -> {
					List<List<Object>> rows = new ArrayList<>(limit);
					for (int i = 0; i < limit; i++) {
						int id = offset + i + 1;
						rows.add(List.of(id, "ORD-" + id, 500.0));
					}
					return rows;
				})
				.build();

		// When
		byte[] zipBytes = adapter.writePartitionZip(command);

		// Then
		assertThat(zipBytes).isNotNull().isNotEmpty();
		assertThat(zipBytes[0]).isEqualTo((byte) 0x50);
		assertThat(zipBytes[1]).isEqualTo((byte) 0x4B);

		// 解析 ZIP 條目清單
		List<String> entryNames = new ArrayList<>();
		try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				entryNames.add(entry.getName());
				zis.closeEntry();
			}
		}

		assertThat(entryNames)
				.hasSize(2)
				.containsExactly("orders_part_001.xlsx", "orders_part_002.xlsx");
	}

	/**
	 * Feature: 模式 B 單一分片 (邊界測試)
	 * Scenario: 總筆數小於單檔上限時，恰好產生 1 個分片之 ZIP 壓縮檔
	 * Given: 總列數 1,000 列，單檔上限 5,000 列
	 * When: 執行 adapter.writePartitionZip(command)
	 * Then: 產出 ZIP 檔案，且內部僅有 orders_part_001.xlsx
	 */
	@Test
	@DisplayName("模式 B 分片邊界：總列數未達上限時產生單一條目 ZIP")
	void should_export_single_partition_zip_when_total_rows_less_than_limit() throws Exception {
		// Given
		ExportPartitionZipCommand command = ExportPartitionZipCommand.builder()
				.zipFileName("single_part.zip")
				.baseFileName("single_part")
				.sheetName("Sheet1")
				.headers(List.of("ID"))
				.totalRows(1000)
				.maxRowsPerFile(5000)
				.chunkSize(500)
				.usePipelinePerPartition(false) // 測試標準串流分支
				.chunkProvider((offset, limit) -> List.of(List.of("Row-" + offset)))
				.build();

		// When
		byte[] zipBytes = adapter.writePartitionZip(command);

		// Then
		assertThat(zipBytes).isNotNull().isNotEmpty();

		List<String> entryNames = new ArrayList<>();
		try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				entryNames.add(entry.getName());
				zis.closeEntry();
			}
		}

		assertThat(entryNames)
				.hasSize(1)
				.containsExactly("single_part_001.xlsx");
	}
}

