package com.example.demo.domain.material.aggregate.root;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import com.example.demo.domain.material.aggregate.vo.MaterialCategory;
import com.example.demo.domain.material.aggregate.vo.MaterialCode;
import com.example.demo.domain.material.aggregate.vo.MaterialId;
import com.example.demo.domain.material.aggregate.vo.MaterialStatus;
import com.example.demo.domain.material.aggregate.vo.Money;
import com.example.demo.domain.material.aggregate.vo.UnitOfMeasure;
import com.example.demo.domain.material.event.MaterialCostChangedEvent;
import com.example.demo.domain.material.event.MaterialCreatedEvent;
import com.example.demo.domain.material.event.MaterialDeactivatedEvent;
import com.example.demo.domain.material.event.MaterialUpdatedEvent;

/**
 * 物料聚合根 (Material Aggregate Root)
 * <p>
 * 遵循 DDD (領域驅動設計) 規範，代表製造與供應鏈系統中的物料主檔核心實體。
 * <p>
 * 封裝物料的唯一識別碼、編碼、規格、分類、計量單位、成本與狀態，
 * 嚴禁使用 Lombok 註解，完全透過具備商業語意的領域方法進行狀態變更，並於狀態異動時發布領域事件。
 */
public class Material {

	private final MaterialId id;
	private final MaterialCode code;
	private String name;
	private String specification;
	private final MaterialCategory category;
	private final UnitOfMeasure unit;
	private Money standardCost;
	private MaterialStatus status;

	/**
	 * 暫存待發布之領域事件清單
	 */
	private final List<Object> domainEvents = new ArrayList<>();

	/**
	 * 私有建構子，確保僅能透過工廠方法或重建方法實例化物料。
	 */
	private Material(
			MaterialId id,
			MaterialCode code,
			String name,
			String specification,
			MaterialCategory category,
			UnitOfMeasure unit,
			Money standardCost,
			MaterialStatus status
	) {
		this.id = Objects.requireNonNull(id, "物料識別碼不得為空");
		this.code = Objects.requireNonNull(code, "物料料號不得為空");
		this.setName(name);
		this.specification = specification != null ? specification.trim() : "";
		this.category = Objects.requireNonNull(category, "物料分類不得為空");
		this.unit = Objects.requireNonNull(unit, "計量單位不得為空");
		this.standardCost = Objects.requireNonNull(standardCost, "標準成本不得為空");
		this.status = Objects.requireNonNull(status, "物料狀態不得為空");
	}

	/**
	 * 建立新物料主檔之靜態工廠方法。
	 * <p>
	 * 實例化新物料並記錄 {@link MaterialCreatedEvent} 領域事件。
	 *
	 * @param code 物料料號實質物件
	 * @param name 物料品名
	 * @param specification 規格型號說明
	 * @param category 物料分類 (成品/半成品/原料)
	 * @param unit 計量單位
	 * @param standardCost 標準單價成本
	 * @return 初始建立之物料聚合根
	 */
	public static Material create(
			MaterialCode code,
			String name,
			String specification,
			MaterialCategory category,
			UnitOfMeasure unit,
			Money standardCost
	) {
		MaterialId id = MaterialId.generate();
		Material material = new Material(
				id,
				code,
				name,
				specification,
				category,
				unit,
				standardCost != null ? standardCost : Money.zero(),
				MaterialStatus.ACTIVE
		);
		material.recordEvent(new MaterialCreatedEvent(id, code, material.getName(), category));
		return material;
	}

	/**
	 * 從持久化設施重建既有物料聚合根 (Reconstitution Factory)。
	 * <p>
	 * 用於 Repository 從資料庫載入資料時還原實體狀態，不觸發 {@link MaterialCreatedEvent}。
	 *
	 * @param id 物料識別碼
	 * @param code 物料料號
	 * @param name 物料品名
	 * @param specification 規格型號
	 * @param category 物料分類
	 * @param unit 計量單位
	 * @param standardCost 標準成本
	 * @param status 物料狀態
	 * @return 重建之物料聚合根
	 */
	public static Material reconstitute(
			MaterialId id,
			MaterialCode code,
			String name,
			String specification,
			MaterialCategory category,
			UnitOfMeasure unit,
			Money standardCost,
			MaterialStatus status
	) {
		return new Material(id, code, name, specification, category, unit, standardCost, status);
	}

	/**
	 * 更新物料基本資訊 (品名與規格型號)。
	 *
	 * @param newName 新品名
	 * @param newSpecification 新規格型號
	 */
	public void updateBasicInfo(String newName, String newSpecification) {
		this.setName(newName);
		this.specification = newSpecification != null ? newSpecification.trim() : "";
		recordEvent(new MaterialUpdatedEvent(this.id, this.name, this.specification));
	}

	/**
	 * 調整物料標準成本。
	 *
	 * @param newCost 新標準成本
	 */
	public void updateCost(Money newCost) {
		Objects.requireNonNull(newCost, "更新成本不得為空");
		if (this.standardCost.equals(newCost)) {
			return;
		}
		Money previousCost = this.standardCost;
		this.standardCost = newCost;
		recordEvent(new MaterialCostChangedEvent(this.id, previousCost, newCost));
	}

	/**
	 * 停用物料。
	 */
	public void discontinue() {
		if (this.status == MaterialStatus.DISCONTINUED) {
			return;
		}
		this.status = MaterialStatus.DISCONTINUED;
		recordEvent(new MaterialDeactivatedEvent(this.id));
	}

	/**
	 * 重新啟用物料。
	 */
	public void activate() {
		this.status = MaterialStatus.ACTIVE;
	}

	/**
	 * 判斷此物料是否能作為 BOM 母件。
	 *
	 * @return 若此物料分類允許擁有 BOM 配方則為 {@code true}
	 */
	public boolean canHaveBom() {
		return this.category.canHaveBom();
	}

	/**
	 * 記錄領域事件。
	 *
	 * @param event 領域事件物件
	 */
	protected void recordEvent(Object event) {
		if (event != null) {
			this.domainEvents.add(event);
		}
	}

	/**
	 * 取得所有未發布之領域事件（唯讀檢視）。
	 *
	 * @return 領域事件不可變清單
	 */
	public List<Object> getDomainEvents() {
		return Collections.unmodifiableList(this.domainEvents);
	}

	/**
	 * 清除已收集之領域事件。
	 */
	public void clearDomainEvents() {
		this.domainEvents.clear();
	}

	private void setName(String name) {
		if (name == null || name.trim().isEmpty()) {
			throw new IllegalArgumentException("物料品名不得為空");
		}
		this.name = name.trim();
	}

	public MaterialId getId() {
		return id;
	}

	public MaterialCode getCode() {
		return code;
	}

	public String getName() {
		return name;
	}

	public String getSpecification() {
		return specification;
	}

	public MaterialCategory getCategory() {
		return category;
	}

	public UnitOfMeasure getUnit() {
		return unit;
	}

	public Money getStandardCost() {
		return standardCost;
	}

	public MaterialStatus getStatus() {
		return status;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Material material = (Material) o;
		return Objects.equals(id, material.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public String toString() {
		return "Material{" +
				"id=" + id +
				", code=" + code +
				", name='" + name + '\'' +
				", category=" + category +
				", unit=" + unit +
				", standardCost=" + standardCost +
				", status=" + status +
				'}';
	}
}

