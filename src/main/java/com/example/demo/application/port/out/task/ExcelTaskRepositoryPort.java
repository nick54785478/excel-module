package com.example.demo.application.port.out.task;

import java.util.Optional;

import com.example.demo.application.shared.command.CreateExcelTaskCommand;
import com.example.demo.application.shared.dto.ExcelTaskGottenResult;

/**
 * Excel 長任務基礎設施輸出埠 (Excel Task Outbound Port)
 * <p>
 * 遵循 Clean Architecture 六角形架構與 AGENTS.md 規則 18/24，
 * 定義應用層對外要求基礎設施層提供之非同步長任務狀態持久化與查詢合約。
 * <b>嚴禁接發任何 ORM 技術實體 (如 ExcelTaskJpaEntity)</b>，僅使用 Command 與 DTO。
 * <p>
 * 命名規範：以 {@code Port} 結尾 (符合 AGENTS.md 3-8 規範)。
 */
public interface ExcelTaskRepositoryPort {

	/**
	 * 於基礎設施層建立並持久化初始長任務記錄
	 *
	 * @param command 建立任務命令
	 * @return 建立完成之任務結果 DTO
	 */
	ExcelTaskGottenResult createTask(CreateExcelTaskCommand command);

	/**
	 * 更新長任務執行進度與已處理筆數
	 *
	 * @param taskId        任務識別碼
	 * @param processedRows 已處理列數
	 * @param totalRows     預期總列數
	 * @return 更新後之任務結果 DTO
	 */
	ExcelTaskGottenResult updateProgress(String taskId, long processedRows, long totalRows);

	/**
	 * 標記長任務為處理中狀態 (PROCESSING)
	 *
	 * @param taskId 任務識別碼
	 * @return 更新後之任務結果 DTO
	 */
	ExcelTaskGottenResult markProcessing(String taskId);

	/**
	 * 標記長任務為已完成 (COMPLETED) 並記錄產出檔案位址
	 *
	 * @param taskId    任務識別碼
	 * @param fileUrl   產出檔案位址或下載識別碼
	 * @param totalRows 實際最終完成總列數
	 * @return 更新後之任務結果 DTO
	 */
	ExcelTaskGottenResult markCompleted(String taskId, String fileUrl, long totalRows);

	/**
	 * 標記長任務為失敗 (FAILED) 並記錄異常原因
	 *
	 * @param taskId       任務識別碼
	 * @param errorMessage 異常摘要原因
	 * @return 更新後之任務結果 DTO
	 */
	ExcelTaskGottenResult markFailed(String taskId, String errorMessage);

	/**
	 * 依據任務識別碼查詢長任務進度
	 *
	 * @param taskId 任務識別碼
	 * @return 任務結果 DTO (若存在)
	 */
	Optional<ExcelTaskGottenResult> findTaskById(String taskId);
}
