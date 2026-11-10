package com.example.demo.application.port.in.bom;

import java.io.OutputStream;

import com.example.demo.application.shared.command.ExportBomCommand;
import com.example.demo.application.shared.dto.BomExcelGottenResult;

/**
 * BOM 物料清單查詢業務使用案例輸入埠 (Query BOM Use Case / Inbound Port)
 * <p>
 * 遵循 Clean Architecture 六角形架構與 CQRS 規範，作為應用層對外開放的物料清單查詢業務合約。
 * 負責依據查詢條件產製並輸出 BOM 報表。
 * <p>
 * 涵蓋技術模組：
 * <ul>
 *     <li><b>【命令式階層樹狀結構報表模組 (Imperative Multi-Level Tree Report)】</b>：多階層遞迴展開、單階平鋪與物料對照表。</li>
 *     <li><b>【大數據串流處理模組 (Big Data Streaming Module)】</b>：百萬級資料量邊產製邊傳輸至輸出串流，達成 O(1) 常數記憶體開銷。</li>
 * </ul>
 * <p>
 * 命名規範：以 {@code UseCase} 結尾 (符合 AGENTS.md 3-8 規範)。
 */
public interface QueryBomUseCase {

	/**
	 * 【命令式階層樹狀結構報表模組 (Imperative Multi-Level Tree Report)】
	 * <p>
	 * 依據查詢條件，於記憶體中動態產製物料清單 (BOM) Excel 檔案。
	 *
	 * @param command 匯出查詢條件資料載體
	 * @return BOM Excel 檔案產製結果 DTO
	 */
	BomExcelGottenResult queryBom(ExportBomCommand command);

	/**
	 * 【大數據串流處理模組 (Big Data Streaming Module)】
	 * <p>
	 * 依據查詢條件，以大數據串流模式將 Excel 二進位內容寫入輸出串流，維持 O(1) 記憶體常數開銷。
	 *
	 * @param outputStream 目的輸出串流 (例如 HttpServletResponse 輸出串流)
	 * @param command      匯出查詢條件與沖刷間隔命令
	 */
	void streamQueryBom(OutputStream outputStream, ExportBomCommand command);
}

