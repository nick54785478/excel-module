package com.example.demo.application.port.in.validation;

import java.io.InputStream;
import java.util.List;

import com.example.demo.application.shared.dto.ExcelValidationGottenResult;

/**
 * Excel 宣告式資料檢核業務案例輸入埠 (Validate Excel UseCase - Inbound Port)
 * <p>
 * <b>【架構定位與職責規範】</b><br>
 * 位於 Clean Architecture 應用層 Inbound Port 套件 ({@code application/port/in/})，
 * 遵循 {@code AGENTS.md} 規則 18 與 3-8 (以 {@code UseCase} 結尾)。<br>
 * 代表應用層對表現層 (Controller) 或批次排程器開放的資料檢核業務合約，
 * 支援將原始 Excel 串流反序列化為指定領域/應用層模型，調度多層次客製驗證管線 (含 EasyRules)，
 * 收集全量錯誤清單並產生錯誤回標檔案，傳參及回傳型別嚴禁使用任何技術類環境物件。
 * </p>
 */
public interface ValidateExcelUseCase {

	/**
	 * 針對輸入之 Excel 檔案串流執行解析與客製業務檢核
	 *
	 * @param excelStream 目標 Excel 檔案二進位輸入串流 (不得為 null)
	 * @param modelClass  目標資料模型類別 (支援標註 {@code @ExcelSheet} / {@code @ExcelColumn} 之 Record 或 JavaBean)
	 * @param <T>         目標模型型別
	 * @return 包含合格資料與全量錯誤清單之檢核結果資料載體 (ExcelValidationGottenResult)
	 */
	<T> ExcelValidationGottenResult<T> validate(InputStream excelStream, Class<T> modelClass);

	/**
	 * 針對已反序列化之記憶體資料列清單執行客製業務檢核
	 *
	 * @param sheetName  工作表名稱
	 * @param rows       資料模型清單
	 * @param modelClass 目標資料模型類別
	 * @param <T>        目標模型型別
	 * @return 檢核結果資料載體 (ExcelValidationGottenResult)
	 */
	<T> ExcelValidationGottenResult<T> validateRows(String sheetName, List<T> rows, Class<T> modelClass);

	/**
	 * 執行檢核；若發現不合格資料則自動渲染產製「錯誤回標檔」二進位資料
	 *
	 * @param excelStream 使用者上傳之原始 Excel 輸入串流
	 * @param modelClass  目標資料模型類別
	 * @param <T>         目標模型型別
	 * @return 若檢核完全通過則回傳空 byte 陣列 (或合格標記)；若包含錯誤則回傳帶有淡紅底深紅字高亮標註之 Excel 檔案二進位資料
	 */
	<T> byte[] validateAndGenerateFeedback(InputStream excelStream, Class<T> modelClass);
}

