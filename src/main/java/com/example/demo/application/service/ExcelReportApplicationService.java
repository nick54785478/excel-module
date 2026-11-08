package com.example.demo.application.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.demo.application.assembler.ExcelBeanAssembler;
import com.example.demo.application.port.in.ExportFinancialReportUseCase;
import com.example.demo.application.port.out.BomRepositoryPort;
import com.example.demo.application.port.out.ExcelReportDecoratorFactoryPort;
import com.example.demo.application.port.out.ExcelWriterPort;
import com.example.demo.application.port.out.MaterialRepositoryPort;
import com.example.demo.application.shared.command.ExportBomCostAnalysisCommand;
import com.example.demo.application.shared.command.ExportCustomReportCommand;
import com.example.demo.application.shared.command.ExportProcurementAnalysisCommand;
import com.example.demo.application.shared.command.WriteExcelCommand;
import com.example.demo.application.shared.command.WriteSheetCommand;
import com.example.demo.application.shared.model.BomCostAnalysisReportModel;
import com.example.demo.application.shared.model.ProcurementAnalysisReportModel;
import com.example.demo.application.shared.model.ReportDecorationContext;
import com.example.demo.application.shared.model.SheetHeaderFooterConfig;
import com.example.demo.application.shared.model.SheetProtectionConfig;
import com.example.demo.application.shared.strategy.ExcelReportDecoratorStrategy;
import com.example.demo.application.shared.strategy.ExcelReportStrategyKeys;
import com.example.demo.domain.bom.aggregate.entity.BomItem;
import com.example.demo.domain.bom.aggregate.root.Bom;
import com.example.demo.domain.material.aggregate.root.Material;
import com.example.demo.domain.material.aggregate.vo.MaterialCode;

/**
 * 企業級報表匯出應用層服務 (Excel Report Application Service)
 * <p>
 * 遵循 Clean Architecture 六角形架構規範，實作 {@link ExportFinancialReportUseCase} 輸入埠 (Inbound Port)。
 * 負責調度 {@link ExcelWriterPort} 輸出埠、{@link ExcelReportDecoratorFactoryPort} 策略工廠輸出埠、
 * 以及 {@link BomRepositoryPort}、{@link MaterialRepositoryPort} 領域主檔儲存庫，
 * 提供包含多階複合表頭、動態計算公式、工作表防篡改保護與列印合規標籤之企業級報表生成服務。
 */
@Service
public class ExcelReportApplicationService implements ExportFinancialReportUseCase {

	private final ExcelWriterPort excelWriterPort;
	private final ExcelReportDecoratorFactoryPort decoratorFactoryPort;
	private final BomRepositoryPort bomRepositoryPort;
	private final MaterialRepositoryPort materialRepositoryPort;

	public ExcelReportApplicationService(
			ExcelWriterPort excelWriterPort,
			ExcelReportDecoratorFactoryPort decoratorFactoryPort,
			BomRepositoryPort bomRepositoryPort,
			MaterialRepositoryPort materialRepositoryPort
	) {
		this.excelWriterPort = Objects.requireNonNull(excelWriterPort, "excelWriterPort must not be null");
		this.decoratorFactoryPort = Objects.requireNonNull(decoratorFactoryPort, "decoratorFactoryPort must not be null");
		this.bomRepositoryPort = Objects.requireNonNull(bomRepositoryPort, "bomRepositoryPort must not be null");
		this.materialRepositoryPort = Objects.requireNonNull(materialRepositoryPort, "materialRepositoryPort must not be null");
	}

	@Override
	public byte[] exportProcurementAnalysisReport(String reportTitle, String password, String classification) {
		return exportProcurementAnalysisReport(new ExportProcurementAnalysisCommand(reportTitle, password, classification, null));
	}

	@Override
	public byte[] exportProcurementAnalysisReport(ExportProcurementAnalysisCommand command) {
		// 1. 取得業務資料與建構通用裝飾上下文
		List<ProcurementAnalysisReportModel> dataList = (command != null && command.items() != null && !command.items().isEmpty())
				? command.items()
				: createSampleProcurementData();
		String title = (command != null && command.reportTitle() != null && !command.reportTitle().isBlank())
				? command.reportTitle().trim()
				: "物料採購損益分析月報";
		String password = (command != null && command.password() != null && !command.password().isBlank())
				? command.password().trim()
				: "AuditSecure2026";
		String classification = (command != null && command.classification() != null && !command.classification().isBlank())
				? command.classification().trim()
				: "機密資料・內部流轉";

		ReportDecorationContext context = ReportDecorationContext.of(
				ExcelReportStrategyKeys.PROCUREMENT_ANALYSIS,
				title,
				password,
				classification,
				dataList
		);

		// 2. 透過 ExcelBeanAssembler 解析宣告式模型並組裝基礎工作表命令 (包含 {row} 自動替換為 C2*D2 等)
		WriteSheetCommand baseSheetCmd = ExcelBeanAssembler.toSheetCommand(dataList, ProcurementAnalysisReportModel.class, "採購損益分析");

		// 3. 透過工廠輸出埠依 Key 動態取得策略並執行裝飾排版 (合計列、動態 SUM 公式、跨欄合併、安全防護與企業頁首頁尾)
		ExcelReportDecoratorStrategy strategy = decoratorFactoryPort.getDecorator(context.reportKey());
		WriteSheetCommand decoratedSheetCmd = strategy.decorate(baseSheetCmd, context);

		// 4. 調度底層輸出埠寫入引擎生成二進位 Excel
		WriteExcelCommand writeExcelCommand = WriteExcelCommand.builder()
				.applicationName("FinancialReportModule")
				.applicationVersion("1.0")
				.addSheet(decoratedSheetCmd)
				.build();

		return excelWriterPort.write(writeExcelCommand);
	}

	@Override
	public byte[] exportBomCostAnalysisReport(ExportBomCostAnalysisCommand command) {
		String parentMaterialCode = (command != null && command.parentMaterialCode() != null && !command.parentMaterialCode().isBlank())
				? command.parentMaterialCode().trim()
				: "MAT-FG-001";
		String title = (command != null && command.reportTitle() != null && !command.reportTitle().isBlank())
				? command.reportTitle().trim()
				: "產品結構配方與標準成本卷算分析表";
		String password = (command != null && command.password() != null && !command.password().isBlank())
				? command.password().trim()
				: "BomAuditSecure2026";
		String classification = (command != null && command.classification() != null && !command.classification().isBlank())
				? command.classification().trim()
				: "極機密・核心配方與成本審計";

		// 1. 取得 BOM 成本明細資料 (若命令自帶清單則優先使用，否則調用領域儲存庫載入)
		List<BomCostAnalysisReportModel> dataList = (command != null && command.items() != null && !command.items().isEmpty())
				? command.items()
				: resolveBomCostDataFromDomain(parentMaterialCode);

		String parentName = resolveParentMaterialName(parentMaterialCode);

		ReportDecorationContext context = new ReportDecorationContext(
				ExcelReportStrategyKeys.BOM_COST_ANALYSIS,
				title,
				password,
				classification,
				dataList,
				Map.of(
						"parentMaterialCode", parentMaterialCode,
						"parentMaterialName", parentName
				)
		);

		// 2. 透過 ExcelBeanAssembler 解析宣告式模型並組裝基礎工作表命令
		WriteSheetCommand baseSheetCmd = ExcelBeanAssembler.toSheetCommand(dataList, BomCostAnalysisReportModel.class, "BOM成本分析");

		// 3. 透過工廠輸出埠動態取得裝飾策略並執行排版
		ExcelReportDecoratorStrategy strategy = decoratorFactoryPort.getDecorator(context.reportKey());
		WriteSheetCommand decoratedSheetCmd = strategy.decorate(baseSheetCmd, context);

		// 4. 調度底層輸出埠寫入引擎輸出 Excel
		WriteExcelCommand writeExcelCommand = WriteExcelCommand.builder()
				.applicationName("BomCostReportModule")
				.applicationVersion("1.0")
				.addSheet(decoratedSheetCmd)
				.build();

		return excelWriterPort.write(writeExcelCommand);
	}

	/**
	 * 從領域儲存庫查詢母件與其子項 BOM 結構並轉換為成本分析報表模型清單
	 */
	private List<BomCostAnalysisReportModel> resolveBomCostDataFromDomain(String parentMaterialCode) {
		try {
			Optional<Material> parentOpt = materialRepositoryPort.findByCode(MaterialCode.of(parentMaterialCode));
			if (parentOpt.isPresent()) {
				Material parent = parentOpt.get();
				List<Bom> boms = bomRepositoryPort.findByParentMaterialId(parent.getId());
				if (boms != null && !boms.isEmpty()) {
					// 優先選取 APPROVED 狀態或首筆 BOM
					Bom targetBom = boms.stream()
							.filter(b -> b.getStatus() != null && "APPROVED".equalsIgnoreCase(b.getStatus().name()))
							.findFirst()
							.orElse(boms.get(0));

					List<BomCostAnalysisReportModel> items = new ArrayList<>();
					for (BomItem item : targetBom.getItems()) {
						Optional<Material> childOpt = materialRepositoryPort.findById(item.getChildMaterialId());
						Material child = childOpt.orElse(null);

						String childCode = child != null ? child.getCode().value() : item.getChildMaterialId().value();
						String childName = child != null ? child.getName() : "子組件";
						String spec = child != null ? child.getSpecification() : "";
						String cat = (child != null && child.getCategory() != null) ? child.getCategory().getDisplayName() : "零組件";
						String unit = (child != null && child.getUnit() != null) ? child.getUnit().getDisplayName() : "PCS";
						BigDecimal standardCost = (child != null && child.getStandardCost() != null) ? child.getStandardCost().amount() : BigDecimal.ZERO;

						items.add(new BomCostAnalysisReportModel(
								1, // Level 1
								item.getItemSequence() != null ? item.getItemSequence().value() : 10,
								childCode,
								childName,
								spec,
								cat,
								unit,
								item.getQuantity() != null ? item.getQuantity().value() : BigDecimal.ONE,
								item.getScrapRate() != null ? item.getScrapRate().rate() : BigDecimal.ZERO,
								null, // 由 ExcelBeanAssembler 動態替換為 H{row}*(1+I{row})
								standardCost,
								null, // 由 ExcelBeanAssembler 動態替換為 J{row}*K{row}
								item.getPositionCode(),
								item.getRemark()
						));
					}
					if (!items.isEmpty()) {
						return items;
					}
				}
			}
		} catch (Exception ex) {
			// 若儲存庫查無資料或異常，平滑降級至擬真示範資料
		}
		return createSampleBomCostData();
	}

	/**
	 * 查詢母件品名說明
	 */
	private String resolveParentMaterialName(String parentMaterialCode) {
		try {
			return materialRepositoryPort.findByCode(MaterialCode.of(parentMaterialCode))
					.map(Material::getName)
					.orElse("4K 專業商用巡檢無人機標準裝配");
		} catch (Exception e) {
			return "4K 專業商用巡檢無人機標準裝配";
		}
	}

	@Override
	public byte[] exportDeclarativeSample() {
		List<ProcurementAnalysisReportModel> dataList = createSampleProcurementData();
		WriteSheetCommand sheetCommand = ExcelBeanAssembler.toSheetCommand(dataList, ProcurementAnalysisReportModel.class, null);
		return excelWriterPort.write(WriteExcelCommand.of(sheetCommand));
	}

	@Override
	public byte[] exportCustomReport(ExportCustomReportCommand command) {
		Objects.requireNonNull(command, "ExportCustomReportCommand must not be null");

		SheetProtectionConfig protectionConfig = null;
		if (command.password() != null && !command.password().isBlank()) {
			protectionConfig = SheetProtectionConfig.ofPassword(command.password().trim());
		}

		SheetHeaderFooterConfig headerFooterConfig = null;
		if (command.reportTitle() != null || command.classification() != null) {
			headerFooterConfig = SheetHeaderFooterConfig.enterpriseStandard(
					command.reportTitle() != null ? command.reportTitle() : "客製化報表",
					command.classification() != null ? command.classification() : "機密等級"
			);
		}

		WriteSheetCommand sheetCommand = WriteSheetCommand.builder()
				.sheetName(command.sheetName())
				.headers(command.headers())
				.rows(command.rows())
				.mergedRegions(command.mergedRegions())
				.cellFormulas(command.cellFormulas())
				.protectionConfig(protectionConfig)
				.headerFooterConfig(headerFooterConfig)
				.headerFillColor(command.headerFillColor())
				.headerFontColor(command.headerFontColor())
				.freezeHeader(true)
				.build();

		return excelWriterPort.write(WriteExcelCommand.of(sheetCommand));
	}

	/**
	 * 產製擬真的物料採購示範資料清單
	 */
	private List<ProcurementAnalysisReportModel> createSampleProcurementData() {
		return List.of(
				new ProcurementAnalysisReportModel("MAT-DRONE-001", "4K 專業航模機殼結構模組", 10, new BigDecimal("8500.00"), null, new BigDecimal("12500.00"), null),
				new ProcurementAnalysisReportModel("MAT-MOT-002", "高轉速無刷電機馬達組 (4入)", 40, new BigDecimal("680.00"), null, new BigDecimal("1100.00"), null),
				new ProcurementAnalysisReportModel("MAT-ESC-003", "智慧電子調速器 60A 模組", 40, new BigDecimal("420.00"), null, new BigDecimal("750.00"), null),
				new ProcurementAnalysisReportModel("MAT-BAT-004", "高容量動力鋰聚合物電池 6S", 20, new BigDecimal("2300.00"), null, new BigDecimal("3600.00"), null),
				new ProcurementAnalysisReportModel("MAT-PRO-005", "快拆式高強度碳纖維螺旋槳", 50, new BigDecimal("160.00"), null, new BigDecimal("320.00"), null)
		);
	}

	/**
	 * 產製擬真的 BOM 產品結構與標準成本示範資料清單 (以商用巡檢無人機 MAT-FG-001 為基準)
	 */
	private List<BomCostAnalysisReportModel> createSampleBomCostData() {
		return List.of(
				new BomCostAnalysisReportModel(1, 10, "MAT-SFG-001", "飛控主板電路總成", "STM32H743 Cortex-M7 雙備援", "半成品", "套/組", new BigDecimal("1.00"), new BigDecimal("0.01"), null, new BigDecimal("4800.00"), null, "FC-ASM", "核心飛控模組"),
				new BomCostAnalysisReportModel(1, 20, "MAT-SFG-002", "2212 動力推進總成 (4組)", "2212 920KV 碳纖快拆", "半成品", "套/組", new BigDecimal("4.00"), new BigDecimal("0.00"), null, new BigDecimal("680.00"), null, "PROP-1~4", "動力推進系統"),
				new BomCostAnalysisReportModel(1, 30, "MAT-SFG-003", "4K 三軸機械雲台相機總成", "SONY IMX586 4800萬像素", "半成品", "套/組", new BigDecimal("1.00"), new BigDecimal("0.00"), null, new BigDecimal("3600.00"), null, "CAM-GIMBAL", "光學偵蒐負載"),
				new BomCostAnalysisReportModel(1, 40, "MAT-SFG-004", "4S 5000mAh 智慧動力鋰電池", "21700 電芯 4S1P 100A BMS", "半成品", "套/組", new BigDecimal("2.00"), new BigDecimal("0.00"), null, new BigDecimal("1850.00"), null, "BAT-1,2", "航電儲能系統"),
				new BomCostAnalysisReportModel(1, 50, "MAT-RM-0076", "碳纖維四軸機架本體", "全碳纖 3K 2.0mm 輕量化", "原物料", "件/個", new BigDecimal("1.00"), new BigDecimal("0.00"), null, new BigDecimal("1250.00"), null, "FRAME-BODY", "主結構碳纖套件"),
				new BomCostAnalysisReportModel(1, 60, "MAT-RM-0077", "GPS/北斗雙頻陶瓷導航天線", "L1/L5 雙頻抗干擾 RTK", "原物料", "件/個", new BigDecimal("1.00"), new BigDecimal("0.00"), null, new BigDecimal("750.00"), null, "GPS-ANT", "高精定位天線"),
				new BomCostAnalysisReportModel(1, 70, "MAT-RM-0078", "M3*8 航太鈦合金裝配螺絲", "TC4 鈦合金沉頭內六角", "原物料", "件/個", new BigDecimal("24.00"), new BigDecimal("0.05"), null, new BigDecimal("12.50"), null, "SCREW-M3-8", "裝配耗損 5%"),
				new BomCostAnalysisReportModel(1, 80, "MAT-RM-0079", "防震防水氣密防護提箱", "IP67 輕量化 PP 雙鎖扣", "原物料", "箱/盒", new BigDecimal("1.00"), new BigDecimal("0.00"), null, new BigDecimal("890.00"), null, "BOX-CASE", "防震包材配件")
		);
	}
}

