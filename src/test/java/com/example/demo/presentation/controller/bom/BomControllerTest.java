package com.example.demo.presentation.controller.bom;

import java.io.OutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.demo.application.port.in.bom.ExportBomUseCase;
import com.example.demo.application.port.in.bom.QueryBomUseCase;
import com.example.demo.application.port.in.bom.StreamExportBomUseCase;
import com.example.demo.application.shared.command.ExportBomCommand;
import com.example.demo.application.shared.dto.BomExcelGottenResult;
import com.example.demo.domain.material.exception.MaterialNotFoundException;
import com.example.demo.presentation.assembler.BomResourceAssembler;
import com.example.demo.presentation.exception.GlobalExceptionHandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 物料清單控制器表現層單元測試 (BomController Test)
 * <p>
 * 針對 {@link BomController} 進行 HTTP GET (Query) 與 POST (Command) 介面契約、OpenAPI 參數轉換與例外處理之測試。
 * 遵循 BDD / Gherkin (Feature, Scenario, Given-When-Then) 測試註解規範。
 */
@DisplayName("BomController 物料清單 REST 控制器測試")
class BomControllerTest {

	private MockMvc mockMvc;
	private QueryBomUseCase queryBomUseCase;
	private ExportBomUseCase exportBomUseCase;
	private StreamExportBomUseCase streamExportBomUseCase;

	@BeforeEach
	void setUp() {
		this.queryBomUseCase = mock(QueryBomUseCase.class);
		this.exportBomUseCase = mock(ExportBomUseCase.class);
		this.streamExportBomUseCase = mock(StreamExportBomUseCase.class);
		BomResourceAssembler resourceAssembler = new BomResourceAssembler();

		BomController controller = new BomController(queryBomUseCase, exportBomUseCase, streamExportBomUseCase, resourceAssembler);

		this.mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	/**
	 * Feature: HTTP GET 下載指定母件之 BOM Excel 檔案 (查詢端)
	 * Scenario: 前端傳入 parentMaterialCode 與 version 查詢參數，獲取二進位 Excel 串流
	 * Given: 應用層模擬回傳 BOM_MAT-FG-001_1.0.xlsx 之位元組結果
	 * When: 發送 GET /api/v1/bom/excel?parentMaterialCode=MAT-FG-001&version=1.0
	 * Then: 回應 200 OK，包含正確的 Content-Type 與 Content-Disposition attachment 檔名
	 */
	@Test
	@DisplayName("場景：GET /api/v1/bom/excel 指定母件料號成功下載 Excel 檔案")
	void shouldDownloadBomExcelByGetSuccessfully() throws Exception {
		// Given
		byte[] mockBytes = "BOM_EXCEL_STREAM_BYTES".getBytes();
		BomExcelGottenResult gottenResult = new BomExcelGottenResult(
				mockBytes,
				"BOM_MAT-FG-001_1.0.xlsx",
				3,
				25
		);
		when(queryBomUseCase.queryBom(any(ExportBomCommand.class))).thenReturn(gottenResult);

		// When & Then
		mockMvc.perform(get("/api/v1/bom/excel")
						.param("parentMaterialCode", "MAT-FG-001")
						.param("version", "1.0")
						.param("includeMultiLevel", "true")
						.param("includeSingleLevel", "true")
						.param("includeMaterialMaster", "true"))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				.andExpect(header().string("Content-Disposition", "attachment; filename*=UTF-8''BOM_MAT-FG-001_1.0.xlsx"))
				.andExpect(content().bytes(mockBytes));

		verify(queryBomUseCase).queryBom(any(ExportBomCommand.class));
	}

	/**
	 * Feature: HTTP GET 查無母件料號時的錯誤回應 (查詢端)
	 * Scenario: 前端傳入不存在之料號，應用層拋出 MaterialNotFoundException
	 * Given: queryBomUseCase 拋出 MaterialNotFoundException
	 * When: 發送 GET /api/v1/bom/excel?parentMaterialCode=NOT_EXIST_CODE
	 * Then: 回應 404 Not Found 結構化錯誤訊息
	 */
	@Test
	@DisplayName("場景：GET /api/v1/bom/excel 查無料號時回應 404 Not Found")
	void shouldReturn404WhenParentMaterialNotFound() throws Exception {
		// Given
		when(queryBomUseCase.queryBom(any(ExportBomCommand.class)))
				.thenThrow(new MaterialNotFoundException("查無指定料號之物料主檔: NOT_EXIST_CODE"));

		// When & Then
		mockMvc.perform(get("/api/v1/bom/excel")
						.param("parentMaterialCode", "NOT_EXIST_CODE"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.message").value("查無指定料號之物料主檔: NOT_EXIST_CODE"));
	}

	/**
	 * Feature: HTTP POST 結構化條件匯出 BOM Excel 檔案 (命令端)
	 * Scenario: 前端透過 JSON RequestBody 傳送匯出條件
	 * Given: 傳入合法之 ExportBomResource JSON
	 * When: 發送 POST /api/v1/bom/excel/export
	 * Then: 回應 200 OK 與對應二進位 Excel 串流
	 */
	@Test
	@DisplayName("場景：POST /api/v1/bom/excel/export 結構化請求成功產製並下載")
	void shouldExportBomExcelByPostSuccessfully() throws Exception {
		// Given
		byte[] mockBytes = "POST_BOM_BYTES".getBytes();
		BomExcelGottenResult gottenResult = new BomExcelGottenResult(
				mockBytes,
				"BOM_LIST_ALL.xlsx",
				2,
				50
		);
		when(exportBomUseCase.exportBom(any(ExportBomCommand.class))).thenReturn(gottenResult);

		String requestJson = """
				{
					"parentMaterialCode": null,
					"version": null,
					"includeMultiLevel": true,
					"includeSingleLevel": true,
					"includeMaterialMaster": false
				}
				""";

		// When & Then
		mockMvc.perform(post("/api/v1/bom/excel/export")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestJson))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				.andExpect(header().string("Content-Disposition", "attachment; filename*=UTF-8''BOM_LIST_ALL.xlsx"))
				.andExpect(content().bytes(mockBytes));

		verify(exportBomUseCase).exportBom(any(ExportBomCommand.class));
	}

	/**
	 * Feature: HTTP GET 大數據串流下載 BOM Excel (查詢端串流)
	 * Scenario: 前端發送 GET /api/v1/bom/excel/stream
	 * Given: 模擬 queryBomUseCase.streamQueryBom 寫入二進位串流
	 * When: 發送 GET 串流請求
	 * Then: 回應 200 OK，包含正確之 attachment 檔名與 Excel 串流內容
	 */
	@Test
	@DisplayName("場景：GET /api/v1/bom/excel/stream 成功執行大數據串流下載")
	void shouldStreamDownloadBomExcelByGetSuccessfully() throws Exception {
		// Given
		byte[] mockStreamBytes = "STREAM_EXCEL_BYTES".getBytes();
		doAnswer(invocation -> {
			OutputStream os = invocation.getArgument(0);
			os.write(mockStreamBytes);
			return null;
		}).when(queryBomUseCase).streamQueryBom(any(OutputStream.class), any(ExportBomCommand.class));

		// When & Then
		mockMvc.perform(get("/api/v1/bom/excel/stream")
						.param("parentMaterialCode", "MAT-FG-001")
						.param("version", "V1.0")
						.param("flushInterval", "500"))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				.andExpect(header().string("Content-Disposition", "attachment; filename*=UTF-8''BOM_MAT-FG-001_V1.0.xlsx"))
				.andExpect(content().bytes(mockStreamBytes));

		verify(queryBomUseCase).streamQueryBom(any(OutputStream.class), any(ExportBomCommand.class));
	}

	/**
	 * Feature: HTTP POST 結構化條件大數據串流匯出 BOM Excel (命令端串流)
	 * Scenario: 前端發送 POST /api/v1/bom/excel/stream 附帶 JSON 結構
	 * Given: 模擬 streamExportBomUseCase.streamExportBom 寫入二進位串流
	 * When: 發送 POST 串流請求
	 * Then: 回應 200 OK，包含正確之 attachment 檔名與 Excel 串流內容
	 */
	@Test
	@DisplayName("場景：POST /api/v1/bom/excel/stream 成功執行大數據串流輸出")
	void shouldStreamExportBomExcelByPostSuccessfully() throws Exception {
		// Given
		byte[] mockStreamBytes = "STREAM_POST_EXCEL_BYTES".getBytes();
		doAnswer(invocation -> {
			OutputStream os = invocation.getArgument(0);
			os.write(mockStreamBytes);
			return null;
		}).when(streamExportBomUseCase).streamExportBom(any(OutputStream.class), any(ExportBomCommand.class));

		String requestJson = """
				{
					"parentMaterialCode": "MAT-FG-001",
					"version": "1.0",
					"includeMultiLevel": true,
					"includeSingleLevel": true,
					"includeMaterialMaster": true,
					"flushInterval": 1000
				}
				""";

		// When & Then
		mockMvc.perform(post("/api/v1/bom/excel/stream")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestJson))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				.andExpect(header().string("Content-Disposition", "attachment; filename*=UTF-8''BOM_MAT-FG-001_1.0.xlsx"))
				.andExpect(content().bytes(mockStreamBytes));

		verify(streamExportBomUseCase).streamExportBom(any(OutputStream.class), any(ExportBomCommand.class));
	}
}
