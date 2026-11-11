package com.example.demo.application.port.in.template;

import com.example.demo.application.shared.dto.ExcelTemplateGottenResult;
import com.example.demo.application.shared.query.ListExcelTemplateQuery;

import java.util.List;

/**
 * 查詢 Excel 範本清單輸入埠 (Inbound Port / Use Case)。
 * <p>
 * 遵循 AGENTS.md 規則 3-8，Inbound Port 一律以 {@code UseCase} 結尾。
 * 接收 {@link ListExcelTemplateQuery} 並回傳 {@link ExcelTemplateGottenResult} 清單。
 * </p>
 */
public interface ListExcelTemplateUseCase {

    /**
     * 依據條件查詢 Excel 範本清單。
     *
     * @param query 範本清單查詢條件
     * @return 符合條件之範本中繼規格清單
     */
    List<ExcelTemplateGottenResult> listTemplates(ListExcelTemplateQuery query);
}
