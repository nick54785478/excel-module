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
 * Excel 匯入查詢應用服務 (Excel Import Query Service / Inbound Adapter)
 * <p>
 * 遵循 Clean Architecture 六角形架構與 CQRS 規範，作為查詢端 (Query Side) 的 Inbound Adapter。
 * 命名規範符合 AGENTS.md 3-9 規範 (以 {@code QueryService} 結尾)。
 * 實作應用層 {@link ImportExcelUseCase} 與 {@link StreamImportExcelUseCase} 業務合約。
 * 協調調度領域與應用層查詢，委託 {@link ExcelBeanAssembler} 進行資料模型逆向組裝，
 * 並呼叫基礎設施輸出端口 {@link ExcelReaderPort} 執行實際檔案解析與事件驅動串流讀取。
 * <p>
 * 涵蓋技術模組：
 * <ul>
 *     <li><b>【命令式工作表讀取模組 (Imperative Sheet Reader)】</b>：解析 Excel 全檔或指定工作表之行列原始資料。</li>
 *     <li><b>【宣告式物件映射模組 (Declarative Bean Mapping)】</b>：依據 {@code @ExcelSheet} 與 {@code @ExcelColumn} 註解逆向映射為 JavaBean / Record 清單。</li>
 *     <li><b>【快速工作表解析模組 (Quick Single Sheet Reader)】</b>：快速提取首張工作表結構。</li>
 *     <li><b>【事件與查詢流解析模組 / 大數據串流分塊讀取模組 (StAX / SAX Chunked Streaming Reader)】</b>：事件驅動分塊讀取，維持極低記憶體開銷。</li>
 *     <li><b>【大數據宣告式串流分塊映射模組 (Streaming Declarative Chunked Bean Reader)】</b>：結合宣告式映射與分塊回呼消費機制。</li>
 * </ul>
 */
@Service
public class ExcelQueryService implements ImportExcelUseCase, StreamImportExcelUseCase {

	private final ExcelReaderPort excelReaderPort;

	public ExcelQueryService(ExcelReaderPort excelReaderPort) {
		this.excelReaderPort = Objects.requireNonNull(excelReaderPort, "excelReaderPort must not be null");
	}

	/**
	 * 【命令式工作表讀取模組 (Imperative Sheet Reader)】
	 * <p>
	 * 依據查詢條件解析傳入之 Excel 輸入串流，回傳所有工作表原始行列資料與元數據。
	 *
	 * @param inputStream Excel 檔案二進位輸入串流
	 * @param query       讀取查詢條件
	 * @return 完整 Excel 內容查詢結果 DTO
	 */
	@Override
	public ExcelGottenResult importExcel(InputStream inputStream, ReadExcelQuery query) {
		Objects.requireNonNull(inputStream, "inputStream must not be null");
		ReadExcelQuery effectiveQuery = query != null ? query : ReadExcelQuery.defaultQuery();
		return excelReaderPort.read(inputStream, effectiveQuery);
	}

	/**
	 * 【宣告式物件映射模組 (Declarative Bean Mapping)】
	 * <p>
	 * 解析 Excel 檔案，依據模型類別上的註解宣告式映射轉換為對應的物件清單 (JavaBean 或 Java 21 Record)。
	 *
	 * @param inputStream Excel 檔案二進位輸入串流
	 * @param modelClass  目標映射資料模型類別
	 * @param <T>         資料模型泛型
	 * @return 解析後之強型別模型清單
	 */
	@Override
	public <T> List<T> importBeans(InputStream inputStream, Class<T> modelClass) {
		return importBeans(inputStream, ReadExcelQuery.defaultQuery(), modelClass);
	}

	/**
	 * 【宣告式物件映射模組 (Declarative Bean Mapping)】
	 * <p>
	 * 解析 Excel 檔案並套用自訂查詢條件，依據模型類別上的註解宣告式映射轉換為對應的物件清單。
	 *
	 * @param inputStream Excel 檔案二進位輸入串流
	 * @param query       自訂讀取查詢條件
	 * @param modelClass  目標映射資料模型類別
	 * @param <T>         資料模型泛型
	 * @return 解析後之強型別模型清單
	 */
	@Override
	public <T> List<T> importBeans(InputStream inputStream, ReadExcelQuery query, Class<T> modelClass) {
		Objects.requireNonNull(inputStream, "inputStream must not be null");
		Objects.requireNonNull(modelClass, "modelClass must not be null");
		ExcelGottenResult result = importExcel(inputStream, query);
		return ExcelBeanAssembler.toBeanList(result, modelClass);
	}

	/**
	 * 【快速工作表解析模組 (Quick Single Sheet Reader)】
	 * <p>
	 * 快速解析 Excel 檔案的第一張工作表，適用於單表匯入情境。
	 *
	 * @param inputStream Excel 檔案二進位輸入串流
	 * @return 第一張工作表之資料與元數據 DTO
	 */
	@Override
	public ExcelSheetGottenResult importFirstSheet(InputStream inputStream) {
		Objects.requireNonNull(inputStream, "inputStream must not be null");
		return excelReaderPort.readFirstSheet(inputStream);
	}

	/**
	 * 【事件與查詢流解析模組 / 大數據串流分塊讀取模組 (StAX / SAX Chunked Streaming Reader)】
	 * <p>
	 * 針對大型 Excel 檔案進行分塊 (Chunked) 串流解析，避免將全量資料一次性載入記憶體引發 OOM。
	 *
	 * @param command 串流匯入命令載體
	 * @return 串流匯入執行結果摘要 DTO
	 */
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

	/**
	 * 【大數據宣告式串流分塊映射模組 (Streaming Declarative Chunked Bean Reader)】
	 * <p>
	 * 結合 SAX / StAX 串流分塊讀取與宣告式物件映射，每當讀滿 {@code chunkSize} 筆資料時，
	 * 自動映射為強型別 Model 清單並回呼傳遞予 {@code chunkConsumer}，實現全鏈路低記憶體常數開銷。
	 *
	 * @param inputStream   輸入串流
	 * @param query         讀取條件
	 * @param modelClass    模型類別
	 * @param chunkSize     分塊筆數大小
	 * @param chunkConsumer 分塊消費處理器
	 * @param <T>           資料模型泛型
	 */
	@Override
	public <T> void streamImportBeans(InputStream inputStream, ReadExcelQuery query, Class<T> modelClass,
			int chunkSize, Consumer<List<T>> chunkConsumer) {
		Objects.requireNonNull(inputStream, "inputStream must not be null");
		Objects.requireNonNull(modelClass, "modelClass must not be null");
		Objects.requireNonNull(chunkConsumer, "chunkConsumer must not be null");

		ReadExcelQuery effectiveQuery = query != null ? query : ReadExcelQuery.defaultQuery();

		excelReaderPort.readChunked(inputStream, effectiveQuery, chunkSize, (chunkRows, context) -> {
			List<T> beanChunk = ExcelBeanAssembler.fromMaps(chunkRows, modelClass);
			if (!beanChunk.isEmpty()) {
				chunkConsumer.accept(beanChunk);
			}
		});
	}
}

