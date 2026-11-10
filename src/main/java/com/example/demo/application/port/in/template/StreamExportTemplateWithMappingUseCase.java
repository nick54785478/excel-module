package com.example.demo.application.port.in.template;

import com.example.demo.application.shared.command.ExportTemplateWithMappingCommand;
import java.io.OutputStream;

/**
 * 具備自訂欄位映射之範本 Excel 串流匯出用例 (Inbound Port / Use Case)。
 * <p>
 * 遵循 AGENTS.md 規則 3-8，Inbound Port 一律以 {@code UseCase} 結尾。
 * 負責將自訂映射規格轉換後之領域資料透過 JXLS 直寫 HTTP 輸出串流 (Zero-Copy Streaming 模式)。
 * </p>
 */
public interface StreamExportTemplateWithMappingUseCase {

    /**
     * 依據動態欄位映射與範本串流匯出 Excel 檔案至輸出串流。
     *
     * @param outputStream 目標二進位輸出串流 (如 HttpServletResponse OutputStream)
     * @param command      具備自訂映射之範本匯出命令
     */
    void streamExportFromTemplateWithMapping(OutputStream outputStream, ExportTemplateWithMappingCommand command);
}

