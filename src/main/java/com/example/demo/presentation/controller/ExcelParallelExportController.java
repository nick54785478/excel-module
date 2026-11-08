package com.example.demo.presentation.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.application.port.in.ExportParallelExcelUseCase;
import com.example.demo.application.shared.command.ExportPartitionZipCommand;
import com.example.demo.application.shared.command.ExportPipelineExcelCommand;
import com.example.demo.presentation.assembler.ExcelParallelResourceAssembler;
import com.example.demo.presentation.resource.in.ExportParallelPipelineResource;
import com.example.demo.presentation.resource.in.ExportPartitionZipResource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

/**
 * 高並發平行管線與分片 ZIP 匯出控制器 (Excel Parallel & Partition Export Presentation Controller)
 * <p>
 * 遵循 Clean Architecture 六角形架構表現層規範，作為外部 HTTP 請求進入點。<br>
 * 具體展示與驗證「高並發與執行緒安全架構」之實踐：
 * <ol>
 *     <li><b>模式 A (微觀管線)</b>：平行查詢生產端 + 背壓佇列 + 單寫入緒循序寫入 SXSSF/FastExcel。</li>
 *     <li><b>模式 B (宏觀分片)</b>：多個活頁簿在多核心上完全並行產製，最後動態串流壓縮寫入 ZIP 輸出流。</li>
 *     <li><b>模式 A + B 終極組合拳</b>：宏觀多檔案並行，各分片內部多緒查詢，徹底釋放多核吞吐量並確保 100% 執行緒安全。</li>
 * </ol>
 */
@Tag(name = "Excel 高並發平行管線與分片 ZIP 匯出介面", description = "提供生產者-消費者背壓管線加速 (模式 A) 與多活頁簿 ZIP 分片並行打包 (模式 B/A+B) 之 REST API")
@Validated
@RestController
@RequestMapping("/api/v1/excel/parallel")
public class ExcelParallelExportController {

	private static final String EXCEL_MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
	private static final String ZIP_MIME_TYPE = "application/zip";

	private final ExportParallelExcelUseCase parallelExcelUseCase;
	private final ExcelParallelResourceAssembler resourceAssembler;

	public ExcelParallelExportController(
			ExportParallelExcelUseCase parallelExcelUseCase,
			ExcelParallelResourceAssembler resourceAssembler
	) {
		this.parallelExcelUseCase = Objects.requireNonNull(parallelExcelUseCase, "ExportParallelExcelUseCase must not be null");
		this.resourceAssembler = Objects.requireNonNull(resourceAssembler, "ExcelParallelResourceAssembler must not be null");
	}

	/**
	 * 一鍵瀏覽器 GET 實測：模式 A 微觀生產者-消費者管線匯出 (預設 10,000 筆)
	 *
	 * @param totalRows 預計產製總列數 (預設 10,000)
	 * @return Excel (.xlsx) 下載串流
	 */
	@Operation(
			summary = "【一鍵實測・模式 A】微觀生產者-消費者管線平行匯出 (支援瀏覽器直接 GET 下載)",
			description = "由 4 個平行執行緒拉取資料並推入有界阻斷佇列 (背壓防爆記憶體)，單一專屬寫入緒循序寫出 SXSSF/FastExcel"
	)
	@GetMapping("/pipeline-sample")
	public ResponseEntity<byte[]> exportPipelineSample(
			@Parameter(description = "總產製列數 (預設 10,000)", example = "10000")
			@RequestParam(required = false, defaultValue = "10000") int totalRows
	) {
		ExportParallelPipelineResource resource = new ExportParallelPipelineResource(
				"模式A管線加速匯出",
				totalRows,
				1000,
				30,
				4,
				null
		);
		ExportPipelineExcelCommand command = resourceAssembler.toCommand(resource, null);
		byte[] fileBytes = parallelExcelUseCase.exportPipeline(command);
		return createAttachmentResponse(fileBytes, "parallel_pipeline_sample.xlsx", EXCEL_MIME_TYPE);
	}

	/**
	 * HTTP POST 實測：模式 A 自訂參數管線匯出
	 *
	 * @param resource 管線請求參數資源
	 * @return Excel (.xlsx) 下載串流
	 */
	@Operation(
			summary = "【自訂參數・模式 A】微觀生產者-消費者管線平行匯出",
			description = "支援前端自訂總列數、批次區塊大小 (chunkSize)、背壓佇列容量 (queueCapacity) 與平行執行緒數"
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "成功產製並下載 Excel 報表"),
			@ApiResponse(responseCode = "400", description = "請求參數不合法"),
			@ApiResponse(responseCode = "500", description = "伺服器內部產生異常")
	})
	@PostMapping("/pipeline")
	public ResponseEntity<byte[]> exportPipelineCustom(
			@RequestBody @Valid ExportParallelPipelineResource resource
	) {
		ExportPipelineExcelCommand command = resourceAssembler.toCommand(resource, null);
		byte[] fileBytes = parallelExcelUseCase.exportPipeline(command);
		String fileName = (command.sheetName() != null && !command.sheetName().isBlank())
				? command.sheetName().trim() + ".xlsx"
				: "parallel_pipeline_export.xlsx";
		return createAttachmentResponse(fileBytes, fileName, EXCEL_MIME_TYPE);
	}

	/**
	 * HTTP POST 串流匯出：模式 A 微觀生產者-消費者管線匯出 (直接寫入 Response OutputStream)
	 *
	 * @param resource 管線請求參數資源
	 * @param response HTTP Servlet 回應物件
	 * @throws IOException 當輸出串流操作異常時拋出
	 */
	@Operation(
			summary = "【串流寫入・模式 A】微觀生產者-消費者管線平行匯出至 HTTP 輸出串流",
			description = "直接將生產者-消費者管線寫入至 Servlet OutputStream，完全不佔用 JVM 堆疊記憶體緩衝"
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "成功串流產製並下載 Excel 報表"),
			@ApiResponse(responseCode = "400", description = "請求參數不合法"),
			@ApiResponse(responseCode = "500", description = "伺服器內部產生異常")
	})
	@PostMapping("/pipeline/stream")
	public void exportPipelineStream(
			@RequestBody @Valid ExportParallelPipelineResource resource,
			HttpServletResponse response
	) throws IOException {
		ExportPipelineExcelCommand command = resourceAssembler.toCommand(resource, null);
		String fileName = (command.sheetName() != null && !command.sheetName().isBlank())
				? command.sheetName().trim()
				: "parallel_pipeline_stream";
		if (!fileName.endsWith(".xlsx")) {
			fileName += ".xlsx";
		}
		String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
		response.setContentType(EXCEL_MIME_TYPE);
		response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName);
		parallelExcelUseCase.exportPipelineStream(response.getOutputStream(), command);
		response.flushBuffer();
	}

	/**
	 * 一鍵瀏覽器 GET 實測：模式 B / A+B 多活頁簿 ZIP 分片並行打包 (預設 25,000 筆，每 10,000 筆一分片)
	 *
	 * @param totalRows      預計產製總列數 (預設 25,000)
	 * @param maxRowsPerFile 單一活頁簿列數上限 (預設 10,000)
	 * @return ZIP 壓縮檔案二進位下載串流
	 */
	@Operation(
			summary = "【一鍵實測・模式 B / A+B】多活頁簿 ZIP 分片平行產製並壓縮打包 (支援瀏覽器直接 GET 下載)",
			description = "突破單表 104 萬列天花板，多個獨立活頁簿在多核心上完全並行產製，最後動態串流壓縮為單一 ZIP 檔"
	)
	@GetMapping("/partition-zip-sample")
	public ResponseEntity<byte[]> exportPartitionZipSample(
			@Parameter(description = "總產製列數 (預設 25,000)", example = "25000")
			@RequestParam(required = false, defaultValue = "25000") int totalRows,

			@Parameter(description = "單一活頁簿上限列數 (預設 10,000，將自動產生 3 個分片)", example = "10000")
			@RequestParam(required = false, defaultValue = "10000") int maxRowsPerFile
	) {
		ExportPartitionZipResource resource = new ExportPartitionZipResource(
				"orders_multi_partition_archive.zip",
				"orders_part",
				"訂單明細",
				totalRows,
				maxRowsPerFile,
				1000,
				4,
				true,
				null
		);
		ExportPartitionZipCommand command = resourceAssembler.toCommand(resource, null);
		byte[] zipBytes = parallelExcelUseCase.exportPartitionZip(command);
		return createAttachmentResponse(zipBytes, command.zipFileName(), ZIP_MIME_TYPE);
	}

	/**
	 * HTTP POST 實測：模式 B / A+B 自訂參數多活頁簿 ZIP 分片匯出
	 *
	 * @param resource 分片 ZIP 請求參數資源
	 * @return ZIP 壓縮檔案二進位下載串流
	 */
	@Operation(
			summary = "【自訂參數・模式 B / A+B】多活頁簿 ZIP 分片平行匯出",
			description = "支援自訂單一檔案上限列數 (maxRowsPerFile)，分片內部可無縫切換是否開啟模式 A 管線加速"
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "成功產製並下載分片 ZIP 壓縮檔"),
			@ApiResponse(responseCode = "400", description = "請求參數不合法"),
			@ApiResponse(responseCode = "500", description = "伺服器內部產生異常")
	})
	@PostMapping("/partition-zip")
	public ResponseEntity<byte[]> exportPartitionZipCustom(
			@RequestBody @Valid ExportPartitionZipResource resource
	) {
		ExportPartitionZipCommand command = resourceAssembler.toCommand(resource, null);
		byte[] zipBytes = parallelExcelUseCase.exportPartitionZip(command);
		return createAttachmentResponse(zipBytes, command.zipFileName(), ZIP_MIME_TYPE);
	}

	/**
	 * HTTP POST 串流匯出：模式 B / A+B 多活頁簿 ZIP 分片匯出 (直接寫入 Response OutputStream)
	 *
	 * @param resource 分片 ZIP 請求參數資源
	 * @param response HTTP Servlet 回應物件
	 * @throws IOException 當輸出串流操作異常時拋出
	 */
	@Operation(
			summary = "【串流壓縮・模式 B / A+B】多活頁簿 ZIP 分片平行產製並動態串流壓縮寫入 HTTP 輸出串流",
			description = "直接將各分片檔案動態串流壓縮寫入 Servlet OutputStream，突破百萬列天花板且不佔用記憶體緩衝"
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "成功串流產製並下載分片 ZIP 壓縮檔"),
			@ApiResponse(responseCode = "400", description = "請求參數不合法"),
			@ApiResponse(responseCode = "500", description = "伺服器內部產生異常")
	})
	@PostMapping({"/partition/zip/stream", "/partition-zip/stream"})
	public void exportPartitionZipStream(
			@RequestBody @Valid ExportPartitionZipResource resource,
			HttpServletResponse response
	) throws IOException {
		ExportPartitionZipCommand command = resourceAssembler.toCommand(resource, null);
		String fileName = (command.zipFileName() != null && !command.zipFileName().isBlank())
				? command.zipFileName().trim()
				: "partition_archive_stream.zip";
		if (!fileName.endsWith(".zip")) {
			fileName += ".zip";
		}
		String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
		response.setContentType(ZIP_MIME_TYPE);
		response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName);
		parallelExcelUseCase.exportPartitionZipStream(response.getOutputStream(), command);
		response.flushBuffer();
	}

	/**
	 * 建構標準 HTTP 附件下載回應 (Content-Disposition: attachment)
	 */
	private ResponseEntity<byte[]> createAttachmentResponse(byte[] fileBytes, String fileName, String contentType) {
		String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.parseMediaType(contentType));
		headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName);
		headers.setContentLength(fileBytes.length);
		return ResponseEntity.ok()
				.headers(headers)
				.body(fileBytes);
	}
}

