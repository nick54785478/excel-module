package com.example.demo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.demo.application.port.out.template.ExcelTemplateFieldMapperPort;
import com.example.demo.application.port.out.template.ExcelTemplateRegistryPort;
import com.example.demo.application.port.out.template.ExcelTemplateWriterPort;
import com.example.demo.infrastructure.adapter.template.ClasspathExcelTemplateRegistryAdapter;
import com.example.demo.infrastructure.adapter.template.JxlsFieldMappingAdapter;
import com.example.demo.infrastructure.adapter.template.JxlsTemplateWriterAdapter;
import com.example.demo.infrastructure.persistence.template.repository.ExcelTemplateJpaRepository;
import com.example.demo.infrastructure.template.mapping.engine.JxlsFieldMappingEngine;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JXLS 宣告式範本引擎全域配置類別 (Configuration Layer - Composition Root)。
 * <p>
 * 遵循 Clean Architecture 六角形架構規範，座落於專案根目錄下的 {@code config/} 套件中。
 * 負責裝配 JXLS 3.x 範本寫入適配器 ({@link JxlsTemplateWriterAdapter}) 與
 * 範本儲存庫適配器 ({@link ClasspathExcelTemplateRegistryAdapter}) 至應用層輸出埠。
 * </p>
 */
@Configuration
public class ExcelJxlsConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ExcelJxlsConfiguration.class);

    @Value("${excel.template.prefix:templates/excel/}")
    private String templatePrefix;

    @PostConstruct
    public void init() {
        log.info("==================================================================");
        log.info(" [ExcelJxlsConfiguration] JXLS 3.x 宣告式範本定義模組已啟動");
        log.info("  • 引擎核心: JXLS 3.2.0 (POI 5.x Transformer)");
        log.info("  • 安全沙箱: JxlsJexlPermissions.RESTRICTED (CVE 防禦生效)");
        log.info("  • 預設範本路徑: classpath:{}", templatePrefix);
        log.info("==================================================================");
    }

    /**
     * 註冊範本寫入輸出埠實作 (JxlsTemplateWriterAdapter)。
     *
     * @return ExcelTemplateWriterPort 實例
     */
    @Bean
    @ConditionalOnMissingBean(ExcelTemplateWriterPort.class)
    public ExcelTemplateWriterPort excelTemplateWriterPort() {
        return new JxlsTemplateWriterAdapter();
    }

    /**
     * 註冊範本儲存庫輸出埠實作 (ClasspathExcelTemplateRegistryAdapter)。
     *
     * @param templateRepository Excel 範本 JPA 持久化儲存庫 (可選注入)
     * @return ExcelTemplateRegistryPort 實例
     */
    @Bean
    @ConditionalOnMissingBean(ExcelTemplateRegistryPort.class)
    public ExcelTemplateRegistryPort excelTemplateRegistryPort(
            @Autowired(required = false) ExcelTemplateJpaRepository templateRepository) {
        return new ClasspathExcelTemplateRegistryAdapter(templatePrefix, templateRepository);
    }

    /**
     * 註冊範本欄位動態映射輸出埠實作 (JxlsFieldMappingAdapter)。
     *
     * @param engine 欄位動態映射核心引擎
     * @return ExcelTemplateFieldMapperPort 實例
     */
    @Bean
    @ConditionalOnMissingBean(ExcelTemplateFieldMapperPort.class)
    public ExcelTemplateFieldMapperPort excelTemplateFieldMapperPort(JxlsFieldMappingEngine engine) {
        return new JxlsFieldMappingAdapter(engine);
    }
}

