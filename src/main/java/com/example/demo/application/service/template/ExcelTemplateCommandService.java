package com.example.demo.application.service.template;

import com.example.demo.application.port.in.template.ExportTemplateExcelUseCase;
import com.example.demo.application.port.in.template.ExportTemplateWithMappingUseCase;
import com.example.demo.application.port.in.template.PreviewTemplateMappingUseCase;
import com.example.demo.application.port.in.template.StreamExportTemplateExcelUseCase;
import com.example.demo.application.port.in.template.StreamExportTemplateWithMappingUseCase;
import com.example.demo.application.port.out.template.ExcelTemplateFieldMapperPort;
import com.example.demo.application.port.out.template.ExcelTemplateRegistryPort;
import com.example.demo.application.port.out.template.ExcelTemplateWriterPort;
import com.example.demo.application.port.out.bom.MaterialRepositoryPort;
import com.example.demo.application.shared.command.ExportTemplateExcelCommand;
import com.example.demo.application.shared.command.ExportTemplateWithMappingCommand;
import com.example.demo.application.shared.exception.ExcelTemplateNotFoundException;
import com.example.demo.application.shared.exception.ExcelWriteException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 範本 Excel 匯出應用層服務 (Application Service)。
 * <p>
 * 遵循 AGENTS.md 規則 3-9，以 {@code *CommandService} 命名。
 * 實作 {@link ExportTemplateExcelUseCase}、{@link StreamExportTemplateExcelUseCase}、
 * {@link ExportTemplateWithMappingUseCase}、{@link StreamExportTemplateWithMappingUseCase}
 * 與 {@link PreviewTemplateMappingUseCase} 輸入埠，
 * 負責協調範本儲存庫輸出埠 ({@link ExcelTemplateRegistryPort})、範本寫入輸出埠 ({@link ExcelTemplateWriterPort})
 * 以及欄位動態映射輸出埠 ({@link ExcelTemplateFieldMapperPort})。
 * 內部嚴格僅與純命令物件與抽象輸出埠互動，無任何基礎設施技術依賴。
 * </p>
 */
@Service
public class ExcelTemplateCommandService implements
        ExportTemplateExcelUseCase,
        StreamExportTemplateExcelUseCase,
        ExportTemplateWithMappingUseCase,
        StreamExportTemplateWithMappingUseCase,
        PreviewTemplateMappingUseCase {

    private final ExcelTemplateRegistryPort templateRegistryPort;
    private final ExcelTemplateWriterPort templateWriterPort;
    private final ExcelTemplateFieldMapperPort templateFieldMapperPort;
    private final MaterialRepositoryPort materialRepositoryPort;

    /**
     * 相容輕量建構函式：僅注入範本註冊表與寫入輸出埠。
     *
     * @param templateRegistryPort 範本儲存庫輸出埠
     * @param templateWriterPort   範本寫入輸出埠
     */
    public ExcelTemplateCommandService(ExcelTemplateRegistryPort templateRegistryPort,
                                       ExcelTemplateWriterPort templateWriterPort) {
        this(templateRegistryPort, templateWriterPort, null, null);
    }

    /**
     * 完整相依建構函式：支援自訂欄位動態映射輸出埠。
     *
     * @param templateRegistryPort   範本儲存庫輸出埠
     * @param templateWriterPort     範本寫入輸出埠
     * @param templateFieldMapperPort 欄位動態映射輸出埠 (可為 null)
     * @param materialRepositoryPort 物料領域儲存庫輸出埠 (可為 null)
     */
    @Autowired
    public ExcelTemplateCommandService(ExcelTemplateRegistryPort templateRegistryPort,
                                       ExcelTemplateWriterPort templateWriterPort,
                                       @Autowired(required = false) ExcelTemplateFieldMapperPort templateFieldMapperPort,
                                       @Autowired(required = false) MaterialRepositoryPort materialRepositoryPort) {
        this.templateRegistryPort = Objects.requireNonNull(templateRegistryPort, "templateRegistryPort 不得為空");
        this.templateWriterPort = Objects.requireNonNull(templateWriterPort, "templateWriterPort 不得為空");
        this.templateFieldMapperPort = templateFieldMapperPort;
        this.materialRepositoryPort = materialRepositoryPort;
    }

    @Override
    public byte[] exportFromTemplate(ExportTemplateExcelCommand command) {
        Objects.requireNonNull(command, "command 不得為空");
        validateTemplateExistence(command.templateKey());

        try (InputStream templateStream = templateRegistryPort.openTemplateStream(command.templateKey())) {
            return templateWriterPort.writeFromTemplate(templateStream, command.dataModel());
        } catch (ExcelTemplateNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new ExcelWriteException("範本 Excel 匯出執行遭遇異常: " + command.templateKey(), e);
        }
    }

    @Override
    public void streamExportFromTemplate(OutputStream outputStream, ExportTemplateExcelCommand command) {
        Objects.requireNonNull(outputStream, "outputStream 不得為空");
        Objects.requireNonNull(command, "command 不得為空");
        validateTemplateExistence(command.templateKey());

        try (InputStream templateStream = templateRegistryPort.openTemplateStream(command.templateKey())) {
            templateWriterPort.streamWriteFromTemplate(templateStream, outputStream, command.dataModel());
        } catch (ExcelTemplateNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new ExcelWriteException("範本 Excel 串流匯出執行遭遇異常: " + command.templateKey(), e);
        }
    }

    @Override
    public byte[] exportFromTemplateWithMapping(ExportTemplateWithMappingCommand command) {
        Objects.requireNonNull(command, "command 不得為空");
        validateTemplateExistence(command.templateKey());

        Map<String, Object> dataModel = resolveAndMapContext(command);

        try (InputStream templateStream = templateRegistryPort.openTemplateStream(command.templateKey())) {
            return templateWriterPort.writeFromTemplate(templateStream, dataModel);
        } catch (ExcelTemplateNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new ExcelWriteException("具自訂映射之範本 Excel 匯出執行遭遇異常: " + command.templateKey(), e);
        }
    }

    @Override
    public void streamExportFromTemplateWithMapping(OutputStream outputStream, ExportTemplateWithMappingCommand command) {
        Objects.requireNonNull(outputStream, "outputStream 不得為空");
        Objects.requireNonNull(command, "command 不得為空");
        validateTemplateExistence(command.templateKey());

        Map<String, Object> dataModel = resolveAndMapContext(command);

        try (InputStream templateStream = templateRegistryPort.openTemplateStream(command.templateKey())) {
            templateWriterPort.streamWriteFromTemplate(templateStream, outputStream, dataModel);
        } catch (ExcelTemplateNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new ExcelWriteException("具自訂映射之範本 Excel 串流匯出執行遭遇異常: " + command.templateKey(), e);
        }
    }

    @Override
    public Map<String, Object> previewTemplateMapping(ExportTemplateWithMappingCommand command) {
        Objects.requireNonNull(command, "command 不得為空");
        return resolveAndMapContext(command);
    }

    private Map<String, Object> resolveAndMapContext(ExportTemplateWithMappingCommand command) {
        if (templateFieldMapperPort == null) {
            throw new IllegalStateException("未配置 ExcelTemplateFieldMapperPort，無法執行動態欄位映射");
        }
        Object sourceData = resolveSourceData(command);
        return templateFieldMapperPort.mapToTemplateContext(sourceData, command.mappingDefinition());
    }

    private Object resolveSourceData(ExportTemplateWithMappingCommand command) {
        if (command.sourceData() != null) {
            return command.sourceData();
        }
        // 若客戶端未提供自訂資料，且範本為 material_master 相關，預設自動自領域儲存庫查詢現有物料實體
        if (materialRepositoryPort != null && command.templateKey().toLowerCase().contains("material")) {
            return materialRepositoryPort.findAll();
        }
        return Collections.emptyList();
    }

    /**
     * 檢驗範本是否存在於儲存庫中，若不存在則拋出領域特定例外。
     *
     * @param templateKey 範本識別鍵
     */
    private void validateTemplateExistence(String templateKey) {
        if (!templateRegistryPort.exists(templateKey)) {
            throw new ExcelTemplateNotFoundException("指定的 Excel 範本不存在或未註冊: " + templateKey);
        }
    }
}
