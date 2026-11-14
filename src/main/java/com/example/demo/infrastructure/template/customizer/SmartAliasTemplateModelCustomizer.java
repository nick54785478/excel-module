package com.example.demo.infrastructure.template.customizer;

import com.example.demo.application.shared.template.ExcelTemplateModelCustomizer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 通用智慧欄位別名範本資料模型自定義策略 (Smart Alias Template Model Customizer / Fallback Normalizer Strategy)。
 * <p>
 * 座落於 {@code infrastructure.template.customizer} 套件，
 * 實作應用層 {@link ExcelTemplateModelCustomizer} 策略契約，
 * 綁定 {@code "*"} 通用識別鍵，作為未匹配專屬自定義器時的保底策略：
 * 支援跨系統常見的欄位命名差異（如料號、品名、數量、規格、單價等），
 * 避免在未特別客製化之範本中因欄位別名微小差異導致報表留白。
 * </p>
 */
@Component
public class SmartAliasTemplateModelCustomizer implements ExcelTemplateModelCustomizer {

    @Override
    public String getTemplateKey() {
        return "*";
    }

    @Override
    public Map<String, Object> customize(Map<String, Object> dataModel) {
        if (dataModel == null || dataModel.isEmpty()) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> result = new LinkedHashMap<>(dataModel);

        alignCommonCollections(result);

        for (Map.Entry<String, Object> entry : result.entrySet()) {
            if (entry.getValue() instanceof List<?> list) {
                entry.setValue(normalizeList(list));
            }
        }

        return result;
    }

    private void alignCommonCollections(Map<String, Object> model) {
        Object quotationItems = model.get("quotationItems");
        Object items = model.get("items");

        if (quotationItems != null && items == null) {
            model.put("items", quotationItems);
            items = quotationItems;
        } else if (items != null && quotationItems == null) {
            model.put("quotationItems", items);
        }
        if (items == null) {
            Object materials = model.get("materials");
            if (materials != null) {
                model.put("items", materials);
            }
        }
    }

    private List<Object> normalizeList(List<?> list) {
        List<Object> normalizedList = new ArrayList<>(list.size());
        for (Object item : list) {
            if (item instanceof Map<?, ?> rawMap) {
                Map<String, Object> itemMap = new LinkedHashMap<>();
                for (Map.Entry<?, ?> e : rawMap.entrySet()) {
                    if (e.getKey() != null) {
                        itemMap.put(e.getKey().toString(), e.getValue());
                    }
                }
                enrichCommonAliases(itemMap);
                normalizedList.add(itemMap);
            } else {
                normalizedList.add(item);
            }
        }
        return normalizedList;
    }

    private void enrichCommonAliases(Map<String, Object> map) {
        TemplateModelCustomizerHelper.aliasIfMissing(map, "materialCode", "itemCode", "childMaterialCode", "code");
        TemplateModelCustomizerHelper.aliasIfMissing(map, "itemCode", "materialCode", "childMaterialCode", "code");
        TemplateModelCustomizerHelper.aliasIfMissing(map, "materialName", "itemName", "childMaterialName", "name");
        TemplateModelCustomizerHelper.aliasIfMissing(map, "itemName", "materialName", "childMaterialName", "name");
        TemplateModelCustomizerHelper.aliasIfMissing(map, "specification", "spec");
        TemplateModelCustomizerHelper.aliasIfMissing(map, "spec", "specification");
        TemplateModelCustomizerHelper.aliasIfMissing(map, "requiredQuantity", "quantity", "baseQuantity", "qty");
        TemplateModelCustomizerHelper.aliasIfMissing(map, "quantity", "requiredQuantity", "baseQuantity", "qty");
        TemplateModelCustomizerHelper.aliasIfMissing(map, "quoteUnitPrice", "lowestPrice", "targetPrice", "unitPrice", "price", "standardCost");
        TemplateModelCustomizerHelper.aliasIfMissing(map, "unitPrice", "quoteUnitPrice", "lowestPrice", "targetPrice", "standardCost");
        TemplateModelCustomizerHelper.aliasIfMissing(map, "standardCost", "quoteUnitPrice", "unitPrice", "lowestPrice", "targetPrice");
        TemplateModelCustomizerHelper.aliasIfMissing(map, "vendorName", "vendor", "supplier");
    }
}
