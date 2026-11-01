package com.example.demo.domain.material.event;

import java.time.Instant;
import com.example.demo.domain.material.aggregate.vo.MaterialId;

/**
 * 物料資訊更新領域事件 (Material Updated Domain Event)
 * <p>
 * 遵循命名規範：{@code N + Ved + Event}。
 * 當物料名稱或規格描述進行異動時觸發並對外發布。
 */
public record MaterialUpdatedEvent(
		MaterialId materialId,
		String materialName,
		String specification,
		Instant occurredOn
) {

	/**
	 * 建構物料資訊更新領域事件。
	 *
	 * @param materialId 物料識別碼
	 * @param materialName 物料名稱
	 * @param specification 規格型號
	 */
	public MaterialUpdatedEvent(
			MaterialId materialId,
			String materialName,
			String specification
	) {
		this(materialId, materialName, specification, Instant.now());
	}
}

