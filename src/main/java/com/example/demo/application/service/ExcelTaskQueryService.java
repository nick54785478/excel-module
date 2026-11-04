package com.example.demo.application.service;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.application.port.in.GetExcelTaskUseCase;
import com.example.demo.application.port.out.ExcelTaskRepositoryPort;
import com.example.demo.application.shared.dto.ExcelTaskGottenResult;
import com.example.demo.application.shared.exception.ExcelTaskNotFoundException;
import com.example.demo.application.shared.query.GetExcelTaskQuery;

/**
 * Excel 長任務進度查詢應用服務 (Excel Task Query Service / Inbound Adapter)
 * <p>
 * 遵循 Clean Architecture 六角形架構與 CQRS 規範，作為查詢端 (Query Side) 的 Inbound Adapter。
 * 命名規範符合 AGENTS.md 3-9 規範 (以 {@code QueryService} 結尾)。
 * 實作應用層 {@link GetExcelTaskUseCase} 業務合約。
 * <p>
 * 協調基礎設施輸出端口 {@link ExcelTaskRepositoryPort} 依據任務識別碼查詢當前狀態、處理進度百分比與產出連結。
 * 若查無該長任務記錄，則拋出特定領域/應用層例外 {@link ExcelTaskNotFoundException}，由表現層統一轉譯為 HTTP 404
 * Not Found。
 * <p>
 * 涵蓋技術模組：
 * <ul>
 * <li><b>【長任務進度狀態查詢模組 (Async Task Progress Query)】</b>：
 * 提供前端輪詢 (Polling) 或查詢當前批次處理列數、進度百分比、檔案下載路徑與異常錯誤日誌。</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class ExcelTaskQueryService implements GetExcelTaskUseCase {

	private final ExcelTaskRepositoryPort excelTaskRepositoryPort;

	public ExcelTaskQueryService(ExcelTaskRepositoryPort excelTaskRepositoryPort) {
		this.excelTaskRepositoryPort = Objects.requireNonNull(excelTaskRepositoryPort, "excelTaskRepositoryPort 不得為空");
	}

	/**
	 * 【長任務進度狀態查詢模組 (Async Task Progress Query)】
	 * <p>
	 * 依據任務識別碼查詢長任務之當前進度與執行狀態。
	 *
	 * @param query 查詢條件物件 (包含 taskId)
	 * @return 長任務當前結果 DTO
	 * @throws ExcelTaskNotFoundException 當查無該 taskId 時拋出
	 */
	@Override
	public ExcelTaskGottenResult getTask(GetExcelTaskQuery query) {
		Objects.requireNonNull(query, "GetExcelTaskQuery 不得為空");

		return excelTaskRepositoryPort.findTaskById(query.taskId())
				.orElseThrow(() -> new ExcelTaskNotFoundException("查無指定識別碼之 Excel 長任務: " + query.taskId()));
	}
}
