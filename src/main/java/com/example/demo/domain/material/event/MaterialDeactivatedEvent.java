package com.example.demo.domain.material.event;

import java.time.Instant;
import com.example.demo.domain.material.aggregate.vo.MaterialId;

/**
 * 物料停用領域事件 (Material Deactivated Domain Event)
 * <p>
 * 遵循命名規範：{@code N + Ved + Event}。
 * 當物料料號被停用（如舊品淘汰、不再採購）時觸發並對外發布。
 *
 * @param materialId 物料識別碼實質物件
 * @param occurredOn 事件發生時間戳記
 */
public record MaterialDeactivatedEvent(
		MaterialId materialId,
		Instant occurredOn
) {

	/**
	 * 建構物料停用領域事件 (自動帶入當前時間戳記)。
	 *
	 * @param materialId 物料識別碼實質物件
	 */
	public MaterialDeactivatedEvent(MaterialId materialId) {
		this(materialId, Instant.now());
	}
}

