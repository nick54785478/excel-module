package com.example.demo.presentation.assembler;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.demo.application.shared.dto.BeanSheetGottenResult;
import com.example.demo.application.shared.model.BomItemExcelModel;
import com.example.demo.application.shared.model.MaterialExcelModel;
import com.example.demo.application.shared.model.MultiLevelBomExcelModel;
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
import com.example.demo.presentation.resource.out.BomItemImportedResource.MaterialMasterSheetResource;
import com.example.demo.presentation.resource.out.BomItemImportedResource.MultiLevelBomRetrievedResource;
import com.example.demo.presentation.resource.out.BomItemImportedResource.MultiLevelBomSheetResource;
import com.example.demo.presentation.resource.out.BomItemImportedResource.SingleLevelBomSheetResource;
import com.example.demo.presentation.resource.out.MaterialImportedResource;
import com.example.demo.presentation.resource.out.MaterialImportedResource.MaterialRetrievedResource;
import com.example.demo.presentation.resource.out.MultiLevelBomImportedResource;

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
		return toMaterialImportedResource(models, sheetName, Collections.emptyList(), null, Collections.emptyList(), null);
	}

	/**
	 * 將 {@link BeanSheetGottenResult} 物料主檔結果封裝為回應結構 {@link MaterialImportedResource}
	 *
	 * @param matResult 物料主檔工作表解析結果
	 * @return 物料匯入回應資源
	 */
	public MaterialImportedResource toMaterialImportedResource(BeanSheetGottenResult<MaterialExcelModel> matResult) {
		if (matResult == null) {
			return toMaterialImportedResource(Collections.emptyList(), null);
		}
		return toMaterialImportedResource(matResult.items(), matResult.sheetName(), Collections.emptyList(), null, Collections.emptyList(), null);
	}

	/**
	 * 將 {@link BeanSheetGottenResult} 單階 BOM 結果封裝至 {@link MaterialImportedResource} 專屬單階工作表槽位
	 *
	 * @param singleResult 單階 BOM 工作表解析結果
	 * @return 包含單階工作表槽位之物料回應資源
	 */
	public MaterialImportedResource toMaterialImportedResourceFromSingleBom(BeanSheetGottenResult<BomItemExcelModel> singleResult) {
		if (singleResult == null) {
			return toMaterialImportedResource(Collections.emptyList(), null);
		}
		return toMaterialImportedResource(Collections.emptyList(), null, singleResult.items(), singleResult.sheetName(), Collections.emptyList(), null);
	}

	/**
	 * 將 {@link BeanSheetGottenResult} 多階層 BOM 結果封裝至 {@link MaterialImportedResource} 專屬多階工作表槽位
	 *
	 * @param multiResult 多階層 BOM 工作表解析結果
	 * @return 包含多階工作表槽位之物料回應資源
	 */
	public MaterialImportedResource toMaterialImportedResourceFromMultiBom(BeanSheetGottenResult<MultiLevelBomExcelModel> multiResult) {
		if (multiResult == null) {
			return toMaterialImportedResource(Collections.emptyList(), null);
		}
		return toMaterialImportedResource(Collections.emptyList(), null, Collections.emptyList(), null, multiResult.items(), multiResult.sheetName());
	}

	/**
	 * 同時整合物料主檔對照表、多階層展開 BOM 與單階 BOM 之全量結構化回應資源組裝
	 * <p>
	 * 確保未填 sheetName 時所有相符工作表資料 (物料主檔、單階與多階) 完整印出，且各自歸屬於獨立專屬 Model。
	 * </p>
	 *
	 * @param materialModels    物料主檔模型清單
	 * @param materialSheetName 物料主檔工作表名稱
	 * @param singleModels      單階 BOM 模型清單
	 * @param singleSheetName   單階 BOM 工作表名稱
	 * @param multiModels       多階層 BOM 模型清單
	 * @param multiSheetName    多階層 BOM 工作表名稱
	 * @return 包含三工作表結構化資訊之物料回應資源
	 */
	public MaterialImportedResource toMaterialImportedResource(
			List<MaterialExcelModel> materialModels,
			String materialSheetName,
			List<BomItemExcelModel> singleModels,
			String singleSheetName,
			List<MultiLevelBomExcelModel> multiModels,
			String multiSheetName
	) {
		MaterialMasterSheetResource materialSheet = null;
		List<MaterialRetrievedResource> matItems = new ArrayList<>();
		if (materialModels != null && !materialModels.isEmpty()) {
			for (MaterialExcelModel m : materialModels) {
				if (m != null) {
					matItems.add(new MaterialRetrievedResource(
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
			materialSheet = new MaterialMasterSheetResource(
					materialSheetName != null ? materialSheetName : "物料主檔對照表",
					matItems.size(),
					matItems
			);
		}

		SingleLevelBomSheetResource singleSheet = null;
		List<BomItemRetrievedResource> singleItems = new ArrayList<>();
		if (singleModels != null && !singleModels.isEmpty()) {
			for (BomItemExcelModel m : singleModels) {
				if (m != null) {
					singleItems.add(new BomItemRetrievedResource(
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
			singleSheet = new SingleLevelBomSheetResource(
					singleSheetName != null ? singleSheetName : "單階BOM匯入清單",
					singleItems.size(),
					singleItems
			);
		}

		MultiLevelBomSheetResource multiSheet = null;
		List<MultiLevelBomRetrievedResource> multiItems = new ArrayList<>();
		if (multiModels != null && !multiModels.isEmpty()) {
			for (MultiLevelBomExcelModel m : multiModels) {
				if (m != null) {
					multiItems.add(toMultiLevelBomRetrievedResource(m));
				}
			}
			multiSheet = new MultiLevelBomSheetResource(
					multiSheetName != null ? multiSheetName : "多階層BOM展開清單",
					multiItems.size(),
					multiItems
			);
		}

		int matCount = materialSheet != null ? materialSheet.totalCount() : 0;
		int singleCount = singleSheet != null ? singleSheet.totalCount() : 0;
		int multiCount = multiSheet != null ? multiSheet.totalCount() : 0;
		int total = matCount + singleCount + multiCount;

		// 向下相容 items：優先提供物料主檔項目，若無物料主檔但有多階項目則轉換多階的料件資訊
		List<MaterialRetrievedResource> fallbackItems = !matItems.isEmpty() ? matItems : new ArrayList<>();
		if (fallbackItems.isEmpty() && !multiItems.isEmpty()) {
			for (MultiLevelBomRetrievedResource mi : multiItems) {
				fallbackItems.add(new MaterialRetrievedResource(
						mi.childMaterialCode(),
						mi.childMaterialName(),
						mi.specification(),
						mi.category(),
						mi.unit(),
						mi.standardPrice(),
						"ACTIVE"
				));
			}
		}

		List<String> sheetSummaries = new ArrayList<>();
		if (multiSheet != null) {
			sheetSummaries.add(multiSheet.sheetName() + ": " + multiCount + "筆");
		}
		if (singleSheet != null) {
			sheetSummaries.add(singleSheet.sheetName() + ": " + singleCount + "筆");
		}
		if (materialSheet != null) {
			sheetSummaries.add(materialSheet.sheetName() + ": " + matCount + "筆");
		}

		String displaySheetName;
		if (sheetSummaries.size() > 1) {
			displaySheetName = "全部工作表 (" + String.join(", ", sheetSummaries) + ")";
		} else if (!sheetSummaries.isEmpty()) {
			if (materialSheet != null) {
				displaySheetName = materialSheet.sheetName();
			} else if (multiSheet != null) {
				displaySheetName = multiSheet.sheetName();
			} else {
				displaySheetName = singleSheet.sheetName();
			}
		} else {
			displaySheetName = "無相符工作表";
		}

		return new MaterialImportedResource(total, displaySheetName, fallbackItems, materialSheet, multiSheet, singleSheet);
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
		SingleLevelBomSheetResource singleSheet = new SingleLevelBomSheetResource(
				sheetName != null ? sheetName : "單階BOM清單", items.size(), items);
		return new BomItemImportedResource(items.size(), sheetName != null ? sheetName : "單階BOM清單", items, null, singleSheet);
	}

	/**
	 * 將 {@link BeanSheetGottenResult} 單階 BOM 結果封裝為回應結構 {@link BomItemImportedResource}
	 *
	 * @param singleResult 單階 BOM 工作表解析結果
	 * @return BOM 匯入回應資源
	 */
	public BomItemImportedResource toBomItemImportedResource(BeanSheetGottenResult<BomItemExcelModel> singleResult) {
		if (singleResult == null) {
			return toBomItemImportedResource(Collections.emptyList(), null);
		}
		return toBomItemImportedResource(singleResult.items(), singleResult.sheetName(), Collections.emptyList(), null, Collections.emptyList(), null);
	}

	/**
	 * 將 {@link BeanSheetGottenResult} 多階層 BOM 結果封裝至 {@link BomItemImportedResource} 專屬多階工作表槽位
	 *
	 * @param multiResult 多階層 BOM 工作表解析結果
	 * @return 包含多階工作表槽位之 BOM 回應資源
	 */
	public BomItemImportedResource toBomItemImportedResourceFromMultiBom(BeanSheetGottenResult<MultiLevelBomExcelModel> multiResult) {
		if (multiResult == null) {
			return toBomItemImportedResource(Collections.emptyList(), null);
		}
		return toBomItemImportedResource(Collections.emptyList(), null, multiResult.items(), multiResult.sheetName(), Collections.emptyList(), null);
	}

	/**
	 * 將 {@link BeanSheetGottenResult} 物料主檔結果封裝至 {@link BomItemImportedResource} 專屬物料工作表槽位
	 *
	 * @param matResult 物料主檔工作表解析結果
	 * @return 包含物料主檔工作表槽位之 BOM 回應資源
	 */
	public BomItemImportedResource toBomItemImportedResourceFromMaterial(BeanSheetGottenResult<MaterialExcelModel> matResult) {
		if (matResult == null) {
			return toBomItemImportedResource(Collections.emptyList(), null);
		}
		return toBomItemImportedResource(Collections.emptyList(), null, Collections.emptyList(), null, matResult.items(), matResult.sheetName());
	}

	/**
	 * 同時整合單階 BOM、多階層展開 BOM 與物料主檔對照表之結構化回應資源組裝
	 * <p>
	 * 確保未填 sheetName 時所有相符工作表資料完整印出，且各自歸屬於獨立專屬 Model，避免欄位錯位與重複加總。
	 * </p>
	 *
	 * @param singleModels    單階 BOM 模型清單
	 * @param singleSheetName 單階 BOM 工作表名稱
	 * @param multiModels     多階層 BOM 模型清單
	 * @param multiSheetName  多階層 BOM 工作表名稱
	 * @return 包含雙工作表結構化資訊之回應資源
	 */
	public BomItemImportedResource toBomItemImportedResource(
			List<BomItemExcelModel> singleModels,
			String singleSheetName,
			List<MultiLevelBomExcelModel> multiModels,
			String multiSheetName
	) {
		return toBomItemImportedResource(singleModels, singleSheetName, multiModels, multiSheetName, Collections.emptyList(), null);
	}

	/**
	 * 同時整合單階 BOM、多階層展開 BOM 與物料主檔對照表之全量結構化回應資源組裝
	 * <p>
	 * 確保未填 sheetName 時所有相符工作表資料 (單階、多階與物料主檔) 完整印出，且各自歸屬於獨立專屬 Model。
	 * </p>
	 *
	 * @param singleModels      單階 BOM 模型清單
	 * @param singleSheetName   單階 BOM 工作表名稱
	 * @param multiModels       多階層 BOM 模型清單
	 * @param multiSheetName    多階層 BOM 工作表名稱
	 * @param materialModels    物料主檔模型清單
	 * @param materialSheetName 物料主檔工作表名稱
	 * @return 包含三工作表結構化資訊之回應資源
	 */
	public BomItemImportedResource toBomItemImportedResource(
			List<BomItemExcelModel> singleModels,
			String singleSheetName,
			List<MultiLevelBomExcelModel> multiModels,
			String multiSheetName,
			List<MaterialExcelModel> materialModels,
			String materialSheetName
	) {
		SingleLevelBomSheetResource singleSheet = null;
		List<BomItemRetrievedResource> singleItems = new ArrayList<>();
		if (singleModels != null && !singleModels.isEmpty()) {
			for (BomItemExcelModel m : singleModels) {
				if (m != null) {
					singleItems.add(new BomItemRetrievedResource(
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
			singleSheet = new SingleLevelBomSheetResource(
					singleSheetName != null ? singleSheetName : "單階BOM匯入清單",
					singleItems.size(),
					singleItems
			);
		}

		MultiLevelBomSheetResource multiSheet = null;
		List<MultiLevelBomRetrievedResource> multiItems = new ArrayList<>();
		if (multiModels != null && !multiModels.isEmpty()) {
			for (MultiLevelBomExcelModel m : multiModels) {
				if (m != null) {
					multiItems.add(toMultiLevelBomRetrievedResource(m));
				}
			}
			multiSheet = new MultiLevelBomSheetResource(
					multiSheetName != null ? multiSheetName : "多階層BOM展開清單",
					multiItems.size(),
					multiItems
			);
		}

		MaterialMasterSheetResource materialSheet = null;
		List<MaterialRetrievedResource> matItems = new ArrayList<>();
		if (materialModels != null && !materialModels.isEmpty()) {
			for (MaterialExcelModel m : materialModels) {
				if (m != null) {
					matItems.add(new MaterialRetrievedResource(
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
			materialSheet = new MaterialMasterSheetResource(
					materialSheetName != null ? materialSheetName : "物料主檔對照表",
					matItems.size(),
					matItems
			);
		}

		int singleCount = singleSheet != null ? singleSheet.totalCount() : 0;
		int multiCount = multiSheet != null ? multiSheet.totalCount() : 0;
		int matCount = materialSheet != null ? materialSheet.totalCount() : 0;
		int total = singleCount + multiCount + matCount;

		// 向下相容 items：若有單階資料優先提供單階，否則將多階資料轉換為相容子項
		List<BomItemRetrievedResource> fallbackItems = !singleItems.isEmpty() ? singleItems : new ArrayList<>();
		if (fallbackItems.isEmpty() && !multiItems.isEmpty()) {
			for (MultiLevelBomRetrievedResource mi : multiItems) {
				fallbackItems.add(new BomItemRetrievedResource(
						mi.parentMaterialCode(),
						"1.0",
						null,
						mi.childMaterialCode(),
						mi.childMaterialName(),
						mi.quantity(),
						mi.unit(),
						mi.scrapRate(),
						mi.positionCode(),
						mi.remark()
				));
			}
		}

		List<String> sheetSummaries = new ArrayList<>();
		if (multiSheet != null) {
			sheetSummaries.add(multiSheet.sheetName() + ": " + multiCount + "筆");
		}
		if (singleSheet != null) {
			sheetSummaries.add(singleSheet.sheetName() + ": " + singleCount + "筆");
		}
		if (materialSheet != null) {
			sheetSummaries.add(materialSheet.sheetName() + ": " + matCount + "筆");
		}

		String displaySheetName;
		if (sheetSummaries.size() > 1) {
			displaySheetName = "全部工作表 (" + String.join(", ", sheetSummaries) + ")";
		} else if (!sheetSummaries.isEmpty()) {
			if (singleSheet != null) {
				displaySheetName = singleSheet.sheetName();
			} else if (multiSheet != null) {
				displaySheetName = multiSheet.sheetName();
			} else {
				displaySheetName = materialSheet.sheetName();
			}
		} else {
			displaySheetName = "無相符工作表";
		}

		return new BomItemImportedResource(total, displaySheetName, fallbackItems, multiSheet, singleSheet, materialSheet);
	}

	/**
	 * 將多階層 BOM 模型清單封裝為專屬回應結構 {@link MultiLevelBomImportedResource}
	 *
	 * @param models    多階層 BOM 模型清單
	 * @param sheetName 工作表名稱
	 * @return 多階層 BOM 匯入回應資源
	 */
	public MultiLevelBomImportedResource toMultiLevelBomImportedResource(
			List<MultiLevelBomExcelModel> models,
			String sheetName
	) {
		if (models == null || models.isEmpty()) {
			return new MultiLevelBomImportedResource(0, sheetName != null ? sheetName : "多階層BOM展開清單", Collections.emptyList());
		}

		List<MultiLevelBomRetrievedResource> items = new ArrayList<>(models.size());
		for (MultiLevelBomExcelModel m : models) {
			if (m != null) {
				items.add(toMultiLevelBomRetrievedResource(m));
			}
		}
		return new MultiLevelBomImportedResource(items.size(), sheetName != null ? sheetName : "多階層BOM展開清單", items);
	}

	private MultiLevelBomRetrievedResource toMultiLevelBomRetrievedResource(MultiLevelBomExcelModel m) {
		return new MultiLevelBomRetrievedResource(
				m.getLevel(),
				m.getLevelSequence(),
				m.getParentMaterialCode(),
				m.getChildMaterialCode(),
				m.getChildMaterialName(),
				m.getSpecification(),
				m.getCategory(),
				m.getUnit(),
				m.getQuantity(),
				m.getScrapRate(),
				m.getEffectiveQuantity(),
				m.getStandardPrice(),
				m.getSubtotalCost(),
				m.getPositionCode(),
				m.getRemark()
		);
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

