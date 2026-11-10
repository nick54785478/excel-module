package com.example.demo.application.port.in.template;

import com.example.demo.application.shared.command.ExportTemplateExcelCommand;
import java.io.OutputStream;

/**
 * 宣告式範本 Excel 串流匯出業務案例介面 (Inbound Port)。
 * <p>
 * 遵循 AGENTS.md 規則 3-8，Inbound Port 一律以 {@code UseCase} 結尾。
 * 提供直通輸出串流 (Zero-Copy Streaming) 之匯出能力，產物直接寫入目標串流，避免堆疊於記憶體中。
 * </p>
 */
public interface StreamExportTemplateExcelUseCase {

    /**
     * 依據範本直接串流寫出 Excel 檔案至目標輸出串流。
     *
     * @param outputStream 目標二進位輸出串流 (例如 HTTP 網路輸出串流)
     * @param command      範本匯出命令 (Java 21 Record)
     */
    void streamExportFromTemplate(OutputStream outputStream, ExportTemplateExcelCommand command);
}

