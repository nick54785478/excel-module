package com.example.demo.application.annotation.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Excel 已校驗合格資料參數注入註解 (Validated Excel Data Parameter Annotation)
 * <p>
 * <b>【架構定位與職責說明】</b><br>
 * 位於 Clean Architecture 應用層資料校驗註解套件 ({@code application/annotation/validation/})。<br>
 * 標註於 Controller 或 Service 方法之入參上（如 {@code List<OrderModel> validOrders}）。
 * 當方法上方標註 {@link ValidateExcel} 且檔案檢核 100% 通過後，
 * AOP 切面與表現層參數解析器將自動將已反序列化並通過 EasyRules 檢核之乾淨資料清單注入此參數，
 * 讓業務邏輯方法徹底免去手動呼叫反序列化與檢核的樣板程式碼。
 * </p>
 *
 * @see ValidateExcel
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface ValidatedExcelData {
}

