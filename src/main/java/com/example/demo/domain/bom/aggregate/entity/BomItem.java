package com.example.demo.domain.bom.aggregate.entity;

import java.util.Objects;
import com.example.demo.domain.bom.aggregate.vo.BomItemId;
import com.example.demo.domain.bom.aggregate.vo.ItemSequence;
import com.example.demo.domain.bom.aggregate.vo.Quantity;
import com.example.demo.domain.bom.aggregate.vo.ScrapRate;
import com.example.demo.domain.material.aggregate.vo.MaterialId;

/**
 * 物料清單子項實體 (BOM Item Entity)
 * <p>
 * 隸屬於 {@link com.example.demo.domain.bom.aggregate.root.Bom} 聚合根內部的領域實體。
 * <p>
 * 代表單一子料件在該 BOM 配方中的組裝關係，包含子料件識別碼、耗用數量、損耗率、安裝位號等資訊。
 */
public class BomItem {

	private final BomItemId id;
	private final MaterialId childMaterialId;
	private ItemSequence itemSequence;
	private Quantity quantity;
	private ScrapRate scrapRate;
	private String positionCode;
	private String remark;

	/**
	 * 建構 BOM 子項實體。
	 *
	 * @param id 子項識別碼
	 * @param childMaterialId 子件物料識別碼
	 * @param itemSequence 項次序號
	 * @param quantity 單機用量
	 * @param scrapRate 損耗率
	 * @param positionCode 安裝位置代碼
	 * @param remark 備註說明
	 */
	public BomItem(
			BomItemId id,
			MaterialId childMaterialId,
			ItemSequence itemSequence,
			Quantity quantity,
			ScrapRate scrapRate,
			String positionCode,
			String remark
	) {
		this.id = Objects.requireNonNull(id, "BOM 子項識別碼不得為空");
		this.childMaterialId = Objects.requireNonNull(childMaterialId, "子件物料識別碼不得為空");
		this.itemSequence = Objects.requireNonNull(itemSequence, "項次序號不得為空");
		this.quantity = Objects.requireNonNull(quantity, "單機用量不得為空");
		this.scrapRate = scrapRate != null ? scrapRate : ScrapRate.ZERO;
		this.positionCode = positionCode != null ? positionCode.trim() : "";
		this.remark = remark != null ? remark.trim() : "";
	}

	/**
	 * 計算考量損耗率後之實際有效需求用量。
	 *
	 * @return 有效用量
	 */
	public Quantity calculateEffectiveQuantity() {
		return this.scrapRate.calculateEffectiveQuantity(this.quantity);
	}

	/**
	 * 更新單機用量與損耗率。
	 *
	 * @param newQuantity 新單機用量
	 * @param newScrapRate 新損耗率
	 */
	public void updateQuantity(Quantity newQuantity, ScrapRate newScrapRate) {
		this.quantity = Objects.requireNonNull(newQuantity, "單機用量不得為空");
		this.scrapRate = newScrapRate != null ? newScrapRate : ScrapRate.ZERO;
	}

	/**
	 * 更新項次序號。
	 *
	 * @param newSequence 新序號
	 */
	public void updateSequence(ItemSequence newSequence) {
		this.itemSequence = Objects.requireNonNull(newSequence, "項次序號不得為空");
	}

	/**
	 * 更新安裝位號與備註說明。
	 *
	 * @param newPositionCode 新安裝位號
	 * @param newRemark 新備註
	 */
	public void updatePositionAndRemark(String newPositionCode, String newRemark) {
		this.positionCode = newPositionCode != null ? newPositionCode.trim() : "";
		this.remark = newRemark != null ? newRemark.trim() : "";
	}

	public BomItemId getId() {
		return id;
	}

	public MaterialId getChildMaterialId() {
		return childMaterialId;
	}

	public ItemSequence getItemSequence() {
		return itemSequence;
	}

	public Quantity getQuantity() {
		return quantity;
	}

	public ScrapRate getScrapRate() {
		return scrapRate;
	}

	public String getPositionCode() {
		return positionCode;
	}

	public String getRemark() {
		return remark;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		BomItem bomItem = (BomItem) o;
		return Objects.equals(id, bomItem.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public String toString() {
		return "BomItem{" +
				"id=" + id +
				", childMaterialId=" + childMaterialId +
				", itemSequence=" + itemSequence +
				", quantity=" + quantity +
				", scrapRate=" + scrapRate +
				", positionCode='" + positionCode + '\'' +
				'}';
	}
}

