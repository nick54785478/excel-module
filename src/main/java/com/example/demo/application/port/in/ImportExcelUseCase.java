package com.example.demo.application.port.in;

import java.io.InputStream;

import com.example.demo.application.shared.dto.ExcelGottenResult;
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
}

