package com.example.demo.presentation.assembler;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.example.demo.application.shared.command.CreateExcelTemplateCommand;
import com.example.demo.application.shared.command.DeleteExcelTemplateCommand;
import com.example.demo.application.shared.command.ExportTemplateExcelCommand;
import com.example.demo.application.shared.command.ExportTemplateWithMappingCommand;
import com.example.demo.application.shared.command.UpdateExcelTemplateCommand;
import com.example.demo.application.shared.dto.ExcelTemplateGottenResult;
import com.example.demo.application.shared.query.GetExcelTemplateQuery;
import com.example.demo.application.shared.query.ListExcelTemplateQuery;
import com.example.demo.presentation.resource.in.CreateExcelTemplateResource;
import com.example.demo.presentation.resource.in.ExportTemplateExcelResource;
import com.example.demo.presentation.resource.in.ExportTemplateWithMappingResource;
import com.example.demo.presentation.resource.in.UpdateExcelTemplateResource;
import com.example.demo.presentation.resource.out.ExcelTemplateCreatedResource;
import com.example.demo.presentation.resource.out.ExcelTemplateRetrievedResource;
import com.example.demo.presentation.resource.out.ExcelTemplateUpdatedResource;

/**
 * 範本 Excel 表現層防腐組裝器 (ACL Assembler)。
 * <p>
 * 遵循 Clean Architecture 職責分離規範，獨立於 Resource 之外，
 * 負責表現層資源 ({@link ExportTemplateExcelResource}, {@link ExportTemplateWithMappingResource},
 * {@link CreateExcelTemplateResource}, {@link UpdateExcelTemplateResource},
 * {@link ExcelTemplateRetrievedResource}, {@link ExcelTemplateCreatedResource}, {@link ExcelTemplateUpdatedResource}) 與
 * 應用層模型 ({@link ExportTemplateExcelCommand}, {@link ExportTemplateWithMappingCommand},
 * {@link CreateExcelTemplateCommand}, {@link UpdateExcelTemplateCommand}, {@link DeleteExcelTemplateCommand},
 * {@link GetExcelTemplateQuery}, {@link ListExcelTemplateQuery}, {@link ExcelTemplateGottenResult}) 間之雙向資料映射。
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
     * 將建立範本請求資源轉換為應用層命令 (Create Resource -> Command)。
     *
     * @param resource 建立範本請求本文
     * @return 建立範本命令載體
     */
    public CreateExcelTemplateCommand toCommand(CreateExcelTemplateResource resource) {
        Objects.requireNonNull(resource, "resource 不得為空");
        return new CreateExcelTemplateCommand(
            resource.templateKey(),
            resource.templateName(),
            resource.category(),
            resource.description(),
            resource.requiredVariables(),
            resource.storageType(),
            resource.filePath(),
            resource.defaultMappingJson()
        );
    }

    /**
     * 將更新範本請求資源轉換為應用層命令 (Update Resource -> Command)。
     *
     * @param templateKey 網址路徑中的範本識別鍵
     * @param resource    更新範本請求本文
     * @return 更新範本命令載體
     */
    public UpdateExcelTemplateCommand toCommand(String templateKey, UpdateExcelTemplateResource resource) {
        Objects.requireNonNull(templateKey, "templateKey 不得為空");
        Objects.requireNonNull(resource, "resource 不得為空");
        return new UpdateExcelTemplateCommand(
            templateKey,
            resource.templateName(),
            resource.category(),
            resource.description(),
            resource.requiredVariables(),
            resource.storageType(),
            resource.filePath(),
            resource.defaultMappingJson(),
            resource.active()
        );
    }

    /**
     * 將路徑參數轉換為刪除範本命令 (Path Variable -> Command)。
     *
     * @param templateKey 範本識別鍵
     * @return 刪除範本命令載體
     */
    public DeleteExcelTemplateCommand toDeleteCommand(String templateKey) {
        Objects.requireNonNull(templateKey, "templateKey 不得為空");
        return new DeleteExcelTemplateCommand(templateKey);
    }

    /**
     * 將路徑參數轉換為單一查詢載體 (Path Variable -> Query)。
     *
     * @param templateKey 範本識別鍵
     * @return 查詢載體
     */
    public GetExcelTemplateQuery toQuery(String templateKey) {
        Objects.requireNonNull(templateKey, "templateKey 不得為空");
        return new GetExcelTemplateQuery(templateKey);
    }

    /**
     * 將查詢參數轉換為清單查詢載體 (Query Params -> Query)。
     *
     * @param category   業務分類
     * @param activeOnly 是否僅查詢啟用
     * @return 清單查詢載體
     */
    public ListExcelTemplateQuery toListQuery(String category, Boolean activeOnly) {
        return new ListExcelTemplateQuery(category, activeOnly);
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

    /**
     * 將應用層範本中繼規格 DTO 轉換為建立回應資源 (DTO -> Created Resource)。
     *
     * @param dto 應用層範本中繼資訊 DTO
     * @return 表現層建立回應資源
     */
    public ExcelTemplateCreatedResource toCreatedResource(ExcelTemplateGottenResult dto) {
        Objects.requireNonNull(dto, "dto 不得為空");
        return new ExcelTemplateCreatedResource(
            dto.templateKey(),
            dto.templateName(),
            dto.category(),
            dto.description(),
            dto.requiredVariables(),
            dto.lastModifiedAt()
        );
    }

    /**
     * 將應用層範本中繼規格 DTO 轉換為更新回應資源 (DTO -> Updated Resource)。
     *
     * @param dto 應用層範本中繼資訊 DTO
     * @return 表現層更新回應資源
     */
    public ExcelTemplateUpdatedResource toUpdatedResource(ExcelTemplateGottenResult dto) {
        Objects.requireNonNull(dto, "dto 不得為空");
        return new ExcelTemplateUpdatedResource(
            dto.templateKey(),
            dto.templateName(),
            dto.category(),
            dto.description(),
            dto.requiredVariables(),
            dto.lastModifiedAt()
        );
    }
}

