package com.example.demo.application.port.in.template;

import com.example.demo.application.shared.command.ExportTemplateExcelCommand;

/**
 * 宣告式範本 Excel 匯出業務案例介面 (Inbound Port)。
 * <p>
 * 遵循 AGENTS.md 規則 3-8，Inbound Port 一律以 {@code UseCase} 結尾。
 * 定義以已註冊之 Excel 範本為基底，結合資料模型產製二進位 Excel 檔案之業務合約。
 * 嚴禁在此介面中使用任何技術性或環境相依型別（如 Workbook、HttpServletRequest 等）。
 * </p>
 */
public interface ExportTemplateExcelUseCase {

    /**
     * 依據範本匯出 Excel 二進位位元組陣列。
     *
     * @param command 範本匯出命令 (Java 21 Record)
     * @return 產生之 Excel 二進位檔案位元組陣列
     */
    byte[] exportFromTemplate(ExportTemplateExcelCommand command);
}

