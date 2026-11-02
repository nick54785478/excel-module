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
import com.example.demo.application.port.in.ExportMaterialBomUseCase;
import com.example.demo.application.port.in.ImportExcelUseCase;
import com.example.demo.application.shared.model.BomItemExcelModel;
import com.example.demo.application.shared.model.MaterialExcelModel;
import com.example.demo.presentation.assembler.MaterialBomResourceAssembler;
import com.example.demo.presentation.resource.in.ExportBomItemResource;
import com.example.demo.presentation.resource.in.ExportMaterialResource;
import com.example.demo.presentation.resource.out.BomItemImportedResource;
import com.example.demo.presentation.resource.out.MaterialImportedResource;
import com.example.demo.presentation.resource.out.ValidationFailedResource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * 物料與 BOM 宣告式 Excel 映射控制器 (Material & BOM Declarative Excel Mapping Controller / Inbound Adapter)
 * <p>
 * 遵循 Clean Architecture 六角形架構表現層規範，作為外部 HTTP 請求進入點。
 * 專門提供物料主檔 ({@link com.example.demo.domain.material.aggregate.root.Material})
 * 與物料清單配方 ({@link com.example.demo.domain.bom.aggregate.root.Bom}) 對應之 Excel 宣告式物件映射 (Declarative Bean Mapping) API。
 * 支援物料主檔與單階 BOM 配方清單之宣告式雙向匯出與解析匯入。
 */
@Tag(name = "物料與 BOM Excel 宣告式映射介面", description = "提供基於 Material 與 BOM 業務模型之宣告式 Excel 物件映射 (Declarative Bean Mapping) 匯出與匯入 API")
@Validated
@RestController
@RequestMapping({"/api/v1/excel/material-bom", "/api/v1/excel/aggregate"})
public class MaterialBomExcelController {

	private static final String EXCEL_MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

	private final ExportMaterialBomUseCase exportMaterialBomUseCase;
	private final ExportExcelUseCase exportExcelUseCase;
	private final ImportExcelUseCase importExcelUseCase;
	private final MaterialBomResourceAssembler resourceAssembler;

	public MaterialBomExcelController(
			ExportMaterialBomUseCase exportMaterialBomUseCase,
			ExportExcelUseCase exportExcelUseCase,
			ImportExcelUseCase importExcelUseCase,
			MaterialBomResourceAssembler resourceAssembler
	) {
		this.exportMaterialBomUseCase = Objects.requireNonNull(exportMaterialBomUseCase, "exportMaterialBomUseCase must not be null");
		this.exportExcelUseCase = Objects.requireNonNull(exportExcelUseCase, "exportExcelUseCase must not be null");
		this.importExcelUseCase = Objects.requireNonNull(importExcelUseCase, "importExcelUseCase must not be null");
		this.resourceAssembler = Objects.requireNonNull(resourceAssembler, "resourceAssembler must not be null");
	}

	// =========================================================================
	// 1. Material (物料主檔) 宣告式匯出與匯入
	// =========================================================================

	/**
	 * 透過 HTTP GET 快速下載系統資料庫現有物料主檔 Excel 檔案
	 *
	 * @return Excel 二進位檔案下載回應
	 */
	@Operation(
			summary = "匯出系統現有物料主檔 Excel (Material 業務模型)",
			description = "直接從物料儲存庫查詢現有物料聚合根，透過純領域方法提取屬性並宣告式產製 MaterialExcelModel 之 Excel 檔案。"
	)
	@GetMapping("/materials/export")
	public ResponseEntity<byte[]> exportLiveMaterials() {
		byte[] excelBytes = exportMaterialBomUseCase.exportMaterials();
		return createDownloadResponse(excelBytes, "materials_master.xlsx");
	}

	/**
	 * 透過 HTTP POST 依傳入 JSON 資料宣告式匯出物料主檔 Excel
	 *
	 * @param resource 物料匯出請求資源
	 * @return Excel 二進位檔案下載回應
	 */
	@Operation(
			summary = "自訂資料宣告式匯出物料主檔 Excel (Material 業務模型)",
			description = "接收客戶端傳入之自訂物料 JSON 清單，經由 ResourceAssembler 轉換為 MaterialExcelModel 後宣告式寫入 Excel。"
	)
	@PostMapping("/materials/export")
	public ResponseEntity<byte[]> exportMaterials(
			@Parameter(description = "物料主檔匯出設定與資料清單 (可選)")
			@Valid @RequestBody(required = false) ExportMaterialResource resource
	) {
		List<MaterialExcelModel> models = resourceAssembler.toMaterialExcelModels(resource);
		String sheetName = (resource != null && resource.sheetName() != null && !resource.sheetName().isBlank())
				? resource.sheetName().trim()
				: null;

		byte[] excelBytes = (sheetName != null)
				? exportExcelUseCase.exportBeans(models, MaterialExcelModel.class, sheetName)
				: exportExcelUseCase.exportBeans(models, MaterialExcelModel.class);

		return createDownloadResponse(excelBytes, "materials_export.xlsx");
	}

	/**
	 * 上傳 Excel 檔案並宣告式解析為物料模型清單
	 *
	 * @param file 上傳之 Excel 檔案
	 * @return 解析完成之物料回應結構
	 * @throws IOException 當讀取檔案輸入串流失敗時拋出
	 */
	@Operation(
			summary = "檔案上傳並宣告式解析為 MaterialExcelModel (Material 業務模型)",
			description = "上傳物料 Excel 檔案，後端自動根據 @ExcelSheet 與 @ExcelColumn 宣告式解析並映射為 MaterialExcelModel 清單，回傳 JSON。"
	)
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "成功解析物料 Excel 內容",
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = MaterialImportedResource.class)
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
	@PostMapping(value = "/materials/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<MaterialImportedResource> importMaterials(
			@Parameter(description = "欲上傳解析之物料 Excel 檔案 (.xlsx)", required = true)
			@RequestParam("file") MultipartFile file
	) throws IOException {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("上傳之 Excel 檔案不能為空");
		}

		List<MaterialExcelModel> models = importExcelUseCase.importBeans(file.getInputStream(), MaterialExcelModel.class);
		MaterialImportedResource response = resourceAssembler.toMaterialImportedResource(models, "物料主檔清單");
		return ResponseEntity.ok(response);
	}

	// =========================================================================
	// 2. BOM (物料清單配方) 宣告式匯出與匯入
	// =========================================================================

	/**
	 * 透過 HTTP GET 快速下載單階 BOM 配方清單範例 Excel 檔案
	 *
	 * @return Excel 二進位檔案下載回應
	 */
	@Operation(
			summary = "匯出系統現有單階 BOM 配方 Excel (BOM 業務模型)",
			description = "直接從 BOM 與物料儲存庫查詢現有 BOM 與關聯物料聚合根，透過純領域方法提取屬性並宣告式產製 BomItemExcelModel 之 Excel 檔案。"
	)
	@GetMapping("/bom-items/export")
	public ResponseEntity<byte[]> exportLiveBomItems() {
		byte[] excelBytes = exportMaterialBomUseCase.exportBomItems();
		return createDownloadResponse(excelBytes, "bom_items_master.xlsx");
	}

	/**
	 * 透過 HTTP POST 依傳入 JSON 資料宣告式匯出單階 BOM 配方 Excel
	 *
	 * @param resource BOM 配方匯出請求資源
	 * @return Excel 二進位檔案下載回應
	 */
	@Operation(
			summary = "自訂資料宣告式匯出單階 BOM 配方 Excel (BOM 業務模型)",
			description = "接收客戶端傳入之自訂 BOM JSON 清單，經由 ResourceAssembler 轉換為 BomItemExcelModel 後宣告式寫入 Excel。"
	)
	@PostMapping("/bom-items/export")
	public ResponseEntity<byte[]> exportBomItems(
			@Parameter(description = "BOM 配方匯出設定與資料清單 (可選)")
			@Valid @RequestBody(required = false) ExportBomItemResource resource
	) {
		List<BomItemExcelModel> models = resourceAssembler.toBomItemExcelModels(resource);
		String sheetName = (resource != null && resource.sheetName() != null && !resource.sheetName().isBlank())
				? resource.sheetName().trim()
				: null;

		byte[] excelBytes = (sheetName != null)
				? exportExcelUseCase.exportBeans(models, BomItemExcelModel.class, sheetName)
				: exportExcelUseCase.exportBeans(models, BomItemExcelModel.class);

		return createDownloadResponse(excelBytes, "bom_items_export.xlsx");
	}

	/**
	 * 上傳 Excel 檔案並宣告式解析為單階 BOM 配方模型清單
	 *
	 * @param file 上傳之 Excel 檔案
	 * @return 解析完成之 BOM 回應結構
	 * @throws IOException 當讀取檔案輸入串流失敗時拋出
	 */
	@Operation(
			summary = "檔案上傳並宣告式解析為 BomItemExcelModel (BOM 業務模型)",
			description = "上傳 BOM Excel 檔案，後端自動根據 @ExcelSheet 與 @ExcelColumn 宣告式解析並映射為 BomItemExcelModel 清單，回傳 JSON。"
	)
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "成功解析 BOM Excel 內容",
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = BomItemImportedResource.class)
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
	@PostMapping(value = "/bom-items/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<BomItemImportedResource> importBomItems(
			@Parameter(description = "欲上傳解析之 BOM 配方 Excel 檔案 (.xlsx)", required = true)
			@RequestParam("file") MultipartFile file
	) throws IOException {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("上傳之 Excel 檔案不能為空");
		}

		List<BomItemExcelModel> models = importExcelUseCase.importBeans(file.getInputStream(), BomItemExcelModel.class);
		BomItemImportedResource response = resourceAssembler.toBomItemImportedResource(models, "單階BOM清單");
		return ResponseEntity.ok(response);
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

