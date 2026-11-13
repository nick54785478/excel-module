package com.example.demo.presentation.controller.bom;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.application.port.in.bom.ExportMaterialBomUseCase;
import com.example.demo.application.port.in.core.ImportExcelUseCase;
import com.example.demo.application.port.out.core.ExcelSheetModelRegistryPort;
import com.example.demo.application.shared.dto.BeanSheetGottenResult;
import com.example.demo.application.shared.model.bom.BomItemExcelModel;
import com.example.demo.application.shared.model.material.MaterialExcelModel;
import com.example.demo.application.shared.model.bom.MultiLevelBomExcelModel;
import com.example.demo.application.shared.query.ReadExcelQuery;
import com.example.demo.presentation.assembler.MaterialBomResourceAssembler;
import com.example.demo.presentation.resource.out.BomItemImportedResource;
import com.example.demo.presentation.resource.out.MaterialImportedResource;
import com.example.demo.presentation.resource.out.MultiLevelBomImportedResource;
import com.example.demo.presentation.resource.out.ValidationFailedResource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 物料與 BOM 宣告式 Excel 映射控制器 (Material & BOM Declarative Excel Mapping Controller / Inbound Adapter)
 * <p>
 * <b>【架構定位與職責規範】</b><br>
 * 位於 Clean Architecture 表現層 ({@code presentation/controller/})，遵循 {@code AGENTS.md} 規範。<br>
 * 作為外部 HTTP 請求進入點，專門提供物料主檔 ({@link com.example.demo.domain.material.aggregate.root.Material})
 * 與物料清單配方 ({@link com.example.demo.domain.bom.aggregate.root.Bom}) 對應之 Excel 宣告式物件映射 (Declarative Bean Mapping) API。
 * </p>
 * <p>
 * <b>【架構定位與 6 隻核心 API】</b><br>
 * 提供對稱完整的「系統實機資料一鍵匯出 (GET) ＋ 雙模態試算表匯入解析 (POST)」端點：
 * <ol>
 *   <li>{@code GET  /materials/export}：一鍵下載資料庫現有物料主檔 Excel (materials_master.xlsx)</li>
 *   <li>{@code POST /materials/import}：上傳物料 Excel 檔案並純依 {@code @ExcelSheet} 註解自動智慧探測解析 (無須指定 sheetName)</li>
 *   <li>{@code POST /materials/import/by-sheet}：上傳物料 Excel 檔案並依指定 {@code sheetName} 精確解析 (找不到時拋出例外)</li>
 *   <li>{@code GET  /bom-items/export}：一鍵下載資料庫現有單階 BOM 配方 Excel (bom_items_master.xlsx)</li>
 *   <li>{@code POST /bom-items/import}：上傳單階 BOM Excel 檔案並純依 {@code @ExcelSheet} 註解自動智慧探測解析 (無須指定 sheetName)</li>
 *   <li>{@code POST /bom-items/import/by-sheet}：上傳單階 BOM Excel 檔案並依指定 {@code sheetName} 精確解析 (找不到時拋出例外)</li>
 * </ol>
 * </p>
 */
@Tag(name = "物料與 BOM Excel 宣告式映射介面", description = "提供基於 Material 與 BOM 業務聚合模型之宣告式 Excel 物件映射匯出與純資料快速解析匯入 REST API")
@Validated
@RestController
@RequestMapping("/api/v1/excel/material-bom")
public class MaterialBomExcelController {

	private static final String EXCEL_MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

	private final ExportMaterialBomUseCase exportMaterialBomUseCase;
	private final ImportExcelUseCase importExcelUseCase;
	private final MaterialBomResourceAssembler resourceAssembler;
	private final ExcelSheetModelRegistryPort sheetModelRegistryPort;

	/**
	 * Spring 容器自動裝配建構子：注入領域專用匯出案例、通用匯入案例、表現層純資料組裝器與模型註冊表輸出埠
	 *
	 * @param exportMaterialBomUseCase 物料與 BOM 聚合匯出業務案例 (Application Inbound Port)
	 * @param importExcelUseCase       試算表通用物件映射匯入業務案例 (Application Inbound Port)
	 * @param resourceAssembler        表現層純資料組裝器 (Presentation Assembler)
	 * @param sheetModelRegistryPort   宣告式 Excel 模型註冊表查詢輸出埠 (Application Outbound Port)
	 */
	@Autowired
	public MaterialBomExcelController(
			ExportMaterialBomUseCase exportMaterialBomUseCase,
			ImportExcelUseCase importExcelUseCase,
			MaterialBomResourceAssembler resourceAssembler,
			ExcelSheetModelRegistryPort sheetModelRegistryPort
	) {
		this.exportMaterialBomUseCase = Objects.requireNonNull(exportMaterialBomUseCase, "exportMaterialBomUseCase must not be null");
		this.importExcelUseCase = Objects.requireNonNull(importExcelUseCase, "importExcelUseCase must not be null");
		this.resourceAssembler = Objects.requireNonNull(resourceAssembler, "resourceAssembler must not be null");
		this.sheetModelRegistryPort = Objects.requireNonNull(sheetModelRegistryPort, "sheetModelRegistryPort must not be null");
	}

	// =========================================================================
	// 1. Material (物料主檔) 宣告式匯出與匯入
	// =========================================================================

	/**
	 * API 1: 匯出系統現有物料主檔 Excel (Material 業務模型)
	 * <p>
	 * 直接從物料持久化儲存庫查詢系統現存的 Material 聚合根清單，
	 * 透過純領域方法提取屬性，並由 {@code ExcelBeanAssembler} 宣告式產製 {@link MaterialExcelModel} 之標準 Excel 檔案。
	 * </p>
	 *
	 * @return HTTP 200 與 materials_master.xlsx 二進位檔案下載串流
	 */
	@Operation(
			summary = "匯出系統現有物料主檔 Excel (Material 業務模型)",
			description = "直接從物料儲存庫查詢現有物料聚合根，透過純領域方法提取屬性並宣告式產製 MaterialExcelModel 之 Excel 檔案。"
	)
	@GetMapping("/materials/export")
	public ResponseEntity<byte[]> exportLiveMaterials() {
		// ---------------------------------------------------------------------
		// 步驟 1：調度應用層匯出業務案例 (Application Inbound Port)
		// ---------------------------------------------------------------------
		// ExportMaterialBomUseCase 協調 MaterialRepositoryPort 取得現有聚合根，
		// 並委託 ExcelWriterPort 將模型集合寫入為標準 OpenXML 試算表二進位位元組。
		byte[] excelBytes = exportMaterialBomUseCase.exportMaterials();

		// ---------------------------------------------------------------------
		// 步驟 2：封裝標準二進位檔案下載 HTTP 回應 (RFC 5987 安全編碼)
		// ---------------------------------------------------------------------
		return createDownloadResponse(excelBytes, "materials_master.xlsx");
	}

	/**
	 * API 2-A: 上傳 Excel 檔案並宣告式解析為物料模型清單 (支援單表與多表全量解析)
	 * <p>
	 * 上傳物料 Excel 檔案。若指定 {@code sheetName} 則精確讀取該工作表 (找不到時拋出例外)；
	 * 若未指定 {@code sheetName}，則自動走訪活頁簿中所有符合 {@link MaterialExcelModel} 之工作表並全部讀出合併回傳。
	 * </p>
	 *
	 * @param file      欲上傳解析之物料 Excel 檔案 (.xlsx)
	 * @param sheetName 指定讀取之工作表名稱 (選填，未填則全數讀出)
	 * @return HTTP 200 與解析完成之 {@link MaterialImportedResource} 結構化資料
	 * @throws IOException 當讀取檔案輸入串流失敗時拋出
	 */
	@Operation(
			summary = "檔案上傳並宣告式解析為 MaterialExcelModel 清單 (可選填 sheetName)",
			description = "上傳物料 Excel 檔案。若指定 sheetName 則精確讀取該工作表 (找不到時拋出 400 錯誤)；若未指定 sheetName 則自動將活頁簿中所有相符之工作表全部讀出並合併回傳，回傳 JSON。"
	)
	@ApiResponse(
			responseCode = "200",
			description = "成功解析物料 Excel 內容",
			content = @Content(
					mediaType = MediaType.APPLICATION_JSON_VALUE,
					schema = @Schema(implementation = MaterialImportedResource.class)
			)
	)
	@ApiResponse(
			responseCode = "400",
			description = "檔案為空、工作表不存在或解析失敗",
			content = @Content(
					mediaType = MediaType.APPLICATION_JSON_VALUE,
					schema = @Schema(implementation = ValidationFailedResource.class)
			)
	)
	@PostMapping(value = "/materials/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<MaterialImportedResource> importMaterials(
			@Parameter(description = "欲上傳解析之物料 Excel 檔案 (.xlsx)", required = true)
			@RequestParam("file") MultipartFile file,
			@Parameter(description = "指定讀取之工作表名稱 (選填，若未填寫則讀取活頁簿中全部相符工作表)", required = false)
			@RequestParam(value = "sheetName", required = false) String sheetName
	) throws IOException {
		validateFile(file);
		byte[] fileBytes = file.getBytes();
		if (sheetName != null && !sheetName.isBlank()) {
			return ResponseEntity.ok(dispatchSingleSheetToMaterial(fileBytes, sheetName.trim()));
		}
		return ResponseEntity.ok(dispatchAllSheetsToMaterial(fileBytes));
	}

	/**
	 * API 2-B: 上傳 Excel 檔案並指定工作表名稱解析為物料模型清單 (SheetName-Driven 模式)
	 * <p>
	 * 上傳物料 Excel 檔案並指定讀取特定工作表名稱 ({@code sheetName})，若在活頁簿中找不到該工作表則嚴格拋出例外，
	 * 防止錯誤降級讀取非預期資料。
	 * </p>
	 *
	 * @param file      欲上傳解析之物料 Excel 檔案 (.xlsx)
	 * @param sheetName 指定讀取之工作表名稱 (必填)
	 * @return HTTP 200 與解析完成之 {@link MaterialImportedResource} 結構化資料
	 * @throws IOException 當讀取檔案輸入串流失敗時拋出
	 */
	@Operation(
			summary = "檔案上傳並指定工作表名稱解析為 MaterialExcelModel (SheetName 模式)",
			description = "上傳物料 Excel 檔案並指定讀取特定工作表名稱 (sheetName)，若找不到該工作表則嚴格拋出例外，成功則回傳 JSON。"
	)
	@ApiResponse(
			responseCode = "200",
			description = "成功解析指定工作表之物料 Excel 內容",
			content = @Content(
					mediaType = MediaType.APPLICATION_JSON_VALUE,
					schema = @Schema(implementation = MaterialImportedResource.class)
			)
	)
	@ApiResponse(
			responseCode = "400",
			description = "檔案為空、工作表不存在或解析失敗",
			content = @Content(
					mediaType = MediaType.APPLICATION_JSON_VALUE,
					schema = @Schema(implementation = ValidationFailedResource.class)
			)
	)
	@PostMapping(value = "/materials/import/by-sheet", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<MaterialImportedResource> importMaterialsBySheet(
			@Parameter(description = "欲上傳解析之物料 Excel 檔案 (.xlsx)", required = true)
			@RequestParam("file") MultipartFile file,
			@Parameter(description = "指定讀取之工作表名稱 (必填)", required = true)
			@RequestParam("sheetName") String sheetName
	) throws IOException {
		validateFile(file);
		validateSheetName(sheetName);
		return ResponseEntity.ok(dispatchSingleSheetToMaterial(file.getBytes(), sheetName.trim()));
	}

	// =========================================================================
	// 2. BOM (物料清單配方) 宣告式匯出與匯入
	// =========================================================================

	/**
	 * API 3: 匯出系統現有單階 BOM 配方 Excel (BOM 業務模型)
	 * <p>
	 * 直接從 BOM 與物料儲存庫查詢系統現有 BOM 聚合根與關聯物料，
	 * 透過純領域方法提取屬性，並由 {@code ExcelBeanAssembler} 宣告式產製 {@link BomItemExcelModel} 之標準 Excel 檔案。
	 * </p>
	 *
	 * @return HTTP 200 與 bom_items_master.xlsx 二進位檔案下載串流
	 */
	@Operation(
			summary = "匯出系統現有單階 BOM 配方 Excel (BOM 業務模型)",
			description = "直接從 BOM 與物料儲存庫查詢現有 BOM 與關聯物料聚合根，透過純領域方法提取屬性並宣告式產製 BomItemExcelModel 之 Excel 檔案。"
	)
	@GetMapping("/bom-items/export")
	public ResponseEntity<byte[]> exportLiveBomItems() {
		// ---------------------------------------------------------------------
		// 步驟 1：調度應用層匯出業務案例 (Application Inbound Port)
		// ---------------------------------------------------------------------
		// ExportMaterialBomUseCase 協調 BomRepositoryPort 展開現有單階關聯，
		// 產製標準 OpenXML 試算表二進位位元組。
		byte[] excelBytes = exportMaterialBomUseCase.exportBomItems();

		// ---------------------------------------------------------------------
		// 步驟 2：封裝標準二進位檔案下載 HTTP 回應 (RFC 5987 安全編碼)
		// ---------------------------------------------------------------------
		return createDownloadResponse(excelBytes, "bom_items_master.xlsx");
	}

	/**
	 * API 4-A: 上傳 Excel 檔案並宣告式解析為單階 BOM 配方模型清單 (支援單表與多表全量解析)
	 * <p>
	 * 上傳 BOM 配方 Excel 檔案。若指定 {@code sheetName} 則精確讀取該工作表 (找不到時拋出例外)；
	 * 若未指定 {@code sheetName}，則自動走訪活頁簿中所有符合 {@link BomItemExcelModel} 之工作表並全部讀出合併回傳。
	 * </p>
	 *
	 * @param file      欲上傳解析之 BOM 配方 Excel 檔案 (.xlsx)
	 * @param sheetName 指定讀取之工作表名稱 (選填，未填則全數讀出)
	 * @return HTTP 200 與解析完成之 {@link BomItemImportedResource} 結構化資料
	 * @throws IOException 當讀取檔案輸入串流失敗時拋出
	 */
	@Operation(
			summary = "檔案上傳並宣告式解析為 BomItemExcelModel 清單 (可選填 sheetName)",
			description = "上傳 BOM Excel 檔案。若指定 sheetName 則精確讀取該工作表 (找不到時拋出 400 錯誤)；若未指定 sheetName 則自動將活頁簿中所有相符之工作表全部讀出並合併回傳，回傳 JSON。"
	)
	@ApiResponse(
			responseCode = "200",
			description = "成功解析 BOM Excel 內容",
			content = @Content(
					mediaType = MediaType.APPLICATION_JSON_VALUE,
					schema = @Schema(implementation = BomItemImportedResource.class)
			)
	)
	@ApiResponse(
			responseCode = "400",
			description = "檔案為空、工作表不存在或解析失敗",
			content = @Content(
					mediaType = MediaType.APPLICATION_JSON_VALUE,
					schema = @Schema(implementation = ValidationFailedResource.class)
			)
	)
	@PostMapping(value = "/bom-items/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<BomItemImportedResource> importBomItems(
			@Parameter(description = "欲上傳解析之 BOM 配方 Excel 檔案 (.xlsx)", required = true)
			@RequestParam("file") MultipartFile file,
			@Parameter(description = "指定讀取之工作表名稱 (選填，若未填寫則讀取活頁簿中全部相符工作表)", required = false)
			@RequestParam(value = "sheetName", required = false) String sheetName
	) throws IOException {
		validateFile(file);
		byte[] fileBytes = file.getBytes();
		if (sheetName != null && !sheetName.isBlank()) {
			return ResponseEntity.ok(dispatchSingleSheetToBom(fileBytes, sheetName.trim()));
		}
		return ResponseEntity.ok(dispatchAllSheetsToBom(fileBytes));
	}

	/**
	 * API 4-B: 上傳 Excel 檔案並指定工作表名稱解析為單階 BOM 配方模型清單 (SheetName-Driven 模式)
	 * <p>
	 * 上傳 BOM 配方 Excel 檔案並指定讀取特定工作表名稱 ({@code sheetName})，若在活頁簿中找不到該工作表則嚴格拋出例外，
	 * 防止錯誤降級讀取非預期資料。
	 * </p>
	 *
	 * @param file      欲上傳解析之 BOM 配方 Excel 檔案 (.xlsx)
	 * @param sheetName 指定讀取之工作表名稱 (必填)
	 * @return HTTP 200 與解析完成之 {@link BomItemImportedResource} 結構化資料
	 * @throws IOException 當讀取檔案輸入串流失敗時拋出
	 */
	@Operation(
			summary = "檔案上傳並指定工作表名稱解析為 BomItemExcelModel (SheetName 模式)",
			description = "上傳 BOM Excel 檔案並指定讀取特定工作表名稱 (sheetName)，若找不到該工作表則嚴格拋出例外，成功則回傳 JSON。"
	)
	@ApiResponse(
			responseCode = "200",
			description = "成功解析指定工作表之 BOM Excel 內容",
			content = @Content(
					mediaType = MediaType.APPLICATION_JSON_VALUE,
					schema = @Schema(implementation = BomItemImportedResource.class)
			)
	)
	@ApiResponse(
			responseCode = "400",
			description = "檔案為空、工作表不存在或解析失敗",
			content = @Content(
					mediaType = MediaType.APPLICATION_JSON_VALUE,
					schema = @Schema(implementation = ValidationFailedResource.class)
			)
	)
	@PostMapping(value = "/bom-items/import/by-sheet", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<BomItemImportedResource> importBomItemsBySheet(
			@Parameter(description = "欲上傳解析之 BOM 配方 Excel 檔案 (.xlsx)", required = true)
			@RequestParam("file") MultipartFile file,
			@Parameter(description = "指定讀取之工作表名稱 (必填)", required = true)
			@RequestParam("sheetName") String sheetName
	) throws IOException {
		validateFile(file);
		validateSheetName(sheetName);
		return ResponseEntity.ok(dispatchSingleSheetToBom(file.getBytes(), sheetName.trim()));
	}

	// =========================================================================
	// 3. Multi-Level BOM (多階層 BOM 展開清單) 宣告式匯入
	// =========================================================================

	/**
	 * API 5: 上傳 Excel 檔案並宣告式解析為多階層 BOM 展開模型清單 (Multi-Level BOM 專屬模型)
	 * <p>
	 * 上傳 BOM Excel 檔案。若指定 {@code sheetName} 則精確讀取該工作表 (找不到時拋出例外)；
	 * 若未指定 {@code sheetName}，則自動走訪活頁簿中所有符合 {@link MultiLevelBomExcelModel} 之工作表並全部讀出。
	 * </p>
	 *
	 * @param file      欲上傳解析之多階層 BOM Excel 檔案 (.xlsx)
	 * @param sheetName 指定讀取之工作表名稱 (選填，未填則全數讀出)
	 * @return HTTP 200 與解析完成之 {@link MultiLevelBomImportedResource} 結構化資料
	 * @throws IOException 當讀取檔案輸入串流失敗時拋出
	 */
	@Operation(
			summary = "檔案上傳並宣告式解析為 MultiLevelBomExcelModel 清單 (多階層展開專屬模型)",
			description = "上傳 BOM Excel 檔案並解析為包含階層 (Level)、階層序號 (WBS)、規格型號、有效用量、標準單價與成本小計之多階層展開清單，回傳 JSON。"
	)
	@ApiResponse(
			responseCode = "200",
			description = "成功解析多階層 BOM Excel 內容",
			content = @Content(
					mediaType = MediaType.APPLICATION_JSON_VALUE,
					schema = @Schema(implementation = MultiLevelBomImportedResource.class)
			)
	)
	@ApiResponse(
			responseCode = "400",
			description = "檔案為空、工作表不存在或解析失敗",
			content = @Content(
					mediaType = MediaType.APPLICATION_JSON_VALUE,
					schema = @Schema(implementation = ValidationFailedResource.class)
			)
	)
	@PostMapping(value = "/multi-level-bom/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<MultiLevelBomImportedResource> importMultiLevelBom(
			@Parameter(description = "欲上傳解析之多階層 BOM Excel 檔案 (.xlsx)", required = true)
			@RequestParam("file") MultipartFile file,
			@Parameter(description = "指定讀取之工作表名稱 (選填，若未填寫則讀取活頁簿中全部相符工作表)", required = false)
			@RequestParam(value = "sheetName", required = false) String sheetName
	) throws IOException {

		validateFile(file);

		BeanSheetGottenResult<MultiLevelBomExcelModel> result = importExcelUseCase.importBeanSheet(
				file.getInputStream(), sheetName, MultiLevelBomExcelModel.class);

		MultiLevelBomImportedResource response = resourceAssembler.toMultiLevelBomImportedResource(
				result.items(), result.sheetName());

		return ResponseEntity.ok(response);
	}

	// =========================================================================
	// 私有輔助方法 (Private Helpers)
	// =========================================================================

	/**
	 * 驗證上傳之 MultipartFile 檔案是否合法
	 *
	 * @param file 上傳之檔案
	 * @throws IllegalArgumentException 當檔案為空或無內容時拋出
	 */
	private void validateFile(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("上傳之 Excel 檔案不能為空");
		}
	}

	/**
	 * 驗證指定之工作表名稱字串是否合法
	 *
	 * @param sheetName 工作表名稱
	 * @throws IllegalArgumentException 當名稱為空或空白時拋出
	 */
	private void validateSheetName(String sheetName) {
		if (sheetName == null || sheetName.isBlank()) {
			throw new IllegalArgumentException("指定讀取之工作表名稱不能為空");
		}
	}

	/**
	 * 單一工作表物料分發調度器：根據工作表名稱向 Registry 動態查詢目標 Model 並封裝為 {@link MaterialImportedResource}
	 *
	 * @param fileBytes 上傳之 Excel 檔案位元組資料
	 * @param sheetName 指定之工作表名稱
	 * @return 封裝完成之物料回應資源
	 */
	private MaterialImportedResource dispatchSingleSheetToMaterial(byte[] fileBytes, String sheetName) {
		Class<?> targetModel = sheetModelRegistryPort.findModelClass(sheetName)
				.orElse(MaterialExcelModel.class);

		if (MultiLevelBomExcelModel.class.equals(targetModel)) {
			BeanSheetGottenResult<MultiLevelBomExcelModel> multiResult = importExcelUseCase.importBeanSheet(
					new ByteArrayInputStream(fileBytes), sheetName, MultiLevelBomExcelModel.class);
			return resourceAssembler.toMaterialImportedResourceFromMultiBom(multiResult);
		} else if (BomItemExcelModel.class.equals(targetModel)) {
			BeanSheetGottenResult<BomItemExcelModel> singleResult = importExcelUseCase.importBeanSheet(
					new ByteArrayInputStream(fileBytes), sheetName, BomItemExcelModel.class);
			return resourceAssembler.toMaterialImportedResourceFromSingleBom(singleResult);
		} else {
			BeanSheetGottenResult<MaterialExcelModel> matResult = importExcelUseCase.importBeanSheet(
					new ByteArrayInputStream(fileBytes), sheetName, MaterialExcelModel.class);
			return resourceAssembler.toMaterialImportedResource(matResult);
		}
	}

	/**
	 * 全工作表物料分發調度器：自動掃描讀取活頁簿中所有相符工作表並合併封裝為 {@link MaterialImportedResource}
	 *
	 * @param fileBytes 上傳之 Excel 檔案位元組資料
	 * @return 封裝完成之全量物料回應資源
	 */
	private MaterialImportedResource dispatchAllSheetsToMaterial(byte[] fileBytes) {
		BeanSheetGottenResult<MaterialExcelModel> matResult = safelyReadBeanSheet(fileBytes, MaterialExcelModel.class);
		BeanSheetGottenResult<BomItemExcelModel> singleResult = safelyReadBeanSheet(fileBytes, BomItemExcelModel.class);
		BeanSheetGottenResult<MultiLevelBomExcelModel> multiResult = safelyReadBeanSheet(fileBytes, MultiLevelBomExcelModel.class);

		return resourceAssembler.toMaterialImportedResource(
				matResult != null ? matResult.items() : Collections.emptyList(),
				matResult != null ? matResult.sheetName() : null,
				singleResult != null ? singleResult.items() : Collections.emptyList(),
				singleResult != null ? singleResult.sheetName() : null,
				multiResult != null ? multiResult.items() : Collections.emptyList(),
				multiResult != null ? multiResult.sheetName() : null
		);
	}

	/**
	 * 單一工作表 BOM 分發調度器：根據工作表名稱向 Registry 動態查詢目標 Model 並封裝為 {@link BomItemImportedResource}
	 *
	 * @param fileBytes 上傳之 Excel 檔案位元組資料
	 * @param sheetName 指定之工作表名稱
	 * @return 封裝完成之 BOM 回應資源
	 */
	private BomItemImportedResource dispatchSingleSheetToBom(byte[] fileBytes, String sheetName) {
		Class<?> targetModel = sheetModelRegistryPort.findModelClass(sheetName)
				.orElse(BomItemExcelModel.class);

		if (MultiLevelBomExcelModel.class.equals(targetModel)) {
			BeanSheetGottenResult<MultiLevelBomExcelModel> multiResult = importExcelUseCase.importBeanSheet(
					new ByteArrayInputStream(fileBytes), sheetName, MultiLevelBomExcelModel.class);
			return resourceAssembler.toBomItemImportedResourceFromMultiBom(multiResult);
		} else if (MaterialExcelModel.class.equals(targetModel)) {
			BeanSheetGottenResult<MaterialExcelModel> matResult = importExcelUseCase.importBeanSheet(
					new ByteArrayInputStream(fileBytes), sheetName, MaterialExcelModel.class);
			return resourceAssembler.toBomItemImportedResourceFromMaterial(matResult);
		} else {
			BeanSheetGottenResult<BomItemExcelModel> singleResult = importExcelUseCase.importBeanSheet(
					new ByteArrayInputStream(fileBytes), sheetName, BomItemExcelModel.class);
			return resourceAssembler.toBomItemImportedResource(singleResult);
		}
	}

	/**
	 * 全工作表 BOM 分發調度器：自動掃描讀取活頁簿中所有相符工作表並合併封裝為 {@link BomItemImportedResource}
	 *
	 * @param fileBytes 上傳之 Excel 檔案位元組資料
	 * @return 封裝完成之全量 BOM 回應資源
	 */
	private BomItemImportedResource dispatchAllSheetsToBom(byte[] fileBytes) {
		BeanSheetGottenResult<BomItemExcelModel> singleResult = safelyReadBeanSheet(fileBytes, BomItemExcelModel.class);
		BeanSheetGottenResult<MultiLevelBomExcelModel> multiResult = safelyReadBeanSheet(fileBytes, MultiLevelBomExcelModel.class);
		BeanSheetGottenResult<MaterialExcelModel> matResult = safelyReadBeanSheet(fileBytes, MaterialExcelModel.class);

		return resourceAssembler.toBomItemImportedResource(
				singleResult != null ? singleResult.items() : Collections.emptyList(),
				singleResult != null ? singleResult.sheetName() : null,
				multiResult != null ? multiResult.items() : Collections.emptyList(),
				multiResult != null ? multiResult.sheetName() : null,
				matResult != null ? matResult.items() : Collections.emptyList(),
				matResult != null ? matResult.sheetName() : null
		);
	}

	/**
	 * 安全讀取指定模型之工作表資料，若模型未命中或解析異常則安全忽略並回傳 null
	 *
	 * @param fileBytes  Excel 檔案位元組資料
	 * @param modelClass 目標宣告式模型 Class
	 * @param <T>        模型型別
	 * @return 工作表解析結果，若解析失敗或不存在則回傳 null
	 */
	private <T> BeanSheetGottenResult<T> safelyReadBeanSheet(byte[] fileBytes, Class<T> modelClass) {
		try {
			return importExcelUseCase.importBeanSheet(new ByteArrayInputStream(fileBytes), null, modelClass);
		} catch (Exception ignored) {
			return null;
		}
	}

	/**
	 * 建構標準 Excel 二進位檔案下載 HTTP 回應實體 (RFC 5987 UTF-8 檔名安全編碼)
	 *
	 * @param content         Excel 二進位檔案位元組內容
	 * @param defaultFileName 下載預設檔案名稱 (如 "materials_master.xlsx")
	 * @return 封裝完成之 HTTP 200 檔案下載回應實體
	 */
	private ResponseEntity<byte[]> createDownloadResponse(byte[] content, String defaultFileName) {
		// ---------------------------------------------------------------------
		// 步驟 1：檔名 UTF-8 安全編碼 (RFC 5987 規範，避免中文字元亂碼)
		// ---------------------------------------------------------------------
		String encodedFileName = URLEncoder.encode(defaultFileName, StandardCharsets.UTF_8).replace("+", "%20");

		// ---------------------------------------------------------------------
		// 步驟 2：建構標準 HTTP 下載標頭與 MIME Content-Type
		// ---------------------------------------------------------------------
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.parseMediaType(EXCEL_MIME_TYPE));
		headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName);
		headers.setContentLength(content.length);

		// ---------------------------------------------------------------------
		// 步驟 3：封裝為 HTTP 200 OK 二進位回應實體
		// ---------------------------------------------------------------------
		return ResponseEntity.ok().headers(headers).body(content);
	}
}
