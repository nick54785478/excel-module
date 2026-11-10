package com.example.demo.application.port.out.validation;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import com.example.demo.application.shared.model.ExcelRowError;

/**
 * Excel 錯誤標註器輸出埠 (Excel Error Marker Port - Outbound Port)
 * <p>
 * <b>【架構定位與職責規範】</b><br>
 * 位於 Clean Architecture 應用層 Outbound Port 套件 ({@code application/port/out/})，
 * 遵循 {@code AGENTS.md} 規則 18 與 3-8 (以 {@code Port} 結尾)。<br>
 * 定義應用層對外要求基礎設施層提供的「錯誤標註試算表 (Error-Marked Spreadsheet)」渲染合約，
 * 負責保留使用者上傳之原始資料排版，在出錯儲存格套用淡紅底深紅字高亮樣式，並於最右側動態追加錯誤原因說明欄。
 * 隔離 Apache POI / FastExcel 等樣式繪製技術細節。
 * </p>
 */
public interface ExcelErrorMarkerPort {

	/**
	 * 讀取原始 Excel 串流，依據錯誤清單渲染樣式並將標註檔寫入輸出串流
	 *
	 * @param originalExcelStream 使用者上傳之原始 Excel 輸入串流 (不得為 null)
	 * @param errors              全量檢核錯誤事實清單 (不得為 null)
	 * @param targetOutputStream 渲染完成後之目標輸出串流 (不得為 null)
	 */
	void writeErrorMarkedWorkbook(InputStream originalExcelStream, List<ExcelRowError> errors, OutputStream targetOutputStream);

	/**
	 * 便捷方法：產製錯誤標註回標檔並回傳二進位 byte 陣列
	 *
	 * @param originalExcelStream 使用者上傳之原始 Excel 輸入串流
	 * @param errors              全量檢核錯誤事實清單
	 * @return 包含錯誤標註與原因說明之 Excel 檔案二進位資料
	 */
	byte[] generateErrorMarkedWorkbook(InputStream originalExcelStream, List<ExcelRowError> errors);
}

