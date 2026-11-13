package com.example.demo.infrastructure.adapter.task;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Excel 長任務本機磁碟暫存適配器單元測試 (LocalDirectoryExcelTaskStorageAdapter Test)
 * <p>
 * 針對 {@link LocalDirectoryExcelTaskStorageAdapter} 驗證產出二進位檔案寫入、
 * 分流 (Tee) 串流即時寫入、檔案讀取與刪除等功能。
 * 遵循 BDD / Gherkin (Feature, Scenario, Given-When-Then) 測試註解規範。
 */
@DisplayName("LocalDirectoryExcelTaskStorageAdapter 磁碟暫存適配器測試")
class LocalDirectoryExcelTaskStorageAdapterTest {

	@TempDir
	Path tempDir;

	private LocalDirectoryExcelTaskStorageAdapter adapter;

	@BeforeEach
	void setUp() {
		adapter = new LocalDirectoryExcelTaskStorageAdapter(tempDir.toString());
		adapter.init();
	}

	@AfterEach
	void tearDown() {
		// Clean up files in tempDir if needed
	}

	/**
	 * Feature: 長任務產出二進位位元組儲存與讀取
	 * Scenario: 成功儲存 byte[] 資料並能依據 taskId 重新讀取
	 * Given: 任務 "task-store-1" 與二進位位元組 [0x50, 0x4B, 0x03, 0x04]
	 * When: 調用 adapter.store 並隨後 retrieve
	 * Then: 成功取回相同內容之位元組陣列
	 */
	@Test
	@DisplayName("場景：成功儲存檔案二進位位元組並依據 taskId 正確讀取")
	void shouldStoreAndRetrieveBytesSuccessfully() {
		// Given
		String taskId = "task-store-1";
		String fileName = "users.xlsx";
		byte[] content = new byte[] { 0x50, 0x4B, 0x03, 0x04, 0x05 };

		// When
		adapter.store(taskId, fileName, content);
		Optional<byte[]> retrieved = adapter.retrieve(taskId);

		// Then
		assertThat(retrieved).isPresent();
		assertThat(retrieved.get()).isEqualTo(content);
	}

	/**
	 * Feature: 串流分流 (TeeOutputStream) 輸出與持久化
	 * Scenario: 當包裝原始 OutputStream 進行寫入時，目標端與暫存檔皆收到完整內容
	 * Given: 原始 ByteArrayOutputStream 與任務 "task-tee-1"
	 * When: 透過 wrapOutputStream 寫入位元組並呼叫 finishStream
	 * Then: 原始 ByteArrayOutputStream 與磁碟暫存 retrieve 均包含寫入內容
	 */
	@Test
	@DisplayName("場景：透過 wrapOutputStream 分流輸出，目標串流與磁碟暫存皆完整保存")
	void shouldWrapOutputStreamAndCaptureContent() throws Exception {
		// Given
		String taskId = "task-tee-1";
		String fileName = "report.xlsx";
		ByteArrayOutputStream targetStream = new ByteArrayOutputStream();
		byte[] mockData = "Excel-Content-Stream-Payload".getBytes();

		// When
		OutputStream teeStream = adapter.wrapOutputStream(taskId, fileName, targetStream);
		teeStream.write(mockData);
		teeStream.flush();
		adapter.finishStream(taskId);

		// Then
		// 1. 目標端 (HTTP Response) 收到完整資料
		assertThat(targetStream.toByteArray()).isEqualTo(mockData);

		// 2. 磁碟暫存亦成功寫入且可讀取
		Optional<byte[]> diskBytes = adapter.retrieve(taskId);
		assertThat(diskBytes).isPresent();
		assertThat(diskBytes.get()).isEqualTo(mockData);
	}

	/**
	 * Feature: 刪除暫存檔案
	 * Scenario: 儲存檔案後調用 delete 應成功移除檔案
	 * Given: 任務 "task-delete-1" 存在於儲存目錄中
	 * When: 調用 adapter.delete
	 * Then: 成功刪除且後續 retrieve 回傳 empty
	 */
	@Test
	@DisplayName("場景：成功刪除暫存檔案")
	void shouldDeleteStoredFileSuccessfully() {
		// Given
		String taskId = "task-delete-1";
		adapter.store(taskId, "test.xlsx", new byte[] { 1, 2, 3 });
		assertThat(adapter.retrieve(taskId)).isPresent();

		// When
		boolean deleted = adapter.delete(taskId);

		// Then
		assertThat(deleted).isTrue();
		assertThat(adapter.retrieve(taskId)).isEmpty();
	}

	/**
	 * Feature: 查詢不存在任務檔案
	 * Scenario: 查詢未產生或已刪除之 taskId
	 * Given: 不存在的任務 ID "task-nonexistent"
	 * When: 調用 adapter.retrieve
	 * Then: 回傳 Optional.empty()
	 */
	@Test
	@DisplayName("場景：查詢不存在任務檔案時回傳 Optional.empty()")
	void shouldReturnEmptyWhenTaskFileNotFound() {
		// When
		Optional<byte[]> result = adapter.retrieve("task-nonexistent");

		// Then
		assertThat(result).isEmpty();
	}
}

