package com.example.demo.infrastructure.adapter.task;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.demo.application.port.out.task.ExcelTaskStoragePort;

import jakarta.annotation.PostConstruct;

/**
 * Excel 長任務本機磁碟暫存適配器 (Local Directory Excel Task Storage Adapter)
 * <p>
 * 遵循 Clean Architecture 六角形架構規範 (AGENTS.md 規則 30/56)，實作應用層 {@link ExcelTaskStoragePort} 輸出埠合約。
 * 負責將非同步與同步串流產生之 Excel 檔案二進位資料持久化暫存於伺服器指定目錄（預設 {@code temp/excel-tasks/}），
 * 支援分流 (Tee) 串流寫入與事後依據 {@code taskId} 快速提取二進位資料。
 * <p>
 * 命名規範：以 {@code Adapter} 結尾 (符合 AGENTS.md 3-9 規範)。
 */
@Component
public class LocalDirectoryExcelTaskStorageAdapter implements ExcelTaskStoragePort {

	private static final Logger log = LoggerFactory.getLogger(LocalDirectoryExcelTaskStorageAdapter.class);

	private final Path storageDirectory;
	private final ConcurrentMap<String, OutputStream> activeFileStreams = new ConcurrentHashMap<>();

	public LocalDirectoryExcelTaskStorageAdapter(
			@Value("${excel.task.storage-dir:temp/excel-tasks}") String storagePath
	) {
		this.storageDirectory = Paths.get(storagePath).toAbsolutePath().normalize();
	}

	@PostConstruct
	public void init() {
		try {
			if (!Files.exists(storageDirectory)) {
				Files.createDirectories(storageDirectory);
				log.info("[ExcelTaskStorage] 成功初始化長任務產出檔案暫存目錄: {}", storageDirectory);
			}
		} catch (IOException e) {
			log.error("[ExcelTaskStorage] 初始化暫存目錄失敗: {}, 原因: {}", storageDirectory, e.getMessage(), e);
		}
	}

	@Override
	public void store(String taskId, String fileName, byte[] content) {
		Objects.requireNonNull(taskId, "taskId 不得為空");
		Objects.requireNonNull(content, "content 不得為空");

		ensureStorageDirectoryExists();
		Path filePath = resolveFilePath(taskId, fileName);
		try {
			Files.write(filePath, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			log.debug("[ExcelTaskStorage] 成功暫存任務檔案: taskId={}, path={}, size={} bytes", taskId, filePath, content.length);
		} catch (IOException e) {
			log.error("[ExcelTaskStorage] 寫入任務暫存檔案失敗: taskId={}, 原因: {}", taskId, e.getMessage(), e);
		}
	}

	@Override
	public OutputStream wrapOutputStream(String taskId, String fileName, OutputStream targetStream) {
		Objects.requireNonNull(taskId, "taskId 不得為空");
		Objects.requireNonNull(targetStream, "targetStream 不得為空");

		ensureStorageDirectoryExists();
		Path filePath = resolveFilePath(taskId, fileName);
		try {
			OutputStream fileOut = new BufferedOutputStream(Files.newOutputStream(
					filePath,
					StandardOpenOption.CREATE,
					StandardOpenOption.TRUNCATE_EXISTING
			));
			activeFileStreams.put(taskId, fileOut);
			return new TeeOutputStream(targetStream, fileOut);
		} catch (IOException e) {
			log.warn("[ExcelTaskStorage] 建立分流暫存檔案串流失敗，降級為直接輸出: taskId={}, 原因: {}", taskId, e.getMessage());
			return targetStream;
		}
	}

	@Override
	public void finishStream(String taskId) {
		if (taskId == null) {
			return;
		}
		OutputStream fileStream = activeFileStreams.remove(taskId);
		if (fileStream != null) {
			try {
				fileStream.flush();
				fileStream.close();
				log.debug("[ExcelTaskStorage] 成功關閉並結算串流暫存檔: taskId={}", taskId);
			} catch (IOException e) {
				log.warn("[ExcelTaskStorage] 關閉串流暫存檔時發生例外: taskId={}, 原因: {}", taskId, e.getMessage());
			}
		}
	}

	@Override
	public Optional<byte[]> retrieve(String taskId) {
		if (taskId == null || taskId.isBlank()) {
			return Optional.empty();
		}
		Path filePath = findFilePath(taskId);
		if (filePath != null && Files.exists(filePath) && Files.isRegularFile(filePath)) {
			try {
				byte[] bytes = Files.readAllBytes(filePath);
				return Optional.of(bytes);
			} catch (IOException e) {
				log.error("[ExcelTaskStorage] 讀取任務暫存檔案失敗: taskId={}, path={}, 原因: {}", taskId, filePath, e.getMessage());
			}
		}
		return Optional.empty();
	}

	@Override
	public boolean delete(String taskId) {
		if (taskId == null || taskId.isBlank()) {
			return false;
		}
		Path filePath = findFilePath(taskId);
		if (filePath != null && Files.exists(filePath)) {
			try {
				return Files.deleteIfExists(filePath);
			} catch (IOException e) {
				log.warn("[ExcelTaskStorage] 刪除暫存檔案失敗: taskId={}, 原因: {}", taskId, e.getMessage());
			}
		}
		return false;
	}

	// =========================================================================
	// 私有輔助方法
	// =========================================================================

	private void ensureStorageDirectoryExists() {
		try {
			if (!Files.exists(storageDirectory)) {
				Files.createDirectories(storageDirectory);
			}
		} catch (IOException e) {
			log.error("[ExcelTaskStorage] 建立暫存目錄失敗: {}, 原因: {}", storageDirectory, e.getMessage());
		}
	}

	private Path resolveFilePath(String taskId, String fileName) {
		String extension = ".xlsx";
		if (fileName != null && fileName.contains(".")) {
			String ext = fileName.substring(fileName.lastIndexOf('.'));
			if (ext.equalsIgnoreCase(".xlsx") || ext.equalsIgnoreCase(".xls")) {
				extension = ext;
			}
		}
		return storageDirectory.resolve(taskId + extension);
	}

	private Path findFilePath(String taskId) {
		Path xlsxPath = storageDirectory.resolve(taskId + ".xlsx");
		if (Files.exists(xlsxPath)) {
			return xlsxPath;
		}
		Path xlsPath = storageDirectory.resolve(taskId + ".xls");
		if (Files.exists(xlsPath)) {
			return xlsPath;
		}
		return null;
	}

	/**
	 * 內部純 Java 分流輸出串流 (TeeOutputStream)
	 * <p>
	 * 於單一寫入呼叫中同步傳遞至目標串流 (客戶端 Response) 與本機暫存檔串流。
	 * </p>
	 */
	public static class TeeOutputStream extends OutputStream {

		private final OutputStream target;
		private final OutputStream copy;

		public TeeOutputStream(OutputStream target, OutputStream copy) {
			this.target = Objects.requireNonNull(target, "target 不得為空");
			this.copy = Objects.requireNonNull(copy, "copy 不得為空");
		}

		@Override
		public void write(int b) throws IOException {
			target.write(b);
			copy.write(b);
		}

		@Override
		public void write(byte[] b) throws IOException {
			target.write(b);
			copy.write(b);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			target.write(b, off, len);
			copy.write(b, off, len);
		}

		@Override
		public void flush() throws IOException {
			target.flush();
			copy.flush();
		}

		@Override
		public void close() throws IOException {
			try {
				target.close();
			} finally {
				copy.close();
			}
		}
	}
}

