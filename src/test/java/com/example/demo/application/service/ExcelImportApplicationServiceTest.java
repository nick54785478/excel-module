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
import java.util.Collections;
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

@DisplayName("ExcelImportApplicationService 匯入業務服務測試")
class ExcelImportApplicationServiceTest {

	private ExcelReaderPort excelReaderPort;
	private ExcelImportApplicationService importService;

	@BeforeEach
	void setUp() {
		excelReaderPort = mock(ExcelReaderPort.class);
		importService = new ExcelImportApplicationService(excelReaderPort);
	}

	/**
	 * Feature: Excel 檔案解析匯入業務
	 * Scenario: 成功讀取活頁簿資料結構
	 * Given 建立 InputStream 與 ReadExcelQuery
	 * When 調用 importService.importExcel(inputStream, query)
	 * Then 應正確委託 excelReaderPort.read 並回傳活頁簿結果物件
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
		ExcelGottenResult result = importService.importExcel(inputStream, query);

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
	 * Given 建立 StreamImportExcelCommand
	 * When 調用 importService.streamImport(command)
	 * Then 應正確觸發分塊消費回呼，並回傳正確之統計摘要
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
		ExcelStreamImportGottenResult result = importService.streamImport(command);

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
	 * Scenario: Application Service 委託 Reader 取得 ExcelGottenResult 並調用 Assembler 轉為強型別 Model 清單
	 * Given 建立測試 Model 與 Mock 讀取結果
	 * When 調用 importService.importBeans(inputStream, ModelClass)
	 * Then 應正確回傳強型別 Model 清單
	 */
	@Test
	@DisplayName("場景：成功透過 ApplicationService 調用 Assembler 解析為 Model 清單")
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
		List<SimpleItem> items = importService.importBeans(inputStream, SimpleItem.class);

		// Then
		assertThat(items).hasSize(1);
		assertThat(items.get(0).code()).isEqualTo("ITEM-1");
		assertThat(items.get(0).name()).isEqualTo("TestName");
		verify(excelReaderPort).read(eq(inputStream), any(ReadExcelQuery.class));
	}
}
