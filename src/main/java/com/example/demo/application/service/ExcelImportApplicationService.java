package com.example.demo.application.service;

import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.springframework.stereotype.Service;

import com.example.demo.application.assembler.ExcelBeanAssembler;
import com.example.demo.application.port.in.ImportExcelUseCase;
import com.example.demo.application.port.in.StreamImportExcelUseCase;
import com.example.demo.application.port.out.ExcelReaderPort;
import com.example.demo.application.shared.command.StreamImportExcelCommand;
import com.example.demo.application.shared.dto.ExcelGottenResult;
import com.example.demo.application.shared.dto.ExcelSheetGottenResult;
import com.example.demo.application.shared.dto.ExcelStreamImportGottenResult;
import com.example.demo.application.shared.query.ReadExcelQuery;

/**
 * Excel 匯入解析應用層服務 (Excel Import Application Service)
 * <p>
 * 遵循 Clean Architecture 六角形架構規範，作為 Inbound Adapter 實作應用層
 * {@link ImportExcelUseCase} 與 {@link StreamImportExcelUseCase} 業務合約。
 * 協調調度領域/應用命令，調用 {@link ExcelBeanAssembler} 進行資料轉換，
 * 並呼叫基礎設施輸出端口 {@link ExcelReaderPort} 執行實際檔案解析與串流分塊處理。
 * 命名規範：以 {@code ApplicationService} 結尾 (3-9 規範)。
 */
@Service
public class ExcelImportApplicationService implements ImportExcelUseCase, StreamImportExcelUseCase {

	private final ExcelReaderPort excelReaderPort;

	public ExcelImportApplicationService(ExcelReaderPort excelReaderPort) {
		this.excelReaderPort = Objects.requireNonNull(excelReaderPort, "excelReaderPort must not be null");
	}

	@Override
	public ExcelGottenResult importExcel(InputStream inputStream, ReadExcelQuery query) {
		Objects.requireNonNull(inputStream, "inputStream must not be null");
		ReadExcelQuery effectiveQuery = query != null ? query : ReadExcelQuery.defaultQuery();
		return excelReaderPort.read(inputStream, effectiveQuery);
	}

	@Override
	public <T> List<T> importBeans(InputStream inputStream, Class<T> modelClass) {
		return importBeans(inputStream, ReadExcelQuery.defaultQuery(), modelClass);
	}

	@Override
	public <T> List<T> importBeans(InputStream inputStream, ReadExcelQuery query, Class<T> modelClass) {
		Objects.requireNonNull(inputStream, "inputStream must not be null");
		Objects.requireNonNull(modelClass, "modelClass must not be null");
		// 1. 調用純粹 Outbound Port 讀取 Excel 原始資料載體
		ExcelGottenResult result = importExcel(inputStream, query);
		// 2. 由 Application Service 調用 Assembler 將 DTO 轉換為 Model
		return ExcelBeanAssembler.toBeanList(result, modelClass);
	}

	@Override
	public ExcelSheetGottenResult importFirstSheet(InputStream inputStream) {
		Objects.requireNonNull(inputStream, "inputStream must not be null");
		return excelReaderPort.readFirstSheet(inputStream);
	}

	@Override
	public ExcelStreamImportGottenResult streamImport(StreamImportExcelCommand command) {
		Objects.requireNonNull(command, "command must not be null");

		long startTime = System.currentTimeMillis();
		AtomicLong totalRows = new AtomicLong(0);
		AtomicInteger chunkCount = new AtomicInteger(0);

		ReadExcelQuery query = command.sheetName() != null && !command.sheetName().isBlank()
				? ReadExcelQuery.ofSheet(command.sheetName())
				: ReadExcelQuery.defaultQuery();

		excelReaderPort.readChunked(command.inputStream(), query, command.chunkSize(), (chunkRows, context) -> {
			chunkCount.incrementAndGet();
			totalRows.addAndGet(chunkRows.size());
		});

		long durationMs = System.currentTimeMillis() - startTime;
		String targetSheet = command.sheetName() != null ? command.sheetName() : "ALL_SHEETS";

		return new ExcelStreamImportGottenResult(
				targetSheet,
				totalRows.get(),
				chunkCount.get(),
				durationMs,
				"SUCCESS",
				"Stream import chunk processing completed successfully"
		);
	}

	@Override
	public <T> void streamImportBeans(InputStream inputStream, ReadExcelQuery query, Class<T> modelClass,
			int chunkSize, Consumer<List<T>> chunkConsumer) {
		Objects.requireNonNull(inputStream, "inputStream must not be null");
		Objects.requireNonNull(modelClass, "modelClass must not be null");
		Objects.requireNonNull(chunkConsumer, "chunkConsumer must not be null");

		ReadExcelQuery effectiveQuery = query != null ? query : ReadExcelQuery.defaultQuery();
		excelReaderPort.readChunked(inputStream, effectiveQuery, chunkSize, (chunkRows, context) -> {
			List<T> beanChunk = ExcelBeanAssembler.fromMaps(chunkRows, modelClass);
			chunkConsumer.accept(beanChunk);
		});
	}
}

