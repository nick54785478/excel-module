package com.example.demo.application.shared.stream;

import java.util.List;

/**
 * Excel 串流逐列消費者函式介面 (Functional Streaming Row Consumer)
 * <p>
 * 當 FastExcel 解析出一筆資料列時立即回呼，記憶體空間複雜度為 O(1)。
 */
@FunctionalInterface
public interface ExcelRowConsumer {

	/**
	 * 接收解析完成的單一資料列
	 *
	 * @param rowValues 儲存格原始型別數值清單 (Number, String, Boolean, LocalDateTime 等)
	 * @param rowTexts  儲存格格式化後的純文字字串清單
	 * @param context   當前資料列上下文環境 (包含工作表名稱、行號、累計筆數等)
	 */
	void accept(List<Object> rowValues, List<String> rowTexts, ExcelRowContext context);
}
