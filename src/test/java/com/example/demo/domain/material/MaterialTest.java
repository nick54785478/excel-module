package com.example.demo.domain.material;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.demo.domain.material.aggregate.root.Material;
import com.example.demo.domain.material.aggregate.vo.MaterialCategory;
import com.example.demo.domain.material.aggregate.vo.MaterialCode;
import com.example.demo.domain.material.aggregate.vo.MaterialStatus;
import com.example.demo.domain.material.aggregate.vo.Money;
import com.example.demo.domain.material.aggregate.vo.UnitOfMeasure;
import com.example.demo.domain.material.event.MaterialCostChangedEvent;
import com.example.demo.domain.material.event.MaterialCreatedEvent;
import com.example.demo.domain.material.event.MaterialDeactivatedEvent;
import com.example.demo.domain.material.event.MaterialUpdatedEvent;
import com.example.demo.domain.material.exception.InvalidMaterialCodeException;
import com.example.demo.domain.material.exception.NegativeMaterialCostException;

@DisplayName("Material 物料聚合根領域測試")
class MaterialTest {

	/**
	 * Feature: 物料聚合根建立業務
	 * Scenario: 成功建立成品物料主檔並觸發發布 MaterialCreatedEvent
	 * Given 具備有效料號 MAT-FG-001、成品分類 FINISHED_GOODS、計量單位 PCS 與標準成本
	 * When 調用 Material.create 工廠方法
	 * Then 聚合根屬性初始化正確，狀態為 ACTIVE，發布 MaterialCreatedEvent，且 canHaveBom() 為 true
	 */
	@Test
	@DisplayName("場景：成功建立成品物料並發布領域事件")
	void shouldCreateMaterialSuccessfullyWithDomainEvent() {
		// Given
		MaterialCode code = MaterialCode.of("MAT-FG-001");
		String name = "智慧型無人機";
		String spec = "PRO-4K-GPS";
		MaterialCategory category = MaterialCategory.FINISHED_GOODS;
		UnitOfMeasure unit = UnitOfMeasure.PCS;
		Money cost = Money.of(BigDecimal.valueOf(25400));

		// When
		Material material = Material.create(code, name, spec, category, unit, cost);

		// Then
		assertThat(material.getId()).isNotNull();
		assertThat(material.getCode()).isEqualTo(code);
		assertThat(material.getName()).isEqualTo(name);
		assertThat(material.getSpecification()).isEqualTo(spec);
		assertThat(material.getCategory()).isEqualTo(MaterialCategory.FINISHED_GOODS);
		assertThat(material.getUnit()).isEqualTo(UnitOfMeasure.PCS);
		assertThat(material.getStandardCost().amount()).isEqualByComparingTo("25400.0000");
		assertThat(material.getStatus()).isEqualTo(MaterialStatus.ACTIVE);
		assertThat(material.canHaveBom()).isTrue();

		List<Object> events = material.getDomainEvents();
		assertThat(events).hasSize(1);
		assertThat(events.get(0)).isInstanceOf(MaterialCreatedEvent.class);
		MaterialCreatedEvent event = (MaterialCreatedEvent) events.get(0);
		assertThat(event.materialId()).isEqualTo(material.getId());
		assertThat(event.materialCode()).isEqualTo(code);
	}

	/**
	 * Feature: 物料料號格式檢核
	 * Scenario: 當料號含有無效特殊字元或長度不足時拋出 InvalidMaterialCodeException
	 * Given 含有無效格式字串 "invalid code!"
	 * When 嘗試建立 MaterialCode 實質物件
	 * Then 應拋出 InvalidMaterialCodeException
	 */
	@Test
	@DisplayName("場景：無效物料料號格式拋出領域例外")
	void shouldFailWhenMaterialCodeIsInvalid() {
		// Given & When & Then
		assertThatThrownBy(() -> MaterialCode.of("invalid code!"))
				.isInstanceOf(InvalidMaterialCodeException.class)
				.hasMessageContaining("物料料號格式無效");

		assertThatThrownBy(() -> MaterialCode.of("AB"))
				.isInstanceOf(InvalidMaterialCodeException.class);

		assertThatThrownBy(() -> MaterialCode.of("   "))
				.isInstanceOf(InvalidMaterialCodeException.class);
	}

	/**
	 * Feature: 物料標準成本檢核
	 * Scenario: 當成本為負數時拋出 NegativeMaterialCostException
	 * Given 負數金額 -500
	 * When 嘗試建立 Money 實質物件
	 * Then 應拋出 NegativeMaterialCostException
	 */
	@Test
	@DisplayName("場景：物料成本為負數時拋出領域例外")
	void shouldFailWhenStandardCostIsNegative() {
		// Given & When & Then
		assertThatThrownBy(() -> Money.of(BigDecimal.valueOf(-500)))
				.isInstanceOf(NegativeMaterialCostException.class)
				.hasMessageContaining("物料金額/成本不得為負數");
	}

	/**
	 * Feature: 物料規格資訊異動業務
	 * Scenario: 更新品名與規格型號並記錄 MaterialUpdatedEvent
	 * Given 既有物料聚合根
	 * When 調用 updateBasicInfo("無人機二代", "PRO-8K-GPS")
	 * Then 品名與規格成功更新，並記錄 MaterialUpdatedEvent
	 */
	@Test
	@DisplayName("場景：更新物料基本資訊並發布領域事件")
	void shouldUpdateBasicInfoAndPublishEvent() {
		// Given
		Material material = Material.create(
				MaterialCode.of("MAT-FG-001"), "舊名稱", "舊規格",
				MaterialCategory.FINISHED_GOODS, UnitOfMeasure.PCS, Money.of(1000)
		);
		material.clearDomainEvents();

		// When
		material.updateBasicInfo("無人機二代", "PRO-8K-GPS");

		// Then
		assertThat(material.getName()).isEqualTo("無人機二代");
		assertThat(material.getSpecification()).isEqualTo("PRO-8K-GPS");

		List<Object> events = material.getDomainEvents();
		assertThat(events).hasSize(1);
		assertThat(events.get(0)).isInstanceOf(MaterialUpdatedEvent.class);
		MaterialUpdatedEvent event = (MaterialUpdatedEvent) events.get(0);
		assertThat(event.materialName()).isEqualTo("無人機二代");
	}

	/**
	 * Feature: 物料標準成本調整業務
	 * Scenario: 調整成本並發布 MaterialCostChangedEvent
	 * Given 原成本為 1000 之物料
	 * When 調用 updateCost(1200)
	 * Then 標準成本更新為 1200，並發布 MaterialCostChangedEvent
	 */
	@Test
	@DisplayName("場景：調整標準成本並發布成本異動領域事件")
	void shouldUpdateCostAndPublishEvent() {
		// Given
		Material material = Material.create(
				MaterialCode.of("MAT-RM-301"), "主控晶片", "8-Core",
				MaterialCategory.RAW_MATERIAL, UnitOfMeasure.PCS, Money.of(1000)
		);
		material.clearDomainEvents();

		// When
		Money newCost = Money.of(1200);
		material.updateCost(newCost);

		// Then
		assertThat(material.getStandardCost().amount()).isEqualByComparingTo("1200.0000");

		List<Object> events = material.getDomainEvents();
		assertThat(events).hasSize(1);
		assertThat(events.get(0)).isInstanceOf(MaterialCostChangedEvent.class);
		MaterialCostChangedEvent event = (MaterialCostChangedEvent) events.get(0);
		assertThat(event.previousCost().amount()).isEqualByComparingTo("1000.0000");
		assertThat(event.newCost().amount()).isEqualByComparingTo("1200.0000");
	}

	/**
	 * Feature: 物料生命週期停用與啟用
	 * Scenario: 停用物料狀態變更為 DISCONTINUED 並發布 MaterialDeactivatedEvent
	 * Given ACTIVE 狀態之物料
	 * When 調用 discontinue()
	 * Then 狀態為 DISCONTINUED，發布停用事件；再調用 activate() 則回復 ACTIVE
	 */
	@Test
	@DisplayName("場景：停用與啟用物料生命週期狀態")
	void shouldDiscontinueAndActivateMaterial() {
		// Given
		Material material = Material.create(
				MaterialCode.of("MAT-SFG-101"), "主機板", "V1",
				MaterialCategory.SEMI_FINISHED, UnitOfMeasure.PCS, Money.of(5000)
		);
		material.clearDomainEvents();

		// When
		material.discontinue();

		// Then
		assertThat(material.getStatus()).isEqualTo(MaterialStatus.DISCONTINUED);
		assertThat(material.getStatus().isActive()).isFalse();

		List<Object> events = material.getDomainEvents();
		assertThat(events).hasSize(1);
		assertThat(events.get(0)).isInstanceOf(MaterialDeactivatedEvent.class);

		// When - 再次啟用
		material.activate();
		assertThat(material.getStatus()).isEqualTo(MaterialStatus.ACTIVE);
		assertThat(material.getStatus().isActive()).isTrue();
	}

	/**
	 * Feature: 物料分類與 BOM 關聯權限
	 * Scenario: 成品與半成品可建立 BOM，原物料嚴禁建立 BOM
	 * Given 三種不同物料分類
	 * When 檢查 canHaveBom()
	 * Then 成品與半成品回傳 true，原物料回傳 false
	 */
	@Test
	@DisplayName("場景：依據物料分類判斷是否可建立 BOM")
	void shouldDistinguishBomEligibilityByCategory() {
		// Given & When & Then
		assertThat(MaterialCategory.FINISHED_GOODS.canHaveBom()).isTrue();
		assertThat(MaterialCategory.SEMI_FINISHED.canHaveBom()).isTrue();
		assertThat(MaterialCategory.RAW_MATERIAL.canHaveBom()).isFalse();
	}
}

