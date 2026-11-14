package com.example.demo.infrastructure.template.customizer;

import com.example.demo.application.shared.template.ExcelTemplateModelCustomizer;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 採購詢價與比價明細單專屬範本資料模型自定義策略 (Procurement Comparison Template Model Customizer)。
 * <p>
 * 座落於 {@code infrastructure.template.customizer} 套件，
 * 實作應用層 {@link ExcelTemplateModelCustomizer} 策略契約，
 * 綁定 {@code PROCUREMENT_COMPARISON} 範本識別鍵：
 * <ol>
 *   <li>集合鍵雙向對齊：自動對齊 {@code quotationItems} 與 {@code items}。</li>
 *   <li>供應商字串拆分：將逗號分隔之 {@code vendorNames} 解析取得首家廠商作為預設供應商。</li>
 *   <li>比價明細欄位補強：自動映射 {@code itemCode}、{@code itemName}、{@code quantity}、{@code lowestPrice} 與 {@code vendorName}。</li>
 * </ol>
 * </p>
 */
@Component
public class ProcurementComparisonTemplateModelCustomizer implements ExcelTemplateModelCustomizer {

    public static final String TEMPLATE_KEY = "PROCUREMENT_COMPARISON";
    private static final Pattern VENDOR_DELIMITER_PATTERN = Pattern.compile("[,，、]");

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

        // 1. 雙向對齊 quotationItems 與 items 鍵
        TemplateModelCustomizerHelper.alignCollectionPair(result, "quotationItems", "items");

        // 2. 解析首選供應商
        String defaultVendor = null;
        if (result.get("vendorNames") instanceof String vn && !vn.isBlank()) {
            defaultVendor = VENDOR_DELIMITER_PATTERN.split(vn)[0].trim();
        }

        final String vendorToInject = defaultVendor;

        // 3. 補強 quotationItems 與 items 內之各別明細項目
        if (result.containsKey("quotationItems")) {
            TemplateModelCustomizerHelper.transformItemList(result, "quotationItems", item -> enrichQuotationItem(item, vendorToInject));
        }
        if (result.containsKey("items")) {
            TemplateModelCustomizerHelper.transformItemList(result, "items", item -> enrichQuotationItem(item, vendorToInject));
        }

        return result;
    }

    private void enrichQuotationItem(Map<String, Object> item, String defaultVendor) {
        TemplateModelCustomizerHelper.aliasIfMissing(item, "materialCode", "itemCode", "childMaterialCode", "code");
        TemplateModelCustomizerHelper.aliasIfMissing(item, "itemCode", "materialCode", "childMaterialCode", "code");
        TemplateModelCustomizerHelper.aliasIfMissing(item, "materialName", "itemName", "childMaterialName", "name");
        TemplateModelCustomizerHelper.aliasIfMissing(item, "itemName", "materialName", "childMaterialName", "name");
        TemplateModelCustomizerHelper.aliasIfMissing(item, "requiredQuantity", "quantity", "baseQuantity", "qty");
        TemplateModelCustomizerHelper.aliasIfMissing(item, "quantity", "requiredQuantity", "baseQuantity", "qty");
        TemplateModelCustomizerHelper.aliasIfMissing(item, "quoteUnitPrice", "lowestPrice", "targetPrice", "unitPrice", "price", "standardCost");
        TemplateModelCustomizerHelper.aliasIfMissing(item, "lowestPrice", "quoteUnitPrice", "targetPrice", "unitPrice", "price", "standardCost");
        TemplateModelCustomizerHelper.aliasIfMissing(item, "vendorName", "vendor", "supplier");
        if (item.get("vendorName") == null && defaultVendor != null && !defaultVendor.isBlank()) {
            item.put("vendorName", defaultVendor);
        }
    }
}
