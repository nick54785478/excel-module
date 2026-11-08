package com.example.demo.application.shared.exception;

import java.util.Objects;

import com.example.demo.application.shared.dto.ExcelValidationGottenResult;

/**
 * Excel 宣告式資料檢核失敗例外 (Excel Validation Failed Exception)
 * <p>
 * <b>【架構定位與職責說明】</b><br>
 * 位於 Clean Architecture 應用層共用例外套件 ({@code application/shared/exception/})。<br>
 * 當方法標註
 * {@link com.example.demo.application.annotation.validation.ValidateExcel}
 * 且處理策略設定為
 * {@link com.example.demo.application.annotation.validation.ValidateExcel.OnErrorAction#THROW_EXCEPTION}
 * 時，
 * 若 AOP 切面在多層次檢核管線中偵測到任何欄位或集合違規事實，將拋出此例外。<br>
 * 攜帶完整之全量錯誤清單 {@link ExcelValidationGottenResult}，
 * 供表現層全域例外處理器 (GlobalExceptionHandler) 捕捉並轉譯為標準 HTTP 422 Unprocessable Entity
 * 回應。
 * </p>
 *
 */
public class ExcelValidationFailedException extends RuntimeException {

	private final ExcelValidationGottenResult<?> validationResult;

	/**
	 * 建構子
	 *
	 * @param message          例外訊息說明
	 * @param validationResult 包含全量錯誤清單之檢核結果資料載體
	 */
	public ExcelValidationFailedException(String message, ExcelValidationGottenResult<?> validationResult) {
		super(message);
		this.validationResult = Objects.requireNonNull(validationResult, "validationResult must not be null");
	}

	/**
	 * 取得全量檢核結果
	 *
	 * @return 檢核結果 DTO
	 */
	public ExcelValidationGottenResult<?> getValidationResult() {
		return validationResult;
	}
}
