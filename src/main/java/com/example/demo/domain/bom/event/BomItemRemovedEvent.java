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
 *
 * @param bomId BOM 識別碼實質物件
 * @param bomItemId 移除之子項識別碼實質物件
 * @param childMaterialId 移除之子件物料識別碼實質物件
 * @param occurredOn 事件發生時間戳記
 */
public record BomItemRemovedEvent(
		BomId bomId,
		BomItemId bomItemId,
		MaterialId childMaterialId,
		Instant occurredOn
) {

	/**
	 * 建構子件移除領域事件 (自動帶入當前時間戳記)。
	 *
	 * @param bomId BOM 識別碼實質物件
	 * @param bomItemId 子項識別碼實質物件
	 * @param childMaterialId 子件物料識別碼實質物件
	 */
	public BomItemRemovedEvent(BomId bomId, BomItemId bomItemId, MaterialId childMaterialId) {
		this(bomId, bomItemId, childMaterialId, Instant.now());
	}
}

