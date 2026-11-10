package com.example.demo.presentation.assembler;

import com.example.demo.application.shared.command.ExportTemplateExcelCommand;
import com.example.demo.application.shared.command.ExportTemplateWithMappingCommand;
import com.example.demo.application.shared.dto.ExcelTemplateGottenResult;
import com.example.demo.presentation.resource.in.ExportTemplateExcelResource;
import com.example.demo.presentation.resource.in.ExportTemplateWithMappingResource;
import com.example.demo.presentation.resource.out.ExcelTemplateRetrievedResource;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 範本 Excel 表現層防腐組裝器 (ACL Assembler)。
 * <p>
 * 遵循 Clean Architecture 職責分離規範，獨立於 Resource 之外，
 * 負責表現層資源 ({@link ExportTemplateExcelResource}, {@link ExportTemplateWithMappingResource}, {@link ExcelTemplateRetrievedResource}) 與
 * 應用層模型 ({@link ExportTemplateExcelCommand}, {@link ExportTemplateWithMappingCommand}, {@link ExcelTemplateGottenResult}) 間之雙向資料映射。
 * </p>
 */
@Component
public class ExcelTemplateResourceAssembler {

    /**
     * 將請求資源載體轉換為應用層匯出命令 (Request Resource -> Command)。
     *
     * @param templateKey 網址路徑中的範本識別鍵
     * @param resource    HTTP 請求本文資源
     * @return 應用層範本匯出命令
     */
    public ExportTemplateExcelCommand toCommand(String templateKey, ExportTemplateExcelResource resource) {
        Objects.requireNonNull(templateKey, "templateKey 不得為空");
        Objects.requireNonNull(resource, "resource 不得為空");
        return new ExportTemplateExcelCommand(
            templateKey,
            resource.dataModel(),
            resource.outputFileName()
        );
    }

    /**
     * 將具自訂映射之請求資源載體轉換為應用層匯出命令 (Request Resource -> Command)。
     *
     * @param templateKey 網址路徑中的範本識別鍵
     * @param resource    HTTP 請求本文資源
     * @return 應用層具自訂映射之範本匯出命令
     */
    public ExportTemplateWithMappingCommand toCommand(String templateKey, ExportTemplateWithMappingResource resource) {
        Objects.requireNonNull(templateKey, "templateKey 不得為空");
        Objects.requireNonNull(resource, "resource 不得為空");
        var mappingDef = resource.mappingDefinition() != null
            ? resource.mappingDefinition().withTemplateKey(templateKey)
            : null;
        return new ExportTemplateWithMappingCommand(
            templateKey,
            resource.sourceData(),
            mappingDef,
            resource.outputFileName()
        );
    }

    /**
     * 將應用層範本中繼規格 DTO 轉換為表現層回應資源 (DTO -> Response Resource)。
     *
     * @param dto 應用層範本中繼資訊 DTO
     * @return 表現層範本回應資源
     */
    public ExcelTemplateRetrievedResource toResource(ExcelTemplateGottenResult dto) {
        Objects.requireNonNull(dto, "dto 不得為空");
        return new ExcelTemplateRetrievedResource(
            dto.templateKey(),
            dto.templateName(),
            dto.category(),
            dto.description(),
            dto.requiredVariables(),
            dto.lastModifiedAt()
        );
    }
}

