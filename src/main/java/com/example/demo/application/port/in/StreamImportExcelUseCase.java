package com.example.demo.application.port.in;

import java.io.InputStream;
import java.util.List;
import java.util.function.Consumer;

import com.example.demo.application.shared.command.StreamImportExcelCommand;
import com.example.demo.application.shared.dto.ExcelStreamImportGottenResult;
import com.example.demo.application.shared.query.ReadExcelQuery;

/**
 * Excel 大數據分塊串流匯入業務合約 (Stream Import Excel Use Case)
 * <p>
 * 遵循 Clean Architecture 六角形架構規範，作為 Inbound Port 開放予表現層調用。
 * 命名規範：以 {@code UseCase} 結尾 (3-8 規範)。
 */
public interface StreamImportExcelUseCase {

	/**
	 * 執行大數據串流分塊讀取與統計處理
	 *
	 * @param command 串流匯入命令物件，封裝輸入源、目標工作表與分塊大小
	 * @return 串流匯入結果統計 DTO
	 */
	ExcelStreamImportGottenResult streamImport(StreamImportExcelCommand command);

	/**
	 * 串流分塊讀取並宣告式轉換為強型別 Model 批次清單 (支援 POJO 與 Java 21 Record)
	 *
	 * @param <T>           模型型別
	 * @param inputStream   Excel 輸入串流
	 * @param query         讀取查詢條件
	 * @param modelClass    模型類別 Class
	 * @param chunkSize     分塊大小
	 * @param chunkConsumer 分塊消費者 (接收強型別 Model 清單)
	 */
	<T> void streamImportBeans(InputStream inputStream, ReadExcelQuery query, Class<T> modelClass,
			int chunkSize, Consumer<List<T>> chunkConsumer);
}


