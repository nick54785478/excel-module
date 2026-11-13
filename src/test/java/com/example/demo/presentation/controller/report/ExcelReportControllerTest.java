package com.example.demo.presentation.controller.report;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.demo.application.port.in.report.ExportFinancialReportUseCase;
import com.example.demo.application.shared.command.ExportBomCostAnalysisCommand;
import com.example.demo.presentation.assembler.ExcelReportResourceAssembler;
import com.example.demo.presentation.exception.GlobalExceptionHandler;

/**
 * 企業級進階報表 REST 控制器測試 (ExcelReportController Test)
 * <p>
 * 遵循 BDD 與 Gherkin 規範，驗證維度五「企業級報表樣式、公式與安全防護」之 HTTP API 端點：
 * 包含一鍵 GET 採購損益月報下載、宣告式範本下載與自訂樣式 POST 匯出。
 */
@DisplayName("ExcelReportController 企業級報表控制器測試")
class ExcelReportControllerTest {

	private MockMvc mockMvc;
	private ExportFinancialReportUseCase exportFinancialReportUseCase;

	@BeforeEach
	void setUp() {
		exportFinancialReportUseCase = mock(ExportFinancialReportUseCase.class);
		ExcelReportResourceAssembler resourceAssembler = new ExcelReportResourceAssembler();

		ExcelReportController controller = new ExcelReportController(
				exportFinancialReportUseCase,
				resourceAssembler
		);

		this.mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	/**
	 * Feature: 一鍵匯出物料採購損益分析月報
	 * Scenario: 前端透過 HTTP GET 請求直接下載 Excel 二進位報表
	 * Given: 帶入自訂標題 "無人機物料採購分析"、密碼 "Pass123"、機密等級 "內部流轉"
	 * When: 發送 GET /api/v1/excel/reports/procurement-analysis
	 * Then: 回應 200 OK，包含 application/vnd.openxmlformats-officedocument.spreadsheetml.sheet 標頭與附件下載名稱
	 */
	@Test
	@DisplayName("情境：成功透過 GET 一鍵匯出物料採購損益分析月報 (預設資料)")
	void shouldExportProcurementAnalysisReportSuccessfully() throws Exception {
		// Given
		byte[] mockBytes = new byte[]{0x50, 0x4B, 0x03, 0x04}; // ZIP header
		when(exportFinancialReportUseCase.exportProcurementAnalysisReport(anyString(), anyString(), anyString()))
				.thenReturn(mockBytes);

		// When & Then
		mockMvc.perform(get("/api/v1/excel/reports/procurement-analysis")
						.param("reportTitle", "無人機物料採購分析")
						.param("password", "Pass123")
						.param("classification", "內部流轉"))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				.andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("procurement_analysis_report.xlsx")))
				.andExpect(content().bytes(mockBytes));

		verify(exportFinancialReportUseCase).exportProcurementAnalysisReport(
				eq("無人機物料採購分析"),
				eq("Pass123"),
				eq("內部流轉")
		);
	}

	/**
	 * Feature: 物料採購損益分析月報 POST 請求傳入自訂資料
	 * Scenario: 前端透過 HTTP POST 傳入包含自訂物料清單的 JSON Request 產生 Excel 報表
	 * When: 發送 POST /api/v1/excel/reports/procurement-analysis
	 * Then: 回應 200 OK 且正確呼叫應用層 UseCase
	 */
	@Test
	@DisplayName("情境：成功透過 POST 傳入自訂物料清單產生採購損益月報")
	void shouldExportProcurementAnalysisReportWithCustomItemsSuccessfully() throws Exception {
		// Given
		byte[] mockBytes = new byte[]{0x50, 0x4B, 0x03, 0x04};
		when(exportFinancialReportUseCase.exportProcurementAnalysisReport(any(com.example.demo.application.shared.command.ExportProcurementAnalysisCommand.class)))
				.thenReturn(mockBytes);

		String jsonPayload = """
				{
				  "reportTitle": "自訂無人機採購月報",
				  "password": "Pass789",
				  "classification": "絕密",
				  "items": [
				    {
				      "itemCode": "MAT-001",
				      "itemName": "高轉速馬達",
				      "quantity": 25,
				      "unitPrice": 500.00,
				      "estimatedPrice": 850.00
				    }
				  ]
				}
				""";

		// When & Then
		mockMvc.perform(post("/api/v1/excel/reports/procurement-analysis")
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonPayload))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				.andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(
						java.net.URLEncoder.encode("自訂無人機採購月報", java.nio.charset.StandardCharsets.UTF_8))))
				.andExpect(content().bytes(mockBytes));

		verify(exportFinancialReportUseCase).exportProcurementAnalysisReport(
				any(com.example.demo.application.shared.command.ExportProcurementAnalysisCommand.class)
		);
	}

	/**
	 * Feature: 宣告式範本一鍵下載
	 * Scenario: 透過 HTTP GET 請求下載純宣告式註解驅動之 Excel 範本
	 * When: 發送 GET /api/v1/excel/reports/declarative-sample
	 * Then: 回應 200 OK 且包含 declarative_procurement_report.xlsx 附件標頭
	 */
	@Test
	@DisplayName("情境：成功透過 GET 下載宣告式模型註解驅動範本")
	void shouldExportDeclarativeSampleSuccessfully() throws Exception {
		// Given
		byte[] mockBytes = new byte[]{0x50, 0x4B, 0x03, 0x04};
		when(exportFinancialReportUseCase.exportDeclarativeSample()).thenReturn(mockBytes);

		// When & Then
		mockMvc.perform(get("/api/v1/excel/reports/declarative-sample"))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				.andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("declarative_procurement_report.xlsx")))
				.andExpect(content().bytes(mockBytes));
	}

	/**
	 * Feature: 自訂進階樣式與防護報表匯出
	 * Scenario: 前端透過 HTTP POST 傳入包含自訂表頭、資料、公式與合併區間的 JSON 請求
	 * When: 發送 POST /api/v1/excel/reports/custom-styled
	 * Then: 回應 200 OK 並串流回傳產出之 Excel 檔案
	 */
	@Test
	@DisplayName("情境：成功透過 POST 傳入 JSON 匯出客製化進階報表")
	void shouldExportCustomReportSuccessfully() throws Exception {
		// Given
		byte[] mockBytes = new byte[]{0x50, 0x4B, 0x03, 0x04};
		when(exportFinancialReportUseCase.exportCustomReport(any())).thenReturn(mockBytes);

		String jsonPayload = """
				{
				  "sheetName": "客製季度損益表",
				  "headers": ["項目", "第一季", "第二季", "合計"],
				  "rows": [
				    ["營收", 1000, 2000, "=B2+C2"],
				    ["合計", null, null, "=D2"]
				  ],
				  "mergedRegions": [
				    { "firstRow": 2, "lastRow": 2, "firstCol": 0, "lastCol": 2 }
				  ],
				  "cellFormulas": {
				    "D3": "D2"
				  },
				  "password": "CustomPassword999",
				  "reportTitle": "2026年上半年損益表",
				  "classification": "絕密"
				}
				""";

		// When & Then
		mockMvc.perform(post("/api/v1/excel/reports/custom-styled")
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonPayload))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				.andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(
						java.net.URLEncoder.encode("客製季度損益表", java.nio.charset.StandardCharsets.UTF_8))))
				.andExpect(content().bytes(mockBytes));
	}

	/**
	 * Feature: 自訂進階樣式報表匯出容錯防禦 (Swagger dummy 佔位符)
	 * Scenario: 前端透過 Swagger UI 預設發送包含 "additionalProp1": "string" 佔位屬性的請求
	 * Given: cellFormulas 包含 "additionalProp1": "string"
	 * When: 發送 POST /api/v1/excel/reports/custom-styled
	 * Then: 成功過濾佔位符並回應 200 OK
	 */
	@Test
	@DisplayName("情境：Swagger UI 自動填入 additionalProp1 佔位屬性時自動容錯過濾並成功匯出")
	void shouldExportCustomReportWhenSwaggerDummyAdditionalPropPassed() throws Exception {
		// Given
		byte[] mockBytes = new byte[]{0x50, 0x4B, 0x03, 0x04};
		when(exportFinancialReportUseCase.exportCustomReport(any())).thenReturn(mockBytes);

		String jsonPayload = """
				{
				  "sheetName": "Swagger測試表",
				  "headers": ["物料名稱", "標準單價"],
				  "rows": [
				    ["馬達", 100]
				  ],
				  "cellFormulas": {
				    "additionalProp1": "string",
				    "additionalProp2": "string"
				  }
				}
				""";

		// When & Then
		mockMvc.perform(post("/api/v1/excel/reports/custom-styled")
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonPayload))
				.andExpect(status().isOk())
				.andExpect(content().bytes(mockBytes));
	}

	/**
	 * Feature: 自訂進階樣式報表座標格式防禦
	 * Scenario: 前端傳入格式不合法的儲存格座標 (非 Excel 標準座標)
	 * Given: cellFormulas 包含 "INVALID_CELL_COORD"
	 * When: 發送 POST /api/v1/excel/reports/custom-styled
	 * Then: 回應 400 Bad Request
	 */
	@Test
	@DisplayName("情境：傳入非法儲存格座標時拋出 400 錯誤")
	void shouldRejectCustomReportWhenInvalidCellCoordinatePassed() throws Exception {
		String jsonPayload = """
				{
				  "sheetName": "異常測試表",
				  "headers": ["物料名稱", "標準單價"],
				  "rows": [
				    ["馬達", 100]
				  ],
				  "cellFormulas": {
				    "INVALID_COORDINATE": "SUM(A1:A2)"
				  }
				}
				""";

		// When & Then
		mockMvc.perform(post("/api/v1/excel/reports/custom-styled")
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonPayload))
				.andExpect(status().isBadRequest());
	}

	/**
	 * Feature: 一鍵匯出 BOM 結構配方與標準成本卷算分析報表
	 * Scenario: 前端透過 HTTP GET 請求直接下載指定母件料號的 BOM 成本分析 Excel 二進位報表
	 * Given: 帶入母件料號 "MAT-FG-001"、自訂標題 "無人機成本分析表"、密碼 "BomPass123"、機密等級 "內部機密"
	 * When: 發送 GET /api/v1/excel/reports/bom-cost-analysis
	 * Then: 回應 200 OK，包含 application/vnd.openxmlformats-officedocument.spreadsheetml.sheet 標頭與附件下載名稱
	 */
	@Test
	@DisplayName("情境：成功透過 GET 一鍵匯出產品結構配方與成本卷算分析報表")
	void shouldExportBomCostAnalysisReportViaGetSuccessfully() throws Exception {
		// Given
		byte[] mockBytes = new byte[]{0x50, 0x4B, 0x03, 0x04};
		when(exportFinancialReportUseCase.exportBomCostAnalysisReport(anyString(), anyString(), anyString(), anyString()))
				.thenReturn(mockBytes);

		// When & Then
		mockMvc.perform(get("/api/v1/excel/reports/bom-cost-analysis")
						.param("materialCode", "MAT-FG-001")
						.param("reportTitle", "無人機成本分析表")
						.param("password", "BomPass123")
						.param("classification", "內部機密"))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				.andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("bom_cost_analysis_MAT-FG-001.xlsx")))
				.andExpect(content().bytes(mockBytes));

		verify(exportFinancialReportUseCase).exportBomCostAnalysisReport(
				eq("MAT-FG-001"),
				eq("無人機成本分析表"),
				eq("BomPass123"),
				eq("內部機密")
		);
	}

	/**
	 * Feature: BOM 成本分析報表 POST 請求傳入自訂資料
	 * Scenario: 前端透過 HTTP POST 傳入包含母件料號與自訂子件清單的 JSON Request 產生 Excel 報表
	 * When: 發送 POST /api/v1/excel/reports/bom-cost-analysis
	 * Then: 回應 200 OK 且正確呼叫應用層 UseCase
	 */
	@Test
	@DisplayName("情境：成功透過 POST 傳入自訂子件清單產生 BOM 成本分析報表")
	void shouldExportBomCostAnalysisReportViaPostSuccessfully() throws Exception {
		// Given
		byte[] mockBytes = new byte[]{0x50, 0x4B, 0x03, 0x04};
		when(exportFinancialReportUseCase.exportBomCostAnalysisReport(any(ExportBomCostAnalysisCommand.class)))
				.thenReturn(mockBytes);

		String jsonPayload = """
				{
				  "parentMaterialCode": "MAT-FG-001",
				  "reportTitle": "自訂無人機BOM成本分析",
				  "password": "SecurePass2026",
				  "classification": "機密等級・審計專用",
				  "items": [
				    {
				      "level": 1,
				      "itemSequence": 10,
				      "childMaterialCode": "MAT-SFG-001",
				      "childMaterialName": "飛控主板電路總成",
				      "specification": "STM32H743 Cortex-M7",
				      "category": "半成品",
				      "unit": "套/組",
				      "baseQuantity": 1.00,
				      "scrapRate": 0.01,
				      "standardCost": 4800.00,
				      "positionCode": "FC-ASM",
				      "remark": "核心飛控模組"
				    }
				  ]
				}
				""";

		// When & Then
		mockMvc.perform(post("/api/v1/excel/reports/bom-cost-analysis")
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonPayload))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				.andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("xlsx")))
				.andExpect(content().bytes(mockBytes));

		verify(exportFinancialReportUseCase).exportBomCostAnalysisReport(any(ExportBomCostAnalysisCommand.class));
	}
}

