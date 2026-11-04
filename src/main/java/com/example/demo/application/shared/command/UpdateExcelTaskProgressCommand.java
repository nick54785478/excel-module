package com.example.demo.application.shared.command;

import java.util.Objects;

/**
 * 更新 Excel 長任務進度命令 (Update Excel Task Progress Command)
 * <p>
 * 遵循 AGENTS.md 3-4 命名規範 ({@code V + N + Command})。
 * 用於串流處理時向基礎設施層回報已處理之列數與最新總列數。
 *
 * @param taskId        任務識別碼
 * @param processedRows 當前已處理列數
 * @param totalRows     預期總列數
 */
public record UpdateExcelTaskProgressCommand(
		String taskId,
		long processedRows,
		long totalRows
) {
	public UpdateExcelTaskProgressCommand {
		Objects.requireNonNull(taskId, "taskId 不得為空");
	}
}

