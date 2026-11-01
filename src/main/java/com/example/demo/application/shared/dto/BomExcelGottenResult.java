package com.example.demo.application.shared.dto;

import java.util.Objects;

/**
 * 物料清單 Excel 匯出結果 DTO (BOM Excel Gotten Result)
 * <p>
 * 遵循 3-6 命名規範 (N + Gotten + Result)，作為應用層回傳產製完成之 BOM Excel 檔案純資料載體 (Pure Data Carrier)。
 * 內部嚴格禁止定義領域轉換或商業邏輯。
 *
 * @param fileBytes    Excel 二進位檔案位元組資料
 * @param fileName     預設下載檔案名稱 (如 BOM_MAT-FG-001.xlsx)
 * @param totalSheets  產生之工作表總數
 * @param totalRows    所有工作表匯總資料列總數
 */
public record BomExcelGottenResult(
		byte[] fileBytes,
		String fileName,
		int totalSheets,
		int totalRows
) {
	public BomExcelGottenResult {
		Objects.requireNonNull(fileBytes, "fileBytes 不得為空");
		Objects.requireNonNull(fileName, "fileName 不得為空");
	}
}

