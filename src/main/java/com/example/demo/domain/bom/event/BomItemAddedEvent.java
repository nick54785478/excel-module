package com.example.demo.domain.bom.event;

import java.time.Instant;
import com.example.demo.domain.bom.aggregate.vo.BomId;
import com.example.demo.domain.bom.aggregate.vo.BomItemId;
import com.example.demo.domain.bom.aggregate.vo.Quantity;
import com.example.demo.domain.material.aggregate.vo.MaterialId;

/**
 * 物料清單子件新增領域事件 (BOM Item Added Domain Event)
 * <p>
 * 遵循命名規範：{@code N + Ved + Event}。
 * 當成功將子件加入 BOM 結構時觸發並發布。
 *
 * @param bomId BOM 識別碼實質物件
 * @param bomItemId 子項識別碼實質物件
 * @param childMaterialId 子件物料識別碼實質物件
 * @param quantity 單機用量實質物件
 * @param occurredOn 事件發生時間戳記
 */
public record BomItemAddedEvent(
		BomId bomId,
		BomItemId bomItemId,
		MaterialId childMaterialId,
		Quantity quantity,
		Instant occurredOn
) {

	/**
	 * 建構子件新增領域事件 (自動帶入當前時間戳記)。
	 *
	 * @param bomId BOM 識別碼實質物件
	 * @param bomItemId 子項識別碼實質物件
	 * @param childMaterialId 子件物料識別碼實質物件
	 * @param quantity 單機用量實質物件
	 */
	public BomItemAddedEvent(
			BomId bomId,
			BomItemId bomItemId,
			MaterialId childMaterialId,
			Quantity quantity
	) {
		this(bomId, bomItemId, childMaterialId, quantity, Instant.now());
	}
}

