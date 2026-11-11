package com.example.demo.application.port.in.core;

import java.io.InputStream;
import java.util.List;

import com.example.demo.application.shared.dto.BeanSheetGottenResult;
import com.example.demo.application.shared.dto.ExcelGottenResult;
import com.example.demo.application.shared.dto.ExcelSheetGottenResult;
import com.example.demo.application.shared.query.ReadExcelQuery;

/**
 * Excel 檔案匯入解析業務合約 (Import Excel Use Case)
 * <p>
 * 遵循 Clean Architecture 六角形架構規範，作為 Inbound Port 開放予表現層調用。
 * 命名規範：以 {@code UseCase} 結尾 (3-8 規範)。
 */
public interface ImportExcelUseCase {

	/**
	 * 執行 Excel 檔案解析並回傳各工作表資料
	 *
	 * @param inputStream 檔案輸入串流
	 * @param query       讀取篩選查詢物件
	 * @return 讀取結果 DTO
	 */
	ExcelGottenResult importExcel(InputStream inputStream, ReadExcelQuery query);

	/**
	 * 執行 Excel 檔案解析並宣告式轉換為強型別 Model 清單 (支援 POJO 與 Java 21 Record)
	 *
	 * @param <T>         模型型別
	 * @param inputStream 檔案輸入串流
	 * @param modelClass  模型類別 Class
	 * @return 強型別 Model 清單
	 */
	<T> List<T> importBeans(InputStream inputStream, Class<T> modelClass);

	/**
	 * 依據查詢條件執行 Excel 檔案解析並宣告式轉換為強型別 Model 清單
	 *
	 * @param <T>         模型型別
	 * @param inputStream 檔案輸入串流
	 * @param query       讀取篩選查詢物件
	 * @param modelClass  模型類別 Class
	 * @return 強型別 Model 清單
	 */
	<T> List<T> importBeans(InputStream inputStream, ReadExcelQuery query, Class<T> modelClass);

	/**
	 * 依據目標模型上的 {@link com.example.demo.application.annotation.mapping.ExcelSheet} 宣告式註解，
	 * 自動智慧探測活頁簿中最相符的工作表並解析為強型別模型結果 DTO。
	 *
	 * @param <T>         模型型別
	 * @param inputStream 檔案輸入串流
	 * @param modelClass  目標模型類別 Class
	 * @return 包含真實工作表名稱與模型集合之 {@link BeanSheetGottenResult}
	 * @throws IllegalArgumentException 當未找到任何匹配之工作表或檔案為空時拋出
	 */
	<T> BeanSheetGottenResult<T> importBeanSheet(InputStream inputStream, Class<T> modelClass);

	/**
	 * 依據呼叫端指定之工作表名稱 (sheetName) 進行嚴格比對並解析為強型別模型結果 DTO。
	 * <p>
	 * 若在 Excel 活頁簿中找不到該指定名稱之工作表，嚴格拋出 {@link IllegalArgumentException} 例外以防止錯誤降級。
	 * </p>
	 *
	 * @param <T>         模型型別
	 * @param inputStream 檔案輸入串流
	 * @param sheetName   指定讀取之工作表名稱 (必填)
	 * @param modelClass  目標模型類別 Class
	 * @return 包含真實工作表名稱與模型集合之 {@link BeanSheetGottenResult}
	 * @throws IllegalArgumentException 當指定之工作表名稱為空或活頁簿中找不到該工作表時拋出
	 */
	<T> BeanSheetGottenResult<T> importBeanSheet(InputStream inputStream, String sheetName, Class<T> modelClass);

	/**
	 * 執行 Excel 檔案解析並回傳第一個工作表純資料載體
	 *
	 * @param inputStream 檔案輸入串流
	 * @return 第一個工作表讀取結果物件 {@link ExcelSheetGottenResult}
	 */
	ExcelSheetGottenResult importFirstSheet(InputStream inputStream);
}


