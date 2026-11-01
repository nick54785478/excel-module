package com.example.demo.application.shared.command;

import java.util.List;
import java.util.Objects;

/**
 * Excel 大數據串流匯出命令 (Stream Export Excel Command)
 * <p>
 * 遵循命名規範：{@code V + N + Command} (3-4 規範)。
 * 封裝欲串流匯出之檔名、工作表串流命令以及定期沖刷 (flush) 筆數大小。
 */
public record StreamExportExcelCommand(
		String fileName,
		List<WriteStreamSheetCommand> sheets,
		Integer batchFlushSize
) {

	public StreamExportExcelCommand {
		Objects.requireNonNull(fileName, "fileName must not be null");
		Objects.requireNonNull(sheets, "sheets must not be null");
		if (sheets.isEmpty()) {
			throw new IllegalArgumentException("At least one stream sheet must be provided");
		}
		if (batchFlushSize == null || batchFlushSize <= 0) {
			batchFlushSize = 1000;
		}
	}
}

