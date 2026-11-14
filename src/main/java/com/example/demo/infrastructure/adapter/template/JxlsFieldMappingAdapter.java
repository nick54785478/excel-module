package com.example.demo.infrastructure.adapter.template;

import com.example.demo.application.port.out.template.ExcelTemplateFieldMapperPort;
import com.example.demo.application.shared.mapping.TemplateMappingDefinition;
import com.example.demo.infrastructure.template.mapping.engine.JxlsFieldMappingEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 * JXLS 範本欄位動態映射適配器 (Outbound Adapter)。
 * <p>
 * 遵循 AGENTS.md 規則 3-9，適配器一律以 {@code Adapter} 結尾。
 * 實作應用層 {@link ExcelTemplateFieldMapperPort} 輸出埠技術合約，
 * 委派基礎設施專屬之 {@link JxlsFieldMappingEngine} 進行屬性導航、
 * Value Object 開箱、格式轉換策略與 JXLS 上下文組裝。
 * </p>
 */
@Slf4j
@Component
public class JxlsFieldMappingAdapter implements ExcelTemplateFieldMapperPort {

    private final JxlsFieldMappingEngine fieldMappingEngine;

    /**
     * 建構函式：注入基礎設施層核心映射引擎。
     *
     * @param fieldMappingEngine 欄位動態映射引擎
     */
    public JxlsFieldMappingAdapter(JxlsFieldMappingEngine fieldMappingEngine) {
        this.fieldMappingEngine = Objects.requireNonNull(fieldMappingEngine, "fieldMappingEngine 不得為空");
    }

    @Override
    public Map<String, Object> mapToTemplateContext(Object sourceData, TemplateMappingDefinition mappingDefinition) {
        Objects.requireNonNull(mappingDefinition, "mappingDefinition 不得為空");
        log.debug("JxlsFieldMappingAdapter 開始處理範本 [{}] 之欄位對齊與轉換", mappingDefinition.templateKey());
        return fieldMappingEngine.mapToContext(sourceData, mappingDefinition);
    }
}

