package com.example.demo.infrastructure.template.mapping.registry;

import com.example.demo.application.shared.mapping.TemplateMappingDefinition;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 範本欄位動態映射規格註冊表 (Template Mapping Registry)。
 * <p>
 * 座落於基礎設施層內部，維護全系統預載及動態註冊之 {@link TemplateMappingDefinition} 快取。
 * 支援執行緒安全之動態熱更新與快速查詢。
 * </p>
 */
@Slf4j
@Component
public class TemplateMappingRegistry {

    private final Map<String, TemplateMappingDefinition> registry = new ConcurrentHashMap<>();

    /**
     * 註冊範本欄位映射規格。
     *
     * @param definition 映射規格定義
     */
    public void register(TemplateMappingDefinition definition) {
        Objects.requireNonNull(definition, "definition 不得為空");
        log.info("註冊 JXLS 範本欄位映射規格: key='{}', collection='{}', rules={}",
            definition.templateKey(), definition.rootCollectionVar(), definition.rules().size());
        registry.put(definition.templateKey().toLowerCase(), definition);
    }

    /**
     * 依據範本鍵查詢映射規格定義。
     *
     * @param templateKey 範本識別鍵
     * @return Optional 包裝之映射定義
     */
    public Optional<TemplateMappingDefinition> find(String templateKey) {
        if (templateKey == null || templateKey.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(registry.get(templateKey.toLowerCase()));
    }

    /**
     * 檢查指定範本鍵是否已註冊。
     *
     * @param templateKey 範本識別鍵
     * @return true 若已存在
     */
    public boolean contains(String templateKey) {
        if (templateKey == null) {
            return false;
        }
        return registry.containsKey(templateKey.toLowerCase());
    }

    /**
     * 移除指定範本映射定義。
     *
     * @param templateKey 範本識別鍵
     * @return 移除前之定義物件
     */
    public TemplateMappingDefinition unregister(String templateKey) {
        if (templateKey == null) {
            return null;
        }
        return registry.remove(templateKey.toLowerCase());
    }

    /**
     * 取得目前所有已註冊之映射規格集合。
     *
     * @return 映射規格集合
     */
    public Collection<TemplateMappingDefinition> getAll() {
        return registry.values();
    }
}

