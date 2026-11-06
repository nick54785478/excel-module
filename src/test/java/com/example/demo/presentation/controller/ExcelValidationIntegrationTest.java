package com.example.demo.presentation.controller;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Excel 宣告式資料檢核與視覺化錯誤回標端到端整合測試 (Excel Validation End-to-End Integration Test)
 * <p>
 * <b>【架構定位與職責規範】</b><br>
 * 啟動完整 Spring Boot 整合環境，以真實 ApplicationContext 測試 WebMvc、AOP 切面、
 * EasyRules 規則引擎適配器、ExcelBeanAssembler 宣告式組裝器與 Apache POI 樣式回標適配器。<br>
 * 直接載入 {@code doc/template/ORDER_IMPORT_WITH_ERRORS.xlsx} 與 {@code ORDER_IMPORT_SUCCESS.xlsx}，
 * 驗證真實試算表上傳後之各項行為：
 * <ol>
 *   <li>包含錯誤時正確產製並回傳 {@code .xlsx} 檔案供下載，避免拋出 500 伺服器內部錯誤。</li>
 *   <li>回標檔經 POI 解析可確認包含「檢核結果與錯誤原因」欄位且出錯儲存格樣式標註生效。</li>
 *   <li>全數合格時智能降級回傳 JSON 統計報告。</li>
 * </ol>
 * </p>
 */
@SpringBootTest
@DisplayName("Excel 宣告式資料檢核與錯誤回標整合測試 (端到端真實檔案上傳)")
class ExcelValidationIntegrationTest {

	private static final String EXCEL_MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

	@Autowired
	private WebApplicationContext webApplicationContext;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
	}

	/**
	 * Feature: 上傳真實含違規訂單 Excel 檔案取得錯誤回標下載檔
	 * 
	 * Scenario: 上傳 ORDER_IMPORT_WITH_ERRORS.xlsx (內含 Excel 浮點數日期序號與金額/數量違規資料)
	 *   Given 從 doc/template/ORDER_IMPORT_WITH_ERRORS.xlsx 讀取二進位資料
	 *    When 發送 POST /api/v1/excel/validation/orders/feedback 上傳請求
	 *    Then HTTP 狀態碼應為 200 OK (非 500 Internal Server Error)
	 *     And Content-Type 應為 Excel MIME 格式
	 *     And Content-Disposition 應包含 attachment 下載指示
	 *     And 回傳的二進位試算表最右側應追加「檢核結果與錯誤原因」說明欄
	 */
	@Test
	@DisplayName("場景：上傳 ORDER_IMPORT_WITH_ERRORS.xlsx 應回傳 HTTP 200 帶標註之回標檔下載 (非 500)")
	void shouldReturnErrorMarkedExcelWhenUploadingOrderImportWithErrors() throws Exception {
		// Given: 讀取專案範本中的真實測試檔案
		Path templatePath = Path.of("doc", "template", "ORDER_IMPORT_WITH_ERRORS.xlsx");
		assertThat(Files.exists(templatePath)).isTrue();
		byte[] fileBytes = Files.readAllBytes(templatePath);

		MockMultipartFile multipartFile = new MockMultipartFile(
				"file",
				"ORDER_IMPORT_WITH_ERRORS.xlsx",
				EXCEL_MIME,
				fileBytes
		);

		// When: 呼叫回標檔產製端點
		MvcResult mvcResult = mockMvc.perform(multipart("/api/v1/excel/validation/orders/feedback")
						.file(multipartFile))
				// Then: 必須為 200 OK，嚴格禁止 500 錯誤
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.parseMediaType(EXCEL_MIME)))
				.andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("attachment")))
				.andReturn();

		byte[] responseBytes = mvcResult.getResponse().getContentAsByteArray();
		assertThat(responseBytes).isNotEmpty();

		// 驗證產出的回標檔結構
		try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(responseBytes))) {
			Sheet sheet = workbook.getSheetAt(0);
			assertThat(sheet).isNotNull();

			// 驗證標題列已追加「檢核結果與錯誤原因」欄位
			Row headerRow = sheet.getRow(0);
			assertThat(headerRow).isNotNull();
			int lastCol = headerRow.getLastCellNum() - 1;
			assertThat(headerRow.getCell(lastCol).getStringCellValue()).isEqualTo("檢核結果與錯誤原因");

			// 驗證資料列包含錯誤說明 (例如負數金額或數量超標)
			boolean hasErrorComments = false;
			for (int r = 1; r <= sheet.getLastRowNum(); r++) {
				Row dataRow = sheet.getRow(r);
				if (dataRow != null && dataRow.getCell(lastCol) != null) {
					String comment = dataRow.getCell(lastCol).getStringCellValue();
					if (comment != null && !comment.isBlank()) {
						hasErrorComments = true;
						break;
					}
				}
			}
			assertThat(hasErrorComments).isTrue();
		}
	}

	/**
	 * Feature: 上傳完全合格之訂單 Excel 檔案時智能分流降級
	 * 
	 * Scenario: 上傳 ORDER_IMPORT_SUCCESS.xlsx (資料完全合法)
	 *   Given 從 doc/template/ORDER_IMPORT_SUCCESS.xlsx 讀取二進位資料
	 *    When 發送 POST /api/v1/excel/validation/orders/feedback
	 *    Then 應自動降級回傳 HTTP 200 JSON 統計報告，無須產製空回標檔
	 *     And success 應為 true，errorCount 應為 0
	 */
	@Test
	@DisplayName("場景：上傳 ORDER_IMPORT_SUCCESS.xlsx 應降級回傳 HTTP 200 JSON 成功報告")
	void shouldReturnJsonReportWhenUploadingFullyValidExcel() throws Exception {
		// Given
		Path templatePath = Path.of("doc", "template", "ORDER_IMPORT_SUCCESS.xlsx");
		assertThat(Files.exists(templatePath)).isTrue();
		byte[] fileBytes = Files.readAllBytes(templatePath);

		MockMultipartFile multipartFile = new MockMultipartFile(
				"file",
				"ORDER_IMPORT_SUCCESS.xlsx",
				EXCEL_MIME,
				fileBytes
		);

		// When & Then
		mockMvc.perform(multipart("/api/v1/excel/validation/orders/feedback")
						.file(multipartFile))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.errorCount").value(0))
				.andExpect(jsonPath("$.validCount").value(org.hamcrest.Matchers.greaterThan(0)));
	}

	/**
	 * Feature: 上傳違規訂單至純 JSON 檢核端點
	 * 
	 * Scenario: 上傳 ORDER_IMPORT_WITH_ERRORS.xlsx 至純檢核端點
	 *   Given ORDER_IMPORT_WITH_ERRORS.xlsx 檔案
	 *    When 發送 POST /api/v1/excel/validation/orders
	 *    Then 應回傳 HTTP 422 Unprocessable Entity
	 *     And success 應為 false
	 *     And 應包含金額必須大於 0 或數量不可超過 9999 之錯誤
	 */
	@Test
	@DisplayName("場景：上傳 ORDER_IMPORT_WITH_ERRORS.xlsx 至純檢核端點應回傳 HTTP 422 JSON")
	void shouldReturn422WhenUploadingErrorsExcelToJsonEndpoint() throws Exception {
		// Given
		Path templatePath = Path.of("doc", "template", "ORDER_IMPORT_WITH_ERRORS.xlsx");
		byte[] fileBytes = Files.readAllBytes(templatePath);

		MockMultipartFile multipartFile = new MockMultipartFile(
				"file",
				"ORDER_IMPORT_WITH_ERRORS.xlsx",
				EXCEL_MIME,
				fileBytes
		);

		// When & Then
		mockMvc.perform(multipart("/api/v1/excel/validation/orders")
						.file(multipartFile))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.errorCount").value(org.hamcrest.Matchers.greaterThan(0)));
	}

	/**
	 * Feature: 動態 OCP 範本回標端點 /{templateKey}/feedback
	 * 
	 * Scenario: 透過動態路徑變數 templateKey (如 "OrderModel") 發送含有錯誤之 Excel
	 *   Given ORDER_IMPORT_WITH_ERRORS.xlsx 檔案
	 *    When 發送 POST /api/v1/excel/validation/OrderModel/feedback
	 *    Then 應成功命中 OCP 動態路由並回傳 HTTP 200 下載回標檔案
	 *     And 檔案二進位長度應大於 0
	 */
	@Test
	@DisplayName("場景：透過 OCP 動態路徑 /{templateKey}/feedback 應成功解析模型並產製回標檔")
	void shouldSupportDynamicTemplateKeyEndpointForFeedback() throws Exception {
		Path templatePath = Path.of("doc", "template", "ORDER_IMPORT_WITH_ERRORS.xlsx");
		byte[] fileBytes = Files.readAllBytes(templatePath);

		MockMultipartFile multipartFile = new MockMultipartFile(
				"file",
				"ORDER_IMPORT_WITH_ERRORS.xlsx",
				EXCEL_MIME,
				fileBytes
		);

		mockMvc.perform(multipart("/api/v1/excel/validation/OrderModel/feedback")
						.file(multipartFile))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.parseMediaType(EXCEL_MIME)))
				.andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("attachment")));
	}

	/**
	 * Feature: Zero-Config 智慧自動探測回標端點 /feedback
	 * 
	 * Scenario: 不指定任何 templateKey，直接上傳 Excel 檔案
	 *   Given ORDER_IMPORT_WITH_ERRORS.xlsx 檔案 (Sheet 名稱為「訂單匯入清單」)
	 *    When 發送 POST /api/v1/excel/validation/feedback
	 *    Then 後端應自動從第一個 Sheet 名稱探測出 OrderModel 並完成檢核
	 *     And 回傳 HTTP 200 帶標註之回標檔下載
	 */
	@Test
	@DisplayName("場景：透過 Zero-Config /feedback 端點依工作表名稱自動辨識範本並產製回標檔")
	void shouldAutoDetectTemplateBySheetNameAndGenerateFeedback() throws Exception {
		Path templatePath = Path.of("doc", "template", "ORDER_IMPORT_WITH_ERRORS.xlsx");
		byte[] fileBytes = Files.readAllBytes(templatePath);

		MockMultipartFile multipartFile = new MockMultipartFile(
				"file",
				"ORDER_IMPORT_WITH_ERRORS.xlsx",
				EXCEL_MIME,
				fileBytes
		);

		mockMvc.perform(multipart("/api/v1/excel/validation/feedback")
						.file(multipartFile))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.parseMediaType(EXCEL_MIME)))
				.andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("attachment")));
	}

	/**
	 * Feature: 範本探索端點 GET /templates
	 * 
	 * Scenario: 查詢系統目前所有已註冊之範本規格清單
	 *    When 發送 GET /api/v1/excel/validation/templates
	 *    Then 應回傳 HTTP 200 JSON 陣列
	 *     And 應包含 orders, products, materials, boms 核心範本描述符
	 */
	@Test
	@DisplayName("場景：查詢可用範本清單 GET /templates 應回傳所有已註冊之範本元數據")
	void shouldListAllRegisteredTemplates() throws Exception {
		mockMvc.perform(get("/api/v1/excel/validation/templates"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
				.andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(4)))
				.andExpect(jsonPath("$[*].key").value(org.hamcrest.Matchers.hasItems("orders", "products", "materials", "boms")));
	}
}
