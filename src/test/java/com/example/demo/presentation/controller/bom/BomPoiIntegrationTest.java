package com.example.demo.presentation.controller.bom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.example.demo.application.port.out.core.ExcelReaderPort;
import com.example.demo.application.port.out.core.ExcelWriterPort;
import com.example.demo.application.shared.dto.ExcelGottenResult;
import com.example.demo.application.shared.dto.ExcelSheetGottenResult;
import com.example.demo.infrastructure.adapter.core.PoiSaxReaderAdapter;
import com.example.demo.infrastructure.adapter.core.PoiSxssfWriterAdapter;

/**
 * Apache POI 引擎整合測試 (BomPoiIntegrationTest)
 * <p>
 * 遵循 Clean Architecture 表現層與基礎設施整合測試規範，驗證在 {@code excel.engine=APACHE_POI}
 * 旗標啟用下，完整 REST API 請求能成功透過 Apache POI (SXSSF) 生成 Excel 檔案，並透過 Apache POI
 * Reader 解析。
 */
@SpringBootTest
@TestPropertySource(properties = "excel.engine=APACHE_POI")
@DisplayName("Apache POI 引擎模式下的 BOM 匯出整合測試")
class BomPoiIntegrationTest {

	@Autowired
	private WebApplicationContext webApplicationContext;

	private MockMvc mockMvc;

	@Autowired
	private ExcelWriterPort excelWriterPort;

	@Autowired
	private ExcelReaderPort excelReaderPort;

	@BeforeEach
	void setUp() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
	}

	/**
	 * Feature: 驗證當前啟用的引擎實體型別為 Apache POI
	 * Scenario: 透過 @TestPropertySource 指定 excel.engine=APACHE_POI
	 * Given: 容器已初始化
	 * When: 檢驗注入之 ExcelWriterPort 與 ExcelReaderPort
	 * Then: 應精確為 PoiSxssfWriterAdapter 與 PoiSaxReaderAdapter
	 */
	@Test
	@DisplayName("整合：驗證成功切換為 Apache POI 適配器實例")
	void shouldVerifyActiveAdaptersArePoi() {
		assertThat(excelWriterPort).isInstanceOf(PoiSxssfWriterAdapter.class);
		assertThat(excelReaderPort).isInstanceOf(PoiSaxReaderAdapter.class);
	}

	/**
	 * Feature: 使用 Apache POI 下載多階層 BOM Excel
	 * Scenario: 發送 GET /api/v1/bom/excel?parentMaterialCode=MAT-FG-001
	 * Given: 資料庫具備 MAT-FG-001 完整物料與 BOM 資料
	 * When: 發送 GET 請求
	 * Then: 回應 200 OK，使用 Apache POI Reader 解析產生之二進位內容，確認包含 3 工作表與 2000 筆主檔
	 */
	@Test
	@DisplayName("整合：透過 Apache POI 產製並下載 MAT-FG-001 BOM Excel")
	void shouldDownloadBomExcelUsingPoiSuccessfully() throws Exception {
		MvcResult mvcResult = mockMvc.perform(get("/api/v1/bom/excel")
				.param("parentMaterialCode", "MAT-FG-001"))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				.andExpect(header().string("Content-Disposition", "attachment; filename*=UTF-8''BOM_MAT-FG-001.xlsx"))
				.andReturn();

		byte[] excelBytes = mvcResult.getResponse().getContentAsByteArray();
		assertThat(excelBytes).isNotEmpty();

		ExcelGottenResult gotten = excelReaderPort.read(excelBytes);
		assertThat(gotten.getSheets()).hasSize(3);
		assertThat(gotten.getSheets().get(0).getSheetName()).isEqualTo("多階層BOM展開清單");
		assertThat(gotten.getSheets().get(1).getSheetName()).isEqualTo("單階BOM匯入清單");
		assertThat(gotten.getSheets().get(2).getSheetName()).isEqualTo("物料主檔對照表");

		// 驗證 2000 筆物料主檔對照表完整寫入與讀出
		ExcelSheetGottenResult sheet3 = gotten.getSheets().get(2);
		assertThat(sheet3.getRows()).hasSize(2000);
	}

	/**
	 * Feature: 使用 Apache POI 執行大數據串流輸出
	 * Scenario: 發送 GET
	 * /api/v1/bom/excel/stream?parentMaterialCode=MAT-FG-001&flushInterval=100
	 * Given: 啟用 Apache POI SXSSF 串流寫入
	 * When: 發送 GET 串流請求
	 * Then: 回應 200 OK，驗證 Excel 二進位內容完整性
	 */
	@Test
	@DisplayName("整合：透過 Apache POI SXSSF 邊寫入邊 Flush 串流下載 BOM Excel")
	void shouldStreamDownloadBomExcelUsingPoiSuccessfully() throws Exception {
		MvcResult mvcResult = mockMvc.perform(get("/api/v1/bom/excel/stream")
				.param("parentMaterialCode", "MAT-FG-001")
				.param("flushInterval", "100"))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				.andExpect(header().string("Content-Disposition", "attachment; filename*=UTF-8''BOM_MAT-FG-001.xlsx"))
				.andReturn();

		byte[] excelBytes = mvcResult.getResponse().getContentAsByteArray();
		assertThat(excelBytes).isNotEmpty();

		ExcelGottenResult gotten = excelReaderPort.read(excelBytes);
		assertThat(gotten.getSheets()).hasSize(3);
	}
}
