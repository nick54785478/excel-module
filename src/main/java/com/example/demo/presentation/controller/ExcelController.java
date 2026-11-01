package com.example.demo.presentation.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.application.port.in.ExportExcelUseCase;
import com.example.demo.application.port.in.ImportExcelUseCase;
import com.example.demo.application.port.in.StreamExportExcelUseCase;
import com.example.demo.application.port.in.StreamImportExcelUseCase;
import com.example.demo.application.shared.command.ExportExcelCommand;
import com.example.demo.application.shared.command.StreamExportExcelCommand;
import com.example.demo.application.shared.command.StreamImportExcelCommand;
import com.example.demo.application.shared.dto.ExcelGottenResult;
import com.example.demo.application.shared.dto.ExcelStreamImportGottenResult;
import com.example.demo.application.shared.query.ReadExcelQuery;
import com.example.demo.presentation.assembler.ExcelResourceAssembler;
import com.example.demo.presentation.resource.in.ExportExcelResource;
import com.example.demo.presentation.resource.in.StreamExportExcelResource;
import com.example.demo.presentation.resource.out.ExcelImportedResource;
import com.example.demo.presentation.resource.out.ExcelStreamImportedResource;
import com.example.demo.presentation.resource.out.ValidationFailedResource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

/**
 * Excel 檔案處理 REST 控制器 (Presentation Inbound Adapter)
 * <p>
 * 遵循 Clean Architecture 表現層職責，作為外部 HTTP 請求進入點。
 * 接收 Request Resource，透過 Spring Validation ({@code @Valid}) 進行基礎格式與範圍檢核，
 * 調用 {@link ExcelResourceAssembler} 防腐轉換為 Command/Query，
 * 發派給應用層 UseCase 業務合約，並將輸出結果封裝為 Response Resource 或檔案下載串流。
 */
@Tag(name = "Excel 檔案處理介面", description = "提供 Excel 檔案標準批次匯入/匯出與百萬級大數據串流讀寫 REST API")
@Validated
@RestController
@RequestMapping("/api/v1/excel")
public class ExcelController {

	private static final String EXCEL_MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

	private final ExportExcelUseCase exportExcelUseCase;
	private final StreamExportExcelUseCase streamExportExcelUseCase;
	private final ImportExcelUseCase importExcelUseCase;
	private final StreamImportExcelUseCase streamImportExcelUseCase;
	private final ExcelResourceAssembler resourceAssembler;

	public ExcelController(
			ExportExcelUseCase exportExcelUseCase,
			StreamExportExcelUseCase streamExportExcelUseCase,
			ImportExcelUseCase importExcelUseCase,
			StreamImportExcelUseCase streamImportExcelUseCase,
			ExcelResourceAssembler resourceAssembler
	) {
		this.exportExcelUseCase = Objects.requireNonNull(exportExcelUseCase, "exportExcelUseCase must not be null");
		this.streamExportExcelUseCase = Objects.requireNonNull(streamExportExcelUseCase, "streamExportExcelUseCase must not be null");
		this.importExcelUseCase = Objects.requireNonNull(importExcelUseCase, "importExcelUseCase must not be null");
		this.streamImportExcelUseCase = Objects.requireNonNull(streamImportExcelUseCase, "streamImportExcelUseCase must not be null");
		this.resourceAssembler = Objects.requireNonNull(resourceAssembler, "resourceAssembler must not be null");
	}

	/**
	 * 標準批次匯出 Excel 檔案 (In-Memory Download)
	 *
	 * @param resource 匯出結構資料載體
	 * @return Excel 二進位檔案下載回應
	 */
	@Operation(
			summary = "標準批次匯出 Excel 檔案",
			description = "依據傳入之工作表結構、標題列與資料陣列，於記憶體中建構 Excel (.xlsx) 檔案，並以二進位串流提供下載。"
	)
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "成功生成 Excel 二進位檔案並提供下載",
					content = @Content(
							mediaType = EXCEL_MIME_TYPE,
							schema = @Schema(type = "string", format = "binary")
					)
			),
			@ApiResponse(
					responseCode = "400",
					description = "請求資料校驗失敗 (如空白檔名、未指定工作表等)",
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = ValidationFailedResource.class)
					)
			)
	})
	@PostMapping("/export")
	public ResponseEntity<byte[]> exportExcel(
			@Parameter(description = "匯出結構載體", required = true)
			@Valid @RequestBody ExportExcelResource resource
	) {
		ExportExcelCommand command = resourceAssembler.toExportCommand(resource);
		byte[] fileBytes = exportExcelUseCase.export(command);

		String encodedFileName = URLEncoder.encode(resource.fileName(), StandardCharsets.UTF_8).replace("+", "%20");
		if (!encodedFileName.endsWith(".xlsx")) {
			encodedFileName += ".xlsx";
		}

		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
				.contentType(MediaType.parseMediaType(EXCEL_MIME_TYPE))
				.body(fileBytes);
	}

	/**
	 * 大數據串流匯出 Excel 檔案 (Streaming Direct to Response Output)
	 *
	 * @param resource 串流匯出參數載體
	 * @param response HTTP Servlet 回應物件
	 * @throws IOException 當輸出串流發生寫入錯誤時拋出
	 */
	@Operation(
			summary = "大數據串流匯出 Excel 檔案",
			description = "針對百萬筆大數據資料，邊寫入邊透過 HTTP 輸出串流 Flush，維持常數級低記憶體消耗，避免 OOM。"
	)
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "成功建立串流輸出傳輸",
					content = @Content(
							mediaType = EXCEL_MIME_TYPE,
							schema = @Schema(type = "string", format = "binary")
					)
			),
			@ApiResponse(
					responseCode = "400",
					description = "串流參數校驗失敗 (如沖刷筆數小於 1 等)",
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = ValidationFailedResource.class)
					)
			)
	})
	@PostMapping("/export/stream")
	public void streamExportExcel(
			@Parameter(description = "串流匯出參數設定", required = true)
			@Valid @RequestBody StreamExportExcelResource resource,
			HttpServletResponse response
	) throws IOException {
		String encodedFileName = URLEncoder.encode(
				resource.fileName() != null ? resource.fileName() : "stream_export.xlsx",
				StandardCharsets.UTF_8
		).replace("+", "%20");

		if (!encodedFileName.endsWith(".xlsx")) {
			encodedFileName += ".xlsx";
		}

		response.setContentType(EXCEL_MIME_TYPE);
		response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName);

		StreamExportExcelCommand command = resourceAssembler.toStreamExportCommand(resource);
		streamExportExcelUseCase.streamExport(response.getOutputStream(), command);
		response.flushBuffer();
	}

	/**
	 * 檔案上傳並完整解析 Excel 內容
	 *
	 * @param file       上傳之 Excel 檔案
	 * @param sheetName  可選：指定工作表名稱
	 * @param sheetIndex 可選：指定工作表索引 (不可小於 0)
	 * @return 解析後的工作表結構與資料回應
	 * @throws IOException 當讀取檔案輸入串流失敗時拋出
	 */
	@Operation(
			summary = "檔案上傳並完整解析 Excel 內容",
			description = "透過 Multipart 形式上傳 Excel 檔案，讀取各工作表欄位名稱與資料列，封裝為結構化 JSON 回應。"
	)
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "成功解析 Excel 內容",
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = ExcelImportedResource.class)
					)
			),
			@ApiResponse(
					responseCode = "400",
					description = "檔案為空或參數錯誤",
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = ValidationFailedResource.class)
					)
			)
	})
	@PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ExcelImportedResource> importExcel(
			@Parameter(description = "欲上傳解析之 Excel 檔案 (.xlsx / .xls)", required = true)
			@RequestParam("file") MultipartFile file,
			@Parameter(description = "指定讀取之工作表名稱 (可選)")
			@RequestParam(value = "sheetName", required = false) String sheetName,
			@Parameter(description = "指定讀取之工作表索引 (0-based，可選)")
			@RequestParam(value = "sheetIndex", required = false) @Min(value = 0, message = "工作表索引 (sheetIndex) 不能小於 0") Integer sheetIndex
	) throws IOException {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("上傳之 Excel 檔案不能為空");
		}

		ReadExcelQuery.Builder queryBuilder = ReadExcelQuery.builder();
		if (sheetName != null && !sheetName.isBlank()) {
			queryBuilder.sheetName(sheetName);
		}
		if (sheetIndex != null) {
			queryBuilder.sheetIndex(sheetIndex);
		}

		ExcelGottenResult result = importExcelUseCase.importExcel(file.getInputStream(), queryBuilder.build());
		return ResponseEntity.ok(resourceAssembler.toImportedResource(result));
	}

	/**
	 * 檔案上傳並執行大數據串流分塊解析統計
	 *
	 * @param file      上傳之 Excel 檔案
	 * @param sheetName 可選：指定工作表名稱
	 * @param chunkSize 分塊處理筆數大小 (預設 1,000，必須大於等於 1)
	 * @return 串流分塊處理統計結果
	 * @throws IOException 當讀取檔案輸入串流失敗時拋出
	 */
	@Operation(
			summary = "檔案上傳並執行大數據串流分塊解析",
			description = "上傳超大型 Excel 檔案，以指定 Chunk 大小（如 1000 筆）進行分塊串流消費與業務處理，產出處理指標統計。"
	)
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "大數據串流解析統計完成",
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = ExcelStreamImportedResource.class)
					)
			),
			@ApiResponse(
					responseCode = "400",
					description = "檔案為空或分塊大小無效",
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = ValidationFailedResource.class)
					)
			)
	})
	@PostMapping(value = "/import/stream", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ExcelStreamImportedResource> streamImportExcel(
			@Parameter(description = "欲上傳串流解析之大數據 Excel 檔案", required = true)
			@RequestParam("file") MultipartFile file,
			@Parameter(description = "指定讀取之工作表名稱 (可選)")
			@RequestParam(value = "sheetName", required = false) String sheetName,
			@Parameter(description = "每批分塊消費之列數大小 (預設 1,000，不可小於 1)")
			@RequestParam(value = "chunkSize", defaultValue = "1000") @Min(value = 1, message = "分塊大小 (chunkSize) 必須大於等於 1") int chunkSize
	) throws IOException {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("上傳之 Excel 檔案不能為空");
		}

		StreamImportExcelCommand command = new StreamImportExcelCommand(file.getInputStream(), sheetName, chunkSize);
		ExcelStreamImportGottenResult result = streamImportExcelUseCase.streamImport(command);
		return ResponseEntity.ok(resourceAssembler.toStreamImportedResource(result));
	}
}
