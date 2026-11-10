package com.example.demo.presentation.controller.parallel;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.demo.application.port.in.parallel.ExportParallelExcelUseCase;
import com.example.demo.presentation.assembler.ExcelParallelResourceAssembler;

/**
 * 高並發平行與分片匯出 REST 控制器單元測試 (ExcelParallelExportController Test)
 * <p>
 * 遵循 AGENTS.md 規則 5 規範，採用 BDD 與 Gherkin (Feature, Scenario, Given-When-Then) 結構撰寫。
 */
@DisplayName("ExcelParallelExportController 平行與分片控制器測試")
class ExcelParallelExportControllerTest {

	private MockMvc mockMvc;
	private ExportParallelExcelUseCase parallelExcelUseCase;
	private ExcelParallelResourceAssembler resourceAssembler;

	@BeforeEach
	void setUp() {
		parallelExcelUseCase = mock(ExportParallelExcelUseCase.class);
		resourceAssembler = new ExcelParallelResourceAssembler();

		ExcelParallelExportController controller = new ExcelParallelExportController(
				parallelExcelUseCase,
				resourceAssembler
		);

		this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
	}

	/**
	 * Feature: 一鍵模式 A 微觀管線樣本 GET 匯出
	 * Scenario: 瀏覽器直接發送 GET 請求下載管線加速 Excel
	 * Given: 呼叫 GET /api/v1/excel/parallel/pipeline-sample?totalRows=1000
	 * When: 發送 GET 請求
	 * Then: 回傳 200 OK，包含 application/vnd.openxmlformats-officedocument.spreadsheetml.sheet 標頭與附件下載名稱
	 */
	@Test
	@DisplayName("一鍵模式 A 管線樣本 GET 下載成功")
	void should_download_pipeline_sample_via_get() throws Exception {
		// Given
		byte[] mockExcel = new byte[]{'P', 'K', 1, 2};
		when(parallelExcelUseCase.exportPipeline(any())).thenReturn(mockExcel);

		// When & Then
		mockMvc.perform(get("/api/v1/excel/parallel/pipeline-sample")
						.param("totalRows", "1000"))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				.andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("parallel_pipeline_sample.xlsx")))
				.andExpect(content().bytes(mockExcel));

		verify(parallelExcelUseCase).exportPipeline(any());
	}

	/**
	 * Feature: POST 模式 A 自訂參數管線匯出
	 * Scenario: 傳送 JSON 自訂總筆數與批次大小，下載產製的 Excel
	 * Given: 構造 JSON 負載並發送 POST /api/v1/excel/parallel/pipeline
	 * When: 發送 POST 請求
	 * Then: 回傳 200 OK，並產出對應之 Excel 附件
	 */
	@Test
	@DisplayName("自訂參數模式 A 管線 POST 匯出成功")
	void should_export_pipeline_custom_via_post() throws Exception {
		// Given
		byte[] mockExcel = new byte[]{'P', 'K', 5, 6};
		when(parallelExcelUseCase.exportPipeline(any())).thenReturn(mockExcel);

		String jsonPayload = """
				{
				  "sheetName": "自訂管線測試",
				  "totalRows": 5000,
				  "chunkSize": 500,
				  "queueCapacity": 20,
				  "parallelProducers": 2
				}
				""";

		// When & Then
		mockMvc.perform(post("/api/v1/excel/parallel/pipeline")
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonPayload))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				.andExpect(content().bytes(mockExcel));

		verify(parallelExcelUseCase).exportPipeline(any());
	}

	/**
	 * Feature: 一鍵模式 B / A+B 多活頁簿 ZIP 分片樣本 GET 匯出
	 * Scenario: 瀏覽器直接發送 GET 請求下載分片打包之 ZIP
	 * Given: 呼叫 GET /api/v1/excel/parallel/partition-zip-sample
	 * When: 發送 GET 請求
	 * Then: 回傳 200 OK，包含 application/zip 標頭與附件下載名稱
	 */
	@Test
	@DisplayName("一鍵模式 B / A+B 分片 ZIP 樣本 GET 下載成功")
	void should_download_partition_zip_sample_via_get() throws Exception {
		// Given
		byte[] mockZip = new byte[]{'P', 'K', 3, 4};
		when(parallelExcelUseCase.exportPartitionZip(any())).thenReturn(mockZip);

		// When & Then
		mockMvc.perform(get("/api/v1/excel/parallel/partition-zip-sample")
						.param("totalRows", "5000")
						.param("maxRowsPerFile", "2000"))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/zip"))
				.andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("orders_multi_partition_archive.zip")))
				.andExpect(content().bytes(mockZip));

		verify(parallelExcelUseCase).exportPartitionZip(any());
	}

	/**
	 * Feature: POST 模式 B / A+B 自訂參數分片 ZIP 匯出
	 * Scenario: 傳送 JSON 參數，要求分片並產製 ZIP 下載
	 * Given: 構造 JSON 負載並發送 POST /api/v1/excel/parallel/partition-zip
	 * When: 發送 POST 請求
	 * Then: 回傳 200 OK，並產出對應之 ZIP 附件
	 */
	@Test
	@DisplayName("自訂參數模式 B / A+B 分片 ZIP POST 匯出成功")
	void should_export_partition_zip_custom_via_post() throws Exception {
		// Given
		byte[] mockZip = new byte[]{'P', 'K', 7, 8};
		when(parallelExcelUseCase.exportPartitionZip(any())).thenReturn(mockZip);

		String jsonPayload = """
				{
				  "zipFileName": "custom_archive.zip",
				  "baseFileName": "custom_part",
				  "sheetName": "明細",
				  "totalRows": 8000,
				  "maxRowsPerFile": 4000,
				  "usePipelinePerPartition": true
				}
				""";

		// When & Then
		mockMvc.perform(post("/api/v1/excel/parallel/partition-zip")
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonPayload))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/zip"))
				.andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("custom_archive.zip")))
				.andExpect(content().bytes(mockZip));

		verify(parallelExcelUseCase).exportPartitionZip(any());
	}

	/**
	 * Feature: POST 模式 A 微觀管線串流匯出 (/pipeline/stream)
	 * Scenario: 傳送 JSON 參數，以 HTTP 串流形式直接寫入 Servlet OutputStream
	 * Given: 構造合法 JSON 負載並發送 POST /api/v1/excel/parallel/pipeline/stream
	 * When: 發送 POST 請求
	 * Then: 回傳 200 OK，包含 application/vnd.openxmlformats-officedocument.spreadsheetml.sheet 標頭並調用 exportPipelineStream
	 */
	@Test
	@DisplayName("微觀管線串流 POST /pipeline/stream 成功寫入 Response 串流")
	void should_stream_export_pipeline_via_post() throws Exception {
		String jsonPayload = """
				{
				  "sheetName": "串流管線明細",
				  "totalRows": 10000,
				  "chunkSize": 1000,
				  "queueCapacity": 20,
				  "parallelProducers": 4
				}
				""";

		mockMvc.perform(post("/api/v1/excel/parallel/pipeline/stream")
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonPayload))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				.andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("filename*=UTF-8''")));

		verify(parallelExcelUseCase).exportPipelineStream(any(), any());
	}

	/**
	 * Feature: POST 模式 B / A+B 多活頁簿 ZIP 分片串流匯出 (/partition/zip/stream)
	 * Scenario: 傳送 JSON 參數，以 HTTP 串流形式動態壓縮並寫入 Servlet OutputStream
	 * Given: 構造合法 JSON 負載並發送 POST /api/v1/excel/parallel/partition/zip/stream
	 * When: 發送 POST 請求
	 * Then: 回傳 200 OK，包含 application/zip 標頭並調用 exportPartitionZipStream
	 */
	@Test
	@DisplayName("分片 ZIP 串流 POST /partition/zip/stream 成功寫入 Response 串流")
	void should_stream_export_partition_zip_via_post() throws Exception {
		String jsonPayload = """
				{
				  "zipFileName": "stream_partition_archive.zip",
				  "baseFileName": "stream_part",
				  "sheetName": "分片串流",
				  "totalRows": 20000,
				  "maxRowsPerFile": 10000,
				  "usePipelinePerPartition": true
				}
				""";

		mockMvc.perform(post("/api/v1/excel/parallel/partition/zip/stream")
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonPayload))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/zip"))
				.andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("stream_partition_archive.zip")));

		verify(parallelExcelUseCase).exportPartitionZipStream(any(), any());
	}
}

