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
 */
public record BomApprovedEvent(
		BomId bomId,
		MaterialId parentMaterialId,
		BomVersion version,
		Instant occurredOn
) {

	/**
	 * 建構 BOM 審核生效領域事件。
	 *
	 * @param bomId BOM 識別碼
	 * @param parentMaterialId 母件物料識別碼
	 * @param version 版本號
	 */
	public BomApprovedEvent(BomId bomId, MaterialId parentMaterialId, BomVersion version) {
		this(bomId, parentMaterialId, version, Instant.now());
	}
}

