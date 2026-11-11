package com.example.demo.application.port.in.template;

import com.example.demo.application.shared.command.CreateExcelTemplateCommand;
import com.example.demo.application.shared.dto.ExcelTemplateGottenResult;

/**
 * 建立 Excel 範本輸入埠 (Inbound Port / Use Case)。
 * <p>
 * 遵循 AGENTS.md 規則 3-8，Inbound Port 一律以 {@code UseCase} 結尾。
 * 負責定義新增 Excel 範本中繼資訊與檔案規格之業務合約，
 * 接收 {@link CreateExcelTemplateCommand} 並回傳 {@link ExcelTemplateGottenResult}。
 * 嚴禁接發任何技術相依或 ORM 實體物件。
 * </p>
 */
public interface CreateExcelTemplateUseCase {

    /**
     * 建立並註冊新的 Excel 範本規格。
     *
     * @param command 建立範本命令載體
     * @return 建立成功後之範本中繼規格 DTO
     * @throws IllegalArgumentException 當範本識別鍵已存在或必填參數不完整時拋出
     */
    ExcelTemplateGottenResult createTemplate(CreateExcelTemplateCommand command);
}
