package com.example.demo.presentation.resource.out;

import java.math.BigDecimal;
import java.util.List;

import com.example.demo.presentation.resource.out.BomItemImportedResource.MaterialMasterSheetResource;
import com.example.demo.presentation.resource.out.BomItemImportedResource.MultiLevelBomSheetResource;
import com.example.demo.presentation.resource.out.BomItemImportedResource.SingleLevelBomSheetResource;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 物料主檔匯入解析完成回應結構載體 (Response Data Carrier)
 * <p>
 * 遵循 {@code N + Ved + Resource} 命名規範 (3-2 規範)。
 * 作為純資料載體，嚴禁於內部實作任何轉換邏輯。
 * 同時支援物料主檔對照表、多階層展開模型與單階配方模型之結構化資料輸出，確保未指定工作表時所有相符工作表資料完整印出。
 * </p>
 *
 * @param totalCount     匯入解析成功的項目總筆數 (包含物料主檔、多階與單階)
 * @param sheetName      工作表名稱 (或多工作表綜合摘要)
 * @param items          解析出的物料明細資料清單 (維持向下相容)
 * @param materialMaster 物料主檔對照表專屬結構 (包含物料料號、品名、規格型號、物料類別、單位與標準單價)
 * @param multiLevelBom  多階層 BOM 展開清單專屬結構 (包含階層、階層序號、規格型號、有效用量、標準單價與成本小計)
 * @param singleLevelBom 單階 BOM 配方清單專屬結構 (包含母件料號、版本號、項次、組裝用量與損耗率)
 */
@Schema(description = "物料主檔匯入解析完成回應結構 (支援物料主檔、多階層與單階專屬模型)")
public record MaterialImportedResource(
		@Schema(description = "匯入成功筆數 (所有解析項目總和)", example = "62")
		int totalCount,

		@Schema(description = "工作表名稱 (或多工作表綜合摘要)", example = "全部工作表 (多階層BOM展開清單: 27筆, 單階BOM匯入清單: 16筆, 物料主檔對照表: 19筆)")
		String sheetName,

		@Schema(description = "解析成功的物料明細清單 (維持向下相容)")
		List<MaterialRetrievedResource> items,

		@Schema(description = "物料主檔對照表專屬結構")
		MaterialMasterSheetResource materialMaster,

		@Schema(description = "多階層 BOM 展開清單專屬結構")
		MultiLevelBomSheetResource multiLevelBom,

		@Schema(description = "單階 BOM 配方清單專屬結構")
		SingleLevelBomSheetResource singleLevelBom
) {

	/**
	 * 便捷建構子：向下相容 3 參數呼叫
	 *
	 * @param totalCount 總筆數
	 * @param sheetName  工作表名稱
	 * @param items      物料項目清單
	 */
	public MaterialImportedResource(int totalCount, String sheetName, List<MaterialRetrievedResource> items) {
		this(totalCount, sheetName, items, null, null, null);
	}

	/**
	 * 單筆解析成功之物料項目
	 *
	 * @param materialCode  物料料號
	 * @param materialName  物料品名
	 * @param specification 規格型號
	 * @param category      物料類別
	 * @param unit          計量單位
	 * @param standardCost  標準成本 (TWD)
	 * @param status        狀態
	 */
	@Schema(description = "已解析物料項目")
	public record MaterialRetrievedResource(
			@Schema(description = "物料料號", example = "MAT-FG-001")
			String materialCode,

			@Schema(description = "物料品名", example = "4K 專業航拍無人機")
			String materialName,

			@Schema(description = "規格型號", example = "DRONE-4K-PRO")
			String specification,

			@Schema(description = "物料類別", example = "FINISHED_GOODS")
			String category,

			@Schema(description = "計量單位", example = "PCS")
			String unit,

			@Schema(description = "標準成本 (TWD)", example = "35000.00")
			BigDecimal standardCost,

			@Schema(description = "狀態", example = "ACTIVE")
			String status
	) {
	}
}

