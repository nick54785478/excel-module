package com.example.demo.application.service.template;

import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.application.port.in.template.GetExcelTemplateUseCase;
import com.example.demo.application.port.in.template.ListExcelTemplateUseCase;
import com.example.demo.application.port.out.template.ExcelTemplateRegistryPort;
import com.example.demo.application.port.out.template.ExcelTemplateRepositoryPort;
import com.example.demo.application.shared.dto.ExcelTemplateGottenResult;
import com.example.demo.application.shared.exception.ExcelTemplateNotFoundException;
import com.example.demo.application.shared.query.GetExcelTemplateQuery;
import com.example.demo.application.shared.query.ListExcelTemplateQuery;

/**
 * 範本 Excel 規格查詢應用層服務 (Query Application Service)。
 * <p>
 * 遵循 AGENTS.md 規則 3-9 (CQRS 查詢端服務命名規範：{@code ... + QueryService})。
 * 實作 {@link GetExcelTemplateUseCase} 與 {@link ListExcelTemplateUseCase} 輸入埠，
 * 協調持久化儲存庫輸出埠 {@link ExcelTemplateRepositoryPort} 執行範本規格的檢索與條件清單查詢。
 * 若持久化輸出埠未配置（如輕量單元測試環境），可安全降級透過 {@link ExcelTemplateRegistryPort} 取得。
 * </p>
 */
@Service
@Transactional(readOnly = true)
public class ExcelTemplateQueryService implements GetExcelTemplateUseCase, ListExcelTemplateUseCase {

    private final ExcelTemplateRepositoryPort templateRepositoryPort;
    private final ExcelTemplateRegistryPort templateRegistryPort;

    /**
     * 建構函式：注入持久化儲存庫輸出埠與快取註冊表輸出埠。
     *
     * @param templateRepositoryPort 範本持久化儲存庫輸出埠 (優先)
     * @param templateRegistryPort   範本快取註冊表輸出埠 (可選降級)
     */
    @Autowired
    public ExcelTemplateQueryService(
            @Autowired(required = false) ExcelTemplateRepositoryPort templateRepositoryPort,
            @Autowired(required = false) ExcelTemplateRegistryPort templateRegistryPort) {
        this.templateRepositoryPort = templateRepositoryPort;
        this.templateRegistryPort = templateRegistryPort;
    }

    @Override
    public ExcelTemplateGottenResult getTemplate(GetExcelTemplateQuery query) {
        Objects.requireNonNull(query, "GetExcelTemplateQuery 不得為空");
        String key = query.templateKey();

        // 1. 優先查詢持久化儲存庫
        if (templateRepositoryPort != null) {
            return templateRepositoryPort.findByTemplateKey(key)
                    .orElseThrow(() -> new ExcelTemplateNotFoundException("指定的 Excel 範本不存在或未註冊: " + key));
        }

        // 2. 降級查詢快取註冊表
        if (templateRegistryPort != null) {
            return templateRegistryPort.findTemplateInfo(key)
                    .orElseThrow(() -> new ExcelTemplateNotFoundException("指定的 Excel 範本不存在或未註冊: " + key));
        }

        throw new ExcelTemplateNotFoundException("查無可用的範本資料來源以檢索範本: " + key);
    }

    @Override
    public List<ExcelTemplateGottenResult> listTemplates(ListExcelTemplateQuery query) {
        ListExcelTemplateQuery safeQuery = query != null ? query : ListExcelTemplateQuery.all();

        // 1. 優先查詢持久化儲存庫
        if (templateRepositoryPort != null) {
            if (safeQuery.category() != null && !safeQuery.category().isBlank()) {
                return templateRepositoryPort.findByCategory(safeQuery.category());
            }
            return Boolean.TRUE.equals(safeQuery.activeOnly())
                    ? templateRepositoryPort.findByActiveTrue()
                    : templateRepositoryPort.findAll();
        }

        // 2. 降級查詢快取註冊表
        if (templateRegistryPort != null) {
            return templateRegistryPort.listTemplates();
        }

        return List.of();
    }
}
