package com.example.demo.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.demo.application.shared.model.BomItemExcelModel;
import com.example.demo.application.shared.model.MaterialExcelModel;
import com.example.demo.application.shared.model.OrderModel;
import com.example.demo.application.shared.model.ProductRecordModel;
import com.example.demo.infrastructure.template.ExcelTemplateCustomizer;
import com.example.demo.infrastructure.template.ExcelTemplateRegistry;

/**
 * Excel 宣告式範本元數據註冊表全域配置類 (Excel Template Configuration)
 * <p>
 * <b>【架構定位與職責規範】</b><br>
 * 位於 Clean Architecture 六角形架構之<b>全域配置層 (Configuration Layer - The Dirtiest Layer / Composition Root)</b>，
 * 嚴格遵循 {@code AGENTS.md} 規則 40~41。<br>
 * 配置層作為整座系統中最依賴技術框架與環境細節的一層，專門負責將表現層、應用層、領域層與基礎設施層組裝連接起來。
 * </p>
 * <p>
 * <b>【架構重構與職責邊界】</b><br>
 * 過去將範本預載邏輯硬編碼於應用層或註冊表類別內部，造成業務模型強耦合與配置外溢的壞味道。
 * 本類別將「<b>範本技術容器 (Infrastructure Container)</b>」與「<b>業務範本裝配 (Business Template Wiring)</b>」徹底拆分：
 * <ul>
 *   <li><b>技術容器純粹化</b>：底層 {@link ExcelTemplateRegistry} 僅作為執行緒安全的純粹元數據容器，零業務模型依賴。</li>
 *   <li><b>配置裝配集中化</b>：由本配置類集中依賴具體業務模型（{@link OrderModel}、{@link ProductRecordModel}、{@link MaterialExcelModel}、{@link BomItemExcelModel}），
 *       並執行路由別名、中文工作表名稱與檢核錯誤回標檔名之全量映射裝配。</li>
 *   <li><b>徹底落實 OCP (Open-Closed Principle)</b>：未來擴充新業務範本時，無需修改底層註冊表類別或表現層控制器，
 *       僅需於本配置類追加註冊，或透過實作 {@link ExcelTemplateCustomizer} SPI 宣告為 Spring Bean 即可隨插即用。</li>
 * </ul>
 * </p>
 *
 * @see ExcelTemplateRegistry
 * @see ExcelTemplateCustomizer
 */
@Configuration
public class ExcelTemplateConfiguration {

	private static final Logger log = LoggerFactory.getLogger(ExcelTemplateConfiguration.class);

	// =========================================================================
	// 1. Spring 容器 Bean 宣告與外掛擴充收集 (Spring Bean Definition & SPI Pipeline)
	// =========================================================================

	/**
	 * 註冊 Excel 範本元數據註冊表 Spring Bean
	 * <p>
	 * <b>【生命週期與裝配流程】</b>
	 * <ol>
	 *   <li>調用 {@link #createDefaultRegistry()} 建立乾淨的 {@link ExcelTemplateRegistry} 並預載系統四大核心業務範本。</li>
	 *   <li>利用 Spring IoC 的容器自動收集特性，注入所有註冊為 Spring Bean 之 {@link ExcelTemplateCustomizer} 自訂擴充器。</li>
	 *   <li>依序回呼每個擴充器的 {@link ExcelTemplateCustomizer#customize(ExcelTemplateRegistry)} 方法，動態外掛自訂單據範本。</li>
	 *   <li>輸出標準化啟動日誌，列出目前系統所支援之所有範本識別鍵與總數。</li>
	 * </ol>
	 * </p>
	 * <p>
	 * <b>【無侵入覆寫支援】</b><br>
	 * 標註 {@link ConditionalOnMissingBean}，支援下游微服務或特定測試環境自行宣告自訂的 {@link ExcelTemplateRegistry} Bean 進行無縫覆寫。
	 * </p>
	 *
	 * @param customizers 容器中所有宣告為 Spring Bean 的範本自訂擴充器清單；若無任何外掛 Bean 則為 null 或空清單
	 * @return 配置完成且預載核心業務模型與動態擴充之 {@link ExcelTemplateRegistry} 單例實例
	 */
	@Bean
	@ConditionalOnMissingBean(ExcelTemplateRegistry.class)
	public ExcelTemplateRegistry excelTemplateRegistry(
			@Autowired(required = false) List<ExcelTemplateCustomizer> customizers
	) {
		// 1. 建立預載四大核心業務範本之標準註冊表
		ExcelTemplateRegistry registry = createDefaultRegistry();

		// 2. 執行外掛擴充器管線 (SPI Customizer Pipeline)
		if (customizers != null && !customizers.isEmpty()) {
			for (ExcelTemplateCustomizer customizer : customizers) {
				if (customizer != null) {
					customizer.customize(registry);
					log.info("[ExcelTemplateConfiguration] 成功套用自訂範本擴充器 (Applied template customizer): {}",
							customizer.getClass().getSimpleName());
				}
			}
		}

		// 3. 輸出容器初始化摘要日誌
		log.info("[ExcelTemplateConfiguration] ExcelTemplateRegistry 初始化完成，當前共註冊 {} 個業務範本，可用鍵值: {}",
				registry.getAllTemplates().size(), registry.getAvailableKeys());

		return registry;
	}

	// =========================================================================
	// 2. 系統核心業務範本預載裝配工廠 (Core Business Templates Wiring Factory)
	// =========================================================================

	/**
	 * 建立並預載系統核心業務範本之標準註冊表實例 (靜態工廠方法)
	 * <p>
	 * <b>【設計目的與職責】</b><br>
	 * 提供純淨無 Spring 容器環境下的註冊表建構能力：
	 * <ul>
	 *   <li><b>生產環境</b>：由 {@link #excelTemplateRegistry(List)} 調用以建立標準 Spring Bean。</li>
	 *   <li><b>單元測試環境</b>：供 Controller 單元測試（如 {@code ExcelValidationControllerTest}）在無需啟動完整 Spring 容器時，
	 *       快速獲取與生產環境 100% 一致之預載註冊表，徹底維持測試獨立性與執行效能。</li>
	 * </ul>
	 * </p>
	 * <p>
	 * <b>【預載之四大核心業務單據】</b>
	 * <ol>
	 *   <li><b>訂單模型 ({@code orders})</b>：傳統 JavaBean POJO 映射，支援訂單欄位檢核與錯誤回標。</li>
	 *   <li><b>商品主檔模型 ({@code products})</b>：現代 Java 21 Record 不可變型別映射，展示高效能無樣板代碼之資料校驗。</li>
	 *   <li><b>物料主檔模型 ({@code materials})</b>：製造業 ERP 核心物料主檔對照表，支援工作表名稱（如「物料主檔對照表」）自動反查。</li>
	 *   <li><b>單階配方模型 ({@code boms})</b>：製造業工程 BOM 結構單階清單，支援階層與損耗率檢核。</li>
	 * </ol>
	 * </p>
	 *
	 * @return 預載完成之 {@link ExcelTemplateRegistry} 實例
	 */
	public static ExcelTemplateRegistry createDefaultRegistry() {
		ExcelTemplateRegistry registry = new ExcelTemplateRegistry();

		// ---------------------------------------------------------------------
		// 1. 訂單模型 (Order Model - POJO)
		// 業務場景：電子商務或 ERP 採購/銷售訂單批次匯入檢核
		// ---------------------------------------------------------------------
		registry.register("orders", OrderModel.class, "訂單", "訂單檢核錯誤回標檔.xlsx");
		registry.addAlias("order", "orders");               // 單數路由別名 (/validation/order)
		registry.addAlias("ordermodel", "orders");          // 類別名稱別名 (OrderModel)

		// ---------------------------------------------------------------------
		// 2. 商品主檔模型 (Product Model - Java 21 Record)
		// 業務場景：商品目錄、定價與規格批次建檔檢核
		// ---------------------------------------------------------------------
		registry.register("products", ProductRecordModel.class, "商品主檔", "商品檢核錯誤回標檔.xlsx");
		registry.addAlias("product", "products");           // 單數路由別名 (/validation/product)
		registry.addAlias("productrecordmodel", "products");// 類別名稱別名 (ProductRecordModel)

		// ---------------------------------------------------------------------
		// 3. 物料主檔模型 (Material Master Model)
		// 業務場景：製造業物料品名、規格、標準成本與計量單位主檔維護
		// ---------------------------------------------------------------------
		registry.register("materials", MaterialExcelModel.class, "物料主檔", "物料檢核錯誤回標檔.xlsx");
		registry.addAlias("material", "materials");         // 單數路由別名 (/validation/material)
		registry.addAlias("materialexcelmodel", "materials");// 類別名稱別名 (MaterialExcelModel)
		registry.addAlias("物料主檔對照表", "materials");    // 工作表文字別名 (Zero-Config 自動探測)
		registry.addAlias("物料主檔清單", "materials");      // 工作表文字別名
		registry.addAlias("物料主檔", "materials");          // 工作表文字別名

		// ---------------------------------------------------------------------
		// 4. 單階配方模型 (Single-Level BOM Model)
		// 業務場景：產品結構清單、父子階關係、投入產出比與標準損耗率檢核
		// ---------------------------------------------------------------------
		registry.register("boms", BomItemExcelModel.class, "單階配方", "BOM檢核錯誤回標檔.xlsx");
		registry.addAlias("bom", "boms");                   // 單數路由別名 (/validation/bom)
		registry.addAlias("bomitemexcelmodel", "boms");     // 類別名稱別名 (BomItemExcelModel)
		registry.addAlias("單階bom匯入清單", "boms");        // 工作表文字別名 (Zero-Config 自動探測)
		registry.addAlias("單階bom清單", "boms");            // 工作表文字別名
		registry.addAlias("單階bom", "boms");                // 工作表文字別名

		return registry;
	}
}
