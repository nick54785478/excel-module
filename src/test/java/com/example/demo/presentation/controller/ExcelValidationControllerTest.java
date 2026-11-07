package com.example.demo.presentation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.demo.application.port.in.ValidateExcelUseCase;
import com.example.demo.application.shared.dto.ExcelValidationGottenResult;
import com.example.demo.application.shared.model.ExcelRowError;
import com.example.demo.application.shared.model.OrderModel;
import com.example.demo.application.shared.model.ProductRecordModel;
import com.example.demo.presentation.aspect.ExcelValidationAspect;
import com.example.demo.presentation.assembler.ExcelValidationResourceAssembler;
import com.example.demo.presentation.exception.GlobalExceptionHandler;
import com.example.demo.presentation.resolver.ValidatedExcelDataArgumentResolver;

/**
 * Excel 客製檢核與錯誤回標控制器測試 (ExcelValidationController Test)
 * <p>
 * 遵循 BDD 與 Gherkin 規範，驗證 {@link ExcelValidationController} 之 REST API 行為：
 * 包括 Multipart 檔案上傳、HTTP 200/422 狀態碼回應、JSON 錯誤細節以及二進位錯誤回標檔下載標頭。
 * </p>
 */
@DisplayName("ExcelValidationController 客製檢核與回標控制器測試")
class ExcelValidationControllerTest {

	private MockMvc mockMvc;
	private ValidateExcelUseCase validateExcelUseCase;
	private ExcelValidationResourceAssembler resourceAssembler;

	@BeforeEach
	void setUp() {
		// 1. 模擬應用層檢核業務案例介面 (Inbound Port / Use Case)
		validateExcelUseCase = mock(ValidateExcelUseCase.class);

		// 2. 建立表現層純資料組裝器實例 (防腐層 ACL 轉換)
		resourceAssembler = new ExcelValidationResourceAssembler();

		// 3. 透過手動依賴注入建立 REST 控制器實例
		ExcelValidationController rawController = new ExcelValidationController(validateExcelUseCase, resourceAssembler);

		// 4. 掛載 ExcelValidationAspect 切面代理，啟用宣告式 @ValidateExcel AOP 攔截
		ExcelValidationAspect aspect = new ExcelValidationAspect(validateExcelUseCase);
		AspectJProxyFactory factory = new AspectJProxyFactory(rawController);
		factory.addAspect(aspect);
		ExcelValidationController controllerProxy = factory.getProxy();

		// 5. 建立整合 ControllerAdvice 與自訂參數解析器的 MockMvc 測試環境
		mockMvc = MockMvcBuilders.standaloneSetup(controllerProxy)
				.setControllerAdvice(new GlobalExceptionHandler(resourceAssembler))
				.setCustomArgumentResolvers(new ValidatedExcelDataArgumentResolver())
				.build();
	}

	/**
	 * Feature: 上傳訂單 Excel 進行檢核並取得 JSON 成功報告
	 * 
	 * Scenario: 上傳完全合格的訂單 Excel 檔案，API 應回傳 HTTP 200 OK 與 success=true
	 *   Given 使用者上傳包含合法訂單的 Excel Multipart 檔案
	 *     And UseCase 回傳檢核完全通過結果
	 *    When 發送 POST /api/v1/excel/validation/orders 請求
	 *    Then 應回傳 HTTP 200 OK 狀態碼
	 *     And JSON 回應中 success 欄位應為 true
	 *     And validCount 應等於 totalRows
	 */
	@Test
	@DisplayName("上傳完全合格之訂單 Excel 時應回傳 HTTP 200 與成功 JSON")
	void shouldReturn200WhenValidatingOrdersSucceeds() throws Exception {
		// ==========================================
		// [Given] 測試資料與 Mock 行為準備
		// ==========================================
		// 1. 模擬前端上傳之標準 Excel 檔案 (包含參數名、原始檔名、MIME 類型與測試用位元組)
		MockMultipartFile file = new MockMultipartFile(
				"file",                                                               // 請求參數名稱 (對應 @RequestParam("file"))
				"valid_orders.xlsx",                                                  // 上傳檔案名稱
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",   // 標準 OpenXML Excel MIME 類型
				new byte[]{1, 2, 3}                                                   // 檔案內容二進位位元組 (測試 Stub)
		);

		// 2. 建立符合領域規範的合法訂單模型實例 (包含單號、訂購人、金額、數量、下單日期與狀態)
		OrderModel o1 = new OrderModel("ORD-001", "張三", new BigDecimal("100.00"), 1, LocalDate.now(), "PAID");

		// 3. 封裝應用層檢核完全合格之結果物件 (包含 1 筆合法資料，錯誤清單為空)
		ExcelValidationGottenResult<OrderModel> mockResult = ExcelValidationGottenResult.success(List.of(o1));

		// 4. 定義 UseCase Mock 行為：當傳入任何輸入串流與 OrderModel.class 時，回傳合格結果
		when(validateExcelUseCase.validate(any(InputStream.class), eq(OrderModel.class))).thenReturn(mockResult);

		// ==========================================
		// [When & Then] 執行 HTTP POST 請求並斷言回應結構
		// ==========================================
		mockMvc.perform(multipart("/api/v1/excel/validation/orders") // 發送 Multipart 檔案上傳 POST 請求
						.file(file))                                 // 附加模擬的 Excel 檔案
				// 驗證 HTTP 狀態碼為 200 OK
				.andExpect(status().isOk())
				// 驗證回應 JSON 根節點 success 旗標為 true (代表整表完全合格)
				.andExpect(jsonPath("$.success").value(true))
				// 驗證總解析列數為 1 筆
				.andExpect(jsonPath("$.totalRows").value(1))
				// 驗證合格列數為 1 筆
				.andExpect(jsonPath("$.validCount").value(1))
				// 驗證錯誤筆數為 0
				.andExpect(jsonPath("$.errorCount").value(0))
				// 驗證錯誤清單陣列為空
				.andExpect(jsonPath("$.errors").isEmpty());
	}

	/**
	 * Feature: 上傳包含違規資料之訂單 Excel 取得 422 錯誤清單
	 * 
	 * Scenario: 上傳包含負數金額的訂單，API 應回傳 HTTP 422 Unprocessable Entity 與詳細錯誤清單
	 *   Given 使用者上傳含有金額為 -50.00 的訂單 Excel 檔案
	 *     And UseCase 回傳包含 1 筆錯誤事實的結果
	 *    When 發送 POST /api/v1/excel/validation/orders 請求
	 *    Then 應回傳 HTTP 422 Unprocessable Entity 狀態碼
	 *     And JSON 回應中 success 應為 false
	 *     And 應包含錯誤細節 (列號 2、購買金額、錯誤原因)
	 */
	@Test
	@DisplayName("上傳包含錯誤資料之訂單時應回傳 HTTP 422 與全量錯誤 JSON")
	void shouldReturn422WhenValidatingOrdersFails() throws Exception {
		// ==========================================
		// [Given] 測試資料與 Mock 行為準備
		// ==========================================
		// 1. 模擬包含異常資料之上傳檔案
		MockMultipartFile file = new MockMultipartFile(
				"file", 
				"invalid_orders.xlsx",
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", 
				new byte[]{1, 2, 3}
		);

		// 2. 構造單列儲存格違規錯誤事實 (Row 2, Column 2「購買金額」金額為負數)
		ExcelRowError error = ExcelRowError.ofCell(
				2,                           // 試算表實體列號 (1-based / 資料列)
				2,                           // 欄位索引 (0-based)
				"購買金額",                   // 出錯欄位中文名稱
				"-50.00",                    // 違規的原始輸入值
				"購買金額必須大於 0.00",        // 業務規則違規原因說明
				"INVALID_ORDER_AMOUNT"       // 錯誤規則代碼
		);

		// 3. 封裝應用層檢核不合格之結果物件 (包含合格清單為空、錯誤事實 1 筆、總列數 1 列)
		ExcelValidationGottenResult<OrderModel> mockResult = ExcelValidationGottenResult.failed(
				List.of(),                   // 合格模型清單
				List.of(error),              // 違規錯誤清單
				1                            // 總列數
		);

		// 4. 定義 UseCase Mock 行為：返回包含錯誤事實之檢核結果
		when(validateExcelUseCase.validate(any(InputStream.class), eq(OrderModel.class))).thenReturn(mockResult);

		// ==========================================
		// [When & Then] 執行 HTTP POST 請求並斷言 422 錯誤結構
		// ==========================================
		mockMvc.perform(multipart("/api/v1/excel/validation/orders") // 發送 Multipart 請求
						.file(file))                                 // 附加檔案
				// 驗證 HTTP 狀態碼為 422 Unprocessable Entity (語法正確但違反領域商業規則)
				.andExpect(status().isUnprocessableEntity())
				// 驗證回應 JSON success 欄位為 false (代表檢核未全數通過)
				.andExpect(jsonPath("$.success").value(false))
				// 驗證錯誤總筆數為 1
				.andExpect(jsonPath("$.errorCount").value(1))
				// 驗證第一筆錯誤所發生的資料列號為 2
				.andExpect(jsonPath("$.errors[0].rowIndex").value(2))
				// 驗證出錯欄位名稱為「購買金額」
				.andExpect(jsonPath("$.errors[0].columnName").value("購買金額"))
				// 驗證錯誤訊息包含詳細業務描述
				.andExpect(jsonPath("$.errors[0].errorMessage").value("購買金額必須大於 0.00"));
	}

	/**
	 * Feature: 產製二進位錯誤回標檔下載
	 * 
	 * Scenario: 透過 feedback 端點上傳有錯誤之訂單，API 應直接回傳帶有標註之 Excel 檔案下載
	 *   Given 使用者上傳含有錯誤的 Excel 檔案
	 *     And UseCase 回傳非空之回標 Excel 二進位陣列
	 *    When 發送 POST /api/v1/excel/validation/orders/feedback 請求
	 *    Then 應回傳 HTTP 200 OK
	 *     And 應帶有 Content-Disposition 附件下載檔名標頭
	 *     And Content-Type 應為 Excel MIME 類型
	 */
	@Test
	@DisplayName("檢核含有錯誤時透過 feedback 端點應回傳 Excel 回標檔二進位下載")
	void shouldReturnAttachmentWhenValidatingOrdersWithFeedbackAndErrorsExist() throws Exception {
		// ==========================================
		// [Given] 測試資料與 Mock 行為準備
		// ==========================================
		// 1. 模擬待檢核之訂單 Excel 上傳檔案
		MockMultipartFile file = new MockMultipartFile(
				"file", 
				"orders.xlsx",
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", 
				new byte[]{1, 2, 3}
		);

		// 2. 模擬已標註淡紅底深紅字與右側追加錯誤欄位的回標 Excel 二進位位元組 (以 PK ZIP Magic Number 為代表)
		byte[] mockFeedbackExcelBytes = new byte[]{80, 75, 3, 4}; // PK ZIP header (標準 .xlsx 壓縮包特徵)

		// 3. 定義 UseCase Mock 行為：檢核並產製回標檔時，回傳非空的二進位位元組陣列
		when(validateExcelUseCase.validateAndGenerateFeedback(any(InputStream.class), eq(OrderModel.class)))
				.thenReturn(mockFeedbackExcelBytes);

		// ==========================================
		// [When & Then] 執行 HTTP POST 請求並斷言下載標頭與檔案二進位內容
		// ==========================================
		mockMvc.perform(multipart("/api/v1/excel/validation/orders/feedback") // 調用回標專用端點
						.file(file))                                          // 附加檔案
				// 驗證 HTTP 狀態碼為 200 OK
				.andExpect(status().isOk())
				// 驗證包含 Content-Disposition 標頭，指示瀏覽器啟動檔案下載 (attachment)
				.andExpect(header().exists("Content-Disposition"))
				// 驗證回應 Content-Type 為微軟 OpenXML Spreadsheet 規格
				.andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				// 驗證回應二進位內容與 Mock 產出的回標位元組完全一致
				.andExpect(content().bytes(mockFeedbackExcelBytes));
	}

	/**
	 * Feature: 上傳完全合格檔案至 feedback 端點時回傳成功 JSON 報告
	 * 
	 * Scenario: 透過 feedback 端點上傳合格檔案，後端不產製空回標檔，而是直接回傳 200 OK 與成功 JSON 統計
	 *   Given 使用者上傳完全合格的訂單 Excel 檔案
	 *     And UseCase 回傳空位元組陣列 (代表無錯誤)
	 *     And UseCase 檢核業務案例回傳完全合格結果
	 *    When 發送 POST /api/v1/excel/validation/orders/feedback 請求
	 *    Then 應回傳 HTTP 200 OK
	 *     And 回應主體應為 JSON 格式且 success=true
	 */
	@Test
	@DisplayName("上傳完全合格之訂單至 feedback 端點時應回傳 HTTP 200 與成功 JSON 報告")
	void shouldReturnJsonReportWhenValidatingOrdersWithFeedbackAndNoErrorsExist() throws Exception {
		// ==========================================
		// [Given] 測試資料與 Mock 行為準備
		// ==========================================
		// 1. 模擬前端上傳之合格訂單 Excel
		MockMultipartFile file = new MockMultipartFile(
				"file", 
				"perfect_orders.xlsx",
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", 
				new byte[]{1, 2, 3}
		);

		// 2. 模擬回標生成方法回傳空陣列 (長度為 0 代表全數合格，無須標註產生回標檔)
		when(validateExcelUseCase.validateAndGenerateFeedback(any(InputStream.class), eq(OrderModel.class)))
				.thenReturn(new byte[0]);

		// 3. 模擬二次校驗回傳合格模型清單
		OrderModel o1 = new OrderModel("ORD-001", "張三", new BigDecimal("200.00"), 2, LocalDate.now(), "PAID");
		ExcelValidationGottenResult<OrderModel> mockResult = ExcelValidationGottenResult.success(List.of(o1));
		when(validateExcelUseCase.validate(any(InputStream.class), eq(OrderModel.class))).thenReturn(mockResult);

		// ==========================================
		// [When & Then] 執行請求並驗證回傳 JSON 統計報告
		// ==========================================
		mockMvc.perform(multipart("/api/v1/excel/validation/orders/feedback")
						.file(file))
				// 驗證 HTTP 狀態碼為 200 OK
				.andExpect(status().isOk())
				// 驗證 Content-Type 為 JSON (非二進位檔案下載)
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				// 驗證 success 為 true
				.andExpect(jsonPath("$.success").value(true))
				// 驗證錯誤數量為 0
				.andExpect(jsonPath("$.errorCount").value(0));
	}

	/**
	 * Feature: 上傳空檔案防呆檢核
	 * 
	 * Scenario: 當上傳的檔案為空時，API 應回傳 HTTP 400 Bad Request
	 *   Given 空的 MultipartFile
	 *    When 發送 POST /api/v1/excel/validation/orders 請求
	 *    Then 應回傳 HTTP 400 Bad Request
	 */
	@Test
	@DisplayName("上傳檔案為空時應回傳 HTTP 400 Bad Request")
	void shouldReturn400WhenFileIsEmpty() throws Exception {
		// ==========================================
		// [Given] 構造 0 位元組的空 MultipartFile (模擬未選擇檔案或空內容)
		// ==========================================
		MockMultipartFile emptyFile = new MockMultipartFile(
				"file",                                // 請求參數名稱
				"",                                    // 空檔名
				"text/plain",                          // 任意 MIME 類型
				new byte[0]                            // 0 位元組空陣列
		);

		// ==========================================
		// [When & Then] 發送空檔案請求，預期控制器於第一道關卡直接攔截防呆
		// ==========================================
		mockMvc.perform(multipart("/api/v1/excel/validation/orders")
						.file(emptyFile))
				// 驗證 HTTP 狀態碼為 400 Bad Request (請求參數無效)
				.andExpect(status().isBadRequest());
	}

	/**
	 * Feature: Java 21 Record 商品主檔檢核
	 * 
	 * Scenario: 上傳合格商品主檔 Record Excel 檔案，應回傳 HTTP 200 OK
	 *   Given 包含合法 ProductRecordModel 之 Excel 檔案
	 *    When 發送 POST /api/v1/excel/validation/products 請求
	 *    Then 應回傳 HTTP 200 OK 且 success=true
	 */
	@Test
	@DisplayName("上傳合法商品主檔 Record 時應成功回傳 HTTP 200")
	void shouldValidateProductsSuccessfully() throws Exception {
		// ==========================================
		// [Given] 測試資料與 Mock 行為準備
		// ==========================================
		// 1. 模擬商品主檔 Excel 上傳檔案
		MockMultipartFile file = new MockMultipartFile(
				"file", 
				"products.xlsx",
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", 
				new byte[]{1, 2, 3}
		);

		// 2. 建立不可變 Java 21 Record 模型實例 (驗證規範建構子 Canonical Constructor 映射)
		ProductRecordModel p1 = new ProductRecordModel(
				"PRD-1",                       // 商品料號
				"筆記型電腦",                   // 商品名稱
				new BigDecimal("35000.00"),    // 正數售價
				true                           // 在售狀態
		);

		// 3. 封裝應用層檢核通過結果
		ExcelValidationGottenResult<ProductRecordModel> mockResult = ExcelValidationGottenResult.success(List.of(p1));

		// 4. 定義 UseCase Mock 行為：傳入 ProductRecordModel.class 時返回合格結果
		when(validateExcelUseCase.validate(any(InputStream.class), eq(ProductRecordModel.class))).thenReturn(mockResult);

		// ==========================================
		// [When & Then] 執行 HTTP POST 請求並斷言 Record 檢核統計回應
		// ==========================================
		mockMvc.perform(multipart("/api/v1/excel/validation/products") // 呼叫商品專用檢核端點
						.file(file))                                  // 附加檔案
				// 驗證 HTTP 狀態碼為 200 OK
				.andExpect(status().isOk())
				// 驗證 success 為 true
				.andExpect(jsonPath("$.success").value(true))
				// 驗證總解析列數為 1 筆
				.andExpect(jsonPath("$.totalRows").value(1));
	}

	/**
	 * Feature: 宣告式 @ValidateExcel 切面短路回標攔截
	 * 
	 * Scenario: 透過宣告式端點上傳包含錯誤之訂單，AOP 切面直接短路並回傳錯誤回標 Excel
	 *   Given 含有錯誤之訂單 Excel
	 *     And UseCase 回傳非空之回標二進位位元組陣列
	 *    When 發送 POST /api/v1/excel/validation/declarative/orders
	 *    Then 應回傳 HTTP 200 OK
	 *     And Content-Disposition 應包含 "AOP訂單檢核錯誤回標檔.xlsx"
	 *     And 回應位元組與 Mock 回標資料完全一致
	 */
	@Test
	@DisplayName("宣告式端點檢核違規時，AOP 切面應直接短路回傳錯誤回標 Excel 下載")
	void shouldShortCircuitWithFeedbackFileWhenDeclarativeValidationFails() throws Exception {
		// Given
		MockMultipartFile file = new MockMultipartFile("file", "bad_orders.xlsx",
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1, 2, 3});

		byte[] mockFeedbackExcelBytes = new byte[]{80, 75, 3, 4};
		when(validateExcelUseCase.validateAndGenerateFeedback(any(InputStream.class), eq(OrderModel.class)))
				.thenReturn(mockFeedbackExcelBytes);

		// When & Then
		mockMvc.perform(multipart("/api/v1/excel/validation/declarative/orders").file(file))
				.andExpect(status().isOk())
				.andExpect(header().exists("Content-Disposition"))
				.andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				.andExpect(content().bytes(mockFeedbackExcelBytes));
	}

	/**
	 * Feature: 宣告式 @ValidateExcel 切面放行與資料自動注入
	 * 
	 * Scenario: 透過宣告式端點上傳合格訂單，AOP 切面放行並將 @ValidatedExcelData 自動注入方法
	 *   Given 完全合格之訂單 Excel
	 *     And UseCase 回傳合格模型清單
	 *    When 發送 POST /api/v1/excel/validation/declarative/orders
	 *    Then 應回傳 HTTP 200 OK
	 *     And success=true
	 *     And totalRows=1
	 */
	@Test
	@DisplayName("宣告式端點檢核合格時，AOP 切面放行並自動注入合格模型清單")
	void shouldProceedAndInjectValidDataWhenDeclarativeValidationSucceeds() throws Exception {
		// Given
		MockMultipartFile file = new MockMultipartFile("file", "good_orders.xlsx",
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1, 2, 3});

		when(validateExcelUseCase.validateAndGenerateFeedback(any(InputStream.class), eq(OrderModel.class)))
				.thenReturn(new byte[0]); // 代表完全合格無錯誤

		OrderModel o1 = new OrderModel("ORD-001", "張三", new BigDecimal("100.00"), 1, LocalDate.now(), "PAID");
		ExcelValidationGottenResult<OrderModel> mockResult = ExcelValidationGottenResult.success(List.of(o1));
		when(validateExcelUseCase.validate(any(InputStream.class), eq(OrderModel.class))).thenReturn(mockResult);

		// When & Then
		mockMvc.perform(multipart("/api/v1/excel/validation/declarative/orders").file(file))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.totalRows").value(1))
				.andExpect(jsonPath("$.validCount").value(1));
	}

	/**
	 * Feature: 查詢系統可用範本清單
	 * 
	 * Scenario: 呼叫 GET /api/v1/excel/validation/templates，應回傳包含所有已註冊範本之清單
	 *   Given 控制器已載入包含 orders, products, materials, boms 之註冊表
	 *    When 發送 GET /api/v1/excel/validation/templates 請求
	 *    Then 應回傳 HTTP 200 OK
	 *     And JSON 陣列長度大於等於 4
	 *     And 應包含 "orders", "products" 等關鍵鍵值
	 */
	@Test
	@DisplayName("查詢可用範本清單時應成功回傳 HTTP 200 與範本描述符清單")
	void shouldReturnAllRegisteredTemplates() throws Exception {
		// When & Then
		mockMvc.perform(get("/api/v1/excel/validation/templates"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
				.andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(4)))
				.andExpect(jsonPath("$[*].key").value(org.hamcrest.Matchers.hasItems("orders", "products", "materials", "boms")));
	}
}

