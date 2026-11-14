package com.example.demo.domain.bom.event;

import java.time.Instant;
import com.example.demo.domain.bom.aggregate.vo.BomId;
import com.example.demo.domain.bom.aggregate.vo.BomVersion;
import com.example.demo.domain.material.aggregate.vo.MaterialId;

/**
 * 物料清單審核生效領域事件 (BOM Approved Domain Event)
 * <p>
 * 遵循命名規範：{@code N + Ved + Event}。
 * 當 BOM 完成工程審核並正式發行生效時觸發並發布。
 *
 * @param bomId BOM 識別碼實質物件
 * @param parentMaterialId 母件物料識別碼實質物件
 * @param version BOM 版本號實質物件
 * @param occurredOn 事件發生時間戳記
 */
public record BomApprovedEvent(
		BomId bomId,
		MaterialId parentMaterialId,
		BomVersion version,
		Instant occurredOn
) {

	/**
	 * 建構 BOM 審核生效領域事件 (自動帶入當前時間戳記)。
	 *
	 * @param bomId BOM 識別碼實質物件
	 * @param parentMaterialId 母件物料識別碼實質物件
	 * @param version 版本號實質物件
	 */
	public BomApprovedEvent(BomId bomId, MaterialId parentMaterialId, BomVersion version) {
		this(bomId, parentMaterialId, version, Instant.now());
	}
}

