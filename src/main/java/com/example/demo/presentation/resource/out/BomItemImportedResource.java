package com.example.demo.presentation.resource.out;

import java.math.BigDecimal;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 單階與多階層 BOM 配方匯入解析完成回應結構載體 (Response Data Carrier)
 * <p>
 * 遵循 {@code N + Ved + Resource} 命名規範 (3-2 規範)。
 * 作為純資料載體，嚴禁於內部實作任何轉換邏輯。
 * 同時支援單階配方模型、多階層展開模型與物料主檔對照表之結構化資料輸出，確保未指定工作表時所有相符工作表資料完整印出。
 * </p>
 *
 * @param totalCount     匯入解析成功的項目總筆數 (包含多階、單階與物料主檔)
 * @param sheetName      工作表名稱 (或多工作表綜合摘要)
 * @param items          單階 BOM 子項明細清單 (維持向下相容)
 * @param multiLevelBom  多階層 BOM 展開清單專屬結構 (包含階層、階層序號、規格型號、有效用量、標準單價與成本小計)
 * @param singleLevelBom 單階 BOM 配方清單專屬結構 (包含母件料號、版本號、項次、組裝用量與損耗率)
 * @param materialMaster 物料主檔對照表專屬結構 (包含物料料號、品名、規格型號、物料類別、單位與標準單價)
 */
@Schema(description = "BOM 配方匯入解析完成回應結構 (支援單階、多階層與物料主檔專屬模型)")
public record BomItemImportedResource(
		@Schema(description = "匯入成功筆數 (所有解析項目總和)", example = "62")
		int totalCount,

		@Schema(description = "工作表名稱 (或多工作表匯總名稱)", example = "全部工作表 (多階層BOM展開清單: 27筆, 單階BOM匯入清單: 16筆, 物料主檔對照表: 19筆)")
		String sheetName,

		@Schema(description = "單階 BOM 子項清單 (維持向下相容)")
		List<BomItemRetrievedResource> items,

		@Schema(description = "多階層 BOM 展開清單專屬結構")
		MultiLevelBomSheetResource multiLevelBom,

		@Schema(description = "單階 BOM 配方清單專屬結構")
		SingleLevelBomSheetResource singleLevelBom,

		@Schema(description = "物料主檔對照表專屬結構")
		MaterialMasterSheetResource materialMaster
) {

	/**
	 * 便捷建構子：向下相容既有 3 參數呼叫
	 *
	 * @param totalCount 總筆數
	 * @param sheetName  工作表名稱
	 * @param items      子項清單
	 */
	public BomItemImportedResource(int totalCount, String sheetName, List<BomItemRetrievedResource> items) {
		this(totalCount, sheetName, items, null, null, null);
	}

	/**
	 * 便捷建構子：向下相容既有 5 參數呼叫
	 *
	 * @param totalCount     總筆數
	 * @param sheetName      工作表名稱
	 * @param items          子項清單
	 * @param multiLevelBom  多階層結構
	 * @param singleLevelBom 單階結構
	 */
	public BomItemImportedResource(
			int totalCount,
			String sheetName,
			List<BomItemRetrievedResource> items,
			MultiLevelBomSheetResource multiLevelBom,
			SingleLevelBomSheetResource singleLevelBom
	) {
		this(totalCount, sheetName, items, multiLevelBom, singleLevelBom, null);
	}

	/**
	 * 物料主檔對照表工作表結構
	 *
	 * @param sheetName  工作表真實名稱
	 * @param totalCount 資料筆數
	 * @param items      物料主檔項目清單
	 */
	@Schema(description = "物料主檔對照表工作表資料封裝")
	public record MaterialMasterSheetResource(
			@Schema(description = "工作表名稱", example = "物料主檔對照表")
			String sheetName,

			@Schema(description = "工作表資料筆數", example = "19")
			int totalCount,

			@Schema(description = "物料主檔項目清單")
			List<MaterialImportedResource.MaterialRetrievedResource> items
	) {
	}

	/**
	 * 單階 BOM 工作表結構
	 *
	 * @param sheetName  工作表真實名稱
	 * @param totalCount 資料筆數
	 * @param items      單階 BOM 項目清單
	 */
	@Schema(description = "單階 BOM 工作表資料封裝")
	public record SingleLevelBomSheetResource(
			@Schema(description = "工作表名稱", example = "單階BOM匯入清單")
			String sheetName,

			@Schema(description = "工作表資料筆數", example = "16")
			int totalCount,

			@Schema(description = "單階 BOM 配方項目清單")
			List<BomItemRetrievedResource> items
	) {
	}

	/**
	 * 多階層 BOM 工作表結構
	 *
	 * @param sheetName  工作表真實名稱
	 * @param totalCount 資料筆數
	 * @param items      多階層 BOM 項目清單
	 */
	@Schema(description = "多階層 BOM 工作表資料封裝")
	public record MultiLevelBomSheetResource(
			@Schema(description = "工作表名稱", example = "多階層BOM展開清單")
			String sheetName,

			@Schema(description = "工作表資料筆數", example = "27")
			int totalCount,

			@Schema(description = "多階層 BOM 展開項目清單")
			List<MultiLevelBomRetrievedResource> items
	) {
	}

	/**
	 * 單筆解析成功之單階 BOM 子項
	 *
	 * @param parentMaterialCode 母件料號
	 * @param bomVersion         BOM 版本號
	 * @param itemSequence       項次序號
	 * @param childMaterialCode  子件料號
	 * @param childMaterialName  子件名稱
	 * @param quantity           組裝用量
	 * @param unit               單位
	 * @param scrapRate          損耗率 (%)
	 * @param positionCode       安裝位置代碼
	 * @param remark             備註說明
	 */
	@Schema(description = "已解析單階 BOM 子項")
	public record BomItemRetrievedResource(
			@Schema(description = "母件料號", example = "MAT-FG-001")
			String parentMaterialCode,

			@Schema(description = "BOM 版本號", example = "V1.0")
			String bomVersion,

			@Schema(description = "項次序號", example = "10")
			Integer itemSequence,

			@Schema(description = "子件料號", example = "MAT-SFG-001")
			String childMaterialCode,

			@Schema(description = "子件名稱", example = "飛控主板電路模組總成")
			String childMaterialName,

			@Schema(description = "組裝用量", example = "1.0000")
			BigDecimal quantity,

			@Schema(description = "單位", example = "PCS")
			String unit,

			@Schema(description = "損耗率 (%)", example = "0.00")
			BigDecimal scrapRate,

			@Schema(description = "安裝位置代碼", example = "FC-ASM")
			String positionCode,

			@Schema(description = "備註說明", example = "核心飛控組裝件")
			String remark
	) {
	}

	/**
	 * 單筆解析成功之多階層 BOM 展開項目
	 *
	 * @param level              階層等級 (0 為整機，1 為直接組件，2 為零件...)
	 * @param levelSequence      階層序號 (WBS 編碼)
	 * @param parentMaterialCode 母件料號
	 * @param childMaterialCode  子件料號
	 * @param childMaterialName  子件名稱 (品名)
	 * @param specification      規格型號
	 * @param category           物料類別
	 * @param unit               計量單位
	 * @param quantity           單機用量
	 * @param scrapRate          損耗率 (%)
	 * @param effectiveQuantity  有效用量
	 * @param standardPrice      標準單價 (TWD)
	 * @param subtotalCost       小計總成本 (TWD)
	 * @param positionCode       安裝位置代碼 (位號)
	 * @param remark             備註說明
	 */
	@Schema(description = "已解析多階層 BOM 展開項目")
	public record MultiLevelBomRetrievedResource(
			@Schema(description = "階層等級", example = "1")
			Integer level,

			@Schema(description = "階層序號 (WBS)", example = "1.1")
			String levelSequence,

			@Schema(description = "母件料號", example = "MAT-FG-001")
			String parentMaterialCode,

			@Schema(description = "子件料號", example = "MAT-SFG-001")
			String childMaterialCode,

			@Schema(description = "子件名稱", example = "飛控主板電路模組總成")
			String childMaterialName,

			@Schema(description = "規格型號", example = "FC-PCB-ASSY-V3")
			String specification,

			@Schema(description = "物料類別", example = "半成品")
			String category,

			@Schema(description = "單位", example = "PCS")
			String unit,

			@Schema(description = "單機用量", example = "1.0000")
			BigDecimal quantity,

			@Schema(description = "損耗率 (%)", example = "0.00")
			BigDecimal scrapRate,

			@Schema(description = "有效用量", example = "1.0000")
			BigDecimal effectiveQuantity,

			@Schema(description = "標準單價", example = "8500.00")
			BigDecimal standardPrice,

			@Schema(description = "小計總成本", example = "8500.00")
			BigDecimal subtotalCost,

			@Schema(description = "安裝位置代碼", example = "FC-ASM")
			String positionCode,

			@Schema(description = "備註說明", example = "核心飛控組裝件")
			String remark
	) {
	}
}
