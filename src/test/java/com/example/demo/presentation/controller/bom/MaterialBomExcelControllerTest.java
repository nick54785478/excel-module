package com.example.demo.presentation.controller.bom;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import com.example.demo.application.port.in.core.ExportExcelUseCase;
import com.example.demo.application.port.in.bom.ExportMaterialBomUseCase;
import com.example.demo.application.port.in.core.ImportExcelUseCase;
import com.example.demo.application.shared.model.BomItemExcelModel;
import com.example.demo.application.shared.model.MaterialExcelModel;
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
	private ExportExcelUseCase exportExcelUseCase;
	private ImportExcelUseCase importExcelUseCase;

	@BeforeEach
	void setUp() {
		exportMaterialBomUseCase = mock(ExportMaterialBomUseCase.class);
		exportExcelUseCase = mock(ExportExcelUseCase.class);
		importExcelUseCase = mock(ImportExcelUseCase.class);
		MaterialBomResourceAssembler resourceAssembler = new MaterialBomResourceAssembler();

		MaterialBomExcelController controller = new MaterialBomExcelController(
				exportMaterialBomUseCase,
				exportExcelUseCase,
				importExcelUseCase,
				resourceAssembler
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
	 * Feature: 物料主檔 Excel 上傳解析
	 * Scenario: 上傳物料 Excel 檔案並宣告式解析為 MaterialExcelModel 清單
	 * Given: 模擬上傳包含 1 筆物料之 Excel 檔案
	 * When: 發送 POST multipart 請求
	 * Then: 回應 200 OK 且包含正確之 MaterialImportedResource JSON 結構
	 */
	@Test
	@DisplayName("場景：POST 上傳檔案宣告式解析為 MaterialImportedResource")
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
		when(importExcelUseCase.importBeans(any(InputStream.class), eq(MaterialExcelModel.class)))
				.thenReturn(List.of(parsedModel));

		// When & Then
		mockMvc.perform(multipart("/api/v1/excel/material-bom/materials/import").file(file))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.totalCount").value(1))
				.andExpect(jsonPath("$.sheetName").value("物料主檔清單"))
				.andExpect(jsonPath("$.items[0].materialCode").value("MAT-001"))
				.andExpect(jsonPath("$.items[0].materialName").value("高精度馬達"));

		verify(importExcelUseCase).importBeans(any(InputStream.class), eq(MaterialExcelModel.class));
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
	 * Feature: 單階 BOM 配方 Excel 上傳解析
	 * Scenario: 上傳 BOM Excel 檔案並宣告式解析為 BomItemExcelModel 清單
	 * Given: 模擬上傳包含 1 筆 BOM 子項之 Excel 檔案
	 * When: 發送 POST multipart 請求
	 * Then: 回應 200 OK 且包含正確之 BomItemImportedResource JSON 結構
	 */
	@Test
	@DisplayName("場景：POST 上傳檔案宣告式解析為 BomItemImportedResource")
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
		when(importExcelUseCase.importBeans(any(InputStream.class), eq(BomItemExcelModel.class)))
				.thenReturn(List.of(parsedModel));

		// When & Then
		mockMvc.perform(multipart("/api/v1/excel/material-bom/bom-items/import").file(file))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.totalCount").value(1))
				.andExpect(jsonPath("$.sheetName").value("單階BOM清單"))
				.andExpect(jsonPath("$.items[0].parentMaterialCode").value("MAT-FG-001"))
				.andExpect(jsonPath("$.items[0].childMaterialCode").value("MAT-RM-001"));

		verify(importExcelUseCase).importBeans(any(InputStream.class), eq(BomItemExcelModel.class));
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
}

