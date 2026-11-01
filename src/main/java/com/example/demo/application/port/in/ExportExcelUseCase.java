package com.example.demo.application.port.in;

import java.util.List;

import com.example.demo.application.shared.command.ExportExcelCommand;

/**
 * Excel 檔案匯出業務合約 (Export Excel Use Case)
 * <p>
 * 遵循 Clean Architecture 六角形架構規範，作為 Inbound Port 開放予表現層調用。
 * 命名規範：以 {@code UseCase} 結尾 (3-8 規範)。
 */
public interface ExportExcelUseCase {

	/**
	 * 執行 Excel 檔案匯出並產出檔案二進位陣列
	 *
	 * @param command 匯出命令物件，封裝檔案名稱與各工作表內容
	 * @return Excel (.xlsx) 檔案二進位資料
	 */
	byte[] export(ExportExcelCommand command);

	/**
	 * 宣告式將強型別 Model 物件清單匯出為 Excel 二進位資料 (支援 POJO 與 Java 21 Record)
	 *
	 * @param <T>        模型型別
	 * @param dataList   資料物件清單
	 * @param modelClass 模型類別 Class
	 * @return Excel (.xlsx) 檔案二進位資料
	 */
	<T> byte[] exportBeans(List<T> dataList, Class<T> modelClass);

	/**
	 * 宣告式將強型別 Model 物件清單匯出為 Excel 二進位資料 (可覆蓋指定工作表名稱)
	 *
	 * @param <T>             模型型別
	 * @param dataList        資料物件清單
	 * @param modelClass      模型類別 Class
	 * @param customSheetName 自訂工作表名稱
	 * @return Excel (.xlsx) 檔案二進位資料
	 */
	<T> byte[] exportBeans(List<T> dataList, Class<T> modelClass, String customSheetName);
}


