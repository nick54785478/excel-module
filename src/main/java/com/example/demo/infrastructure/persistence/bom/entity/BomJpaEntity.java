package com.example.demo.infrastructure.persistence.bom.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.example.demo.domain.bom.aggregate.entity.BomItem;
import com.example.demo.domain.bom.aggregate.root.Bom;
import com.example.demo.domain.bom.aggregate.vo.BomId;
import com.example.demo.domain.bom.aggregate.vo.BomStatus;
import com.example.demo.domain.bom.aggregate.vo.BomVersion;
import com.example.demo.domain.bom.aggregate.vo.Quantity;
import com.example.demo.domain.material.aggregate.vo.MaterialId;

/**
 * 物料清單 JPA 持久化實體 (BOM JPA Entity)
 * <p>
 * 隸屬於基礎設施層 (Infrastructure Layer)，映射資料表 {@code t_bom}。
 * 封裝一對多關聯之子項明細清單 {@link BomItemJpaEntity}，支援級聯操作與孤兒移除。
 */
@Entity
@Table(
		name = "t_bom",
		uniqueConstraints = @UniqueConstraint(name = "uk_bom_parent_version", columnNames = {"parent_material_id", "bom_version"})
)
public class BomJpaEntity {

	@Id
	@Column(name = "id", length = 36, nullable = false)
	private String id;

	@Column(name = "parent_material_id", length = 36, nullable = false)
	private String parentMaterialId;

	@Column(name = "bom_version", length = 20, nullable = false)
	private String bomVersion;

	@Column(name = "description", length = 200)
	private String description;

	@Column(name = "base_quantity", precision = 15, scale = 4, nullable = false)
	private BigDecimal baseQuantity;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", length = 20, nullable = false)
	private BomStatus status;

	@OneToMany(mappedBy = "bom", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	private List<BomItemJpaEntity> items = new ArrayList<>();

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	/**
	 * JPA 規範無參建構子。
	 */
	public BomJpaEntity() {
	}

	public BomJpaEntity(
			String id,
			String parentMaterialId,
			String bomVersion,
			String description,
			BigDecimal baseQuantity,
			BomStatus status,
			LocalDateTime createdAt,
			LocalDateTime updatedAt
	) {
		this.id = id;
		this.parentMaterialId = parentMaterialId;
		this.bomVersion = bomVersion;
		this.description = description;
		this.baseQuantity = baseQuantity;
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
	 * @param domain BOM 領域聚合根
	 * @return BOM JPA 實體
	 */
	public static BomJpaEntity fromDomain(Bom domain) {
		Objects.requireNonNull(domain, "領域 BOM 聚合根不得為空");
		BomJpaEntity entity = new BomJpaEntity();
		entity.setId(domain.getId().value());
		entity.setParentMaterialId(domain.getParentMaterialId().value());
		entity.setBomVersion(domain.getVersion().value());
		entity.setDescription(domain.getDescription());
		entity.setBaseQuantity(domain.getBaseQuantity().value());
		entity.setStatus(domain.getStatus());

		List<BomItemJpaEntity> itemEntities = new ArrayList<>();
		for (BomItem item : domain.getItems()) {
			itemEntities.add(BomItemJpaEntity.fromDomain(item, entity));
		}
		entity.setItems(itemEntities);
		return entity;
	}

	/**
	 * 轉換為純 Java 領域聚合根 (To Domain)。
	 *
	 * @return 重建之 BOM 領域聚合根
	 */
	public Bom toDomain() {
		List<BomItem> domainItems = new ArrayList<>();
		if (this.items != null) {
			for (BomItemJpaEntity itemEntity : this.items) {
				domainItems.add(itemEntity.toDomain());
			}
		}
		return Bom.reconstitute(
				BomId.of(this.id),
				MaterialId.of(this.parentMaterialId),
				BomVersion.of(this.bomVersion),
				this.description,
				Quantity.of(this.baseQuantity),
				this.status,
				domainItems
		);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getParentMaterialId() {
		return parentMaterialId;
	}

	public void setParentMaterialId(String parentMaterialId) {
		this.parentMaterialId = parentMaterialId;
	}

	public String getBomVersion() {
		return bomVersion;
	}

	public void setBomVersion(String bomVersion) {
		this.bomVersion = bomVersion;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public BigDecimal getBaseQuantity() {
		return baseQuantity;
	}

	public void setBaseQuantity(BigDecimal baseQuantity) {
		this.baseQuantity = baseQuantity;
	}

	public BomStatus getStatus() {
		return status;
	}

	public void setStatus(BomStatus status) {
		this.status = status;
	}

	public List<BomItemJpaEntity> getItems() {
		return items;
	}

	public void setItems(List<BomItemJpaEntity> items) {
		this.items = items;
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

