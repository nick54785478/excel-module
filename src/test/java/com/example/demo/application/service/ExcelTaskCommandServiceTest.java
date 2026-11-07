package com.example.demo.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.demo.application.port.in.DownloadExcelTaskFileUseCase;
import com.example.demo.application.port.out.ExcelReaderPort;
import com.example.demo.application.port.out.ExcelTaskRepositoryPort;
import com.example.demo.application.port.out.ExcelWriterPort;
import com.example.demo.application.shared.command.CreateExcelTaskCommand;
import com.example.demo.application.shared.command.ExportExcelCommand;
import com.example.demo.application.shared.command.WriteExcelCommand;
import com.example.demo.application.shared.command.WriteSheetCommand;
import com.example.demo.application.shared.dto.ExcelTaskGottenResult;
import com.example.demo.application.shared.vo.ExcelTaskStatus;
import com.example.demo.application.shared.vo.ExcelTaskType;
import com.example.demo.application.shared.query.ReadExcelQuery;
import com.example.demo.application.shared.stream.ExcelChunkConsumer;

/**
 * Excel 長任務命令應用服務單元測試 (ExcelTaskCommandService Test)
 * <p>
 * 針對 {@link ExcelTaskCommandService} 驗證非同步長任務排程調度、大數據分塊進度回呼與產出快取。
 * 使用同步直接執行器 (Direct Executor: {@code Runnable::run}) 確保單元測試完全可預測且無非同步延遲。
 * 遵循 BDD / Gherkin (Feature, Scenario, Given-When-Then) 測試註解規範。
 */
@DisplayName("ExcelTaskCommandService 非同步長任務命令服務測試")
class ExcelTaskCommandServiceTest {

	private ExcelTaskRepositoryPort excelTaskRepositoryPort;
	private ExcelWriterPort excelWriterPort;
	private ExcelReaderPort excelReaderPort;
	private ExcelTaskCommandService commandService;

	@BeforeEach
	void setUp() {
		this.excelTaskRepositoryPort = mock(ExcelTaskRepositoryPort.class);
		this.excelWriterPort = mock(ExcelWriterPort.class);
		this.excelReaderPort = mock(ExcelReaderPort.class);

		// 使用 Runnable::run 作為同步 Executor，確保背景工作於測試當下立即可靠執行
		this.commandService = new ExcelTaskCommandService(
				excelTaskRepositoryPort,
				excelWriterPort,
				excelReaderPort,
				Runnable::run);
	}

	/**
	 * Feature: 非同步通用 Excel 匯出調度業務
	 * Scenario: 成功提交匯出長任務並在背景產製檔案與標記完成
	 * Given: 包含單一工作表與 2 列資料之 ExportExcelCommand
	 * When: 調用 commandService.submitAsyncExport(command)
	 * Then: 應立即回傳 PENDING 狀態結果，背景執行緒調用 markProcessing、updateProgress、write 並
	 * markCompleted
	 */
	@Test
	@DisplayName("場景：成功提交通用 Excel 匯出任務並於背景完成寫入與進度更新")
	void shouldSubmitAsyncExportSuccessfully() {
		// Given
		WriteSheetCommand sheet = WriteSheetCommand.of(
				"Sheet1",
				List.of("ID", "Name"),
				List.of(List.of(1, "Alice"), List.of(2, "Bob")));
		ExportExcelCommand command = new ExportExcelCommand("users.xlsx", List.of(sheet));

		when(excelTaskRepositoryPort.createTask(any(CreateExcelTaskCommand.class)))
				.thenAnswer(invocation -> {
					CreateExcelTaskCommand cmd = invocation.getArgument(0);
					return new ExcelTaskGottenResult(
							cmd.taskId(),
							cmd.taskType(),
							ExcelTaskStatus.PENDING,
							cmd.totalRows(),
							0L,
							0.0,
							cmd.fileName(),
							null,
							null,
							LocalDateTime.now(),
							LocalDateTime.now());
				});

		byte[] mockBytes = new byte[] { 10, 20, 30 };
		when(excelWriterPort.write(any(WriteExcelCommand.class))).thenReturn(mockBytes);

		// When
		ExcelTaskGottenResult initialResult = commandService.submitAsyncExport(command);

		// Then
		assertThat(initialResult).isNotNull();
		assertThat(initialResult.taskStatus()).isEqualTo(ExcelTaskStatus.PENDING);
		assertThat(initialResult.totalRows()).isEqualTo(2L);

		verify(excelTaskRepositoryPort).markProcessing(initialResult.taskId());
		verify(excelTaskRepositoryPort).updateProgress(eq(initialResult.taskId()), eq(2L), eq(2L));
		verify(excelWriterPort).write(any(WriteExcelCommand.class));
		verify(excelTaskRepositoryPort).markCompleted(eq(initialResult.taskId()), anyString(), eq(2L));

		// 驗證檔案已快取供下載
		byte[] cachedFile = commandService.getExportedFile(initialResult.taskId());
		assertThat(cachedFile).isEqualTo(mockBytes);
	}

	/**
	 * Feature: 非同步 Excel 檔案分塊串流匯入解析業務
	 * Scenario: 成功受理 Excel 輸入流並於背景分塊讀取與回報進度
	 * Given: 包含 Excel 二進位位元組之 InputStream 與查詢條件
	 * When: 調用 commandService.submitAsyncImport(inputStream, "materials.xlsx",
	 * query)
	 * Then: 應即時回傳 PENDING 狀態，背景分塊消費回呼 updateProgress，最終 markCompleted
	 */
	@Test
	@DisplayName("場景：成功提交非同步 Excel 匯入並於背景分塊解析更新進度")
	void shouldSubmitAsyncImportSuccessfully() {
		// Given
		byte[] mockExcelContent = new byte[] { 80, 75, 3, 4 };
		InputStream inputStream = new ByteArrayInputStream(mockExcelContent);
		ReadExcelQuery query = ReadExcelQuery.defaultQuery();

		when(excelTaskRepositoryPort.createTask(any(CreateExcelTaskCommand.class)))
				.thenAnswer(invocation -> {
					CreateExcelTaskCommand cmd = invocation.getArgument(0);
					return new ExcelTaskGottenResult(
							cmd.taskId(),
							cmd.taskType(),
							ExcelTaskStatus.PENDING,
							0L,
							0L,
							0.0,
							cmd.fileName(),
							null,
							null,
							LocalDateTime.now(),
							LocalDateTime.now());
				});

		// 模擬 readChunked 回呼觸發 2 次分塊 (每次 500 筆)
		doAnswer(invocation -> {
			ExcelChunkConsumer consumer = invocation.getArgument(3);
			consumer.accept(List.of(Map.of("code", "M001"), Map.of("code", "M002")), null);
			consumer.accept(List.of(Map.of("code", "M003")), null);
			return null;
		}).when(excelReaderPort).readChunked(any(byte[].class), any(ReadExcelQuery.class), anyInt(),
				any(ExcelChunkConsumer.class));

		// When
		ExcelTaskGottenResult initialResult = commandService.submitAsyncImport(inputStream, "materials.xlsx", query);

		// Then
		assertThat(initialResult).isNotNull();
		assertThat(initialResult.taskStatus()).isEqualTo(ExcelTaskStatus.PENDING);

		verify(excelTaskRepositoryPort).markProcessing(initialResult.taskId());
		// 驗證進度回呼被觸發 (累積 2 筆，再累積至 3 筆)
		verify(excelTaskRepositoryPort).updateProgress(eq(initialResult.taskId()), eq(2L), eq(2L));
		verify(excelTaskRepositoryPort).updateProgress(eq(initialResult.taskId()), eq(3L), eq(3L));
		verify(excelTaskRepositoryPort).markCompleted(eq(initialResult.taskId()), eq(null), eq(3L));
	}

	/**
	 * Feature: 非同步匯出異常防護
	 * Scenario: 背景執行寫入發生例外時應捕獲並標記任務失敗 (FAILED)
	 * Given: 一個匯出命令，但 excelWriterPort.write 拋出異常
	 * When: 調用 commandService.submitAsyncExport(command)
	 * Then: 應調用 excelTaskRepositoryPort.markFailed 記錄異常原因
	 */
	@Test
	@DisplayName("場景：背景寫入異常時應捕獲並標記任務為失敗狀態")
	void shouldMarkTaskFailedWhenExportThrowsException() {
		// Given
		WriteSheetCommand sheet = WriteSheetCommand.of("Sheet1", List.of("ID"), List.of(List.of(1)));
		ExportExcelCommand command = new ExportExcelCommand("error.xlsx", List.of(sheet));

		when(excelTaskRepositoryPort.createTask(any(CreateExcelTaskCommand.class)))
				.thenAnswer(invocation -> {
					CreateExcelTaskCommand cmd = invocation.getArgument(0);
					return new ExcelTaskGottenResult(
							cmd.taskId(),
							cmd.taskType(),
							ExcelTaskStatus.PENDING,
							1L,
							0L,
							0.0,
							cmd.fileName(),
							null,
							null,
							LocalDateTime.now(),
							LocalDateTime.now());
				});

		when(excelWriterPort.write(any(WriteExcelCommand.class)))
				.thenThrow(new RuntimeException("磁碟空間不足"));

		// When
		ExcelTaskGottenResult initialResult = commandService.submitAsyncExport(command);

		// Then
		assertThat(initialResult).isNotNull();
		verify(excelTaskRepositoryPort).markFailed(eq(initialResult.taskId()), eq("磁碟空間不足"));
	}

	/**
	 * Feature: 下載長任務產出檔案使用案例介面合約驗證
	 * Scenario: 驗證實作 DownloadExcelTaskFileUseCase 介面且查無檔案時安全回傳 null
	 * Given: 注入 DownloadExcelTaskFileUseCase 介面合約
	 * When: 調用 getExportedFile 傳入不存在之 taskId
	 * Then: 應回傳 null 且不拋出非預期例外
	 */
	@Test
	@DisplayName("場景：透過 DownloadExcelTaskFileUseCase 查詢不存在任務檔案時應回傳 null")
	void shouldReturnNullWhenDownloadFileNotCached() {
		// Given
		DownloadExcelTaskFileUseCase downloadUseCase = commandService;

		// When
		byte[] result = downloadUseCase.getExportedFile("non-existent-task");

		// Then
		assertThat(result).isNull();
	}
}
