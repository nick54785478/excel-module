package com.example.demo.application.port.in.bom;

import com.example.demo.application.annotation.mapping.ExcelColumn;
import com.example.demo.application.annotation.mapping.ExcelSheet;
import com.example.demo.application.shared.model.bom.BomItemExcelModel;
import com.example.demo.application.shared.model.material.MaterialExcelModel;
import com.example.demo.domain.bom.aggregate.root.Bom;
import com.example.demo.domain.material.aggregate.root.Material;

/**
 * 物料與 BOM 匯出業務使用案例輸入埠 (Export Material & BOM Use Case / Inbound Port)。
 * <p>
 * <b>【架構定位】</b><br>
 * 遵循 Clean Architecture 六角形架構規範，作為應用層對外開放的物料主檔與 BOM 配方清單業務匯出合約。
 * 命名嚴格遵循 {@code AGENTS.md} 規則 3-8，以 {@code UseCase} 結尾 (動詞 + 名詞 + UseCase)。
 * </p>
 * <p>
 * <b>【CQRS 定位】</b><br>
 * 作為 Query 端的宣告式映射匯出合約，由 {@code BomQueryService} 實作，專職提供表現層控制器
 * {@code MaterialBomExcelController} (GET {@code /api/v1/excel/material-bom/materials/export} 與
 * GET {@code /api/v1/excel/material-bom/bom-items/export}) 調用。
 * </p>
 * <p>
 * <b>【核心功能】</b><br>
 * 專門提供透過<b>【宣告式物件映射模組 (Declarative Bean Mapping)】</b>將系統持久化儲存庫中現存之
 * 物料主檔 ({@link Material}) 與單階 BOM 配方清單 ({@link Bom}) 匯出為標準 Excel 檔案之能力。
 * </p>
 * <p>
 * <b>【型別約束與 Clean Code】</b><br>
 * 嚴格遵循 {@code AGENTS.md} 規則 2 與規則 67，型別透過特定 import 引入，避免行內全限定類名 (fully qualified class names)；
 * 僅回傳純資料 byte[] 二進位檔案內容。
 * </p>
 */
public interface ExportMaterialBomUseCase {

	/**
	 * 【宣告式物件映射模組 (Declarative Bean Mapping)】
	 * <p>
	 * 從物料儲存庫查詢系統現有所有物料聚合根 ({@link Material})，映射為應用層模型 {@link MaterialExcelModel}，
	 * 自動解析 {@link ExcelSheet} 與 {@link ExcelColumn} 註解元資料，宣告式產製物料主檔 Excel 二進位檔案。
	 * </p>
	 *
	 * @return 物料主檔 Excel (.xlsx) 檔案二進位資料
	 */
	byte[] exportMaterials();

	/**
	 * 【宣告式物件映射模組 (Declarative Bean Mapping)】
	 * <p>
	 * 從 BOM 儲存庫與物料儲存庫查詢系統現有所有 BOM 配方 ({@link Bom}) 及其關聯子料件聚合根，
	 * 映射為應用層模型 {@link BomItemExcelModel}，
	 * 自動解析 {@link ExcelSheet} 與 {@link ExcelColumn} 註解元資料，宣告式產製單階 BOM 配方清單 Excel 二進位檔案。
	 * </p>
	 *
	 * @return 單階 BOM 配方清單 Excel (.xlsx) 檔案二進位資料
	 */
	byte[] exportBomItems();
}
