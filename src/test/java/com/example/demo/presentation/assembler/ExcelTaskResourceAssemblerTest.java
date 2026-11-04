package com.example.demo.presentation.assembler;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.demo.application.shared.dto.ExcelTaskGottenResult;
import com.example.demo.application.shared.vo.ExcelTaskStatus;
import com.example.demo.application.shared.vo.ExcelTaskType;
import com.example.demo.presentation.resource.out.ExcelTaskRetrievedResource;

/**
 * Excel 長任務資源組裝器單元測試 (ExcelTaskResourceAssembler Test)
 * <p>
 * 針對 {@link ExcelTaskResourceAssembler} 驗證表現層 Response Resource 與應用層 DTO 之間的單向防腐轉譯能力。
 * 遵循 BDD / Gherkin (Feature, Scenario, Given-When-Then) 測試註解規範。
 */
@DisplayName("ExcelTaskResourceAssembler 表現層組裝器測試")
class ExcelTaskResourceAssemblerTest {

	private ExcelTaskResourceAssembler assembler;

	@BeforeEach
	void setUp() {
		this.assembler = new ExcelTaskResourceAssembler();
	}

	/**
	 * Feature: 封裝長任務查詢回應
	 * Scenario: 成功將 ExcelTaskGottenResult 轉換為 ExcelTaskRetrievedResource
	 * Given: 一個進行中且完成 60% 之任務結果 DTO
	 * When: 調用 assembler.toRetrievedResource(result)
	 * Then: 產出之 ExcelTaskRetrievedResource 欄位應完全精確映射
	 */
	@Test
	@DisplayName("場景：成功封裝長任務結果 DTO 為 ExcelTaskRetrievedResource")
	void shouldConvertToRetrievedResource() {
		// Given
		LocalDateTime now = LocalDateTime.now();
		ExcelTaskGottenResult result = new ExcelTaskGottenResult(
				"task-999",
				ExcelTaskType.IMPORT,
				ExcelTaskStatus.PROCESSING,
				1000L,
				600L,
				60.0,
				"materials.xlsx",
				null,
				null,
				now,
				now
		);

		// When
		ExcelTaskRetrievedResource response = assembler.toRetrievedResource(result);

		// Then
		assertThat(response.taskId()).isEqualTo("task-999");
		assertThat(response.taskType()).isEqualTo(ExcelTaskType.IMPORT);
		assertThat(response.taskStatus()).isEqualTo(ExcelTaskStatus.PROCESSING);
		assertThat(response.totalRows()).isEqualTo(1000L);
		assertThat(response.processedRows()).isEqualTo(600L);
		assertThat(response.progressPercentage()).isEqualTo(60.0);
		assertThat(response.fileName()).isEqualTo("materials.xlsx");
	}
}
