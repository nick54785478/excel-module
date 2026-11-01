package com.example.demo.infrastructure.persistence.bom.entity;

import java.math.BigDecimal;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.example.demo.domain.bom.aggregate.entity.BomItem;
import com.example.demo.domain.bom.aggregate.vo.BomItemId;
import com.example.demo.domain.bom.aggregate.vo.ItemSequence;
import com.example.demo.domain.bom.aggregate.vo.Quantity;
import com.example.demo.domain.bom.aggregate.vo.ScrapRate;
import com.example.demo.domain.material.aggregate.vo.MaterialId;

/**
 * 物料清單明細子項 JPA 持久化實體 (BOM Item JPA Entity)
 * <p>
 * 隸屬於基礎設施層 (Infrastructure Layer)，映射資料表 {@code t_bom_item}。
 * 作為多對一關聯歸屬於 {@link BomJpaEntity}。
 */
@Entity
@Table(name = "t_bom_item")
public class BomItemJpaEntity {

	@Id
	@Column(name = "id", length = 36, nullable = false)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "bom_id", nullable = false)
	private BomJpaEntity bom;

	@Column(name = "child_material_id", length = 36, nullable = false)
	private String childMaterialId;

	@Column(name = "item_sequence", nullable = false)
	private Integer itemSequence;

	@Column(name = "quantity", precision = 15, scale = 4, nullable = false)
	private BigDecimal quantity;

	@Column(name = "scrap_rate", precision = 5, scale = 4, nullable = false)
	private BigDecimal scrapRate;

	@Column(name = "position_code", length = 100)
	private String positionCode;

	@Column(name = "remark", length = 255)
	private String remark;

	/**
	 * JPA 規範無參建構子。
	 */
	public BomItemJpaEntity() {
	}

	public BomItemJpaEntity(
			String id,
			BomJpaEntity bom,
			String childMaterialId,
			Integer itemSequence,
			BigDecimal quantity,
			BigDecimal scrapRate,
			String positionCode,
			String remark
	) {
		this.id = id;
		this.bom = bom;
		this.childMaterialId = childMaterialId;
		this.itemSequence = itemSequence;
		this.quantity = quantity;
		this.scrapRate = scrapRate;
		this.positionCode = positionCode;
		this.remark = remark;
	}

	/**
	 * 從領域實體轉換為 JPA 實體 (From Domain)。
	 *
	 * @param domain 子項領域實體
	 * @param parentBom 所屬父 BOM JPA 實體
	 * @return 子項 JPA 實體
	 */
	public static BomItemJpaEntity fromDomain(BomItem domain, BomJpaEntity parentBom) {
		Objects.requireNonNull(domain, "領域子項實體不得為空");
		BomItemJpaEntity entity = new BomItemJpaEntity();
		entity.setId(domain.getId().value());
		entity.setBom(parentBom);
		entity.setChildMaterialId(domain.getChildMaterialId().value());
		entity.setItemSequence(domain.getItemSequence().value());
		entity.setQuantity(domain.getQuantity().value());
		entity.setScrapRate(domain.getScrapRate().rate());
		entity.setPositionCode(domain.getPositionCode());
		entity.setRemark(domain.getRemark());
		return entity;
	}

	/**
	 * 轉換為純 Java 領域實體 (To Domain)。
	 *
	 * @return 子項領域實體
	 */
	public BomItem toDomain() {
		return new BomItem(
				BomItemId.of(this.id),
				MaterialId.of(this.childMaterialId),
				ItemSequence.of(this.itemSequence),
				Quantity.of(this.quantity),
				ScrapRate.of(this.scrapRate),
				this.positionCode,
				this.remark
		);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public BomJpaEntity getBom() {
		return bom;
	}

	public void setBom(BomJpaEntity bom) {
		this.bom = bom;
	}

	public String getChildMaterialId() {
		return childMaterialId;
	}

	public void setChildMaterialId(String childMaterialId) {
		this.childMaterialId = childMaterialId;
	}

	public Integer getItemSequence() {
		return itemSequence;
	}

	public void setItemSequence(Integer itemSequence) {
		this.itemSequence = itemSequence;
	}

	public BigDecimal getQuantity() {
		return quantity;
	}

	public void setQuantity(BigDecimal quantity) {
		this.quantity = quantity;
	}

	public BigDecimal getScrapRate() {
		return scrapRate;
	}

	public void setScrapRate(BigDecimal scrapRate) {
		this.scrapRate = scrapRate;
	}

	public String getPositionCode() {
		return positionCode;
	}

	public void setPositionCode(String positionCode) {
		this.positionCode = positionCode;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}
}

