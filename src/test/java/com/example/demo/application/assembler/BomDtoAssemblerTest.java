package com.example.demo.application.assembler;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.demo.application.shared.command.ExportBomCommand;
import com.example.demo.application.shared.model.bom.BomItemExcelModel;
import com.example.demo.application.shared.model.material.MaterialExcelModel;
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
import com.example.demo.domain.material.exception.MaterialNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 物料與 BOM 資料組裝器單元測試 (BomDtoAssembler Test)
 * <p>
 * 遵循 BDD / Gherkin 規範，針對 {@link BomDtoAssembler} 進行多階層樹狀展開、
 * 單階清單建構、物料主檔建構以及宣告式模型轉換之單元驗證。
 */
@DisplayName("BomDtoAssembler 物料與 BOM 資料組裝器測試")
class BomDtoAssemblerTest {

	private Material parentMaterial;
	private Material childMaterial;
	private Bom bom;

	@BeforeEach
	void setUp() {
		this.parentMaterial = Material.create(
				MaterialCode.of("MAT-FG-001"),
				"無人機總成",
				"UAV-450",
				MaterialCategory.FINISHED_GOODS,
				UnitOfMeasure.SET,
				Money.of(new BigDecimal("20000.00"))
		);

		this.childMaterial = Material.create(
				MaterialCode.of("MAT-RM-001"),
				"碳纖維螺旋槳",
				"PROP-9X4",
				MaterialCategory.RAW_MATERIAL,
				UnitOfMeasure.PCS,
				Money.of(new BigDecimal("350.00"))
		);

		this.bom = Bom.create(
				parentMaterial.getId(),
				parentMaterial.getCategory(),
				BomVersion.of("V1.0"),
				"四軸飛行器總成配方",
				Quantity.ONE
		);
		this.bom.addItem(
				childMaterial.getId(),
				Quantity.of(new BigDecimal("4")),
				ScrapRate.of(new BigDecimal("0.02")),
				"P1-P4",
				"四軸螺旋槳"
		);
		this.bom.approve();
	}

	/**
	 * Feature: 檔名解析邏輯
	 * Scenario: 依據 ExportBomCommand 是否提供母件料號與版本號，產製適當的檔案名稱
	 * Given: 不同的 ExportBomCommand 設定
	 * When: 呼叫 BomDtoAssembler.resolveFileName
	 * Then: 產出正確格式化之檔名
	 */
	@Test
	@DisplayName("場景：檔案名稱解析正確性")
	void shouldResolveFileNameCorrectly() {
		ExportBomCommand cmdWithVersion = new ExportBomCommand("MAT-001", "2.0", true, true, true);
		assertThat(BomDtoAssembler.resolveFileName(cmdWithVersion)).isEqualTo("BOM_MAT-001_2.0.xlsx");

		ExportBomCommand cmdWithoutCode = new ExportBomCommand(null, null, true, true, true);
		assertThat(BomDtoAssembler.resolveFileName(cmdWithoutCode)).isEqualTo("BOM_LIST_ALL.xlsx");
	}

	/**
	 * Feature: 目標 BOM 篩選邏輯
	 * Scenario: 依據母件料號自索引庫篩選目標 BOM
	 * Given: 具備母件索引之 Map
	 * When: 呼叫 resolveTargetBoms 帶入存在料號與不存在料號
	 * Then: 存在料號正確返回清單，不存在料號拋出 MaterialNotFoundException
	 */
	@Test
	@DisplayName("場景：篩選目標 BOM 成功與例外分支")
	void shouldResolveTargetBoms() {
		Map<String, Material> codeMap = Map.of(parentMaterial.getCode().value(), parentMaterial);
		Map<MaterialId, List<Bom>> bomsByParentId = Map.of(parentMaterial.getId(), List.of(bom));
		List<Bom> allBoms = List.of(bom);

		ExportBomCommand validCmd = new ExportBomCommand("MAT-FG-001", "V1.0", true, true, true);
		List<Bom> resolved = BomDtoAssembler.resolveTargetBoms(validCmd, codeMap, bomsByParentId, allBoms);
		assertThat(resolved).hasSize(1);

		ExportBomCommand invalidCmd = new ExportBomCommand("UNKNOWN", "V1.0", true, true, true);
		assertThatThrownBy(() -> BomDtoAssembler.resolveTargetBoms(invalidCmd, codeMap, bomsByParentId, allBoms))
				.isInstanceOf(MaterialNotFoundException.class);
	}

	/**
	 * Feature: 多階層展開列資料建構
	 * Scenario: 正確產製含 Level 0 總成與 Level 1 子件之多階展開二維列
	 * Given: 目標 BOM 與物料索引
	 * When: 呼叫 buildMultiLevelRows
	 * Then: 返回 2 筆列資料，Level 0 與 .1 子項屬性完整
	 */
	@Test
	@DisplayName("場景：成功建構多階層展開列資料")
	void shouldBuildMultiLevelRows() {
		Map<MaterialId, Material> idMap = Map.of(
				parentMaterial.getId(), parentMaterial,
				childMaterial.getId(), childMaterial
		);
		Map<MaterialId, List<Bom>> bomsByParentId = Map.of(parentMaterial.getId(), List.of(bom));

		List<List<Object>> rows = BomDtoAssembler.buildMultiLevelRows(List.of(bom), idMap, bomsByParentId);
		assertThat(rows).hasSize(2);
		assertThat(rows.get(0).get(0)).isEqualTo("0");
		assertThat(rows.get(0).get(1)).isEqualTo("MAT-FG-001");
		assertThat(rows.get(1).get(0)).isEqualTo(".1");
		assertThat(rows.get(1).get(1)).isEqualTo("MAT-RM-001");
	}

	/**
	 * Feature: 宣告式 Model 轉換
	 * Scenario: 轉換 Material 與 Bom 聚合根至 Excel 映射 Model
	 * Given: 聚合根實例
	 * When: 呼叫 toMaterialExcelModels 與 toBomItemExcelModels
	 * Then: 模型欄位數值完全吻合
	 */
	@Test
	@DisplayName("場景：宣告式 Model 轉換正確性")
	void shouldConvertToExcelModels() {
		List<MaterialExcelModel> matModels = BomDtoAssembler.toMaterialExcelModels(List.of(parentMaterial));
		assertThat(matModels).hasSize(1);
		assertThat(matModels.get(0).getMaterialCode()).isEqualTo("MAT-FG-001");

		Map<MaterialId, Material> idMap = Map.of(
				parentMaterial.getId(), parentMaterial,
				childMaterial.getId(), childMaterial
		);
		List<BomItemExcelModel> bomModels = BomDtoAssembler.toBomItemExcelModels(List.of(bom), idMap);
		assertThat(bomModels).hasSize(1);
		assertThat(bomModels.get(0).getParentMaterialCode()).isEqualTo("MAT-FG-001");
		assertThat(bomModels.get(0).getChildMaterialCode()).isEqualTo("MAT-RM-001");
	}
}
