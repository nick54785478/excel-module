package com.example.demo.application.service.bom;

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

import com.example.demo.application.annotation.task.TrackExcelTask;
import com.example.demo.application.assembler.BomDtoAssembler;
import com.example.demo.application.assembler.ExcelBeanAssembler;
import com.example.demo.application.port.in.bom.ExportMaterialBomUseCase;
import com.example.demo.application.port.in.bom.QueryBomUseCase;
import com.example.demo.application.port.out.bom.BomRepositoryPort;
import com.example.demo.application.port.out.core.ExcelWriterPort;
import com.example.demo.application.port.out.bom.MaterialRepositoryPort;
import com.example.demo.application.shared.command.ExportBomCommand;
import com.example.demo.application.shared.command.WriteExcelCommand;
import com.example.demo.application.shared.command.WriteSheetCommand;
import com.example.demo.application.shared.command.WriteStreamExcelCommand;
import com.example.demo.application.shared.command.WriteStreamSheetCommand;
import com.example.demo.application.shared.dto.BomExcelGottenResult;
import com.example.demo.application.shared.vo.ExcelTaskType;
import com.example.demo.application.shared.model.bom.BomItemExcelModel;
import com.example.demo.application.shared.model.material.MaterialExcelModel;
import com.example.demo.domain.bom.aggregate.root.Bom;
import com.example.demo.domain.material.aggregate.root.Material;
import com.example.demo.domain.material.aggregate.vo.MaterialId;

/**
 * 物料與 BOM 查詢應用服務 (BOM & Material Query Service / Inbound Adapter)
 * <p>
 * 遵循 Clean Architecture 六角形架構與 CQRS 規範，作為查詢端 (Query Side) 的 Inbound Adapter。
 * 命名規範符合 AGENTS.md 3-9 規範 (以 {@code QueryService} 結尾)。
 * 實作以下查詢業務合約：
 * <ul>
 *     <li>{@link QueryBomUseCase}：
 *         <ul>
 *             <li><b>【命令式階層樹狀結構報表模組 (Imperative Multi-Level Tree Report)】</b>：多階層遞迴展開、單階平鋪與物料對照表。</li>
 *             <li><b>【大數據串流處理模組 (Big Data Streaming Module)】</b>：百萬級資料量邊產製邊傳輸至輸出串流，維持 O(1) 常數記憶體開銷。</li>
 *         </ul>
 *     </li>
 *     <li>{@link ExportMaterialBomUseCase}：
 *         <ul>
 *             <li><b>【宣告式物件映射模組 (Declarative Bean Mapping)】</b>：依據 {@code @ExcelSheet} 與 {@code @ExcelColumn} 註解，從儲存庫查詢並產製物料主檔與單階 BOM 配方 Excel。</li>
 *         </ul>
 *     </li>
 * </ul>
 * 調度 {@link BomRepositoryPort}、{@link MaterialRepositoryPort} 與 {@link ExcelWriterPort}，
 * 委託 {@link BomDtoAssembler} 進行聚合根向列資料與模型之組裝，維持應用服務簡潔並防腐。
 */
@Service
@Transactional(readOnly = true)
public class BomQueryService implements QueryBomUseCase, ExportMaterialBomUseCase {

	private final BomRepositoryPort bomRepositoryPort;
	private final MaterialRepositoryPort materialRepositoryPort;
	private final ExcelWriterPort excelWriterPort;

	public BomQueryService(
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
	 * 依據查詢條件命令，動態產製物料清單 (BOM) Excel 檔案。
	 * 採用標準記憶體批次模式，負責多階層展開、單階平鋪匯入格式與物料主檔對照表等綜合活頁簿產製。
	 *
	 * @param command 查詢條件資料載體
	 * @return BOM Excel 檔案產製結果 DTO
	 */
	@TrackExcelTask(
			type = ExcelTaskType.EXPORT,
			fileName = "#command != null ? T(com.example.demo.application.assembler.BomDtoAssembler).resolveFileName(#command) : 'BOM_EXPORT.xlsx'",
			description = "物料清單 (BOM) 批次查詢匯出長任務"
	)
	@Override
	public BomExcelGottenResult queryBom(ExportBomCommand command) {
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
	 * @param command      查詢條件與串流沖刷間隔命令
	 */
	@TrackExcelTask(
			type = ExcelTaskType.EXPORT,
			fileName = "#command != null ? T(com.example.demo.application.assembler.BomDtoAssembler).resolveFileName(#command) : 'BOM_EXPORT.xlsx'",
			description = "物料清單 (BOM) 大數據串流查詢匯出長任務"
	)
	@Override
	public void streamQueryBom(OutputStream outputStream, ExportBomCommand command) {
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

	// =========================================================================
	// 3. 【宣告式物件映射模組 (Declarative Bean Mapping)】
	// =========================================================================

	/**
	 * 【宣告式物件映射模組 (Declarative Bean Mapping)】
	 * <p>
	 * 從物料儲存庫查詢系統現有所有物料聚合根，透過 {@link BomDtoAssembler} 映射為應用層模型 {@link MaterialExcelModel}，
	 * 自動解析 {@code @ExcelSheet} 與 {@code @ExcelColumn} 註解元資料，
	 * 呼叫 {@link ExcelWriterPort} 宣告式產製物料主檔 Excel 二進位檔案。
	 *
	 * @return 物料主檔 Excel (.xlsx) 檔案二進位資料
	 */
	@Override
	public byte[] exportMaterials() {
		List<Material> materials = materialRepositoryPort.findAll();
		List<MaterialExcelModel> models = BomDtoAssembler.toMaterialExcelModels(materials);
		WriteExcelCommand writeCommand = ExcelBeanAssembler.toWriteCommand(models, MaterialExcelModel.class, null);
		return excelWriterPort.write(writeCommand);
	}

	/**
	 * 【宣告式物件映射模組 (Declarative Bean Mapping)】
	 * <p>
	 * 從 BOM 儲存庫與物料儲存庫查詢系統現有所有 BOM 配方及其關聯子料件聚合根，
	 * 透過 {@link BomDtoAssembler} 映射為應用層模型 {@link BomItemExcelModel}，
	 * 自動解析 {@code @ExcelSheet} 與 {@code @ExcelColumn} 註解元資料，
	 * 呼叫 {@link ExcelWriterPort} 宣告式產製單階 BOM 配方清單 Excel 二進位檔案。
	 *
	 * @return 單階 BOM 配方清單 Excel (.xlsx) 檔案二進位資料
	 */
	@Override
	public byte[] exportBomItems() {
		List<Bom> boms = bomRepositoryPort.findAll();
		List<Material> materials = materialRepositoryPort.findAll();
		Map<MaterialId, Material> materialMap = materials.stream()
				.collect(Collectors.toMap(Material::getId, Function.identity(), (m1, m2) -> m1));

		List<BomItemExcelModel> models = BomDtoAssembler.toBomItemExcelModels(boms, materialMap);
		WriteExcelCommand writeCommand = ExcelBeanAssembler.toWriteCommand(models, BomItemExcelModel.class, null);
		return excelWriterPort.write(writeCommand);
	}
}

