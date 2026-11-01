package com.example.demo.application.port.in;

import java.io.OutputStream;
import java.util.List;

import com.example.demo.application.shared.command.StreamExportExcelCommand;

/**
 * Excel 大數據串流匯出業務合約 (Stream Export Excel Use Case)
 * <p>
 * 遵循 Clean Architecture 六角形架構規範，作為 Inbound Port 開放予表現層調用。
 * 命名規範：以 {@code UseCase} 結尾 (3-8 規範)。
 */
public interface StreamExportExcelUseCase {

	/**
	 * 執行大數據串流匯出，直接將資料分批 Flush 至指定的輸出串流
	 *
	 * @param outputStream 目標輸出串流 (如 HTTP 回應串流)
	 * @param command      串流匯出命令物件，包含資料流與分批沖刷大小
	 */
	void streamExport(OutputStream outputStream, StreamExportExcelCommand command);

	/**
	 * 宣告式將強型別 Model 物件清單串流寫入輸出串流
	 *
	 * @param <T>          模型型別
	 * @param outputStream 輸出資料流 (不得為 null)
	 * @param dataList     資料物件清單
	 * @param modelClass   模型類別 Class (支援 POJO 與 Java 21 Record)
	 */
	<T> void streamExportBeans(OutputStream outputStream, List<T> dataList, Class<T> modelClass);

	/**
	 * 宣告式將強型別 Model 物件清單串流寫入輸出串流 (自訂工作表名稱)
	 *
	 * @param <T>             模型型別
	 * @param outputStream    輸出資料流 (不得為 null)
	 * @param dataList        資料物件清單
	 * @param modelClass      模型類別 Class
	 * @param customSheetName 自訂工作表名稱
	 */
	<T> void streamExportBeans(OutputStream outputStream, List<T> dataList, Class<T> modelClass, String customSheetName);

	/**
	 * 將資料字典清單 (Map List) 串流寫入指定輸出串流
	 *
	 * @param outputStream 輸出資料流 (不得為 null)
	 * @param sheetName    工作表名稱
	 * @param headers      標題列清單
	 * @param mapRows      資料字典清單 (Key 為 Header 名稱)
	 */
	void streamExportMaps(OutputStream outputStream, String sheetName, List<String> headers, java.util.Map<String, Object>... mapRows);

	/**
	 * 將資料字典清單 (Map List) 串流寫入指定輸出串流
	 *
	 * @param outputStream 輸出資料流 (不得為 null)
	 * @param sheetName    工作表名稱
	 * @param headers      標題列清單
	 * @param mapRows      資料字典清單 (Key 為 Header 名稱)
	 */
	void streamExportMaps(OutputStream outputStream, String sheetName, List<String> headers, List<java.util.Map<String, Object>> mapRows);
}


