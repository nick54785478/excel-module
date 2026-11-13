package com.example.demo.infrastructure.report;

import com.example.demo.application.shared.command.WriteSheetCommand;
import com.example.demo.application.shared.context.ReportDecorationContext;
import com.example.demo.application.shared.strategy.ExcelReportDecoratorStrategy;
import com.example.demo.application.shared.strategy.ExcelReportStrategyKeys;

/**
 * 預設無操作保底裝飾策略 (No-Op Fallback Decorator Strategy)
 * <p>
 * <b>【架構定位與職責規範】</b><br>
 * 位於 Clean Architecture 基礎設施層報表策略套件 ({@code infrastructure/report/})，抽離出 {@code adapter/} 套件。<br>
 * 實作應用層策略契約 {@link ExcelReportDecoratorStrategy}，綁定識別鍵 {@link ExcelReportStrategyKeys#DEFAULT}。<br>
 * 當請求之報表 Key 未註冊任何自訂策略時，原樣回傳基礎工作表命令，保證系統高可用性。
 */
public class DefaultNoOpDecoratorStrategy implements ExcelReportDecoratorStrategy {

	/**
	 * 策略標準識別鍵
	 */
	public static final String STRATEGY_KEY = ExcelReportStrategyKeys.DEFAULT;

	@Override
	public String getDecoratorKey() {
		return STRATEGY_KEY;
	}

	@Override
	public WriteSheetCommand decorate(WriteSheetCommand baseSheet, ReportDecorationContext context) {
		return baseSheet; // 原樣回傳，不做任何修改
	}
}

