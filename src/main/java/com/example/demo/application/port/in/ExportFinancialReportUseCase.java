package com.example.demo.application.port.in;

import com.example.demo.application.shared.command.ExportBomCostAnalysisCommand;
import com.example.demo.application.shared.command.ExportCustomReportCommand;
import com.example.demo.application.shared.command.ExportProcurementAnalysisCommand;

/**
 * 企業級財務與物料損益分析報表匯出業務合約 (Export Financial Report Use Case)
 * <p>
 * 遵循 AGENTS.md 3-8 命名規範 (Inbound Port 一律以 {@code UseCase} 結尾)。
 * 屬於 Clean Architecture 六角形架構之輸入埠 (Inbound Port)，提供表現層調用維度五企業級功能：
 * 包含儲存格合併、動態公式計算、工作表密碼防護與列印頁首頁尾合規標記。
 */
public interface ExportFinancialReportUseCase {

	/**
	 * 產製物料採購損益分析月報 (支援傳入自訂物料清單，包含表身小計公式、合計彙總跨欄合併、加總 SUM 公式、密碼鎖定與頁首頁尾)
	 *
	 * @param command 物料採購損益報表匯出命令
	 * @return Excel (.xlsx) 檔案二進位資料 byte 陣列
	 */
	byte[] exportProcurementAnalysisReport(ExportProcurementAnalysisCommand command);

	/**
	 * 產製物料採購損益分析月報 (便捷預設呼叫，使用系統擬真示範資料)
	 *
	 * @param reportTitle    報表標題 (可為 null，預設 "物料採購損益分析月報")
	 * @param password       工作表防護密碼 (可為 null，預設 "AuditSecure2026")
	 * @param classification 機密等級 (可為 null，預設 "機密資料・內部流轉")
	 * @return Excel (.xlsx) 檔案二進位資料 byte 陣列
	 */
	default byte[] exportProcurementAnalysisReport(String reportTitle, String password, String classification) {
		return exportProcurementAnalysisReport(new ExportProcurementAnalysisCommand(reportTitle, password, classification, null));
	}

	/**
	 * 產製 BOM 結構配方與標準成本卷算分析報表 (結合 Bom 與 Material 聚合根，包含單件損耗計算、有效需求用量公式、單項成本小計公式、合計彙總跨欄合併、動態 SUM 公式、密碼鎖定與頁首頁尾)
	 *
	 * @param command BOM 成本分析報表匯出命令
	 * @return Excel (.xlsx) 檔案二進位資料 byte 陣列
	 */
	byte[] exportBomCostAnalysisReport(ExportBomCostAnalysisCommand command);

	/**
	 * 產製 BOM 結構配方與標準成本卷算分析報表 (便捷查詢呼叫，依據母件料號自動查詢 Bom 與 Material 聚合根)
	 *
	 * @param parentMaterialCode 母件物料代碼 (如 MAT-FG-001)
	 * @param reportTitle        報表標題 (可為 null，預設 "產品結構配方與標準成本分析表")
	 * @param password           工作表防護密碼 (可為 null，預設 "BomAuditSecure2026")
	 * @param classification     機密等級 (可為 null，預設 "極機密・核心配方")
	 * @return Excel (.xlsx) 檔案二進位資料 byte 陣列
	 */
	default byte[] exportBomCostAnalysisReport(String parentMaterialCode, String reportTitle, String password, String classification) {
		return exportBomCostAnalysisReport(new ExportBomCostAnalysisCommand(parentMaterialCode, reportTitle, password, classification, null));
	}

	/**
	 * 產製純宣告式模型註解驅動範本 (示範 @ExcelSheet 與 @ExcelColumn 原生動態公式)
	 *
	 * @return Excel (.xlsx) 檔案二進位資料 byte 陣列
	 */
	byte[] exportDeclarativeSample();

	/**
	 * 依據呼叫方指定之客製化命令產製進階報表 (支援彈性自訂任意合併區間、公式與防護)
	 *
	 * @param command 客製化報表匯出命令
	 * @return Excel (.xlsx) 檔案二進位資料 byte 陣列
	 */
	byte[] exportCustomReport(ExportCustomReportCommand command);
}

