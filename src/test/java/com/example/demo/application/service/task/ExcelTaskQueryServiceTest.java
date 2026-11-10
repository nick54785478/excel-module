package com.example.demo.application.service.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.demo.application.port.out.task.ExcelTaskRepositoryPort;
import com.example.demo.application.shared.dto.ExcelTaskGottenResult;
import com.example.demo.application.shared.vo.ExcelTaskStatus;
import com.example.demo.application.shared.vo.ExcelTaskType;
import com.example.demo.application.shared.exception.ExcelTaskNotFoundException;
import com.example.demo.application.shared.query.GetExcelTaskQuery;

/**
 * Excel 長任務進度查詢應用服務單元測試 (ExcelTaskQueryService Test)
 * <p>
 * 針對 {@link ExcelTaskQueryService} 驗證實作
 * {@link com.example.demo.application.port.in.GetExcelTaskUseCase}
 * 查詢合約之正常流程與查無資料時之例外拋出。
 * 遵循 BDD / Gherkin (Feature, Scenario, Given-When-Then) 測試註解規範。
 */
@DisplayName("ExcelTaskQueryService 長任務查詢業務服務測試")
class ExcelTaskQueryServiceTest {

	private ExcelTaskRepositoryPort excelTaskRepositoryPort;
	private ExcelTaskQueryService queryService;

	@BeforeEach
	void setUp() {
		this.excelTaskRepositoryPort = mock(ExcelTaskRepositoryPort.class);
		this.queryService = new ExcelTaskQueryService(excelTaskRepositoryPort);
	}

	/**
	 * Feature: 查詢長任務進度
	 * Scenario: 成功取得已存在任務之最新狀態與進度
	 * Given: 任務識別碼為 "task-100" 且存在於儲存庫中
	 * When: 調用 queryService.getTask(new GetExcelTaskQuery("task-100"))
	 * Then: 應正確回傳包含進度百分比與狀態之 ExcelTaskGottenResult
	 */
	@Test
	@DisplayName("場景：成功查詢現存長任務之進度與狀態")
	void shouldGetTaskSuccessfullyWhenExists() {
		// Given
		GetExcelTaskQuery query = new GetExcelTaskQuery("task-100");
		ExcelTaskGottenResult mockResult = new ExcelTaskGottenResult(
				"task-100",
				ExcelTaskType.EXPORT,
				ExcelTaskStatus.PROCESSING,
				1000L,
				450L,
				45.0,
				"export.xlsx",
				null,
				null,
				LocalDateTime.now(),
				LocalDateTime.now());
		when(excelTaskRepositoryPort.findTaskById("task-100")).thenReturn(Optional.of(mockResult));

		// When
		ExcelTaskGottenResult result = queryService.getTask(query);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.taskId()).isEqualTo("task-100");
		assertThat(result.taskStatus()).isEqualTo(ExcelTaskStatus.PROCESSING);
		assertThat(result.progressPercentage()).isEqualTo(45.0);
		verify(excelTaskRepositoryPort).findTaskById("task-100");
	}

	/**
	 * Feature: 查詢長任務進度
	 * Scenario: 當查詢不存在之任務識別碼時拋出 ExcelTaskNotFoundException
	 * Given: 任務識別碼為 "task-unknown" 且儲存庫查無資料
	 * When: 調用 queryService.getTask(new GetExcelTaskQuery("task-unknown"))
	 * Then: 應拋出 ExcelTaskNotFoundException 例外
	 */
	@Test
	@DisplayName("場景：查詢不存在之長任務時應拋出 ExcelTaskNotFoundException")
	void shouldThrowExcelTaskNotFoundExceptionWhenTaskNotFound() {
		// Given
		GetExcelTaskQuery query = new GetExcelTaskQuery("task-unknown");
		when(excelTaskRepositoryPort.findTaskById("task-unknown")).thenReturn(Optional.empty());

		// When & Then
		assertThatThrownBy(() -> queryService.getTask(query))
				.isInstanceOf(ExcelTaskNotFoundException.class)
				.hasMessageContaining("task-unknown");
	}
}
