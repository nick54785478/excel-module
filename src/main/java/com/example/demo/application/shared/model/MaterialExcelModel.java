package com.example.demo.application.shared.model;

import java.math.BigDecimal;
import java.util.Objects;

import com.example.demo.application.annotation.mapping.ExcelColumn;
import com.example.demo.application.annotation.mapping.ExcelSheet;

/**
 * 物料聚合根對應之 Excel 宣告式映射模型 (Material Aggregate Excel Model)
 * <p>
 * 遵循維度一「宣告式物件映射」設計，專門對應領域層 {@link com.example.demo.domain.material.aggregate.root.Material} 聚合根。
 * 透過 {@link ExcelSheet} 與 {@link ExcelColumn} 定義 Excel 表頭、欄位順序與格式化規則。
 * 保持 Domain 領域層為純 Java，將技術性 Excel 映射封裝於應用層展示模型中。
 */
@ExcelSheet(name = "物料主檔清單", headerRowIndex = 0, dataRowStartIndex = 1)
public class MaterialExcelModel {

	@ExcelColumn(name = "物料編號", index = 0, aliases = {"物料料號", "料號", "品號", "MaterialCode"})
	private String materialCode;

	@ExcelColumn(name = "物料名稱", index = 1, aliases = {"物料品名", "品名", "MaterialName"})
	private String materialName;

	@ExcelColumn(name = "規格型號", index = 2, defaultValue = "-", aliases = {"規格", "型號", "Specification"})
	private String specification;

	@ExcelColumn(name = "物料類別", index = 3, aliases = {"物料分類", "分類", "類別", "Category"})
	private String category;

	@ExcelColumn(name = "計量單位", index = 4, defaultValue = "PCS", aliases = {"基本單位", "單位", "Unit"})
	private String unit;

	@ExcelColumn(name = "標準成本 (TWD)", index = 5, format = "#,##0.00", aliases = {"標準單價 (TWD)", "標準成本", "標準單價", "單價", "成本"})
	private BigDecimal standardCost;

	@ExcelColumn(name = "物料狀態", index = 6, defaultValue = "ACTIVE", aliases = {"生命週期狀態", "狀態", "Status"})
	private String status;

	public MaterialExcelModel() {
	}

	public MaterialExcelModel(
			String materialCode,
			String materialName,
			String specification,
			String category,
			String unit,
			BigDecimal standardCost,
			String status
	) {
		this.materialCode = materialCode;
		this.materialName = materialName;
		this.specification = specification;
		this.category = category;
		this.unit = unit;
		this.standardCost = standardCost;
		this.status = status;
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

	public BigDecimal getStandardCost() {
		return standardCost;
	}

	public void setStandardCost(BigDecimal standardCost) {
		this.standardCost = standardCost;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		MaterialExcelModel that = (MaterialExcelModel) o;
		return Objects.equals(materialCode, that.materialCode) &&
				Objects.equals(materialName, that.materialName) &&
				Objects.equals(specification, that.specification) &&
				Objects.equals(category, that.category) &&
				Objects.equals(unit, that.unit) &&
				Objects.equals(standardCost, that.standardCost) &&
				Objects.equals(status, that.status);
	}

	@Override
	public int hashCode() {
		return Objects.hash(materialCode, materialName, specification, category, unit, standardCost, status);
	}

	@Override
	public String toString() {
		return "MaterialExcelModel{" +
				"materialCode='" + materialCode + '\'' +
				", materialName='" + materialName + '\'' +
				", specification='" + specification + '\'' +
				", category='" + category + '\'' +
				", unit='" + unit + '\'' +
				", standardCost=" + standardCost +
				", status='" + status + '\'' +
				'}';
	}
}

