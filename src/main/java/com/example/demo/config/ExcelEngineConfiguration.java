package com.example.demo.config;

import java.util.List;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.demo.application.port.out.core.ExcelReaderPort;
import com.example.demo.application.port.out.report.ExcelReportDecoratorFactoryPort;
import com.example.demo.application.port.out.core.ExcelWriterPort;
import com.example.demo.application.shared.strategy.ExcelReportDecoratorStrategy;
import com.example.demo.infrastructure.adapter.core.FastExcelReaderAdapter;
import com.example.demo.infrastructure.adapter.core.FastExcelWriterAdapter;
import com.example.demo.infrastructure.adapter.core.PoiSaxReaderAdapter;
import com.example.demo.infrastructure.adapter.core.PoiSxssfWriterAdapter;
import com.example.demo.infrastructure.adapter.report.SpringReportDecoratorFactoryAdapter;
import com.example.demo.infrastructure.engine.ExcelStreamWriterEngine;
import com.example.demo.infrastructure.engine.fastexcel.FastExcelStreamWriterEngine;
import com.example.demo.infrastructure.engine.poi.PoiSxssfStreamWriterEngine;

/**
 * Excel 處理引擎動態裝配配置類 (Configuration Layer - Composition Root)
 * <p>
 * 遵循 Clean Architecture 六角形架構規範，配置層集中管理所有外層技術框架與組件組裝。
 * 依據 {@code application.properties} 中定義的 {@code excel.engine} 模組開關 (Flag)，
 * 動態決定將高效能輕量級的 FastExcel 適配器或是功能全能型 Apache POI (SXSSF/SAX) 適配器
 * 注入為應用層輸出埠 (Outbound Port: {@link ExcelWriterPort} 與 {@link ExcelReaderPort})。
 * <p>
 * 支援組態設定值：
 * <ul>
 * <li>{@code FAST_EXCEL} (預設值)：極速、超低記憶體佔用之常數級大數據串流輸出與解析。</li>
 * <li>{@code APACHE_POI}：支援複雜樣式、全格式相容、下拉清單驗證與硬碟暫存沖刷之經典企業級引擎。</li>
 * </ul>
 */
@Configuration
public class ExcelEngineConfiguration {

	private static final Logger log = LoggerFactory.getLogger(ExcelEngineConfiguration.class);

	@Value("${excel.engine:FAST_EXCEL}")
	private String configuredEngine;

	/**
	 * 於 Spring 容器初始化時，印出清晰醒目的引擎啟動提示通知。
	 */
	@PostConstruct
	public void logEngineStartup() {
		String engine = (configuredEngine != null) ? configuredEngine.trim().toUpperCase() : "FAST_EXCEL";
		boolean isPoi = "APACHE_POI".equals(engine);

		log.info("""
				
				========================================================================================
				 🚀 [Excel Module Startup Notification] 引擎啟動宣告
				    目前生效引擎 (Active Engine) : {}
				    模式特性 (Mode & Features)   : {}
				    寫入適配器 (Writer Adapter)  : {}
				    讀取適配器 (Reader Adapter)  : {}
				========================================================================================""",
				engine,
				isPoi ? "Apache POI (SXSSF / SAX) - 工業級豐富樣式、凍結窗格、全格式支援與磁碟暫存緩衝"
						: "FastExcel (StAX Streaming) - 極速常數級超低記憶體串流、毫秒級高併發吞吐量 (純記憶體)",
				isPoi ? "PoiSxssfWriterAdapter" : "FastExcelWriterAdapter",
				isPoi ? "PoiSaxReaderAdapter" : "FastExcelReaderAdapter");
	}

	// =========================================================================
	// 1. FastExcel 引擎適配器裝配 (預設啟用: matchIfMissing = true)
	// =========================================================================

	/**
	 * 註冊 FastExcel 串流寫入共用引擎 (基礎設施層內部共用)
	 *
	 * @return FastExcel 串流寫入引擎實例
	 */
	@Bean
	@ConditionalOnProperty(name = "excel.engine", havingValue = "FAST_EXCEL", matchIfMissing = true)
	public ExcelStreamWriterEngine fastExcelStreamWriterEngine() {
		return new FastExcelStreamWriterEngine();
	}

	/**
	 * 註冊 FastExcel 寫入適配器
	 *
	 * @param engine 基礎設施層串流寫入共用引擎
	 * @return FastExcel 寫入適配器實例
	 */
	@Bean
	@ConditionalOnProperty(name = "excel.engine", havingValue = "FAST_EXCEL", matchIfMissing = true)
	public ExcelWriterPort fastExcelWriterPort(ExcelStreamWriterEngine engine) {
		log.info("[ExcelEngineConfiguration] Activating FastExcel Writer Adapter (High-performance stream mode)");
		return new FastExcelWriterAdapter(engine);
	}

	/**
	 * 註冊 FastExcel 讀取適配器
	 *
	 * @return FastExcel 讀取適配器實例
	 */
	@Bean
	@ConditionalOnProperty(name = "excel.engine", havingValue = "FAST_EXCEL", matchIfMissing = true)
	public ExcelReaderPort fastExcelReaderPort() {
		log.info("[ExcelEngineConfiguration] Activating FastExcel Reader Adapter (StAX low-memory mode)");
		return new FastExcelReaderAdapter();
	}

	// =========================================================================
	// 2. Apache POI (SXSSF / SAX) 引擎適配器裝配
	// =========================================================================

	/**
	 * 註冊 Apache POI SXSSF 串流寫入共用引擎 (基礎設施層內部共用)
	 *
	 * @return Apache POI SXSSF 串流寫入引擎實例
	 */
	@Bean
	@ConditionalOnProperty(name = "excel.engine", havingValue = "APACHE_POI")
	public ExcelStreamWriterEngine poiExcelStreamWriterEngine() {
		return new PoiSxssfStreamWriterEngine();
	}

	/**
	 * 註冊 Apache POI SXSSF 寫入適配器
	 *
	 * @param engine 基礎設施層串流寫入共用引擎
	 * @return Apache POI SXSSF 寫入適配器實例
	 */
	@Bean
	@ConditionalOnProperty(name = "excel.engine", havingValue = "APACHE_POI")
	public ExcelWriterPort poiExcelWriterPort(ExcelStreamWriterEngine engine) {
		log.info(
				"[ExcelEngineConfiguration] Activating Apache POI SXSSF Writer Adapter (Feature-rich / Disk-buffered mode)");
		return new PoiSxssfWriterAdapter(engine);
	}

	/**
	 * 註冊 Apache POI SAX 讀取適配器
	 *
	 * @return Apache POI SAX 讀取適配器實例
	 */
	@Bean
	@ConditionalOnProperty(name = "excel.engine", havingValue = "APACHE_POI")
	public ExcelReaderPort poiExcelReaderPort() {
		log.info(
				"[ExcelEngineConfiguration] Activating Apache POI Reader Adapter (Full format / Formula evaluation mode)");
		return new PoiSaxReaderAdapter();
	}

	// =========================================================================
	// 3. 企業級報表裝飾策略工廠裝配 (支援自定義覆寫)
	// =========================================================================

	/**
	 * 註冊報表裝飾策略工廠輸出埠
	 * <p>
	 * 標註 {@link ConditionalOnMissingBean}，支援二次開發者或下游子系統透過提供自訂 {@link ExcelReportDecoratorFactoryPort} Bean
	 * 進行無侵入覆寫。
	 *
	 * @param strategies 容器中所有自動收集之裝飾策略清單
	 * @return 策略工廠輸出埠實例
	 */
	@Bean
	@ConditionalOnMissingBean(ExcelReportDecoratorFactoryPort.class)
	public ExcelReportDecoratorFactoryPort excelReportDecoratorFactoryPort(List<ExcelReportDecoratorStrategy> strategies) {
		log.info("[ExcelEngineConfiguration] Activating Spring Report Decorator Factory Port with {} strategies",
				strategies != null ? strategies.size() : 0);
		return new SpringReportDecoratorFactoryAdapter(strategies);
	}
}
