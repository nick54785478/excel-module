package com.example.demo.application.port.out.task;

import java.io.OutputStream;
import java.util.Optional;

/**
 * Excel 長任務產出檔案儲存庫輸出埠 (Excel Task Storage Outbound Port)
 * <p>
 * 遵循 Clean Architecture 六角形架構與 AGENTS.md 規則 18/24，
 * 定義應用層對外要求基礎設施層提供之任務產物二進位資料儲存、串流雙向分流與讀取合約。
 * <b>嚴禁接發任何 ORM 或技術相依實體</b>，專注於檔案二進位位元組與輸出串流之抽象操作。
 * <p>
 * 命名規範：以 {@code Port} 結尾 (符合 AGENTS.md 3-8 規範)。
 */
public interface ExcelTaskStoragePort {

	/**
	 * 儲存已完成長任務之檔案二進位位元組至持久化儲存介質
	 *
	 * @param taskId   長任務唯一識別碼 (UUID)
	 * @param fileName 原始產出檔案名稱 (例如 "material_master.xlsx")
	 * @param content  檔案完整二進位位元組陣列
	 */
	void store(String taskId, String fileName, byte[] content);

	/**
	 * 包裝目標 {@link OutputStream}，以分流 (Tee) 模式在向目標端串流輸出的同時同步寫入磁碟暫存檔
	 * <p>
	 * 適用於大數據即時串流下載場景，在將數據邊讀邊沖刷給客戶端 HTTP Response 的同時，
	 * 一併落盤保存，供後續透過 {@code /api/v1/excel/tasks/{taskId}/download} 再次提取。
	 * </p>
	 *
	 * @param taskId       長任務唯一識別碼
	 * @param fileName     原始產出檔案名稱
	 * @param targetStream 目標原始輸出串流 (例如 ServletOutputStream)
	 * @return 具備分流暫存能力之 {@link OutputStream}
	 */
	OutputStream wrapOutputStream(String taskId, String fileName, OutputStream targetStream);

	/**
	 * 標記指定任務之串流輸出已完成，並安全沖刷並關閉底層檔案暫存輸出串流
	 *
	 * @param taskId 長任務唯一識別碼
	 */
	void finishStream(String taskId);

	/**
	 * 依據任務識別碼讀取已落盤之檔案二進位位元組
	 *
	 * @param taskId 長任務唯一識別碼
	 * @return 檔案二進位資料 (若檔案不存在或已過期則回傳 {@link Optional#empty()})
	 */
	Optional<byte[]> retrieve(String taskId);

	/**
	 * 依據任務識別碼刪除對應之暫存檔案
	 *
	 * @param taskId 長任務唯一識別碼
	 * @return 若刪除成功回傳 {@code true}，否則回傳 {@code false}
	 */
	boolean delete(String taskId);
}

