package com.example.demo.presentation.assembler;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.demo.application.shared.model.BomItemExcelModel;
import com.example.demo.application.shared.model.MaterialExcelModel;
import com.example.demo.domain.bom.aggregate.root.Bom;
import com.example.demo.domain.bom.aggregate.vo.BomVersion;
import com.example.demo.domain.bom.aggregate.vo.Quantity;
import com.example.demo.domain.bom.aggregate.vo.ScrapRate;
import com.example.demo.domain.material.aggregate.root.Material;
import com.example.demo.domain.material.aggregate.vo.MaterialCategory;
import com.example.demo.domain.material.aggregate.vo.MaterialCode;
import com.example.demo.domain.material.aggregate.vo.MaterialId;
import com.example.demo.domain.material.aggregate.vo.Money;
import com.example.demo.domain.material.aggregate.vo.UnitOfMeasure;
import com.example.demo.presentation.resource.in.ExportBomItemResource;
import com.example.demo.presentation.resource.in.ExportMaterialResource;
import com.example.demo.presentation.resource.out.BomItemImportedResource;
import com.example.demo.presentation.resource.out.MaterialImportedResource;

/**
 * 物料與 BOM 表現層組裝器單元測試 (MaterialBomResourceAssembler Unit Test)
 * <p>
 * 遵循 BDD / Gherkin (Feature, Scenario, Given-When-Then) 語法規範，
 * 驗證防腐層組裝器於 Request/Response 與領域模型之間的轉換正確性。
 */
@DisplayName("MaterialBomResourceAssembler 物料與 BOM 資源組裝器測試")
class MaterialBomResourceAssemblerTest {

	private MaterialBomResourceAssembler assembler;

	private Material parentMaterial;
	private Material childMaterial;
	private Bom bom;

	@BeforeEach
	void setUp() {
		assembler = new MaterialBomResourceAssembler();

		parentMaterial = Material.create(
				MaterialCode.of("MAT-FG-001"),
				"4K 專業航拍無人機",
				"DRONE-4K-PRO",
				MaterialCategory.FINISHED_GOODS,
				UnitOfMeasure.PCS,
				Money.of(new BigDecimal("35000.00"))
		);

		childMaterial = Material.create(
				MaterialCode.of("MAT-SFG-001"),
				"4K 雲台相機模組",
				"CAM-4K-GIMBAL",
				MaterialCategory.SEMI_FINISHED,
				UnitOfMeasure.SET,
				Money.of(new BigDecimal("8500.00"))
		);

		bom = Bom.create(
				parentMaterial.getId(),
				parentMaterial.getCategory(),
				BomVersion.DEFAULT,
				"標準配方",
				Quantity.ONE
		);
		bom.addItem(childMaterial.getId(), Quantity.of(1), ScrapRate.of(new BigDecimal("0.02")), "CAM-01", "光學雲台");
	}

	/**
	 * Feature: 物料聚合根轉換為 Excel 映射模型
	 * Scenario: 成功將單筆與多筆 Material 聚合根轉換為 MaterialExcelModel
	 * Given: 建立物料聚合根
	 * When: 調用 toMaterialExcelModel 與 toMaterialExcelModelsFromDomain
	 * Then: 模型屬性應與聚合根完全一致
	 */
	@Test
	@DisplayName("場景：成功將 Material 聚合根轉換為 MaterialExcelModel")
	void shouldConvertMaterialToExcelModel() {
		// When
		MaterialExcelModel singleModel = assembler.toMaterialExcelModel(parentMaterial);
		List<MaterialExcelModel> listModels = assembler.toMaterialExcelModelsFromDomain(List.of(parentMaterial, childMaterial));

		// Then
		assertThat(singleModel).isNotNull();
		assertThat(singleModel.getMaterialCode()).isEqualTo("MAT-FG-001");
		assertThat(singleModel.getMaterialName()).isEqualTo("4K 專業航拍無人機");
		assertThat(singleModel.getCategory()).isEqualTo("FINISHED_GOODS");
		assertThat(singleModel.getUnit()).isEqualTo("PCS");
		assertThat(singleModel.getStandardCost()).isEqualByComparingTo("35000.00");

		assertThat(listModels).hasSize(2);
		assertThat(listModels.get(1).getMaterialCode()).isEqualTo("MAT-SFG-001");
		assertThat(listModels.get(1).getMaterialName()).isEqualTo("4K 雲台相機模組");
	}

	/**
	 * Feature: BOM 聚合根轉換為 Excel 映射模型
	 * Scenario: 成功將 BOM 聚合根及其子項轉為 BomItemExcelModel 清單
	 * Given: 建立 BOM 聚合根與物料對照 Map
	 * When: 調用 toBomItemExcelModelsFromDomain
	 * Then: 正確解析出子項之料號、名稱與用量
	 */
	@Test
	@DisplayName("場景：成功自 BOM 聚合根轉換為 BomItemExcelModel 清單")
	void shouldConvertBomToExcelModelsFromDomain() {
		// Given
		Map<MaterialId, Material> materialMap = Map.of(
				parentMaterial.getId(), parentMaterial,
				childMaterial.getId(), childMaterial
		);

		// When
		List<BomItemExcelModel> models = assembler.toBomItemExcelModelsFromDomain(List.of(bom), materialMap);

		// Then
		assertThat(models).hasSize(1);
		BomItemExcelModel model = models.get(0);
		assertThat(model.getParentMaterialCode()).isEqualTo("MAT-FG-001");
		assertThat(model.getChildMaterialCode()).isEqualTo("MAT-SFG-001");
		assertThat(model.getChildMaterialName()).isEqualTo("4K 雲台相機模組");
		assertThat(model.getUnit()).isEqualTo("SET");
		assertThat(model.getQuantity()).isEqualByComparingTo("1");
	}

	/**
	 * Feature: 物料匯入模型轉換為回應資源
	 * Scenario: 將 MaterialExcelModel 清單包裝為 MaterialImportedResource
	 * Given: 建立 MaterialExcelModel 清單
	 * When: 調用 toMaterialImportedResource
	 * Then: 回應資源結構完整且計數正確
	 */
	@Test
	@DisplayName("場景：成功將 MaterialExcelModel 轉換為 MaterialImportedResource")
	void shouldConvertToMaterialImportedResource() {
		// Given
		List<MaterialExcelModel> models = List.of(
				new MaterialExcelModel("M-01", "物料1", "SPEC-1", "RAW_MATERIAL", "PCS", new BigDecimal("100"), "ACTIVE")
		);

		// When
		MaterialImportedResource resource = assembler.toMaterialImportedResource(models, "物料測試表");

		// Then
		assertThat(resource).isNotNull();
		assertThat(resource.totalCount()).isEqualTo(1);
		assertThat(resource.sheetName()).isEqualTo("物料測試表");
		assertThat(resource.items()).hasSize(1);
		assertThat(resource.items().get(0).materialCode()).isEqualTo("M-01");
	}

	/**
	 * Feature: BOM 匯入模型轉換為回應資源
	 * Scenario: 將 BomItemExcelModel 清單包裝為 BomItemImportedResource
	 * Given: 建立 BomItemExcelModel 清單
	 * When: 調用 toBomItemImportedResource
	 * Then: 回應資源結構完整且計數正確
	 */
	@Test
	@DisplayName("場景：成功將 BomItemExcelModel 轉換為 BomItemImportedResource")
	void shouldConvertToBomItemImportedResource() {
		// Given
		List<BomItemExcelModel> models = List.of(
				new BomItemExcelModel("P-01", "1.0", 1, "C-01", "子料1", new BigDecimal("2"), "PCS", BigDecimal.ZERO, "POS1", "備註")
		);

		// When
		BomItemImportedResource resource = assembler.toBomItemImportedResource(models, "BOM測試表");

		// Then
		assertThat(resource).isNotNull();
		assertThat(resource.totalCount()).isEqualTo(1);
		assertThat(resource.sheetName()).isEqualTo("BOM測試表");
		assertThat(resource.items()).hasSize(1);
		assertThat(resource.items().get(0).parentMaterialCode()).isEqualTo("P-01");
	}
}

