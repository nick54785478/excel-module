package com.example.demo.domain.material.event;

import java.time.Instant;
import com.example.demo.domain.material.aggregate.vo.MaterialId;
import com.example.demo.domain.material.aggregate.vo.Money;

/**
 * 物料成本變更領域事件 (Material Cost Changed Domain Event)
 * <p>
 * 遵循命名規範：{@code N + Ved + Event}。
 * 當物料之標準成本調整時觸發並對外發布。
 *
 * @param materialId 物料識別碼實質物件
 * @param previousCost 變更前標準成本
 * @param newCost 變更後標準成本
 * @param occurredOn 事件發生時間戳記
 */
public record MaterialCostChangedEvent(
		MaterialId materialId,
		Money previousCost,
		Money newCost,
		Instant occurredOn
) {

	/**
	 * 建構物料成本變更領域事件 (自動帶入當前時間戳記)。
	 *
	 * @param materialId 物料識別碼實質物件
	 * @param previousCost 變更前成本
	 * @param newCost 變更後成本
	 */
	public MaterialCostChangedEvent(
			MaterialId materialId,
			Money previousCost,
			Money newCost
	) {
		this(materialId, previousCost, newCost, Instant.now());
	}
}

