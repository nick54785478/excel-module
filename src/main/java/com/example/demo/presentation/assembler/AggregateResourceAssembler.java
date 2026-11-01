package com.example.demo.presentation.assembler;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import com.example.demo.application.shared.model.BomItemExcelModel;
import com.example.demo.application.shared.model.MaterialExcelModel;
import com.example.demo.domain.material.aggregate.root.Material;
import com.example.demo.presentation.resource.in.ExportBomItemResource;
import com.example.demo.presentation.resource.in.ExportBomItemResource.BomItemDetailResource;
import com.example.demo.presentation.resource.in.ExportMaterialResource;
import com.example.demo.presentation.resource.in.ExportMaterialResource.MaterialItemResource;
import com.example.demo.presentation.resource.out.BomItemImportedResource;
import com.example.demo.presentation.resource.out.BomItemImportedResource.BomItemRetrievedResource;
import com.example.demo.presentation.resource.out.MaterialImportedResource;
import com.example.demo.presentation.resource.out.MaterialImportedResource.MaterialRetrievedResource;

/**
 * 聚合根對應之表現層資源組裝器 (Aggregate Presentation Resource Assembler)
 * <p>
 * 遵循 Clean Architecture 六角形架構，擔任表現層防腐層 (ACL) 角色。
 * 負責將 Request Resource 與 {@link MaterialExcelModel}、{@link BomItemExcelModel} 互相轉換，
 * 以及提供領域聚合根 {@link Material} 至 Excel 模型的對映轉換，確保各層資料隔離與單一職責。
 */
@Component
public class AggregateResourceAssembler {

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
			return createSampleMaterials();
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
			return createSampleBomItems();
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

	// =========================================================================
	// 3. 範例資料生成方法 (結合 Aggregate 領域真實數據結構)
	// =========================================================================

	/**
	 * 產製對應真實 {@link Material} 聚合根特性的展示物料資料
	 *
	 * @return 展示物料 Excel 模型清單
	 */
	public List<MaterialExcelModel> createSampleMaterials() {
		return List.of(
				new MaterialExcelModel("MAT-FG-001", "4K 專業航拍無人機", "DRONE-4K-PRO", "FINISHED_GOODS", "PCS", new BigDecimal("35000.00"), "ACTIVE"),
				new MaterialExcelModel("MAT-SFG-001", "4K 雲台相機模組", "CAM-4K-GIMBAL", "SEMI_FINISHED_GOODS", "SET", new BigDecimal("8500.00"), "ACTIVE"),
				new MaterialExcelModel("MAT-SFG-002", "高容量無人機鋰電池", "BAT-6S-5000MAH", "SEMI_FINISHED_GOODS", "PCS", new BigDecimal("3200.00"), "ACTIVE"),
				new MaterialExcelModel("MAT-RM-001", "碳纖維主機臂 (左前)", "CF-ARM-LF", "RAW_MATERIAL", "PCS", new BigDecimal("450.00"), "ACTIVE"),
				new MaterialExcelModel("MAT-RM-002", "無刷旋翼馬達 2306", "BRUSHLESS-2306-2400KV", "RAW_MATERIAL", "PCS", new BigDecimal("550.00"), "ACTIVE")
		);
	}

	/**
	 * 產製對應真實 {@link com.example.demo.domain.bom.aggregate.root.Bom} 聚合根特性的展示單階 BOM 配方資料
	 *
	 * @return 展示 BOM 配方 Excel 模型清單
	 */
	public List<BomItemExcelModel> createSampleBomItems() {
		return List.of(
				new BomItemExcelModel("MAT-FG-001", "1.0", 10, "MAT-SFG-001", "4K 雲台相機模組", new BigDecimal("1.0000"), "SET", new BigDecimal("0.00"), "CAM-BAY-01", "光學防手震相機模組"),
				new BomItemExcelModel("MAT-FG-001", "1.0", 20, "MAT-SFG-002", "高容量無人機鋰電池", new BigDecimal("1.0000"), "PCS", new BigDecimal("0.00"), "BAT-BAY-01", "快拆智慧電池"),
				new BomItemExcelModel("MAT-FG-001", "1.0", 30, "MAT-RM-001", "碳纖維主機臂 (左前)", new BigDecimal("1.0000"), "PCS", new BigDecimal("2.50"), "ARM-LF-01", "抗扭高剛性主臂"),
				new BomItemExcelModel("MAT-FG-001", "1.0", 40, "MAT-RM-002", "無刷旋翼馬達 2306", new BigDecimal("4.0000"), "PCS", new BigDecimal("1.00"), "M1-M4", "動力旋翼馬達")
		);
	}
}

