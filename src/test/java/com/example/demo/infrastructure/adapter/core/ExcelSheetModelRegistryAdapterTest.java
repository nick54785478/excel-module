package com.example.demo.infrastructure.adapter.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.demo.application.port.out.core.ExcelSheetModelRegistryPort;
import com.example.demo.application.shared.model.BomItemExcelModel;
import com.example.demo.application.shared.model.MaterialExcelModel;
import com.example.demo.application.shared.model.MultiLevelBomExcelModel;
import com.example.demo.config.ExcelSheetModelConfiguration;

/**
 * 宣告式 Excel 模型註冊適配器測試 (ExcelSheetModelRegistryAdapter Test)
 * <p>
 * <b>【架構定位與測試職責】</b><br>
 * 遵循 BDD 與 Gherkin 規範，驗證 {@link ExcelSheetModelRegistryAdapter}
 * 與 {@link ExcelSheetModelRegistryPort} 輸出埠對於 Excel 工作表名稱、別名索引、模糊比對與子字串智慧路由能力。
 * </p>
 */
@DisplayName("ExcelSheetModelRegistryAdapter 宣告式模型註冊與路由查詢測試")
class ExcelSheetModelRegistryAdapterTest {

	private ExcelSheetModelRegistryAdapter registryAdapter;

	@BeforeEach
	void setUp() {
		// 透過配置工廠建立預載核心模型之註冊表
		registryAdapter = ExcelSheetModelConfiguration.createDefaultRegistry();
	}

	/**
	 * Feature: 精確工作表名稱查找模型
	 * Scenario: 使用 @ExcelSheet 主名稱查詢對應 Java 模型
	 * Given: 註冊表中已預載 MaterialExcelModel
	 * When: 查詢 "物料主檔清單"
	 * Then: 成功返回 MaterialExcelModel.class
	 */
	@Test
	@DisplayName("場景：使用 @ExcelSheet 主名稱成功查得模型類別")
	void shouldFindModelByExactName() {
		// When
		Optional<Class<?>> result = registryAdapter.findModelClass("物料主檔清單");

		// Then
		assertThat(result).isPresent().contains(MaterialExcelModel.class);
	}

	/**
	 * Feature: 多別名 (Aliases) 查找模型
	 * Scenario: 使用已配置之各種別名 (Aliases) 查詢對應模型
	 * Given: 預載物料主檔、單階 BOM 與多階層 BOM 模型
	 * When: 分別以別名 "物料主檔對照表"、"單階BOM匯入清單"、"多階層BOM展開清單" 查詢
	 * Then: 皆能精確命中對應的模型類別
	 */
	@Test
	@DisplayName("場景：使用 @ExcelSheet aliases 別名精確命中各業務模型")
	void shouldFindModelByAliases() {
		// When & Then
		assertThat(registryAdapter.findModelClass("物料主檔對照表")).contains(MaterialExcelModel.class);
		assertThat(registryAdapter.findModelClass("單階BOM匯入清單")).contains(BomItemExcelModel.class);
		assertThat(registryAdapter.findModelClass("多階層BOM展開清單")).contains(MultiLevelBomExcelModel.class);
		assertThat(registryAdapter.findModelClass("BOM配方")).contains(BomItemExcelModel.class);
	}

	/**
	 * Feature: 不區分大小寫與前後空白寬容查找
	 * Scenario: 傳入含有空白或大寫英文字串之別名
	 * Given: 註冊表中包含 Materials 與 BOM 等別名
	 * When: 查詢 "  MATERIALS  " 或 "  bom  "
	 * Then: 自動去除前後空白並忽略大小寫，正確解析為對應模型
	 */
	@Test
	@DisplayName("場景：不區分大小寫且自動去除前後空白成功查得模型")
	void shouldFindModelCaseInsensitiveAndTrimmed() {
		// When & Then
		assertThat(registryAdapter.findModelClass("  MATERIALS  ")).contains(MaterialExcelModel.class);
		assertThat(registryAdapter.findModelClass("  bom  ")).contains(BomItemExcelModel.class);
	}

	/**
	 * Feature: 符號與空白寬鬆模糊比對 (Fuzzy Simplified Match)
	 * Scenario: 工作表名稱中含有破折號、底線或空格
	 * Given: 註冊表中包含別名 "多階層BOM展開清單"
	 * When: 查詢 "多階層 BOM 展開清單"
	 * Then: 寬鬆去除空格後成功命中 MultiLevelBomExcelModel.class
	 */
	@Test
	@DisplayName("場景：符號與空格寬鬆模糊比對成功匹配")
	void shouldFindModelFuzzySimplified() {
		// When & Then
		assertThat(registryAdapter.findModelClass("多階層 BOM 展開清單")).contains(MultiLevelBomExcelModel.class);
	}

	/**
	 * Feature: 智慧包含比對 (Smart Substring Matching)
	 * Scenario: 上傳之 Excel 工作表含有年份前綴或括號備註 (如 "2026年多階層BOM展開清單")
	 * Given: 註冊表中已定義長別名 "多階層BOM展開清單"
	 * When: 查詢包含該關鍵字之延伸工作表名稱
	 * Then: 降冪優先匹配最長最精確別名，成功解析為 MultiLevelBomExcelModel.class
	 */
	@Test
	@DisplayName("場景：智慧子字串包含比對成功識別包含前綴或備註的工作表名稱")
	void shouldFindModelBySubstringMatch() {
		// When & Then
		assertThat(registryAdapter.findModelClass("2026年多階層BOM展開清單(最新)"))
				.contains(MultiLevelBomExcelModel.class);
		assertThat(registryAdapter.findModelClass("廠區A_物料主檔對照表"))
				.contains(MaterialExcelModel.class);
	}

	/**
	 * Feature: 未知工作表防禦查尋
	 * Scenario: 查詢完全不存在之工作表名稱
	 * Given: 註冊表已初始化
	 * When: 查詢 "財務損益表2026" 或 null 或空白
	 * Then: 返回 Optional.empty()
	 */
	@Test
	@DisplayName("場景：查尋未知工作表名稱安全回傳 Optional.empty")
	void shouldReturnEmptyForUnknownOrBlankSheet() {
		// When & Then
		assertThat(registryAdapter.findModelClass("財務損益表2026")).isEmpty();
		assertThat(registryAdapter.findModelClass(null)).isEmpty();
		assertThat(registryAdapter.findModelClass("   ")).isEmpty();
	}

	/**
	 * Feature: 取得必要模型 (getRequiredModelClass) 異常防禦
	 * Scenario: 調用 getRequiredModelClass 但查無模型
	 * Given: 註冊表已初始化
	 * When: 調用 getRequiredModelClass("未知表")
	 * Then: 拋出 IllegalArgumentException 說明工作表未支援
	 */
	@Test
	@DisplayName("異常：調用 getRequiredModelClass 查無模型時拋出 IllegalArgumentException")
	void shouldThrowExceptionWhenRequiredModelNotFound() {
		// When & Then
		assertThatThrownBy(() -> registryAdapter.getRequiredModelClass("不存在的工作表"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("未支援或未註冊之 Excel 工作表名稱: [不存在的工作表]");
	}

	/**
	 * Feature: 註冊清單總覽與支援檢查 (supports & getAllModelClasses)
	 * Scenario: 檢查註冊表狀態
	 * Given: 註冊表已加載核心模型
	 * When: 調用 supports() 與 getAllModelClasses()
	 * Then: supports() 正確回傳布林值，getAllModelClasses() 包含不重複之核心模型
	 */
	@Test
	@DisplayName("場景：supports 與 getAllModelClasses 狀態檢核正確無重複")
	void shouldVerifySupportsAndGetAllModelClasses() {
		// When & Then
		assertThat(registryAdapter.supports("物料主檔對照表")).isTrue();
		assertThat(registryAdapter.supports("幽靈工作表")).isFalse();

		List<Class<?>> models = registryAdapter.getAllModelClasses();
		assertThat(models).contains(
				MaterialExcelModel.class,
				BomItemExcelModel.class,
				MultiLevelBomExcelModel.class
		);
	}
}

