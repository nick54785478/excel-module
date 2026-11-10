package com.example.demo.application.port.out.core;

import java.io.OutputStream;
import java.util.List;

import com.example.demo.application.shared.command.WriteExcelCommand;
import com.example.demo.application.shared.command.WriteSheetCommand;
import com.example.demo.application.shared.command.WriteStreamExcelCommand;
import com.example.demo.application.shared.exception.ExcelWriteException;

/**
 * Excel 寫入輸出端口 (Outbound Port)
 * <p>
 * 依據 Clean Architecture 與六角形架構規範，定義應用層對外要求基礎設施層提供的 Excel 產製與寫入合約。
 * 本介面隔離底層第三方技術實作 (如 FastExcel、Apache POI 等)，保護應用層不直接依賴特定技術細節。
 * 命名規範：以 {@code Port} 結尾 (3-8 規範)。
 */
public interface ExcelWriterPort {

	/**
	 * 將 Excel 活頁簿資料串流寫入指定的輸出串流 (Streaming Write)
	 * <p>
	 * 適用於大數據量匯出，以串流方式寫入目的地，避免記憶體溢位 (OOM)。
	 *
	 * @param outputStream 輸出資料流 (不得為 null)
	 * @param command      Excel 活頁簿寫入命令資料
	 * @throws ExcelWriteException 當寫入過程中發生 I/O 或格式化錯誤時拋出
	 */
	void write(OutputStream outputStream, WriteExcelCommand command);

	/**
	 * 將 Excel 活頁簿資料寫入並回傳 byte 陣列 (In-memory Write)
	 * <p>
	 * 適用於檔案上傳、郵件附件或小型報表暫存需求。
	 *
	 * @param command Excel 活頁簿寫入命令資料
	 * @return Excel 檔案 (.xlsx) 之二進位 byte 陣列
	 * @throws ExcelWriteException 當寫入過程中發生 I/O 或格式化錯誤時拋出
	 */
	byte[] write(WriteExcelCommand command);

	/**
	 * 便捷方法：將單一工作表資料串流寫入指定的輸出串流
	 *
	 * @param outputStream 輸出資料流 (不得為 null)
	 * @param sheetName    工作表名稱
	 * @param headers      標題列清單
	 * @param rows         資料列清單 (每列包含欄位物件)
	 * @throws ExcelWriteException 當寫入過程中發生錯誤時拋出
	 */
	default void writeSingleSheet(OutputStream outputStream, String sheetName, List<String> headers, List<List<Object>> rows) {
		WriteSheetCommand sheet = WriteSheetCommand.of(sheetName, headers, rows);
		write(outputStream, WriteExcelCommand.of(sheet));
	}

	/**
	 * 便捷方法：將單一工作表資料寫入並回傳 byte 陣列
	 *
	 * @param sheetName 工作表名稱
	 * @param headers   標題列清單
	 * @param rows      資料列清單 (每列包含欄位物件)
	 * @return Excel 檔案 (.xlsx) 之二進位 byte 陣列
	 * @throws ExcelWriteException 當寫入過程中發生錯誤時拋出
	 */
	default byte[] writeSingleSheet(String sheetName, List<String> headers, List<List<Object>> rows) {
		WriteSheetCommand sheet = WriteSheetCommand.of(sheetName, headers, rows);
		return write(WriteExcelCommand.of(sheet));
	}

	/**
	 * 串流式大量資料寫入 Excel 活頁簿 (Streaming Write)
	 * <p>
	 * 接收延遲載入或迭代器資料源，寫入期間定時執行 flush 將快取刷入輸出流，達成 O(1) 記憶體開銷。
	 *
	 * @param outputStream 輸出資料流 (不得為 null)
	 * @param command      串流活頁簿寫入命令資料
	 * @throws ExcelWriteException 當寫入過程中發生 I/O 或格式化錯誤時拋出
	 */
	void writeStream(OutputStream outputStream, WriteStreamExcelCommand command);

	/**
	 * 串流式大量資料寫入並回傳 byte 陣列
	 *
	 * @param command 串流活頁簿寫入命令資料
	 * @return Excel 檔案 (.xlsx) 之二進位 byte 陣列
	 * @throws ExcelWriteException 當寫入過程中發生錯誤時拋出
	 */
	byte[] writeStream(WriteStreamExcelCommand command);
}
