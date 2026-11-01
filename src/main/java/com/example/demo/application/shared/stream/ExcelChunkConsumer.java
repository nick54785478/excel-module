package com.example.demo.application.shared.stream;

import java.util.List;
import java.util.Map;

/**
 * Excel 串流分塊批次消費者函式介面 (Functional Chunked Batch Consumer)
 * <p>
 * 專門針對資料庫批次操作 (Batch Insert) 設計，當緩衝區累積達指定 {@code chunkSize} 筆數時立即回呼。
 */
@FunctionalInterface
public interface ExcelChunkConsumer {

	/**
	 * 接收累積達標的分塊資料集合
	 *
	 * @param chunkRows 分塊資料清單，每筆資料為已依標題名稱映射之鍵值字典 (Key: 標題, Value: 儲存格值)
	 * @param context   當前分塊完成時的上下文資訊
	 */
	void accept(List<Map<String, Object>> chunkRows, ExcelRowContext context);
}
