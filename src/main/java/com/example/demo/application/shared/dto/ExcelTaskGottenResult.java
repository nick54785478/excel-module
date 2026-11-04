package com.example.demo.application.shared.dto;

import java.time.LocalDateTime;

import com.example.demo.application.shared.vo.ExcelTaskStatus;
import com.example.demo.application.shared.vo.ExcelTaskType;

/**
 * Excel 長任務查詢結果資料傳輸物件 (Excel Task Gotten Result DTO)
 * <p>
 * 遵循 AGENTS.md 3-6 DTO 命名規範 ({@code N + Gotten + Result})。
 * 作為純資料載體，承載任務進度、狀態、檔案名稱與完成下載連結等資訊，嚴禁包含技術實體或轉換邏輯。
 *
 * @param taskId             任務唯一識別碼 (UUID)
 * @param taskType           任務類型 (匯入 / 匯出)
 * @param taskStatus         任務當前執行狀態
 * @param totalRows          總預計處理列數 (若未知則為 0)
 * @param processedRows      已完成處理列數
 * @param progressPercentage 進度百分比 (0.00 ~ 100.00)
 * @param fileName           目標檔案名稱
 * @param fileUrl            產出檔案之下載連結或儲存鍵 (可選)
 * @param errorMessage       任務失敗時之異常摘要原因 (可選)
 * @param createdAt          任務建立時間戳記
 * @param updatedAt          最後狀態更新時間戳記
 */
public record ExcelTaskGottenResult(
		String taskId,
		ExcelTaskType taskType,
		ExcelTaskStatus taskStatus,
		long totalRows,
		long processedRows,
		double progressPercentage,
		String fileName,
		String fileUrl,
		String errorMessage,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}

