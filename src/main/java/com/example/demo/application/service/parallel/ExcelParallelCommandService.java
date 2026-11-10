package com.example.demo.application.service.parallel;

import java.io.OutputStream;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.example.demo.application.port.in.parallel.ExportParallelExcelUseCase;
import com.example.demo.application.port.out.parallel.ExcelPartitionWriterPort;
import com.example.demo.application.shared.command.ExportPartitionZipCommand;
import com.example.demo.application.shared.command.ExportPipelineExcelCommand;

/**
 * 高並發平行與分片 Excel 匯出命令服務 (Excel Parallel Export Command Application Service)
 * <p>
 * 遵循 Clean Architecture 六角形架構規範，實作 Inbound Port {@link ExportParallelExcelUseCase}。<br>
 * 命名規範：以 {@code CommandService} 結尾 (3-9 CQRS 規範)。<br>
 * 負責協調應用層命令與底層基礎設施輸出埠 {@link ExcelPartitionWriterPort}，
 * 支援微觀生產者-消費者管線加速 (模式 A) 與宏觀多活頁簿 ZIP 分片 (模式 B)。
 */
@Service
public class ExcelParallelCommandService implements ExportParallelExcelUseCase {

	private final ExcelPartitionWriterPort partitionWriterPort;

	public ExcelParallelCommandService(ExcelPartitionWriterPort partitionWriterPort) {
		this.partitionWriterPort = Objects.requireNonNull(partitionWriterPort, "ExcelPartitionWriterPort must not be null");
	}

	@Override
	public void exportPipelineStream(OutputStream outputStream, ExportPipelineExcelCommand command) {
		Objects.requireNonNull(outputStream, "OutputStream must not be null");
		Objects.requireNonNull(command, "ExportPipelineExcelCommand must not be null");
		partitionWriterPort.writePipelineStream(outputStream, command);
	}

	@Override
	public byte[] exportPipeline(ExportPipelineExcelCommand command) {
		Objects.requireNonNull(command, "ExportPipelineExcelCommand must not be null");
		return partitionWriterPort.writePipeline(command);
	}

	@Override
	public void exportPartitionZipStream(OutputStream outputStream, ExportPartitionZipCommand command) {
		Objects.requireNonNull(outputStream, "OutputStream must not be null");
		Objects.requireNonNull(command, "ExportPartitionZipCommand must not be null");
		partitionWriterPort.writePartitionZipStream(outputStream, command);
	}

	@Override
	public byte[] exportPartitionZip(ExportPartitionZipCommand command) {
		Objects.requireNonNull(command, "ExportPartitionZipCommand must not be null");
		return partitionWriterPort.writePartitionZip(command);
	}
}

