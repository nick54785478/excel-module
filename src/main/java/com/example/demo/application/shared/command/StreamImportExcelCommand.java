package com.example.demo.application.shared.command;

import java.io.InputStream;
import java.util.Objects;

/**
 * Excel 檔案串流匯入解析命令 (Stream Import Excel Command)
 * <p>
 * 遵循命名規範：{@code V + N + Command} (3-4 規範)。
 * 封裝串流輸入源、工作表名稱與分塊批次大小。
 */
public record StreamImportExcelCommand(
		InputStream inputStream,
		String sheetName,
		int chunkSize
) {

	public StreamImportExcelCommand {
		Objects.requireNonNull(inputStream, "inputStream must not be null");
		if (chunkSize <= 0) {
			chunkSize = 1000;
		}
	}
}

