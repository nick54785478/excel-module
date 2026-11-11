package com.example.demo.application.port.in.bom;

import java.io.OutputStream;

import com.example.demo.application.shared.command.ExportBomCommand;
import com.example.demo.application.shared.dto.BomExcelGottenResult;
import com.example.demo.domain.material.exception.MaterialNotFoundException;

/**
 * BOM 物料清單查詢業務使用案例輸入埠 (Query BOM Use Case / Inbound Port)。
 * <p>
 * <b>【架構定位】</b><br>
 * 遵循 Clean Architecture 六角形架構與 CQRS 規範，作為應用層對外開放的物料清單查詢業務合約。
 * 命名嚴格遵循 {@code AGENTS.md} 規則 3-8，以 {@code UseCase} 結尾。
 * </p>
 * <p>
 * <b>【CQRS 定位】</b><br>
 * 作為 Query 端的業務合約，由 {@code BomQueryService} 實作，專職提供表現層查詢導向端點調用
 * (例如 GET {@code /api/v1/bom/excel} 與 GET {@code /api/v1/bom/excel/stream})。
 * </p>
 * <p>
 * <b>【涵蓋技術模組】</b>
 * <ul>
 *   <li><b>【命令式階層樹狀結構報表模組 (Imperative Multi-Level Tree Report)】</b>：多階層遞迴展開、單階平鋪與物料對照表。</li>
 *   <li><b>【大數據串流處理模組 (Big Data Streaming Module)】</b>：百萬級資料量邊產製邊傳輸至輸出串流，達成常數級 O(1) 記憶體開銷。</li>
 * </ul>
 * </p>
 * <p>
 * <b>【輸入與輸出型別約束】</b><br>
 * 嚴格遵循規則 2，嚴禁接收任何技術類或 Web 環境相依物件；僅接發應用層命令 {@link ExportBomCommand}、
 * DTO {@link BomExcelGottenResult} 與 Java 標準抽象串流 {@link OutputStream}。
 * </p>
 */
public interface QueryBomUseCase {

	/**
	 * 【命令式階層樹狀結構報表模組 (Imperative Multi-Level Tree Report)】
	 * <p>
	 * 依據查詢條件，於記憶體中動態產製物料清單 (BOM) Excel 檔案。
	 * </p>
	 *
	 * @param command 匯出查詢條件資料載體，封裝目標母件料號、版本號、展開階數與各工作表勾選旗標
	 * @return BOM Excel 檔案產製結果 DTO，封裝產出之檔名、二進位 byte[] 資料、總列數與工作表數量
	 * @throws MaterialNotFoundException 當指定之母件料號不存在於物料主檔中時拋出
	 * @throws NullPointerException      當 command 為 null 時拋出
	 */
	BomExcelGottenResult queryBom(ExportBomCommand command);

	/**
	 * 【大數據串流處理模組 (Big Data Streaming Module)】
	 * <p>
	 * 依據查詢條件，以大數據串流模式將 Excel 二進位內容寫入輸出串流，維持常數級 O(1) 記憶體開銷。
	 * </p>
	 *
	 * @param outputStream 目標抽象二進位輸出串流 (例如檔案輸出串流、網路串流或表現層回應串流)
	 * @param command      匯出查詢條件與沖刷間隔命令，封裝查詢母件與批次沖刷列數 (flushInterval)
	 * @throws MaterialNotFoundException 當指定之母件料號不存在於物料主檔中時拋出
	 * @throws NullPointerException      當 outputStream 或 command 為 null 時拋出
	 */
	void streamQueryBom(OutputStream outputStream, ExportBomCommand command);
}
