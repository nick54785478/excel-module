package com.example.demo.application.port.out.parallel;

import java.io.OutputStream;

import com.example.demo.application.shared.command.ExportPartitionZipCommand;
import com.example.demo.application.shared.command.ExportPipelineExcelCommand;

/**
 * 高並發平行管線與分片 ZIP 輸出埠 (Excel Partition & Pipeline Writer Outbound Port)
 * <p>
 * 遵循 Clean Architecture 六角形架構規範，作為應用層對外要求基礎設施層提供之平行加速寫入合約。<br>
 * 命名規範：以 {@code Port} 結尾 (3-8 規範)。<br>
 * 嚴禁於傳參及回傳型別使用任何特定技術或環境相依物件。
 */
public interface ExcelPartitionWriterPort {

	/**
	 * 模式 A：執行微觀管線串流寫出
	 *
	 * @param outputStream 目標二進位輸出串流
	 * @param command      管線匯出命令
	 */
	void writePipelineStream(OutputStream outputStream, ExportPipelineExcelCommand command);

	/**
	 * 模式 A：執行微觀管線寫出並回傳 byte 陣列
	 *
	 * @param command 管線匯出命令
	 * @return Excel 二進位位元組陣列
	 */
	byte[] writePipeline(ExportPipelineExcelCommand command);

	/**
	 * 模式 B / 模式 A+B：執行多活頁簿分片平行產製並動態串流壓縮寫入 ZIP
	 *
	 * @param outputStream 目標 ZIP 輸出串流
	 * @param command      分片 ZIP 匯出命令
	 */
	void writePartitionZipStream(OutputStream outputStream, ExportPartitionZipCommand command);

	/**
	 * 模式 B / 模式 A+B：執行多活頁簿分片平行產製並回傳 ZIP byte 陣列
	 *
	 * @param command 分片 ZIP 匯出命令
	 * @return ZIP 壓縮檔二進位位元組陣列
	 */
	byte[] writePartitionZip(ExportPartitionZipCommand command);
}

