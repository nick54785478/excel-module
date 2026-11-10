package com.example.demo.application.port.in.parallel;

import java.io.OutputStream;

import com.example.demo.application.shared.command.ExportPartitionZipCommand;
import com.example.demo.application.shared.command.ExportPipelineExcelCommand;

/**
 * 高並發平行與分片 Excel 匯出業務合約 (Export Parallel & Partition Excel Inbound Port)
 * <p>
 * 遵循 Clean Architecture 六角形架構規範，作為應用層開放予表現層（REST Controller / 排程服務）調用之 Inbound Port。<br>
 * 命名規範：以 {@code UseCase} 結尾 (3-8 規範)。
 * 涵蓋兩大企業級平行加速架構：
 * <ul>
 *     <li><b>模式 A (微觀管線)</b>：平行查詢資料庫 + 背壓佇列 + 專屬單緒循序串流寫入。</li>
 *     <li><b>模式 B (宏觀分片)</b>：突破 Excel 單表百萬列限制，多個獨立活頁簿並行產製，串流封裝為 ZIP 壓縮檔。</li>
 * </ul>
 */
public interface ExportParallelExcelUseCase {

	/**
	 * 模式 A：以微觀生產者-消費者管線串流寫出單一 Excel 檔案至目標輸出串流
	 *
	 * @param outputStream 目標輸出串流 (不得為 null)
	 * @param command      管線匯出命令載體 (不得為 null)
	 */
	void exportPipelineStream(OutputStream outputStream, ExportPipelineExcelCommand command);

	/**
	 * 模式 A：以微觀生產者-消費者管線產製單一 Excel 檔案位元組陣列
	 *
	 * @param command 管線匯出命令載體 (不得為 null)
	 * @return Excel (.xlsx) 二進位位元組陣列
	 */
	byte[] exportPipeline(ExportPipelineExcelCommand command);

	/**
	 * 模式 B / 模式 A+B：以多活頁簿分片並行產製並動態串流壓縮寫入 ZIP 輸出串流
	 *
	 * @param outputStream 目標輸出串流 (不得為 null)
	 * @param command      分片 ZIP 匯出命令載體 (不得為 null)
	 */
	void exportPartitionZipStream(OutputStream outputStream, ExportPartitionZipCommand command);

	/**
	 * 模式 B / 模式 A+B：以多活頁簿分片並行產製並產出完整 ZIP 二進位位元組陣列
	 *
	 * @param command 分片 ZIP 匯出命令載體 (不得為 null)
	 * @return ZIP 壓縮檔二進位位元組陣列
	 */
	byte[] exportPartitionZip(ExportPartitionZipCommand command);
}

