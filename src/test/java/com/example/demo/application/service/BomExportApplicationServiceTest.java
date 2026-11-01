package com.example.demo.application.service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.application.port.in.ExportBomUseCase;
import com.example.demo.application.port.out.BomRepositoryPort;
import com.example.demo.application.port.out.ExcelWriterPort;
import com.example.demo.application.port.out.MaterialRepositoryPort;
import com.example.demo.application.shared.command.ExportBomCommand;
import com.example.demo.application.shared.command.WriteExcelCommand;
import com.example.demo.application.shared.command.WriteStreamExcelCommand;
import com.example.demo.application.shared.dto.BomExcelGottenResult;
import com.example.demo.domain.bom.aggregate.root.Bom;
import com.example.demo.domain.bom.aggregate.vo.BomStatus;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 物料清單匯出應用服務單元測試 (BomExportApplicationService Unit Test)
 * <p>
 * 針對 {@link BomExportApplicationService} 進行多階展開、單階平鋪與物料主檔對照表產製業務邏輯之驗證。
 * 所有測試案例嚴格遵循 BDD / Gherkin (Feature, Scenario, Given-When-Then) 語法语境規範。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BomExportApplicationService 物料清單匯出服務測試")
class BomExportApplicationServiceTest {

	@Mock
	private BomRepositoryPort bomRepositoryPort;

	@Mock
	private MaterialRepositoryPort materialRepositoryPort;

	@Mock
	private ExcelWriterPort excelWriterPort;

	private ExportBomUseCase exportBomUseCase;

	private Material parentMaterial;
	private Material subAssemblyMaterial;
	private Material rawMaterial;
	private Bom parentBom;
	private Bom subAssemblyBom;

	@BeforeEach
	void setUp() {
		this.exportBomUseCase = new BomExportApplicationService(
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

		// 準備多階 BOM
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
	 * Feature: 特定成品之多階層 BOM Excel 匯出
	 * Scenario: 依據指定母件料號 (MAT-FG-001) 產製包含多階展開、單階平鋪與物料主檔對照表之 Excel 檔案
	 * Given: 系統中存在成品 MAT-FG-001 與其下階半成品 MAT-SA-001 和原物料 MAT-RM-001
	 * When: 呼叫 exportBomUseCase.exportBom(command)
	 * Then: 成功調用 excelWriterPort 產製 3 個工作表，且檔案名稱包含該母件料號
	 */
	@Test
	@DisplayName("成功：依指定母件料號匯出多階層 BOM Excel 檔案")
	void shouldExportBomForSpecificParentMaterialSuccessfully() {
		// Given
		ExportBomCommand command = new ExportBomCommand("MAT-FG-001", "V1.0", true, true, true);
		byte[] mockBytes = new byte[]{0x50, 0x4B, 0x03, 0x04}; // ZIP/XLSX header magic bytes

		when(materialRepositoryPort.findAll()).thenReturn(List.of(parentMaterial, subAssemblyMaterial, rawMaterial));
		when(bomRepositoryPort.findAll()).thenReturn(List.of(parentBom, subAssemblyBom));
		when(excelWriterPort.write(any(WriteExcelCommand.class))).thenReturn(mockBytes);

		// When
		BomExcelGottenResult result = exportBomUseCase.exportBom(command);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.fileBytes()).isEqualTo(mockBytes);
		assertThat(result.fileName()).isEqualTo("BOM_MAT-FG-001_V1.0.xlsx");
		assertThat(result.totalSheets()).isEqualTo(3);
		assertThat(result.totalRows()).isGreaterThan(0);

		ArgumentCaptor<WriteExcelCommand> captor = ArgumentCaptor.forClass(WriteExcelCommand.class);
		verify(excelWriterPort).write(captor.capture());
		WriteExcelCommand captured = captor.getValue();
		assertThat(captured.getSheets()).hasSize(3);
		assertThat(captured.getSheets().get(0).getSheetName()).isEqualTo("多階層BOM展開清單");
		assertThat(captured.getSheets().get(1).getSheetName()).isEqualTo("單階BOM匯入清單");
		assertThat(captured.getSheets().get(2).getSheetName()).isEqualTo("物料主檔對照表");
	}

	/**
	 * Feature: 全系統 BOM 清單 Excel 批次匯出
	 * Scenario: 未指定母件料號時，匯出系統中所有 BOM 清單
	 * Given: 匯出命令之 parentMaterialCode 為 null
	 * When: 呼叫 exportBomUseCase.exportBom(command)
	 * Then: 預設檔名為 BOM_LIST_ALL.xlsx，產製所有 BOM 之階層與單階資料
	 */
	@Test
	@DisplayName("成功：未指定母件料號時匯出全系統所有 BOM 清單")
	void shouldExportAllBomsWhenParentMaterialCodeIsNull() {
		// Given
		ExportBomCommand command = new ExportBomCommand(null, null, true, true, false);
		byte[] mockBytes = new byte[]{1, 2, 3};

		when(materialRepositoryPort.findAll()).thenReturn(List.of(parentMaterial, subAssemblyMaterial, rawMaterial));
		when(bomRepositoryPort.findAll()).thenReturn(List.of(parentBom, subAssemblyBom));
		when(excelWriterPort.write(any(WriteExcelCommand.class))).thenReturn(mockBytes);

		// When
		BomExcelGottenResult result = exportBomUseCase.exportBom(command);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.fileName()).isEqualTo("BOM_LIST_ALL.xlsx");
		assertThat(result.totalSheets()).isEqualTo(2); // 排除物料主檔對照表
	}

	/**
	 * Feature: 查詢不存在之母件料號例外處理
	 * Scenario: 輸入無效或不存在之母件料號請求匯出
	 * Given: parentMaterialCode 為 NON-EXISTENT
	 * When: 呼叫 exportBomUseCase.exportBom(command)
	 * Then: 應拋出 MaterialNotFoundException 領域例外
	 */
	@Test
	@DisplayName("異常：查無指定母件料號時拋出 MaterialNotFoundException")
	void shouldThrowMaterialNotFoundExceptionWhenMaterialDoesNotExist() {
		// Given
		ExportBomCommand command = new ExportBomCommand("NON-EXISTENT", null, true, true, true);
		when(materialRepositoryPort.findAll()).thenReturn(List.of(parentMaterial));
		when(bomRepositoryPort.findAll()).thenReturn(List.of());

		// When & Then
		assertThatThrownBy(() -> exportBomUseCase.exportBom(command))
				.isInstanceOf(MaterialNotFoundException.class)
				.hasMessageContaining("查無指定料號之物料主檔");
	}

	/**
	 * Feature: 大數據串流匯出 BOM
	 * Scenario: 透過 streamExportBom 將 Excel 資料分批 Flush 至 OutputStream
	 * Given: 傳入 ByteArrayOutputStream 與指定 flushInterval 之 ExportBomCommand
	 * When: 呼叫 service.streamExportBom(baos, command)
	 * Then: 成功調用 excelWriterPort.writeStream(...) 且包含 WriteStreamExcelCommand
	 */
	@Test
	@DisplayName("成功：透過 streamExportBom 執行大數據串流輸出")
	void shouldStreamExportBomSuccessfully() {
		// Given
		BomExportApplicationService service = new BomExportApplicationService(
				bomRepositoryPort,
				materialRepositoryPort,
				excelWriterPort
		);
		ExportBomCommand command = new ExportBomCommand("MAT-FG-001", "V1.0", true, true, true, 500);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		when(materialRepositoryPort.findAll()).thenReturn(List.of(parentMaterial, subAssemblyMaterial, rawMaterial));
		when(bomRepositoryPort.findAll()).thenReturn(List.of(parentBom, subAssemblyBom));

		// When
		service.streamExportBom(baos, command);

		// Then
		ArgumentCaptor<WriteStreamExcelCommand> captor = ArgumentCaptor.forClass(WriteStreamExcelCommand.class);
		verify(excelWriterPort).writeStream(any(OutputStream.class), captor.capture());
		WriteStreamExcelCommand captured = captor.getValue();
		assertThat(captured.getSheets()).hasSize(3);
		assertThat(captured.getSheets().get(0).getFlushInterval()).isEqualTo(500);
	}
}
