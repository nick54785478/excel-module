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

	/**
	 * BOM 子項唯一識別碼 (Entity Identifier)
	 */
	private final BomItemId id;

	/**
	 * 耗用之子件物料識別碼
	 */
	private final MaterialId childMaterialId;

	/**
	 * 子件於清單中的排列項次序號 (例如 10, 20, 30)
	 */
	private ItemSequence itemSequence;

	/**
	 * 單機基準耗用量
	 */
	private Quantity quantity;

	/**
	 * 製程耗損率 (0.0000 ~ 1.0000)
	 */
	private ScrapRate scrapRate;

	/**
	 * 安裝位置代碼 (如 PCB 位置 R101, C204)
	 */
	private String positionCode;

	/**
	 * 備註說明 (如替代料料號、特殊製程要求等)
	 */
	private String remark;

	/**
	 * 建構 BOM 子項實體。
	 *
	 * @param id 子項識別碼實質物件，不可為 null
	 * @param childMaterialId 子件物料識別碼實質物件，不可為 null
	 * @param itemSequence 項次序號實質物件，不可為 null
	 * @param quantity 單機用量實質物件，不可為 null
	 * @param scrapRate 損耗率實質物件，若為 null 預設為 {@link ScrapRate#ZERO}
	 * @param positionCode 安裝位置代碼，若為 null 預設為空字串
	 * @param remark 備註說明，若為 null 預設為空字串
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
	 * <p>
	 * 公式：{@code 有效用量 = 單機用量 * (1 + 損耗率)}
	 *
	 * @return 計算後之實際有效需求用量實質物件
	 */
	public Quantity calculateEffectiveQuantity() {
		return this.scrapRate.calculateEffectiveQuantity(this.quantity);
	}

	/**
	 * 更新單機用量與損耗率。
	 *
	 * @param newQuantity 新單機用量實質物件，不可為 null
	 * @param newScrapRate 新損耗率實質物件，若為 null 則重設為 {@link ScrapRate#ZERO}
	 */
	public void updateQuantity(Quantity newQuantity, ScrapRate newScrapRate) {
		this.quantity = Objects.requireNonNull(newQuantity, "單機用量不得為空");
		this.scrapRate = newScrapRate != null ? newScrapRate : ScrapRate.ZERO;
	}

	/**
	 * 更新項次序號。
	 *
	 * @param newSequence 新項次序號實質物件，不可為 null
	 */
	public void updateSequence(ItemSequence newSequence) {
		this.itemSequence = Objects.requireNonNull(newSequence, "項次序號不得為空");
	}

	/**
	 * 更新安裝位號與備註說明。
	 *
	 * @param newPositionCode 新安裝位號字串，若為 null 則為空字串
	 * @param newRemark 新備註說明字串，若為 null 則為空字串
	 */
	public void updatePositionAndRemark(String newPositionCode, String newRemark) {
		this.positionCode = newPositionCode != null ? newPositionCode.trim() : "";
		this.remark = newRemark != null ? newRemark.trim() : "";
	}

	/**
	 * 取得 BOM 子項唯一識別碼。
	 *
	 * @return 子項識別碼實質物件
	 */
	public BomItemId getId() {
		return id;
	}

	/**
	 * 取得耗用之子件物料識別碼。
	 *
	 * @return 子件物料識別碼實質物件
	 */
	public MaterialId getChildMaterialId() {
		return childMaterialId;
	}

	/**
	 * 取得子件項次序號。
	 *
	 * @return 項次序號實質物件
	 */
	public ItemSequence getItemSequence() {
		return itemSequence;
	}

	/**
	 * 取得單機用量。
	 *
	 * @return 用量實質物件
	 */
	public Quantity getQuantity() {
		return quantity;
	}

	/**
	 * 取得損耗率。
	 *
	 * @return 損耗率實質物件
	 */
	public ScrapRate getScrapRate() {
		return scrapRate;
	}

	/**
	 * 取得安裝位置代碼。
	 *
	 * @return 位置代碼字串
	 */
	public String getPositionCode() {
		return positionCode;
	}

	/**
	 * 取得備註說明。
	 *
	 * @return 備註字串
	 */
	public String getRemark() {
		return remark;
	}

	/**
	 * 判斷兩 BOM 子項實體是否相等（以實體唯一識別碼 {@link BomItemId} 為準）。
	 *
	 * @param o 欲比較的物件
	 * @return 若識別碼相同回傳 {@code true}，否則回傳 {@code false}
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		BomItem bomItem = (BomItem) o;
		return Objects.equals(id, bomItem.id);
	}

	/**
	 * 依據子項識別碼 {@link BomItemId} 計算雜湊值。
	 *
	 * @return 雜湊碼
	 */
	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	/**
	 * 產生 BOM 子項文字表達形式（便於除錯與排查）。
	 *
	 * @return 包含關鍵屬性之字串
	 */
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

