package com.example.demo.infrastructure.persistence.material.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import com.example.demo.domain.material.aggregate.root.Material;
import com.example.demo.domain.material.aggregate.vo.MaterialCategory;
import com.example.demo.domain.material.aggregate.vo.MaterialCode;
import com.example.demo.domain.material.aggregate.vo.MaterialId;
import com.example.demo.domain.material.aggregate.vo.MaterialStatus;
import com.example.demo.domain.material.aggregate.vo.Money;
import com.example.demo.domain.material.aggregate.vo.UnitOfMeasure;

/**
 * 物料 JPA 持久化實體 (Material JPA Entity)
 * <p>
 * 隸屬於基礎設施層 (Infrastructure Layer)，映射關聯式資料庫之 {@code t_material} 資料表。
 * 負責將純 Java 領域聚合根 {@link Material} 與關聯式資料表欄位進行雙向轉換。
 */
@Entity
@Table(name = "t_material")
public class MaterialJpaEntity {

	@Id
	@Column(name = "id", length = 36, nullable = false)
	private String id;

	@Column(name = "material_code", length = 50, nullable = false, unique = true)
	private String materialCode;

	@Column(name = "material_name", length = 100, nullable = false)
	private String materialName;

	@Column(name = "specification", length = 200)
	private String specification;

	@Enumerated(EnumType.STRING)
	@Column(name = "category", length = 20, nullable = false)
	private MaterialCategory category;

	@Enumerated(EnumType.STRING)
	@Column(name = "unit", length = 20, nullable = false)
	private UnitOfMeasure unit;

	@Column(name = "standard_cost", precision = 15, scale = 4, nullable = false)
	private BigDecimal standardCost;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", length = 20, nullable = false)
	private MaterialStatus status;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	/**
	 * JPA 規範之無參建構子。
	 */
	public MaterialJpaEntity() {
	}

	/**
	 * 全參建構子。
	 */
	public MaterialJpaEntity(
			String id,
			String materialCode,
			String materialName,
			String specification,
			MaterialCategory category,
			UnitOfMeasure unit,
			BigDecimal standardCost,
			MaterialStatus status,
			LocalDateTime createdAt,
			LocalDateTime updatedAt
	) {
		this.id = id;
		this.materialCode = materialCode;
		this.materialName = materialName;
		this.specification = specification;
		this.category = category;
		this.unit = unit;
		this.standardCost = standardCost;
		this.status = status;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	@PrePersist
	protected void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		if (this.createdAt == null) {
			this.createdAt = now;
		}
		if (this.updatedAt == null) {
			this.updatedAt = now;
		}
	}

	@PreUpdate
	protected void onUpdate() {
		this.updatedAt = LocalDateTime.now();
	}

	/**
	 * 從領域聚合根轉換為 JPA 實體 (From Domain)。
	 *
	 * @param domain 物料聚合根
	 * @return 物料 JPA 實體
	 */
	public static MaterialJpaEntity fromDomain(Material domain) {
		Objects.requireNonNull(domain, "領域物料聚合根不得為空");
		MaterialJpaEntity entity = new MaterialJpaEntity();
		entity.setId(domain.getId().value());
		entity.setMaterialCode(domain.getCode().value());
		entity.setMaterialName(domain.getName());
		entity.setSpecification(domain.getSpecification());
		entity.setCategory(domain.getCategory());
		entity.setUnit(domain.getUnit());
		entity.setStandardCost(domain.getStandardCost().amount());
		entity.setStatus(domain.getStatus());
		return entity;
	}

	/**
	 * 轉換為純 Java 領域聚合根 (To Domain)。
	 *
	 * @return 重建之物料聚合根
	 */
	public Material toDomain() {
		return Material.reconstitute(
				MaterialId.of(this.id),
				MaterialCode.of(this.materialCode),
				this.materialName,
				this.specification,
				this.category,
				this.unit,
				Money.of(this.standardCost),
				this.status
		);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getMaterialCode() {
		return materialCode;
	}

	public void setMaterialCode(String materialCode) {
		this.materialCode = materialCode;
	}

	public String getMaterialName() {
		return materialName;
	}

	public void setMaterialName(String materialName) {
		this.materialName = materialName;
	}

	public String getSpecification() {
		return specification;
	}

	public void setSpecification(String specification) {
		this.specification = specification;
	}

	public MaterialCategory getCategory() {
		return category;
	}

	public void setCategory(MaterialCategory category) {
		this.category = category;
	}

	public UnitOfMeasure getUnit() {
		return unit;
	}

	public void setUnit(UnitOfMeasure unit) {
		this.unit = unit;
	}

	public BigDecimal getStandardCost() {
		return standardCost;
	}

	public void setStandardCost(BigDecimal standardCost) {
		this.standardCost = standardCost;
	}

	public MaterialStatus getStatus() {
		return status;
	}

	public void setStatus(MaterialStatus status) {
		this.status = status;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
}

