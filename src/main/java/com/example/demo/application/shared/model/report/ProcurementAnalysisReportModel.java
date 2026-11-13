package com.example.demo.application.shared.model.report;

import java.math.BigDecimal;

import com.example.demo.application.annotation.mapping.ExcelColumn;
import com.example.demo.application.annotation.mapping.ExcelSheet;

/**
 * 物料採購損益分析報表宣告式模型 (Procurement & Cost Analysis Declarative Report Model)
 * <p>
 * 遵循 AGENTS.md 規範，採用 Java 21 Record 作為應用層不可變資料載體。
 * 結合 {@link ExcelSheet} 與 {@link ExcelColumn} 宣告式註解，示範維度五企業級功能：
 * <ul>
 *   <li><b>原生計算公式 (Dynamic Formulas)</b>：在註解中配置 {@code formula = "C{row}*D{row}"}，
 *       由組裝器自動將 {@code {row}} 替換為實際 Excel 列號。</li>
 *   <li><b>工作表防護 (Sheet Protection)</b>：透過 {@code protectPassword} 啟動密碼防護，防止儲存格被篡改。</li>
 *   <li><b>列印頁首頁尾 (Header & Footer)</b>：內建標註機密審計標籤與動態頁碼 ({@code &P / &N})。</li>
 * </ul>
 *
 * @param itemCode        物料代碼 (如 MAT-001)
 * @param itemName        物料名稱 (如 4K 專業航模機殼)
 * @param quantity        採購數量
 * @param unitPrice       採購單價
 * @param subtotal        採購小計 (動態公式：C{row}*D{row})
 * @param estimatedPrice  預估銷售單價
 * @param grossMarginRate 預估毛利率 (動態公式：(F{row}-D{row})/F{row})
 */
@ExcelSheet(
		name = "採購損益分析",
		protect = true,
		protectPassword = "AuditSecure2026",
		leftHeader = "【機密資料・內部流轉】",
		centerHeader = "物料採購損益分析月報",
		rightHeader = "產製時間：&D &T",
		leftFooter = "【嚴禁外流】",
		centerFooter = "第 &P 頁 / 共 &N 頁",
		rightFooter = "系統製表"
)
public record ProcurementAnalysisReportModel(
		@ExcelColumn(name = "物料代碼", index = 0)
		String itemCode,

		@ExcelColumn(name = "物料名稱", index = 1)
		String itemName,

		@ExcelColumn(name = "採購數量", index = 2, format = "#,##0")
		Integer quantity,

		@ExcelColumn(name = "採購單價", index = 3, format = "#,##0.00")
		BigDecimal unitPrice,

		@ExcelColumn(name = "採購小計", index = 4, formula = "C{row}*D{row}", format = "$#,##0.00")
		BigDecimal subtotal,

		@ExcelColumn(name = "預估售價", index = 5, format = "#,##0.00")
		BigDecimal estimatedPrice,

		@ExcelColumn(name = "預估毛利率", index = 6, formula = "(F{row}-D{row})/F{row}", format = "0.00%")
		BigDecimal grossMarginRate
) {}

