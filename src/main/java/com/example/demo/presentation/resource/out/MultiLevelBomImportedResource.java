package com.example.demo.presentation.resource.out;

import java.util.List;

import com.example.demo.presentation.resource.out.BomItemImportedResource.MultiLevelBomRetrievedResource;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 多階層 BOM 展開清單匯入解析完成回應結構載體 (Response Data Carrier)
 * <p>
 * 遵循 {@code N + Ved + Resource} 命名規範 (3-2 規範)。
 * 作為純資料載體，嚴禁於內部實作任何轉換邏輯。
 * </p>
 *
 * @param totalCount 匯入解析成功的多階層 BOM 項目總筆數
 * @param sheetName  工作表名稱
 * @param items      解析出的多階層 BOM 項目明細清單
 */
@Schema(description = "多階層 BOM 展開清單匯入解析完成回應結構")
public record MultiLevelBomImportedResource(
		@Schema(description = "匯入成功筆數", example = "27")
		int totalCount,

		@Schema(description = "工作表名稱", example = "多階層BOM展開清單")
		String sheetName,

		@Schema(description = "解析成功的多階層 BOM 項目清單")
		List<MultiLevelBomRetrievedResource> items
) {
}
