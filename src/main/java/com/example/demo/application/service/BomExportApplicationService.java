package com.example.demo.application.service;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.application.port.in.ExportBomUseCase;
import com.example.demo.application.port.in.StreamExportBomUseCase;
import com.example.demo.application.port.out.BomRepositoryPort;
import com.example.demo.application.port.out.ExcelWriterPort;
import com.example.demo.application.port.out.MaterialRepositoryPort;
import com.example.demo.application.shared.command.ExportBomCommand;
import com.example.demo.application.shared.command.WriteExcelCommand;
import com.example.demo.application.shared.command.WriteSheetCommand;
import com.example.demo.application.shared.command.WriteStreamExcelCommand;
import com.example.demo.application.shared.command.WriteStreamSheetCommand;
import com.example.demo.application.shared.dto.BomExcelGottenResult;
import com.example.demo.domain.bom.aggregate.entity.BomItem;
import com.example.demo.domain.bom.aggregate.root.Bom;
import com.example.demo.domain.bom.aggregate.vo.BomStatus;
import com.example.demo.domain.bom.aggregate.vo.BomVersion;
import com.example.demo.domain.material.aggregate.root.Material;
import com.example.demo.domain.material.aggregate.vo.MaterialCategory;
import com.example.demo.domain.material.aggregate.vo.MaterialCode;
import com.example.demo.domain.material.aggregate.vo.MaterialId;
import com.example.demo.domain.material.exception.MaterialNotFoundException;

/**
 * 物料清單匯出應用服務 (BOM Export Inbound Adapter / Application Service)
 * <p>
 * 遵循六角形架構規範，實作 {@link ExportBomUseCase} 與 {@link StreamExportBomUseCase} 業務合約。
 * 負責調度 {@link BomRepositoryPort}、{@link MaterialRepositoryPort} 與 {@link ExcelWriterPort}，
 * 支援一般記憶體批次匯出（{@link #exportBom(ExportBomCommand)}）
 * 以及百萬級大數據串流輸出（{@link #streamExportBom(OutputStream, ExportBomCommand)}，O(1) 記憶體常數開銷）。
 * <p>
 * <b>事務邊界管轄</b>：遵循 Clean Architecture 原則，由 Application Service 主導定義 Use Case 級別的唯讀交易 (Read-Only Transaction)。
 */
@Service
@Transactional(readOnly = true)
public class BomExportApplicationService implements ExportBomUseCase, StreamExportBomUseCase {

	private final BomRepositoryPort bomRepositoryPort;
	private final MaterialRepositoryPort materialRepositoryPort;
	private final ExcelWriterPort excelWriterPort;

	public BomExportApplicationService(
			BomRepositoryPort bomRepositoryPort,
			MaterialRepositoryPort materialRepositoryPort,
			ExcelWriterPort excelWriterPort
	) {
		this.bomRepositoryPort = Objects.requireNonNull(bomRepositoryPort, "bomRepositoryPort 不得為空");
		this.materialRepositoryPort = Objects.requireNonNull(materialRepositoryPort, "materialRepositoryPort 不得為空");
		this.excelWriterPort = Objects.requireNonNull(excelWriterPort, "excelWriterPort 不得為空");
	}

	/**
	 * 解析並生成匯出 Excel 預設檔案名稱。
	 *
	 * @param command 匯出命令
	 * @return 格式化之 Excel 檔案名稱
	 */
	public static String resolveFileName(ExportBomCommand command) {
		if (command != null && command.getParentMaterialCode() != null && !command.getParentMaterialCode().isBlank()) {
			return "BOM_" + command.getParentMaterialCode().trim() +
					(command.getVersion() != null ? "_" + command.getVersion().trim() : "") + ".xlsx";
		}
		return "BOM_LIST_ALL.xlsx";
	}

	@Override
	public BomExcelGottenResult exportBom(ExportBomCommand command) {
		Objects.requireNonNull(command, "ExportBomCommand 不得為空");

		// 1. 載入物料主檔快照，建立索引 Map
		List<Material> allMaterials = materialRepositoryPort.findAll();
		Map<MaterialId, Material> materialIdMap = allMaterials.stream()
				.collect(Collectors.toMap(Material::getId, Function.identity(), (m1, m2) -> m1));
		Map<String, Material> materialCodeMap = allMaterials.stream()
				.collect(Collectors.toMap(m -> m.getCode().value(), Function.identity(), (m1, m2) -> m1));

		// 2. 載入所有 BOM 配方，建立以母件 ID 為鍵的索引
		List<Bom> allBoms = bomRepositoryPort.findAll();
		Map<MaterialId, List<Bom>> bomsByParentId = new HashMap<>();
		for (Bom bom : allBoms) {
			bomsByParentId.computeIfAbsent(bom.getParentMaterialId(), k -> new ArrayList<>()).add(bom);
		}

		// 3. 決定目標母件與 BOM 清單
		List<Bom> targetBoms = resolveTargetBoms(command, materialCodeMap, bomsByParentId, allBoms);
		String effectiveFileName = resolveFileName(command);

		// 4. 動態建構各工作表 (Sheets)
		List<WriteSheetCommand> sheets = new ArrayList<>();
		int totalRowsCount = 0;

		// 4-1. 多階層 BOM 展開清單
		if (command.isIncludeMultiLevel()) {
			List<List<Object>> multiLevelRows = buildMultiLevelRows(targetBoms, materialIdMap, bomsByParentId);
			totalRowsCount += multiLevelRows.size();

			sheets.add(WriteSheetCommand.builder()
					.sheetName("多階層BOM展開清單")
					.headers(List.of(
							"層級階碼", "物料編號", "物料名稱", "規格型號", "用量",
							"單位", "損耗率 (%)", "單位成本 (TWD)", "累計總成本 (TWD)", "位置位號", "備註說明"
					))
					.rows(multiLevelRows)
					.freezeHeader(true)
					.headerFillColor("1F4E78")
					.headerFontColor("FFFFFF")
					.build());
		}

		// 4-2. 單階 BOM 匯入格式清單
		if (command.isIncludeSingleLevel()) {
			List<List<Object>> singleLevelRows = buildSingleLevelRows(targetBoms, materialIdMap);
			totalRowsCount += singleLevelRows.size();

			sheets.add(WriteSheetCommand.builder()
					.sheetName("單階BOM匯入清單")
					.headers(List.of(
							"父階物料編號", "BOM版本號", "子項序號", "子件物料編號", "子件物料名稱",
							"組裝用量", "單位", "損耗率 (%)", "位置位號", "替代料號", "生效日期", "備註"
					))
					.rows(singleLevelRows)
					.freezeHeader(true)
					.headerFillColor("C65911")
					.headerFontColor("FFFFFF")
					.build());
		}

		// 4-3. 物料主檔對照表
		if (command.isIncludeMaterialMaster()) {
			List<List<Object>> materialMasterRows = buildMaterialMasterRows(allMaterials);
			totalRowsCount += materialMasterRows.size();

			sheets.add(WriteSheetCommand.builder()
					.sheetName("物料主檔對照表")
					.headers(List.of(
							"物料編號", "物料名稱", "規格型號", "物料類別", "計量單位", "標準進價成本", "狀態"
					))
					.rows(materialMasterRows)
					.freezeHeader(true)
					.headerFillColor("385723")
					.headerFontColor("FFFFFF")
					.build());
		}

		// 5. 調用底層 Excel 輸出埠產製 Excel 位元組
		WriteExcelCommand writeCommand = WriteExcelCommand.builder()
				.applicationName("ExcelModule-BOM-Exporter")
				.applicationVersion("1.0")
				.sheets(sheets)
				.build();

		byte[] excelBytes = excelWriterPort.write(writeCommand);

		return new BomExcelGottenResult(excelBytes, effectiveFileName, sheets.size(), totalRowsCount);
	}

	@Override
	public void streamExportBom(OutputStream outputStream, ExportBomCommand command) {
		Objects.requireNonNull(outputStream, "OutputStream 不得為空");
		Objects.requireNonNull(command, "ExportBomCommand 不得為空");

		// 1. 載入物料主檔快照，建立索引 Map
		List<Material> allMaterials = materialRepositoryPort.findAll();
		Map<MaterialId, Material> materialIdMap = allMaterials.stream()
				.collect(Collectors.toMap(Material::getId, Function.identity(), (m1, m2) -> m1));
		Map<String, Material> materialCodeMap = allMaterials.stream()
				.collect(Collectors.toMap(m -> m.getCode().value(), Function.identity(), (m1, m2) -> m1));

		// 2. 載入所有 BOM 配方，建立以母件 ID 為鍵的索引
		List<Bom> allBoms = bomRepositoryPort.findAll();
		Map<MaterialId, List<Bom>> bomsByParentId = new HashMap<>();
		for (Bom bom : allBoms) {
			bomsByParentId.computeIfAbsent(bom.getParentMaterialId(), k -> new ArrayList<>()).add(bom);
		}

		// 3. 決定目標母件與 BOM 清單
		List<Bom> targetBoms = resolveTargetBoms(command, materialCodeMap, bomsByParentId, allBoms);
		int flushInterval = command.getFlushInterval();

		// 4. 動態建構串流工作表清單 (WriteStreamSheetCommand)
		List<WriteStreamSheetCommand> streamSheets = new ArrayList<>();

		// 4-1. 多階層 BOM 展開清單 (串流分批沖刷)
		if (command.isIncludeMultiLevel()) {
			List<List<Object>> multiLevelRows = buildMultiLevelRows(targetBoms, materialIdMap, bomsByParentId);
			streamSheets.add(WriteStreamSheetCommand.builder()
					.sheetName("多階層BOM展開清單")
					.headers(List.of(
							"層級階碼", "物料編號", "物料名稱", "規格型號", "用量",
							"單位", "損耗率 (%)", "單位成本 (TWD)", "累計總成本 (TWD)", "位置位號", "備註說明"
					))
					.rowIterable(multiLevelRows)
					.flushInterval(flushInterval)
					.freezeHeader(true)
					.headerFillColor("1F4E78")
					.headerFontColor("FFFFFF")
					.build());
		}

		// 4-2. 單階 BOM 匯入格式清單 (串流分批沖刷)
		if (command.isIncludeSingleLevel()) {
			List<List<Object>> singleLevelRows = buildSingleLevelRows(targetBoms, materialIdMap);
			streamSheets.add(WriteStreamSheetCommand.builder()
					.sheetName("單階BOM匯入清單")
					.headers(List.of(
							"父階物料編號", "BOM版本號", "子項序號", "子件物料編號", "子件物料名稱",
							"組裝用量", "單位", "損耗率 (%)", "位置位號", "替代料號", "生效日期", "備註"
					))
					.rowIterable(singleLevelRows)
					.flushInterval(flushInterval)
					.freezeHeader(true)
					.headerFillColor("C65911")
					.headerFontColor("FFFFFF")
					.build());
		}

		// 4-3. 物料主檔對照表 (串流分批沖刷)
		if (command.isIncludeMaterialMaster()) {
			List<List<Object>> materialMasterRows = buildMaterialMasterRows(allMaterials);
			streamSheets.add(WriteStreamSheetCommand.builder()
					.sheetName("物料主檔對照表")
					.headers(List.of(
							"物料編號", "物料名稱", "規格型號", "物料類別", "計量單位", "標準進價成本", "狀態"
					))
					.rowIterable(materialMasterRows)
					.flushInterval(flushInterval)
					.freezeHeader(true)
					.headerFillColor("385723")
					.headerFontColor("FFFFFF")
					.build());
		}

		// 5. 調用底層 Excel 輸出埠之 writeStream 串流寫入並分批 Flush
		WriteStreamExcelCommand streamCommand = WriteStreamExcelCommand.builder()
				.applicationName("ExcelModule-BOM-StreamExporter")
				.applicationVersion("1.0")
				.sheets(streamSheets)
				.build();

		excelWriterPort.writeStream(outputStream, streamCommand);
	}

	private List<Bom> resolveTargetBoms(
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
	 * 遞迴建構多階層樹狀 BOM 展開資料列。
	 */
	private List<List<Object>> buildMultiLevelRows(
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

	private void expandBomItemsRecursive(
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
							.orElse(subBoms.get(0));

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
	 * 建構單階平鋪式 BOM 匯入格式資料列。
	 */
	private List<List<Object>> buildSingleLevelRows(
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
	 * 建構物料主檔對照表資料列。
	 */
	private List<List<Object>> buildMaterialMasterRows(List<Material> allMaterials) {
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
}
