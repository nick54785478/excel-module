package com.example.demo.presentation.assembler;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.demo.application.shared.model.BomItemExcelModel;
import com.example.demo.application.shared.model.MaterialExcelModel;
import com.example.demo.domain.bom.aggregate.entity.BomItem;
import com.example.demo.domain.bom.aggregate.root.Bom;
import com.example.demo.domain.material.aggregate.root.Material;
import com.example.demo.domain.material.aggregate.vo.MaterialId;
import com.example.demo.presentation.resource.in.ExportBomItemResource;
import com.example.demo.presentation.resource.in.ExportBomItemResource.BomItemDetailResource;
import com.example.demo.presentation.resource.in.ExportMaterialResource;
import com.example.demo.presentation.resource.in.ExportMaterialResource.MaterialItemResource;
import com.example.demo.presentation.resource.out.BomItemImportedResource;
import com.example.demo.presentation.resource.out.BomItemImportedResource.BomItemRetrievedResource;
import com.example.demo.presentation.resource.out.MaterialImportedResource;
import com.example.demo.presentation.resource.out.MaterialImportedResource.MaterialRetrievedResource;

/**
 * 物料與 BOM 表現層資源組裝器 (Material & BOM Presentation Resource Assembler)
 * <p>
 * 遵循 Clean Architecture 六角形架構與 AGENTS.md 規則 35，擔任表現層防腐層 (ACL) 角色。
 * 負責將 Request Resource 與應用層模型（{@link MaterialExcelModel}、{@link BomItemExcelModel}）互相轉換，
 * 以及提供領域聚合根（{@link Material}、{@link Bom}）至 Excel 模型的對映轉換，確保表現層與應用層職責清晰隔離。
 */
@Component
public class MaterialBomResourceAssembler {

	// =========================================================================
	// 1. Material 物料主檔轉換
	// =========================================================================

	/**
	 * 將物料主檔匯出請求資源轉換為 {@link MaterialExcelModel} 清單
	 *
	 * @param resource 物料匯出請求資源
	 * @return 物料 Excel 映射模型清單
	 */
	public List<MaterialExcelModel> toMaterialExcelModels(ExportMaterialResource resource) {
		if (resource == null || resource.materials() == null || resource.materials().isEmpty()) {
			return Collections.emptyList();
		}

		List<MaterialExcelModel> models = new ArrayList<>(resource.materials().size());
		for (MaterialItemResource item : resource.materials()) {
			if (item != null) {
				models.add(new MaterialExcelModel(
						item.materialCode(),
						item.materialName(),
						item.specification(),
						item.category(),
						item.unit(),
						item.standardCost(),
						item.status()
				));
			}
		}
		return models;
	}

	/**
	 * 將已解析的 {@link MaterialExcelModel} 清單封裝為回應結構 {@link MaterialImportedResource}
	 *
	 * @param models    物料模型清單
	 * @param sheetName 工作表名稱
	 * @return 物料匯入回應資源
	 */
	public MaterialImportedResource toMaterialImportedResource(List<MaterialExcelModel> models, String sheetName) {
		if (models == null || models.isEmpty()) {
			return new MaterialImportedResource(0, sheetName != null ? sheetName : "物料主檔清單", Collections.emptyList());
		}

		List<MaterialRetrievedResource> items = new ArrayList<>(models.size());
		for (MaterialExcelModel m : models) {
			if (m != null) {
				items.add(new MaterialRetrievedResource(
						m.getMaterialCode(),
						m.getMaterialName(),
						m.getSpecification(),
						m.getCategory(),
						m.getUnit(),
						m.getStandardCost(),
						m.getStatus()
				));
			}
		}
		return new MaterialImportedResource(items.size(), sheetName != null ? sheetName : "物料主檔清單", items);
	}

	/**
	 * 將領域層 {@link Material} 聚合根轉換為應用層 {@link MaterialExcelModel}
	 *
	 * @param material 物料聚合根
	 * @return 物料 Excel 映射模型
	 */
	public MaterialExcelModel toMaterialExcelModel(Material material) {
		if (material == null) {
			return null;
		}
		return new MaterialExcelModel(
				material.getCode().value(),
				material.getName(),
				material.getSpecification(),
				material.getCategory().name(),
				material.getUnit().name(),
				material.getStandardCost().amount(),
				material.getStatus().name()
		);
	}

	/**
	 * 將多筆領域層 {@link Material} 聚合根轉換為應用層 {@link MaterialExcelModel} 清單
	 *
	 * @param materials 物料聚合根清單
	 * @return 物料 Excel 映射模型清單
	 */
	public List<MaterialExcelModel> toMaterialExcelModelsFromDomain(List<Material> materials) {
		if (materials == null || materials.isEmpty()) {
			return Collections.emptyList();
		}
		List<MaterialExcelModel> list = new ArrayList<>(materials.size());
		for (Material m : materials) {
			MaterialExcelModel model = toMaterialExcelModel(m);
			if (model != null) {
				list.add(model);
			}
		}
		return list;
	}

	// =========================================================================
	// 2. BOM 單階物料清單轉換
	// =========================================================================

	/**
	 * 將 BOM 匯出請求資源轉換為 {@link BomItemExcelModel} 清單
	 *
	 * @param resource BOM 匯出請求資源
	 * @return BOM 子項 Excel 映射模型清單
	 */
	public List<BomItemExcelModel> toBomItemExcelModels(ExportBomItemResource resource) {
		if (resource == null || resource.items() == null || resource.items().isEmpty()) {
			return Collections.emptyList();
		}

		List<BomItemExcelModel> models = new ArrayList<>(resource.items().size());
		for (BomItemDetailResource item : resource.items()) {
			if (item != null) {
				models.add(new BomItemExcelModel(
						item.parentMaterialCode(),
						item.bomVersion(),
						item.itemSequence(),
						item.childMaterialCode(),
						item.childMaterialName(),
						item.quantity(),
						item.unit(),
						item.scrapRate(),
						item.positionCode(),
						item.remark()
				));
			}
		}
		return models;
	}

	/**
	 * 將已解析的 {@link BomItemExcelModel} 清單封裝為回應結構 {@link BomItemImportedResource}
	 *
	 * @param models    BOM 模型清單
	 * @param sheetName 工作表名稱
	 * @return BOM 匯入回應資源
	 */
	public BomItemImportedResource toBomItemImportedResource(List<BomItemExcelModel> models, String sheetName) {
		if (models == null || models.isEmpty()) {
			return new BomItemImportedResource(0, sheetName != null ? sheetName : "單階BOM清單", Collections.emptyList());
		}

		List<BomItemRetrievedResource> items = new ArrayList<>(models.size());
		for (BomItemExcelModel m : models) {
			if (m != null) {
				items.add(new BomItemRetrievedResource(
						m.getParentMaterialCode(),
						m.getBomVersion(),
						m.getItemSequence(),
						m.getChildMaterialCode(),
						m.getChildMaterialName(),
						m.getQuantity(),
						m.getUnit(),
						m.getScrapRate(),
						m.getPositionCode(),
						m.getRemark()
				));
			}
		}
		return new BomItemImportedResource(items.size(), sheetName != null ? sheetName : "單階BOM清單", items);
	}

	/**
	 * 將多筆領域層 {@link Bom} 聚合根與關聯物料映射轉換為應用層 {@link BomItemExcelModel} 清單
	 *
	 * @param boms        BOM 聚合根清單
	 * @param materialMap 物料識別碼與物料聚合根映射 Map (用於解析料號、品名與單位)
	 * @return BOM 子項 Excel 映射模型清單
	 */
	public List<BomItemExcelModel> toBomItemExcelModelsFromDomain(
			List<Bom> boms,
			Map<MaterialId, Material> materialMap
	) {
		if (boms == null || boms.isEmpty()) {
			return Collections.emptyList();
		}

		List<BomItemExcelModel> models = new ArrayList<>();
		for (Bom bom : boms) {
			if (bom == null || bom.getItems() == null) {
				continue;
			}

			Material parentMaterial = materialMap != null ? materialMap.get(bom.getParentMaterialId()) : null;
			String parentCode = parentMaterial != null
					? parentMaterial.getCode().value()
					: (bom.getParentMaterialId() != null ? bom.getParentMaterialId().value().toString() : "");
			String version = bom.getVersion() != null ? bom.getVersion().value() : "1.0";

			for (BomItem item : bom.getItems()) {
				if (item == null) {
					continue;
				}

				Material childMaterial = materialMap != null ? materialMap.get(item.getChildMaterialId()) : null;
				String childCode = childMaterial != null
						? childMaterial.getCode().value()
						: (item.getChildMaterialId() != null ? item.getChildMaterialId().value().toString() : "");
				String childName = childMaterial != null ? childMaterial.getName() : "";
				String unit = childMaterial != null ? childMaterial.getUnit().name() : "";

				models.add(new BomItemExcelModel(
						parentCode,
						version,
						item.getItemSequence() != null ? item.getItemSequence().value() : 0,
						childCode,
						childName,
						item.getQuantity() != null ? item.getQuantity().value() : null,
						unit,
						item.getScrapRate() != null ? item.getScrapRate().rate() : null,
						item.getPositionCode(),
						item.getRemark()
				));
			}
		}
		return models;
	}
}

