package com.example.demo.application.port.out.template;

import com.example.demo.application.shared.template.ExcelTemplateModelCustomizer;

/**
 * Excel 範本資料模型自定義器工廠輸出埠 (Excel Template Model Customizer Factory Outbound Port)。
 * <p>
 * <b>【六角形架構職責】</b><br>
 * 位於應用層輸出埠 ({@code application/port/out/template/})。<br>
 * 負責依據範本識別鍵 (templateKey) 動態獲取對應的 {@link ExcelTemplateModelCustomizer} 自定義策略實例。<br>
 * 若未找到特定範本之自定義器，工廠應保底回傳通用或無操作 (NO_OP) 之自定義器，保證回傳值永不為 null。
 * </p>
 */
public interface ExcelTemplateModelCustomizerFactoryPort {

    /**
     * 依據範本識別鍵獲取對應之資料模型自定義策略實例。
     *
     * @param templateKey 範本唯一識別鍵 (例如 "PROCUREMENT_COMPARISON")，大小寫不敏感
     * @return 符合該範本之 {@link ExcelTemplateModelCustomizer} 實例 (保證不為 null)
     */
    ExcelTemplateModelCustomizer getCustomizer(String templateKey);

    /**
     * 檢查容器中是否存在專屬支援該範本識別鍵之自定義器 (不計通用保底策略)。
     *
     * @param templateKey 範本唯一識別鍵
     * @return 若存在專屬自定義器則回傳 true，否則回傳 false
     */
    boolean hasCustomizer(String templateKey);
}
