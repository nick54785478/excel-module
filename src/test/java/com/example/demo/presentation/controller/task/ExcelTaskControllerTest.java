package com.example.demo.presentation.controller.task;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.demo.application.port.in.task.DownloadExcelTaskFileUseCase;
import com.example.demo.application.port.in.task.GetExcelTaskUseCase;
import com.example.demo.application.shared.dto.ExcelTaskGottenResult;
import com.example.demo.application.shared.exception.ExcelTaskNotFoundException;
import com.example.demo.application.shared.query.GetExcelTaskQuery;
import com.example.demo.application.shared.vo.ExcelTaskStatus;
import com.example.demo.application.shared.vo.ExcelTaskType;
import com.example.demo.presentation.assembler.ExcelTaskResourceAssembler;
import com.example.demo.presentation.exception.GlobalExceptionHandler;

/**
 * Excel 長任務 REST 控制器單元測試 (ExcelTaskController Test)
 * <p>
 * 針對精簡後的 {@link ExcelTaskController} 驗證 HTTP 200 OK 進度查詢、HTTP 404 查無任務例外轉譯、
 * 與已完成產物之二進位檔案下載串流回應。
 * 遵循 BDD / Gherkin (Feature, Scenario, Given-When-Then) 測試註解規範。
 */
@DisplayName("ExcelTaskController 非同步長任務 REST 控制器測試")
class ExcelTaskControllerTest {

	private MockMvc mockMvc;
	private GetExcelTaskUseCase getExcelTaskUseCase;
	private DownloadExcelTaskFileUseCase downloadExcelTaskFileUseCase;

	@BeforeEach
	void setUp() {
		this.getExcelTaskUseCase = mock(GetExcelTaskUseCase.class);
		this.downloadExcelTaskFileUseCase = mock(DownloadExcelTaskFileUseCase.class);
		ExcelTaskResourceAssembler resourceAssembler = new ExcelTaskResourceAssembler();

		ExcelTaskController controller = new ExcelTaskController(
				getExcelTaskUseCase,
				downloadExcelTaskFileUseCase,
				resourceAssembler
		);

		this.mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	/**
	 * Feature: 查詢長任務進度端點 (標準 RESTful GET /{taskId} 路徑)
	 * Scenario: 成功透過標準 RESTful 端點查詢現存長任務之進度與狀態
	 * Given: 任務 "task-100" 存在於系統中
	 * When: 發送 GET /api/v1/excel/tasks/task-100
	 * Then: 應回傳 HTTP 200 OK，包含進度百分比與處理列數
	 */
	@Test
	@DisplayName("場景：成功透過標準 RESTful GET /{taskId} 端點查詢長任務進度")
	void shouldGetTaskProgressViaRestfulPathSuccessfully() throws Exception {
		// Given
		ExcelTaskGottenResult taskResult = new ExcelTaskGottenResult(
				"task-100",
				ExcelTaskType.EXPORT,
				ExcelTaskStatus.PROCESSING,
				1000L,
				450L,
				45.0,
				"users.xlsx",
				null,
				null,
				LocalDateTime.now(),
				LocalDateTime.now()
		);
		when(getExcelTaskUseCase.getTask(new GetExcelTaskQuery("task-100")))
				.thenReturn(taskResult);

		// When & Then
		mockMvc.perform(get("/api/v1/excel/tasks/task-100"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.taskId").value("task-100"))
				.andExpect(jsonPath("$.taskStatus").value("PROCESSING"))
				.andExpect(jsonPath("$.totalRows").value(1000))
				.andExpect(jsonPath("$.processedRows").value(450))
				.andExpect(jsonPath("$.progressPercentage").value(45.0));
	}

	/**
	 * Feature: 查詢長任務進度端點
	 * Scenario: 成功查詢現存長任務之進度與狀態
	 * Given: 任務 "task-100" 存在於系統中
	 * When: 發送 GET /api/v1/excel/tasks/task-100/progress
	 * Then: 應回傳 HTTP 200 OK，包含進度百分比與處理列數
	 */
	@Test
	@DisplayName("場景：成功查詢現存長任務之進度詳細資訊 (向後相容 /progress)")
	void shouldGetTaskProgressSuccessfully() throws Exception {
		// Given
		ExcelTaskGottenResult taskResult = new ExcelTaskGottenResult(
				"task-100",
				ExcelTaskType.EXPORT,
				ExcelTaskStatus.PROCESSING,
				1000L,
				450L,
				45.0,
				"users.xlsx",
				null,
				null,
				LocalDateTime.now(),
				LocalDateTime.now()
		);
		when(getExcelTaskUseCase.getTask(new GetExcelTaskQuery("task-100")))
				.thenReturn(taskResult);

		// When & Then
		mockMvc.perform(get("/api/v1/excel/tasks/task-100/progress"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.taskId").value("task-100"))
				.andExpect(jsonPath("$.taskStatus").value("PROCESSING"))
				.andExpect(jsonPath("$.totalRows").value(1000))
				.andExpect(jsonPath("$.processedRows").value(450))
				.andExpect(jsonPath("$.progressPercentage").value(45.0));
	}

	/**
	 * Feature: 查詢不存在長任務進度
	 * Scenario: 查詢不存在之任務回傳 HTTP 404 Not Found
	 * Given: 任務 "task-unknown" 不存在，UseCase 拋出 ExcelTaskNotFoundException
	 * When: 發送 GET /api/v1/excel/tasks/task-unknown/progress
	 * Then: 應由全域例外處理器轉譯為 HTTP 404 Not Found
	 */
	@Test
	@DisplayName("場景：查詢不存在之任務識別碼時應回傳 HTTP 404 Not Found")
	void shouldReturn404WhenTaskNotFound() throws Exception {
		// Given
		when(getExcelTaskUseCase.getTask(new GetExcelTaskQuery("task-unknown")))
				.thenThrow(new ExcelTaskNotFoundException("查無指定識別碼之 Excel 長任務: task-unknown"));

		// When & Then
		mockMvc.perform(get("/api/v1/excel/tasks/task-unknown/progress"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("task-unknown")));
	}

	/**
	 * Feature: 下載長任務產出檔案端點
	 * Scenario: 成功下載已完成任務之產出 Excel 二進位檔案
	 * Given: 任務已完成且檔案快取存在
	 * When: 發送 GET /api/v1/excel/tasks/task-100/download
	 * Then: 應回傳 HTTP 200 OK，帶有 Content-Disposition 標頭與 byte[] 串流
	 */
	@Test
	@DisplayName("場景：成功下載已完成長任務之 Excel 產出二進位檔案")
	void shouldDownloadExportedFileSuccessfully() throws Exception {
		// Given
		ExcelTaskGottenResult taskResult = new ExcelTaskGottenResult(
				"task-100",
				ExcelTaskType.EXPORT,
				ExcelTaskStatus.COMPLETED,
				100L,
				100L,
				100.0,
				"users.xlsx",
				"/download/100",
				null,
				LocalDateTime.now(),
				LocalDateTime.now()
		);
		when(getExcelTaskUseCase.getTask(new GetExcelTaskQuery("task-100"))).thenReturn(taskResult);
		when(downloadExcelTaskFileUseCase.getExportedFile("task-100")).thenReturn(new byte[] { 1, 2, 3, 4, 5 });

		// When & Then
		mockMvc.perform(get("/api/v1/excel/tasks/task-100/download"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("attachment; filename=\"users.xlsx\"")))
				.andExpect(content().bytes(new byte[] { 1, 2, 3, 4, 5 }));
	}

	/**
	 * Feature: 下載未產出檔案防護
	 * Scenario: 下載尚未產出或不存在之檔案時回傳 HTTP 404
	 * Given: 任務快取無該檔案 (回傳 null)
	 * When: 發送 GET /api/v1/excel/tasks/task-100/download
	 * Then: 應回傳 HTTP 404 Not Found
	 */
	@Test
	@DisplayName("場景：下載尚未完成或不存在之長任務檔案時應回傳 HTTP 404")
	void shouldReturn404WhenDownloadFileNotReady() throws Exception {
		// Given
		ExcelTaskGottenResult taskResult = new ExcelTaskGottenResult(
				"task-100",
				ExcelTaskType.EXPORT,
				ExcelTaskStatus.PROCESSING,
				100L,
				50L,
				50.0,
				"users.xlsx",
				null,
				null,
				LocalDateTime.now(),
				LocalDateTime.now()
		);
		when(getExcelTaskUseCase.getTask(new GetExcelTaskQuery("task-100"))).thenReturn(taskResult);
		when(downloadExcelTaskFileUseCase.getExportedFile("task-100")).thenReturn(null);

		// When & Then
		mockMvc.perform(get("/api/v1/excel/tasks/task-100/download"))
				.andExpect(status().isNotFound());
	}
}
