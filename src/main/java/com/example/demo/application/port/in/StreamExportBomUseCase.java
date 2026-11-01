package com.example.demo.application.port.in;

import java.io.OutputStream;

import com.example.demo.application.shared.command.ExportBomCommand;

/**
 * 物料清單 (BOM) 大數據串流匯出業務輸入埠 (Stream Export BOM Inbound Port / Use Case)
 * <p>
 * 遵循六角形架構規範，代表應用層對外開放的物料清單 (BOM) 大數據串流匯出業務合約。
 * 直接將 Excel 活頁簿資料以分批 Flush 機制串流寫入至指定的 {@link OutputStream}，
 * 維持常數級 O(1) 極低記憶體開銷，防止海量 BOM 匯出時發生 OOM。
 * <p>
 * <b>嚴禁接發技術類物件</b>：傳入型別僅為 Java 抽象輸出資料流 {@link OutputStream} 與業務命令 {@link ExportBomCommand}。
 */
public interface StreamExportBomUseCase {

	/**
	 * 執行 BOM 大數據串流匯出，直接寫入指定的輸出串流。
	 *
	 * @param outputStream 目標輸出串流 (如 HTTP 回應資料流)
	 * @param command      BOM 匯出命令資料
	 */
	void streamExportBom(OutputStream outputStream, ExportBomCommand command);
}

