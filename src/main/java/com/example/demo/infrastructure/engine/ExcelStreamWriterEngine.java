package com.example.demo.infrastructure.engine;

import java.io.OutputStream;

import com.example.demo.application.shared.command.WriteStreamExcelCommand;

/**
 * 基礎設施層底層 Excel 串流寫入引擎合約 (Infrastructure Excel Stream Writer Engine SPI)
 * <p>
 * <b>【架構定位與職責說明】</b><br>
 * 遵循 Clean Architecture 六角形架構設計準則：
 * <ul>
 *     <li>本介面為 <b>Infrastructure 層內部共用引擎合約 (Internal SPI)</b>，專門封裝底層二進位活頁簿串流序列化邏輯。</li>
 *     <li>提供基礎設施適配器（例如 {@code PoiSxssfWriterAdapter}、{@code FastExcelWriterAdapter} 與
 *         {@code ParallelPartitionExcelWriterAdapter}）共用之低階 I/O 序列化能力。</li>
 *     <li><b>嚴禁作為應用層輸出埠 (Outbound Port)</b>，徹底杜絕適配器內部取巧跨層依賴應用層 Port 的架構壞味道。</li>
 * </ul>
 */
public interface ExcelStreamWriterEngine {

	/**
	 * 串流寫入二進位 Excel 活頁簿至目標 OutputStream
	 *
	 * @param outputStream 目標二進位輸出串流 (不可為 null)
	 * @param command      大數據串流寫入命令 (不可為 null)
	 */
	void writeStream(OutputStream outputStream, WriteStreamExcelCommand command);

	/**
	 * 串流寫入二進位 Excel 活頁簿並產出 byte 陣列
	 *
	 * @param command 大數據串流寫入命令 (不可為 null)
	 * @return Excel 二進位檔案 byte 陣列
	 */
	byte[] writeStream(WriteStreamExcelCommand command);
}

