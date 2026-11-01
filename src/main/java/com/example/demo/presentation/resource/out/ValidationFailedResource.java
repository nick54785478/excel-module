package com.example.demo.presentation.resource.out;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 請求參數校驗失敗回應資源物件 (Validation Failed Response Resource)
 * <p>
 * 遵循命名規範：{@code N + Ved + Resource} (3-2 規範)。
 * 作為純資料載體 (Pure Data Carrier)，負責封裝 Spring Validation 檢核失敗時的詳細欄位錯誤資訊。
 * 內部嚴禁撰寫任何業務或防腐轉換邏輯。
 *
 * @param timestamp   錯誤發生的時間戳記
 * @param status      HTTP 狀態碼 (通常為 400 Bad Request)
 * @param error       HTTP 錯誤名稱 (例如 "Bad Request")
 * @param message     綜合錯誤提示訊息
 * @param fieldErrors 各欄位的具體校驗失敗明細
 */
@Schema(description = "請求參數校驗失敗結構化錯誤回應載體")
public record ValidationFailedResource(
		@Schema(description = "錯誤發生時間戳記", example = "2026-10-31T16:50:00")
		LocalDateTime timestamp,

		@Schema(description = "HTTP 狀態碼", example = "400")
		int status,

		@Schema(description = "HTTP 錯誤名稱", example = "Bad Request")
		String error,

		@Schema(description = "錯誤摘要訊息", example = "請求主體資料校驗失敗，請檢查輸入參數")
		String message,

		@Schema(description = "各欄位詳細違規明細清單")
		List<FieldErrorResource> fieldErrors
) {

	/**
	 * 單一欄位校驗失敗明細載體
	 *
	 * @param field         發生校驗錯誤的欄位名稱 (例如 "fileName", "sheets[0].sheetName")
	 * @param rejectedValue 被拒絕的無效輸入值
	 * @param message       具體校驗失敗提示說明 (例如 "匯出檔案名稱不能為空")
	 */
	@Schema(description = "單一欄位違規明細")
	public record FieldErrorResource(
			@Schema(description = "發生錯誤之欄位路徑", example = "fileName")
			String field,

			@Schema(description = "被拒絕的非法值", example = "")
			Object rejectedValue,

			@Schema(description = "校驗失敗提示說明", example = "匯出檔案名稱不能為空")
			String message
	) {
	}
}
