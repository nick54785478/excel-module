package com.example.demo.infrastructure.adapter.template;

import com.example.demo.application.port.out.template.ExcelTemplateModelCustomizerFactoryPort;
import com.example.demo.application.shared.template.ExcelTemplateModelCustomizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Excel 範本資料模型自定義器工廠適配器 (Excel Template Model Customizer Factory Adapter - Outbound Adapter)。
 * <p>
 * 遵循 AGENTS.md 規則 3-9，Outbound Adapter 一律以 {@code Adapter} 結尾，
 * 位於 {@code infrastructure.adapter.template} 套件下，作為應用層輸出埠的對外技術實作。
 * 實作應用層 {@link ExcelTemplateModelCustomizerFactoryPort} 技術合約，
 * 集中管理與路由所有 {@link ExcelTemplateModelCustomizer} 實例。
 * 提供基於範本鍵值 (templateKey) 之大小寫不敏感動態查找，並支援通用保底降級。
 * </p>
 */
@Slf4j
@Component
public class ExcelTemplateModelCustomizerFactoryAdapter implements ExcelTemplateModelCustomizerFactoryPort {

    private final Map<String, ExcelTemplateModelCustomizer> registry = new ConcurrentHashMap<>();
    private final ExcelTemplateModelCustomizer fallbackCustomizer;

    /**
     * 建構函式：自動裝配 Spring 容器中所有 ExcelTemplateModelCustomizer 策略實例。
     *
     * @param customizers 策略實例清單 (可為 null)
     */
    @Autowired
    public ExcelTemplateModelCustomizerFactoryAdapter(@Autowired(required = false) List<ExcelTemplateModelCustomizer> customizers) {
        ExcelTemplateModelCustomizer detectedFallback = ExcelTemplateModelCustomizer.NO_OP;
        if (customizers != null) {
            for (ExcelTemplateModelCustomizer c : customizers) {
                String key = c.getTemplateKey();
                if (key != null && !key.isBlank() && !"*".equals(key)) {
                    registry.put(key.toUpperCase(Locale.ROOT), c);
                    log.info("註冊 Excel 範本資料模型自定義策略: key={}, class={}", key, c.getClass().getSimpleName());
                } else if ("*".equals(key)) {
                    detectedFallback = c;
                    log.info("註冊 Excel 範本資料模型通用保底自定義策略: class={}", c.getClass().getSimpleName());
                }
            }
        }
        this.fallbackCustomizer = detectedFallback;
    }

    @Override
    public ExcelTemplateModelCustomizer getCustomizer(String templateKey) {
        if (templateKey == null || templateKey.isBlank()) {
            return fallbackCustomizer;
        }
        ExcelTemplateModelCustomizer exactMatch = registry.get(templateKey.toUpperCase(Locale.ROOT));
        if (exactMatch != null) {
            return exactMatch;
        }
        return fallbackCustomizer;
    }

    @Override
    public boolean hasCustomizer(String templateKey) {
        if (templateKey == null || templateKey.isBlank()) {
            return false;
        }
        return registry.containsKey(templateKey.toUpperCase(Locale.ROOT));
    }
}
