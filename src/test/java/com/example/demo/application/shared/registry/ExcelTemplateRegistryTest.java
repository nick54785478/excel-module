package com.example.demo.application.shared.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.demo.application.annotation.mapping.ExcelColumn;
import com.example.demo.application.annotation.mapping.ExcelSheet;
import com.example.demo.application.shared.model.OrderModel;
import com.example.demo.application.shared.model.ProductRecordModel;

/**
 * Excel 範本註冊表單元測試 (ExcelTemplateRegistry Test)
 * <p>
 * 遵循 BDD 與 Gherkin 規範，驗證 {@link ExcelTemplateRegistry} 之動態註冊、別名多重解析、
 * 工作表名稱反查 (Zero-Config) 以及 OCP 開放封閉擴充機制。
 * </p>
 */
@DisplayName("ExcelTemplateRegistry 宣告式範本註冊表測試")
class ExcelTemplateRegistryTest {

	private ExcelTemplateRegistry registry;

	@BeforeEach
	void setUp() {
		registry = new ExcelTemplateRegistry();
	}

	/**
	 * 測試自訂擴充模型
	 */
	@ExcelSheet(name = "客戶主檔清單")
	static class CustomerTestModel {
		@ExcelColumn(name = "客戶編號", index = 0)
		private String customerId;

		public String getCustomerId() {
			return customerId;
		}

		public void setCustomerId(String customerId) {
			this.customerId = customerId;
		}
	}

	/**
	 * Feature: 核心領域模型預載驗證
	 * Scenario: 驗證系統建構時自動預先載入訂單、商品、物料與配方範本
	 * Given: 建立全新 ExcelTemplateRegistry 實例
	 * When: 查詢預設鍵值 orders, products, materials, boms
	 * Then: 皆能成功取得描述符，且包含正確之 Model Class 與工作表名稱
	 */
	@Test
	@DisplayName("情境：預設建構子應自動預載系統核心業務範本")
	void shouldPreloadCoreTemplatesByDefault() {
		// When & Then - 訂單
		ExcelTemplateRegistry.TemplateDescriptor orderDesc = registry.getRequired("orders");
		assertThat(orderDesc.modelClass()).isEqualTo(OrderModel.class);
		assertThat(orderDesc.sheetName()).isEqualTo("訂單匯入清單");
		assertThat(orderDesc.feedbackFilename()).isEqualTo("訂單檢核錯誤回標檔.xlsx");

		// When & Then - 商品主檔 (Java 21 Record)
		ExcelTemplateRegistry.TemplateDescriptor prodDesc = registry.getRequired("products");
		assertThat(prodDesc.modelClass()).isEqualTo(ProductRecordModel.class);
		assertThat(prodDesc.sheetName()).isEqualTo("商品主檔清單");
	}

	/**
	 * Feature: 多重別名與大小寫寬鬆匹配
	 * Scenario: 驗證以類別名、單數名或大寫名稱皆能精準解析
	 * Given: 預載有 orders 之註冊表
	 * When: 分別以 "ORDER", "OrderModel", "orders" 查詢
	 * Then: 皆能正確定位至同一 OrderModel 描述符
	 */
	@Test
	@DisplayName("情境：支援大小寫不敏感、類別名與單複數別名智能解析")
	void shouldResolveByAliasesCaseInsensitively() {
		Optional<ExcelTemplateRegistry.TemplateDescriptor> opt1 = registry.find("ORDER");
		Optional<ExcelTemplateRegistry.TemplateDescriptor> opt2 = registry.find("OrderModel");
		Optional<ExcelTemplateRegistry.TemplateDescriptor> opt3 = registry.find("orders");

		assertThat(opt1).isPresent();
		assertThat(opt2).isPresent();
		assertThat(opt3).isPresent();
		assertThat(opt1.get().modelClass()).isEqualTo(OrderModel.class);
		assertThat(opt2.get().modelClass()).isEqualTo(OrderModel.class);
	}

	/**
	 * Feature: 工作表名稱反查 (Zero-Config 自動偵測)
	 * Scenario: 依據 Excel 工作表文字 (如 "訂單匯入清單") 反查對應之資料模型規格
	 * Given: 註冊表包含 @ExcelSheet(name = "訂單匯入清單") 之 OrderModel
	 * When: 呼叫 findBySheetName("訂單匯入清單")
	 * Then: 成功返回 OrderModel 之描述符
	 */
	@Test
	@DisplayName("情境：可直接透過 Excel 工作表名稱反查範本規格")
	void shouldFindBySheetNameSuccessfully() {
		Optional<ExcelTemplateRegistry.TemplateDescriptor> opt = registry.findBySheetName("訂單匯入清單");

		assertThat(opt).isPresent();
		assertThat(opt.get().key()).isEqualTo("orders");
		assertThat(opt.get().modelClass()).isEqualTo(OrderModel.class);
	}

	/**
	 * Feature: 遵循 OCP 開放封閉原則之動態擴充能力
	 * Scenario: 當新增 CustomerTestModel 時，直接註冊即可生效，無須更動任何 Controller 程式碼
	 * Given: 透過 registry.register("customers", CustomerTestModel.class, "客戶主檔")
	 * When: 以鍵值 "customers" 或工作表名 "客戶主檔清單" 查詢
	 * Then: 成功取得新註冊之描述符，且包含預設生成的客戶主檔檢核錯誤回標檔.xlsx
	 */
	@Test
	@DisplayName("情境：遵循 OCP 原則，支援動態擴充註冊新資料模型與自動抽取 @ExcelSheet")
	void shouldSupportDynamicRegistrationFollowingOcp() {
		// When: 註冊全新範本
		registry.register("customers", CustomerTestModel.class, "客戶主檔");

		// Then: 以鍵值查詢
		ExcelTemplateRegistry.TemplateDescriptor desc = registry.getRequired("customers");
		assertThat(desc.modelClass()).isEqualTo(CustomerTestModel.class);
		assertThat(desc.sheetName()).isEqualTo("客戶主檔清單");
		assertThat(desc.feedbackFilename()).isEqualTo("客戶主檔檢核錯誤回標檔.xlsx");

		// Then: 以工作表名稱自動反查
		Optional<ExcelTemplateRegistry.TemplateDescriptor> bySheet = registry.findBySheetName("客戶主檔清單");
		assertThat(bySheet).isPresent();
		assertThat(bySheet.get().key()).isEqualTo("customers");
	}

	/**
	 * Feature: 未註冊範本之防呆與友善例外提示
	 * Scenario: 當客戶端傳入不存在之鍵值時，應拋出包含當前可用列表之清晰例外
	 * Given: 傳入 "unknown-template"
	 * When: 呼叫 getRequired("unknown-template")
	 * Then: 拋出 IllegalArgumentException 並列出當前所有已註冊列表
	 */
	@Test
	@DisplayName("情境：查詢未知範本時應拋出包含可用範本清單之友善例外訊息")
	void shouldThrowFriendlyExceptionWhenTemplateNotFound() {
		assertThatThrownBy(() -> registry.getRequired("unknown-template"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("不支援或未註冊之 Excel 範本鍵值")
				.hasMessageContaining("orders")
				.hasMessageContaining("products");
	}
}

