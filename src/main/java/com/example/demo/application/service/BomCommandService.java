package com.example.demo.application.service;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.application.assembler.BomDtoAssembler;
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
import com.example.demo.domain.bom.aggregate.root.Bom;
import com.example.demo.domain.material.aggregate.root.Material;
import com.example.demo.domain.material.aggregate.vo.MaterialId;

/**
 * 物料清單 (BOM) 匯出命令應用服務 (BOM Export Command Service / Inbound Adapter)
 * <p>
 * 遵循 Clean Architecture 六角形架構與 CQRS 規範，作為命令端 (Command Side) 的 Inbound Adapter。
 * 命名規範符合 AGENTS.md 3-9 規範 (以 {@code CommandService} 結尾)。
 * 實作以下命令業務合約：
 * <ul>
 *     <li>{@link ExportBomUseCase}：<b>【命令式階層樹狀結構報表模組 (Imperative Multi-Level Tree Report)】</b>
 *         — 記憶體批次模式，負責多階層展開、單階平鋪與物料對照表等綜合活頁簿產製與二進位檔案匯出。</li>
 *     <li>{@link StreamExportBomUseCase}：<b>【大數據串流處理模組 (Big Data Streaming Module)】</b>
 *         — 百萬筆級別大數據串流輸出模式，配合分批 Flush 達到 O(1) 記憶體常數開銷保護。</li>
 * </ul>
 * 負責調度 {@link BomRepositoryPort}、{@link MaterialRepositoryPort} 與 {@link ExcelWriterPort}，
 * 委託 {@link BomDtoAssembler} 進行多階樹狀展開與列資料組裝。
 */
@Service
@Transactional(readOnly = true)
public class BomCommandService implements ExportBomUseCase, StreamExportBomUseCase {

	private final BomRepositoryPort bomRepositoryPort;
	private final MaterialRepositoryPort materialRepositoryPort;
	private final ExcelWriterPort excelWriterPort;

	public BomCommandService(
			BomRepositoryPort bomRepositoryPort,
			MaterialRepositoryPort materialRepositoryPort,
			ExcelWriterPort excelWriterPort
	) {
		this.bomRepositoryPort = Objects.requireNonNull(bomRepositoryPort, "bomRepositoryPort 不得為空");
		this.materialRepositoryPort = Objects.requireNonNull(materialRepositoryPort, "materialRepositoryPort 不得為空");
		this.excelWriterPort = Objects.requireNonNull(excelWriterPort, "excelWriterPort 不得為空");
	}

	// =========================================================================
	// 1. 【命令式階層樹狀結構報表模組 (Imperative Multi-Level Tree Report)】
	// =========================================================================

	/**
	 * 【命令式階層樹狀結構報表模組 (Imperative Multi-Level Tree Report)】
	 * <p>
	 * 依據匯出條件命令，動態產製物料清單 (BOM) Excel 檔案。
	 * 採用標準記憶體批次模式，負責多階層展開、單階平鋪匯入格式與物料主檔對照表等綜合活頁簿產製。
	 *
	 * @param command 匯出命令資料載體
	 * @return BOM Excel 檔案產製結果 DTO
	 */
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

		// 3. 委託 Assembler 決定目標母件與 BOM 清單及檔案名稱
		List<Bom> targetBoms = BomDtoAssembler.resolveTargetBoms(command, materialCodeMap, bomsByParentId, allBoms);
		String effectiveFileName = BomDtoAssembler.resolveFileName(command);

		// 4. 動態建構各工作表 (Sheets)
		List<WriteSheetCommand> sheets = new ArrayList<>();
		int totalRowsCount = 0;

		// 4-1. 多階層 BOM 展開清單
		if (command.isIncludeMultiLevel()) {
			List<List<Object>> multiLevelRows = BomDtoAssembler.buildMultiLevelRows(targetBoms, materialIdMap, bomsByParentId);
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
			List<List<Object>> singleLevelRows = BomDtoAssembler.buildSingleLevelRows(targetBoms, materialIdMap);
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
			List<List<Object>> materialMasterRows = BomDtoAssembler.buildMaterialMasterRows(allMaterials);
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

	// =========================================================================
	// 2. 【大數據串流處理模組 (Big Data Streaming Module)】
	// =========================================================================

	/**
	 * 【大數據串流處理模組 (Big Data Streaming Module)】
	 * <p>
	 * 百萬筆級別大數據串流模式，使用 {@link WriteStreamSheetCommand} 與底層串流管道，
	 * 依據 {@code flushInterval}（預設 1,000 筆）持續將列資料寫入磁碟暫存，
	 * 達成 O(1) 記憶體常數開銷保護，嚴防大型生產 BOM 報表導出時引發 OutOfMemoryError。
	 *
	 * @param outputStream 輸出串流 (例如 HttpServletResponse 輸出串流)
	 * @param command      匯出條件與串流沖刷間隔命令
	 */
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

		// 3. 委託 Assembler 決定目標母件與 BOM 清單
		List<Bom> targetBoms = BomDtoAssembler.resolveTargetBoms(command, materialCodeMap, bomsByParentId, allBoms);
		int flushInterval = command.getFlushInterval();

		// 4. 動態建構串流工作表清單 (WriteStreamSheetCommand)
		List<WriteStreamSheetCommand> streamSheets = new ArrayList<>();

		// 4-1. 多階層 BOM 展開清單 (串流分批沖刷)
		if (command.isIncludeMultiLevel()) {
			List<List<Object>> multiLevelRows = BomDtoAssembler.buildMultiLevelRows(targetBoms, materialIdMap, bomsByParentId);
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
			List<List<Object>> singleLevelRows = BomDtoAssembler.buildSingleLevelRows(targetBoms, materialIdMap);
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
			List<List<Object>> materialMasterRows = BomDtoAssembler.buildMaterialMasterRows(allMaterials);
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
}

