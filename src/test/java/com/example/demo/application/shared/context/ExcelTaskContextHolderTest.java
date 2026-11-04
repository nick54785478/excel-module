package com.example.demo.application.shared.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.demo.application.port.out.ExcelTaskRepositoryPort;
import com.example.demo.application.shared.vo.ExcelTaskType;

/**
 * Excel 長任務執行緒上下文保持器單元測試 (ExcelTaskContextHolder Test)
 * <p>
 * 遵循 BDD / Gherkin (Feature, Scenario, Given-When-Then) 測試註解規範。
 */
@DisplayName("ExcelTaskContextHolder 執行緒上下文保持器測試")
class ExcelTaskContextHolderTest {

	@AfterEach
	void tearDown() {
		ExcelTaskContextHolder.unbind();
	}

	/**
	 * Feature: 上下文綁定與清除
	 * Scenario: 成功綁定任務資訊並於 unbind 後清除
	 * Given: 任務 ID 為 "task-1"、總列數為 500
	 * When: 調用 bind("task-1", ...)
	 * Then: isActive 為 true，且可獲取正確之任務屬性；unbind 後 isActive 變為 false
	 */
	@Test
	@DisplayName("場景：成功綁定任務資訊並於 unbind 後清除")
	void shouldBindAndUnbindContextSuccessfully() {
		// Given
		ExcelTaskRepositoryPort mockPort = mock(ExcelTaskRepositoryPort.class);

		// When
		ExcelTaskContextHolder.bind("task-1", ExcelTaskType.EXPORT, "data.xlsx", 500L, mockPort);

		// Then
		assertThat(ExcelTaskContextHolder.isActive()).isTrue();
		assertThat(ExcelTaskContextHolder.getTaskId()).isEqualTo("task-1");
		assertThat(ExcelTaskContextHolder.getTotalRows()).isEqualTo(500L);
		assertThat(ExcelTaskContextHolder.getProcessedRows()).isEqualTo(0L);

		// When unbind
		ExcelTaskContextHolder.unbind();

		// Then
		assertThat(ExcelTaskContextHolder.isActive()).isFalse();
		assertThat(ExcelTaskContextHolder.getTaskId()).isNull();
	}

	/**
	 * Feature: 進度增量更新
	 * Scenario: 調用 incrementProgress 時同步累加計數並通知 RepositoryPort
	 * Given: 綁定總列數 1000 之任務
	 * When: 連續 incrementProgress(200) 與 incrementProgress(300)
	 * Then: 已處理筆數為 500，且 RepositoryPort 收到對應次數之 updateProgress 調用
	 */
	@Test
	@DisplayName("場景：調用 incrementProgress 時同步累加計數並通知 RepositoryPort")
	void shouldIncrementProgressAndNotifyRepository() {
		// Given
		ExcelTaskRepositoryPort mockPort = mock(ExcelTaskRepositoryPort.class);
		ExcelTaskContextHolder.bind("task-1", ExcelTaskType.IMPORT, "data.xlsx", 1000L, mockPort);

		// When
		long first = ExcelTaskContextHolder.incrementProgress(200);
		long second = ExcelTaskContextHolder.incrementProgress(300);

		// Then
		assertThat(first).isEqualTo(200L);
		assertThat(second).isEqualTo(500L);
		assertThat(ExcelTaskContextHolder.getProcessedRows()).isEqualTo(500L);

		verify(mockPort).updateProgress("task-1", 200L, 1000L);
		verify(mockPort).updateProgress("task-1", 500L, 1000L);
	}

	/**
	 * Feature: 絕對進度更新
	 * Scenario: 調用 updateProgress 時直接覆寫計數並通知 RepositoryPort
	 * Given: 綁定任務
	 * When: 調用 updateProgress(800, 1000)
	 * Then: 已處理筆數為 800
	 */
	@Test
	@DisplayName("場景：調用 updateProgress 時直接覆寫計數並通知 RepositoryPort")
	void shouldUpdateProgressDirectly() {
		// Given
		ExcelTaskRepositoryPort mockPort = mock(ExcelTaskRepositoryPort.class);
		ExcelTaskContextHolder.bind("task-1", ExcelTaskType.EXPORT, "data.xlsx", 1000L, mockPort);

		// When
		ExcelTaskContextHolder.updateProgress(800L, 1000L);

		// Then
		assertThat(ExcelTaskContextHolder.getProcessedRows()).isEqualTo(800L);
		verify(mockPort).updateProgress("task-1", 800L, 1000L);
	}
}

