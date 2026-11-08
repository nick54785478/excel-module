package com.example.demo.application.shared.command;

import java.util.List;

import com.example.demo.application.shared.model.ProcurementAnalysisReportModel;

/**
 * 物料採購損益分析報表匯出命令 (Export Procurement Analysis Command)
 * <p>
 * 遵循 AGENTS.md 3-4 命名規範 ({@code V + N + Command})。
 * 純 Java 21 Record，作為應用層匯出物料採購損益報表的完整參數載體。
 *
 * @param reportTitle    自訂列印頁首報表標題
 * @param password       工作表防護解鎖密碼
 * @param classification 列印頁尾機密等級標籤
 * @param items          物料採購損益模型明細清單 (若為 null 或空清單，則應用服務自動使用預設示範資料)
 */
public record ExportProcurementAnalysisCommand(
		String reportTitle,
		String password,
		String classification,
		List<ProcurementAnalysisReportModel> items
) {

	/**
	 * 便捷靜態工廠方法：建立不帶自訂明細清單之命令
	 *
	 * @param reportTitle    報表標題
	 * @param password       防護密碼
	 * @param classification 機密等級標籤
	 * @return 匯出命令實例
	 */
	public static ExportProcurementAnalysisCommand of(String reportTitle, String password, String classification) {
		return new ExportProcurementAnalysisCommand(reportTitle, password, classification, null);
	}
}

