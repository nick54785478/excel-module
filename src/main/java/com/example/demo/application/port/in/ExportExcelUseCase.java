package com.example.demo.application.port.in;

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
}

