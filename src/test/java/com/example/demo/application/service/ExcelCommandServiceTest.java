package com.example.demo.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.demo.application.port.out.ExcelWriterPort;
import com.example.demo.application.shared.command.ExportExcelCommand;
import com.example.demo.application.shared.command.StreamExportExcelCommand;
import com.example.demo.application.shared.command.WriteExcelCommand;
import com.example.demo.application.shared.command.WriteSheetCommand;
import com.example.demo.application.shared.command.WriteStreamExcelCommand;
import com.example.demo.application.shared.command.WriteStreamSheetCommand;

/**
 * Excel 匯出命令應用服務單元測試 (ExcelCommandService Test)
 * <p>
 * 針對 {@link ExcelCommandService} 驗證 Clean Architecture / CQRS 命令端業務邏輯、
 * 宣告式物件映射與大數據串流輸出之調度行為。
 * 遵循 BDD / Gherkin 規範。
 */
@DisplayName("ExcelCommandService 匯出命令業務服務測試")
class ExcelCommandServiceTest {

	private ExcelWriterPort excelWriterPort;
	private ExcelCommandService commandService;

	@BeforeEach
	void setUp() {
		excelWriterPort = mock(ExcelWriterPort.class);
		commandService = new ExcelCommandService(excelWriterPort);
	}

	/**
	 * Feature: Excel 檔案命令式匯出業務
	 * Scenario: 成功調用 WriteExcelCommand 匯出二進位檔案
	 * Given: 建立一個包含工作表的 ExportExcelCommand
	 * When: 調用 commandService.export(command)
	 * Then: 應正確委託 excelWriterPort.write 並回傳生成的 byte[]
	 */
	@Test
	@DisplayName("場景：成功匯出 Excel 二進位資料")
	void shouldExportExcelSuccessfully() {
		// Given
		byte[] mockBytes = new byte[] { 1, 2, 3, 4 };
		when(excelWriterPort.write(any(WriteExcelCommand.class))).thenReturn(mockBytes);

		WriteSheetCommand sheet = WriteSheetCommand.of("Sheet1", List.of("ID", "Name"), List.of(List.of(1, "Alice")));
		ExportExcelCommand command = new ExportExcelCommand("orders.xlsx", List.of(sheet));

		// When
		byte[] result = commandService.export(command);

		// Then
		assertThat(result).isEqualTo(mockBytes);
		verify(excelWriterPort).write(any(WriteExcelCommand.class));
	}

	/**
	 * Feature: Excel 大數據串流匯出業務
	 * Scenario: 成功調用 WriteStreamExcelCommand 進行串流寫入
	 * Given: 建立包含資料流的 StreamExportExcelCommand 與輸出串流
	 * When: 調用 commandService.streamExport(outputStream, command)
	 * Then: 應正確委託 excelWriterPort.writeStream
	 */
	@Test
	@DisplayName("場景：成功執行串流式資料匯出至 OutputStream")
	void shouldStreamExportExcelSuccessfully() {
		// Given
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		WriteStreamSheetCommand streamSheet = WriteStreamSheetCommand.builder()
				.sheetName("BigDataSheet")
				.headers(List.of("ID", "Metric"))
				.rowIterable(List.of(List.of(101, "M1")))
				.flushInterval(500)
				.build();

		StreamExportExcelCommand command = new StreamExportExcelCommand("big_data.xlsx", List.of(streamSheet), 500);

		// When
		commandService.streamExport(outputStream, command);

		// Then
		verify(excelWriterPort).writeStream(any(), any(WriteStreamExcelCommand.class));
	}

	/**
	 * Feature: 宣告式 Model 匯出業務
	 * Scenario: Command Service 調用 Assembler 將強型別 Model 清單轉為 Command 並委託 Writer 寫入
	 * Given: 建立測試 Model 清單
	 * When: 調用 commandService.exportBeans(list, ModelClass)
	 * Then: 應正確委託 excelWriterPort.write 並回傳 byte[]
	 */
	@Test
	@DisplayName("場景：成功透過 CommandService 調用 Assembler 匯出 Model 清單")
	void shouldExportBeansSuccessfully() {
		// Given
		byte[] mockBytes = new byte[] { 9, 8, 7 };
		when(excelWriterPort.write(any(WriteExcelCommand.class))).thenReturn(mockBytes);

		record SimpleItem(String code, String name) {}
		List<SimpleItem> items = List.of(new SimpleItem("C1", "N1"));

		// When
		byte[] result = commandService.exportBeans(items, SimpleItem.class);

		// Then
		assertThat(result).isEqualTo(mockBytes);
		verify(excelWriterPort).write(any(WriteExcelCommand.class));
	}
}

