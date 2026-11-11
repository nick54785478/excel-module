package com.example.demo.application.port.in.bom;

import java.io.OutputStream;

import com.example.demo.application.shared.command.ExportBomCommand;
import com.example.demo.domain.material.exception.MaterialNotFoundException;

/**
 * 物料清單 (BOM) 大數據串流匯出業務輸入埠 (Stream Export BOM Inbound Port / Use Case)。
 * <p>
 * <b>【架構定位】</b><br>
 * 遵循 Clean Architecture 六角形架構規範，代表應用層對外開放的物料清單 (BOM) 大數據串流匯出業務合約。
 * 命名嚴格遵循 {@code AGENTS.md} 規則 3-8，以 {@code UseCase} 結尾。
 * </p>
 * <p>
 * <b>【CQRS 定位】</b><br>
 * 作為 Command 端的大數據串流輸出合約，對應表現層命令導向串流端點 (例如 POST {@code /api/v1/bom/excel/export/stream})，
 * 由 {@code BomCommandService} 實作。
 * </p>
 * <p>
 * <b>【大數據串流機制】</b><br>
 * 直接將 Excel 活頁簿資料以分批 Flush 機制串流寫入至傳入的抽象 {@link OutputStream}，
 * 透過批次釋放底層記憶體，將記憶體開銷維持在常數級 O(1)，防止百萬級巨量 BOM 結構匯出時發生記憶體溢出 (OOM)。
 * </p>
 * <p>
 * <b>【輸入與輸出型別約束】</b><br>
 * 嚴格遵循規則 2，嚴禁接收任何技術類或 Web 環境相依物件 (如 HttpServletResponse 等)；
 * 傳入型別限定為 Java 標準庫抽象輸出資料流 {@link OutputStream} 與業務命令 {@link ExportBomCommand}。
 * </p>
 */
public interface StreamExportBomUseCase {

	/**
	 * 執行 BOM 大數據串流匯出，直接寫入指定的抽象輸出串流。
	 *
	 * @param outputStream 目標抽象二進位輸出串流 (例如檔案輸出串流、網路串流或表現層回應串流)
	 * @param command      BOM 匯出命令資料，封裝母件料號、展開參數與沖刷列數間隔 (flushInterval)
	 * @throws MaterialNotFoundException 當指定之母件料號不存在於物料主檔中時拋出
	 * @throws NullPointerException      當 outputStream 或 command 為 null 時拋出
	 */
	void streamExportBom(OutputStream outputStream, ExportBomCommand command);
}
