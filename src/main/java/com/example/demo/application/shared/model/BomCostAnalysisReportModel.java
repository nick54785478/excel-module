package com.example.demo.application.shared.model;

import java.math.BigDecimal;

import com.example.demo.application.annotation.mapping.ExcelColumn;
import com.example.demo.application.annotation.mapping.ExcelSheet;

/**
 * BOM 結構配方與標準成本卷算分析報表宣告式模型 (BOM Structure & Standard Cost Rollup Declarative Report Model)
 * <p>
 * <b>【架構定位與職責規範】</b><br>
 * 遵循 Clean Architecture 應用層資料載體規範，採用 Java 21 Record 實作不可變 (Immutable) 宣告式報表模型。<br>
 * 結合 {@link ExcelSheet} 與 {@link ExcelColumn} 註解，完整展現維度五企業級功能：
 * <ul>
 *   <li><b>表身原生計算公式 (Dynamic Formulas)</b>：
 *     <ul>
 *       <li>需求用量公式：{@code formula = "H{row}*(1+I{row})"}，依據基準用量與損耗率動態展開。</li>
 *       <li>單項成本小計公式：{@code formula = "J{row}*K{row}"}，依據有效需求用量乘上標準單價即時卷算。</li>
 *     </ul>
 *   </li>
 *   <li><b>工作表防護 (Sheet Protection)</b>：透過 {@code protect = true} 與自訂密碼鎖定，防止儲存格與公式被竄改。</li>
 *   <li><b>企業標準頁首頁尾 (Header & Footer)</b>：內建機密審計標籤與動態頁碼 ({@code &P / &N})。</li>
 * </ul>
 *
 * @param level             結構階層 (Level 1 為母件直接子項，Level 2 為次階零組件)
 * @param itemSequence      項次序號 (如 10, 20, 30...)
 * @param childMaterialCode 子件料號 (如 MAT-SFG-001)
 * @param childMaterialName 子件品名 (如 飛控主板電路總成)
 * @param specification     規格型號說明 (如 STM32H743 Cortex-M7)
 * @param category          物料分類 (成品 / 半成品 / 原物料)
 * @param unit              計量單位 (如 套/組, 件/個, PCS)
 * @param baseQuantity      單機基準用量 (Base Quantity)
 * @param scrapRate         損耗率 (如 0.05 代表 5.00%)
 * @param effectiveQuantity 有效需求用量 (動態公式：H{row}*(1+I{row}))
 * @param standardCost      標準採購單價
 * @param subtotalCost      單項標準成本小計 (動態公式：J{row}*K{row})
 * @param positionCode      安裝位號代碼 (如 FC-ASM, U1~U3)
 * @param remark            備註說明
 */
@ExcelSheet(
		name = "BOM成本分析",
		protect = true,
		protectPassword = "BomAuditSecure2026",
		leftHeader = "【機密審計・嚴禁外流】",
		centerHeader = "產品結構配方與標準成本卷算分析表",
		rightHeader = "製表時間：&D &T",
		leftFooter = "智慧製造與成本工程系統",
		centerFooter = "第 &P 頁 / 共 &N 頁",
		rightFooter = "【極機密・核心配方】"
)
public record BomCostAnalysisReportModel(
		@ExcelColumn(name = "階層", index = 0, format = "0")
		Integer level,

		@ExcelColumn(name = "項次", index = 1, format = "0")
		Integer itemSequence,

		@ExcelColumn(name = "子件料號", index = 2)
		String childMaterialCode,

		@ExcelColumn(name = "子件品名", index = 3)
		String childMaterialName,

		@ExcelColumn(name = "規格型號", index = 4)
		String specification,

		@ExcelColumn(name = "物料分類", index = 5)
		String category,

		@ExcelColumn(name = "單位", index = 6)
		String unit,

		@ExcelColumn(name = "基準用量", index = 7, format = "#,##0.00")
		BigDecimal baseQuantity,

		@ExcelColumn(name = "損耗率", index = 8, format = "0.00%")
		BigDecimal scrapRate,

		@ExcelColumn(name = "需求用量", index = 9, formula = "H{row}*(1+I{row})", format = "#,##0.00")
		BigDecimal effectiveQuantity,

		@ExcelColumn(name = "標準單價", index = 10, format = "$#,##0.00")
		BigDecimal standardCost,

		@ExcelColumn(name = "成本小計", index = 11, formula = "J{row}*K{row}", format = "$#,##0.00")
		BigDecimal subtotalCost,

		@ExcelColumn(name = "安裝位號", index = 12)
		String positionCode,

		@ExcelColumn(name = "備註說明", index = 13)
		String remark
) {}

