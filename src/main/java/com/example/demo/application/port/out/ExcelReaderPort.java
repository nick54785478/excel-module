package com.example.demo.application.port.out;

import java.io.InputStream;

import com.example.demo.application.shared.dto.ExcelGottenResult;
import com.example.demo.application.shared.dto.ExcelSheetGottenResult;
import com.example.demo.application.shared.exception.ExcelReadException;
import com.example.demo.application.shared.query.ReadExcelQuery;

/**
 * Excel 讀取輸出端口 (Outbound Port)
 * <p>
 * 依據 Clean Architecture 與六角形架構規範，定義應用層對外要求基礎設施層提供的 Excel 解析與讀取合約。
 * 本介面隔離底層第三方讀取套件 (如 FastExcel Reader)，保護業務邏輯不受技術選型變更影響。
 * 命名規範：以 {@code Port} 結尾 (3-8 規範)。
 */
public interface ExcelReaderPort {

	/**
	 * 從輸入資料流讀取並解析 Excel 檔案 (Streaming Read)
	 *
	 * @param inputStream Excel 檔案輸入串流 (不得為 null)
	 * @param query       讀取查詢條件 (如指定工作表、起始行、標題列等，若為 null 則套用預設)
	 * @return Excel 活頁簿解析結果物件
	 * @throws ExcelReadException 當串流讀取、格式解析或檔案損壞時拋出
	 */
	ExcelGottenResult read(InputStream inputStream, ReadExcelQuery query);

	/**
	 * 從二進位 byte 陣列讀取並解析 Excel 檔案 (In-memory Read)
	 *
	 * @param excelBytes Excel 二進位位元組陣列 (不得為 null)
	 * @param query      讀取查詢條件
	 * @return Excel 活頁簿解析結果物件
	 * @throws ExcelReadException 當格式解析或檔案損壞時拋出
	 */
	ExcelGottenResult read(byte[] excelBytes, ReadExcelQuery query);

	/**
	 * 便捷方法：使用預設條件從輸入串流讀取整份 Excel 活頁簿
	 *
	 * @param inputStream Excel 檔案輸入串流
	 * @return Excel 活頁簿解析結果物件
	 */
	default ExcelGottenResult read(InputStream inputStream) {
		return read(inputStream, ReadExcelQuery.defaultQuery());
	}

	/**
	 * 便捷方法：使用預設條件從 byte 陣列讀取整份 Excel 活頁簿
	 *
	 * @param excelBytes Excel 二進位位元組陣列
	 * @return Excel 活頁簿解析結果物件
	 */
	default ExcelGottenResult read(byte[] excelBytes) {
		return read(excelBytes, ReadExcelQuery.defaultQuery());
	}

	/**
	 * 便捷方法：讀取第一個工作表內容
	 *
	 * @param inputStream Excel 檔案輸入串流
	 * @return 第一個工作表解析結果物件 (若無工作表則回傳空工作表結果)
	 */
	default ExcelSheetGottenResult readFirstSheet(InputStream inputStream) {
		ExcelGottenResult result = read(inputStream, ReadExcelQuery.ofSheet(0));
		return result.getFirstSheet().orElseGet(() -> new ExcelSheetGottenResult(0, "Sheet1", null, null, null));
	}

	/**
	 * 便捷方法：讀取第一個工作表內容
	 *
	 * @param excelBytes Excel 二進位位元組陣列
	 * @return 第一個工作表解析結果物件 (若無工作表則回傳空工作表結果)
	 */
	default ExcelSheetGottenResult readFirstSheet(byte[] excelBytes) {
		ExcelGottenResult result = read(excelBytes, ReadExcelQuery.ofSheet(0));
		return result.getFirstSheet().orElseGet(() -> new ExcelSheetGottenResult(0, "Sheet1", null, null, null));
	}

	/**
	 * 串流逐列讀取 (Streaming Row Consumer)
	 * <p>
	 * 透過 StAX 串流逐行回呼消費者，記憶體開銷維持 O(1)，支援百萬級巨量資料解析。
	 *
	 * @param inputStream Excel 檔案輸入串流 (不得為 null)
	 * @param query       讀取查詢條件
	 * @param rowConsumer 逐列消費者 (接收原生儲存格數值、文字與上下文環境)
	 * @throws ExcelReadException 當串流讀取、格式解析或檔案損壞時拋出
	 */
	void readStream(InputStream inputStream, ReadExcelQuery query, com.example.demo.application.shared.stream.ExcelRowConsumer rowConsumer);

	/**
	 * 串流逐列讀取 (從 byte[] 讀取)
	 *
	 * @param excelBytes  Excel 二進位位元組陣列
	 * @param query       讀取查詢條件
	 * @param rowConsumer 逐列消費者
	 */
	void readStream(byte[] excelBytes, ReadExcelQuery query, com.example.demo.application.shared.stream.ExcelRowConsumer rowConsumer);

	/**
	 * 串流分塊批次讀取 (Chunked Stream Consumer)
	 * <p>
	 * 每累積達指定 {@code chunkSize} 筆數即觸發一次回呼，專門對接資料庫批次寫入 (Batch Insert)。
	 *
	 * @param inputStream   Excel 檔案輸入串流 (不得為 null)
	 * @param query         讀取查詢條件
	 * @param chunkSize     分塊大小 (如 1,000 或 5,000，必須大於 0)
	 * @param chunkConsumer 分塊批次消費者 (接收已映射標題名稱之資料 Map 清單)
	 * @throws ExcelReadException 當串流讀取、格式解析或檔案損壞時拋出
	 */
	void readChunked(InputStream inputStream, ReadExcelQuery query, int chunkSize, com.example.demo.application.shared.stream.ExcelChunkConsumer chunkConsumer);

	/**
	 * 串流分塊批次讀取 (從 byte[] 讀取)
	 *
	 * @param excelBytes    Excel 二進位位元組陣列
	 * @param query         讀取查詢條件
	 * @param chunkSize     分塊大小
	 * @param chunkConsumer 分塊批次消費者
	 */
	void readChunked(byte[] excelBytes, ReadExcelQuery query, int chunkSize, com.example.demo.application.shared.stream.ExcelChunkConsumer chunkConsumer);

	/**
	 * 串流全生命週期監聽讀取 (Full Lifecycle Listener)
	 *
	 * @param inputStream Excel 檔案輸入串流 (不得為 null)
	 * @param query       讀取查詢條件
	 * @param listener    生命週期監聽器
	 * @throws ExcelReadException 當串流讀取、格式解析或檔案損壞時拋出
	 */
	void readWithListener(InputStream inputStream, ReadExcelQuery query, com.example.demo.application.shared.stream.ExcelStreamListener listener);

	/**
	 * 串流全生命週期監聽讀取 (從 byte[] 讀取)
	 *
	 * @param excelBytes Excel 二進位位元組陣列
	 * @param query      讀取查詢條件
	 * @param listener   生命週期監聽器
	 */
	void readWithListener(byte[] excelBytes, ReadExcelQuery query, com.example.demo.application.shared.stream.ExcelStreamListener listener);

	// =========================================================================
	// 宣告式 Model 物件 (Bean / Record) 讀取擴充
	// =========================================================================

	/**
	 * 從輸入串流讀取 Excel 並宣告式解析為強型別 Model 清單
	 * <p>
	 * 依據 {@link com.example.demo.application.annotation.ExcelSheet} 與
	 * {@link com.example.demo.application.annotation.ExcelColumn} 註解解析為 POJO 或 Java 21 Record。
	 *
	 * @param <T>         模型型別
	 * @param inputStream Excel 輸入串流 (不得為 null)
	 * @param modelClass  模型類別 Class (支援 POJO 與 Record)
	 * @return 強型別 Model 物件清單
	 * @throws ExcelReadException 當串流讀取或格式解析發生錯誤時拋出
	 */
	default <T> java.util.List<T> readBeans(InputStream inputStream, Class<T> modelClass) {
		ExcelGottenResult result = read(inputStream);
		return com.example.demo.application.assembler.ExcelBeanAssembler.toBeanList(result, modelClass);
	}

	/**
	 * 從 byte 陣列讀取 Excel 並宣告式解析為強型別 Model 清單
	 *
	 * @param <T>        模型型別
	 * @param excelBytes Excel 二進位位元組陣列 (不得為 null)
	 * @param modelClass 模型類別 Class (支援 POJO 與 Record)
	 * @return 強型別 Model 物件清單
	 * @throws ExcelReadException 當格式解析發生錯誤時拋出
	 */
	default <T> java.util.List<T> readBeans(byte[] excelBytes, Class<T> modelClass) {
		ExcelGottenResult result = read(excelBytes);
		return com.example.demo.application.assembler.ExcelBeanAssembler.toBeanList(result, modelClass);
	}

	/**
	 * 依據查詢條件從輸入串流讀取 Excel 並解析為強型別 Model 清單
	 *
	 * @param <T>         模型型別
	 * @param inputStream Excel 輸入串流 (不得為 null)
	 * @param query       讀取查詢條件
	 * @param modelClass  模型類別 Class
	 * @return 強型別 Model 物件清單
	 */
	default <T> java.util.List<T> readBeans(InputStream inputStream, ReadExcelQuery query, Class<T> modelClass) {
		ExcelGottenResult result = read(inputStream, query);
		return com.example.demo.application.assembler.ExcelBeanAssembler.toBeanList(result, modelClass);
	}

	/**
	 * 依據查詢條件從 byte 陣列讀取 Excel 並解析為強型別 Model 清單
	 *
	 * @param <T>        模型型別
	 * @param excelBytes Excel 二進位位元組陣列 (不得為 null)
	 * @param query      讀取查詢條件
	 * @param modelClass 模型類別 Class
	 * @return 強型別 Model 物件清單
	 */
	default <T> java.util.List<T> readBeans(byte[] excelBytes, ReadExcelQuery query, Class<T> modelClass) {
		ExcelGottenResult result = read(excelBytes, query);
		return com.example.demo.application.assembler.ExcelBeanAssembler.toBeanList(result, modelClass);
	}

	/**
	 * 串流分塊讀取並宣告式轉換為 Model 批次清單 (Chunked Stream Consumer)
	 * <p>
	 * 兼具 $O(1)$ 串流低記憶體特性與強型別便利性，內部接收資料 Map 分塊後自動映射為 Model 清單回呼。
	 *
	 * @param <T>           模型型別
	 * @param inputStream   Excel 輸入串流
	 * @param query         讀取查詢條件
	 * @param modelClass    模型類別 Class
	 * @param chunkSize     分塊大小
	 * @param chunkConsumer 分塊消費者 (接收強型別 Model 清單)
	 */
	default <T> void readBeansChunked(InputStream inputStream, ReadExcelQuery query, Class<T> modelClass,
			int chunkSize, java.util.function.Consumer<java.util.List<T>> chunkConsumer) {
		readChunked(inputStream, query, chunkSize, (chunk, context) -> {
			java.util.List<T> beanChunk = com.example.demo.application.assembler.ExcelBeanAssembler.fromMaps(chunk, modelClass);
			chunkConsumer.accept(beanChunk);
		});
	}

	/**
	 * 串流分塊讀取並宣告式轉換為 Model 批次清單 (從 byte 陣列讀取)
	 *
	 * @param <T>           模型型別
	 * @param excelBytes    Excel 二進位位元組陣列
	 * @param query         讀取查詢條件
	 * @param modelClass    模型類別 Class
	 * @param chunkSize     分塊大小
	 * @param chunkConsumer 分塊消費者
	 */
	default <T> void readBeansChunked(byte[] excelBytes, ReadExcelQuery query, Class<T> modelClass,
			int chunkSize, java.util.function.Consumer<java.util.List<T>> chunkConsumer) {
		readChunked(excelBytes, query, chunkSize, (chunk, context) -> {
			java.util.List<T> beanChunk = com.example.demo.application.assembler.ExcelBeanAssembler.fromMaps(chunk, modelClass);
			chunkConsumer.accept(beanChunk);
		});
	}
}
