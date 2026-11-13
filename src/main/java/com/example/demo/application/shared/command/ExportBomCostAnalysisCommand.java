package com.example.demo.application.shared.command;

import java.util.List;

import com.example.demo.application.shared.model.report.BomCostAnalysisReportModel;

/**
 * 產品結構配方與標準成本卷算分析報表匯出命令 (Export BOM Cost Analysis Command)
 * <p>
 * <b>【架構定位與職責規範】</b><br>
 * 遵循 AGENTS.md 3-4 命名規範 (動詞 + 名詞 + Command)。<br>
 * 屬於 Clean Architecture 應用層不可變純資料載體 (Java 21 Record)，
 * 封裝呼叫端傳入之母件料號、自訂報表標題、解鎖密碼、機密等級以及可選之自訂物料子項清單。
 *
 * @param parentMaterialCode 母件物料料號 (如 MAT-FG-001)
 * @param reportTitle        自訂列印頁首報表大標題 (可為 null，將套用預設值)
 * @param password           工作表防篡改防護密碼 (可為 null，將套用預設值)
 * @param classification     列印頁尾機密等級標籤 (可為 null，將套用預設值)
 * @param items              自訂 BOM 成本分析明細清單 (若為 null 則自動由儲存庫查詢組裝)
 */
public record ExportBomCostAnalysisCommand(
		String parentMaterialCode,
		String reportTitle,
		String password,
		String classification,
		List<BomCostAnalysisReportModel> items
) {}

