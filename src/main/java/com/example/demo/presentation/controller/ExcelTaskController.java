package com.example.demo.presentation.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.application.port.in.GetExcelTaskUseCase;
import com.example.demo.application.service.ExcelTaskCommandService;
import com.example.demo.application.shared.dto.ExcelTaskGottenResult;
import com.example.demo.application.shared.exception.ExcelTaskNotFoundException;
import com.example.demo.application.shared.query.GetExcelTaskQuery;
import com.example.demo.presentation.assembler.ExcelTaskResourceAssembler;
import com.example.demo.presentation.resource.out.ExcelTaskRetrievedResource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;

/**
 * Excel 長任務管理控制器 (Excel Task Management REST Controller / Inbound Adapter)
 * <p>
 * 遵循 Clean Architecture 表現層職責，專職聚焦於通用長任務之「進度輪詢」與「檔案產物下載」（符合單一職責原則 SRP）。
 * 長任務提交本身由 Annotation-Driven ({@code @TrackExcelTask}) 切面或業務 UseCase 自主承接，
 * 本控制器專注於提供任務進度查詢 (Progress Polling) 與產出檔案下載 (Artifact Retrieval) 之 REST API。
 */
@Validated
@RestController
@RequestMapping("/api/v1/excel/tasks")
@Tag(name = "Excel 非同步長任務控制器", description = "提供 Excel 長任務進度輪詢、狀態追蹤與產出檔案下載之 REST API")
public class ExcelTaskController {

	private final GetExcelTaskUseCase getExcelTaskUseCase;
	private final ExcelTaskCommandService excelTaskCommandService;
	private final ExcelTaskResourceAssembler resourceAssembler;

	public ExcelTaskController(
			GetExcelTaskUseCase getExcelTaskUseCase,
			ExcelTaskCommandService excelTaskCommandService,
			ExcelTaskResourceAssembler resourceAssembler
	) {
		this.getExcelTaskUseCase = Objects.requireNonNull(getExcelTaskUseCase, "getExcelTaskUseCase 不得為空");
		this.excelTaskCommandService = Objects.requireNonNull(excelTaskCommandService, "excelTaskCommandService 不得為空");
		this.resourceAssembler = Objects.requireNonNull(resourceAssembler, "resourceAssembler 不得為空");
	}

	/**
	 * 查詢指定識別碼之長任務當前進度與執行狀態
	 *
	 * @param taskId 任務唯一識別碼
	 * @return HTTP 200 OK 與任務進度詳細資訊
	 */
	@GetMapping(value = "/{taskId}/progress", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "查詢 Excel 長任務進度與狀態", description = "提供前端輪詢當前任務狀態、總列數、已處理筆數、百分比與錯誤資訊")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "查詢成功，回傳進度資訊"),
			@ApiResponse(responseCode = "404", description = "查無該 taskId 對應之任務")
	})
	public ResponseEntity<ExcelTaskRetrievedResource> getTaskProgress(
			@Parameter(description = "任務唯一識別碼", required = true)
			@PathVariable("taskId") @NotBlank(message = "任務識別碼 (taskId) 不得為空") String taskId
	) {
		GetExcelTaskQuery query = new GetExcelTaskQuery(taskId);
		ExcelTaskGottenResult taskResult = getExcelTaskUseCase.getTask(query);
		ExcelTaskRetrievedResource response = resourceAssembler.toRetrievedResource(taskResult);
		return ResponseEntity.ok(response);
	}

	/**
	 * 下載已完成長任務產出之 Excel 檔案
	 *
	 * @param taskId 任務唯一識別碼
	 * @return Excel 二進位檔案下載串流
	 */
	@GetMapping(value = "/{taskId}/download")
	@Operation(summary = "下載已完成長任務之 Excel 產出檔案", description = "當任務狀態為 COMPLETED 且檔案產出後，可經由此端點下載二進位檔案")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "檔案下載成功"),
			@ApiResponse(responseCode = "404", description = "查無該任務產出檔案或任務尚未完成")
	})
	public ResponseEntity<byte[]> downloadExportedFile(
			@Parameter(description = "任務唯一識別碼", required = true)
			@PathVariable("taskId") @NotBlank(message = "任務識別碼 (taskId) 不得為空") String taskId
	) {
		GetExcelTaskQuery query = new GetExcelTaskQuery(taskId);
		ExcelTaskGottenResult taskResult = getExcelTaskUseCase.getTask(query);

		byte[] fileBytes = excelTaskCommandService.getExportedFile(taskId);
		if (fileBytes == null) {
			throw new ExcelTaskNotFoundException("長任務匯出檔案尚未產生或已過期: " + taskId);
		}

		String fileName = taskResult.fileName() != null && !taskResult.fileName().isBlank()
				? taskResult.fileName()
				: "download.xlsx";

		String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");

		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName)
				.contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				.body(fileBytes);
	}
}

