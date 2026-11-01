package com.example.demo.application.port.in;

import java.io.OutputStream;

import com.example.demo.application.shared.command.StreamExportExcelCommand;

/**
 * Excel 大數據串流匯出業務合約 (Stream Export Excel Use Case)
 * <p>
 * 遵循 Clean Architecture 六角形架構規範，作為 Inbound Port 開放予表現層調用。
 * 命名規範：以 {@code UseCase} 結尾 (3-8 規範)。
 */
public interface StreamExportExcelUseCase {

	/**
	 * 執行大數據串流匯出，直接將資料分批 Flush 至指定的輸出串流
	 *
	 * @param outputStream 目標輸出串流 (如 HTTP 回應串流)
	 * @param command      串流匯出命令物件，包含資料流與分批沖刷大小
	 */
	void streamExport(OutputStream outputStream, StreamExportExcelCommand command);
}

