package com.example.demo.application.service;

import java.io.OutputStream;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.example.demo.application.assembler.ExcelBeanAssembler;
import com.example.demo.application.port.in.ExportExcelUseCase;
import com.example.demo.application.port.in.StreamExportExcelUseCase;
import com.example.demo.application.port.out.ExcelWriterPort;
import com.example.demo.application.shared.command.ExportExcelCommand;
import com.example.demo.application.shared.command.StreamExportExcelCommand;
import com.example.demo.application.shared.command.WriteExcelCommand;
import com.example.demo.application.shared.command.WriteStreamExcelCommand;

/**
 * Excel 匯出應用層服務 (Excel Export Application Service)
 * <p>
 * 遵循 Clean Architecture 六角形架構規範，作為 Inbound Adapter 實作應用層
 * {@link ExportExcelUseCase} 與 {@link StreamExportExcelUseCase} 業務合約。
 * 協調調度領域/應用命令，調用 {@link ExcelBeanAssembler} 進行資料轉換，
 * 並呼叫基礎設施輸出端口 {@link ExcelWriterPort} 執行實際檔案產生。
 * 命名規範：以 {@code ApplicationService} 結尾 (3-9 規範)。
 */
@Service
public class ExcelExportApplicationService implements ExportExcelUseCase, StreamExportExcelUseCase {

	private final ExcelWriterPort excelWriterPort;

	public ExcelExportApplicationService(ExcelWriterPort excelWriterPort) {
		this.excelWriterPort = Objects.requireNonNull(excelWriterPort, "excelWriterPort must not be null");
	}

	@Override
	public byte[] export(ExportExcelCommand command) {
		Objects.requireNonNull(command, "command must not be null");
		WriteExcelCommand writeExcelCommand = WriteExcelCommand.of(command.sheets());
		return excelWriterPort.write(writeExcelCommand);
	}

	@Override
	public <T> byte[] exportBeans(List<T> dataList, Class<T> modelClass) {
		return exportBeans(dataList, modelClass, null);
	}

	@Override
	public <T> byte[] exportBeans(List<T> dataList, Class<T> modelClass, String customSheetName) {
		Objects.requireNonNull(modelClass, "modelClass must not be null");
		WriteExcelCommand writeCommand = ExcelBeanAssembler.toWriteCommand(dataList, modelClass, customSheetName);
		return excelWriterPort.write(writeCommand);
	}

	@Override
	public void streamExport(OutputStream outputStream, StreamExportExcelCommand command) {
		Objects.requireNonNull(outputStream, "outputStream must not be null");
		Objects.requireNonNull(command, "command must not be null");

		WriteStreamExcelCommand writeStreamExcelCommand = WriteStreamExcelCommand.builder()
				.sheets(command.sheets())
				.build();

		excelWriterPort.writeStream(outputStream, writeStreamExcelCommand);
	}

	@Override
	public <T> void streamExportBeans(OutputStream outputStream, List<T> dataList, Class<T> modelClass) {
		streamExportBeans(outputStream, dataList, modelClass, null);
	}

	@Override
	public <T> void streamExportBeans(OutputStream outputStream, List<T> dataList, Class<T> modelClass, String customSheetName) {
		Objects.requireNonNull(outputStream, "outputStream must not be null");
		Objects.requireNonNull(modelClass, "modelClass must not be null");
		WriteExcelCommand writeCommand = ExcelBeanAssembler.toWriteCommand(dataList, modelClass, customSheetName);
		excelWriterPort.write(outputStream, writeCommand);
	}
}
