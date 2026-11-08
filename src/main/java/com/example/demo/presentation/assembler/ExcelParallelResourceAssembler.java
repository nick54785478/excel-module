package com.example.demo.presentation.assembler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.example.demo.application.shared.command.ExportPartitionZipCommand;
import com.example.demo.application.shared.command.ExportPipelineExcelCommand;
import com.example.demo.application.shared.pipeline.ExcelChunkProvider;
import com.example.demo.presentation.resource.in.ExportParallelPipelineResource;
import com.example.demo.presentation.resource.in.ExportPartitionZipResource;

/**
 * 高並發平行與分片表現層組裝器 (Excel Parallel Presentation Resource Assembler)
 * <p>
 * <b>【架構定位與職責規範】</b><br>
 * 遵循 AGENTS.md 規則 33~35 規範。<br>
 * 作為表現層與應用層之間的防腐層 (ACL)，專責將表現層 Resource 轉譯為應用層 Command。<br>
 * 嚴禁將此轉換邏輯置入 Resource 物件內部。
 */
@Component
public class ExcelParallelResourceAssembler {

	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private static final List<String> DEFAULT_HEADERS = List.of(
			"項次", "單據流水號", "物料料號", "出貨數量", "標準單價", "金額小計", "產製時間"
	);

	/**
	 * 將模式 A 管線請求資源轉譯為應用層命令
	 *
	 * @param resource       表現層請求資源
	 * @param customProvider 外部資料提供者 (若為 null 則自動注入合成資料生成器)
	 * @return 應用層管線命令
	 */
	public ExportPipelineExcelCommand toCommand(ExportParallelPipelineResource resource, ExcelChunkProvider customProvider) {
		int totalRows = (resource != null && resource.totalRows() != null) ? resource.totalRows() : 10000;
		int chunkSize = (resource != null && resource.chunkSize() != null) ? resource.chunkSize() : 1000;
		int queueCapacity = (resource != null && resource.queueCapacity() != null) ? resource.queueCapacity() : 30;
		int parallelProducers = (resource != null && resource.parallelProducers() != null) ? resource.parallelProducers() : 4;
		String sheetName = (resource != null && resource.sheetName() != null && !resource.sheetName().isBlank())
				? resource.sheetName().trim()
				: "高並發管線匯出明細";
		List<String> headers = (resource != null && resource.headers() != null && !resource.headers().isEmpty())
				? resource.headers()
				: DEFAULT_HEADERS;

		ExcelChunkProvider provider = customProvider != null ? customProvider : createSyntheticChunkProvider();

		return ExportPipelineExcelCommand.builder()
				.applicationName("ExcelParallelModule")
				.sheetName(sheetName)
				.headers(headers)
				.totalRows(totalRows)
				.chunkSize(chunkSize)
				.queueCapacity(queueCapacity)
				.parallelProducers(parallelProducers)
				.columnWidths(List.of(10.0, 22.0, 18.0, 14.0, 14.0, 16.0, 24.0))
				.freezeHeader(true)
				.chunkProvider(provider)
				.build();
	}

	/**
	 * 將模式 B / A+B 分片 ZIP 請求資源轉譯為應用層命令
	 *
	 * @param resource       表現層請求資源
	 * @param customProvider 外部資料提供者 (若為 null 則自動注入合成資料生成器)
	 * @return 應用層分片 ZIP 命令
	 */
	public ExportPartitionZipCommand toCommand(ExportPartitionZipResource resource, ExcelChunkProvider customProvider) {
		int totalRows = (resource != null && resource.totalRows() != null) ? resource.totalRows() : 30000;
		int maxRowsPerFile = (resource != null && resource.maxRowsPerFile() != null) ? resource.maxRowsPerFile() : 10000;
		int chunkSize = (resource != null && resource.chunkSize() != null) ? resource.chunkSize() : 1000;
		int parallelProducers = (resource != null && resource.parallelProducersPerPartition() != null)
				? resource.parallelProducersPerPartition()
				: 4;
		boolean usePipeline = resource == null || resource.usePipelinePerPartition() == null || resource.usePipelinePerPartition();

		String zipName = (resource != null && resource.zipFileName() != null && !resource.zipFileName().isBlank())
				? resource.zipFileName().trim()
				: "orders_multi_partition_archive.zip";
		String baseName = (resource != null && resource.baseFileName() != null && !resource.baseFileName().isBlank())
				? resource.baseFileName().trim()
				: "orders_part";
		String sheetName = (resource != null && resource.sheetName() != null && !resource.sheetName().isBlank())
				? resource.sheetName().trim()
				: "訂單明細";
		List<String> headers = (resource != null && resource.headers() != null && !resource.headers().isEmpty())
				? resource.headers()
				: DEFAULT_HEADERS;

		ExcelChunkProvider provider = customProvider != null ? customProvider : createSyntheticChunkProvider();

		return ExportPartitionZipCommand.builder()
				.zipFileName(zipName)
				.baseFileName(baseName)
				.sheetName(sheetName)
				.headers(headers)
				.totalRows(totalRows)
				.maxRowsPerFile(maxRowsPerFile)
				.chunkSize(chunkSize)
				.parallelProducersPerPartition(parallelProducers)
				.usePipelinePerPartition(usePipeline)
				.columnWidths(List.of(10.0, 22.0, 18.0, 14.0, 14.0, 16.0, 24.0))
				.freezeHeader(true)
				.chunkProvider(provider)
				.build();
	}

	/**
	 * 產製高吞吐合成業務資料產生器 (模擬真實資料庫查詢與業務轉換)
	 */
	public ExcelChunkProvider createSyntheticChunkProvider() {
		return (offset, limit) -> {
			List<List<Object>> chunk = new ArrayList<>(limit);
			String nowStr = LocalDateTime.now().format(TIME_FORMATTER);

			for (int i = 0; i < limit; i++) {
				int rowNum = offset + i + 1;
				String orderNo = String.format("ORD-%07d", rowNum);
				String matCode = String.format("MAT-SFG-%04d", (rowNum % 25) + 1);
				int qty = (rowNum % 10) + 1;
				double unitPrice = 120.0 + (rowNum % 50) * 10.5;
				double totalAmount = qty * unitPrice;

				chunk.add(List.of(
						rowNum,
						orderNo,
						matCode,
						qty,
						unitPrice,
						totalAmount,
						nowStr
				));
			}
			return chunk;
		};
	}
}

