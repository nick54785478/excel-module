package com.example.demo.application.shared.pipeline;

import java.util.List;

/**
 * Excel 區塊資料非同步生產者提供者 (Excel Chunk Data Provider SPI)
 * <p>
 * 遵循 Clean Architecture 規範，作為應用層與領域/資料庫查詢之純 Java 函數式合約 (Functional Interface)。
 * 用於高並發「模式 A：生產者-消費者管線」架構中，供多個工作執行緒依據 offset 與 limit 平行拉取資料。
 */
@FunctionalInterface
public interface ExcelChunkProvider {

	/**
	 * 拉取指定區間之資料列區塊 (Chunk)
	 *
	 * @param offset 資料位移起始索引 (0-based)
	 * @param limit  本次預計拉取之最大列數 (批次大小)
	 * @return 資料列集合 (每列為 Object 欄位值清單)
	 */
	List<List<Object>> fetchChunk(int offset, int limit);
}

