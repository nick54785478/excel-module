package com.example.demo.config;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;

import com.example.demo.application.annotation.mapping.ExcelSheet;
import com.example.demo.application.port.out.core.ExcelSheetModelRegistryPort;
import com.example.demo.application.shared.model.report.BomCostAnalysisReportModel;
import com.example.demo.application.shared.model.bom.BomItemExcelModel;
import com.example.demo.application.shared.model.material.MaterialExcelModel;
import com.example.demo.application.shared.model.bom.MultiLevelBomExcelModel;
import com.example.demo.application.shared.model.demo.OrderModel;
import com.example.demo.application.shared.model.report.ProcurementAnalysisReportModel;
import com.example.demo.application.shared.model.demo.ProductRecordModel;
import com.example.demo.infrastructure.adapter.core.ExcelSheetModelRegistryAdapter;

/**
 * 宣告式 Excel 模型註冊表全域配置類 (Excel Sheet Model Configuration)
 * <p>
 * <b>【架構定位與職責規範】</b><br>
 * 位於 Clean Architecture 六角形架構之<b>全域配置層 (Configuration Layer - The Dirtiest Layer / Composition Root)</b>，
 * 嚴格遵循 {@code AGENTS.md} 規則 40~41。<br>
 * 作為系統中最依賴技術框架與環境細節的一層，專門負責將表現層、應用層與基礎設施層組裝連接起來。
 * </p>
 * <p>
 * <b>【全自動探測裝配機制 (Zero-Config Auto-Discovery)】</b><br>
 * 利用 Spring 核心的 {@link ClassPathScanningCandidateComponentProvider} 與 ASM 位元組碼快速掃描機制，
 * 於應用程式啟動時自動掃描指定套件下所有標註有 {@link ExcelSheet} 之模型類別，
 * 動態抽取其工作表主名稱與別名並登錄至 {@link ExcelSheetModelRegistryAdapter} 中。<br>
 * 未來開發者新增任何業務模型，只要標註 {@link ExcelSheet}，無需任何手動配置即可自動生效。
 * </p>
 */
@Configuration
public class ExcelSheetModelConfiguration {

	private static final Logger log = LoggerFactory.getLogger(ExcelSheetModelConfiguration.class);

	/**
	 * 預設模型掃描基礎套件
	 */
	private static final String DEFAULT_SCAN_PACKAGE = "com.example.demo.application.shared.model";

	/**
	 * 註冊宣告式 Excel 模型註冊表 Spring Bean (輸出埠 {@link ExcelSheetModelRegistryPort})
	 *
	 * @return 配置完成且預載所有宣告式模型索引之 {@link ExcelSheetModelRegistryPort} 單例實例
	 */
	@Bean
	@ConditionalOnMissingBean(ExcelSheetModelRegistryPort.class)
	public ExcelSheetModelRegistryPort excelSheetModelRegistryPort() {
		ExcelSheetModelRegistryAdapter adapter = new ExcelSheetModelRegistryAdapter();

		// 1. 透過 Spring 位元組碼掃描器執行全自動探測
		autoScanAndRegister(adapter, DEFAULT_SCAN_PACKAGE);

		// 2. 保底註冊核心既有模型 (防禦非標準 ClassLoader 或測試環境掃描受限情境)
		registerCoreFallbackModels(adapter);

		log.info("[ExcelSheetModelConfiguration] ExcelSheetModelRegistryPort 自動裝配完成，當前共註冊 {} 個模型類別",
				adapter.getAllModelClasses().size());

		return adapter;
	}

	/**
	 * 建立預載核心模型之獨立註冊表實例 (供無 Spring 容器之純單元測試使用)
	 *
	 * @return 預載核心模型之 {@link ExcelSheetModelRegistryAdapter} 實例
	 */
	public static ExcelSheetModelRegistryAdapter createDefaultRegistry() {
		ExcelSheetModelRegistryAdapter adapter = new ExcelSheetModelRegistryAdapter();
		registerCoreFallbackModels(adapter);
		return adapter;
	}

	/**
	 * 透過 Spring 位元組碼掃描器自動搜尋標註有 @ExcelSheet 之類別並註冊
	 */
	private void autoScanAndRegister(ExcelSheetModelRegistryAdapter adapter, String scanPackage) {
		try {
			ClassPathScanningCandidateComponentProvider scanner =
					new ClassPathScanningCandidateComponentProvider(false) {
						@Override
						protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
							return beanDefinition.getMetadata().isIndependent()
									&& !beanDefinition.getMetadata().isInterface();
						}
					};

			scanner.addIncludeFilter(new AnnotationTypeFilter(ExcelSheet.class));

			Set<BeanDefinition> candidates = scanner.findCandidateComponents(scanPackage);
			ClassLoader classLoader = ClassUtils.getDefaultClassLoader();

			for (BeanDefinition bd : candidates) {
				String className = bd.getBeanClassName();
				if (className != null && classLoader != null) {
					try {
						Class<?> clazz = ClassUtils.forName(className, classLoader);
						adapter.register(clazz);
					} catch (Throwable ex) {
						log.warn("[ExcelSheetModelConfiguration] 載入模型類別失敗: {}", className, ex);
					}
				}
			}
			log.info("[ExcelSheetModelConfiguration] 自動掃描套件 '{}' 發現並註冊了 {} 個 @ExcelSheet 模型",
					scanPackage, candidates.size());
		} catch (Throwable ex) {
			log.warn("[ExcelSheetModelConfiguration] 自動掃描 @ExcelSheet 失敗，將回退為保底裝配: {}", ex.getMessage());
		}
	}

	/**
	 * 保底註冊系統現有之核心模型
	 */
	private static void registerCoreFallbackModels(ExcelSheetModelRegistryAdapter adapter) {
		adapter.register(MaterialExcelModel.class);
		adapter.register(BomItemExcelModel.class);
		adapter.register(MultiLevelBomExcelModel.class);
		adapter.register(OrderModel.class);
		adapter.register(ProductRecordModel.class);
		adapter.register(BomCostAnalysisReportModel.class);
		adapter.register(ProcurementAnalysisReportModel.class);
	}
}

