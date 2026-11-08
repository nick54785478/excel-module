package com.example.demo.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.demo.application.port.out.BomRepositoryPort;
import com.example.demo.application.port.out.ExcelReportDecoratorFactoryPort;
import com.example.demo.application.port.out.MaterialRepositoryPort;
import com.example.demo.application.shared.command.ExportBomCostAnalysisCommand;
import com.example.demo.application.shared.command.ExportCustomReportCommand;
import com.example.demo.application.shared.command.ExportProcurementAnalysisCommand;
import com.example.demo.application.shared.model.BomCostAnalysisReportModel;
import com.example.demo.application.shared.model.CellRange;
import com.example.demo.application.shared.model.ProcurementAnalysisReportModel;
import com.example.demo.application.shared.strategy.ExcelReportDecoratorStrategy;
import com.example.demo.infrastructure.adapter.engine.PoiSxssfWriterAdapter;
import com.example.demo.infrastructure.adapter.report.SpringReportDecoratorFactoryAdapter;
import com.example.demo.infrastructure.report.BomCostAnalysisReportDecoratorStrategy;
import com.example.demo.infrastructure.report.ProcurementReportDecoratorStrategy;

/**
 * 企業級進階報表應用服務測試 (ExcelReportApplicationService Test)
 * <p>
 * 針對 {@link ExcelReportApplicationService} 進行單元測試，
 * 嚴格遵循 BDD / Gherkin (Feature, Scenario, Given-When-Then) 語法註解規範。
 */
@DisplayName("ExcelReportApplicationService 企業級進階報表應用服務測試")
class ExcelReportApplicationServiceTest {

	private ExcelReportApplicationService reportService;
	private BomRepositoryPort bomRepositoryPort;
	private MaterialRepositoryPort materialRepositoryPort;

	@BeforeEach
	void setUp() {
		this.bomRepositoryPort = org.mockito.Mockito.mock(BomRepositoryPort.class);
		this.materialRepositoryPort = org.mockito.Mockito.mock(MaterialRepositoryPort.class);

		// 使用真實的 POI SXSSF 適配器與工廠適配器以驗證端到端檔案產出
		this.reportService = new ExcelReportApplicationService(
				new PoiSxssfWriterAdapter(),
				new SpringReportDecoratorFactoryAdapter(List.of(
						new ProcurementReportDecoratorStrategy(),
						new BomCostAnalysisReportDecoratorStrategy()
				)),
				bomRepositoryPort,
				materialRepositoryPort
		);
	}

	/**
	 * Feature: 物料採購損益分析月報匯出
	 * Scenario: 產製包含表身小計公式、合計彙總跨欄合併、動態加總 SUM 公式、密碼鎖定與頁首頁尾之月報
	 * Given: 指定報表大標題 "2026年11月無人機物料採購分析" 與密碼 "AuditPass888"
	 * When: 呼叫 reportService.exportProcurementAnalysisReport
	 * Then: 產出之 byte 陣列大於 0，解析驗證合併區間、公式儲存格、工作表保護與頁首頁尾
	 */
	@Test
	@DisplayName("場景：成功產製物料採購損益分析月報並驗證維度五全特性")
	void shouldExportProcurementAnalysisReportSuccessfully() throws Exception {
		// Given
		String title = "2026年11月無人機物料採購分析";
		String password = "AuditPass888";
		String classification = "極機密";

		// When
		byte[] bytes = reportService.exportProcurementAnalysisReport(title, password, classification);

		// Then
		assertThat(bytes).isNotEmpty();
		try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
			Sheet sheet = wb.getSheet("採購損益分析");
			assertThat(sheet).isNotNull();

			// 1. 驗證合計彙總跨欄合併 (第 7 列即 0-based index 6，A 到 D 欄合併)
			assertThat(sheet.getNumMergedRegions()).isGreaterThanOrEqualTo(1);
			org.apache.poi.ss.util.CellRangeAddress merged = sheet.getMergedRegion(0);
			assertThat(merged.getFirstRow()).isEqualTo(6);
			assertThat(merged.getLastRow()).isEqualTo(6);
			assertThat(merged.getFirstColumn()).isEqualTo(0);
			assertThat(merged.getLastColumn()).isEqualTo(3);

			// 2. 驗證合計彙總列文字與動態加總公式 (=SUM(E2:E6))
			Row summaryRow = sheet.getRow(6);
			assertThat(summaryRow.getCell(0).getStringCellValue()).isEqualTo("合計彙總");
			Cell sumFormulaCell = summaryRow.getCell(4);
			assertThat(sumFormulaCell.getCellType()).isEqualTo(CellType.FORMULA);
			assertThat(sumFormulaCell.getCellFormula()).isEqualTo("SUM(E2:E6)");

			// 3. 驗證表身資料動態公式 (第 2 列的小計為 C2*D2)
			Row dataRow1 = sheet.getRow(1);
			Cell formulaCell = dataRow1.getCell(4);
			assertThat(formulaCell.getCellType()).isEqualTo(CellType.FORMULA);
			assertThat(formulaCell.getCellFormula()).isEqualTo("C2*D2");

			// 4. 驗證工作表密碼防護鎖定
			assertThat(sheet.getProtect()).isTrue();

			// 5. 驗證列印頁首頁尾
			assertThat(sheet.getHeader().getCenter()).isEqualTo(title);
			assertThat(sheet.getFooter().getLeft()).contains(classification);
		}
	}

	/**
	 * Feature: 物料採購損益分析月報匯出 (自訂明細清單)
	 * Scenario: 透過 ExportProcurementAnalysisCommand 傳入自訂之物料明細產製報表
	 * Given: 建立包含 2 筆自訂物料之命令
	 * When: 呼叫 reportService.exportProcurementAnalysisReport(command)
	 * Then: 驗證產出之 Excel 包含 2 筆資料、第 3 列為跨欄合併之合計彙總列、SUM公式為 =SUM(E2:E3)
	 */
	@Test
	@DisplayName("場景：成功透過命令傳入自訂物料清單產製採購損益月報")
	void shouldExportProcurementAnalysisReportWithCustomItemsSuccessfully() throws Exception {
		// Given
		List<ProcurementAnalysisReportModel> customItems = List.of(
				new ProcurementAnalysisReportModel("MAT-CUSTOM-01", "碳纖維支架", 10, new java.math.BigDecimal("1200.00"), null, new java.math.BigDecimal("2000.00"), null),
				new ProcurementAnalysisReportModel("MAT-CUSTOM-02", "高頻天線", 20, new java.math.BigDecimal("350.00"), null, new java.math.BigDecimal("600.00"), null)
		);
		ExportProcurementAnalysisCommand command = new ExportProcurementAnalysisCommand(
				"2026年Q3客製物料採購分析",
				"LockPassword321",
				"高度機密",
				customItems
		);

		// When
		byte[] bytes = reportService.exportProcurementAnalysisReport(command);

		// Then
		assertThat(bytes).isNotEmpty();
		try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
			Sheet sheet = wb.getSheet("採購損益分析");
			assertThat(sheet).isNotNull();

			// 驗證自訂資料列數為 2 筆，合計列為第 3 列 (0-based index 3，即第 4 列)
			assertThat(sheet.getNumMergedRegions()).isGreaterThanOrEqualTo(1);
			org.apache.poi.ss.util.CellRangeAddress merged = sheet.getMergedRegion(0);
			assertThat(merged.getFirstRow()).isEqualTo(3);
			assertThat(merged.getLastRow()).isEqualTo(3);
			assertThat(merged.getFirstColumn()).isEqualTo(0);
			assertThat(merged.getLastColumn()).isEqualTo(3);

			// 驗證合計彙總列之動態加總公式為 =SUM(E2:E3)
			Row summaryRow = sheet.getRow(3);
			assertThat(summaryRow.getCell(0).getStringCellValue()).isEqualTo("合計彙總");
			Cell sumFormulaCell = summaryRow.getCell(4);
			assertThat(sumFormulaCell.getCellType()).isEqualTo(CellType.FORMULA);
			assertThat(sumFormulaCell.getCellFormula()).isEqualTo("SUM(E2:E3)");

			// 驗證工作表密碼保護
			assertThat(sheet.getProtect()).isTrue();
		}
	}

	/**
	 * Feature: Application Service 與 Decorator Factory Port 調度協同
	 * Scenario: 驗證 Application Service 正確透過 Factory Port 依 Key 取得策略並委派裝飾
	 * Given: Mock 的 ExcelWriterPort 與 ExcelReportDecoratorFactoryPort、ExcelReportDecoratorStrategy
	 * When: 呼叫 reportService.exportProcurementAnalysisReport
	 * Then: 驗證 factory.getDecorator("PROCUREMENT_ANALYSIS") 與 strategy.decorate 被依序精確呼叫
	 */
	@Test
	@DisplayName("場景：驗證 Application Service 正確調度策略工廠與寫入引擎")
	void shouldOrchestrateDecoratorPortAndWriterPortCorrectly() {
		// Given
		com.example.demo.application.port.out.ExcelWriterPort mockWriter = org.mockito.Mockito.mock(com.example.demo.application.port.out.ExcelWriterPort.class);
		ExcelReportDecoratorFactoryPort mockFactory = org.mockito.Mockito.mock(ExcelReportDecoratorFactoryPort.class);
		ExcelReportDecoratorStrategy mockStrategy = org.mockito.Mockito.mock(ExcelReportDecoratorStrategy.class);
		ExcelReportApplicationService service = new ExcelReportApplicationService(mockWriter, mockFactory, bomRepositoryPort, materialRepositoryPort);

		byte[] expectedBytes = new byte[]{1, 2, 3};
		com.example.demo.application.shared.command.WriteSheetCommand decoratedSheet = com.example.demo.application.shared.command.WriteSheetCommand.builder()
				.sheetName("裝飾完成工作表")
				.headers(List.of("A", "B"))
				.rows(List.of(List.of(1, 2)))
				.build();

		org.mockito.Mockito.when(mockFactory.getDecorator(ProcurementReportDecoratorStrategy.STRATEGY_KEY))
				.thenReturn(mockStrategy);

		org.mockito.Mockito.when(mockStrategy.decorate(
				org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.any()
		)).thenReturn(decoratedSheet);

		org.mockito.Mockito.when(mockWriter.write(org.mockito.ArgumentMatchers.any()))
				.thenReturn(expectedBytes);

		// When
		byte[] result = service.exportProcurementAnalysisReport("標題", "Pass", "等級");

		// Then
		assertThat(result).isEqualTo(expectedBytes);
		org.mockito.Mockito.verify(mockFactory).getDecorator(ProcurementReportDecoratorStrategy.STRATEGY_KEY);
		org.mockito.Mockito.verify(mockStrategy).decorate(
				org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.argThat(ctx ->
						ProcurementReportDecoratorStrategy.STRATEGY_KEY.equals(ctx.reportKey()) &&
						"標題".equals(ctx.reportTitle()) &&
						"Pass".equals(ctx.password()) &&
						"等級".equals(ctx.classification())
				)
		);
		org.mockito.Mockito.verify(mockWriter).write(org.mockito.ArgumentMatchers.argThat(cmd ->
				cmd.sheets().size() == 1 && "裝飾完成工作表".equals(cmd.sheets().get(0).sheetName())
		));
	}

	/**
	 * Feature: 純宣告式模型註解驅動範本匯出
	 * Scenario: 透過 @ExcelSheet 與 @ExcelColumn 宣告式動態公式產製範本
	 * Given: 呼叫 reportService.exportDeclarativeSample()
	 * When: 產製二進位 Excel 檔案
	 * Then: 驗證產出工作表包含自動動態替換後的欄位公式與工作表防護
	 */
	@Test
	@DisplayName("場景：成功產製純宣告式動態公式範本")
	void shouldExportDeclarativeSampleSuccessfully() throws Exception {
		// When
		byte[] bytes = reportService.exportDeclarativeSample();

		// Then
		assertThat(bytes).isNotEmpty();
		try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
			Sheet sheet = wb.getSheet("採購損益分析");
			assertThat(sheet).isNotNull();
			assertThat(sheet.getProtect()).isTrue();

			Row row2 = sheet.getRow(1);
			assertThat(row2.getCell(4).getCellType()).isEqualTo(CellType.FORMULA);
			assertThat(row2.getCell(4).getCellFormula()).isEqualTo("C2*D2");
		}
	}

	/**
	 * Feature: 客製化進階報表匯出
	 * Scenario: 依據 ExportCustomReportCommand 產製任意合併區間與自訂公式之報表
	 * Given: 建立包含自訂合併、公式與防護之命令
	 * When: 呼叫 reportService.exportCustomReport(command)
	 * Then: 驗證自訂報表生成正確性
	 */
	@Test
	@DisplayName("場景：成功依據客製化命令匯出進階報表")
	void shouldExportCustomReportSuccessfully() throws Exception {
		// Given
		ExportCustomReportCommand command = ExportCustomReportCommand.builder()
				.sheetName("自訂財務報表")
				.headers(List.of("科目", "第一季", "第二季", "合計"))
				.rows(List.of(
						List.of("營業收入", 1500000, 1800000, "=B2+C2"),
						List.of("營業成本", 900000, 1100000, "=B3+C3"),
						java.util.Arrays.asList("營業利益", null, null, "=D2-D3")
				))
				.mergedRegions(List.of(CellRange.ofRow(3, 0, 2)))
				.cellFormulas(Map.of("D4", "D2-D3"))
				.password("FinancePass2026")
				.reportTitle("2026年度上半年財務損益表")
				.classification("絕密")
				.build();

		// When
		byte[] bytes = reportService.exportCustomReport(command);

		// Then
		assertThat(bytes).isNotEmpty();
		try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
			Sheet sheet = wb.getSheet("自訂財務報表");
			assertThat(sheet).isNotNull();
			assertThat(sheet.getProtect()).isTrue();
			assertThat(sheet.getNumMergedRegions()).isEqualTo(1);
			assertThat(sheet.getRow(3).getCell(3).getCellFormula()).isEqualTo("D2-D3");
		}
	}

	/**
	 * Feature: BOM 產品結構配方與標準成本卷算分析報表匯出
	 * Scenario: 產製包含表身有效用量動態公式、單項成本小計動態公式、A~K欄合計彙總跨欄合併、動態加總 SUM 公式、密碼鎖定與頁首頁尾之分析表
	 * Given: 指定母件料號 "MAT-FG-001"、報表大標題 "2026年Q4產品結構與成本分析表"、密碼 "BomAuditSecure888"、機密等級 "極機密"
	 * When: 呼叫 reportService.exportBomCostAnalysisReport
	 * Then: 產出之 byte 陣列大於 0，解析驗證合併區間、公式儲存格、工作表保護與頁首頁尾
	 */
	@Test
	@DisplayName("場景：成功產製 BOM 成本分析報表並驗證動態公式、合計跨欄合併與工作表安全防護")
	void shouldExportBomCostAnalysisReportSuccessfully() throws Exception {
		// Given
		String parentMaterialCode = "MAT-FG-001";
		String title = "2026年Q4產品結構與成本分析表";
		String password = "BomAuditSecure888";
		String classification = "極機密・核心配方";

		// When
		byte[] bytes = reportService.exportBomCostAnalysisReport(parentMaterialCode, title, password, classification);

		// Then
		assertThat(bytes).isNotEmpty();
		try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
			Sheet sheet = wb.getSheet("BOM成本分析");
			assertThat(sheet).isNotNull();

			// 1. 驗證合計彙總跨欄合併 (8 筆資料 + 1 筆標題，合計列位於 index 9 即第 10 列，跨 A 到 K 欄即 col 0 到 10)
			assertThat(sheet.getNumMergedRegions()).isGreaterThanOrEqualTo(1);
			org.apache.poi.ss.util.CellRangeAddress merged = sheet.getMergedRegion(0);
			assertThat(merged.getFirstRow()).isEqualTo(9);
			assertThat(merged.getLastRow()).isEqualTo(9);
			assertThat(merged.getFirstColumn()).isEqualTo(0);
			assertThat(merged.getLastColumn()).isEqualTo(10);

			// 2. 驗證合計彙總列文字與動態加總公式 (=SUM(L2:L9))
			Row summaryRow = sheet.getRow(9);
			assertThat(summaryRow.getCell(0).getStringCellValue()).contains("【整機 BOM 標準總成本合計】");
			Cell sumFormulaCell = summaryRow.getCell(11); // L 欄 (col 11)
			assertThat(sumFormulaCell.getCellType()).isEqualTo(CellType.FORMULA);
			assertThat(sumFormulaCell.getCellFormula()).isEqualTo("SUM(L2:L9)");

			// 3. 驗證表身資料動態公式 (第 2 列：有效需求用量為 H2*(1+I2)，成本小計為 J2*K2)
			Row dataRow1 = sheet.getRow(1);
			Cell effectiveQtyCell = dataRow1.getCell(9); // J 欄 (col 9)
			assertThat(effectiveQtyCell.getCellType()).isEqualTo(CellType.FORMULA);
			assertThat(effectiveQtyCell.getCellFormula()).isEqualTo("H2*(1+I2)");

			Cell subtotalCostCell = dataRow1.getCell(11); // L 欄 (col 11)
			assertThat(subtotalCostCell.getCellType()).isEqualTo(CellType.FORMULA);
			assertThat(subtotalCostCell.getCellFormula()).isEqualTo("J2*K2");

			// 4. 驗證工作表密碼防護鎖定
			assertThat(sheet.getProtect()).isTrue();

			// 5. 驗證列印頁首頁尾
			assertThat(sheet.getHeader().getCenter()).isEqualTo(title);
			assertThat(sheet.getFooter().getRight()).contains(classification);
		}
	}
}

