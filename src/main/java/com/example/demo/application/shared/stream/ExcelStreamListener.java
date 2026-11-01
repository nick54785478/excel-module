package com.example.demo.application.shared.stream;

import java.util.List;
import java.util.Map;

/**
 * Excel 串流全生命週期監聽器 (Full Lifecycle Excel Stream Listener)
 * <p>
 * 提供工作表開始、標題識別、逐列處理、分塊批次、解析完成與異常捕獲之全套事件掛載點。
 * 業務端可繼承此介面並覆寫特定事件方法以達到高度客製化。
 */
public interface ExcelStreamListener {

	/**
	 * 當解析出工作表標題列時觸發
	 *
	 * @param headers 標題文字清單
	 * @param context 當前上下文
	 */
	default void onHeader(List<String> headers, ExcelRowContext context) {
	}

	/**
	 * 當逐列解析出單筆資料時觸發 (逐列流式處理)
	 *
	 * @param rowValues 儲存格原生型別數值清單
	 * @param rowTexts  儲存格純文字清單
	 * @param context   當前資料列上下文
	 */
	default void onRow(List<Object> rowValues, List<String> rowTexts, ExcelRowContext context) {
	}

	/**
	 * 當分塊累積達標時觸發 (分批批次處理)
	 *
	 * @param chunkRows 映射為標題鍵值對之資料分塊
	 * @param context   當前上下文
	 */
	default void onChunk(List<Map<String, Object>> chunkRows, ExcelRowContext context) {
	}

	/**
	 * 當工作表或檔案串流完全解析完畢時觸發
	 *
	 * @param context 結束時的最終上下文
	 */
	default void onComplete(ExcelRowContext context) {
	}

	/**
	 * 當串流解析過程中發生異常時觸發
	 *
	 * @param throwable 異常物件
	 * @param context   出錯時的當前上下文
	 */
	default void onError(Throwable throwable, ExcelRowContext context) {
	}
}
