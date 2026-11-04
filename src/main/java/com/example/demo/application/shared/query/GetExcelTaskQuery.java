package com.example.demo.application.shared.query;

import java.util.Objects;

/**
 * 查詢 Excel 長任務進度查詢物件 (Get Excel Task Query)
 * <p>
 * 遵循 AGENTS.md 3-5 命名規範 ({@code V + N + Query})。
 * 負責作為查詢長任務狀態之條件載體。
 *
 * @param taskId 欲查詢之任務識別碼
 */
public record GetExcelTaskQuery(String taskId) {

	public GetExcelTaskQuery {
		Objects.requireNonNull(taskId, "taskId 不得為空");
	}

	public static GetExcelTaskQuery of(String taskId) {
		return new GetExcelTaskQuery(taskId);
	}
}

