package com.example.demo.application.port.in.task;

/**
 * 下載已完成 Excel 長任務產出檔案使用案例輸入埠 (Download Excel Task File Use Case / Inbound Port)
 * <p>
 * 遵循 Clean Architecture 六角形架構規範，作為表現層控制器取得已完成長任務產出二進位檔案內容之 Inbound Port 業務合約。
 * 隔離表現層與應用層具體服務實作，確保控制器僅依賴業務合約介面。
 * <p>
 * 命名規範：以 {@code UseCase} 結尾 (符合 AGENTS.md 3-8 規範)。
 */
public interface DownloadExcelTaskFileUseCase {

	/**
	 * 取得指定已完成長任務產出之檔案二進位內容
	 *
	 * @param taskId 任務唯一識別碼
	 * @return 檔案二進位位元組陣列 (若尚未完成或檔案已過期不存在則回傳 {@code null})
	 */
	byte[] getExportedFile(String taskId);
}

