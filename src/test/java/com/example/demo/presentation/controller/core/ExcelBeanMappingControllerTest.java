package com.example.demo.presentation.controller.core;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.demo.application.port.in.core.ExportExcelUseCase;
import com.example.demo.application.port.in.core.ImportExcelUseCase;
import com.example.demo.application.port.in.core.StreamExportExcelUseCase;
import com.example.demo.application.shared.model.demo.OrderModel;
import com.example.demo.application.shared.model.demo.ProductRecordModel;
import com.example.demo.presentation.assembler.ExcelBeanMappingResourceAssembler;
import com.example.demo.presentation.exception.GlobalExceptionHandler;

/**
 * 宣告式物件映射控制器測試 (ExcelBeanMappingController Test)
 * <p>
 * 遵循 BDD 與 Gherkin 規範，驗證維度一「宣告式物件映射」之 HTTP 端點：
 * 包含 JavaBean 與 Java 21 Record 雙向流轉、自訂匯出、大數據串流輸出與檔案解析匯入。
 */
@DisplayName("ExcelBeanMappingController 宣告式物件映射控制器測試")
class ExcelBeanMappingControllerTest {

	private MockMvc mockMvc;
	private ExportExcelUseCase exportExcelUseCase;
	private StreamExportExcelUseCase streamExportExcelUseCase;
	private ImportExcelUseCase importExcelUseCase;

	@BeforeEach
	void setUp() {
		exportExcelUseCase = mock(ExportExcelUseCase.class);
		streamExportExcelUseCase = mock(StreamExportExcelUseCase.class);
		importExcelUseCase = mock(ImportExcelUseCase.class);
		ExcelBeanMappingResourceAssembler resourceAssembler = new ExcelBeanMappingResourceAssembler();

		ExcelBeanMappingController controller = new ExcelBeanMappingController(
				exportExcelUseCase,
				streamExportExcelUseCase,
				importExcelUseCase,
				resourceAssembler
		);

		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	/**
	 * Feature: 訂單資料宣告式匯出參數校驗 (JavaBean POST)
	 * Scenario: 當前端傳送空訂單清單時，觸發 @NotEmpty 校驗失敗
	 * Given 傳入包含空訂單陣列的 JSON Payload
	 * When 發送 POST /api/v1/excel/bean-mapping/orders/export 請求
	 * Then 應回傳 HTTP 400 Bad Request
	 */
	@Test
	@DisplayName("場景：POST 匯出訂單時訂單清單為空應拒絕並回傳 400 (JavaBean)")
	void shouldRejectExportOrdersWhenOrdersIsEmpty() throws Exception {
		String invalidJson = """
				{
				  "sheetName": "特約客戶訂單",
				  "orders": []
				}
				""";

		mockMvc.perform(post("/api/v1/excel/bean-mapping/orders/export")
						.contentType(MediaType.APPLICATION_JSON)
						.content(invalidJson))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));
	}

	/**
	 * Feature: 自訂訂單資料宣告式匯出 (JavaBean POST)
	 * Scenario: 提供自訂訂單 JSON 資料並匯出為 Excel 檔案
	 * Given 傳入包含自訂工作表與訂單項目的 JSON Payload
	 * When 發送 POST /api/v1/excel/bean-mapping/orders/export 請求
	 * Then 應調用 exportBeans 並回傳 Excel 二進位檔案
	 */
	@Test
	@DisplayName("場景：成功透過 POST 依自訂 JSON 匯出訂單 Excel (JavaBean)")
	void shouldExportCustomOrdersSuccessfully() throws Exception {
		// Given
		byte[] mockBytes = "CUSTOM_ORDER_EXCEL".getBytes();
		when(exportExcelUseCase.exportBeans(any(), eq(OrderModel.class), eq("特約客戶訂單"))).thenReturn(mockBytes);

		String requestJson = """
				{
				  "sheetName": "特約客戶訂單",
				  "orders": [
				    {
				      "orderNo": "VIP-001",
				      "customerName": "陳董",
				      "amount": 99999.00,
				      "quantity": 10,
				      "orderDate": "2026-11-01",
				      "status": "PAID"
				    }
				  ]
				}
				""";

		// When & Then
		mockMvc.perform(post("/api/v1/excel/bean-mapping/orders/export")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestJson))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				.andExpect(content().bytes(mockBytes));

		verify(exportExcelUseCase).exportBeans(any(), eq(OrderModel.class), eq("特約客戶訂單"));
	}

	/**
	 * Feature: 訂單資料大數據串流匯出 (JavaBean Stream POST)
	 * Scenario: 成功將 OrderModel 清單直接沖刷至 HTTP 輸出流中
	 * Given 準備匯出請求 JSON 載體
	 * When 發送 POST /api/v1/excel/bean-mapping/orders/stream-export 請求
	 * Then 應調用 streamExportExcelUseCase.streamExportBeans 並回傳 HTTP 200
	 */
	@Test
	@DisplayName("場景：成功透過 POST 串流匯出訂單 Excel (JavaBean)")
	void shouldStreamExportOrdersSuccessfully() throws Exception {
		// Given
		String requestJson = """
				{
				  "sheetName": "串流訂單清單",
				  "orders": [
				    {
				      "orderNo": "ORD-STR-01",
				      "customerName": "陳董",
				      "amount": 55000.00,
				      "quantity": 5,
				      "orderDate": "2026-11-01",
				      "status": "PAID"
				    }
				  ]
				}
				""";

		// When & Then
		mockMvc.perform(post("/api/v1/excel/bean-mapping/orders/stream-export")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestJson))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Disposition", "attachment; filename*=UTF-8''orders_stream_export.xlsx"));

		verify(streamExportExcelUseCase).streamExportBeans(any(OutputStream.class), any(), eq(OrderModel.class), eq("串流訂單清單"));
	}

	/**
	 * Feature: 訂單 Excel 檔案上傳解析 (JavaBean Import POST)
	 * Scenario: 上傳 Excel 檔案並自動映射為 OrderModel 清單
	 * Given 建立 MockMultipartFile 與 Mock 解析模型清單
	 * When 發送 POST /api/v1/excel/bean-mapping/orders/import 請求
	 * Then 應回傳 HTTP 200、totalCount 為 1、包含訂單編號與欄位明細之 JSON 回應
	 */
	@Test
	@DisplayName("場景：成功上傳 Excel 並宣告式解析為 OrderModel 清單 (JavaBean)")
	void shouldImportOrdersSuccessfully() throws Exception {
		// Given
		MockMultipartFile file = new MockMultipartFile(
				"file", "orders.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				"FAKE_EXCEL_CONTENT".getBytes()
		);

		List<OrderModel> mockOrders = List.of(
				new OrderModel("ORD-888", "王小明", new BigDecimal("12500.50"), 2, LocalDate.of(2026, 11, 1), "PAID")
		);
		when(importExcelUseCase.importBeans(any(InputStream.class), eq(OrderModel.class))).thenReturn(mockOrders);

		// When & Then
		mockMvc.perform(multipart("/api/v1/excel/bean-mapping/orders/import").file(file))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalCount").value(1))
				.andExpect(jsonPath("$.sheetName").value("訂單匯入清單"))
				.andExpect(jsonPath("$.items[0].orderNo").value("ORD-888"))
				.andExpect(jsonPath("$.items[0].customerName").value("王小明"))
				.andExpect(jsonPath("$.items[0].amount").value(12500.50))
				.andExpect(jsonPath("$.items[0].quantity").value(2))
				.andExpect(jsonPath("$.items[0].status").value("PAID"));

		verify(importExcelUseCase).importBeans(any(InputStream.class), eq(OrderModel.class));
	}

	/**
	 * Feature: 商品資料宣告式匯出參數校驗 (Java 21 Record POST)
	 * Scenario: 當前端傳送空商品清單時，觸發 @NotEmpty 校驗失敗
	 * Given 傳入包含空商品陣列的 JSON Payload
	 * When 發送 POST /api/v1/excel/bean-mapping/products/export 請求
	 * Then 應回傳 HTTP 400 Bad Request
	 */
	@Test
	@DisplayName("場景：POST 匯出商品時商品清單為空應拒絕並回傳 400 (Java 21 Record)")
	void shouldRejectExportProductsWhenProductsIsEmpty() throws Exception {
		String invalidJson = """
				{
				  "sheetName": "商品清單",
				  "products": []
				}
				""";

		mockMvc.perform(post("/api/v1/excel/bean-mapping/products/export")
						.contentType(MediaType.APPLICATION_JSON)
						.content(invalidJson))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));
	}

	/**
	 * Feature: 自訂商品資料宣告式匯出 (Java 21 Record POST)
	 * Scenario: 提供自訂商品 JSON 資料並匯出為 Excel 檔案
	 * Given 傳入自訂商品 JSON Payload
	 * When 發送 POST /api/v1/excel/bean-mapping/products/export 請求
	 * Then 應調用 exportBeans 並回傳 Excel 二進位檔案
	 */
	@Test
	@DisplayName("場景：成功透過 POST 依自訂 JSON 匯出商品 Excel (Java 21 Record)")
	void shouldExportCustomProductsSuccessfully() throws Exception {
		// Given
		byte[] mockBytes = "CUSTOM_PRODUCT_EXCEL".getBytes();
		when(exportExcelUseCase.exportBeans(any(), eq(ProductRecordModel.class), eq("旗艦商品清單"))).thenReturn(mockBytes);

		String requestJson = """
				{
				  "sheetName": "旗艦商品清單",
				  "products": [
				    {
				      "productCode": "PROD-999",
				      "productName": "頂級旗艦無人機",
				      "price": 88000.00,
				      "onSale": true
				    }
				  ]
				}
				""";

		// When & Then
		mockMvc.perform(post("/api/v1/excel/bean-mapping/products/export")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestJson))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				.andExpect(content().bytes(mockBytes));

		verify(exportExcelUseCase).exportBeans(any(), eq(ProductRecordModel.class), eq("旗艦商品清單"));
	}

	/**
	 * Feature: 商品主檔 Excel 檔案上傳解析 (Java 21 Record Import POST)
	 * Scenario: 上傳 Excel 檔案並自動映射為不可變 ProductRecordModel 清單
	 * Given 建立 MockMultipartFile 與 Mock 解析模型清單
	 * When 發送 POST /api/v1/excel/bean-mapping/products/import 請求
	 * Then 應回傳 HTTP 200、totalCount 為 1、包含商品代碼與售價之 JSON 回應
	 */
	@Test
	@DisplayName("場景：成功上傳 Excel 並宣告式解析為 ProductRecordModel 清單 (Java 21 Record)")
	void shouldImportProductsSuccessfully() throws Exception {
		// Given
		MockMultipartFile file = new MockMultipartFile(
				"file", "products.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				"FAKE_EXCEL_CONTENT".getBytes()
		);

		List<ProductRecordModel> mockProducts = List.of(
				new ProductRecordModel("PROD-001", "4K 專業航拍無人機", new BigDecimal("35000.00"), true)
		);
		when(importExcelUseCase.importBeans(any(InputStream.class), eq(ProductRecordModel.class))).thenReturn(mockProducts);

		// When & Then
		mockMvc.perform(multipart("/api/v1/excel/bean-mapping/products/import").file(file))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalCount").value(1))
				.andExpect(jsonPath("$.sheetName").value("商品主檔清單"))
				.andExpect(jsonPath("$.items[0].productCode").value("PROD-001"))
				.andExpect(jsonPath("$.items[0].productName").value("4K 專業航拍無人機"))
				.andExpect(jsonPath("$.items[0].price").value(35000.00))
				.andExpect(jsonPath("$.items[0].onSale").value(true));

		verify(importExcelUseCase).importBeans(any(InputStream.class), eq(ProductRecordModel.class));
	}

	/**
	 * Feature: 上傳空檔案異常校驗
	 * Scenario: 使用者上傳空檔案時應拒絕請求並拋出 IllegalArgumentException
	 * Given 建立空檔案 MockMultipartFile
	 * When 發送 POST /api/v1/excel/bean-mapping/orders/import 請求
	 * Then 應攔截並回傳 HTTP 400 Bad Request
	 */
	@Test
	@DisplayName("場景：上傳空檔案時應拒絕並回傳 400 錯誤")
	void shouldRejectEmptyFileOnImport() throws Exception {
		// Given
		MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.xlsx", "text/plain", new byte[0]);

		// When & Then
		mockMvc.perform(multipart("/api/v1/excel/bean-mapping/orders/import").file(emptyFile))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("上傳之 Excel 檔案不能為空"));
	}
}
