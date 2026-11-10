package com.example.demo.infrastructure.template.mapping.registry;

import com.example.demo.application.shared.mapping.TemplateMappingDefinition;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.Reader;
import java.util.Objects;

/**
 * JSON 格式範本欄位映射規格載入器 (JSON Template Mapping Loader)。
 * <p>
 * 支援將外部 JSON 字串、輸入串流反序列化為不可變 {@link TemplateMappingDefinition} 領域規格模型，
 * 便於微服務接收前端自選欄位或自關聯式資料庫讀取多租戶配置。
 * </p>
 */
@Slf4j
@Component
public class JsonTemplateMappingLoader {

    private final ObjectMapper objectMapper;

    public JsonTemplateMappingLoader() {
        this(createDefaultObjectMapper());
    }

    public JsonTemplateMappingLoader(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper 不得為空");
    }

    /**
     * 從 JSON 字串解析範本映射規格定義。
     *
     * @param jsonString JSON 字串
     * @return 映射規格定義 Record
     */
    public TemplateMappingDefinition fromJson(String jsonString) {
        Objects.requireNonNull(jsonString, "jsonString 不得為空");
        try {
            return objectMapper.readValue(jsonString, TemplateMappingDefinition.class);
        } catch (Exception e) {
            log.error("解析 JSON 範本欄位映射規格失敗: {}", e.getMessage(), e);
            throw new IllegalArgumentException("JSON 範本欄位映射規格格式無效: " + e.getMessage(), e);
        }
    }

    /**
     * 從輸入串流解析範本映射規格定義。
     *
     * @param inputStream 輸入串流
     * @return 映射規格定義 Record
     */
    public TemplateMappingDefinition fromJson(InputStream inputStream) {
        Objects.requireNonNull(inputStream, "inputStream 不得為空");
        try {
            return objectMapper.readValue(inputStream, TemplateMappingDefinition.class);
        } catch (Exception e) {
            log.error("從 InputStream 解析 JSON 範本映射規格失敗: {}", e.getMessage(), e);
            throw new IllegalArgumentException("InputStream 範本欄位映射規格格式無效: " + e.getMessage(), e);
        }
    }

    /**
     * 將範本映射規格定義序列化為 JSON 字串。
     *
     * @param definition 映射規格定義
     * @return 格式化之 JSON 字串
     */
    public String toJson(TemplateMappingDefinition definition) {
        Objects.requireNonNull(definition, "definition 不得為空");
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(definition);
        } catch (Exception e) {
            log.error("序列化範本欄位映射規格為 JSON 失敗: {}", e.getMessage(), e);
            throw new IllegalStateException("序列化範本映射規格失敗: " + e.getMessage(), e);
        }
    }

    private static ObjectMapper createDefaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }
}

