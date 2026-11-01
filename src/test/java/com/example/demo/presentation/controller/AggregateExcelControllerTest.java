package com.example.demo.presentation.controller;

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

import com.example.demo.application.port.in.ExportExcelUseCase;
import com.example.demo.application.port.in.ImportExcelUseCase;
import com.example.demo.application.shared.model.BomItemExcelModel;
import com.example.demo.application.shared.model.MaterialExcelModel;
import com.example.demo.presentation.assembler.AggregateResourceAssembler;
import com.example.demo.presentation.exception.GlobalExceptionHandler;

/**
 * 聚合根宣告式 Excel 映射控制器測試 (AggregateExcelController Test)
 * <p>
 * 遵循 BDD 與 Gherkin 規範，驗證物料主檔 (Material) 與單階 BOM 配方清單 (BOM)
 * 聚合根對應之宣告式 Excel 匯出與解析端點。
 */
@DisplayName("AggregateExcelController 聚合根宣告式 Excel 控制器測試")
class AggregateExcelControllerTest {

	private MockMvc mockMvc;
	private ExportExcelUseCase exportExcelUseCase;
	private ImportExcelUseCase importExcelUseCase;

	@BeforeEach
	void setUp() {
		exportExcelUseCase = mock(ExportExcelUseCase.class);
		importExcelUseCase = mock(ImportExcelUseCase.class);
		AggregateResourceAssembler resourceAssembler = new AggregateResourceAssembler();

		AggregateExcelController controller = new AggregateExcelController(
				exportExcelUseCase,
				importExcelUseCase,
				resourceAssembler
		);

		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	// =========================================================================
	// 1. Material (物料聚合根) 測試案例
	// =========================================================================

	/**
	 * Feature: 物料主檔範例資料宣告式匯出 (Material GET)
	 * Scenario: 成功透過 GET 下載系統產製之物料主檔範例 Excel 檔案
	 * Given 模擬 ExportExcelUseCase 回傳 Excel 二進位陣列
	 * When 發送 GET /api/v1/excel/aggregate/materials/export 請求
	 * Then 應回傳 HTTP 200 與 Content-Disposition 附件標頭
	 */
	@Test
	@DisplayName("場景：成功透過 GET 下載物料主檔範例 Excel (Material)")
	void shouldExportSampleMaterialsSuccessfully() throws Exception {
		// Given
		byte[] mockBytes = "MATERIAL_EXCEL_BYTES".getBytes();
		when(exportExcelUseCase.exportBeans(any(), eq(MaterialExcelModel.class))).thenReturn(mockBytes);

		// When & Then
		mockMvc.perform(get("/api/v1/excel/aggregate/materials/export"))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				.andExpect(header().string("Content-Disposition", "form-data; name=\"attachment\"; filename=\"materials_sample.xlsx\""))
				.andExpect(content().bytes(mockBytes));

		verify(exportExcelUseCase).exportBeans(any(), eq(MaterialExcelModel.class));
	}

	/**
	 * Feature: 自訂物料資料宣告式匯出 (Material POST)
	 * Scenario: 提供自訂物料 JSON 資料並宣告式匯出為 Excel 檔案
	 * Given 傳入包含自訂物料項目之 JSON Payload
	 * When 發送 POST /api/v1/excel/aggregate/materials/export 請求
	 * Then 應調用 exportBeans 並回傳 Excel 二進位檔案
	 */
	@Test
	@DisplayName("場景：成功透過 POST 依自訂 JSON 匯出物料主檔 Excel (Material)")
	void shouldExportCustomMaterialsSuccessfully() throws Exception {
		// Given
		byte[] mockBytes = "CUSTOM_MATERIAL_EXCEL".getBytes();
		when(exportExcelUseCase.exportBeans(any(), eq(MaterialExcelModel.class), eq("高階物料主檔"))).thenReturn(mockBytes);

		String requestJson = """
				{
				  "sheetName": "高階物料主檔",
				  "materials": [
				    {
				      "materialCode": "MAT-FG-999",
				      "materialName": "8K 航太級空拍機",
				      "specification": "DRONE-8K-AERO",
				      "category": "FINISHED_GOODS",
				      "unit": "PCS",
				      "standardCost": 98000.00,
				      "status": "ACTIVE"
				    }
				  ]
				}
				""";

		// When & Then
		mockMvc.perform(post("/api/v1/excel/aggregate/materials/export")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestJson))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				.andExpect(content().bytes(mockBytes));

		verify(exportExcelUseCase).exportBeans(any(), eq(MaterialExcelModel.class), eq("高階物料主檔"));
	}

	/**
	 * Feature: 物料 Excel 檔案上傳解析 (Material Import POST)
	 * Scenario: 上傳物料 Excel 檔案並自動映射為 MaterialExcelModel 清單
	 * Given 建立 MockMultipartFile 與 Mock 解析模型清單
	 * When 發送 POST /api/v1/excel/aggregate/materials/import 請求
	 * Then 應回傳 HTTP 200、totalCount 為 1、包含物料料號與標準成本之 JSON 回應
	 */
	@Test
	@DisplayName("場景：成功上傳 Excel 並宣告式解析為 MaterialExcelModel 清單 (Material)")
	void shouldImportMaterialsSuccessfully() throws Exception {
		// Given
		MockMultipartFile file = new MockMultipartFile(
				"file", "materials.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				"FAKE_EXCEL_CONTENT".getBytes()
		);

		List<MaterialExcelModel> mockList = List.of(
				new MaterialExcelModel("MAT-FG-001", "4K 專業航拍無人機", "DRONE-4K-PRO", "FINISHED_GOODS", "PCS", new BigDecimal("35000.00"), "ACTIVE")
		);
		when(importExcelUseCase.importBeans(any(InputStream.class), eq(MaterialExcelModel.class))).thenReturn(mockList);

		// When & Then
		mockMvc.perform(multipart("/api/v1/excel/aggregate/materials/import").file(file))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalCount").value(1))
				.andExpect(jsonPath("$.sheetName").value("物料主檔清單"))
				.andExpect(jsonPath("$.items[0].materialCode").value("MAT-FG-001"))
				.andExpect(jsonPath("$.items[0].materialName").value("4K 專業航拍無人機"))
				.andExpect(jsonPath("$.items[0].specification").value("DRONE-4K-PRO"))
				.andExpect(jsonPath("$.items[0].category").value("FINISHED_GOODS"))
				.andExpect(jsonPath("$.items[0].unit").value("PCS"))
				.andExpect(jsonPath("$.items[0].standardCost").value(35000.00))
				.andExpect(jsonPath("$.items[0].status").value("ACTIVE"));

		verify(importExcelUseCase).importBeans(any(InputStream.class), eq(MaterialExcelModel.class));
	}

	// =========================================================================
	// 2. BOM (物料清單聚合根) 測試案例
	// =========================================================================

	/**
	 * Feature: 單階 BOM 配方清單範例資料宣告式匯出 (BOM GET)
	 * Scenario: 成功透過 GET 下載系統產製之單階 BOM 範例 Excel 檔案
	 * Given 模擬 ExportExcelUseCase 回傳 Excel 二進位資料
	 * When 發送 GET /api/v1/excel/aggregate/bom-items/export 請求
	 * Then 應回傳 HTTP 200 與 Content-Disposition 附件標頭
	 */
	@Test
	@DisplayName("場景：成功透過 GET 下載單階 BOM 配方範例 Excel (BOM)")
	void shouldExportSampleBomItemsSuccessfully() throws Exception {
		// Given
		byte[] mockBytes = "BOM_EXCEL_BYTES".getBytes();
		when(exportExcelUseCase.exportBeans(any(), eq(BomItemExcelModel.class))).thenReturn(mockBytes);

		// When & Then
		mockMvc.perform(get("/api/v1/excel/aggregate/bom-items/export"))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				.andExpect(header().string("Content-Disposition", "form-data; name=\"attachment\"; filename=\"bom_items_sample.xlsx\""))
				.andExpect(content().bytes(mockBytes));

		verify(exportExcelUseCase).exportBeans(any(), eq(BomItemExcelModel.class));
	}

	/**
	 * Feature: 自訂單階 BOM 資料宣告式匯出 (BOM POST)
	 * Scenario: 提供自訂 BOM 配方 JSON 資料並匯出為 Excel 檔案
	 * Given 傳入包含自訂 BOM 子項之 JSON Payload
	 * When 發送 POST /api/v1/excel/aggregate/bom-items/export 請求
	 * Then 應調用 exportBeans 並回傳 Excel 二進位檔案
	 */
	@Test
	@DisplayName("場景：成功透過 POST 依自訂 JSON 匯出單階 BOM 配方 Excel (BOM)")
	void shouldExportCustomBomItemsSuccessfully() throws Exception {
		// Given
		byte[] mockBytes = "CUSTOM_BOM_EXCEL".getBytes();
		when(exportExcelUseCase.exportBeans(any(), eq(BomItemExcelModel.class), eq("特製BOM配方"))).thenReturn(mockBytes);

		String requestJson = """
				{
				  "sheetName": "特製BOM配方",
				  "items": [
				    {
				      "parentMaterialCode": "MAT-FG-001",
				      "bomVersion": "2.0",
				      "itemSequence": 10,
				      "childMaterialCode": "MAT-SFG-999",
				      "childMaterialName": "次世代 AI 視覺晶片模組",
				      "quantity": 1.0000,
				      "unit": "SET",
				      "scrapRate": 0.00,
				      "positionCode": "AI-CHIP-01",
				      "remark": "高算力晶片"
				    }
				  ]
				}
				""";

		// When & Then
		mockMvc.perform(post("/api/v1/excel/aggregate/bom-items/export")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestJson))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				.andExpect(content().bytes(mockBytes));

		verify(exportExcelUseCase).exportBeans(any(), eq(BomItemExcelModel.class), eq("特製BOM配方"));
	}

	/**
	 * Feature: 單階 BOM 配方 Excel 檔案上傳解析 (BOM Import POST)
	 * Scenario: 上傳 BOM Excel 檔案並自動映射為 BomItemExcelModel 清單
	 * Given 建立 MockMultipartFile 與 Mock 解析模型清單
	 * When 發送 POST /api/v1/excel/aggregate/bom-items/import 請求
	 * Then 應回傳 HTTP 200、totalCount 為 1、包含母件料號與組裝用量之 JSON 回應
	 */
	@Test
	@DisplayName("場景：成功上傳 Excel 並宣告式解析為 BomItemExcelModel 清單 (BOM)")
	void shouldImportBomItemsSuccessfully() throws Exception {
		// Given
		MockMultipartFile file = new MockMultipartFile(
				"file", "bom_items.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				"FAKE_EXCEL_CONTENT".getBytes()
		);

		List<BomItemExcelModel> mockList = List.of(
				new BomItemExcelModel("MAT-FG-001", "1.0", 10, "MAT-SFG-001", "4K 雲台相機模組", new BigDecimal("1.0000"), "SET", new BigDecimal("0.00"), "CAM-01", "雲台相機")
		);
		when(importExcelUseCase.importBeans(any(InputStream.class), eq(BomItemExcelModel.class))).thenReturn(mockList);

		// When & Then
		mockMvc.perform(multipart("/api/v1/excel/aggregate/bom-items/import").file(file))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalCount").value(1))
				.andExpect(jsonPath("$.sheetName").value("單階BOM清單"))
				.andExpect(jsonPath("$.items[0].parentMaterialCode").value("MAT-FG-001"))
				.andExpect(jsonPath("$.items[0].bomVersion").value("1.0"))
				.andExpect(jsonPath("$.items[0].itemSequence").value(10))
				.andExpect(jsonPath("$.items[0].childMaterialCode").value("MAT-SFG-001"))
				.andExpect(jsonPath("$.items[0].childMaterialName").value("4K 雲台相機模組"))
				.andExpect(jsonPath("$.items[0].quantity").value(1.0000))
				.andExpect(jsonPath("$.items[0].unit").value("SET"))
				.andExpect(jsonPath("$.items[0].positionCode").value("CAM-01"));

		verify(importExcelUseCase).importBeans(any(InputStream.class), eq(BomItemExcelModel.class));
	}

	/**
	 * Feature: 上傳空檔案異常攔截校驗
	 * Scenario: 使用者上傳空檔案時應拒絕請求並拋出 IllegalArgumentException
	 * Given 建立空檔案 MockMultipartFile
	 * When 發送 POST /api/v1/excel/aggregate/materials/import 請求
	 * Then 應由 GlobalExceptionHandler 攔截並回傳 HTTP 400 Bad Request
	 */
	@Test
	@DisplayName("場景：上傳空檔案時應拒絕並回傳 400 錯誤")
	void shouldRejectEmptyFileOnImport() throws Exception {
		// Given
		MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.xlsx", "text/plain", new byte[0]);

		// When & Then
		mockMvc.perform(multipart("/api/v1/excel/aggregate/materials/import").file(emptyFile))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("上傳之 Excel 檔案不能為空"));
	}
}

