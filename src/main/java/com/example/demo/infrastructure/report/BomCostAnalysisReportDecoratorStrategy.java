package com.example.demo.infrastructure.report;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.example.demo.application.shared.command.WriteSheetCommand;
import com.example.demo.application.shared.model.CellRange;
import com.example.demo.application.shared.model.ReportDecorationContext;
import com.example.demo.application.shared.model.SheetHeaderFooterConfig;
import com.example.demo.application.shared.model.SheetProtectionConfig;
import com.example.demo.application.shared.strategy.ExcelReportDecoratorStrategy;
import com.example.demo.application.shared.strategy.ExcelReportStrategyKeys;

/**
 * 產品結構配方與標準成本分析專屬裝飾策略 (BOM Cost Analysis Report Decorator Strategy)
 * <p>
 * <b>【架構定位與職責規範】</b><br>
 * 位於 Clean Architecture 基礎設施層報表策略套件 ({@code infrastructure/report/})，抽離出 {@code adapter/} 套件。<br>
 * 實作應用層策略契約 {@link ExcelReportDecoratorStrategy}，綁定識別鍵 {@link ExcelReportStrategyKeys#BOM_COST_ANALYSIS}。<br>
 * 負責 BOM 成本分析之微觀排版算術、合計彙總列追加、A~K 欄大跨度跨欄合併、L 欄動態加總 SUM 公式、
 * 商務深海軍藍 (#1B365D) 配色、工作表密碼防護與審計頁首頁尾標記。
 */
@Component
public class BomCostAnalysisReportDecoratorStrategy implements ExcelReportDecoratorStrategy {

	/**
	 * BOM 成本分析報表標準識別鍵
	 */
	public static final String STRATEGY_KEY = ExcelReportStrategyKeys.BOM_COST_ANALYSIS;

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
		int lastDataRowNumber = Math.max(totalDataRows + 1, 2);

		// 1. 組裝母件資訊與「合計彙總列」資料列：A~K 欄 (col 0~10) 將被合併，L 欄為成本總計公式，M~N 欄為空
		String parentInfo = "";
		if (context.extraParameters() != null && context.extraParameters().containsKey("parentMaterialCode")) {
			Object code = context.extraParameters().get("parentMaterialCode");
			Object name = context.extraParameters().get("parentMaterialName");
			parentInfo = " (母件：" + code + (name != null && !name.toString().isBlank() ? " - " + name : "") + ")";
		}
		String summaryText = "【整機 BOM 標準總成本合計】" + parentInfo;

		List<List<Object>> allRows = new ArrayList<>(baseSheet.rows());
		List<Object> summaryRow = Arrays.asList(
				summaryText, null, null, null, null, null, null, null, null, null, null, // col 0~10 (A~K)
				null, // col 11 (L 欄：將由動態公式填充)
				null, null // col 12~13 (M~N 欄)
		);
		allRows.add(summaryRow);

		// 2. 注入動態 SUM 公式：=SUM(L2:L{lastDataRowNumber})
		Map<String, String> formulas = new HashMap<>(baseSheet.cellFormulas());
		String sumFormula = (totalDataRows > 0)
				? "SUM(L2:L" + lastDataRowNumber + ")"
				: "0.00";
		formulas.put("L" + excelSummaryRowNumber, sumFormula);

		// 3. 配置跨欄合併區間：合併合計列的 A 欄至 K 欄 (col 0 ~ 10)
		List<CellRange> mergeRanges = new ArrayList<>(baseSheet.mergedRegions());
		mergeRanges.add(CellRange.ofRow(summaryRowIndex, 0, 10));

		// 4. 配置工作表安全防護密碼與列印頁首頁尾
		String effectivePassword = (context.password() != null && !context.password().isBlank())
				? context.password().trim()
				: "BomAuditSecure2026";
		SheetProtectionConfig protectionConfig = SheetProtectionConfig.ofPassword(effectivePassword);

		SheetHeaderFooterConfig headerFooterConfig = new SheetHeaderFooterConfig(
				"【機密審計・嚴禁外流】",
				context.reportTitle(),
				"產製時間：&D &T",
				"智慧製造與成本工程系統",
				"第 &P 頁 / 共 &N 頁",
				"【" + context.classification() + "】"
		);

		// 5. 組合最終具備樣式、公式與防護之工作表命令
		return WriteSheetCommand.builder()
				.from(baseSheet)
				.rows(allRows)
				.cellFormulas(formulas)
				.mergedRegions(mergeRanges)
				.protectionConfig(protectionConfig)
				.headerFooterConfig(headerFooterConfig)
				.headerFillColor("1B365D") // 商務深海軍藍 (Deep Navy)
				.headerFontColor("FFFFFF") // 白字粗體
				.freezeHeader(true)        // 凍結首列
				.build();
	}
}

