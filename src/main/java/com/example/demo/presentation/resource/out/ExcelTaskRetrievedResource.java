package com.example.demo.presentation.resource.out;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.demo.application.shared.vo.ExcelTaskStatus;
import com.example.demo.application.shared.vo.ExcelTaskType;

/**
 * Excel 長任務進度查詢回應載體 (Excel Task Retrieved Response Resource)
 * <p>
 * 遵循命名規範：{@code N + Ved + Resource} (AGENTS.md 3-2 規範)。
 * 用於 HTTP 200 OK 輪詢進度回應，展示當前總列數、已處理列數、完成百分比與下載連結。
 * 作為純資料載體 (Pure Data Carrier)，內部嚴禁撰寫任何防腐或轉換邏輯。
 *
 * @param taskId             任務唯一識別碼 (UUID)
 * @param taskType           任務類型 (IMPORT / EXPORT)
 * @param taskStatus         任務當前執行狀態
 * @param totalRows          總預計處理列數 (若未知則為 0)
 * @param processedRows      已完成處理列數
 * @param progressPercentage 進度百分比 (0.00 ~ 100.00)
 * @param fileName           目標檔案名稱
 * @param fileUrl            產出檔案下載連結或識別位址 (完成時可用)
 * @param errorMessage       任務失敗時之異常摘要原因 (若無則為 null)
 * @param createdAt          任務建立時間戳記
 * @param updatedAt          最後狀態更新時間戳記
 */
@Schema(description = "Excel 長任務進度查詢回應載體")
public record ExcelTaskRetrievedResource(
		@Schema(description = "任務唯一識別碼 (UUID)", example = "e0b9685a-0638-4e89-be21-d0554b50d998")
		String taskId,

		@Schema(description = "任務類型", example = "EXPORT")
		ExcelTaskType taskType,

		@Schema(description = "任務當前執行狀態", example = "PROCESSING")
		ExcelTaskStatus taskStatus,

		@Schema(description = "總預計處理列數", example = "10000")
		long totalRows,

		@Schema(description = "已完成處理列數", example = "4500")
		long processedRows,

		@Schema(description = "進度百分比 (0.00 ~ 100.00)", example = "45.0")
		double progressPercentage,

		@Schema(description = "目標檔案名稱", example = "使用者名冊.xlsx")
		String fileName,

		@Schema(description = "產出檔案下載連結或識別位址 (完成時可用)", example = "/api/v1/excel/tasks/e0b9685a-0638-4e89-be21-d0554b50d998/download")
		String fileUrl,

		@Schema(description = "任務失敗時之異常摘要原因 (若無則為 null)")
		String errorMessage,

		@Schema(description = "任務建立時間戳記")
		LocalDateTime createdAt,

		@Schema(description = "最後狀態更新時間戳記")
		LocalDateTime updatedAt
) {
}

