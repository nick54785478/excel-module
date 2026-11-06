package com.example.demo.infrastructure.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.domain.material.aggregate.root.Material;
import com.example.demo.domain.material.aggregate.vo.MaterialCategory;
import com.example.demo.domain.material.aggregate.vo.MaterialCode;
import com.example.demo.domain.material.aggregate.vo.MaterialId;
import com.example.demo.domain.material.aggregate.vo.MaterialStatus;
import com.example.demo.domain.material.aggregate.vo.Money;
import com.example.demo.domain.material.aggregate.vo.UnitOfMeasure;

@SpringBootTest
@Transactional
@DisplayName("MaterialRepositoryAdapter 物料儲存庫適配器持久化整合測試")
class MaterialRepositoryAdapterTest {

	@Autowired
	private MaterialRepositoryAdapter materialRepositoryAdapter;

	/**
	 * Feature: 物料主檔識別碼查詢
	 * Scenario: 成功查詢初始化 SQL 載入之成品物料 MAT-FG-001
	 * Given 初始化資料庫已載入 MAT-FG-001 測試資料
	 * When 調用 findById("00000000-0000-0000-0000-000000000001")
	 * Then 應取得正確之 Material 領域聚合根，且屬性與品名相符
	 */
	@Test
	@DisplayName("場景：依識別碼查詢既有物料聚合根")
	void shouldFindMaterialByIdSuccessfully() {
		// Given
		MaterialId id = MaterialId.of("00000000-0000-0000-0000-000000000001");

		// When
		Optional<Material> opt = materialRepositoryAdapter.findById(id);

		// Then
		assertThat(opt).isPresent();
		Material material = opt.get();
		assertThat(material.getCode().value()).isEqualTo("MAT-FG-001");
		assertThat(material.getName()).isEqualTo("4K 專業商用巡檢無人機");
		assertThat(material.getCategory()).isEqualTo(MaterialCategory.FINISHED_GOODS);
		assertThat(material.getUnit()).isEqualTo(UnitOfMeasure.SET);
		assertThat(material.getStandardCost().amount()).isEqualByComparingTo("28500.0000");
		assertThat(material.getStatus()).isEqualTo(MaterialStatus.ACTIVE);
	}

	/**
	 * Feature: 物料料號查詢業務
	 * Scenario: 依料號 MAT-FG-002 查詢工業閘道器
	 * Given 系統存在料號 MAT-FG-002
	 * When 調用 findByCode(MaterialCode.of("MAT-FG-002"))
	 * Then 應成功返回該物料領域模型
	 */
	@Test
	@DisplayName("場景：依料號查詢物料聚合根")
	void shouldFindMaterialByCodeSuccessfully() {
		// Given
		MaterialCode code = MaterialCode.of("MAT-FG-002");

		// When
		Optional<Material> opt = materialRepositoryAdapter.findByCode(code);

		// Then
		assertThat(opt).isPresent();
		assertThat(opt.get().getName()).isEqualTo("工業級邊緣運算物聯網閘道器");
		assertThat(opt.get().getCategory()).isEqualTo(MaterialCategory.FINISHED_GOODS);
	}

	/**
	 * Feature: 物料主檔依分類統計查詢
	 * Scenario: 驗證初始化資料集中的分類數量分佈 (100 成品、400 半成品、1500 原料，共 2000 筆)
	 * Given 2000 筆物料主檔已載入資料庫
	 * When 分別依 FINISHED_GOODS, SEMI_FINISHED, RAW_MATERIAL 查詢
	 * Then 筆數應精確對應 100, 400, 1500 筆，且總數為 2000 筆
	 */
	@Test
	@DisplayName("場景：依分類查詢物料清單驗證資料集分佈")
	void shouldFindByCategoryCorrectly() {
		// When
		List<Material> fgList = materialRepositoryAdapter.findByCategory(MaterialCategory.FINISHED_GOODS);
		List<Material> sfgList = materialRepositoryAdapter.findByCategory(MaterialCategory.SEMI_FINISHED);
		List<Material> rmList = materialRepositoryAdapter.findByCategory(MaterialCategory.RAW_MATERIAL);
		List<Material> allList = materialRepositoryAdapter.findAll();

		// Then
		assertThat(fgList).hasSize(100);
		assertThat(sfgList).hasSize(400);
		assertThat(rmList).hasSize(1500);
		assertThat(allList).hasSize(2000);
	}

	/**
	 * Feature: 新增與儲存物料聚合根業務
	 * Scenario: 成功保存新建立之物料並自資料庫讀回驗證
	 * Given 透過 Material.create 工廠方法建立之新物料領域模型
	 * When 調用 materialRepositoryAdapter.save(newMaterial)
	 * Then 資料庫應成功持久化，且後續查詢可取得相同屬性
	 */
	@Test
	@DisplayName("場景：儲存新物料聚合根並查詢驗證")
	void shouldSaveAndRetrieveNewMaterialSuccessfully() {
		// Given
		MaterialCode code = MaterialCode.of("MAT-NEW-999");
		Material material = Material.create(
				code,
				"新測試感測模組",
				"TEST-SPEC-999",
				MaterialCategory.SEMI_FINISHED,
				UnitOfMeasure.PCS,
				Money.of(BigDecimal.valueOf(3500.50))
		);

		// When
		Material saved = materialRepositoryAdapter.save(material);

		// Then
		assertThat(saved).isNotNull();
		assertThat(saved.getId()).isEqualTo(material.getId());

		Optional<Material> retrieved = materialRepositoryAdapter.findById(saved.getId());
		assertThat(retrieved).isPresent();
		assertThat(retrieved.get().getCode()).isEqualTo(code);
		assertThat(retrieved.get().getName()).isEqualTo("新測試感測模組");
		assertThat(retrieved.get().getStandardCost().amount()).isEqualByComparingTo("3500.5000");

		// 檢查 existsByCode
		assertThat(materialRepositoryAdapter.existsByCode(code)).isTrue();

		// When - 刪除
		materialRepositoryAdapter.deleteById(saved.getId());
		assertThat(materialRepositoryAdapter.findById(saved.getId())).isEmpty();
	}
}

