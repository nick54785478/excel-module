package com.example.demo.application.port.in.template;

import com.example.demo.application.shared.command.ExportTemplateWithMappingCommand;

/**
 * 具備自訂欄位映射之範本 Excel 匯出用例 (Inbound Port / Use Case)。
 * <p>
 * 遵循 AGENTS.md 規則 3-8，Inbound Port 一律以 {@code UseCase} 結尾。
 * 負責將自訂映射規格轉換後之領域資料透過 JXLS 渲染並回傳二進位檔案位元組 (Buffered 模式)。
 * </p>
 */
public interface ExportTemplateWithMappingUseCase {

    /**
     * 依據動態欄位映射與範本匯出 Excel 檔案二進位位元組。
     *
     * @param command 具備自訂映射之範本匯出命令
     * @return Excel (.xlsx) 檔案二進位位元組陣列
     */
    byte[] exportFromTemplateWithMapping(ExportTemplateWithMappingCommand command);
}

