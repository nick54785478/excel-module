package com.example.demo.infrastructure.adapter.template;

import com.example.demo.application.port.out.template.ExcelTemplateWriterPort;
import com.example.demo.application.shared.exception.ExcelWriteException;
import org.jxls.builder.JxlsOutput;
import org.jxls.expression.ExpressionEvaluatorFactory;
import org.jxls.expression.ExpressionEvaluatorFactoryJexlImpl;
import org.jxls.expression.JxlsJexlPermissions;
import org.jxls.transform.poi.JxlsPoiTemplateFillerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * JXLS 3.x 宣告式範本寫入適配器 (Outbound Adapter)。
 * <p>
 * 遵循 AGENTS.md 規則 3-9，適配器一律以 {@code Adapter} 結尾。
 * 實作應用層 {@link ExcelTemplateWriterPort} 技術合約，
 * 封裝 JXLS 3.x 範本填充核心 ({@link JxlsPoiTemplateFillerBuilder})、
 * JEXL 安全沙箱權限 ({@link JxlsJexlPermissions#RESTRICTED}) 與 POI 渲染轉換。
 * 內建智慧欄位別名正規化機制 (Smart Alias Normalization)，自動相容 itemCode/materialCode、quantity/requiredQuantity 等常見命名變體。
 * </p>
 */
@Component
public class JxlsTemplateWriterAdapter implements ExcelTemplateWriterPort {

    private static final Logger log = LoggerFactory.getLogger(JxlsTemplateWriterAdapter.class);

    private final ExpressionEvaluatorFactory expressionEvaluatorFactory;

    /**
     * 預設建構函式：初始化啟用 RESTRICTED 沙箱權限之 JEXL 表達式評估工廠。
     * 防範 CVE-2020-13973 與 CVE-2022-26136 等 RCE 任意代碼執行風險。
     */
    public JxlsTemplateWriterAdapter() {
        this(createDefaultSandboxEvaluatorFactory());
    }

    /**
     * 建構函式：注入自訂之表達式評估工廠。
     *
     * @param expressionEvaluatorFactory 表達式評估工廠
     */
    public JxlsTemplateWriterAdapter(ExpressionEvaluatorFactory expressionEvaluatorFactory) {
        this.expressionEvaluatorFactory = Objects.requireNonNull(expressionEvaluatorFactory, "expressionEvaluatorFactory 不得為空");
    }

    @Override
    public byte[] writeFromTemplate(InputStream templateStream, Map<String, Object> dataModel) {
        Objects.requireNonNull(templateStream, "templateStream 不得為空");
        try {
            log.debug("開始以 JXLS 3.x 執行範本填充並產出位元組陣列 (In-Memory 模式)");
            Map<String, Object> model = normalizeDataModel(dataModel);
            return JxlsPoiTemplateFillerBuilder.newInstance()
                .withTemplate(templateStream)
                .withExpressionEvaluatorFactory(expressionEvaluatorFactory)
                .withExceptionThrower()
                .withRecalculateFormulasOnOpening(true)
                .buildAndFill(model);
        } catch (Exception e) {
            log.error("JXLS 範本寫入失敗: {}", e.getMessage(), e);
            throw new ExcelWriteException("JXLS 範本寫入失敗: " + e.getMessage(), e);
        }
    }

    @Override
    public void streamWriteFromTemplate(InputStream templateStream, OutputStream targetStream, Map<String, Object> dataModel) {
        Objects.requireNonNull(templateStream, "templateStream 不得為空");
        Objects.requireNonNull(targetStream, "targetStream 不得為空");
        try {
            log.debug("開始以 JXLS 3.x 執行範本填充並直通輸出串流 (Streaming 模式)");
            Map<String, Object> model = normalizeDataModel(dataModel);
            JxlsOutput output = () -> targetStream;
            JxlsPoiTemplateFillerBuilder.newInstance()
                .withTemplate(templateStream)
                .withExpressionEvaluatorFactory(expressionEvaluatorFactory)
                .withExceptionThrower()
                .withRecalculateFormulasOnOpening(true)
                .buildAndFill(model, output);
            targetStream.flush();
        } catch (Exception e) {
            log.error("JXLS 範本串流寫入失敗: {}", e.getMessage(), e);
            throw new ExcelWriteException("JXLS 範本串流寫入失敗: " + e.getMessage(), e);
        }
    }

    /**
     * 對資料模型執行智慧欄位別名正規化與補強 (Smart Alias Normalization)。
     * <p>
     * 支援跨系統、不同命名慣例 (如 itemCode vs materialCode, quantity vs requiredQuantity, vendorName 等)
     * 之雙向自動映射，確保客戶端傳入常見同義欄位時，JXLS 範本皆能順暢解析取值，避免空白儲存格。
     * </p>
     *
     * @param sourceModel 原始傳入之資料上下文 Map
     * @return 經正規化與別名擴充後之資料上下文 Map
     */
    public static Map<String, Object> normalizeDataModel(Map<String, Object> sourceModel) {
        if (sourceModel == null || sourceModel.isEmpty()) {
            return new HashMap<>();
        }
        Map<String, Object> result = new LinkedHashMap<>(sourceModel);

        // 1. 若只有 quotationItems 但範本使用 items，或反之，自動雙向對齊
        if (result.containsKey("quotationItems") && !result.containsKey("items")) {
            result.put("items", result.get("quotationItems"));
        } else if (result.containsKey("items") && !result.containsKey("quotationItems")) {
            result.put("quotationItems", result.get("items"));
        }
        if (result.containsKey("materials") && !result.containsKey("items")) {
            result.put("items", result.get("materials"));
        }

        String defaultVendor = null;
        if (result.get("vendorNames") instanceof String vn && !vn.isBlank()) {
            defaultVendor = vn.split("[,，、]")[0].trim();
        }

        // 2. 遞迴正規化集合內各元素之 Map 欄位別名
        for (Map.Entry<String, Object> entry : new ArrayList<>(result.entrySet())) {
            Object val = entry.getValue();
            if (val instanceof List<?> list) {
                List<Object> normalizedList = new ArrayList<>(list.size());
                for (Object item : list) {
                    if (item instanceof Map<?, ?> rawMap) {
                        Map<String, Object> itemMap = new LinkedHashMap<>();
                        for (Map.Entry<?, ?> e : rawMap.entrySet()) {
                            if (e.getKey() != null) {
                                itemMap.put(e.getKey().toString(), e.getValue());
                            }
                        }
                        enrichItemAliases(itemMap, defaultVendor);
                        normalizedList.add(itemMap);
                    } else {
                        normalizedList.add(item);
                    }
                }
                result.put(entry.getKey(), normalizedList);
            }
        }

        return result;
    }

    private static void enrichItemAliases(Map<String, Object> map, String defaultVendor) {
        // 料號別名互通
        aliasIfMissing(map, "materialCode", "itemCode", "childMaterialCode", "code");
        aliasIfMissing(map, "itemCode", "materialCode", "childMaterialCode", "code");

        // 品名別名互通
        aliasIfMissing(map, "materialName", "itemName", "childMaterialName", "name");
        aliasIfMissing(map, "itemName", "materialName", "childMaterialName", "name");

        // 規格別名互通
        aliasIfMissing(map, "specification", "spec");
        aliasIfMissing(map, "spec", "specification");

        // 數量別名互通
        aliasIfMissing(map, "requiredQuantity", "quantity", "baseQuantity", "qty");
        aliasIfMissing(map, "quantity", "requiredQuantity", "baseQuantity", "qty");

        // 單價 / 報價別名互通
        aliasIfMissing(map, "quoteUnitPrice", "lowestPrice", "targetPrice", "unitPrice", "price", "standardCost");
        aliasIfMissing(map, "unitPrice", "quoteUnitPrice", "lowestPrice", "targetPrice", "standardCost");
        aliasIfMissing(map, "standardCost", "quoteUnitPrice", "unitPrice", "lowestPrice", "targetPrice");

        // 供應商別名互通
        if (!map.containsKey("vendorName")) {
            if (map.containsKey("vendor")) {
                map.put("vendorName", map.get("vendor"));
            } else if (map.containsKey("supplier")) {
                map.put("vendorName", map.get("supplier"));
            } else if (defaultVendor != null && !defaultVendor.isBlank()) {
                map.put("vendorName", defaultVendor);
            }
        }
    }

    private static void aliasIfMissing(Map<String, Object> map, String targetKey, String... candidateKeys) {
        if (!map.containsKey(targetKey) || map.get(targetKey) == null) {
            for (String candidate : candidateKeys) {
                if (map.containsKey(candidate) && map.get(candidate) != null) {
                    map.put(targetKey, map.get(candidate));
                    break;
                }
            }
        }
    }

    /**
     * 建立預設安全沙箱評估工廠：
     * 1. silent = false (語法錯誤顯式拋出以利除錯)
     * 2. strict = true (未宣告變數或方法視為錯誤)
     * 3. permissions = JxlsJexlPermissions.RESTRICTED (限制類別與反射調用)
     *
     * @return 具備沙箱防護之 ExpressionEvaluatorFactory
     */
    public static ExpressionEvaluatorFactory createDefaultSandboxEvaluatorFactory() {
        return new ExpressionEvaluatorFactoryJexlImpl(
            false,
            true,
            JxlsJexlPermissions.RESTRICTED
        );
    }
}
