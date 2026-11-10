package com.example.demo.application.port.in.bom;

import com.example.demo.application.shared.command.ExportBomCommand;
import com.example.demo.application.shared.dto.BomExcelGottenResult;

/**
 * 匯出物料清單業務使用案例輸入埠 (Export BOM Inbound Port / Use Case)
 * <p>
 * 遵循六角形架構規範，代表應用層對外開放的物料清單 (BOM) Excel 匯出業務合約。
 * <p>
 * <b>嚴禁接發任何技術類或環境相依之物件</b>：
 * 方法傳入與回傳型別嚴格限定為應用層命令 {@link ExportBomCommand} 與純資料結果載體 {@link BomExcelGottenResult}。
 */
public interface ExportBomUseCase {

	/**
	 * 依據匯出命令條件，動態產製物料清單 (BOM) Excel 檔案。
	 *
	 * @param command 匯出命令資料載體
	 * @return BOM Excel 檔案產製結果 DTO
	 */
	BomExcelGottenResult exportBom(ExportBomCommand command);
}

