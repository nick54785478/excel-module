package com.example.demo.presentation.assembler;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import com.example.demo.application.shared.dto.ExcelValidationGottenResult;
import com.example.demo.application.shared.model.ExcelRowError;
import com.example.demo.presentation.resource.out.ExcelRowErrorResource;
import com.example.demo.presentation.resource.out.ExcelValidatedResource;

/**
 * Excel 檢核表現層資源組裝器 (Excel Validation Resource Assembler - ACL)
 * <p>
 * <b>【架構定位與職責規範】</b><br>
 * 位於 Clean Architecture 表現層組裝器套件 ({@code presentation/assembler/})，
 * 遵循 {@code AGENTS.md} 規則 33~35。<br>
 * 作為表現層與應用層之間的防腐層 (Anti-Corruption Layer, ACL)，
 * 負責將應用層資料載體 {@link ExcelValidationGottenResult} 與錯誤事實 {@link ExcelRowError}
 * 轉換為對外 REST API 回應資源 {@link ExcelValidatedResource} 與 {@link ExcelRowErrorResource}，
 * 確保 Resource 保持純資料載體屬性，不侵入業務或映射邏輯。
 * </p>
 */
@Component
public class ExcelValidationResourceAssembler {

	/**
	 * 將應用層 DTO 轉換為表現層回應資源
	 *
	 * @param result 應用層檢核結果 DTO (不得為 null)
	 * @param <T>    模型型別
	 * @return 表現層回應資源物件
	 */
	public <T> ExcelValidatedResource toResource(ExcelValidationGottenResult<T> result) {
		if (result == null) {
			return new ExcelValidatedResource(false, 0, 0, 0, Collections.emptyList(), Collections.emptyList());
		}

		List<ExcelRowErrorResource> errorResources = result.errors() != null
				? result.errors().stream().map(this::toErrorResource).toList()
				: Collections.emptyList();

		return new ExcelValidatedResource(
				result.valid(),
				result.totalRows(),
				result.validCount(),
				result.errorCount(),
				errorResources,
				result.validItems()
		);
	}

	/**
	 * 將應用層錯誤模型轉換為表現層錯誤資源
	 *
	 * @param error 應用層錯誤模型
	 * @return 表現層錯誤資源
	 */
	public ExcelRowErrorResource toErrorResource(ExcelRowError error) {
		if (error == null) {
			return null;
		}
		return new ExcelRowErrorResource(
				error.rowIndex(),
				error.columnIndex(),
				error.columnName(),
				error.invalidValue(),
				error.errorMessage(),
				error.ruleCode()
		);
	}
}

