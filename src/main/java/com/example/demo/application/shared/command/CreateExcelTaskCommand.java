package com.example.demo.application.shared.command;

import java.util.Objects;

import com.example.demo.application.shared.vo.ExcelTaskType;

/**
 * 建立 Excel 長任務命令 (Create Excel Task Command)
 * <p>
 * 遵循 AGENTS.md 3-4 命名規範 ({@code V + N + Command})。
 * 用於在基礎設施層初始化非同步任務實體與狀態記錄。
 *
 * @param taskId    任務識別碼 (UUID)
 * @param taskType  任務類型 (IMPORT / EXPORT)
 * @param fileName  關聯檔案名稱
 * @param totalRows 預估總列數 (若未知則為 0)
 */
public record CreateExcelTaskCommand(
		String taskId,
		ExcelTaskType taskType,
		String fileName,
		long totalRows
) {
	public CreateExcelTaskCommand {
		Objects.requireNonNull(taskId, "taskId 不得為空");
		Objects.requireNonNull(taskType, "taskType 不得為空");
	}
}

