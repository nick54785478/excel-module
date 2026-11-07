package com.example.demo.application.port.in;

import com.example.demo.application.shared.command.ExportExcelCommand;
import com.example.demo.application.shared.dto.ExcelTaskGottenResult;

/**
 * 提交非同步 Excel 匯出長任務使用案例輸入埠 (Submit Async Excel Export Use Case / Inbound Port)
 * <p>
 * 遵循 Clean Architecture 六角形架構規範，定義通用非同步長任務匯出合約。
 * 呼叫此合約後將立即建立排隊狀態之任務並指派背景工作緒執行串流匯出，支援大數據海量輸出防禦連線逾時。
 * <p>
 * 命名規範：以 {@code UseCase} 結尾 (符合 AGENTS.md 3-8 規範)。
 */
public interface SubmitAsyncExcelExportUseCase {

	/**
	 * 提交標準通用 Excel 結構之非同步匯出長任務
	 *
	 * @param command 標準匯出命令資料結構體
	 * @return 初始長任務結果 DTO (包含指派之 taskId 與 PENDING 狀態)
	 */
	ExcelTaskGottenResult submitAsyncExport(ExportExcelCommand command);
}


