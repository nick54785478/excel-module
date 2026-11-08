package com.example.demo.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.demo.application.port.out.ExcelPartitionWriterPort;
import com.example.demo.application.shared.command.ExportPartitionZipCommand;
import com.example.demo.application.shared.command.ExportPipelineExcelCommand;

/**
 * 高並發平行與分片匯出命令服務單元測試 (ExcelParallelCommandService Test)
 * <p>
 * 遵循 AGENTS.md 規則 5 規範，採用 BDD 與 Gherkin (Feature, Scenario, Given-When-Then) 結構撰寫。
 */
@DisplayName("ExcelParallelCommandService 高並發平行與分片命令服務測試")
class ExcelParallelCommandServiceTest {

	private ExcelPartitionWriterPort partitionWriterPort;
	private ExcelParallelCommandService commandService;

	@BeforeEach
	void setUp() {
		partitionWriterPort = mock(ExcelPartitionWriterPort.class);
		commandService = new ExcelParallelCommandService(partitionWriterPort);
	}

	/**
	 * Feature: 模式 A 微觀生產者-消費者管線匯出
	 * Scenario: 呼叫 exportPipeline 成功調度 Outbound Port 產製 Excel 位元組
	 * Given: 建立合法的 ExportPipelineExcelCommand 命令載體
	 * When: 呼叫 commandService.exportPipeline(command)
	 * Then: 成功回傳假定之 Excel 位元組陣列，且 Outbound Port 恰好被調用一次
	 */
	@Test
	@DisplayName("模式 A 微觀管線匯出成功調度輸出埠")
	void should_export_pipeline_successfully() {
		// Given
		byte[] mockBytes = new byte[]{1, 2, 3, 4};
		when(partitionWriterPort.writePipeline(any())).thenReturn(mockBytes);

		ExportPipelineExcelCommand command = ExportPipelineExcelCommand.builder()
				.applicationName("TestApp")
				.sheetName("TestSheet")
				.headers(List.of("ID", "Name"))
				.totalRows(100)
				.chunkSize(50)
				.queueCapacity(10)
				.parallelProducers(2)
				.chunkProvider((offset, limit) -> List.of(List.of(1, "Item1")))
				.build();

		// When
		byte[] result = commandService.exportPipeline(command);

		// Then
		assertThat(result).isNotNull().isEqualTo(mockBytes);
		verify(partitionWriterPort, times(1)).writePipeline(command);
	}

	/**
	 * Feature: 模式 A 微觀管線串流寫出
	 * Scenario: 呼叫 exportPipelineStream 成功調度 Outbound Port
	 * Given: 建立目標 OutputStream 與管線命令
	 * When: 呼叫 commandService.exportPipelineStream(os, command)
	 * Then: 輸出埠之 writePipelineStream 恰好被調用一次
	 */
	@Test
	@DisplayName("模式 A 微觀管線串流寫出成功調度輸出埠")
	void should_export_pipeline_stream_successfully() {
		// Given
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ExportPipelineExcelCommand command = ExportPipelineExcelCommand.builder()
				.applicationName("TestApp")
				.sheetName("TestSheet")
				.headers(List.of("ID", "Name"))
				.totalRows(100)
				.chunkProvider((offset, limit) -> List.of(List.of(1, "Item1")))
				.build();

		// When
		commandService.exportPipelineStream(baos, command);

		// Then
		verify(partitionWriterPort, times(1)).writePipelineStream(baos, command);
	}

	/**
	 * Feature: 模式 B 多活頁簿分片 ZIP 匯出
	 * Scenario: 呼叫 exportPartitionZip 成功調度 Outbound Port 產製 ZIP 二進位檔
	 * Given: 建立合法的 ExportPartitionZipCommand 命令載體
	 * When: 呼叫 commandService.exportPartitionZip(command)
	 * Then: 成功回傳假定之 ZIP 位元組陣列，且 Outbound Port 恰好被調用一次
	 */
	@Test
	@DisplayName("模式 B 多活頁簿分片 ZIP 匯出成功調度輸出埠")
	void should_export_partition_zip_successfully() {
		// Given
		byte[] mockZip = new byte[]{'P', 'K', 3, 4};
		when(partitionWriterPort.writePartitionZip(any())).thenReturn(mockZip);

		ExportPartitionZipCommand command = ExportPartitionZipCommand.builder()
				.zipFileName("test_archive.zip")
				.baseFileName("test_part")
				.sheetName("Orders")
				.headers(List.of("ID", "Price"))
				.totalRows(2500)
				.maxRowsPerFile(1000)
				.chunkSize(500)
				.chunkProvider((offset, limit) -> List.of(List.of(1, 99.9)))
				.build();

		// When
		byte[] result = commandService.exportPartitionZip(command);

		// Then
		assertThat(result).isNotNull().isEqualTo(mockZip);
		verify(partitionWriterPort, times(1)).writePartitionZip(command);
	}

	/**
	 * Feature: 參數前置防禦
	 * Scenario: 傳入 null 參數時立即拋出 NullPointerException
	 * Given: 傳入 null 指標
	 * When: 呼叫對應業務方法
	 * Then: 拋出 NullPointerException
	 */
	@Test
	@DisplayName("傳入 null 參數拋出 NullPointerException")
	void should_throw_exception_when_params_null() {
		assertThatThrownBy(() -> commandService.exportPipeline(null))
				.isInstanceOf(NullPointerException.class);

		assertThatThrownBy(() -> commandService.exportPipelineStream(null, null))
				.isInstanceOf(NullPointerException.class);

		assertThatThrownBy(() -> commandService.exportPartitionZip(null))
				.isInstanceOf(NullPointerException.class);

		assertThatThrownBy(() -> commandService.exportPartitionZipStream(null, null))
				.isInstanceOf(NullPointerException.class);
	}
}

