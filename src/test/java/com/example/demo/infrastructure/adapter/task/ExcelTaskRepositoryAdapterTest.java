package com.example.demo.infrastructure.adapter.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.demo.application.shared.command.CreateExcelTaskCommand;
import com.example.demo.application.shared.dto.ExcelTaskGottenResult;
import com.example.demo.application.shared.vo.ExcelTaskStatus;
import com.example.demo.application.shared.vo.ExcelTaskType;
import com.example.demo.infrastructure.persistence.task.entity.ExcelTaskJpaEntity;
import com.example.demo.infrastructure.persistence.task.repository.ExcelTaskJpaRepository;

/**
 * Excel 長任務基礎設施適配器單元測試 (ExcelTaskRepositoryAdapter Test)
 * <p>
 * 針對 {@link ExcelTaskRepositoryAdapter} 驗證其實作
 * {@link com.example.demo.application.port.out.ExcelTaskRepositoryPort}
 * 之資料庫協調、進度演算與狀態防腐轉換。
 * 遵循 BDD / Gherkin (Feature, Scenario, Given-When-Then) 測試註解規範。
 */
@DisplayName("ExcelTaskRepositoryAdapter 儲存庫輸出適配器測試")
class ExcelTaskRepositoryAdapterTest {

	private ExcelTaskJpaRepository taskJpaRepository;
	private ExcelTaskRepositoryAdapter adapter;

	@BeforeEach
	void setUp() {
		this.taskJpaRepository = mock(ExcelTaskJpaRepository.class);
		this.adapter = new ExcelTaskRepositoryAdapter(taskJpaRepository);
	}

	/**
	 * Feature: Excel 長任務建立功能
	 * Scenario: 成功將建立任務命令轉換為 JPA 實體並持久化
	 * Given: 一個合法的 CreateExcelTaskCommand 命令
	 * When: 調用 adapter.createTask(command)
	 * Then: 應將實體存入 JPA 儲存庫並回傳 PENDING 狀態之任務結果 DTO
	 */
	@Test
	@DisplayName("場景：成功建立初始長任務")
	void shouldCreateTaskSuccessfully() {
		// Given
		CreateExcelTaskCommand command = new CreateExcelTaskCommand(
				"task-123",
				ExcelTaskType.EXPORT,
				"materials.xlsx",
				1000L);

		when(taskJpaRepository.saveAndFlush(any(ExcelTaskJpaEntity.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		// When
		ExcelTaskGottenResult result = adapter.createTask(command);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.taskId()).isEqualTo("task-123");
		assertThat(result.taskType()).isEqualTo(ExcelTaskType.EXPORT);
		assertThat(result.taskStatus()).isEqualTo(ExcelTaskStatus.PENDING);
		assertThat(result.totalRows()).isEqualTo(1000L);
		assertThat(result.processedRows()).isEqualTo(0L);
		assertThat(result.progressPercentage()).isEqualTo(0.0);
		verify(taskJpaRepository).saveAndFlush(any(ExcelTaskJpaEntity.class));
	}

	/**
	 * Feature: Excel 長任務進度更新功能
	 * Scenario: 成功更新已處理列數並自動計算進度百分比
	 * Given: 存在一個總列數為 1000 之長任務實體
	 * When: 調用 adapter.updateProgress("task-123", 500, 1000)
	 * Then: 狀態應變更為 PROCESSING，已處理筆數為 500，進度百分比為 50.0%
	 */
	@Test
	@DisplayName("場景：成功更新長任務進度與計算百分比")
	void shouldUpdateProgressSuccessfully() {
		// Given
		ExcelTaskJpaEntity entity = new ExcelTaskJpaEntity(
				"task-123",
				ExcelTaskType.EXPORT,
				ExcelTaskStatus.PENDING,
				1000L,
				0L,
				0.0,
				"materials.xlsx",
				null,
				null);
		when(taskJpaRepository.findById("task-123")).thenReturn(Optional.of(entity));
		when(taskJpaRepository.saveAndFlush(any(ExcelTaskJpaEntity.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		// When
		ExcelTaskGottenResult result = adapter.updateProgress("task-123", 500L, 1000L);

		// Then
		assertThat(result.taskStatus()).isEqualTo(ExcelTaskStatus.PROCESSING);
		assertThat(result.processedRows()).isEqualTo(500L);
		assertThat(result.progressPercentage()).isEqualTo(50.0);
	}

	/**
	 * Feature: Excel 長任務標記處理中功能
	 * Scenario: 成功將長任務狀態推進為 PROCESSING
	 * Given: 存在一個 PENDING 狀態之任務實體
	 * When: 調用 adapter.markProcessing("task-123")
	 * Then: 任務狀態應推進為 PROCESSING
	 */
	@Test
	@DisplayName("場景：成功標記長任務為處理中狀態")
	void shouldMarkProcessingSuccessfully() {
		// Given
		ExcelTaskJpaEntity entity = new ExcelTaskJpaEntity(
				"task-123",
				ExcelTaskType.EXPORT,
				ExcelTaskStatus.PENDING,
				100L,
				0L,
				0.0,
				"data.xlsx",
				null,
				null);
		when(taskJpaRepository.findById("task-123")).thenReturn(Optional.of(entity));
		when(taskJpaRepository.saveAndFlush(any(ExcelTaskJpaEntity.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		// When
		ExcelTaskGottenResult result = adapter.markProcessing("task-123");

		// Then
		assertThat(result.taskStatus()).isEqualTo(ExcelTaskStatus.PROCESSING);
	}

	/**
	 * Feature: Excel 長任務標記完成功能
	 * Scenario: 成功將長任務標記為 COMPLETED 並記錄下載連結
	 * Given: 存在一個 PROCESSING 狀態之任務實體
	 * When: 調用 adapter.markCompleted("task-123", "/download/123", 1000L)
	 * Then: 任務狀態應為 COMPLETED，進度百分比應為 100.0%，且記錄下載連結
	 */
	@Test
	@DisplayName("場景：成功標記長任務完成")
	void shouldMarkCompletedSuccessfully() {
		// Given
		ExcelTaskJpaEntity entity = new ExcelTaskJpaEntity(
				"task-123",
				ExcelTaskType.EXPORT,
				ExcelTaskStatus.PROCESSING,
				1000L,
				500L,
				50.0,
				"data.xlsx",
				null,
				null);
		when(taskJpaRepository.findById("task-123")).thenReturn(Optional.of(entity));
		when(taskJpaRepository.saveAndFlush(any(ExcelTaskJpaEntity.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		// When
		ExcelTaskGottenResult result = adapter.markCompleted("task-123", "/download/123", 1000L);

		// Then
		assertThat(result.taskStatus()).isEqualTo(ExcelTaskStatus.COMPLETED);
		assertThat(result.progressPercentage()).isEqualTo(100.0);
		assertThat(result.fileUrl()).isEqualTo("/download/123");
	}

	/**
	 * Feature: Excel 長任務標記失敗功能
	 * Scenario: 成功將長任務標記為 FAILED 並記錄錯誤訊息
	 * Given: 存在一個任務實體
	 * When: 調用 adapter.markFailed("task-123", "資料格式損毀")
	 * Then: 任務狀態應為 FAILED 且 errorMessage 應記錄異常原因
	 */
	@Test
	@DisplayName("場景：成功標記長任務失敗")
	void shouldMarkFailedSuccessfully() {
		// Given
		ExcelTaskJpaEntity entity = new ExcelTaskJpaEntity(
				"task-123",
				ExcelTaskType.IMPORT,
				ExcelTaskStatus.PROCESSING,
				0L,
				0L,
				0.0,
				"corrupt.xlsx",
				null,
				null);
		when(taskJpaRepository.findById("task-123")).thenReturn(Optional.of(entity));
		when(taskJpaRepository.saveAndFlush(any(ExcelTaskJpaEntity.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		// When
		ExcelTaskGottenResult result = adapter.markFailed("task-123", "資料格式損毀");

		// Then
		assertThat(result.taskStatus()).isEqualTo(ExcelTaskStatus.FAILED);
		assertThat(result.errorMessage()).isEqualTo("資料格式損毀");
	}

	/**
	 * Feature: 查詢長任務進度
	 * Scenario: 查詢現存任務與不存在任務
	 * Given: 任務 "task-123" 存在而 "unknown" 不存在
	 * When: 分別調用 findTaskById
	 * Then: "task-123" 回傳 Optional.of，"unknown" 回傳 Optional.empty
	 */
	@Test
	@DisplayName("場景：查詢現存與不存在之長任務")
	void shouldFindTaskByIdCorrectly() {
		// Given
		ExcelTaskJpaEntity entity = new ExcelTaskJpaEntity(
				"task-123",
				ExcelTaskType.EXPORT,
				ExcelTaskStatus.PENDING,
				10L,
				0L,
				0.0,
				"test.xlsx",
				null,
				null);
		when(taskJpaRepository.findById("task-123")).thenReturn(Optional.of(entity));
		when(taskJpaRepository.findById("unknown")).thenReturn(Optional.empty());

		// When & Then
		assertThat(adapter.findTaskById("task-123")).isPresent();
		assertThat(adapter.findTaskById("unknown")).isEmpty();
	}

	/**
	 * Feature: 異常狀態處理
	 * Scenario: 當更新不存在之任務時拋出例外
	 * Given: 查無 "not-found" 任務
	 * When: 調用 updateProgress
	 * Then: 應拋出 IllegalArgumentException
	 */
	@Test
	@DisplayName("場景：更新不存在之任務時應拋出例外")
	void shouldThrowExceptionWhenTaskNotFoundOnUpdate() {
		when(taskJpaRepository.findById("not-found")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> adapter.updateProgress("not-found", 10, 100))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("查無長任務實體");
	}
}
