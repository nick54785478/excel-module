package com.example.demo.presentation.assembler;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.example.demo.application.shared.dto.ExcelTaskGottenResult;
import com.example.demo.presentation.resource.out.ExcelTaskRetrievedResource;

/**
 * Excel 長任務表現層資源組裝器 (Excel Task Presentation Resource Assembler)
 * <p>
 * 遵循 Clean Architecture 與 AGENTS.md 規則 21/35 之防腐層規範 (Anti-Corruption Layer)。
 * 專職負責將應用層 {@link ExcelTaskGottenResult} 轉譯為表現層回應物件 {@link ExcelTaskRetrievedResource}，
 * 確保 Resource 物件維持純資料載體，不具備任何轉換或領域邏輯。
 */
@Component
public class ExcelTaskResourceAssembler {

	/**
	 * 將長任務結果 DTO 封裝為 HTTP 200 進度查詢回應載體
	 *
	 * @param result 應用層任務結果 DTO
	 * @return ExcelTaskRetrievedResource 回應物件
	 */
	public ExcelTaskRetrievedResource toRetrievedResource(ExcelTaskGottenResult result) {
		Objects.requireNonNull(result, "ExcelTaskGottenResult 不得為空");

		return new ExcelTaskRetrievedResource(
				result.taskId(),
				result.taskType(),
				result.taskStatus(),
				result.totalRows(),
				result.processedRows(),
				result.progressPercentage(),
				result.fileName(),
				result.fileUrl(),
				result.errorMessage(),
				result.createdAt(),
				result.updatedAt()
		);
	}
}

