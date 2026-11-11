package com.example.demo.application.port.in.template;

import com.example.demo.application.shared.dto.ExcelTemplateGottenResult;
import com.example.demo.application.shared.exception.ExcelTemplateNotFoundException;
import com.example.demo.application.shared.query.GetExcelTemplateQuery;

/**
 * 查詢單一 Excel 範本輸入埠 (Inbound Port / Use Case)。
 * <p>
 * 遵循 AGENTS.md 規則 3-8，Inbound Port 一律以 {@code UseCase} 結尾。
 * 接收 {@link GetExcelTemplateQuery} 並回傳 {@link ExcelTemplateGottenResult}。
 * </p>
 */
public interface GetExcelTemplateUseCase {

    /**
     * 依據範本唯一識別鍵查詢範本中繼資訊。
     *
     * @param query 單一範本查詢載體
     * @return 範本中繼資訊 DTO
     * @throws ExcelTemplateNotFoundException 當查無該範本時拋出
     */
    ExcelTemplateGottenResult getTemplate(GetExcelTemplateQuery query);
}
