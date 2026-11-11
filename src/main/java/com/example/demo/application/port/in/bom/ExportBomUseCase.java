package com.example.demo.application.port.in.bom;

import com.example.demo.application.shared.command.ExportBomCommand;
import com.example.demo.application.shared.dto.BomExcelGottenResult;
import com.example.demo.domain.material.exception.MaterialNotFoundException;

/**
 * 匯出物料清單業務使用案例輸入埠 (Export BOM Inbound Port / Use Case)。
 * <p>
 * <b>【架構定位】</b><br>
 * 遵循 Clean Architecture 六角形架構規範，代表應用層對外開放的物料清單 (BOM) Excel 檔案匯出業務合約。
 * 命名嚴格遵循 {@code AGENTS.md} 規則 3-8，以 {@code UseCase} 結尾 (動詞 + 名詞 + UseCase)。
 * </p>
 * <p>
 * <b>【CQRS 定位】</b><br>
 * 作為 Command 端的批次匯出合約，對應表現層命令導向端點 (例如 POST {@code /api/v1/bom/excel/export})，
 * 由 {@code BomCommandService} 實作。
 * </p>
 * <p>
 * <b>【輸入與輸出型別約束】</b><br>
 * 嚴格遵循規則 2，<b>嚴禁接發任何技術類或環境相依之物件</b>；
 * 僅接收不可變命令載體 {@link ExportBomCommand}，並回傳純資料結果載體 {@link BomExcelGottenResult}。
 * </p>
 */
public interface ExportBomUseCase {

	/**
	 * 依據匯出命令條件，動態產製物料清單 (BOM) Excel 檔案。
	 * <p>
	 * 業務邏輯支援依據 {@link ExportBomCommand} 旗標動態組合產製下列工作表：
	 * <ul>
	 *   <li><b>多階層 BOM 展開清單</b>：以階層樹狀遞迴展開並計算累計單耗與總成本。</li>
	 *   <li><b>單階 BOM 匯入格式清單</b>：平鋪展平之單階配方，符合 ERP/MES 匯入格式。</li>
	 *   <li><b>物料主檔對照表</b>：彙整所有關聯料件之規格、類別與標準成本。</li>
	 * </ul>
	 * </p>
	 *
	 * @param command 匯出命令資料載體，封裝目標母件料號、版本、最大展開階數與各工作表勾選旗標
	 * @return BOM Excel 檔案產製結果 DTO，封裝產出之檔名、二進位 byte[] 資料、總列數與工作表數量
	 * @throws MaterialNotFoundException 當指定之母件料號不存在於物料主檔中時拋出
	 * @throws NullPointerException      當 command 為 null 時拋出
	 */
	BomExcelGottenResult exportBom(ExportBomCommand command);
}
