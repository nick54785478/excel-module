package com.example.demo.application.port.in.template;

import com.example.demo.application.shared.command.ExportTemplateWithMappingCommand;
import java.util.Map;

/**
 * 範本欄位動態映射預覽用例 (Inbound Port / Use Case)。
 * <p>
 * 遵循 AGENTS.md 規則 3-8，Inbound Port 一律以 {@code UseCase} 結尾。
 * 負責執行屬性解析、Value Object 開箱、格式轉換策略與 SpEL 虛擬表達式運算，
 * 回傳組裝後之 JXLS 資料模型 Context Map (供 API 檢核與前端即時除錯，不進行實體 Excel 渲染)。
 * </p>
 */
public interface PreviewTemplateMappingUseCase {

    /**
     * 預覽欄位動態映射產出之 JXLS 上下文資料模型。
     *
     * @param command 具備自訂映射之範本匯出命令
     * @return 供 JXLS 渲染消費之資料模型 Map
     */
    Map<String, Object> previewTemplateMapping(ExportTemplateWithMappingCommand command);
}

