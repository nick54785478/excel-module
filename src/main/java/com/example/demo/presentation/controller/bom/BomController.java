package com.example.demo.presentation.controller.bom;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
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

import com.example.demo.application.assembler.BomDtoAssembler;
import com.example.demo.application.port.in.bom.ExportBomUseCase;
import com.example.demo.application.port.in.bom.QueryBomUseCase;
import com.example.demo.application.port.in.bom.StreamExportBomUseCase;
import com.example.demo.application.shared.command.ExportBomCommand;
import com.example.demo.application.shared.dto.BomExcelGottenResult;
import com.example.demo.presentation.assembler.BomResourceAssembler;
import com.example.demo.presentation.resource.in.ExportBomResource;
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

/**
 * 物料清單 (BOM) REST 控制器 (Presentation Inbound Adapter)
 * <p>
 * 遵循 Clean Architecture 表現層職責與 CQRS 規範，作為外部 HTTP 請求進入點。
 * 針對 GET 查詢導向操作調度 {@link QueryBomUseCase}，針對 POST 命令導向操作調度 {@link ExportBomUseCase} 與 {@link StreamExportBomUseCase}。
 * 支援一般記憶體快速下載 (GET/POST) 以及百萬級大數據串流輸出 (GET/POST /stream)。
 */
@Tag(name = "BOM 物料清單介面", description = "提供物料清單 (BOM) Excel 檔案動態匯出與下載 REST API")
@Validated
@RestController
@RequestMapping("/api/v1/bom")
public class BomController {

	private static final String EXCEL_MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

	private final QueryBomUseCase queryBomUseCase;
	private final ExportBomUseCase exportBomUseCase;
	private final StreamExportBomUseCase streamExportBomUseCase;
	private final BomResourceAssembler bomResourceAssembler;

	@Autowired
	public BomController(
			QueryBomUseCase queryBomUseCase,
			ExportBomUseCase exportBomUseCase,
			StreamExportBomUseCase streamExportBomUseCase,
			BomResourceAssembler bomResourceAssembler
	) {
		this.queryBomUseCase = Objects.requireNonNull(queryBomUseCase, "queryBomUseCase must not be null");
		this.exportBomUseCase = Objects.requireNonNull(exportBomUseCase, "exportBomUseCase must not be null");
		this.streamExportBomUseCase = streamExportBomUseCase;
		this.bomResourceAssembler = Objects.requireNonNull(bomResourceAssembler, "bomResourceAssembler must not be null");
	}

	public BomController(
			ExportBomUseCase exportBomUseCase,
			StreamExportBomUseCase streamExportBomUseCase,
			BomResourceAssembler bomResourceAssembler
	) {
		this(
				new QueryBomUseCase() {
					@Override
					public BomExcelGottenResult queryBom(ExportBomCommand command) {
						return exportBomUseCase.exportBom(command);
					}

					@Override
					public void streamQueryBom(OutputStream outputStream, ExportBomCommand command) {
						if (streamExportBomUseCase != null) {
							streamExportBomUseCase.streamExportBom(outputStream, command);
						}
					}
				},
				exportBomUseCase,
				streamExportBomUseCase,
				bomResourceAssembler
		);
	}

	public BomController(ExportBomUseCase exportBomUseCase, BomResourceAssembler bomResourceAssembler) {
		this(
				exportBomUseCase,
				exportBomUseCase instanceof StreamExportBomUseCase s ? s : null,
				bomResourceAssembler
		);
	}

	/**
	 * 透過 HTTP GET 下載物料清單 (BOM) Excel 檔案 (查詢端操作)
	 *
	 * @param parentMaterialCode    母件物料料號 (選填，如 MAT-FG-001)
	 * @param version               BOM 版本號 (選填，如 1.0)
	 * @param includeMultiLevel     是否包含多階層 BOM 展開工作表 (預設 true)
	 * @param includeSingleLevel    是否包含單階 BOM 匯入格式工作表 (預設 true)
	 * @param includeMaterialMaster 是否包含物料主檔對照表 (預設 true)
	 * @return Excel 二進位檔案下載回應
	 */
	@Operation(
			summary = "下載物料清單 (BOM) Excel 檔案 (GET)",
			description = "依據母件料號、版本號與工作表選項，動態產製包含多階展開清單、單階匯入清單與物料主檔對照表之 Excel 檔案並提供下載。若未指定料號則匯出全部 BOM。"
	)
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "成功生成 BOM Excel 二進位檔案並提供下載",
					content = @Content(
							mediaType = EXCEL_MIME_TYPE,
							schema = @Schema(type = "string", format = "binary")
					)
			),
			@ApiResponse(
					responseCode = "400",
					description = "請求參數錯誤",
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = ValidationFailedResource.class)
					)
			),
			@ApiResponse(
					responseCode = "404",
					description = "查無指定母件料號之物料主檔",
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = ValidationFailedResource.class)
					)
			)
	})
	@GetMapping("/excel")
	public ResponseEntity<byte[]> downloadBomExcel(
			@Parameter(description = "母件物料料號 (選填，若未填寫則匯出系統中所有 BOM 清單)", example = "MAT-FG-001")
			@RequestParam(required = false) String parentMaterialCode,

			@Parameter(description = "BOM 版本號 (選填，如 1.0)", example = "1.0")
			@RequestParam(required = false) String version,

			@Parameter(description = "是否包含多階層 BOM 展開工作表", example = "true")
			@RequestParam(required = false, defaultValue = "true") Boolean includeMultiLevel,

			@Parameter(description = "是否包含單階 BOM 匯入格式工作表", example = "true")
			@RequestParam(required = false, defaultValue = "true") Boolean includeSingleLevel,

			@Parameter(description = "是否包含物料主檔對照表", example = "true")
			@RequestParam(required = false, defaultValue = "true") Boolean includeMaterialMaster
	) {
		ExportBomCommand command = bomResourceAssembler.toExportCommand(
				parentMaterialCode,
				version,
				includeMultiLevel,
				includeSingleLevel,
				includeMaterialMaster
		);

		BomExcelGottenResult result = queryBomUseCase.queryBom(command);
		return buildExcelDownloadResponse(result);
	}

	/**
	 * 透過 HTTP POST 結構化請求匯出物料清單 (BOM) Excel 檔案 (命令端操作)
	 *
	 * @param resource 匯出條件請求結構體
	 * @return Excel 二進位檔案下載回應
	 */
	@Operation(
			summary = "以結構化條件匯出物料清單 (BOM) Excel 檔案 (POST)",
			description = "接收 JSON 結構化條件，動態產製並下載物料清單 (BOM) Excel 檔案。"
	)
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "成功生成 BOM Excel 二進位檔案並提供下載",
					content = @Content(
							mediaType = EXCEL_MIME_TYPE,
							schema = @Schema(type = "string", format = "binary")
					)
			),
			@ApiResponse(
					responseCode = "400",
					description = "請求資料校驗失敗",
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = ValidationFailedResource.class)
					)
			),
			@ApiResponse(
					responseCode = "404",
					description = "查無指定母件料號之物料主檔",
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = ValidationFailedResource.class)
					)
			)
	})
	@PostMapping("/excel/export")
	public ResponseEntity<byte[]> exportBomExcel(
			@Parameter(description = "BOM 匯出請求載體", required = true)
			@Valid @RequestBody ExportBomResource resource
	) {
		ExportBomCommand command = bomResourceAssembler.toExportCommand(resource);
		BomExcelGottenResult result = exportBomUseCase.exportBom(command);
		return buildExcelDownloadResponse(result);
	}

	/**
	 * 透過 HTTP GET 大數據串流下載物料清單 (BOM) Excel 檔案 (查詢端串流操作)
	 *
	 * @param parentMaterialCode    母件物料料號 (選填)
	 * @param version               BOM 版本號 (選填)
	 * @param includeMultiLevel     是否包含多階層展開工作表
	 * @param includeSingleLevel    是否包含單階平鋪工作表
	 * @param includeMaterialMaster 是否包含物料主檔對照表
	 * @param flushInterval         串流沖刷筆數間隔 (預設 1000)
	 * @param response              HTTP 回應物件
	 * @throws IOException 當串流傳輸失敗時拋出
	 */
	@Operation(
			summary = "大數據串流下載物料清單 (BOM) Excel 檔案 (GET)",
			description = "邊產製邊將 Excel 二進位串流 Flush 至 HTTP 回應流中，維持常數級極低記憶體消耗，適合海量 BOM 下載。"
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
					responseCode = "404",
					description = "查無指定母件料號之物料主檔",
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = ValidationFailedResource.class)
					)
			)
	})
	@GetMapping("/excel/stream")
	public void streamDownloadBomExcel(
			@Parameter(description = "母件物料料號 (選填，若未填寫則匯出系統中所有 BOM 清單)", example = "MAT-FG-001")
			@RequestParam(required = false) String parentMaterialCode,

			@Parameter(description = "BOM 版本號 (選填，如 1.0)", example = "1.0")
			@RequestParam(required = false) String version,

			@Parameter(description = "是否包含多階層 BOM 展開工作表", example = "true")
			@RequestParam(required = false, defaultValue = "true") Boolean includeMultiLevel,

			@Parameter(description = "是否包含單階 BOM 匯入格式工作表", example = "true")
			@RequestParam(required = false, defaultValue = "true") Boolean includeSingleLevel,

			@Parameter(description = "是否包含物料主檔對照表", example = "true")
			@RequestParam(required = false, defaultValue = "true") Boolean includeMaterialMaster,

			@Parameter(description = "串流分批沖刷筆數間隔 (預設 1000)", example = "1000")
			@RequestParam(required = false, defaultValue = "1000") Integer flushInterval,

			HttpServletResponse response
	) throws IOException {
		ExportBomCommand command = bomResourceAssembler.toExportCommand(
				parentMaterialCode,
				version,
				includeMultiLevel,
				includeSingleLevel,
				includeMaterialMaster,
				flushInterval
		);

		String fileName = BomDtoAssembler.resolveFileName(command);
		setStreamResponseHeaders(response, fileName);

		queryBomUseCase.streamQueryBom(response.getOutputStream(), command);
		response.flushBuffer();
	}

	/**
	 * 透過 HTTP POST 結構化條件大數據串流匯出物料清單 (BOM) Excel 檔案 (命令端串流操作)
	 *
	 * @param resource 匯出條件請求結構體 (含可選之 flushInterval)
	 * @param response HTTP Servlet 回應物件
	 * @throws IOException 當串流傳輸失敗時拋出
	 */
	@Operation(
			summary = "以結構化條件大數據串流匯出物料清單 (BOM) Excel 檔案 (POST)",
			description = "接收 JSON 結構化條件，邊產製邊以分批 Flush 機制將 Excel 串流傳輸至前端。"
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
					description = "請求資料校驗失敗",
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = ValidationFailedResource.class)
					)
			),
			@ApiResponse(
					responseCode = "404",
					description = "查無指定母件料號之物料主檔",
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = ValidationFailedResource.class)
					)
			)
	})
	@PostMapping("/excel/stream")
	public void streamExportBomExcel(
			@Parameter(description = "BOM 匯出請求載體", required = true)
			@Valid @RequestBody ExportBomResource resource,
			HttpServletResponse response
	) throws IOException {
		ExportBomCommand command = bomResourceAssembler.toExportCommand(resource);
		String fileName = BomDtoAssembler.resolveFileName(command);
		setStreamResponseHeaders(response, fileName);

		Objects.requireNonNull(streamExportBomUseCase, "streamExportBomUseCase must not be null");
		streamExportBomUseCase.streamExportBom(response.getOutputStream(), command);
		response.flushBuffer();
	}

	// =========================================================================
	// 私有輔助方法 (Private Helpers)
	// =========================================================================

	/**
	 * 設定巨量資料串流輸出至 HTTP Response 的標準標頭 (Stream Response Headers Setter)
	 * <p>
	 * <b>【架構定位與安全性規範】</b><br>
	 * 專門用於直接將二進位試算表串流寫入 {@link HttpServletResponse#getOutputStream()} 的場景。<br>
	 * 依據 RFC 5987 規範執行檔名 UTF-8 安全編碼，將空白符號轉義為標準 {@code %20}，避免在不同客戶端瀏覽器產生檔名亂碼；
	 * 同時設置標準 OpenXML 試算表 MIME 格式 ({@value #EXCEL_MIME_TYPE})。
	 * </p>
	 *
	 * @param response    當前 HTTP 請求回應物件，用於輸出資料串流與寫入標頭
	 * @param rawFileName 未編碼之原始下載檔案名稱 (若無附檔名將自動補齊 {@code .xlsx})
	 */
	private void setStreamResponseHeaders(HttpServletResponse response, String rawFileName) {
		// ---------------------------------------------------------------------
		// 步驟 1：檔名 RFC 5987 UTF-8 安全編碼 (避免中文與空格在各瀏覽器亂碼)
		// ---------------------------------------------------------------------
		String safeFileName = (rawFileName != null && !rawFileName.isBlank()) ? rawFileName.trim() : "bom_stream_export";
		String encodedFileName = URLEncoder.encode(safeFileName, StandardCharsets.UTF_8).replace("+", "%20");

		// ---------------------------------------------------------------------
		// 步驟 2：副檔名防禦補齊
		// ---------------------------------------------------------------------
		if (!encodedFileName.endsWith(".xlsx")) {
			encodedFileName += ".xlsx";
		}

		// ---------------------------------------------------------------------
		// 步驟 3：配置標準 Content-Type 與 Content-Disposition 下載標頭
		// ---------------------------------------------------------------------
		response.setContentType(EXCEL_MIME_TYPE);
		response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName);
	}

	/**
	 * 建構全量 BOM 試算表二進位下載之 HTTP 回應實體 (Excel Binary Download Builder)
	 * <p>
	 * <b>【架構定位與職責說明】</b><br>
	 * 接收應用層產製完成之 {@link BomExcelGottenResult} DTO，提取其內部二進位陣列與檔案名稱，
	 * 包裝為符合 RFC 5987 規範的標準 {@link ResponseEntity} 下載回應。
	 * </p>
	 *
	 * @param result 應用層 BOM 匯出業務產出之結果純資料載體 (包含二進位檔案 byte 陣列與推薦檔名)
	 * @return HTTP 200 OK 且附帶二進位試算表內容之標準下載回應
	 */
	private ResponseEntity<byte[]> buildExcelDownloadResponse(BomExcelGottenResult result) {
		// ---------------------------------------------------------------------
		// 步驟 1：邊界安全提取與預設檔名防禦
		// ---------------------------------------------------------------------
		String rawName = (result.fileName() != null && !result.fileName().isBlank())
				? result.fileName().trim()
				: "bom_export.xlsx";

		// ---------------------------------------------------------------------
		// 步驟 2：檔名 RFC 5987 UTF-8 編碼 (相容 Chrome, Edge, Firefox, Safari)
		// ---------------------------------------------------------------------
		String encodedFileName = URLEncoder.encode(rawName, StandardCharsets.UTF_8).replace("+", "%20");
		if (!encodedFileName.endsWith(".xlsx")) {
			encodedFileName += ".xlsx";
		}

		// ---------------------------------------------------------------------
		// 步驟 3：建構二進位檔案回應實體並寫入 Content-Length
		// ---------------------------------------------------------------------
		byte[] fileBytes = result.fileBytes() != null ? result.fileBytes() : new byte[0];
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
				.contentType(MediaType.parseMediaType(EXCEL_MIME_TYPE))
				.contentLength(fileBytes.length)
				.body(fileBytes);
	}
}
