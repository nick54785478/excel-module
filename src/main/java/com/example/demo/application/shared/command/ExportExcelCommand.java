package com.example.demo.application.shared.command;

import java.util.List;
import java.util.Objects;

/**
 * Excel 檔案匯出命令 (Export Excel Command)
 * <p>
 * 遵循命名規範：{@code V + N + Command} (3-4 規範)。
 * 作為應用層 Inbound Port ({@code ExportExcelUseCase}) 的輸入載體，封裝檔案名稱與資料工作表命令清單。
 */
public record ExportExcelCommand(
		String fileName,
		List<WriteSheetCommand> sheets
) {

	public ExportExcelCommand {
		Objects.requireNonNull(fileName, "fileName must not be null");
		Objects.requireNonNull(sheets, "sheets must not be null");
		if (sheets.isEmpty()) {
			throw new IllegalArgumentException("At least one sheet must be provided");
		}
	}
}

