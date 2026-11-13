package com.example.demo.application.shared.model.bom;

import java.math.BigDecimal;
import java.util.Objects;

import com.example.demo.application.annotation.mapping.ExcelColumn;
import com.example.demo.application.annotation.mapping.ExcelSheet;

/**
 * 多階層 BOM 展開清單專屬之 Excel 宣告式映射模型 (Multi-Level BOM Aggregate Excel Model)
 * <p>
 * 遵循維度一「宣告式物件映射」設計，專門對應多階層樹狀 BOM (Tree BOM) 展開結構。
 * 包含完整階層等級 (Level)、階層序號 (WBS)、規格型號、有效用量、標準單價與成本小計等專屬欄位。
 * 保持 Domain 領域層為純 Java，將多階層試算表技術映射封裝於應用層展示模型中。
 * </p>
 */
@ExcelSheet(
		name = "多階層BOM展開清單",
		aliases = {"多階層BOM清單", "多階BOM展開清單", "多階BOM", "階層BOM", "MultiLevelBom"},
		headerRowIndex = 0,
		dataRowStartIndex = 1
)
public class MultiLevelBomExcelModel {

	@ExcelColumn(name = "階層", index = 0, aliases = {"Level", "階層等級"})
	private Integer level;

	@ExcelColumn(name = "階層序號", index = 1, aliases = {"序號", "階層代碼", "WBS", "LevelSeq"})
	private String levelSequence;

	@ExcelColumn(name = "母件料號", index = 2, defaultValue = "-", aliases = {"母件代碼", "父料號", "主件料號", "ParentCode"})
	private String parentMaterialCode;

	@ExcelColumn(name = "子件料號", index = 3, aliases = {"子料號", "元件料號", "料號", "ChildCode"})
	private String childMaterialCode;

	@ExcelColumn(name = "品名", index = 4, aliases = {"子件品名", "子件名稱", "物料名稱", "品名規格", "MaterialName"})
	private String childMaterialName;

	@ExcelColumn(name = "規格型號", index = 5, defaultValue = "-", aliases = {"規格", "型號", "Specification"})
	private String specification;

	@ExcelColumn(name = "類別", index = 6, defaultValue = "原物料", aliases = {"物料類別", "物料分類", "分類", "Category"})
	private String category;

	@ExcelColumn(name = "單位", index = 7, defaultValue = "PCS", aliases = {"計量單位", "基本單位", "Unit"})
	private String unit;

	@ExcelColumn(name = "單機用量", index = 8, format = "#,##0.0000", aliases = {"組裝用量", "用量", "數量", "Qty"})
	private BigDecimal quantity;

	@ExcelColumn(name = "損耗率", index = 9, format = "0.00", aliases = {"損耗率 (%)", "損耗比率", "損耗率(%)", "ScrapRate"})
	private BigDecimal scrapRate;

	@ExcelColumn(name = "有效用量", index = 10, format = "#,##0.0000", aliases = {"實際用量", "總用量", "EffectiveQty"})
	private BigDecimal effectiveQuantity;

	@ExcelColumn(name = "標準單價", index = 11, format = "#,##0.00", aliases = {"單價", "標準成本", "標準單價 (TWD)", "Price"})
	private BigDecimal standardPrice;

	@ExcelColumn(name = "小計總成本", index = 12, format = "#,##0.00", aliases = {"小計", "總成本", "成本小計", "Subtotal"})
	private BigDecimal subtotalCost;

	@ExcelColumn(name = "安裝位號", index = 13, defaultValue = "", aliases = {"安裝位置", "位號", "位置代碼", "RefDes"})
	private String positionCode;

	@ExcelColumn(name = "備註說明", index = 14, defaultValue = "", aliases = {"備註", "說明", "Remark"})
	private String remark;

	public MultiLevelBomExcelModel() {
	}

	public MultiLevelBomExcelModel(
			Integer level,
			String levelSequence,
			String parentMaterialCode,
			String childMaterialCode,
			String childMaterialName,
			String specification,
			String category,
			String unit,
			BigDecimal quantity,
			BigDecimal scrapRate,
			BigDecimal effectiveQuantity,
			BigDecimal standardPrice,
			BigDecimal subtotalCost,
			String positionCode,
			String remark
	) {
		this.level = level;
		this.levelSequence = levelSequence;
		this.parentMaterialCode = parentMaterialCode;
		this.childMaterialCode = childMaterialCode;
		this.childMaterialName = childMaterialName;
		this.specification = specification;
		this.category = category;
		this.unit = unit;
		this.quantity = quantity;
		this.scrapRate = scrapRate;
		this.effectiveQuantity = effectiveQuantity;
		this.standardPrice = standardPrice;
		this.subtotalCost = subtotalCost;
		this.positionCode = positionCode;
		this.remark = remark;
	}

	public Integer getLevel() {
		return level;
	}

	public void setLevel(Integer level) {
		this.level = level;
	}

	public String getLevelSequence() {
		return levelSequence;
	}

	public void setLevelSequence(String levelSequence) {
		this.levelSequence = levelSequence;
	}

	public String getParentMaterialCode() {
		return parentMaterialCode;
	}

	public void setParentMaterialCode(String parentMaterialCode) {
		this.parentMaterialCode = parentMaterialCode;
	}

	public String getChildMaterialCode() {
		return childMaterialCode;
	}

	public void setChildMaterialCode(String childMaterialCode) {
		this.childMaterialCode = childMaterialCode;
	}

	public String getChildMaterialName() {
		return childMaterialName;
	}

	public void setChildMaterialName(String childMaterialName) {
		this.childMaterialName = childMaterialName;
	}

	public String getSpecification() {
		return specification;
	}

	public void setSpecification(String specification) {
		this.specification = specification;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
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

	public BigDecimal getEffectiveQuantity() {
		return effectiveQuantity;
	}

	public void setEffectiveQuantity(BigDecimal effectiveQuantity) {
		this.effectiveQuantity = effectiveQuantity;
	}

	public BigDecimal getStandardPrice() {
		return standardPrice;
	}

	public void setStandardPrice(BigDecimal standardPrice) {
		this.standardPrice = standardPrice;
	}

	public BigDecimal getSubtotalCost() {
		return subtotalCost;
	}

	public void setSubtotalCost(BigDecimal subtotalCost) {
		this.subtotalCost = subtotalCost;
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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		MultiLevelBomExcelModel that = (MultiLevelBomExcelModel) o;
		return Objects.equals(level, that.level)
				&& Objects.equals(levelSequence, that.levelSequence)
				&& Objects.equals(parentMaterialCode, that.parentMaterialCode)
				&& Objects.equals(childMaterialCode, that.childMaterialCode);
	}

	@Override
	public int hashCode() {
		return Objects.hash(level, levelSequence, parentMaterialCode, childMaterialCode);
	}

	@Override
	public String toString() {
		return "MultiLevelBomExcelModel{" +
				"level=" + level +
				", levelSequence='" + levelSequence + '\'' +
				", parentMaterialCode='" + parentMaterialCode + '\'' +
				", childMaterialCode='" + childMaterialCode + '\'' +
				", childMaterialName='" + childMaterialName + '\'' +
				", specification='" + specification + '\'' +
				", category='" + category + '\'' +
				", unit='" + unit + '\'' +
				", quantity=" + quantity +
				", scrapRate=" + scrapRate +
				", effectiveQuantity=" + effectiveQuantity +
				", standardPrice=" + standardPrice +
				", subtotalCost=" + subtotalCost +
				", positionCode='" + positionCode + '\'' +
				", remark='" + remark + '\'' +
				'}';
	}
}

