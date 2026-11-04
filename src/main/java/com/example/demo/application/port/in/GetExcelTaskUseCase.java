package com.example.demo.application.port.in;

import com.example.demo.application.shared.dto.ExcelTaskGottenResult;
import com.example.demo.application.shared.exception.ExcelTaskNotFoundException;
import com.example.demo.application.shared.query.GetExcelTaskQuery;

/**
 * 查詢 Excel 長任務進度使用案例輸入埠 (Get Excel Task Use Case / Inbound Port)
 * <p>
 * 遵循 Clean Architecture 六角形架構規範，提供前端輪詢或查詢非同步任務狀態、處理列數與百分比之合約。
 * <p>
 * 命名規範：以 {@code UseCase} 結尾 (符合 AGENTS.md 3-8 規範)。
 */
public interface GetExcelTaskUseCase {

	/**
	 * 依據任務查詢條件獲取長任務進度與狀態
	 *
	 * @param query 查詢條件物件 (包含 taskId)
	 * @return 長任務當前結果 DTO
	 * @throws ExcelTaskNotFoundException 當查無該 taskId 時拋出
	 */
	ExcelTaskGottenResult getTask(GetExcelTaskQuery query);
}

