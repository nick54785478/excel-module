package com.example.demo.presentation.assembler;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.demo.application.shared.command.ExportBomCostAnalysisCommand;
import com.example.demo.application.shared.command.ExportCustomReportCommand;
import com.example.demo.application.shared.command.ExportProcurementAnalysisCommand;
import com.example.demo.application.shared.model.report.BomCostAnalysisReportModel;
import com.example.demo.application.shared.vo.CellCoordinate;
import com.example.demo.application.shared.vo.CellRange;
import com.example.demo.application.shared.model.report.ProcurementAnalysisReportModel;
import com.example.demo.presentation.resource.in.ExportBomCostAnalysisResource;
import com.example.demo.presentation.resource.in.ExportBomCostAnalysisResource.BomCostItemResource;
import com.example.demo.presentation.resource.in.ExportCustomReportResource;
import com.example.demo.presentation.resource.in.ExportCustomReportResource.MergedRegionResource;
import com.example.demo.presentation.resource.in.ExportProcurementAnalysisResource;
import com.example.demo.presentation.resource.in.ExportProcurementAnalysisResource.ProcurementItemResource;

/**
 * 企業級報表表現層資源組裝器 (Excel Report Presentation Resource Assembler)
 * <p>
 * 遵循 AGENTS.md 規則 33~35 規範，作為表現層與應用層之間的防腐層 (ACL) 轉換中樞。
 * 負責將 HTTP 請求載體 {@link ExportCustomReportResource}、{@link ExportProcurementAnalysisResource}
 * 與 {@link ExportBomCostAnalysisResource} 轉譯為應用層命令，
 * 確保 Resource 物件嚴格作為純資料載體，不侵入任何轉換邏輯。
 */
@Component
public class ExcelReportResourceAssembler {

	/**
	 * 將表現層客製化報表匯出資源轉換為應用層命令
	 *
	 * @param resource 表現層請求資源
	 * @return 應用層匯出命令
	 */
	public ExportCustomReportCommand toCommand(ExportCustomReportResource resource) {
		if (resource == null) {
			return null;
		}

		List<CellRange> ranges = Collections.emptyList();
		if (resource.mergedRegions() != null && !resource.mergedRegions().isEmpty()) {
			ranges = resource.mergedRegions().stream()
					.map(this::toCellRange)
					.toList();
		}

		Map<String, String> sanitizedFormulas = Collections.emptyMap();
		if (resource.cellFormulas() != null && !resource.cellFormulas().isEmpty()) {
			sanitizedFormulas = new LinkedHashMap<>();
			for (Map.Entry<String, String> entry : resource.cellFormulas().entrySet()) {
				String cellRef = entry.getKey();
				String formula = entry.getValue();
				if (cellRef == null || cellRef.isBlank() || formula == null || formula.isBlank()) {
					continue;
				}
				String trimmedRef = cellRef.trim();
				// 略過 Swagger / OpenAPI 工具自動生成的 dummy 佔位屬性 (如 additionalProp1, additionalProp2)
				if (trimmedRef.startsWith("additionalProp")) {
					continue;
				}
				if (!CellCoordinate.isValid(trimmedRef)) {
					throw new IllegalArgumentException("無效的 Excel 儲存格座標格式: '" + cellRef + "'，正確格式應為英文字母加列號 (如 A1, D4, AA10，最高支援至 XFD1048576)");
				}
				sanitizedFormulas.put(trimmedRef.toUpperCase(), formula.trim());
			}
		}

		return ExportCustomReportCommand.builder()
				.sheetName(resource.sheetName())
				.headers(resource.headers())
				.rows(resource.rows())
				.mergedRegions(ranges)
				.cellFormulas(sanitizedFormulas)
				.password(resource.password())
				.reportTitle(resource.reportTitle())
				.classification(resource.classification())
				.headerFillColor(resource.headerFillColor())
				.headerFontColor(resource.headerFontColor())
				.build();
	}

	/**
	 * 將表現層物料採購損益分析資源轉換為應用層命令
	 *
	 * @param resource 表現層請求資源
	 * @return 應用層匯出命令
	 */
	public ExportProcurementAnalysisCommand toCommand(ExportProcurementAnalysisResource resource) {
		if (resource == null) {
			return null;
		}

		List<ProcurementAnalysisReportModel> models = null;
		if (resource.items() != null && !resource.items().isEmpty()) {
			models = resource.items().stream()
					.map(this::toProcurementModel)
					.toList();
		}

		return new ExportProcurementAnalysisCommand(
				resource.reportTitle(),
				resource.password(),
				resource.classification(),
				models
		);
	}

	/**
	 * 將表現層 BOM 結構配方與成本分析資源轉換為應用層命令
	 *
	 * @param resource 表現層請求資源
	 * @return 應用層匯出命令
	 */
	public ExportBomCostAnalysisCommand toCommand(ExportBomCostAnalysisResource resource) {
		if (resource == null) {
			return null;
		}

		List<BomCostAnalysisReportModel> models = null;
		if (resource.items() != null && !resource.items().isEmpty()) {
			models = resource.items().stream()
					.map(this::toBomCostModel)
					.toList();
		}

		return new ExportBomCostAnalysisCommand(
				resource.parentMaterialCode(),
				resource.reportTitle(),
				resource.password(),
				resource.classification(),
				models
		);
	}

	/**
	 * 將明細項目資源轉為宣告式 BOM 成本分析模型 (有效用量與小計由公式動態產生)
	 */
	private BomCostAnalysisReportModel toBomCostModel(BomCostItemResource item) {
		return new BomCostAnalysisReportModel(
				item.level() != null ? item.level() : 1,
				item.itemSequence(),
				item.childMaterialCode(),
				item.childMaterialName(),
				item.specification(),
				item.category(),
				item.unit(),
				item.baseQuantity(),
				item.scrapRate(),
				null, // 由 ExcelBeanAssembler 動態替換為 H{row}*(1+I{row})
				item.standardCost(),
				null, // 由 ExcelBeanAssembler 動態替換為 J{row}*K{row}
				item.positionCode(),
				item.remark()
		);
	}

	/**
	 * 將明細項目資源轉為宣告式採購損益模型 (小計與毛利率由公式動態產生)
	 */
	private ProcurementAnalysisReportModel toProcurementModel(ProcurementItemResource item) {
		return new ProcurementAnalysisReportModel(
				item.itemCode(),
				item.itemName(),
				item.quantity(),
				item.unitPrice(),
				null, // 由 ExcelBeanAssembler 動態替換為 C{row}*D{row}
				item.estimatedPrice(),
				null  // 由 ExcelBeanAssembler 動態替換為 (F{row}-D{row})/F{row}
		);
	}

	/**
	 * 將表現層合併區間資源轉為應用層 CellRange 值物件
	 */
	private CellRange toCellRange(MergedRegionResource res) {
		return new CellRange(res.firstRow(), res.lastRow(), res.firstCol(), res.lastCol());
	}
}

