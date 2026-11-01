package com.example.demo.application.shared.stream;

import java.util.Collections;
import java.util.List;

/**
 * Excel 串流讀取上下文環境 (Excel Streaming Row Context)
 * <p>
 * 提供串流逐列解析過程中的即時中繼資訊，包含當前工作表索引、工作表名稱、行索引、標題清單及累計處理筆數，
 * 並支援呼叫 {@link #stop()} 主動中斷串流讀取。
 */
public class ExcelRowContext {

	private final int sheetIndex;
	private final String sheetName;
	private final int rowIndex;
	private final List<String> headers;
	private final boolean headerRow;
	private final long totalDataRowsRead;
	private boolean stopped = false;

	public ExcelRowContext(int sheetIndex, String sheetName, int rowIndex, List<String> headers,
			boolean headerRow, long totalDataRowsRead) {
		this.sheetIndex = sheetIndex;
		this.sheetName = sheetName != null ? sheetName : "Sheet" + sheetIndex;
		this.rowIndex = rowIndex;
		this.headers = headers != null ? Collections.unmodifiableList(headers) : Collections.emptyList();
		this.headerRow = headerRow;
		this.totalDataRowsRead = totalDataRowsRead;
	}

	public int getSheetIndex() {
		return sheetIndex;
	}

	public String getSheetName() {
		return sheetName;
	}

	public int getRowIndex() {
		return rowIndex;
	}

	public List<String> getHeaders() {
		return headers;
	}

	public boolean isHeaderRow() {
		return headerRow;
	}

	public long getTotalDataRowsRead() {
		return totalDataRowsRead;
	}

	public boolean isStopped() {
		return stopped;
	}

	/**
	 * 主動請求中斷串流讀取（例如：已達成特定業務終止條件，或偵測到重大異常時停止後續解析）
	 */
	public void stop() {
		this.stopped = true;
	}
}
