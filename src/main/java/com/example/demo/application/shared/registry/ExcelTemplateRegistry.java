package com.example.demo.application.shared.registry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.example.demo.application.annotation.mapping.ExcelSheet;
import com.example.demo.application.shared.model.BomItemExcelModel;
import com.example.demo.application.shared.model.MaterialExcelModel;
import com.example.demo.application.shared.model.OrderModel;
import com.example.demo.application.shared.model.ProductRecordModel;

/**
 * Excel 宣告式資料模型與範本註冊表 (Excel Template & Model Registry)
 * <p>
 * <b>【架構定位與職責規範】</b><br>
 * 位於 Clean Architecture 應用層共享註冊表套件 ({@code application/shared/registry/})。<br>
 * 遵循物件導向設計原則中的 <b>開放封閉原則 (Open-Closed Principle, OCP)</b> 與單一職責原則 (SRP)：
 * 集中管理所有受支援的 Excel 資料模型（JavaBean 與 Java 21 Record）元數據。
 * 表現層控制器無需為每種業務範本重複撰寫專屬端點，僅需透過本註冊表動態路由與解析模型型別，
 * 後續擴充新範本時（如客戶主檔、請購單等），僅需註冊模型類別，現有 Controller 介面程式碼保持 <b>100% 零修改</b>。
 * </p>
 * <p>
 * <b>【核心特性與支援機制】</b>
 * <ul>
 *   <li><b>多重鍵值彈性匹配</b>：支援以短路由別名 (Slug, 如 {@code orders})、類別名稱 (如 {@code OrderModel})、或大小寫寬鬆匹配目標範本。</li>
 *   <li><b>Excel 工作表名稱自動反查 (Zero-Config Auto-Detection)</b>：自動剖析 {@link ExcelSheet#name()}，支援客戶端無傳入鍵值時直接從 Excel 第一個 Sheet 名稱反查對應模型。</li>
 *   <li><b>執行緒安全與動態擴充</b>：內部以 {@link ConcurrentHashMap} 維護快取，支援應用啟動時自動預載與執行期動態外掛新模型。</li>
 * </ul>
 * </p>
 */
@Component
public class ExcelTemplateRegistry {

	/**
	 * 範本描述符元數據 (Template Descriptor)
	 *
	 * @param key              標準範本鍵值 (如 "orders", "products")
	 * @param modelClass       對應之資料模型 Class (POJO 或 Record)
	 * @param sheetName        對應之 Excel 工作表名稱 (來自 @ExcelSheet)
	 * @param displayName      人類可讀之範本顯示名稱 (如 "訂單", "商品主檔")
	 * @param feedbackFilename 預設產製之錯誤回標檔案名稱 (如 "訂單檢核錯誤回標檔.xlsx")
	 */
	public record TemplateDescriptor(
			String key,
			Class<?> modelClass,
			String sheetName,
			String displayName,
			String feedbackFilename
	) {
		public TemplateDescriptor {
			Objects.requireNonNull(key, "key 不得為空");
			Objects.requireNonNull(modelClass, "modelClass 不得為空");
			sheetName = (sheetName != null && !sheetName.isBlank()) ? sheetName.trim() : "Sheet1";
			displayName = (displayName != null && !displayName.isBlank()) ? displayName.trim() : key;
			feedbackFilename = (feedbackFilename != null && !feedbackFilename.isBlank()) ? feedbackFilename.trim() : (displayName + "檢核錯誤回標檔.xlsx");
		}
	}

	/**
	 * 標準鍵值至範本描述符之主索引表 (Key -> Descriptor)
	 */
	private final Map<String, TemplateDescriptor> descriptorMap = new ConcurrentHashMap<>();

	/**
	 * 寬鬆別名與工作表名稱至標準鍵值之查表 (Alias / SheetName -> Key)
	 */
	private final Map<String, String> aliasMap = new ConcurrentHashMap<>();

	/**
	 * 預設建構子：自動預載系統核心領域範本 (訂單、商品主檔、物料主檔、單階配方)
	 */
	public ExcelTemplateRegistry() {
		// 1. 訂單模型
		register("orders", OrderModel.class, "訂單", "訂單檢核錯誤回標檔.xlsx");
		addAlias("order", "orders");
		addAlias("ordermodel", "orders");

		// 2. 商品主檔模型 (Java 21 Record)
		register("products", ProductRecordModel.class, "商品主檔", "商品檢核錯誤回標檔.xlsx");
		addAlias("product", "products");
		addAlias("productrecordmodel", "products");

		// 3. 物料主檔模型
		register("materials", MaterialExcelModel.class, "物料主檔", "物料檢核錯誤回標檔.xlsx");
		addAlias("material", "materials");
		addAlias("materialexcelmodel", "materials");

		// 4. 單階配方模型 (BOM)
		register("boms", BomItemExcelModel.class, "單階配方", "BOM檢核錯誤回標檔.xlsx");
		addAlias("bom", "boms");
		addAlias("bomitemexcelmodel", "boms");
	}

	/**
	 * 註冊新 Excel 資料模型至註冊表中 (遵循 OCP 原則的擴充點)
	 *
	 * @param key              標準識別鍵值 (如 "customers")
	 * @param modelClass       模型類別 Class (支援 JavaBean 與 Record)
	 * @param displayName      人類可讀顯示名稱 (如 "客戶主檔")
	 * @param feedbackFilename 產製之回標檔案名稱 (如 "客戶檢核錯誤回標檔.xlsx")
	 */
	public void register(String key, Class<?> modelClass, String displayName, String feedbackFilename) {
		Objects.requireNonNull(key, "key 不得為空");
		Objects.requireNonNull(modelClass, "modelClass 不得為空");

		String normalizedKey = key.trim().toLowerCase();

		// 自動探測 @ExcelSheet 註解工作表名稱
		String sheetName = "Sheet1";
		ExcelSheet sheetAnno = modelClass.getAnnotation(ExcelSheet.class);
		if (sheetAnno != null && !sheetAnno.name().isBlank()) {
			sheetName = sheetAnno.name().trim();
		}

		TemplateDescriptor descriptor = new TemplateDescriptor(
				normalizedKey,
				modelClass,
				sheetName,
				displayName,
				feedbackFilename
		);

		descriptorMap.put(normalizedKey, descriptor);

		// 建立內部索引與寬鬆別名
		addAlias(normalizedKey, normalizedKey);
		addAlias(modelClass.getSimpleName(), normalizedKey);
		addAlias(sheetName, normalizedKey);
	}

	/**
	 * 便捷註冊方法 (自動以 displayName 組合回標檔名)
	 *
	 * @param key         標準識別鍵值
	 * @param modelClass  模型類別 Class
	 * @param displayName 人類可讀顯示名稱
	 */
	public void register(String key, Class<?> modelClass, String displayName) {
		register(key, modelClass, displayName, displayName + "檢核錯誤回標檔.xlsx");
	}

	/**
	 * 為指定鍵值建立查詢別名
	 *
	 * @param alias  別名 (如工作表名、類別名或簡寫)
	 * @param target 目標標準鍵值
	 */
	public void addAlias(String alias, String target) {
		if (alias != null && !alias.isBlank() && target != null && !target.isBlank()) {
			aliasMap.put(alias.trim().toLowerCase(), target.trim().toLowerCase());
		}
	}

	/**
	 * 依據鍵值、別名或工作表名稱反查範本描述符
	 *
	 * @param keyOrSheetName 識別鍵值、類別名或試算表工作表名稱
	 * @return 匹配之範本描述符 {@link Optional}
	 */
	public Optional<TemplateDescriptor> find(String keyOrSheetName) {
		if (keyOrSheetName == null || keyOrSheetName.isBlank()) {
			return Optional.empty();
		}
		String normalized = keyOrSheetName.trim().toLowerCase();
		String targetKey = aliasMap.get(normalized);
		if (targetKey != null) {
			return Optional.ofNullable(descriptorMap.get(targetKey));
		}
		return Optional.ofNullable(descriptorMap.get(normalized));
	}

	/**
	 * 依據鍵值、別名或工作表名稱取得必要之範本描述符；若查無則拋出清晰之業務例外
	 *
	 * @param keyOrSheetName 識別鍵值或工作表名稱
	 * @return 匹配之範本描述符
	 * @throws IllegalArgumentException 若查無任何匹配之已註冊範本
	 */
	public TemplateDescriptor getRequired(String keyOrSheetName) {
		return find(keyOrSheetName).orElseThrow(() -> new IllegalArgumentException(
				"不支援或未註冊之 Excel 範本鍵值: [" + keyOrSheetName + "]。目前系統已註冊之可用範本列表: " + getAvailableKeys()
		));
	}

	/**
	 * 依據 Excel 工作表名稱精準反查對應範本
	 *
	 * @param sheetName 試算表工作表名稱
	 * @return 匹配之範本描述符 {@link Optional}
	 */
	public Optional<TemplateDescriptor> findBySheetName(String sheetName) {
		return find(sheetName);
	}

	/**
	 * 取得目前所有已註冊標準範本清單
	 *
	 * @return 不可變範本描述符清單
	 */
	public List<TemplateDescriptor> getAllTemplates() {
		return List.copyOf(descriptorMap.values());
	}

	/**
	 * 取得目前所有已註冊之標準鍵值集合
	 *
	 * @return 標準鍵值集合 (例如 ["orders", "products", "materials", "boms"])
	 */
	public Set<String> getAvailableKeys() {
		return Collections.unmodifiableSet(descriptorMap.keySet());
	}
}

