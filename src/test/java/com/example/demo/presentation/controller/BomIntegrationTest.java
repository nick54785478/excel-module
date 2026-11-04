package com.example.demo.presentation.controller;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.example.demo.application.port.out.ExcelReaderPort;
import com.example.demo.application.port.out.ExcelWriterPort;
import com.example.demo.application.shared.dto.ExcelGottenResult;
import com.example.demo.application.shared.dto.ExcelSheetGottenResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 物料清單 (BOM) Excel 下載 API 端到端整合測試 (BOM Excel Download Integration Test)
 * <p>
 * 啟動完整 Spring Boot 測試環境（使用 H2 模擬 MySQL），載入資料庫初始化 SQL (200 筆物料 + 25 筆 BOM 配方)，
 * 透過 MockMvc 發送真實 HTTP 請求，並使用 {@link ExcelReaderPort} 解析回傳之 Excel 二進位串流，
 * 驗證多階展開、單階平鋪與物料對照表資料內容之完整性。
 * <p>
 * 遵循 BDD / Gherkin (Feature, Scenario, Given-When-Then) 測試語法。
 */
@SpringBootTest
@DisplayName("BOM 下載 API 端到端整合測試")
class BomIntegrationTest {

	@Autowired
	private WebApplicationContext webApplicationContext;

	@Autowired
	private ExcelReaderPort excelReaderPort;

	@Autowired
	private ExcelWriterPort excelWriterPort;

	@Autowired
	private com.example.demo.infrastructure.persistence.task.repository.ExcelTaskJpaRepository excelTaskJpaRepository;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
	}

	/**
	 * Feature: 真實資料庫環境下下載 MAT-FG-001 多階層 BOM Excel
	 * Scenario: 呼叫 GET /api/v1/bom/excel?parentMaterialCode=MAT-FG-001 下載 Excel 檔案
	 * Given: 資料庫已預載 200 筆物料與 25 筆 BOM 結構
	 * When: 發送 GET /api/v1/bom/excel?parentMaterialCode=MAT-FG-001
	 * Then: 回應 200 OK，產製之 Excel 包含 3 個工作表，且多階層展開清單首筆為 MAT-FG-001
	 */
	@Test
	@DisplayName("整合：GET /api/v1/bom/excel?parentMaterialCode=MAT-FG-001 產製並解析多階層 BOM Excel")
	void shouldDownloadAndParseBomExcelForMatFg001() throws Exception {
		// When
		MvcResult mvcResult = mockMvc.perform(get("/api/v1/bom/excel")
						.param("parentMaterialCode", "MAT-FG-001"))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				.andExpect(header().string("Content-Disposition", "attachment; filename*=UTF-8''BOM_MAT-FG-001.xlsx"))
				.andReturn();

		byte[] excelBytes = mvcResult.getResponse().getContentAsByteArray();
		assertThat(excelBytes).isNotEmpty();

		// Then - 回讀驗證 Excel 結構
		ExcelGottenResult gotten = excelReaderPort.read(excelBytes);
		assertThat(gotten.getSheets()).hasSize(3);

		ExcelSheetGottenResult sheet1 = gotten.getSheets().get(0);
		assertThat(sheet1.getSheetName()).isEqualTo("多階層BOM展開清單");
		assertThat(sheet1.getHeaders()).contains("層級階碼", "物料編號", "物料名稱", "用量", "累計總成本 (TWD)");
		assertThat(sheet1.getRows()).isNotEmpty();
		// 首列應為 Level 0: MAT-FG-001
		assertThat(sheet1.getDataRowsAsMaps().get(0).get("層級階碼")).isEqualTo("0");
		assertThat(sheet1.getDataRowsAsMaps().get(0).get("物料編號")).isEqualTo("MAT-FG-001");

		ExcelSheetGottenResult sheet2 = gotten.getSheets().get(1);
		assertThat(sheet2.getSheetName()).isEqualTo("單階BOM匯入清單");
		assertThat(sheet2.getHeaders()).contains("父階物料編號", "BOM版本號", "子件物料編號", "組裝用量");
		assertThat(sheet2.getRows()).isNotEmpty();

		ExcelSheetGottenResult sheet3 = gotten.getSheets().get(2);
		assertThat(sheet3.getSheetName()).isEqualTo("物料主檔對照表");
		assertThat(sheet3.getHeaders()).contains("物料編號", "物料名稱", "物料類別", "標準進價成本");
		// 驗證 2000 筆測試物料皆列於物料主檔對照表
		assertThat(sheet3.getRows()).hasSize(2000);
	}

	/**
	 * Feature: 下載全系統所有產品 BOM Excel 清單
	 * Scenario: 未指定料號時呼叫 GET /api/v1/bom/excel 下載全部 BOM 清單
	 * Given: 請求參數中不包含 parentMaterialCode
	 * When: 發送 GET /api/v1/bom/excel
	 * Then: 回應 200 OK，檔名為 BOM_LIST_ALL.xlsx，產製所有 25 筆 BOM 配方
	 */
	@Test
	@DisplayName("整合：GET /api/v1/bom/excel 未指定料號時下載系統所有 BOM 清單")
	void shouldDownloadAllBomsExcel() throws Exception {
		// When
		MvcResult mvcResult = mockMvc.perform(get("/api/v1/bom/excel"))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Disposition", "attachment; filename*=UTF-8''BOM_LIST_ALL.xlsx"))
				.andReturn();

		byte[] excelBytes = mvcResult.getResponse().getContentAsByteArray();
		assertThat(excelBytes).isNotEmpty();

		// Then - 解析單階工作表確認包含所有 BOM 項目
		ExcelGottenResult gotten = excelReaderPort.read(excelBytes);
		ExcelSheetGottenResult singleLevelSheet = gotten.getSheets().stream()
				.filter(s -> "單階BOM匯入清單".equals(s.getSheetName()))
				.findFirst()
				.orElseThrow();

		// 測試資料集包含 75 筆 BOM Items
		assertThat(singleLevelSheet.getRows()).hasSizeGreaterThanOrEqualTo(75);
	}

	/**
	 * Feature: POST 結構化條件下載 BOM Excel
	 * Scenario: 透過 POST /api/v1/bom/excel/export 僅匯出單階工作表
	 * Given: JSON 載體指定 includeMultiLevel=false, includeMaterialMaster=false
	 * When: 發送 POST 請求
	 * Then: 回應 200 OK，且生成的 Excel 僅有 1 個單階工作表
	 */
	@Test
	@DisplayName("整合：POST /api/v1/bom/excel/export 依選項只匯出單階 BOM 工作表")
	void shouldExportOnlySingleLevelSheetByPost() throws Exception {
		String requestBody = """
				{
					"parentMaterialCode": "MAT-FG-001",
					"version": "V1.0",
					"includeMultiLevel": false,
					"includeSingleLevel": true,
					"includeMaterialMaster": false
				}
				""";

		MvcResult mvcResult = mockMvc.perform(post("/api/v1/bom/excel/export")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Disposition", "attachment; filename*=UTF-8''BOM_MAT-FG-001_V1.0.xlsx"))
				.andReturn();

		byte[] excelBytes = mvcResult.getResponse().getContentAsByteArray();
		ExcelGottenResult gotten = excelReaderPort.read(excelBytes);
		assertThat(gotten.getSheets()).hasSize(1);
		assertThat(gotten.getSheets().get(0).getSheetName()).isEqualTo("單階BOM匯入清單");

		// 驗證批次匯出亦成功觸發 @TrackExcelTask 並於 t_excel_task 建立記錄
		assertThat(excelTaskJpaRepository.count()).isGreaterThan(0);
	}

	/**
	 * Feature: 查詢不存在之料號返回 404
	 * Scenario: 傳入 NOT_EXIST_MATERIAL
	 * When: 發送 GET /api/v1/bom/excel?parentMaterialCode=NOT_EXIST_MATERIAL
	 * Then: 回應 404 Not Found
	 */
	@Test
	@DisplayName("整合：GET /api/v1/bom/excel 查無母件料號時返回 404 Not Found")
	void shouldReturn404WhenMaterialCodeNotFound() throws Exception {
		mockMvc.perform(get("/api/v1/bom/excel")
						.param("parentMaterialCode", "NOT_EXIST_MATERIAL"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.message").value("查無指定料號之物料主檔: NOT_EXIST_MATERIAL"));
	}

	/**
	 * Feature: 真實資料庫大數據串流下載 BOM Excel
	 * Scenario: 透過 GET /api/v1/bom/excel/stream 串流下載 MAT-FG-001 BOM Excel
	 * Given: 資料庫載入 200 筆物料與 25 筆 BOM
	 * When: 發送 GET /api/v1/bom/excel/stream?parentMaterialCode=MAT-FG-001&flushInterval=10
	 * Then: 回應 200 OK，使用 FastExcelReader 解析串流結果驗證工作表與資料完整性
	 */
	@Test
	@DisplayName("整合：GET /api/v1/bom/excel/stream 真實大數據串流下載並解析 Excel")
	void shouldStreamDownloadAndParseBomExcel() throws Exception {
		MvcResult mvcResult = mockMvc.perform(get("/api/v1/bom/excel/stream")
						.param("parentMaterialCode", "MAT-FG-001")
						.param("flushInterval", "10"))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				.andExpect(header().string("Content-Disposition", "attachment; filename*=UTF-8''BOM_MAT-FG-001.xlsx"))
				.andReturn();

		byte[] excelBytes = mvcResult.getResponse().getContentAsByteArray();
		assertThat(excelBytes).isNotEmpty();

		ExcelGottenResult gotten = excelReaderPort.read(excelBytes);
		assertThat(gotten.getSheets()).hasSize(3);
		assertThat(gotten.getSheets().get(0).getSheetName()).isEqualTo("多階層BOM展開清單");
		assertThat(gotten.getSheets().get(0).getRows()).isNotEmpty();

		// 驗證 ExcelTaskTrackingAspect 成功攔截並於資料表 t_excel_task 建立持久化長任務記錄
		assertThat(excelTaskJpaRepository.count()).isGreaterThan(0);
	}

	/**
	 * Feature: 真實資料庫大數據 POST 串流下載 BOM Excel
	 * Scenario: 透過 POST /api/v1/bom/excel/stream 串流下載包含單階與多階之 BOM Excel
	 * Given: 資料庫載入 200 筆物料與 25 筆 BOM
	 * When: 發送 POST /api/v1/bom/excel/stream 帶入 JSON 請求體
	 * Then: 回應 200 OK，驗證 Excel 二進位串流完整生成
	 */
	@Test
	@DisplayName("整合：POST /api/v1/bom/excel/stream 真實大數據結構化串流下載")
	void shouldPostStreamDownloadBomExcel() throws Exception {
		String requestJson = """
				{
					"parentMaterialCode": "MAT-FG-001",
					"version": "1.0",
					"includeMultiLevel": true,
					"includeSingleLevel": true,
					"includeMaterialMaster": false,
					"flushInterval": 10
				}
				""";

		MvcResult mvcResult = mockMvc.perform(post("/api/v1/bom/excel/stream")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestJson))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				.andExpect(header().string("Content-Disposition", "attachment; filename*=UTF-8''BOM_MAT-FG-001_1.0.xlsx"))
				.andReturn();

		byte[] excelBytes = mvcResult.getResponse().getContentAsByteArray();
		assertThat(excelBytes).isNotEmpty();

		ExcelGottenResult gotten = excelReaderPort.read(excelBytes);
		assertThat(gotten.getSheets()).hasSize(2);
		assertThat(gotten.getSheets().get(0).getSheetName()).isEqualTo("多階層BOM展開清單");
		assertThat(gotten.getSheets().get(1).getSheetName()).isEqualTo("單階BOM匯入清單");

		// 驗證結構化 POST 串流匯出成功觸發 @TrackExcelTask 並寫入 t_excel_task
		assertThat(excelTaskJpaRepository.count()).isGreaterThan(0);
	}
}
