package com.example.demo.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.demo.application.port.out.ExcelReaderPort;
import com.example.demo.application.shared.command.StreamImportExcelCommand;
import com.example.demo.application.shared.dto.ExcelGottenResult;
import com.example.demo.application.shared.dto.ExcelSheetGottenResult;
import com.example.demo.application.shared.dto.ExcelStreamImportGottenResult;
import com.example.demo.application.shared.query.ReadExcelQuery;
import com.example.demo.application.shared.stream.ExcelChunkConsumer;

/**
 * Excel 匯入查詢應用服務單元測試 (ExcelQueryService Test)
 * <p>
 * 針對 {@link ExcelQueryService} 驗證 Clean Architecture / CQRS 查詢端業務邏輯、
 * 宣告式物件映射與大數據串流分塊讀取之調度行為。
 * 遵循 BDD / Gherkin 規範。
 */
@DisplayName("ExcelQueryService 匯入查詢業務服務測試")
class ExcelQueryServiceTest {

	private ExcelReaderPort excelReaderPort;
	private ExcelQueryService queryService;

	@BeforeEach
	void setUp() {
		excelReaderPort = mock(ExcelReaderPort.class);
		queryService = new ExcelQueryService(excelReaderPort);
	}

	/**
	 * Feature: Excel 檔案解析匯入業務
	 * Scenario: 成功讀取活頁簿資料結構
	 * Given: 建立 InputStream 與 ReadExcelQuery
	 * When: 調用 queryService.importExcel(inputStream, query)
	 * Then: 應正確委託 excelReaderPort.read 並回傳活頁簿結果物件
	 */
	@Test
	@DisplayName("場景：成功解析 Excel 內容為 ExcelGottenResult")
	void shouldImportExcelSuccessfully() {
		// Given
		InputStream inputStream = new ByteArrayInputStream(new byte[0]);
		ReadExcelQuery query = ReadExcelQuery.defaultQuery();

		ExcelSheetGottenResult sheetResult = new ExcelSheetGottenResult(0, "Sheet1", List.of("ID", "Name"), List.of(List.of(1, "Bob")), List.of(List.of("1", "Bob")));
		ExcelGottenResult mockResult = new ExcelGottenResult(List.of(sheetResult));
		when(excelReaderPort.read(any(InputStream.class), any(ReadExcelQuery.class))).thenReturn(mockResult);

		// When
		ExcelGottenResult result = queryService.importExcel(inputStream, query);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getSheetCount()).isEqualTo(1);
		assertThat(result.getFirstSheet()).isPresent();
		assertThat(result.getFirstSheet().get().getSheetName()).isEqualTo("Sheet1");
		verify(excelReaderPort).read(eq(inputStream), eq(query));
	}

	/**
	 * Feature: Excel 串流分塊批次匯入業務
	 * Scenario: 成功調用 readChunked 並統計分塊與總筆數
	 * Given: 建立 StreamImportExcelCommand
	 * When: 調用 queryService.streamImport(command)
	 * Then: 應正確觸發分塊消費回呼，並回傳正確之統計摘要
	 */
	@Test
	@DisplayName("場景：成功執行串流分塊解析並產出統計報告")
	void shouldStreamImportExcelSuccessfully() {
		// Given
		InputStream inputStream = new ByteArrayInputStream(new byte[0]);
		StreamImportExcelCommand command = new StreamImportExcelCommand(inputStream, "DataSheet", 100);

		doAnswer(invocation -> {
			ExcelChunkConsumer consumer = invocation.getArgument(3);
			// 模擬觸發兩個 chunk
			consumer.accept(List.of(Map.of("A", 1), Map.of("A", 2)), null);
			consumer.accept(List.of(Map.of("A", 3)), null);
			return null;
		}).when(excelReaderPort).readChunked(any(InputStream.class), any(ReadExcelQuery.class), anyInt(), any(ExcelChunkConsumer.class));

		// When
		ExcelStreamImportGottenResult result = queryService.streamImport(command);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.sheetName()).isEqualTo("DataSheet");
		assertThat(result.totalRowsProcessed()).isEqualTo(3);
		assertThat(result.totalChunks()).isEqualTo(2);
		assertThat(result.status()).isEqualTo("SUCCESS");
		verify(excelReaderPort).readChunked(any(InputStream.class), any(ReadExcelQuery.class), eq(100), any(ExcelChunkConsumer.class));
	}

	/**
	 * Feature: 宣告式 Model 匯入業務
	 * Scenario: Query Service 委託 Reader 取得 ExcelGottenResult 並調用 Assembler 轉為強型別 Model 清單
	 * Given: 建立測試 Model 與 Mock 讀取結果
	 * When: 調用 queryService.importBeans(inputStream, ModelClass)
	 * Then: 應正確回傳強型別 Model 清單
	 */
	@Test
	@DisplayName("場景：成功透過 QueryService 調用 Assembler 解析為 Model 清單")
	void shouldImportBeansSuccessfully() {
		// Given
		InputStream inputStream = new ByteArrayInputStream(new byte[0]);
		ExcelSheetGottenResult sheet = new ExcelSheetGottenResult(0, "Sheet1",
				List.of("code", "name"),
				List.of(List.of("ITEM-1", "TestName")),
				List.of(List.of("ITEM-1", "TestName")));
		when(excelReaderPort.read(any(InputStream.class), any(ReadExcelQuery.class)))
				.thenReturn(new ExcelGottenResult(List.of(sheet)));

		record SimpleItem(String code, String name) {}

		// When
		List<SimpleItem> items = queryService.importBeans(inputStream, SimpleItem.class);

		// Then
		assertThat(items).hasSize(1);
		assertThat(items.get(0).code()).isEqualTo("ITEM-1");
		assertThat(items.get(0).name()).isEqualTo("TestName");
		verify(excelReaderPort).read(eq(inputStream), any(ReadExcelQuery.class));
	}

	/**
	 * Feature: Excel 首個工作表匯入業務
	 * Scenario: 成功讀取第一個工作表純資料載體
	 * Given: 建立 InputStream 與 Mock 讀取結果
	 * When: 調用 queryService.importFirstSheet(inputStream)
	 * Then: 應正確委託 excelReaderPort.readFirstSheet 並回傳 ExcelSheetGottenResult
	 */
	@Test
	@DisplayName("場景：成功讀取第一個工作表純資料載體")
	void shouldImportFirstSheetSuccessfully() {
		// Given
		InputStream inputStream = new ByteArrayInputStream(new byte[0]);
		ExcelSheetGottenResult mockSheet = new ExcelSheetGottenResult(0, "Sheet1", List.of("Col1"), List.of(List.of("Val1")), List.of(List.of("Val1")));
		when(excelReaderPort.readFirstSheet(any(InputStream.class))).thenReturn(mockSheet);

		// When
		ExcelSheetGottenResult result = queryService.importFirstSheet(inputStream);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getSheetName()).isEqualTo("Sheet1");
		assertThat(result.getRowCount()).isEqualTo(1);
		verify(excelReaderPort).readFirstSheet(eq(inputStream));
	}
}

