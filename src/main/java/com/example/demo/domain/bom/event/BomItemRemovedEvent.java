package com.example.demo.domain.bom.event;

import java.time.Instant;
import com.example.demo.domain.bom.aggregate.vo.BomId;
import com.example.demo.domain.bom.aggregate.vo.BomItemId;
import com.example.demo.domain.material.aggregate.vo.MaterialId;

/**
 * 物料清單子件移除領域事件 (BOM Item Removed Domain Event)
 * <p>
 * 遵循命名規範：{@code N + Ved + Event}。
 * 當子件自草稿狀態之 BOM 結構中移除時觸發並發布。
 */
public record BomItemRemovedEvent(
		BomId bomId,
		BomItemId bomItemId,
		MaterialId childMaterialId,
		Instant occurredOn
) {

	/**
	 * 建構子件移除領域事件。
	 *
	 * @param bomId BOM 識別碼
	 * @param bomItemId 子項識別碼
	 * @param childMaterialId 子件物料識別碼
	 */
	public BomItemRemovedEvent(BomId bomId, BomItemId bomItemId, MaterialId childMaterialId) {
		this(bomId, bomItemId, childMaterialId, Instant.now());
	}
}

