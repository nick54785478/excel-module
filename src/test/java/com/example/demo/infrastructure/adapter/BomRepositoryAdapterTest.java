package com.example.demo.infrastructure.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.domain.bom.aggregate.entity.BomItem;
import com.example.demo.domain.bom.aggregate.root.Bom;
import com.example.demo.domain.bom.aggregate.vo.BomId;
import com.example.demo.domain.bom.aggregate.vo.BomStatus;
import com.example.demo.domain.bom.aggregate.vo.BomVersion;
import com.example.demo.domain.bom.aggregate.vo.Quantity;
import com.example.demo.domain.bom.aggregate.vo.ScrapRate;
import com.example.demo.domain.material.aggregate.vo.MaterialCategory;
import com.example.demo.domain.material.aggregate.vo.MaterialId;

@SpringBootTest
@Transactional
@DisplayName("BomRepositoryAdapter BOM 儲存庫適配器持久化整合測試")
class BomRepositoryAdapterTest {

	@Autowired
	private BomRepositoryAdapter bomRepositoryAdapter;

	/**
	 * Feature: BOM 配方識別碼查詢
	 * Scenario: 成功查詢無人機 BOM-001 及其階層子項清單
	 * Given 初始化 SQL 載入 BOM00000-0000-0000-0000-000000000001 (商用無人機)
	 * When 調用 findById
	 * Then 應取得完整 BOM 聚合根，狀態為 APPROVED，且其內部包含 8 項第一階組成零件
	 */
	@Test
	@DisplayName("場景：依識別碼查詢既有 BOM 聚合根及其明細子項")
	void shouldFindBomByIdWithItemsSuccessfully() {
		// Given
		BomId id = BomId.of("BOM00000-0000-0000-0000-000000000001");

		// When
		Optional<Bom> opt = bomRepositoryAdapter.findById(id);

		// Then
		assertThat(opt).isPresent();
		Bom bom = opt.get();
		assertThat(bom.getParentMaterialId().value()).isEqualTo("00000000-0000-0000-0000-000000000001");
		assertThat(bom.getVersion().value()).isEqualTo("V1.0");
		assertThat(bom.getStatus()).isEqualTo(BomStatus.APPROVED);
		assertThat(bom.getBaseQuantity().value()).isEqualByComparingTo("1.0000");

		List<BomItem> items = bom.getItems();
		assertThat(items).hasSize(8);

		// 驗證第一個子項 (飛控總成)
		BomItem item1 = items.get(0);
		assertThat(item1.getChildMaterialId().value()).isEqualTo("00000000-0000-0000-0000-000000000016");
		assertThat(item1.getItemSequence().value()).isEqualTo(10);
		assertThat(item1.getQuantity().value()).isEqualByComparingTo("1.0000");

		// 驗證第 7 個子項 (螺絲，含 5% 損耗)
		BomItem screwItem = items.get(6);
		assertThat(screwItem.getScrapRate().rate()).isEqualByComparingTo("0.0500");
		assertThat(screwItem.getQuantity().value()).isEqualByComparingTo("24.0000");
		assertThat(screwItem.calculateEffectiveQuantity().value()).isEqualByComparingTo("25.2000");
	}

	/**
	 * Feature: 依母件與版本查詢 BOM
	 * Scenario: 依母件 ID 與版本 V1.0 查詢飛控主板之次階 BOM (Level 2)
	 * Given 飛控主板母件 ID
	 * When 調用 findByParentMaterialIdAndVersion
	 * Then 應返回包含 8 項電子零件之 BOM 聚合根
	 */
	@Test
	@DisplayName("場景：依母件物料與版本號查詢子階 BOM 配方")
	void shouldFindByParentMaterialIdAndVersionSuccessfully() {
		// Given
		MaterialId parentId = MaterialId.of("00000000-0000-0000-0000-000000000016");
		BomVersion version = BomVersion.of("V1.0");

		// When
		Optional<Bom> opt = bomRepositoryAdapter.findByParentMaterialIdAndVersion(parentId, version);

		// Then
		assertThat(opt).isPresent();
		Bom bom = opt.get();
		assertThat(bom.getDescription()).contains("飛控主板");
		assertThat(bom.getItems()).hasSize(8);
	}

	/**
	 * Feature: 新增與儲存 BOM 聚合根業務
	 * Scenario: 建立新草稿 BOM 並加入多個子件，保存後讀回驗證級聯持久化
	 * Given 成品母件 ID、版本 V2.0，並加入 2 筆子料件
	 * When 調用 bomRepositoryAdapter.save(newBom)
	 * Then BOM 與所有子項皆成功存入資料庫，且能重新完整載入
	 */
	@Test
	@DisplayName("場景：儲存新 BOM 聚合根並級聯保存所有子項實體")
	void shouldSaveAndRetrieveNewBomCascadeSuccessfully() {
		// Given
		MaterialId parentId = MaterialId.of("00000000-0000-0000-0000-000000000001");
		BomVersion version = BomVersion.of("V2.0");

		Bom bom = Bom.create(
				parentId,
				MaterialCategory.FINISHED_GOODS,
				version,
				"二代升級配方",
				Quantity.ONE
		);

		MaterialId child1 = MaterialId.of("00000000-0000-0000-0000-000000000016");
		MaterialId child2 = MaterialId.of("00000000-0000-0000-0000-000000000017");
		bom.addItem(child1, Quantity.of(1.0), ScrapRate.ZERO, "FC-UP", "升級版主控");
		bom.addItem(child2, Quantity.of(4.0), ScrapRate.ofPercent(2.0), "M1-4", "升級馬達");

		// When
		Bom saved = bomRepositoryAdapter.save(bom);

		// Then
		assertThat(saved).isNotNull();
		assertThat(saved.getId()).isEqualTo(bom.getId());

		Optional<Bom> retrieved = bomRepositoryAdapter.findById(saved.getId());
		assertThat(retrieved).isPresent();
		assertThat(retrieved.get().getVersion()).isEqualTo(version);
		assertThat(retrieved.get().getItems()).hasSize(2);

		// 檢查 existsByParentMaterialIdAndVersion
		assertThat(bomRepositoryAdapter.existsByParentMaterialIdAndVersion(parentId, version)).isTrue();

		// When - 刪除
		bomRepositoryAdapter.deleteById(saved.getId());
		assertThat(bomRepositoryAdapter.findById(saved.getId())).isEmpty();
	}
}

