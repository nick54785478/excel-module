package com.example.demo.application.port.in.template;

import com.example.demo.application.shared.command.UpdateExcelTemplateCommand;
import com.example.demo.application.shared.dto.ExcelTemplateGottenResult;
import com.example.demo.application.shared.exception.ExcelTemplateNotFoundException;

/**
 * 更新 Excel 範本輸入埠 (Inbound Port / Use Case)。
 * <p>
 * 遵循 AGENTS.md 規則 3-8，Inbound Port 一律以 {@code UseCase} 結尾。
 * 負責定義異動既有 Excel 範本中繼資訊、必備變數或啟用狀態之業務合約，
 * 接收 {@link UpdateExcelTemplateCommand} 並回傳 {@link ExcelTemplateGottenResult}。
 * </p>
 */
public interface UpdateExcelTemplateUseCase {

    /**
     * 更新既有 Excel 範本規格。
     *
     * @param command 更新範本命令載體
     * @return 更新後之範本中繼規格 DTO
     * @throws ExcelTemplateNotFoundException 當指定範本鍵值不存在時拋出
     */
    ExcelTemplateGottenResult updateTemplate(UpdateExcelTemplateCommand command);
}
