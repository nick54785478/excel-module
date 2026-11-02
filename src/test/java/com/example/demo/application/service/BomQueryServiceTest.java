package com.example.demo.application.service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.application.port.out.BomRepositoryPort;
import com.example.demo.application.port.out.ExcelWriterPort;
import com.example.demo.application.port.out.MaterialRepositoryPort;
import com.example.demo.application.shared.command.ExportBomCommand;
import com.example.demo.application.shared.command.WriteExcelCommand;
import com.example.demo.application.shared.command.WriteStreamExcelCommand;
import com.example.demo.application.shared.dto.BomExcelGottenResult;
import com.example.demo.domain.bom.aggregate.root.Bom;
import com.example.demo.domain.bom.aggregate.vo.BomVersion;
import com.example.demo.domain.bom.aggregate.vo.Quantity;
import com.example.demo.domain.bom.aggregate.vo.ScrapRate;
import com.example.demo.domain.material.aggregate.root.Material;
import com.example.demo.domain.material.aggregate.vo.MaterialCategory;
import com.example.demo.domain.material.aggregate.vo.MaterialCode;
import com.example.demo.domain.material.aggregate.vo.Money;
import com.example.demo.domain.material.aggregate.vo.UnitOfMeasure;
import com.example.demo.domain.material.exception.MaterialNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 物料與 BOM 查詢應用服務單元測試 (BomQueryService Test)
 * <p>
 * 針對 {@link BomQueryService} 驗證多階展開、單階平鋪、物料主檔對照表查詢、大數據串流輸出與宣告式物件映射之業務邏輯。
 * 所有測試案例嚴格遵循 BDD / Gherkin (Feature, Scenario, Given-When-Then) 語法規範。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BomQueryService 物料與 BOM 查詢服務測試")
class BomQueryServiceTest {

	@Mock
	private BomRepositoryPort bomRepositoryPort;

	@Mock
	private MaterialRepositoryPort materialRepositoryPort;

	@Mock
	private ExcelWriterPort excelWriterPort;

	private BomQueryService queryService;

	private Material parentMaterial;
	private Material subAssemblyMaterial;
	private Material rawMaterial;
	private Bom parentBom;
	private Bom subAssemblyBom;

	@BeforeEach
	void setUp() {
		this.queryService = new BomQueryService(
				bomRepositoryPort,
				materialRepositoryPort,
				excelWriterPort
		);

		// 準備物料主檔
		this.parentMaterial = Material.create(
				MaterialCode.of("MAT-FG-001"),
				"無人機成品",
				"UAV-450",
				MaterialCategory.FINISHED_GOODS,
				UnitOfMeasure.SET,
				Money.of(new BigDecimal("15000.00"))
		);

		this.subAssemblyMaterial = Material.create(
				MaterialCode.of("MAT-SA-001"),
				"飛控模組",
				"FC-H7",
				MaterialCategory.SEMI_FINISHED,
				UnitOfMeasure.SET,
				Money.of(new BigDecimal("3500.00"))
		);

		this.rawMaterial = Material.create(
				MaterialCode.of("MAT-RM-001"),
				"MCU晶片",
				"STM32H743",
				MaterialCategory.RAW_MATERIAL,
				UnitOfMeasure.PCS,
				Money.of(new BigDecimal("450.00"))
		);

		// 準備 BOM 配方 (MAT-FG-001 -> MAT-SA-001 -> MAT-RM-001)
		this.parentBom = Bom.create(
				parentMaterial.getId(),
				parentMaterial.getCategory(),
				BomVersion.of("V1.0"),
				"旗艦成品配方",
				Quantity.ONE
		);
		this.parentBom.addItem(
				subAssemblyMaterial.getId(),
				Quantity.of(BigDecimal.ONE),
				ScrapRate.ZERO,
				"U1",
				"主飛控總成"
		);
		this.parentBom.approve();

		this.subAssemblyBom = Bom.create(
				subAssemblyMaterial.getId(),
				subAssemblyMaterial.getCategory(),
				BomVersion.of("V1.0"),
				"飛控模組配方",
				Quantity.ONE
		);
		this.subAssemblyBom.addItem(
				rawMaterial.getId(),
				Quantity.of(BigDecimal.ONE),
				ScrapRate.of(new BigDecimal("0.05")),
				"U101",
				"主運算核心"
		);
		this.subAssemblyBom.approve();
	}

	/**
	 * Feature: 多階層 BOM 展開與 Excel 查詢產製
	 * Scenario: 指定母件料號查詢產製包含多階展開、單階平鋪與物料主檔三張工作表之 Excel
	 * Given: 儲存庫存在完整成品與半成品 BOM，且 ExcelWriterPort 正常產製二進位陣列
	 * When: 呼叫 queryService.queryBom(command)
	 * Then: 成功返回 BomExcelGottenResult，工作表數量為 3，檔名符合 BOM_MAT-FG-001_V1.0.xlsx
	 */
	@Test
	@DisplayName("成功：依指定母件料號與版本查詢完整三張工作表 BOM Excel")
	void shouldQueryBomWithAllSheetsSuccessfully() {
		// Given
		ExportBomCommand command = new ExportBomCommand("MAT-FG-001", "V1.0", true, true, true);
		byte[] mockBytes = "MOCK_EXCEL_BYTES".getBytes();

		when(materialRepositoryPort.findAll()).thenReturn(List.of(parentMaterial, subAssemblyMaterial, rawMaterial));
		when(bomRepositoryPort.findAll()).thenReturn(List.of(parentBom, subAssemblyBom));
		when(excelWriterPort.write(any(WriteExcelCommand.class))).thenReturn(mockBytes);

		// When
		BomExcelGottenResult result = queryService.queryBom(command);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.fileName()).isEqualTo("BOM_MAT-FG-001_V1.0.xlsx");
		assertThat(result.totalSheets()).isEqualTo(3);
		assertThat(result.fileBytes()).isEqualTo(mockBytes);

		ArgumentCaptor<WriteExcelCommand> captor = ArgumentCaptor.forClass(WriteExcelCommand.class);
		verify(excelWriterPort).write(captor.capture());
		WriteExcelCommand capturedCommand = captor.getValue();
		assertThat(capturedCommand.getSheets()).hasSize(3);
		assertThat(capturedCommand.getSheets().get(0).getSheetName()).isEqualTo("多階層BOM展開清單");
		assertThat(capturedCommand.getSheets().get(1).getSheetName()).isEqualTo("單階BOM匯入清單");
		assertThat(capturedCommand.getSheets().get(2).getSheetName()).isEqualTo("物料主檔對照表");
	}

	/**
	 * Feature: 指定不存在之母件料號時拋出例外
	 * Scenario: 前端傳入未登錄之母件料號進行 BOM 查詢
	 * Given: 儲存庫查無該料號對應之物料主檔
	 * When: 呼叫 queryService.queryBom(command)
	 * Then: 拋出 MaterialNotFoundException 領域例外
	 */
	@Test
	@DisplayName("失敗：指定不存在之母件料號應拋出 MaterialNotFoundException")
	void shouldThrowExceptionWhenParentMaterialNotFound() {
		// Given
		ExportBomCommand command = new ExportBomCommand("NOT_EXIST_MAT", "1.0", true, false, false);
		when(materialRepositoryPort.findAll()).thenReturn(List.of(parentMaterial, subAssemblyMaterial));
		when(bomRepositoryPort.findAll()).thenReturn(List.of(parentBom));

		// When & Then
		assertThatThrownBy(() -> queryService.queryBom(command))
				.isInstanceOf(MaterialNotFoundException.class)
				.hasMessageContaining("查無指定料號之物料主檔: NOT_EXIST_MAT");
	}

	/**
	 * Feature: 大數據串流查詢物料清單 (BOM) Excel
	 * Scenario: 呼叫 streamQueryBom 邊產製邊寫入 OutputStream
	 * Given: 儲存庫存在完整 BOM 資料
	 * When: 呼叫 queryService.streamQueryBom(baos, command)
	 * Then: 成功調用 excelWriterPort.writeStream(...) 且包含 WriteStreamExcelCommand
	 */
	@Test
	@DisplayName("成功：透過 streamQueryBom 執行大數據串流輸出")
	void shouldStreamQueryBomSuccessfully() {
		// Given
		ExportBomCommand command = new ExportBomCommand("MAT-FG-001", "V1.0", true, true, true, 500);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		when(materialRepositoryPort.findAll()).thenReturn(List.of(parentMaterial, subAssemblyMaterial, rawMaterial));
		when(bomRepositoryPort.findAll()).thenReturn(List.of(parentBom, subAssemblyBom));

		// When
		queryService.streamQueryBom(baos, command);

		// Then
		ArgumentCaptor<WriteStreamExcelCommand> captor = ArgumentCaptor.forClass(WriteStreamExcelCommand.class);
		verify(excelWriterPort).writeStream(any(OutputStream.class), captor.capture());
		WriteStreamExcelCommand captured = captor.getValue();
		assertThat(captured.getSheets()).hasSize(3);
		assertThat(captured.getSheets().get(0).getFlushInterval()).isEqualTo(500);
	}

	// =========================================================================
	// 宣告式物件映射模組測試案例 (Declarative Bean Mapping Tests)
	// =========================================================================

	/**
	 * Feature: 【宣告式物件映射模組】物料聚合根宣告式 Excel 匯出
	 * Scenario: 成功查詢物料儲存庫並宣告式匯出為 Excel 二進位檔案
	 * Given: 儲存庫存在物料聚合根，且 ExcelWriterPort 正常產製二進位陣列
	 * When: 調用 queryService.exportMaterials()
	 * Then: 應取得二進位檔案資料，且 WriteExcelCommand 中包含正確之資料列與欄位
	 */
	@Test
	@DisplayName("場景：【宣告式物件映射】成功自物料聚合根匯出物料主檔 Excel")
	void shouldExportMaterialsUsingDeclarativeMappingSuccessfully() {
		// Given
		byte[] mockBytes = "EXCEL_MATERIAL_BYTES".getBytes();
		when(materialRepositoryPort.findAll()).thenReturn(List.of(parentMaterial, subAssemblyMaterial));
		when(excelWriterPort.write(any(WriteExcelCommand.class))).thenReturn(mockBytes);

		// When
		byte[] result = queryService.exportMaterials();

		// Then
		assertThat(result).isEqualTo(mockBytes);

		ArgumentCaptor<WriteExcelCommand> captor = ArgumentCaptor.forClass(WriteExcelCommand.class);
		verify(excelWriterPort).write(captor.capture());
		WriteExcelCommand command = captor.getValue();

		assertThat(command.getSheets()).hasSize(1);
		assertThat(command.getSheets().get(0).getSheetName()).isEqualTo("物料主檔清單");
		assertThat(command.getSheets().get(0).getRows()).hasSize(2);
		assertThat(command.getSheets().get(0).getRows().get(0).get(0)).isEqualTo("MAT-FG-001");
		assertThat(command.getSheets().get(0).getRows().get(0).get(1)).isEqualTo("無人機成品");
	}

	/**
	 * Feature: 【宣告式物件映射模組】BOM 聚合根宣告式 Excel 匯出
	 * Scenario: 成功查詢 BOM 與物料儲存庫並宣告式匯出為單階 BOM 配方 Excel
	 * Given: 儲存庫存在 BOM 與關聯物料聚合根，且 ExcelWriterPort 正常產製二進位陣列
	 * When: 調用 queryService.exportBomItems()
	 * Then: 應取得二進位檔案資料，且 WriteExcelCommand 中包含正確之 BOM 子項資料列
	 */
	@Test
	@DisplayName("場景：【宣告式物件映射】成功自 BOM 聚合根匯出單階 BOM 配方 Excel")
	void shouldExportBomItemsUsingDeclarativeMappingSuccessfully() {
		// Given
		byte[] mockBytes = "EXCEL_BOM_BYTES".getBytes();
		when(bomRepositoryPort.findAll()).thenReturn(List.of(parentBom, subAssemblyBom));
		when(materialRepositoryPort.findAll()).thenReturn(List.of(parentMaterial, subAssemblyMaterial, rawMaterial));
		when(excelWriterPort.write(any(WriteExcelCommand.class))).thenReturn(mockBytes);

		// When
		byte[] result = queryService.exportBomItems();

		// Then
		assertThat(result).isEqualTo(mockBytes);

		ArgumentCaptor<WriteExcelCommand> captor = ArgumentCaptor.forClass(WriteExcelCommand.class);
		verify(excelWriterPort).write(captor.capture());
		WriteExcelCommand command = captor.getValue();

		assertThat(command.getSheets()).hasSize(1);
		assertThat(command.getSheets().get(0).getSheetName()).isEqualTo("單階BOM清單");
		assertThat(command.getSheets().get(0).getRows()).hasSize(2);
		assertThat(command.getSheets().get(0).getRows().get(0).get(0)).isEqualTo("MAT-FG-001");
		assertThat(command.getSheets().get(0).getRows().get(1).get(0)).isEqualTo("MAT-SA-001");
	}

	/**
	 * Feature: 【宣告式物件映射模組】儲存庫無資料時防禦處理
	 * Scenario: 當儲存庫為空時仍能正常產製含表頭之空 Excel
	 * Given: 儲存庫回傳空清單
	 * When: 調用 queryService.exportMaterials() 與 queryService.exportBomItems()
	 * Then: 應正常呼叫 ExcelWriterPort，且不拋出異常
	 */
	@Test
	@DisplayName("場景：【宣告式物件映射】儲存庫為空時正常產出空工作表 Excel")
	void shouldHandleEmptyDeclarativeMaterialsAndBoms() {
		// Given
		byte[] mockBytes = new byte[0];
		when(materialRepositoryPort.findAll()).thenReturn(List.of());
		when(bomRepositoryPort.findAll()).thenReturn(List.of());
		when(excelWriterPort.write(any(WriteExcelCommand.class))).thenReturn(mockBytes);

		// When
		byte[] matResult = queryService.exportMaterials();
		byte[] bomResult = queryService.exportBomItems();

		// Then
		assertThat(matResult).isEqualTo(mockBytes);
		assertThat(bomResult).isEqualTo(mockBytes);
	}
}
