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
 * 物料清單匯出命令應用服務單元測試 (BomCommandService Test)
 * <p>
 * 針對 {@link BomCommandService} 驗證多階展開、單階平鋪、物料主檔對照表產製與大數據串流輸出之命令端邏輯。
 * 所有測試案例嚴格遵循 BDD / Gherkin (Feature, Scenario, Given-When-Then) 語法規範。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BomCommandService 物料清單匯出命令服務測試")
class BomCommandServiceTest {

	@Mock
	private BomRepositoryPort bomRepositoryPort;

	@Mock
	private MaterialRepositoryPort materialRepositoryPort;

	@Mock
	private ExcelWriterPort excelWriterPort;

	private BomCommandService commandService;

	private Material parentMaterial;
	private Material subAssemblyMaterial;
	private Material rawMaterial;
	private Bom parentBom;
	private Bom subAssemblyBom;

	@BeforeEach
	void setUp() {
		this.commandService = new BomCommandService(
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
	 * Feature: 多階層 BOM 展開與 Excel 匯出
	 * Scenario: 指定母件料號匯出包含多階展開、單階平鋪與物料主檔三張工作表之 Excel
	 * Given: 儲存庫存在完整成品與半成品 BOM，且 ExcelWriterPort 正常產製二進位陣列
	 * When: 呼叫 commandService.exportBom(command)
	 * Then: 成功返回 BomExcelGottenResult，工作表數量為 3，檔名符合 BOM_MAT-FG-001_V1.0.xlsx
	 */
	@Test
	@DisplayName("成功：依指定母件料號與版本匯出完整三張工作表 BOM Excel")
	void shouldExportBomWithAllSheetsSuccessfully() {
		// Given
		ExportBomCommand command = new ExportBomCommand("MAT-FG-001", "V1.0", true, true, true);
		byte[] mockBytes = "MOCK_EXCEL_BYTES".getBytes();

		when(materialRepositoryPort.findAll()).thenReturn(List.of(parentMaterial, subAssemblyMaterial, rawMaterial));
		when(bomRepositoryPort.findAll()).thenReturn(List.of(parentBom, subAssemblyBom));
		when(excelWriterPort.write(any(WriteExcelCommand.class))).thenReturn(mockBytes);

		// When
		BomExcelGottenResult result = commandService.exportBom(command);

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
	 * Scenario: 前端傳入未登錄之母件料號進行 BOM 匯出
	 * Given: 儲存庫查無該料號對應之物料主檔
	 * When: 呼叫 commandService.exportBom(command)
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
		assertThatThrownBy(() -> commandService.exportBom(command))
				.isInstanceOf(MaterialNotFoundException.class)
				.hasMessageContaining("查無指定料號之物料主檔: NOT_EXIST_MAT");
	}

	/**
	 * Feature: 大數據串流匯出物料清單 (BOM) Excel
	 * Scenario: 呼叫 streamExportBom 邊產製邊寫入 OutputStream
	 * Given: 儲存庫存在完整 BOM 資料
	 * When: 呼叫 commandService.streamExportBom(baos, command)
	 * Then: 成功調用 excelWriterPort.writeStream(...) 且包含 WriteStreamExcelCommand
	 */
	@Test
	@DisplayName("成功：透過 streamExportBom 執行大數據串流輸出")
	void shouldStreamExportBomSuccessfully() {
		// Given
		ExportBomCommand command = new ExportBomCommand("MAT-FG-001", "V1.0", true, true, true, 500);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		when(materialRepositoryPort.findAll()).thenReturn(List.of(parentMaterial, subAssemblyMaterial, rawMaterial));
		when(bomRepositoryPort.findAll()).thenReturn(List.of(parentBom, subAssemblyBom));

		// When
		commandService.streamExportBom(baos, command);

		// Then
		ArgumentCaptor<WriteStreamExcelCommand> captor = ArgumentCaptor.forClass(WriteStreamExcelCommand.class);
		verify(excelWriterPort).writeStream(any(OutputStream.class), captor.capture());
		WriteStreamExcelCommand captured = captor.getValue();
		assertThat(captured.getSheets()).hasSize(3);
		assertThat(captured.getSheets().get(0).getFlushInterval()).isEqualTo(500);
	}
}
