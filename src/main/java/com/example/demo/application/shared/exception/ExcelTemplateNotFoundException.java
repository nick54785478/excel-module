package com.example.demo.application.shared.exception;

/**
 * 找不到指定 Excel 範本異常 (Excel Template Not Found Exception)。
 * <p>
 * 當調用範本匯出業務但指定的 {@code templateKey} 不存在於註冊表或實體檔案遺失時拋出。
 * </p>
 */
public class ExcelTemplateNotFoundException extends RuntimeException {

    /**
     * 建構包含錯誤訊息之例外實例。
     *
     * @param message 錯誤詳細訊息
     */
    public ExcelTemplateNotFoundException(String message) {
        super(message);
    }

    /**
     * 建構包含錯誤訊息與根本原因之例外實例。
     *
     * @param message 錯誤詳細訊息
     * @param cause   根本原因例外
     */
    public ExcelTemplateNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

