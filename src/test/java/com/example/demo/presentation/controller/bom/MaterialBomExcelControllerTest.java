package com.example.demo.presentation.controller.bom;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.demo.application.port.in.bom.ExportMaterialBomUseCase;
import com.example.demo.application.port.in.core.ImportExcelUseCase;
import com.example.demo.application.port.out.core.ExcelSheetModelRegistryPort;
import com.example.demo.application.shared.dto.BeanSheetGottenResult;
import com.example.demo.application.shared.model.BomItemExcelModel;
import com.example.demo.application.shared.model.MaterialExcelModel;
import com.example.demo.application.shared.model.MultiLevelBomExcelModel;
import com.example.demo.config.ExcelSheetModelConfiguration;
import com.example.demo.presentation.assembler.MaterialBomResourceAssembler;
import com.example.demo.presentation.exception.GlobalExceptionHandler;

/**
 * 物料與 BOM 宣告式 Excel 映射控制器測試 (MaterialBomExcelController Test)
 * <p>
 * 遵循 BDD 與 Gherkin 規範，驗證物料主檔 (Material) 與單階 BOM 配方清單 (BOM)
 * 業務模型對應之宣告式 Excel 匯出與解析端點。
 */
@DisplayName("MaterialBomExcelController 物料與 BOM 宣告式 Excel 控制器測試")
class MaterialBomExcelControllerTest {

	private MockMvc mockMvc;
	private ExportMaterialBomUseCase exportMaterialBomUseCase;
	private ImportExcelUseCase importExcelUseCase;

	@BeforeEach
	void setUp() {
		exportMaterialBomUseCase = mock(ExportMaterialBomUseCase.class);
		importExcelUseCase = mock(ImportExcelUseCase.class);
		MaterialBomResourceAssembler resourceAssembler = new MaterialBomResourceAssembler();
		ExcelSheetModelRegistryPort registryPort = ExcelSheetModelConfiguration.createDefaultRegistry();

		MaterialBomExcelController controller = new MaterialBomExcelController(
				exportMaterialBomUseCase,
				importExcelUseCase,
				resourceAssembler,
				registryPort
		);

		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	// =========================================================================
	// 1. Material (物料主檔) 測試案例
	// =========================================================================

	/**
	 * Feature: 物料聚合根快速匯出
	 * Scenario: 透過 GET /api/v1/excel/material-bom/materials/export 下載系統現有物料 Excel
	 * Given: ExportMaterialBomUseCase 產製 mock Excel byte[]
	 * When: 發送 GET 請求
	 * Then: 回應 200 OK，包含正確之 attachment 檔名與 Excel MIME
	 */
	@Test
	@DisplayName("場景：GET 快速下載現有物料主檔 Excel")
	void shouldExportLiveMaterialsSuccessfully() throws Exception {
		// Given
		byte[] mockBytes = "MATERIALS_EXCEL_BYTES".getBytes();
		when(exportMaterialBomUseCase.exportMaterials()).thenReturn(mockBytes);

		// When & Then (驗證標準路徑 /api/v1/excel/material-bom/materials/export)
		mockMvc.perform(get("/api/v1/excel/material-bom/materials/export"))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				.andExpect(header().string("Content-Disposition", "attachment; filename=\"materials_master.xlsx\"; filename*=UTF-8''materials_master.xlsx"))
				.andExpect(content().bytes(mockBytes));

		verify(exportMaterialBomUseCase).exportMaterials();
	}

	/**
	 * Feature: 物料主檔 Excel 上傳解析 (未指定 sheetName 時全量讀取)
	 * Scenario: 上傳物料 Excel 檔案未傳入 sheetName，自動解析活頁簿中所有相符之物料工作表
	 * Given: 模擬上傳包含 1 筆物料之 Excel 檔案，ImportExcelUseCase 回傳包含真實工作表名稱之結果
	 * When: 發送 POST multipart 請求至 /materials/import (無 sheetName)
	 * Then: 回應 200 OK 且包含正確之 MaterialImportedResource JSON 結構與真實工作表名稱
	 */
	@Test
	@DisplayName("場景：POST 上傳檔案未指定 sheetName 宣告式解析為 MaterialImportedResource")
	void shouldImportMaterialsSuccessfully() throws Exception {
		// Given
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"materials.xlsx",
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				new byte[]{1, 2, 3}
		);

		MaterialExcelModel parsedModel = new MaterialExcelModel(
				"MAT-001", "高精度馬達", "2204-KV2300", "RAW_MATERIAL", "PCS", new BigDecimal("450.00"), "ACTIVE"
		);
		when(importExcelUseCase.importBeanSheet(any(InputStream.class), isNull(), eq(MaterialExcelModel.class)))
				.thenReturn(new BeanSheetGottenResult<>("物料主檔對照表", List.of(parsedModel)));

		// When & Then
		mockMvc.perform(multipart("/api/v1/excel/material-bom/materials/import").file(file))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.totalCount").value(1))
				.andExpect(jsonPath("$.sheetName").value("物料主檔對照表"))
				.andExpect(jsonPath("$.items[0].materialCode").value("MAT-001"))
				.andExpect(jsonPath("$.items[0].materialName").value("高精度馬達"));

		verify(importExcelUseCase).importBeanSheet(any(InputStream.class), isNull(), eq(MaterialExcelModel.class));
	}

	/**
	 * Feature: 物料主檔 Excel 上傳解析 (選填指定 sheetName)
	 * Scenario: 上傳物料 Excel 檔案並傳入特定 sheetName="物料主檔對照表"
	 * Given: 模擬上傳包含物料資料之 Excel 檔案
	 * When: 發送 POST multipart 請求至 /materials/import 並帶有 sheetName 參數
	 * Then: 回應 200 OK 且正確解析該指定工作表
	 */
	@Test
	@DisplayName("場景：POST /materials/import 填入 sheetName 參數精確解析")
	void shouldImportMaterialsWithSheetNameParam() throws Exception {
		// Given
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"materials.xlsx",
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				new byte[]{1, 2, 3}
		);

		MaterialExcelModel parsedModel = new MaterialExcelModel(
				"MAT-001", "高精度馬達", "2204-KV2300", "RAW_MATERIAL", "PCS", new BigDecimal("450.00"), "ACTIVE"
		);
		when(importExcelUseCase.importBeanSheet(any(InputStream.class), eq("物料主檔對照表"), eq(MaterialExcelModel.class)))
				.thenReturn(new BeanSheetGottenResult<>("物料主檔對照表", List.of(parsedModel)));

		// When & Then
		mockMvc.perform(multipart("/api/v1/excel/material-bom/materials/import")
						.file(file)
						.param("sheetName", "物料主檔對照表"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.totalCount").value(1))
				.andExpect(jsonPath("$.sheetName").value("物料主檔對照表"));

		verify(importExcelUseCase).importBeanSheet(any(InputStream.class), eq("物料主檔對照表"), eq(MaterialExcelModel.class));
	}

	/**
	 * Feature: 物料主檔指定工作表 Excel 上傳解析 (SheetName 模式)
	 * Scenario: 上傳物料 Excel 檔案並指定 sheetName 參數精確解析
	 * Given: 模擬上傳 Excel 檔案並指定 sheetName="自訂物料表"
	 * When: 發送 POST multipart 請求至 /materials/import/by-sheet
	 * Then: 回應 200 OK 且包含指定工作表名稱與解析資料
	 */
	@Test
	@DisplayName("場景：POST 上傳檔案指定 sheetName 成功解析為 MaterialImportedResource")
	void shouldImportMaterialsBySheetSuccessfully() throws Exception {
		// Given
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"materials.xlsx",
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				new byte[]{1, 2, 3}
		);

		MaterialExcelModel parsedModel = new MaterialExcelModel(
				"MAT-002", "碳纖維機架", "F450-V2", "RAW_MATERIAL", "SET", new BigDecimal("1200.00"), "ACTIVE"
		);
		when(importExcelUseCase.importBeanSheet(any(InputStream.class), eq("自訂物料表"), eq(MaterialExcelModel.class)))
				.thenReturn(new BeanSheetGottenResult<>("自訂物料表", List.of(parsedModel)));

		// When & Then
		mockMvc.perform(multipart("/api/v1/excel/material-bom/materials/import/by-sheet")
						.file(file)
						.param("sheetName", "自訂物料表"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.totalCount").value(1))
				.andExpect(jsonPath("$.sheetName").value("自訂物料表"))
				.andExpect(jsonPath("$.items[0].materialCode").value("MAT-002"));

		verify(importExcelUseCase).importBeanSheet(any(InputStream.class), eq("自訂物料表"), eq(MaterialExcelModel.class));
	}

	/**
	 * Feature: 物料主檔指定工作表不存在異常處理 (SheetName 模式)
	 * Scenario: 指定之工作表在 Excel 活頁簿中不存在
	 * Given: 模擬調用 importBeanSheet 拋出 IllegalArgumentException
	 * When: 發送 POST multipart 請求至 /materials/import/by-sheet
	 * Then: 回應 400 Bad Request
	 */
	@Test
	@DisplayName("異常：指定不存在之工作表名稱時拋出 400 錯誤")
	void shouldRejectWhenSheetNameNotFoundOnImportMaterialsBySheet() throws Exception {
		// Given
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"materials.xlsx",
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				new byte[]{1, 2, 3}
		);
		when(importExcelUseCase.importBeanSheet(any(InputStream.class), eq("不存在的表"), eq(MaterialExcelModel.class)))
				.thenThrow(new IllegalArgumentException("在 Excel 活頁簿中找不到指定名稱之工作表: 不存在的表"));

		// When & Then
		mockMvc.perform(multipart("/api/v1/excel/material-bom/materials/import/by-sheet")
						.file(file)
						.param("sheetName", "不存在的表"))
				.andExpect(status().isBadRequest());
	}

	// =========================================================================
	// 2. BOM (單階 BOM 配方清單) 測試案例
	// =========================================================================

	/**
	 * Feature: 單階 BOM 配方清單快速匯出
	 * Scenario: 透過 GET /api/v1/excel/material-bom/bom-items/export 下載系統現有單階 BOM Excel
	 * Given: ExportMaterialBomUseCase 產製 mock Excel byte[]
	 * When: 發送 GET 請求
	 * Then: 回應 200 OK，包含正確之 attachment 檔名與 Excel MIME
	 */
	@Test
	@DisplayName("場景：GET 快速下載現有單階 BOM 配方 Excel")
	void shouldExportLiveBomItemsSuccessfully() throws Exception {
		// Given
		byte[] mockBytes = "BOM_ITEMS_EXCEL_BYTES".getBytes();
		when(exportMaterialBomUseCase.exportBomItems()).thenReturn(mockBytes);

		// When & Then
		mockMvc.perform(get("/api/v1/excel/material-bom/bom-items/export"))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				.andExpect(header().string("Content-Disposition", "attachment; filename=\"bom_items_master.xlsx\"; filename*=UTF-8''bom_items_master.xlsx"))
				.andExpect(content().bytes(mockBytes));

		verify(exportMaterialBomUseCase).exportBomItems();
	}

	/**
	 * Feature: 單階 BOM 配方 Excel 上傳解析 (未指定 sheetName 時全量讀取)
	 * Scenario: 上傳 BOM Excel 檔案未傳入 sheetName，自動走訪讀取所有相符之 BOM 工作表
	 * Given: 模擬上傳包含 1 筆 BOM 子項之 Excel 檔案，ImportExcelUseCase 回傳真實工作表名稱「全部工作表 (多階層BOM展開清單, 單階BOM匯入清單)」
	 * When: 發送 POST multipart 請求至 /bom-items/import (無 sheetName)
	 * Then: 回應 200 OK 且包含正確之 BomItemImportedResource JSON 結構與真實工作表名稱
	 */
	@Test
	@DisplayName("場景：POST 上傳檔案未指定 sheetName 宣告式解析為 BomItemImportedResource")
	void shouldImportBomItemsSuccessfully() throws Exception {
		// Given
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"bom_items.xlsx",
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				new byte[]{4, 5, 6}
		);

		BomItemExcelModel parsedModel = new BomItemExcelModel(
				"MAT-FG-001", "1.0", 1, "MAT-RM-001", "碳纖維螺旋槳",
				new BigDecimal("4"), "PCS", new BigDecimal("0.02"), "P1-P4", "四軸螺旋槳"
		);
		when(importExcelUseCase.importBeanSheet(any(InputStream.class), isNull(), eq(BomItemExcelModel.class)))
				.thenReturn(new BeanSheetGottenResult<>("全部工作表", List.of(parsedModel)));

		// When & Then
		mockMvc.perform(multipart("/api/v1/excel/material-bom/bom-items/import").file(file))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.totalCount").value(1))
				.andExpect(jsonPath("$.sheetName").value("全部工作表"))
				.andExpect(jsonPath("$.items[0].parentMaterialCode").value("MAT-FG-001"))
				.andExpect(jsonPath("$.items[0].childMaterialCode").value("MAT-RM-001"));

		verify(importExcelUseCase).importBeanSheet(any(InputStream.class), isNull(), eq(BomItemExcelModel.class));
	}

	/**
	 * Feature: 單階 BOM 配方 Excel 上傳解析 (選填指定 sheetName)
	 * Scenario: 上傳 BOM Excel 檔案並傳入特定 sheetName="單階BOM匯入清單"
	 * Given: 模擬上傳包含 BOM 資料之 Excel 檔案
	 * When: 發送 POST multipart 請求至 /bom-items/import 並帶有 sheetName 參數
	 * Then: 回應 200 OK 且正確解析該指定工作表
	 */
	@Test
	@DisplayName("場景：POST /bom-items/import 填入 sheetName 參數精確解析")
	void shouldImportBomItemsWithSheetNameParam() throws Exception {
		// Given
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"bom_items.xlsx",
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				new byte[]{4, 5, 6}
		);

		BomItemExcelModel parsedModel = new BomItemExcelModel(
				"MAT-FG-001", "1.0", 1, "MAT-RM-001", "碳纖維螺旋槳",
				new BigDecimal("4"), "PCS", new BigDecimal("0.02"), "P1-P4", "四軸螺旋槳"
		);
		when(importExcelUseCase.importBeanSheet(any(InputStream.class), eq("單階BOM匯入清單"), eq(BomItemExcelModel.class)))
				.thenReturn(new BeanSheetGottenResult<>("單階BOM匯入清單", List.of(parsedModel)));

		// When & Then
		mockMvc.perform(multipart("/api/v1/excel/material-bom/bom-items/import")
						.file(file)
						.param("sheetName", "單階BOM匯入清單"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.totalCount").value(1))
				.andExpect(jsonPath("$.sheetName").value("單階BOM匯入清單"));

		verify(importExcelUseCase).importBeanSheet(any(InputStream.class), eq("單階BOM匯入清單"), eq(BomItemExcelModel.class));
	}

	/**
	 * Feature: 單階 BOM 配方指定工作表 Excel 上傳解析 (SheetName 模式)
	 * Scenario: 上傳 BOM Excel 檔案並指定 sheetName="單階BOM匯入清單"
	 * Given: 模擬上傳 Excel 檔案並指定工作表名稱
	 * When: 發送 POST multipart 請求至 /bom-items/import/by-sheet
	 * Then: 回應 200 OK 且包含指定工作表名稱與資料
	 */
	@Test
	@DisplayName("場景：POST 上傳檔案指定 sheetName 成功解析為 BomItemImportedResource")
	void shouldImportBomItemsBySheetSuccessfully() throws Exception {
		// Given
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"bom_items.xlsx",
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				new byte[]{4, 5, 6}
		);

		BomItemExcelModel parsedModel = new BomItemExcelModel(
				"MAT-FG-001", "1.0", 1, "MAT-SFG-001", "飛控主板電路模組總成",
				new BigDecimal("1"), "PCS", BigDecimal.ZERO, "FC-ASM", "核心飛控組裝件"
		);
		when(importExcelUseCase.importBeanSheet(any(InputStream.class), eq("單階BOM匯入清單"), eq(BomItemExcelModel.class)))
				.thenReturn(new BeanSheetGottenResult<>("單階BOM匯入清單", List.of(parsedModel)));

		// When & Then
		mockMvc.perform(multipart("/api/v1/excel/material-bom/bom-items/import/by-sheet")
						.file(file)
						.param("sheetName", "單階BOM匯入清單"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.totalCount").value(1))
				.andExpect(jsonPath("$.sheetName").value("單階BOM匯入清單"))
				.andExpect(jsonPath("$.items[0].childMaterialName").value("飛控主板電路模組總成"));

		verify(importExcelUseCase).importBeanSheet(any(InputStream.class), eq("單階BOM匯入清單"), eq(BomItemExcelModel.class));
	}

	/**
	 * Feature: 單階 BOM 指定工作表不存在異常處理 (SheetName 模式)
	 * Scenario: 指定之工作表在 Excel 活頁簿中不存在
	 * Given: 模擬調用 importBeanSheet 拋出 IllegalArgumentException
	 * When: 發送 POST multipart 請求至 /bom-items/import/by-sheet
	 * Then: 回應 400 Bad Request
	 */
	@Test
	@DisplayName("異常：指定不存在之工作表名稱時拋出 400 錯誤")
	void shouldRejectWhenSheetNameNotFoundOnImportBomItemsBySheet() throws Exception {
		// Given
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"bom_items.xlsx",
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				new byte[]{4, 5, 6}
		);
		when(importExcelUseCase.importBeanSheet(any(InputStream.class), eq("不存在的BOM表"), eq(BomItemExcelModel.class)))
				.thenThrow(new IllegalArgumentException("在 Excel 活頁簿中找不到指定名稱之工作表: 不存在的BOM表"));

		// When & Then
		mockMvc.perform(multipart("/api/v1/excel/material-bom/bom-items/import/by-sheet")
						.file(file)
						.param("sheetName", "不存在的BOM表"))
				.andExpect(status().isBadRequest());
	}

	/**
	 * Feature: 多階層 BOM 展開清單 Excel 上傳解析
	 * Scenario: 上傳 BOM Excel 檔案至 /multi-level-bom/import 解析為多階層展開專屬模型
	 * Given: 模擬上傳包含多階層資料之 Excel 檔案
	 * When: 發送 POST multipart 請求至 /multi-level-bom/import
	 * Then: 回應 200 OK 且正確解析出 MultiLevelBomImportedResource 結構
	 */
	@Test
	@DisplayName("場景：POST /multi-level-bom/import 解析多階層展開清單")
	void shouldImportMultiLevelBomSuccessfully() throws Exception {
		// Given
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"bom_multilevel.xlsx",
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				new byte[]{7, 8, 9}
		);

		MultiLevelBomExcelModel parsedModel = new MultiLevelBomExcelModel(
				1, "1.1", "MAT-FG-001", "MAT-SFG-001", "飛控主板電路模組總成",
				"FC-PCB-ASSY-V3", "半成品", "PCS", new BigDecimal("1"), BigDecimal.ZERO,
				new BigDecimal("1"), new BigDecimal("8500"), new BigDecimal("8500"), "FC-ASM", "核心飛控組裝件"
		);
		when(importExcelUseCase.importBeanSheet(any(InputStream.class), isNull(), eq(MultiLevelBomExcelModel.class)))
				.thenReturn(new BeanSheetGottenResult<>("多階層BOM展開清單", List.of(parsedModel)));

		// When & Then
		mockMvc.perform(multipart("/api/v1/excel/material-bom/multi-level-bom/import").file(file))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.totalCount").value(1))
				.andExpect(jsonPath("$.sheetName").value("多階層BOM展開清單"))
				.andExpect(jsonPath("$.items[0].level").value(1))
				.andExpect(jsonPath("$.items[0].levelSequence").value("1.1"))
				.andExpect(jsonPath("$.items[0].specification").value("FC-PCB-ASSY-V3"))
				.andExpect(jsonPath("$.items[0].standardPrice").value(8500));

		verify(importExcelUseCase).importBeanSheet(any(InputStream.class), isNull(), eq(MultiLevelBomExcelModel.class));
	}

	/**
	 * Feature: 空檔案上傳異常處理
	 * Scenario: 上傳空內容之 Excel 檔案
	 * Given: 建立長度為 0 之空檔案
	 * When: 發送 POST multipart 請求
	 * Then: 拋出 IllegalArgumentException 錯誤
	 */
	@Test
	@DisplayName("異常：上傳空檔案時拋出錯誤回應")
	void shouldRejectEmptyFileUpload() throws Exception {
		// Given
		MockMultipartFile emptyFile = new MockMultipartFile(
				"file",
				"empty.xlsx",
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				new byte[0]
		);

		// When & Then
		mockMvc.perform(multipart("/api/v1/excel/material-bom/materials/import").file(emptyFile))
				.andExpect(status().isBadRequest());
	}

	/**
	 * Feature: 宣告式模型註冊表 (ExcelSheetModelRegistryPort) 智慧路由分流
	 * Scenario: 在 /bom-items/import 端點指定 sheetName 為多階層 BOM 工作表
	 * Given: 活頁簿包含多階層工作表，Registry 自動識別並分派為 MultiLevelBomExcelModel
	 * When: 發送 POST multipart 請求至 /bom-items/import 附帶 sheetName="多階層BOM展開清單"
	 * Then: 成功委派 ImportExcelUseCase 以 MultiLevelBomExcelModel 解析並封裝回傳
	 */
	@Test
	@DisplayName("場景：/bom-items/import 透過模型註冊表動態路由至 MultiLevelBomExcelModel")
	void shouldRouteToMultiLevelBomModelViaRegistry() throws Exception {
		// Given
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"bom.xlsx",
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				new byte[]{1, 2, 3}
		);

		MultiLevelBomExcelModel parsedModel = new MultiLevelBomExcelModel(
				1, "1.1", "MAT-FG-001", "MAT-SFG-001", "飛控主板電路模組總成",
				"FC-PCB-ASSY-V3", "半成品", "PCS", new BigDecimal("1"), BigDecimal.ZERO,
				new BigDecimal("1"), new BigDecimal("8500"), new BigDecimal("8500"), "FC-ASM", "核心飛控組裝件"
		);
		when(importExcelUseCase.importBeanSheet(any(InputStream.class), eq("多階層BOM展開清單"), eq(MultiLevelBomExcelModel.class)))
				.thenReturn(new BeanSheetGottenResult<>("多階層BOM展開清單", List.of(parsedModel)));

		// When & Then
		mockMvc.perform(multipart("/api/v1/excel/material-bom/bom-items/import")
						.file(file)
						.param("sheetName", "多階層BOM展開清單"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.totalCount").value(1))
				.andExpect(jsonPath("$.sheetName").value("多階層BOM展開清單"))
				.andExpect(jsonPath("$.multiLevelBom.items[0].childMaterialCode").value("MAT-SFG-001"));

		verify(importExcelUseCase).importBeanSheet(any(InputStream.class), eq("多階層BOM展開清單"), eq(MultiLevelBomExcelModel.class));
	}

	/**
	 * Feature: 宣告式模型註冊表 (ExcelSheetModelRegistryPort) 智慧路由分流至物料主檔
	 * Scenario: 在 /bom-items/import 端點指定 sheetName 為物料主檔工作表
	 * Given: 活頁簿包含物料主檔工作表，Registry 自動識別並分派為 MaterialExcelModel
	 * When: 發送 POST multipart 請求至 /bom-items/import 附帶 sheetName="物料主檔對照表"
	 * Then: 成功委派 ImportExcelUseCase 以 MaterialExcelModel 解析並封裝回傳
	 */
	@Test
	@DisplayName("場景：/bom-items/import 透過模型註冊表動態路由至 MaterialExcelModel")
	void shouldRouteToMaterialModelViaRegistryOnBomImport() throws Exception {
		// Given
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"bom.xlsx",
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				new byte[]{1, 2, 3}
		);

		MaterialExcelModel parsedModel = new MaterialExcelModel(
				"MAT-001", "高精度馬達", "2204-KV2300", "RAW_MATERIAL", "PCS", new BigDecimal("450.00"), "ACTIVE"
		);
		when(importExcelUseCase.importBeanSheet(any(InputStream.class), eq("物料主檔對照表"), eq(MaterialExcelModel.class)))
				.thenReturn(new BeanSheetGottenResult<>("物料主檔對照表", List.of(parsedModel)));

		// When & Then
		mockMvc.perform(multipart("/api/v1/excel/material-bom/bom-items/import")
						.file(file)
						.param("sheetName", "物料主檔對照表"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.totalCount").value(1))
				.andExpect(jsonPath("$.sheetName").value("物料主檔對照表"))
				.andExpect(jsonPath("$.materialMaster.items[0].materialCode").value("MAT-001"));

		verify(importExcelUseCase).importBeanSheet(any(InputStream.class), eq("物料主檔對照表"), eq(MaterialExcelModel.class));
	}
}

