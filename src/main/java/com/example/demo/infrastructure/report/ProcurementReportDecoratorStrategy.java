package com.example.demo.infrastructure.report;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.example.demo.application.shared.command.WriteSheetCommand;
import com.example.demo.application.shared.vo.CellRange;
import com.example.demo.application.shared.context.ReportDecorationContext;
import com.example.demo.application.shared.vo.SheetHeaderFooterConfig;
import com.example.demo.application.shared.vo.SheetProtectionConfig;
import com.example.demo.application.shared.strategy.ExcelReportDecoratorStrategy;
import com.example.demo.application.shared.strategy.ExcelReportStrategyKeys;

/**
 * 物料採購損益月報專屬裝飾策略 (Procurement Report Decorator Strategy)
 * <p>
 * <b>【架構定位與職責規範】</b><br>
 * 位於 Clean Architecture 基礎設施層報表策略套件 ({@code infrastructure/report/})，抽離出 {@code adapter/} 套件。<br>
 * 實作應用層策略契約 {@link ExcelReportDecoratorStrategy}，綁定識別鍵 {@link ExcelReportStrategyKeys#PROCUREMENT_ANALYSIS}。<br>
 * 負責採購損益報表之微觀排版算術、合計彙總列追加、動態加總 SUM 公式注入、跨欄合併、工作表防護與企業頁首頁尾標記。
 */
@Component
public class ProcurementReportDecoratorStrategy implements ExcelReportDecoratorStrategy {

	/**
	 * 策略標準識別鍵
	 */
	public static final String STRATEGY_KEY = ExcelReportStrategyKeys.PROCUREMENT_ANALYSIS;

	@Override
	public String getDecoratorKey() {
		return STRATEGY_KEY;
	}

	@Override
	public WriteSheetCommand decorate(WriteSheetCommand baseSheet, ReportDecorationContext context) {
		Objects.requireNonNull(baseSheet, "baseSheet must not be null");
		Objects.requireNonNull(context, "context must not be null");

		int totalDataRows = (context.rawDataList() != null) ? context.rawDataList().size() : 0;
		int summaryRowIndex = totalDataRows + 1; // 0-based 列索引 (第 0 列為標題，第 1~N 列為資料，第 N+1 列為合計)
		int excelSummaryRowNumber = summaryRowIndex + 1; // 1-based Excel 列號
		int lastDataRowNumber = totalDataRows + 1;

		// 1. 追加「合計彙總列」資料列：A~D 欄將被合併，E 欄為採購總額合計公式，F 欄為空，G 欄為毛利率
		List<List<Object>> allRows = new ArrayList<>(baseSheet.rows());
		List<Object> summaryRow = Arrays.asList("合計彙總", null, null, null, null, null, null);
		allRows.add(summaryRow);

		// 2. 注入動態 SUM 公式：=SUM(E2:E{lastDataRowNumber})
		Map<String, String> formulas = new HashMap<>(baseSheet.cellFormulas());
		formulas.put("E" + excelSummaryRowNumber, "SUM(E2:E" + lastDataRowNumber + ")");

		// 3. 配置跨欄合併區間：合併合計列的 A 欄至 D 欄 (col 0 ~ 3)
		List<CellRange> mergeRanges = new ArrayList<>(baseSheet.mergedRegions());
		mergeRanges.add(CellRange.ofRow(summaryRowIndex, 0, 3));

		// 4. 配置工作表安全防護密碼與列印頁首頁尾
		SheetProtectionConfig protectionConfig = SheetProtectionConfig.ofPassword(context.password());
		SheetHeaderFooterConfig headerFooterConfig = SheetHeaderFooterConfig.enterpriseStandard(
				context.reportTitle(),
				context.classification()
		);

		// 5. 組合最終具備樣式、公式與防護之工作表命令
		return WriteSheetCommand.builder()
				.from(baseSheet)
				.rows(allRows)
				.cellFormulas(formulas)
				.mergedRegions(mergeRanges)
				.protectionConfig(protectionConfig)
				.headerFooterConfig(headerFooterConfig)
				.headerFillColor("1F4E78")
				.headerFontColor("FFFFFF")
				.freezeHeader(true)
				.build();
	}
}

