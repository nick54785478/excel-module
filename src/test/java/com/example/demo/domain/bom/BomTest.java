package com.example.demo.domain.bom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.demo.domain.bom.aggregate.entity.BomItem;
import com.example.demo.domain.bom.aggregate.root.Bom;
import com.example.demo.domain.bom.aggregate.vo.BomStatus;
import com.example.demo.domain.bom.aggregate.vo.BomVersion;
import com.example.demo.domain.bom.aggregate.vo.Quantity;
import com.example.demo.domain.bom.aggregate.vo.ScrapRate;
import com.example.demo.domain.bom.event.BomApprovedEvent;
import com.example.demo.domain.bom.event.BomCreatedEvent;
import com.example.demo.domain.bom.event.BomItemAddedEvent;
import com.example.demo.domain.bom.event.BomItemRemovedEvent;
import com.example.demo.domain.bom.exception.CircularBomReferenceException;
import com.example.demo.domain.bom.exception.DuplicateBomItemException;
import com.example.demo.domain.bom.exception.IllegalBomOperationException;
import com.example.demo.domain.bom.exception.InvalidBomParentCategoryException;
import com.example.demo.domain.material.aggregate.vo.MaterialCategory;
import com.example.demo.domain.material.aggregate.vo.MaterialId;

@DisplayName("Bom 物料清單聚合根領域測試")
class BomTest {

	/**
	 * Feature: 物料清單聚合根建立業務
	 * Scenario: 成功為成品建立草稿狀態 BOM 並發布 BomCreatedEvent
	 * Given 成品母件 MaterialId 與成品分類 FINISHED_GOODS
	 * When 調用 Bom.create 工廠方法
	 * Then 初始狀態為 DRAFT，子項清單為空，發布 BomCreatedEvent
	 */
	@Test
	@DisplayName("場景：成功建立成品 BOM 並發布領域事件")
	void shouldCreateBomSuccessfullyForFinishedGoods() {
		// Given
		MaterialId parentId = MaterialId.generate();
		MaterialCategory category = MaterialCategory.FINISHED_GOODS;
		BomVersion version = BomVersion.of("V1.0");

		// When
		Bom bom = Bom.create(parentId, category, version, "無人機主配方", Quantity.ONE);

		// Then
		assertThat(bom.getId()).isNotNull();
		assertThat(bom.getParentMaterialId()).isEqualTo(parentId);
		assertThat(bom.getVersion()).isEqualTo(version);
		assertThat(bom.getStatus()).isEqualTo(BomStatus.DRAFT);
		assertThat(bom.getBaseQuantity().value()).isEqualByComparingTo("1.0000");
		assertThat(bom.getItems()).isEmpty();

		List<Object> events = bom.getDomainEvents();
		assertThat(events).hasSize(1);
		assertThat(events.get(0)).isInstanceOf(BomCreatedEvent.class);
		BomCreatedEvent event = (BomCreatedEvent) events.get(0);
		assertThat(event.parentMaterialId()).isEqualTo(parentId);
	}

	/**
	 * Feature: 母件物料分類檢核
	 * Scenario: 嘗試為原物料建立 BOM 時拋出 InvalidBomParentCategoryException
	 * Given 原物料分類 RAW_MATERIAL
	 * When 調用 Bom.create
	 * Then 拋出 InvalidBomParentCategoryException，阻斷非法配方建立
	 */
	@Test
	@DisplayName("場景：原物料嚴禁作為母件建立 BOM")
	void shouldFailWhenCreatingBomForRawMaterial() {
		// Given
		MaterialId rawMaterialId = MaterialId.generate();
		MaterialCategory category = MaterialCategory.RAW_MATERIAL;

		// When & Then
		assertThatThrownBy(() -> Bom.create(rawMaterialId, category, BomVersion.DEFAULT, "螺絲配方", Quantity.ONE))
				.isInstanceOf(InvalidBomParentCategoryException.class)
				.hasMessageContaining("原物料，嚴禁作為母件建立 BOM 清單");
	}

	/**
	 * Feature: BOM 子件新增與損耗率計算
	 * Scenario: 新增子件項目，項次序號依序遞增 (10, 20)，正確計算考量損耗率後之有效用量
	 * Given 草稿狀態之 BOM 聚合根
	 * When 分別加入子件 A (用量 2.0, 損耗率 5%) 與子件 B (用量 4.0, 損耗率 0%)
	 * Then 子件 A 序號為 10 且有效用量為 2.1000；子件 B 序號為 20 且有效用量為 4.0000；發布 BomItemAddedEvent
	 */
	@Test
	@DisplayName("場景：新增子料件並計算考量損耗率之有效需求量")
	void shouldAddItemWithSequenceAndCalculateEffectiveQuantity() {
		// Given
		Bom bom = Bom.create(
				MaterialId.generate(), MaterialCategory.FINISHED_GOODS,
				BomVersion.DEFAULT, "配方", Quantity.ONE
		);
		bom.clearDomainEvents();

		MaterialId childA = MaterialId.generate();
		MaterialId childB = MaterialId.generate();

		// When
		BomItem itemA = bom.addItem(
				childA,
				Quantity.of(2.0),
				ScrapRate.ofPercent(5.0),
				"R1, R2",
				"主控周邊電阻"
		);
		BomItem itemB = bom.addItem(
				childB,
				Quantity.of(4.0),
				ScrapRate.ZERO,
				"M1-M4",
				"無刷馬達"
		);

		// Then
		assertThat(bom.getItems()).hasSize(2);
		assertThat(itemA.getItemSequence().value()).isEqualTo(10);
		assertThat(itemA.calculateEffectiveQuantity().value()).isEqualByComparingTo("2.1000"); // 2 * 1.05 = 2.1

		assertThat(itemB.getItemSequence().value()).isEqualTo(20);
		assertThat(itemB.calculateEffectiveQuantity().value()).isEqualByComparingTo("4.0000");

		List<Object> events = bom.getDomainEvents();
		assertThat(events).hasSize(2);
		assertThat(events.get(0)).isInstanceOf(BomItemAddedEvent.class);
		assertThat(events.get(1)).isInstanceOf(BomItemAddedEvent.class);
	}

	/**
	 * Feature: 防直接循環引用檢核
	 * Scenario: 當子件料號與母件料號相同時拋出 CircularBomReferenceException
	 * Given 具有特定 MaterialId 之母件 BOM
	 * When 嘗試將該母件 MaterialId 作為子件加入
	 * Then 拋出 CircularBomReferenceException
	 */
	@Test
	@DisplayName("場景：子件不可為母件本身，防止直接循環引用")
	void shouldFailWhenAddingParentAsChildComponent() {
		// Given
		MaterialId parentId = MaterialId.generate();
		Bom bom = Bom.create(parentId, MaterialCategory.FINISHED_GOODS, BomVersion.DEFAULT, "配方", Quantity.ONE);

		// When & Then
		assertThatThrownBy(() -> bom.addItem(parentId, Quantity.of(1.0), ScrapRate.ZERO, null, null))
				.isInstanceOf(CircularBomReferenceException.class)
				.hasMessageContaining("不可作為自身 BOM 之子項");
	}

	/**
	 * Feature: 子料件唯一性檢核
	 * Scenario: 同一 BOM 內重複加入相同子件時拋出 DuplicateBomItemException
	 * Given 已包含子件 A 之 BOM
	 * When 再次嘗試將子件 A 加入清單
	 * Then 拋出 DuplicateBomItemException
	 */
	@Test
	@DisplayName("場景：重複加入相同子料件時拋出領域例外")
	void shouldFailWhenAddingDuplicateChildItem() {
		// Given
		Bom bom = Bom.create(MaterialId.generate(), MaterialCategory.FINISHED_GOODS, BomVersion.DEFAULT, "配方", Quantity.ONE);
		MaterialId childA = MaterialId.generate();
		bom.addItem(childA, Quantity.of(1.0), ScrapRate.ZERO, null, null);

		// When & Then
		assertThatThrownBy(() -> bom.addItem(childA, Quantity.of(2.0), ScrapRate.ZERO, null, null))
				.isInstanceOf(DuplicateBomItemException.class)
				.hasMessageContaining("已存在於此 BOM 清單中，不得重複加入");
	}

	/**
	 * Feature: BOM 子件移除業務
	 * Scenario: 自草稿狀態 BOM 移除子件並發布 BomItemRemovedEvent
	 * Given 包含 1 筆子件之 BOM
	 * When 調用 removeItem(itemId)
	 * Then 子件自清單移除，發布 BomItemRemovedEvent
	 */
	@Test
	@DisplayName("場景：移除子料件並發布領域事件")
	void shouldRemoveItemAndPublishEvent() {
		// Given
		Bom bom = Bom.create(MaterialId.generate(), MaterialCategory.FINISHED_GOODS, BomVersion.DEFAULT, "配方", Quantity.ONE);
		MaterialId childA = MaterialId.generate();
		BomItem item = bom.addItem(childA, Quantity.of(1.0), ScrapRate.ZERO, null, null);
		bom.clearDomainEvents();

		// When
		bom.removeItem(item.getId());

		// Then
		assertThat(bom.getItems()).isEmpty();
		List<Object> events = bom.getDomainEvents();
		assertThat(events).hasSize(1);
		assertThat(events.get(0)).isInstanceOf(BomItemRemovedEvent.class);
		BomItemRemovedEvent event = (BomItemRemovedEvent) events.get(0);
		assertThat(event.bomItemId()).isEqualTo(item.getId());
	}

	/**
	 * Feature: BOM 審核生效與結構凍結
	 * Scenario: 草稿 BOM 審核通過後狀態為 APPROVED，發布 BomApprovedEvent，且後續禁止異動
	 * Given 包含子件之草稿 BOM
	 * When 調用 approve()
	 * Then 狀態為 APPROVED，發布 BomApprovedEvent；後續嘗試 addItem 或 removeItem 拋出 IllegalBomOperationException
	 */
	@Test
	@DisplayName("場景：審核生效後進入凍結狀態，禁止子件異動")
	void shouldApproveBomAndFreezeModifications() {
		// Given
		Bom bom = Bom.create(MaterialId.generate(), MaterialCategory.FINISHED_GOODS, BomVersion.DEFAULT, "配方", Quantity.ONE);
		BomItem item = bom.addItem(MaterialId.generate(), Quantity.of(1.0), ScrapRate.ZERO, null, null);
		bom.clearDomainEvents();

		// When
		bom.approve();

		// Then
		assertThat(bom.getStatus()).isEqualTo(BomStatus.APPROVED);
		assertThat(bom.getStatus().isApproved()).isTrue();

		List<Object> events = bom.getDomainEvents();
		assertThat(events).hasSize(1);
		assertThat(events.get(0)).isInstanceOf(BomApprovedEvent.class);

		// When & Then - 已審核狀態嘗試新增
		assertThatThrownBy(() -> bom.addItem(MaterialId.generate(), Quantity.of(1.0), ScrapRate.ZERO, null, null))
				.isInstanceOf(IllegalBomOperationException.class)
				.hasMessageContaining("不允許執行 [新增子料件] 操作");

		// When & Then - 已審核狀態嘗試移除
		assertThatThrownBy(() -> bom.removeItem(item.getId()))
				.isInstanceOf(IllegalBomOperationException.class)
				.hasMessageContaining("不允許執行 [移除子料件] 操作");
	}

	/**
	 * Feature: 空 BOM 審核保護
	 * Scenario: 嘗試審核未包含任何子項之空清單時拋出 IllegalBomOperationException
	 * Given 未包含任何子項之草稿 BOM
	 * When 調用 approve()
	 * Then 拋出 IllegalBomOperationException
	 */
	@Test
	@DisplayName("場景：未包含任何子件之空清單嚴禁審核生效")
	void shouldFailWhenApprovingEmptyBom() {
		// Given
		Bom bom = Bom.create(MaterialId.generate(), MaterialCategory.FINISHED_GOODS, BomVersion.DEFAULT, "空配方", Quantity.ONE);

		// When & Then
		assertThatThrownBy(bom::approve)
				.isInstanceOf(IllegalBomOperationException.class)
				.hasMessageContaining("不得審核未包含任何子項零件之空 BOM 清單");
	}

	/**
	 * Feature: 多階層遞迴循環引用檢核
	 * Scenario: 檢核當前 BOM 之母件或子件是否存在於上層祖先母件鏈中
	 * Given 祖先母件集合包含物料 Ancestor-1
	 * When 當前 BOM 包含 Ancestor-1 作為子項，調用 checkCircularReference
	 * Then 拋出 CircularBomReferenceException 避免無限遞迴
	 */
	@Test
	@DisplayName("場景：檢測多階層循環引用並拋出領域例外")
	void shouldDetectMultiLevelCircularReference() {
		// Given
		MaterialId ancestorId = MaterialId.generate();
		MaterialId currentParentId = MaterialId.generate();

		Bom bom = Bom.create(currentParentId, MaterialCategory.SEMI_FINISHED, BomVersion.DEFAULT, "半成品配方", Quantity.ONE);
		bom.addItem(ancestorId, Quantity.of(1.0), ScrapRate.ZERO, null, null);

		Set<MaterialId> ancestorChain = Set.of(ancestorId);

		// When & Then
		assertThatThrownBy(() -> bom.checkCircularReference(ancestorChain))
				.isInstanceOf(CircularBomReferenceException.class)
				.hasMessageContaining("檢測到 BOM 循環引用");
	}
}

