package com.example.demo.application.assembler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.example.demo.application.shared.command.ExportBomCommand;
import com.example.demo.application.shared.model.BomItemExcelModel;
import com.example.demo.application.shared.model.MaterialExcelModel;
import com.example.demo.domain.bom.aggregate.entity.BomItem;
import com.example.demo.domain.bom.aggregate.root.Bom;
import com.example.demo.domain.bom.aggregate.vo.BomStatus;
import com.example.demo.domain.material.aggregate.root.Material;
import com.example.demo.domain.material.aggregate.vo.MaterialId;
import com.example.demo.domain.material.exception.MaterialNotFoundException;

/**
 * 物料與 BOM 資料組裝器 (BOM DTO Assembler / Application Assembler)
 * <p>
 * 遵循 Clean Architecture 與 DDD 規範 (AGENTS.md 規則 21)，負責將純 Java 領域聚合根
 * ({@link Material}、{@link Bom}) 轉換為應用層資料傳輸物件 (DTO)、Excel 映射模型
 * ({@link MaterialExcelModel}、{@link BomItemExcelModel}) 以及工作表二維資料列陣列。
 * 嚴守單一職責原則，解耦領域模型、應用層服務與表現層。
 */
@Component
public class BomDtoAssembler {

	/**
	 * 解析並生成匯出 Excel 預設檔案名稱。
	 *
	 * @param command 匯出命令資料載體
	 * @return 格式化之 Excel 檔案名稱 (如 "BOM_MAT-FG-001_1.0.xlsx" 或 "BOM_LIST_ALL.xlsx")
	 */
	public static String resolveFileName(ExportBomCommand command) {
		if (command != null && command.getParentMaterialCode() != null && !command.getParentMaterialCode().isBlank()) {
			return "BOM_" + command.getParentMaterialCode().trim() +
					(command.getVersion() != null ? "_" + command.getVersion().trim() : "") + ".xlsx";
		}
		return "BOM_LIST_ALL.xlsx";
	}

	/**
	 * 依據匯出命令中的母件料號與版本號，自全量 BOM 中篩選目標 BOM 集合。
	 *
	 * @param command         匯出命令
	 * @param materialCodeMap 料號對應之物料索引 Map
	 * @param bomsByParentId  母件 ID 對應之 BOM 清單 Map
	 * @param allBoms         系統全量 BOM 配方清單
	 * @return 符合篩選條件之 BOM 清單
	 * @throws MaterialNotFoundException 當指定之母件料號不存在時拋出
	 */
	public static List<Bom> resolveTargetBoms(
			ExportBomCommand command,
			Map<String, Material> materialCodeMap,
			Map<MaterialId, List<Bom>> bomsByParentId,
			List<Bom> allBoms
	) {
		List<Bom> targetBoms = new ArrayList<>();
		if (command.getParentMaterialCode() != null) {
			Material parentMaterial = materialCodeMap.get(command.getParentMaterialCode());
			if (parentMaterial == null) {
				throw new MaterialNotFoundException("查無指定料號之物料主檔: " + command.getParentMaterialCode());
			}

			List<Bom> parentBoms = bomsByParentId.getOrDefault(parentMaterial.getId(), List.of());
			if (command.getVersion() != null) {
				parentBoms.stream()
						.filter(b -> b.getVersion().value().equals(command.getVersion()))
						.findFirst()
						.ifPresent(targetBoms::add);
			} else {
				targetBoms.addAll(parentBoms);
			}
		} else {
			targetBoms.addAll(allBoms);
		}
		return targetBoms;
	}

	/**
	 * 【命令式階層樹狀結構報表模組】遞迴建構多階層樹狀 BOM 展開資料列。
	 *
	 * @param targetBoms      目標根節點 BOM 清單
	 * @param materialIdMap   物料 ID 對應之物料索引 Map
	 * @param bomsByParentId  母件 ID 對應之 BOM 配方清單 Map
	 * @return 多階展開二維列資料清單
	 */
	public static List<List<Object>> buildMultiLevelRows(
			List<Bom> targetBoms,
			Map<MaterialId, Material> materialIdMap,
			Map<MaterialId, List<Bom>> bomsByParentId
	) {
		List<List<Object>> rows = new ArrayList<>();
		Set<MaterialId> processedParents = new HashSet<>();

		for (Bom rootBom : targetBoms) {
			Material rootMaterial = materialIdMap.get(rootBom.getParentMaterialId());
			if (rootMaterial == null || processedParents.contains(rootMaterial.getId())) {
				continue;
			}
			processedParents.add(rootMaterial.getId());

			// Level 0 根節點列
			BigDecimal rootUnitCost = rootMaterial.getStandardCost().amount();
			rows.add(List.of(
					"0",
					rootMaterial.getCode().value(),
					rootMaterial.getName(),
					rootMaterial.getSpecification(),
					BigDecimal.ONE,
					rootMaterial.getUnit().name(),
					"0.00%",
					rootUnitCost,
					rootUnitCost,
					"-",
					"產品主檔總成 (Top Level)"
			));

			// 遞迴展開 Level 1, 2, ...
			Set<MaterialId> visitedBranch = new HashSet<>();
			visitedBranch.add(rootMaterial.getId());
			expandBomItemsRecursive(
					rootBom,
					1,
					BigDecimal.ONE,
					visitedBranch,
					rows,
					materialIdMap,
					bomsByParentId
			);
		}

		return rows;
	}

	/**
	 * 遞迴展開 BOM 子項節點。
	 */
	private static void expandBomItemsRecursive(
			Bom currentBom,
			int level,
			BigDecimal accumulatedQuantity,
			Set<MaterialId> visitedBranch,
			List<List<Object>> rows,
			Map<MaterialId, Material> materialIdMap,
			Map<MaterialId, List<Bom>> bomsByParentId
	) {
		List<BomItem> sortedItems = currentBom.getItems().stream()
				.sorted(Comparator.comparing(item -> item.getItemSequence().value()))
				.toList();

		String levelPrefix = ".".repeat(level) + level;

		for (BomItem item : sortedItems) {
			Material childMaterial = materialIdMap.get(item.getChildMaterialId());
			if (childMaterial == null) {
				continue;
			}

			BigDecimal itemQty = item.getQuantity().value();
			BigDecimal totalEffectiveQty = itemQty.multiply(accumulatedQuantity);
			BigDecimal unitCost = childMaterial.getStandardCost().amount();
			BigDecimal totalCost = unitCost.multiply(totalEffectiveQty).setScale(2, RoundingMode.HALF_UP);
			String scrapPercentString = item.getScrapRate().toPercentString();

			rows.add(List.of(
					levelPrefix,
					childMaterial.getCode().value(),
					childMaterial.getName(),
					childMaterial.getSpecification(),
					totalEffectiveQty,
					childMaterial.getUnit().name(),
					scrapPercentString,
					unitCost,
					totalCost,
					item.getPositionCode(),
					item.getRemark()
			));

			// 檢查此子項是否具備下階子 BOM 配方 (如半成品)
			if (!visitedBranch.contains(childMaterial.getId())) {
				List<Bom> subBoms = bomsByParentId.get(childMaterial.getId());
				if (subBoms != null && !subBoms.isEmpty()) {
					// 優先選擇已審核 (APPROVED) 或最新版之 BOM
					Bom activeSubBom = subBoms.stream()
							.filter(b -> b.getStatus() == BomStatus.APPROVED)
							.findFirst()
							.orElse(subBoms.getFirst());

					visitedBranch.add(childMaterial.getId());
					expandBomItemsRecursive(
							activeSubBom,
							level + 1,
							totalEffectiveQty,
							visitedBranch,
							rows,
							materialIdMap,
							bomsByParentId
					);
					visitedBranch.remove(childMaterial.getId());
				}
			}
		}
	}

	/**
	 * 【命令式階層樹狀結構報表模組】建構單階平鋪式 BOM 匯入格式資料列。
	 *
	 * @param targetBoms    目標 BOM 清單
	 * @param materialIdMap 物料 ID 對應之物料索引 Map
	 * @return 單階平鋪資料列清單
	 */
	public static List<List<Object>> buildSingleLevelRows(
			List<Bom> targetBoms,
			Map<MaterialId, Material> materialIdMap
	) {
		List<List<Object>> rows = new ArrayList<>();

		for (Bom bom : targetBoms) {
			Material parentMaterial = materialIdMap.get(bom.getParentMaterialId());
			String parentCode = parentMaterial != null ? parentMaterial.getCode().value() : bom.getParentMaterialId().value();

			List<BomItem> sortedItems = bom.getItems().stream()
					.sorted(Comparator.comparing(item -> item.getItemSequence().value()))
					.toList();

			for (BomItem item : sortedItems) {
				Material childMaterial = materialIdMap.get(item.getChildMaterialId());
				String childCode = childMaterial != null ? childMaterial.getCode().value() : item.getChildMaterialId().value();
				String childName = childMaterial != null ? childMaterial.getName() : "";
				String unit = childMaterial != null ? childMaterial.getUnit().name() : "PCS";

				rows.add(List.of(
						parentCode,
						bom.getVersion().value(),
						item.getItemSequence().value(),
						childCode,
						childName,
						item.getQuantity().value(),
						unit,
						item.getScrapRate().rate().multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP),
						item.getPositionCode(),
						"", // 替代料號
						"2026-01-01", // 生效日期
						item.getRemark()
				));
			}
		}

		return rows;
	}

	/**
	 * 【命令式階層樹狀結構報表模組】建構物料主檔對照表資料列。
	 *
	 * @param allMaterials 系統所有物料聚合根清單
	 * @return 物料主檔對照表資料列清單
	 */
	public static List<List<Object>> buildMaterialMasterRows(List<Material> allMaterials) {
		List<List<Object>> rows = new ArrayList<>();
		List<Material> sortedMaterials = allMaterials.stream()
				.sorted(Comparator.comparing(m -> m.getCode().value()))
				.toList();

		for (Material m : sortedMaterials) {
			rows.add(List.of(
					m.getCode().value(),
					m.getName(),
					m.getSpecification(),
					m.getCategory().getDisplayName() + " (" + m.getCategory().name() + ")",
					m.getUnit().name(),
					m.getStandardCost().amount(),
					m.getStatus().getDisplayName() + " (" + m.getStatus().name() + ")"
			));
		}

		return rows;
	}

	/**
	 * 【宣告式物件映射模組】將多筆物料聚合根轉換為物料宣告式 Excel 模型清單。
	 *
	 * @param materials 物料聚合根清單
	 * @return 物料 Excel 映射模型清單
	 */
	public static List<MaterialExcelModel> toMaterialExcelModels(List<Material> materials) {
		if (materials == null || materials.isEmpty()) {
			return Collections.emptyList();
		}
		List<MaterialExcelModel> list = new ArrayList<>(materials.size());
		for (Material m : materials) {
			if (m != null) {
				list.add(new MaterialExcelModel(
						m.getCode().value(),
						m.getName(),
						m.getSpecification(),
						m.getCategory().name(),
						m.getUnit().name(),
						m.getStandardCost().amount(),
						m.getStatus().name()
				));
			}
		}
		return list;
	}

	/**
	 * 【宣告式物件映射模組】將多筆 BOM 聚合根及其所屬子項實體轉換為單階 BOM 宣告式 Excel 模型清單。
	 *
	 * @param boms        BOM 聚合根清單
	 * @param materialMap 物料索引 Map，用於取得料號、名稱與單位
	 * @return 單階 BOM Excel 映射模型清單
	 */
	public static List<BomItemExcelModel> toBomItemExcelModels(List<Bom> boms, Map<MaterialId, Material> materialMap) {
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

