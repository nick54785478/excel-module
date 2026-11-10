package com.example.demo.application.port.in.bom;

/**
 * 物料與 BOM 匯出業務使用案例輸入埠 (Export Material & BOM Use Case / Inbound Port)
 * <p>
 * 遵循 Clean Architecture 六角形架構規範，作為應用層對外開放的物料與物料清單業務匯出合約。
 * 專門提供透過<b>【宣告式物件映射模組 (Declarative Bean Mapping)】</b>將系統持久化儲存庫中現存之
 * 物料主檔（{@link com.example.demo.domain.material.aggregate.root.Material}）與
 * 單階 BOM 配方清單（{@link com.example.demo.domain.bom.aggregate.root.Bom}）匯出為標準 Excel 檔案之能力。
 * <p>
 * 命名規範：以 {@code UseCase} 結尾 (符合 AGENTS.md 3-8 規範，動詞 + 名詞 + UseCase)。
 */
public interface ExportMaterialBomUseCase {

	/**
	 * 【宣告式物件映射模組 (Declarative Bean Mapping)】
	 * <p>
	 * 從物料儲存庫查詢系統現有所有物料聚合根，映射為應用層模型 {@link com.example.demo.application.shared.model.MaterialExcelModel}，
	 * 自動解析 {@link com.example.demo.application.annotation.ExcelSheet} 與 {@link com.example.demo.application.annotation.ExcelColumn}
	 * 註解元資料，宣告式產製物料主檔 Excel 二進位檔案。
	 *
	 * @return 物料主檔 Excel (.xlsx) 檔案二進位資料
	 */
	byte[] exportMaterials();

	/**
	 * 【宣告式物件映射模組 (Declarative Bean Mapping)】
	 * <p>
	 * 從 BOM 儲存庫與物料儲存庫查詢系統現有所有 BOM 配方及其關聯子料件聚合根，
	 * 映射為應用層模型 {@link com.example.demo.application.shared.model.BomItemExcelModel}，
	 * 自動解析 {@link com.example.demo.application.annotation.ExcelSheet} 與 {@link com.example.demo.application.annotation.ExcelColumn}
	 * 註解元資料，宣告式產製單階 BOM 配方清單 Excel 二進位檔案。
	 *
	 * @return 單階 BOM 配方清單 Excel (.xlsx) 檔案二進位資料
	 */
	byte[] exportBomItems();
}

