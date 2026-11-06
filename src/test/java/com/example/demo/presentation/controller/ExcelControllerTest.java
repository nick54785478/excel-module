package com.example.demo.presentation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.io.OutputStream;
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
import com.example.demo.application.port.in.StreamExportExcelUseCase;
import com.example.demo.application.port.in.StreamImportExcelUseCase;
import com.example.demo.application.shared.command.ExportExcelCommand;
import com.example.demo.application.shared.command.StreamExportExcelCommand;
import com.example.demo.application.shared.command.StreamImportExcelCommand;
import com.example.demo.application.shared.dto.ExcelGottenResult;
import com.example.demo.application.shared.dto.ExcelSheetGottenResult;
import com.example.demo.application.shared.dto.ExcelStreamImportGottenResult;
import com.example.demo.application.shared.query.ReadExcelQuery;
import com.example.demo.presentation.assembler.ExcelResourceAssembler;
import com.example.demo.presentation.exception.GlobalExceptionHandler;

@DisplayName("ExcelController 表現層 REST 控制器與參數校驗測試")
class ExcelControllerTest {

	private MockMvc mockMvc;
	private ExportExcelUseCase exportExcelUseCase;
	private StreamExportExcelUseCase streamExportExcelUseCase;
	private ImportExcelUseCase importExcelUseCase;
	private StreamImportExcelUseCase streamImportExcelUseCase;

	@BeforeEach
	void setUp() {
		exportExcelUseCase = mock(ExportExcelUseCase.class);
		streamExportExcelUseCase = mock(StreamExportExcelUseCase.class);
		importExcelUseCase = mock(ImportExcelUseCase.class);
		streamImportExcelUseCase = mock(StreamImportExcelUseCase.class);
		ExcelResourceAssembler resourceAssembler = new ExcelResourceAssembler();

		ExcelController controller = new ExcelController(
				exportExcelUseCase,
				streamExportExcelUseCase,
				importExcelUseCase,
				streamImportExcelUseCase,
				resourceAssembler
		);

		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	/**
	 * Feature: Excel 標準檔案匯出 API
	 * Scenario: 前端傳送合法 JSON 格式資料，回傳二進位 xlsx 下載回應
	 * Given 傳送 ExportExcelResource JSON 載體
	 * When 發送 POST /api/v1/excel/export
	 * Then 回應 200 OK，且 Content-Disposition 標頭包含正確之檔名
	 */
	@Test
	@DisplayName("場景：POST /api/v1/excel/export 成功匯出並提供檔案下載")
	void shouldExportExcelFileSuccessfully() throws Exception {
		// Given
		byte[] mockBytes = "EXCEL_FILE_CONTENT".getBytes();
		when(exportExcelUseCase.export(any(ExportExcelCommand.class))).thenReturn(mockBytes);

		String requestJson = """
				{
					"fileName": "test_orders.xlsx",
					"sheets": [
						{
							"sheetName": "訂單清單",
							"headers": ["訂單編號", "金額"],
							"rows": [
								["ORD-001", 100],
								["ORD-002", 250]
							]
						}
					]
				}
				""";

		// When & Then
		mockMvc.perform(post("/api/v1/excel/export")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestJson))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				.andExpect(header().exists("Content-Disposition"))
				.andExpect(content().bytes(mockBytes));

		verify(exportExcelUseCase).export(any(ExportExcelCommand.class));
	}

	/**
	 * Feature: Excel 檔案匯出參數校驗 (Spring Validation)
	 * Scenario: 前端傳送空白檔名，觸發 @NotBlank 校驗失敗
	 * Given 傳送 fileName 為空白之 JSON 載體
	 * When 發送 POST /api/v1/excel/export
	 * Then 回應 400 Bad Request，並回傳 ValidationFailedResource 錯誤明細
	 */
	@Test
	@DisplayName("場景：POST /api/v1/excel/export 檔名為空白時拒絕並回傳 400")
	void shouldRejectExportWhenFileNameIsBlank() throws Exception {
		String invalidJson = """
				{
					"fileName": "",
					"sheets": [
						{
							"sheetName": "Sheet1",
							"headers": ["A"],
							"rows": []
						}
					]
				}
				""";

		mockMvc.perform(post("/api/v1/excel/export")
						.contentType(MediaType.APPLICATION_JSON)
						.content(invalidJson))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.fieldErrors").isArray());
	}

	/**
	 * Feature: Excel 檔案匯出參數校驗 (Spring Validation)
	 * Scenario: 前端未包含任何工作表，觸發 @NotEmpty 校驗失敗
	 * Given 傳送 sheets 為空陣列之 JSON 載體
	 * When 發送 POST /api/v1/excel/export
	 * Then 回應 400 Bad Request
	 */
	@Test
	@DisplayName("場景：POST /api/v1/excel/export sheets 為空時拒絕並回傳 400")
	void shouldRejectExportWhenSheetsIsEmpty() throws Exception {
		String invalidJson = """
				{
					"fileName": "test.xlsx",
					"sheets": []
				}
				""";

		mockMvc.perform(post("/api/v1/excel/export")
						.contentType(MediaType.APPLICATION_JSON)
						.content(invalidJson))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));
	}

	/**
	 * Feature: Excel 大數據串流匯出 API
	 * Scenario: 前端請求串流匯出，直接寫入 ServletOutputStream
	 * Given 傳送 StreamExportExcelResource JSON 載體
	 * When 發送 POST /api/v1/excel/export/stream
	 * Then 回應 200 OK，且成功觸發 StreamExportExcelUseCase.streamExport
	 */
	@Test
	@DisplayName("場景：POST /api/v1/excel/export/stream 成功串流輸出檔案")
	void shouldStreamExportExcelFileSuccessfully() throws Exception {
		// Given
		String requestJson = """
				{
					"fileName": "large_orders.xlsx",
					"batchFlushSize": 100,
					"sheets": [
						{
							"sheetName": "大數據工作表",
							"headers": ["序號", "數據"],
							"rows": [
								[1, "Data_1"],
								[2, "Data_2"]
							]
						}
					]
				}
				""";

		// When & Then
		mockMvc.perform(post("/api/v1/excel/export/stream")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestJson))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

		verify(streamExportExcelUseCase).streamExport(any(OutputStream.class), any(StreamExportExcelCommand.class));
	}

	/**
	 * Feature: Excel 大數據串流匯出參數校驗 (Spring Validation)
	 * Scenario: 前端傳送 batchFlushSize 小於 1，觸發 @Min 校驗失敗
	 * Given 傳送 batchFlushSize = 0 之 JSON 載體
	 * When 發送 POST /api/v1/excel/export/stream
	 * Then 回應 400 Bad Request
	 */
	@Test
	@DisplayName("場景：POST /api/v1/excel/export/stream batchFlushSize 小於 1 時拒絕並回傳 400")
	void shouldRejectStreamExportWhenBatchFlushSizeIsInvalid() throws Exception {
		String invalidJson = """
				{
					"fileName": "large.xlsx",
					"batchFlushSize": 0,
					"sheets": [
						{
							"sheetName": "Sheet1",
							"headers": ["A"],
							"rows": []
						}
					]
				}
				""";

		mockMvc.perform(post("/api/v1/excel/export/stream")
						.contentType(MediaType.APPLICATION_JSON)
						.content(invalidJson))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));
	}

	/**
	 * Feature: Excel 大數據串流匯出參數校驗 (Spring Validation)
	 * Scenario: 前端傳送 sheets 為空清單，觸發 @NotEmpty 校驗失敗
	 * Given 傳送 sheets 為空陣列之 JSON 載體
	 * When 發送 POST /api/v1/excel/export/stream
	 * Then 回應 400 Bad Request
	 */
	@Test
	@DisplayName("場景：POST /api/v1/excel/export/stream sheets 為空時拒絕並回傳 400")
	void shouldRejectStreamExportWhenSheetsIsEmpty() throws Exception {
		String invalidJson = """
				{
					"fileName": "large.xlsx",
					"batchFlushSize": 500,
					"sheets": []
				}
				""";

		mockMvc.perform(post("/api/v1/excel/export/stream")
						.contentType(MediaType.APPLICATION_JSON)
						.content(invalidJson))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));
	}

	/**
	 * Feature: Excel 檔案上傳解析 API
	 * Scenario: 上傳 Multipart 檔案，解析並回傳工作表結構 JSON
	 * Given 建立包含標題與列資料之 MockMultipartFile
	 * When 發送 POST /api/v1/excel/import
	 * Then 回應 200 OK，且 JSON 包含正確之工作表數量與欄位名稱
	 */
	@Test
	@DisplayName("場景：POST /api/v1/excel/import 成功上傳解析檔案")
	void shouldImportExcelFileSuccessfully() throws Exception {
		// Given
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"sample.xlsx",
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				new byte[] { 1, 2, 3 }
		);

		ExcelSheetGottenResult sheetResult = new ExcelSheetGottenResult(
				0,
				"Sheet1",
				List.of("ID", "Name"),
				List.of(List.of(1, "Alice")),
				List.of(List.of("1", "Alice"))
		);
		when(importExcelUseCase.importExcel(any(InputStream.class), any(ReadExcelQuery.class)))
				.thenReturn(new ExcelGottenResult(List.of(sheetResult)));

		// When & Then
		mockMvc.perform(multipart("/api/v1/excel/import")
						.file(file)
						.param("sheetName", "Sheet1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sheetCount").value(1))
				.andExpect(jsonPath("$.sheets[0].sheetName").value("Sheet1"))
				.andExpect(jsonPath("$.sheets[0].headers[0]").value("ID"))
				.andExpect(jsonPath("$.sheets[0].headers[1]").value("Name"));

		verify(importExcelUseCase).importExcel(any(InputStream.class), any(ReadExcelQuery.class));
	}

	/**
	 * Feature: Excel 檔案上傳解析參數校驗
	 * Scenario: 上傳空檔案，觸發業務或參數校驗攔截
	 * Given 上傳 byte[] 為空之 MockMultipartFile
	 * When 發送 POST /api/v1/excel/import
	 * Then 回應 400 Bad Request
	 */
	@Test
	@DisplayName("場景：POST /api/v1/excel/import 上傳空檔案時拒絕並回傳 400")
	void shouldRejectImportWhenFileIsEmpty() throws Exception {
		MockMultipartFile emptyFile = new MockMultipartFile(
				"file",
				"empty.xlsx",
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				new byte[0]
		);

		mockMvc.perform(multipart("/api/v1/excel/import")
						.file(emptyFile))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.message").value("上傳之 Excel 檔案不能為空"));
	}

	/**
	 * Feature: Excel 大數據串流匯入解析 API
	 * Scenario: 上傳檔案並執行分塊批次解析，回傳處理指標統計
	 * Given 建立 MockMultipartFile
	 * When 發送 POST /api/v1/excel/import/stream
	 * Then 回應 200 OK，且 JSON 包含正確之處理總列數與分塊數
	 */
	@Test
	@DisplayName("場景：POST /api/v1/excel/import/stream 成功分塊解析大數據檔案")
	void shouldStreamImportExcelFileSuccessfully() throws Exception {
		// Given
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"large.xlsx",
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				new byte[] { 1, 2, 3 }
		);

		ExcelStreamImportGottenResult mockResult = new ExcelStreamImportGottenResult(
				"BigSheet",
				10000,
				10,
				150,
				"SUCCESS",
				"Processed 10000 rows"
		);
		when(streamImportExcelUseCase.streamImport(any(StreamImportExcelCommand.class)))
				.thenReturn(mockResult);

		// When & Then
		mockMvc.perform(multipart("/api/v1/excel/import/stream")
						.file(file)
						.param("sheetName", "BigSheet")
						.param("chunkSize", "1000"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sheetName").value("BigSheet"))
				.andExpect(jsonPath("$.totalRowsProcessed").value(10000))
				.andExpect(jsonPath("$.totalChunks").value(10))
				.andExpect(jsonPath("$.status").value("SUCCESS"));

		verify(streamImportExcelUseCase).streamImport(any(StreamImportExcelCommand.class));
	}
}
