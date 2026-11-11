package com.example.demo.application.port.in.template;

import com.example.demo.application.shared.command.DeleteExcelTemplateCommand;
import com.example.demo.application.shared.exception.ExcelTemplateNotFoundException;

/**
 * 刪除或下架 Excel 範本輸入埠 (Inbound Port / Use Case)。
 * <p>
 * 遵循 AGENTS.md 規則 3-8，Inbound Port 一律以 {@code UseCase} 結尾。
 * 負責定義刪除或停用指定 Excel 範本之業務合約，接收 {@link DeleteExcelTemplateCommand}。
 * </p>
 */
public interface DeleteExcelTemplateUseCase {

    /**
     * 刪除或下架指定識別鍵之 Excel 範本。
     *
     * @param command 刪除範本命令載體
     * @throws ExcelTemplateNotFoundException 當指定範本鍵值不存在時拋出
     */
    void deleteTemplate(DeleteExcelTemplateCommand command);
}

