package com.example.demo.application.shared.model;

import java.math.BigDecimal;
import java.util.Objects;

import com.example.demo.application.annotation.mapping.ExcelColumn;
import com.example.demo.application.annotation.mapping.ExcelSheet;

/**
 * BOM 物料清單聚合對應之 Excel 宣告式映射模型 (BOM Aggregate Excel Model)
 * <p>
 * 遵循維度一「宣告式物件映射」設計，專門對應領域層 {@link com.example.demo.domain.bom.aggregate.root.Bom} 聚合根
 * 與其內部實體 {@link com.example.demo.domain.bom.aggregate.entity.BomItem}。
 * 透過 {@link ExcelSheet} 與 {@link ExcelColumn} 定義單階 BOM 配方之 Excel 表頭與資料欄位映射。
 */
@ExcelSheet(
		name = "單階BOM清單",
		aliases = {"單階BOM匯入清單", "BOM清單", "BOM配方", "單階BOM", "BOM"},
		headerRowIndex = 0,
		dataRowStartIndex = 1
)
public class BomItemExcelModel {

	@ExcelColumn(name = "母件料號", index = 0, aliases = {"母件代碼", "父料號", "主件料號"})
	private String parentMaterialCode;

	@ExcelColumn(name = "BOM版本號", index = 1, defaultValue = "1.0", aliases = {"BOM版本", "版本號", "版本"})
	private String bomVersion;

	@ExcelColumn(name = "項次", index = 2, aliases = {"序號", "Item"})
	private Integer itemSequence;

	@ExcelColumn(name = "子件料號", index = 3, aliases = {"子料號", "元件料號", "料號"})
	private String childMaterialCode;

	@ExcelColumn(name = "子件名稱", index = 4, aliases = {"子件品名", "品名", "物料名稱"})
	private String childMaterialName;

	@ExcelColumn(name = "組裝用量", index = 5, format = "#,##0.0000", aliases = {"單機用量", "用量", "數量"})
	private BigDecimal quantity;

	@ExcelColumn(name = "單位", index = 6, defaultValue = "PCS", aliases = {"計量單位", "基本單位"})
	private String unit;

	@ExcelColumn(name = "損耗率 (%)", index = 7, format = "0.00", aliases = {"損耗率", "損耗比率", "損耗率(%)"})
	private BigDecimal scrapRate;

	@ExcelColumn(name = "安裝位置代碼", index = 8, defaultValue = "", aliases = {"安裝位號", "位置", "安裝位置", "RefDes"})
	private String positionCode;

	@ExcelColumn(name = "備註說明", index = 9, defaultValue = "", aliases = {"備註", "說明"})
	private String remark;

	public BomItemExcelModel() {
	}

	public BomItemExcelModel(
			String parentMaterialCode,
			String bomVersion,
			Integer itemSequence,
			String childMaterialCode,
			String childMaterialName,
			BigDecimal quantity,
			String unit,
			BigDecimal scrapRate,
			String positionCode,
			String remark
	) {
		this.parentMaterialCode = parentMaterialCode;
		this.bomVersion = bomVersion;
		this.itemSequence = itemSequence;
		this.childMaterialCode = childMaterialCode;
		this.childMaterialName = childMaterialName;
		this.quantity = quantity;
		this.unit = unit;
		this.scrapRate = scrapRate;
		this.positionCode = positionCode;
		this.remark = remark;
	}

	public String getParentMaterialCode() {
		return parentMaterialCode;
	}

	public void setParentMaterialCode(String parentMaterialCode) {
		this.parentMaterialCode = parentMaterialCode;
	}

	public String getBomVersion() {
		return bomVersion;
	}

	public void setBomVersion(String bomVersion) {
		this.bomVersion = bomVersion;
	}

	public Integer getItemSequence() {
		return itemSequence;
	}

	public void setItemSequence(Integer itemSequence) {
		this.itemSequence = itemSequence;
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

	public BigDecimal getQuantity() {
		return quantity;
	}

	public void setQuantity(BigDecimal quantity) {
		this.quantity = quantity;
	}

	public String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		BomItemExcelModel that = (BomItemExcelModel) o;
		return Objects.equals(parentMaterialCode, that.parentMaterialCode) &&
				Objects.equals(bomVersion, that.bomVersion) &&
				Objects.equals(itemSequence, that.itemSequence) &&
				Objects.equals(childMaterialCode, that.childMaterialCode) &&
				Objects.equals(childMaterialName, that.childMaterialName) &&
				Objects.equals(quantity, that.quantity) &&
				Objects.equals(unit, that.unit) &&
				Objects.equals(scrapRate, that.scrapRate) &&
				Objects.equals(positionCode, that.positionCode) &&
				Objects.equals(remark, that.remark);
	}

	@Override
	public int hashCode() {
		return Objects.hash(parentMaterialCode, bomVersion, itemSequence, childMaterialCode, childMaterialName, quantity, unit, scrapRate, positionCode, remark);
	}

	@Override
	public String toString() {
		return "BomItemExcelModel{" +
				"parentMaterialCode='" + parentMaterialCode + '\'' +
				", bomVersion='" + bomVersion + '\'' +
				", itemSequence=" + itemSequence +
				", childMaterialCode='" + childMaterialCode + '\'' +
				", childMaterialName='" + childMaterialName + '\'' +
				", quantity=" + quantity +
				", unit='" + unit + '\'' +
				", scrapRate=" + scrapRate +
				", positionCode='" + positionCode + '\'' +
				", remark='" + remark + '\'' +
				'}';
	}
}

