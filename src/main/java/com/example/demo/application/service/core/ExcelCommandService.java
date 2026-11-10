package com.example.demo.application.service.core;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.example.demo.application.assembler.ExcelBeanAssembler;
import com.example.demo.application.port.in.core.ExportExcelUseCase;
import com.example.demo.application.port.in.core.StreamExportExcelUseCase;
import com.example.demo.application.port.out.core.ExcelWriterPort;
import com.example.demo.application.shared.command.ExportExcelCommand;
import com.example.demo.application.shared.command.StreamExportExcelCommand;
import com.example.demo.application.shared.command.WriteExcelCommand;
import com.example.demo.application.shared.command.WriteStreamExcelCommand;

/**
 * Excel 匯出命令應用服務 (Excel Export Command Service / Inbound Adapter)
 * <p>
 * 遵循 Clean Architecture 六角形架構與 CQRS 規範，作為命令端 (Command Side) 的 Inbound Adapter。
 * 命名規範符合 AGENTS.md 3-9 規範 (以 {@code CommandService} 結尾)。
 * 實作應用層 {@link ExportExcelUseCase} 與 {@link StreamExportExcelUseCase} 業務合約。
 * 協調調度領域與應用層命令，委託 {@link ExcelBeanAssembler} 進行資料結構轉換，
 * 並呼叫基礎設施輸出端口 {@link ExcelWriterPort} 執行實際檔案產生。
 * <p>
 * 涵蓋技術模組：
 * <ul>
 *     <li><b>【命令式工作表寫入模組 (Imperative Sheet Writer)】</b>：接收自訂標題、二維列資料之動態匯出。</li>
 *     <li><b>【宣告式物件映射模組 (Declarative Bean Mapping)】</b>：依據 {@code @ExcelSheet} 與 {@code @ExcelColumn} 註解之 JavaBean / Record 物件映射。</li>
 *     <li><b>【通用動態字典映射模組 (Dynamic Map Mapping)】</b>：接收 Map 鍵值對結構的動態報表輸出。</li>
 *     <li><b>【大數據串流處理模組 (Big Data Streaming Module)】</b>：百萬級資料量邊寫入邊 Flush，維持 O(1) 記憶體常數開銷。</li>
 * </ul>
 */
@Service
public class ExcelCommandService implements ExportExcelUseCase, StreamExportExcelUseCase {

	private final ExcelWriterPort excelWriterPort;

	public ExcelCommandService(ExcelWriterPort excelWriterPort) {
		this.excelWriterPort = Objects.requireNonNull(excelWriterPort, "excelWriterPort must not be null");
	}

	/**
	 * 【命令式工作表寫入模組 (Imperative Sheet Writer)】
	 * <p>
	 * 依據傳入之多工作表結構、自訂標題列與純 Object 資料列，於記憶體中建構 Excel 活頁簿二進位資料。
	 *
	 * @param command 匯出結構資料載體
	 * @return Excel 二進位位元組陣列
	 */
	@Override
	public byte[] export(ExportExcelCommand command) {
		Objects.requireNonNull(command, "command must not be null");
		WriteExcelCommand writeExcelCommand = WriteExcelCommand.of(command.sheets());
		return excelWriterPort.write(writeExcelCommand);
	}

	/**
	 * 【宣告式物件映射模組 (Declarative Bean Mapping)】
	 * <p>
	 * 將物件清單依據其類別之 {@code @ExcelSheet} 與 {@code @ExcelColumn} 註解元資料宣告式產製為 Excel 二進位檔案。
	 *
	 * @param dataList   資料模型清單
	 * @param modelClass 模型類別 (JavaBean 或 Java 21 Record)
	 * @param <T>        資料模型泛型
	 * @return Excel 二進位位元組陣列
	 */
	@Override
	public <T> byte[] exportBeans(List<T> dataList, Class<T> modelClass) {
		return exportBeans(dataList, modelClass, null);
	}

	/**
	 * 【宣告式物件映射模組 (Declarative Bean Mapping)】
	 * <p>
	 * 將物件清單依據其類別註解元資料宣告式產製為 Excel 二進位檔案，並允許覆寫預設工作表名稱。
	 *
	 * @param dataList        資料模型清單
	 * @param modelClass      模型類別 (JavaBean 或 Java 21 Record)
	 * @param customSheetName 自訂工作表名稱 (若為 null 則套用註解設定)
	 * @param <T>             資料模型泛型
	 * @return Excel 二進位位元組陣列
	 */
	@Override
	public <T> byte[] exportBeans(List<T> dataList, Class<T> modelClass, String customSheetName) {
		Objects.requireNonNull(modelClass, "modelClass must not be null");
		WriteExcelCommand writeCommand = ExcelBeanAssembler.toWriteCommand(dataList, modelClass, customSheetName);
		return excelWriterPort.write(writeCommand);
	}

	/**
	 * 【通用動態字典映射模組 (Dynamic Map Mapping)】
	 * <p>
	 * 接收動態鍵值對清單，依據指定標題列映射並產生 Excel 二進位位元組陣列。
	 *
	 * @param sheetName 工作表名稱
	 * @param headers   標題列清單
	 * @param mapRows   資料字典列清單
	 * @return Excel 二進位位元組陣列
	 */
	@Override
	public byte[] exportMaps(String sheetName, List<String> headers, List<Map<String, Object>> mapRows) {
		WriteExcelCommand writeCommand = ExcelBeanAssembler.toWriteCommandFromMaps(sheetName, headers, mapRows);
		return excelWriterPort.write(writeCommand);
	}

	/**
	 * 【大數據串流處理模組 (Big Data Streaming Module)】
	 * <p>
	 * 針對海量大數據資料，邊寫入邊透過輸出串流 Flush，維持 O(1) 極低常數記憶體消耗。
	 *
	 * @param outputStream 目的輸出串流
	 * @param command      串流匯出命令載體
	 */
	@Override
	public void streamExport(OutputStream outputStream, StreamExportExcelCommand command) {
		Objects.requireNonNull(outputStream, "outputStream must not be null");
		Objects.requireNonNull(command, "command must not be null");

		WriteStreamExcelCommand writeStreamExcelCommand = WriteStreamExcelCommand.builder()
				.sheets(command.sheets())
				.build();

		excelWriterPort.writeStream(outputStream, writeStreamExcelCommand);
	}

	/**
	 * 【大數據宣告式串流處理模組 (Big Data Declarative Streaming)】
	 * <p>
	 * 將物件清單宣告式轉換後以串流模式寫入輸出串流。
	 *
	 * @param outputStream 目的輸出串流
	 * @param dataList     資料模型清單
	 * @param modelClass   模型類別
	 * @param <T>          資料模型泛型
	 */
	@Override
	public <T> void streamExportBeans(OutputStream outputStream, List<T> dataList, Class<T> modelClass) {
		streamExportBeans(outputStream, dataList, modelClass, null);
	}

	/**
	 * 【大數據宣告式串流處理模組 (Big Data Declarative Streaming)】
	 * <p>
	 * 將物件清單宣告式轉換後以串流模式寫入輸出串流，支援自訂工作表名稱。
	 *
	 * @param outputStream    目的輸出串流
	 * @param dataList        資料模型清單
	 * @param modelClass      模型類別
	 * @param customSheetName 自訂工作表名稱
	 * @param <T>             資料模型泛型
	 */
	@Override
	public <T> void streamExportBeans(OutputStream outputStream, List<T> dataList, Class<T> modelClass, String customSheetName) {
		Objects.requireNonNull(outputStream, "outputStream must not be null");
		Objects.requireNonNull(modelClass, "modelClass must not be null");
		WriteExcelCommand writeCommand = ExcelBeanAssembler.toWriteCommand(dataList, modelClass, customSheetName);
		excelWriterPort.write(outputStream, writeCommand);
	}

	/**
	 * 【大數據字典串流處理模組 (Big Data Map Streaming)】
	 * <p>
	 * 將動態鍵值對資料以串流模式寫入輸出串流。
	 *
	 * @param outputStream 目的輸出串流
	 * @param sheetName    工作表名稱
	 * @param headers      標題列清單
	 * @param mapRows      字典列資料清單
	 */
	@Override
	public void streamExportMaps(OutputStream outputStream, String sheetName, List<String> headers, List<Map<String, Object>> mapRows) {
		Objects.requireNonNull(outputStream, "outputStream must not be null");
		WriteExcelCommand writeCommand = ExcelBeanAssembler.toWriteCommandFromMaps(sheetName, headers, mapRows);
		excelWriterPort.write(outputStream, writeCommand);
	}

	/**
	 * 【大數據字典串流處理模組 (Big Data Map Streaming)】
	 * <p>
	 * 將動態鍵值對資料以可變參數形式以串流模式寫入輸出串流。
	 *
	 * @param outputStream 目的輸出串流
	 * @param sheetName    工作表名稱
	 * @param headers      標題列清單
	 * @param mapRows      字典列可變參數
	 */
	@SafeVarargs
	@Override
	public final void streamExportMaps(OutputStream outputStream, String sheetName, List<String> headers, Map<String, Object>... mapRows) {
		streamExportMaps(outputStream, sheetName, headers, mapRows != null ? List.of(mapRows) : List.of());
	}
}

