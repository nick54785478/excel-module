package com.example.demo.presentation.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.application.port.in.ExportExcelUseCase;
import com.example.demo.application.port.in.ImportExcelUseCase;
import com.example.demo.application.port.in.StreamExportExcelUseCase;
import com.example.demo.application.shared.model.OrderModel;
import com.example.demo.application.shared.model.ProductRecordModel;
import com.example.demo.presentation.assembler.ExcelBeanMappingResourceAssembler;
import com.example.demo.presentation.resource.in.ExportOrderResource;
import com.example.demo.presentation.resource.in.ExportProductResource;
import com.example.demo.presentation.resource.out.OrderImportedResource;
import com.example.demo.presentation.resource.out.ProductImportedResource;
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
 * 宣告式物件映射測試 REST 控制器 (Declarative Bean Mapping Presentation Controller)
 * <p>
 * 遵循 Clean Architecture 六角形架構表現層規範，作為外部 HTTP 請求進入點。
 * 專門用於驗證與測試維度一「宣告式物件映射 (Declarative Bean Mapping)」功能，
 * 支援以宣告式註解 {@code @ExcelSheet} 與 {@code @ExcelColumn} 標註之標準 JavaBean (如 {@link OrderModel})
 * 與 Java 21 原生 Record (如 {@link ProductRecordModel}) 之雙向 Excel 解析與匯出。
 */
@Tag(name = "Excel 宣告式物件映射測試介面", description = "提供測試維度一「宣告式物件映射 (Declarative Bean Mapping)」的匯出與匯入 REST API，支援 JavaBean 與 Java 21 Record 雙向流轉")
@Validated
@RestController
@RequestMapping("/api/v1/excel/bean-mapping")
public class ExcelBeanMappingController {

	private static final String EXCEL_MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

	private final ExportExcelUseCase exportExcelUseCase;
	private final StreamExportExcelUseCase streamExportExcelUseCase;
	private final ImportExcelUseCase importExcelUseCase;
	private final ExcelBeanMappingResourceAssembler resourceAssembler;

	public ExcelBeanMappingController(
			ExportExcelUseCase exportExcelUseCase,
			StreamExportExcelUseCase streamExportExcelUseCase,
			ImportExcelUseCase importExcelUseCase,
			ExcelBeanMappingResourceAssembler resourceAssembler
	) {
		this.exportExcelUseCase = Objects.requireNonNull(exportExcelUseCase, "exportExcelUseCase must not be null");
		this.streamExportExcelUseCase = Objects.requireNonNull(streamExportExcelUseCase, "streamExportExcelUseCase must not be null");
		this.importExcelUseCase = Objects.requireNonNull(importExcelUseCase, "importExcelUseCase must not be null");
		this.resourceAssembler = Objects.requireNonNull(resourceAssembler, "resourceAssembler must not be null");
	}

	// =========================================================================
	// 1. JavaBean 宣告式匯出與匯入 (OrderModel 測試端點)
	// =========================================================================

	/**
	 * 透過 HTTP GET 快速下載訂單範例 Excel 檔案 (JavaBean 模式)
	 *
	 * @return Excel 二進位檔案下載回應
	 */
	@Operation(
			summary = "快速下載訂單範例 Excel (JavaBean 模式)",
			description = "使用系統內建展示資料，經由 ExportExcelUseCase.exportBeans 將 OrderModel 清單宣告式產製為 Excel 活頁簿二進位檔案下載。"
	)
	@GetMapping("/orders/export")
	public ResponseEntity<byte[]> exportSampleOrders() {
		List<OrderModel> sampleOrders = resourceAssembler.createSampleOrders();
		byte[] excelBytes = exportExcelUseCase.exportBeans(sampleOrders, OrderModel.class);
		return createDownloadResponse(excelBytes, "orders_sample.xlsx");
	}

	/**
	 * 透過 HTTP POST 依傳入 JSON 資料宣告式匯出訂單 Excel 檔案
	 *
	 * @param resource 訂單匯出請求資源 (選填，若為空則套用預設展示資料)
	 * @return Excel 二進位檔案下載回應
	 */
	@Operation(
			summary = "自訂資料宣告式匯出訂單 Excel (JavaBean 模式)",
			description = "接收客戶端傳入之自訂訂單 JSON 清單，經由 ResourceAssembler 轉換為 OrderModel 後宣告式寫入 Excel 下載。"
	)
	@PostMapping("/orders/export")
	public ResponseEntity<byte[]> exportOrders(
			@Parameter(description = "訂單匯出設定與資料清單 (可選)")
			@Valid @RequestBody(required = false) ExportOrderResource resource
	) {
		List<OrderModel> models = resourceAssembler.toOrderModels(resource);
		String sheetName = (resource != null && resource.sheetName() != null && !resource.sheetName().isBlank())
				? resource.sheetName().trim()
				: null;

		byte[] excelBytes = (sheetName != null)
				? exportExcelUseCase.exportBeans(models, OrderModel.class, sheetName)
				: exportExcelUseCase.exportBeans(models, OrderModel.class);

		return createDownloadResponse(excelBytes, "orders_export.xlsx");
	}

	/**
	 * 透過 HTTP POST 串流匯出訂單 Excel (大數據串流模式)
	 *
	 * @param resource 訂單匯出請求資源 (可選)
	 * @param response Servlet HTTP 回應物件
	 * @throws IOException 當串流寫入發生 I/O 異常時拋出
	 */
	@Operation(
			summary = "串流匯出訂單 Excel (JavaBean 大數據模式)",
			description = "調用 StreamExportExcelUseCase.streamExportBeans 將 OrderModel 清單直接串流沖刷至 HTTP 輸出流中。"
	)
	@PostMapping("/orders/stream-export")
	public void streamExportOrders(
			@Parameter(description = "訂單匯出設定與資料清單 (可選)")
			@Valid @RequestBody(required = false) ExportOrderResource resource,
			HttpServletResponse response
	) throws IOException {
		List<OrderModel> models = resourceAssembler.toOrderModels(resource);
		String sheetName = (resource != null && resource.sheetName() != null && !resource.sheetName().isBlank())
				? resource.sheetName().trim()
				: null;

		String encodedFileName = URLEncoder.encode("orders_stream_export.xlsx", StandardCharsets.UTF_8).replace("+", "%20");
		response.setContentType(EXCEL_MIME_TYPE);
		response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName);

		if (sheetName != null) {
			streamExportExcelUseCase.streamExportBeans(response.getOutputStream(), models, OrderModel.class, sheetName);
		} else {
			streamExportExcelUseCase.streamExportBeans(response.getOutputStream(), models, OrderModel.class);
		}
		response.flushBuffer();
	}

	/**
	 * 上傳 Excel 檔案並宣告式解析為訂單模型結構 (JavaBean 模式)
	 *
	 * @param file 上傳之 Excel 檔案
	 * @return 解析完成之訂單回應結構
	 * @throws IOException 當讀取檔案輸入串流失敗時拋出
	 */
	@Operation(
			summary = "檔案上傳並宣告式解析為 OrderModel (JavaBean 模式)",
			description = "上傳 Excel 檔案，後端自動根據 @ExcelSheet 與 @ExcelColumn 宣告式解析並映射為 OrderModel 清單，回傳 JSON。"
	)
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "成功解析 Excel 內容",
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = OrderImportedResource.class)
					)
			),
			@ApiResponse(
					responseCode = "400",
					description = "檔案為空或解析失敗",
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = ValidationFailedResource.class)
					)
			)
	})
	@PostMapping(value = "/orders/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<OrderImportedResource> importOrders(
			@Parameter(description = "欲上傳解析之訂單 Excel 檔案 (.xlsx)", required = true)
			@RequestParam("file") MultipartFile file
	) throws IOException {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("上傳之 Excel 檔案不能為空");
		}

		List<OrderModel> models = importExcelUseCase.importBeans(file.getInputStream(), OrderModel.class);
		OrderImportedResource resource = resourceAssembler.toOrderImportedResource(models, "訂單匯入清單");
		return ResponseEntity.ok(resource);
	}

	// =========================================================================
	// 2. Java 21 Record 宣告式匯出與匯入 (ProductRecordModel 測試端點)
	// =========================================================================

	/**
	 * 透過 HTTP GET 快速下載商品主檔範例 Excel 檔案 (Java 21 Record 模式)
	 *
	 * @return Excel 二進位檔案下載回應
	 */
	@Operation(
			summary = "快速下載商品主檔範例 Excel (Java 21 Record 模式)",
			description = "使用系統展示資料，經由 ExportExcelUseCase.exportBeans 將不可變 ProductRecordModel 清單宣告式產製為 Excel 下載。"
	)
	@GetMapping("/products/export")
	public ResponseEntity<byte[]> exportSampleProducts() {
		List<ProductRecordModel> sampleProducts = resourceAssembler.createSampleProducts();
		byte[] excelBytes = exportExcelUseCase.exportBeans(sampleProducts, ProductRecordModel.class);
		return createDownloadResponse(excelBytes, "products_sample.xlsx");
	}

	/**
	 * 透過 HTTP POST 依傳入 JSON 資料宣告式匯出商品主檔 Excel (Java 21 Record 模式)
	 *
	 * @param resource 商品匯出請求資源 (選填)
	 * @return Excel 二進位檔案下載回應
	 */
	@Operation(
			summary = "自訂資料宣告式匯出商品 Excel (Java 21 Record 模式)",
			description = "接收客戶端傳入之自訂商品 JSON 清單，經由 ResourceAssembler 轉換為 ProductRecordModel 後宣告式寫入 Excel 下載。"
	)
	@PostMapping("/products/export")
	public ResponseEntity<byte[]> exportProducts(
			@Parameter(description = "商品匯出設定與資料清單 (可選)")
			@Valid @RequestBody(required = false) ExportProductResource resource
	) {
		List<ProductRecordModel> models = resourceAssembler.toProductModels(resource);
		String sheetName = (resource != null && resource.sheetName() != null && !resource.sheetName().isBlank())
				? resource.sheetName().trim()
				: null;

		byte[] excelBytes = (sheetName != null)
				? exportExcelUseCase.exportBeans(models, ProductRecordModel.class, sheetName)
				: exportExcelUseCase.exportBeans(models, ProductRecordModel.class);

		return createDownloadResponse(excelBytes, "products_export.xlsx");
	}

	/**
	 * 上傳 Excel 檔案並宣告式解析為商品主檔結構 (Java 21 Record 模式)
	 *
	 * @param file 上傳之 Excel 檔案
	 * @return 解析完成之商品回應結構
	 * @throws IOException 當讀取檔案輸入串流失敗時拋出
	 */
	@Operation(
			summary = "檔案上傳並宣告式解析為 ProductRecordModel (Java 21 Record 模式)",
			description = "上傳 Excel 檔案，後端自動根據 @ExcelSheet 與 @ExcelColumn 宣告式解析並映射為不可變 ProductRecordModel 清單，回傳 JSON。"
	)
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "成功解析商品 Excel 內容",
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = ProductImportedResource.class)
					)
			),
			@ApiResponse(
					responseCode = "400",
					description = "檔案為空或解析失敗",
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = ValidationFailedResource.class)
					)
			)
	})
	@PostMapping(value = "/products/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ProductImportedResource> importProducts(
			@Parameter(description = "欲上傳解析之商品 Excel 檔案 (.xlsx)", required = true)
			@RequestParam("file") MultipartFile file
	) throws IOException {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("上傳之 Excel 檔案不能為空");
		}

		List<ProductRecordModel> models = importExcelUseCase.importBeans(file.getInputStream(), ProductRecordModel.class);
		ProductImportedResource resource = resourceAssembler.toProductImportedResource(models, "商品主檔清單");
		return ResponseEntity.ok(resource);
	}

	// =========================================================================
	// 私有輔助方法
	// =========================================================================

	private ResponseEntity<byte[]> createDownloadResponse(byte[] content, String defaultFileName) {
		String encodedFileName = URLEncoder.encode(defaultFileName, StandardCharsets.UTF_8).replace("+", "%20");
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.parseMediaType(EXCEL_MIME_TYPE));
		headers.setContentDispositionFormData("attachment", encodedFileName);
		headers.setContentLength(content.length);
		return ResponseEntity.ok().headers(headers).body(content);
	}
}

