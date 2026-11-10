package com.example.demo.application.port.out.template;

import com.example.demo.application.shared.mapping.TemplateMappingDefinition;
import java.util.Map;

/**
 * JXLS 範本欄位動態映射輸出埠 (Outbound Port)。
 * <p>
 * 遵循 AGENTS.md 規則 3-8，Outbound Port 一律以 {@code Port} 結尾。
 * 代表應用層對基礎設施層所要求之合約：
 * 接收純業務資料（如 Material/Bom Aggregate 或 DTO 集合）與欄位映射規則，
 * 負責執行巢狀路徑屬性導航、Value Object 自動開箱解構、欄位格式轉換與虛擬運算，
 * 產出符合 JXLS 範本變數期望之結構化 Context Map。
 * </p>
 */
public interface ExcelTemplateFieldMapperPort {

    /**
     * 將業務資料來源動態映射並轉換為 JXLS 範本填充上下文 Map。
     *
     * @param sourceData 業務實體、聚合根或其集合清單 (例如 {@code List<Material>})
     * @param mappingDefinition 範本欄位映射規格定義
     * @return 供 JXLS 渲染直接消費之資料模型 Map (包含根集合清單與全域變數)
     */
    Map<String, Object> mapToTemplateContext(Object sourceData, TemplateMappingDefinition mappingDefinition);
}

