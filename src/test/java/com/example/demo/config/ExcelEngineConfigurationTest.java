package com.example.demo.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.example.demo.application.port.out.ExcelReaderPort;
import com.example.demo.application.port.out.ExcelWriterPort;
import com.example.demo.infrastructure.adapter.FastExcelReaderAdapter;
import com.example.demo.infrastructure.adapter.FastExcelWriterAdapter;
import com.example.demo.infrastructure.adapter.PoiSaxReaderAdapter;
import com.example.demo.infrastructure.adapter.PoiSxssfWriterAdapter;

/**
 * ExcelEngineConfiguration 配置切換測試 (Configuration Layer Tests)
 * <p>
 * 遵循 Clean Architecture 配置層測試規範，驗證依據 {@code excel.engine} 旗標動態決定注入
 * FastExcel 或是 Apache POI 之適配器實例。
 */
@DisplayName("ExcelEngineConfiguration 模組開關配置測試")
class ExcelEngineConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(ExcelEngineConfiguration.class);

	/**
	 * Feature: 預設配置注入 FastExcel 適配器
	 * Scenario: 未指定 excel.engine 屬性時
	 * Given: 容器未提供 excel.engine 配置
	 * When: 啟動 Spring 應用上下文
	 * Then: 預設注入 FastExcelWriterAdapter 與 FastExcelReaderAdapter
	 */
	@Test
	@DisplayName("情境：未配置 excel.engine 時預設注入 FastExcel 適配器")
	void shouldDefaultToFastExcelWhenNoPropertyConfigured() {
		contextRunner.run(context -> {
			assertThat(context).hasSingleBean(ExcelWriterPort.class);
			assertThat(context).hasSingleBean(ExcelReaderPort.class);

			ExcelWriterPort writerPort = context.getBean(ExcelWriterPort.class);
			ExcelReaderPort readerPort = context.getBean(ExcelReaderPort.class);

			assertThat(writerPort).isInstanceOf(FastExcelWriterAdapter.class);
			assertThat(readerPort).isInstanceOf(FastExcelReaderAdapter.class);
		});
	}

	/**
	 * Feature: 明確配置 FAST_EXCEL 旗標
	 * Scenario: 設定 excel.engine=FAST_EXCEL
	 * Given: excel.engine=FAST_EXCEL
	 * When: 啟動 Spring 應用上下文
	 * Then: 注入 FastExcel 適配器
	 */
	@Test
	@DisplayName("情境：配置 excel.engine=FAST_EXCEL 成功注入 FastExcel 適配器")
	void shouldInjectFastExcelWhenPropertyIsFastExcel() {
		contextRunner
				.withPropertyValues("excel.engine=FAST_EXCEL")
				.run(context -> {
					assertThat(context).hasSingleBean(ExcelWriterPort.class);
					assertThat(context).hasSingleBean(ExcelReaderPort.class);

					assertThat(context.getBean(ExcelWriterPort.class)).isInstanceOf(FastExcelWriterAdapter.class);
					assertThat(context.getBean(ExcelReaderPort.class)).isInstanceOf(FastExcelReaderAdapter.class);
				});
	}

	/**
	 * Feature: 明確配置 APACHE_POI 旗標
	 * Scenario: 設定 excel.engine=APACHE_POI
	 * Given: excel.engine=APACHE_POI
	 * When: 啟動 Spring 應用上下文
	 * Then: 注入 PoiSxssfWriterAdapter 與 PoiSaxReaderAdapter
	 */
	@Test
	@DisplayName("情境：配置 excel.engine=APACHE_POI 成功注入 Apache POI 適配器")
	void shouldInjectApachePoiWhenPropertyIsApachePoi() {
		contextRunner
				.withPropertyValues("excel.engine=APACHE_POI")
				.run(context -> {
					assertThat(context).hasSingleBean(ExcelWriterPort.class);
					assertThat(context).hasSingleBean(ExcelReaderPort.class);

					assertThat(context.getBean(ExcelWriterPort.class)).isInstanceOf(PoiSxssfWriterAdapter.class);
					assertThat(context.getBean(ExcelReaderPort.class)).isInstanceOf(PoiSaxReaderAdapter.class);
				});
	}
}

