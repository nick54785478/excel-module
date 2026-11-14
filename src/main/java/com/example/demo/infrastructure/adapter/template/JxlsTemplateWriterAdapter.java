package com.example.demo.infrastructure.adapter.template;

import com.example.demo.application.port.out.template.ExcelTemplateWriterPort;
import com.example.demo.application.shared.exception.ExcelWriteException;
import lombok.extern.slf4j.Slf4j;
import org.jxls.builder.JxlsOutput;
import org.jxls.expression.ExpressionEvaluatorFactory;
import org.jxls.expression.ExpressionEvaluatorFactoryJexlImpl;
import org.jxls.expression.JxlsJexlPermissions;
import org.jxls.transform.poi.JxlsPoiTemplateFillerBuilder;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * JXLS 3.x 宣告式範本寫入適配器 (Outbound Adapter)。
 * <p>
 * 遵循 AGENTS.md 規則 3-9，適配器一律以 {@code Adapter} 結尾。
 * 實作應用層 {@link ExcelTemplateWriterPort} 技術合約，
 * 專注於 JXLS 3.x 範本填充核心 ({@link JxlsPoiTemplateFillerBuilder})、
 * JEXL 安全沙箱權限 ({@link JxlsJexlPermissions#RESTRICTED}) 與 POI 渲染轉換。
 * <p>
 * <b>【單一職責與架構解耦】</b><br>
 * 本適配器維持純粹的基礎設施技術生成職責，不含任何個別業務範本之資料預處理或欄位別名邏輯。
 * 個別範本之自定義與資料模型擴充已下推至各專屬之自定義適配器 (如 {@code ProcurementComparisonTemplateCustomizerAdapter})，
 * 並由應用層服務或自定義輸出埠合約統一調度。
 * </p>
 */
@Slf4j
@Component
public class JxlsTemplateWriterAdapter implements ExcelTemplateWriterPort {

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
            Map<String, Object> model = (dataModel != null) ? new LinkedHashMap<>(dataModel) : new LinkedHashMap<>();
            return JxlsPoiTemplateFillerBuilder.newInstance()
                .withTemplate(templateStream)
                .withExpressionEvaluatorFactory(expressionEvaluatorFactory)
                .withExceptionThrower()
                .withRecalculateFormulasOnOpening(true)
                .buildAndFill(model);
        } catch (Exception e) {
            throw new ExcelWriteException("JXLS 範本寫入失敗: " + e.getMessage(), e);
        }
    }

    @Override
    public void streamWriteFromTemplate(InputStream templateStream, OutputStream targetStream, Map<String, Object> dataModel) {
        Objects.requireNonNull(templateStream, "templateStream 不得為空");
        Objects.requireNonNull(targetStream, "targetStream 不得為空");
        try {
            log.debug("開始以 JXLS 3.x 執行範本填充並直通輸出串流 (Streaming 模式)");
            Map<String, Object> model = (dataModel != null) ? new LinkedHashMap<>(dataModel) : new LinkedHashMap<>();
            JxlsOutput output = () -> targetStream;
            JxlsPoiTemplateFillerBuilder.newInstance()
                .withTemplate(templateStream)
                .withExpressionEvaluatorFactory(expressionEvaluatorFactory)
                .withExceptionThrower()
                .withRecalculateFormulasOnOpening(true)
                .buildAndFill(model, output);
            targetStream.flush();
        } catch (Exception e) {
            throw new ExcelWriteException("JXLS 範本串流寫入失敗: " + e.getMessage(), e);
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
