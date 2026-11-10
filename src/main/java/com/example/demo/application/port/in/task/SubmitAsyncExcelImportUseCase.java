package com.example.demo.application.port.in.task;

import java.io.InputStream;

import com.example.demo.application.shared.dto.ExcelTaskGottenResult;
import com.example.demo.application.shared.query.ReadExcelQuery;

/**
 * 提交非同步 Excel 匯入長任務使用案例輸入埠 (Submit Async Excel Import Use Case / Inbound Port)
 * <p>
 * 遵循 Clean Architecture 六角形架構規範，定義非同步長任務匯入解析合約。
 * 呼叫此合約後將檔案暫存並立即建立排隊狀態任務，由背景工作緒執行分塊串流解析與進度更新。
 * <p>
 * 命名規範：以 {@code UseCase} 結尾 (符合 AGENTS.md 3-8 規範)。
 */
public interface SubmitAsyncExcelImportUseCase {

	/**
	 * 提交 Excel 檔案二進位串流之非同步匯入解析長任務
	 *
	 * @param inputStream 欲上傳解析之 Excel 檔案輸入串流
	 * @param fileName    原始檔案名稱
	 * @param query       讀取查詢條件
	 * @return 初始長任務結果 DTO (包含指派之 taskId 與 PENDING 狀態)
	 */
	ExcelTaskGottenResult submitAsyncImport(InputStream inputStream, String fileName, ReadExcelQuery query);
}

