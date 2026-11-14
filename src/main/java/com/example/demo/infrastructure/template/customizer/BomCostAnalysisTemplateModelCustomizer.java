package com.example.demo.infrastructure.template.customizer;

import com.example.demo.application.shared.template.ExcelTemplateModelCustomizer;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * BOM 成本分析單專屬範本資料模型自定義策略 (Bom Cost Analysis Template Model Customizer)。
 * <p>
 * 座落於 {@code infrastructure.template.customizer} 套件，
 * 實作應用層 {@link ExcelTemplateModelCustomizer} 策略契約，
 * 綁定 {@code BOM_COST_ANALYSIS} 範本識別鍵：
 * <ol>
 *   <li>集合鍵雙向對齊：自動對齊 {@code materials} 與 {@code items}。</li>
 *   <li>子件欄位別名擴充：自動相容 {@code childMaterialCode}、{@code specification}、{@code scrapRate} 與 {@code standardCost}。</li>
 * </ol>
 * </p>
 */
@Component
public class BomCostAnalysisTemplateModelCustomizer implements ExcelTemplateModelCustomizer {

    public static final String TEMPLATE_KEY = "BOM_COST_ANALYSIS";

    @Override
    public String getTemplateKey() {
        return TEMPLATE_KEY;
    }

    @Override
    public Map<String, Object> customize(Map<String, Object> dataModel) {
        if (dataModel == null || dataModel.isEmpty()) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> result = new LinkedHashMap<>(dataModel);

        TemplateModelCustomizerHelper.alignCollectionPair(result, "materials", "items");

        if (result.containsKey("items")) {
            TemplateModelCustomizerHelper.transformItemList(result, "items", this::enrichBomItem);
        }
        if (result.containsKey("materials")) {
            TemplateModelCustomizerHelper.transformItemList(result, "materials", this::enrichBomItem);
        }

        return result;
    }

    private void enrichBomItem(Map<String, Object> item) {
        TemplateModelCustomizerHelper.aliasIfMissing(item, "childMaterialCode", "materialCode", "itemCode", "code");
        TemplateModelCustomizerHelper.aliasIfMissing(item, "materialCode", "childMaterialCode", "itemCode", "code");
        TemplateModelCustomizerHelper.aliasIfMissing(item, "itemCode", "childMaterialCode", "materialCode", "code");
        TemplateModelCustomizerHelper.aliasIfMissing(item, "childMaterialName", "materialName", "itemName", "name");
        TemplateModelCustomizerHelper.aliasIfMissing(item, "materialName", "childMaterialName", "itemName", "name");
        TemplateModelCustomizerHelper.aliasIfMissing(item, "itemName", "childMaterialName", "materialName", "name");
        TemplateModelCustomizerHelper.aliasIfMissing(item, "specification", "spec");
        TemplateModelCustomizerHelper.aliasIfMissing(item, "spec", "specification");
        TemplateModelCustomizerHelper.aliasIfMissing(item, "baseQuantity", "requiredQuantity", "quantity", "qty");
        TemplateModelCustomizerHelper.aliasIfMissing(item, "requiredQuantity", "baseQuantity", "quantity", "qty");
        TemplateModelCustomizerHelper.aliasIfMissing(item, "quantity", "baseQuantity", "requiredQuantity", "qty");
        TemplateModelCustomizerHelper.aliasIfMissing(item, "standardCost", "unitPrice", "quoteUnitPrice", "lowestPrice", "targetPrice");
        TemplateModelCustomizerHelper.aliasIfMissing(item, "unitPrice", "standardCost", "quoteUnitPrice", "lowestPrice", "targetPrice");
    }
}
