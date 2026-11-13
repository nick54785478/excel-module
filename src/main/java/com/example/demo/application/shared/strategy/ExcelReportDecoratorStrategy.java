package com.example.demo.application.shared.strategy;

import com.example.demo.application.shared.command.WriteSheetCommand;
import com.example.demo.application.shared.context.ReportDecorationContext;

/**
 * 企業級報表裝飾策略介面 (Excel Report Decorator Strategy)
 * <p>
 * <b>【架構定位與職責規範】</b><br>
 * 位於 Clean Architecture 應用層共享策略套件 ({@code application/shared/strategy/})。<br>
 * 作為策略模式 (Strategy Pattern) 中的抽象策略規格契約 (Policy / SPI)，
 * 與對外通訊孔道 (Outbound Port: {@code ExcelReportDecoratorFactoryPort}) 職責分離：
 * 工廠負責管理與提供策略實例，本策略介面負責定義「如何對基礎工作表執行排版裝飾」。
 * <p>
 * 每個具體策略綁定一個唯一的識別鍵值 (Key)，例如 "PROCUREMENT_ANALYSIS"、"INVENTORY_ANALYSIS"。
 */
public interface ExcelReportDecoratorStrategy {

	/**
	 * 策略所支援的唯一識別鍵值 (Key)
	 *
	 * @return 唯一識別鍵值字串 (大小寫不敏感)
	 */
	String getDecoratorKey();

	/**
	 * 對基礎工作表命令執行進階樣式裝飾、公式注入、儲存格合併與安全防護
	 *
	 * @param baseSheet 由 ExcelBeanAssembler 解析模型產出的基礎工作表命令
	 * @param context   通用裝飾上下文 (包含標題、密碼、機密等級、原始資料清單與擴充參數)
	 * @return 裝飾完成之 WriteSheetCommand
	 */
	WriteSheetCommand decorate(WriteSheetCommand baseSheet, ReportDecorationContext context);
}

