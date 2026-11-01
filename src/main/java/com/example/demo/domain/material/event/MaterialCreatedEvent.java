package com.example.demo.domain.material.event;

import java.time.Instant;
import com.example.demo.domain.material.aggregate.vo.MaterialCategory;
import com.example.demo.domain.material.aggregate.vo.MaterialCode;
import com.example.demo.domain.material.aggregate.vo.MaterialId;

/**
 * 物料建立領域事件 (Material Created Domain Event)
 * <p>
 * 遵循命名規範：{@code N + Ved + Event}。
 * 當新物料主檔成功建立時觸發並對外發布。
 */
public record MaterialCreatedEvent(
		MaterialId materialId,
		MaterialCode materialCode,
		String materialName,
		MaterialCategory category,
		Instant occurredOn
) {

	/**
	 * 建構物料建立領域事件。
	 *
	 * @param materialId 物料識別碼
	 * @param materialCode 物料料號
	 * @param materialName 物料名稱
	 * @param category 物料分類
	 */
	public MaterialCreatedEvent(
			MaterialId materialId,
			MaterialCode materialCode,
			String materialName,
			MaterialCategory category
	) {
		this(materialId, materialCode, materialName, category, Instant.now());
	}
}

