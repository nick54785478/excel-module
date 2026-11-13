package com.example.demo.application.shared.context;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 試算表檢核執行上下文 (Excel Validation Context)
 * <p>
 * <b>【架構定位與職責】</b><br>
 * 位於應用層共享模型，封裝工作表名稱、全量資料列快照與可擴充共享屬性字典，
 * 提供給行級驗證器（Row Validator）或整表驗證器（Sheet Validator）在推論時查詢跨列與全局資訊。
 * </p>
 *
 * @param <T>              資料模型型別
 * @param sheetName        工作表名稱
 * @param allRows          整張工作表全量資料列唯讀清單
 * @param sharedAttributes 共享屬性字典 (例如：代碼清單、外部參數)
 */
public record ExcelValidationContext<T>(
		String sheetName,
		List<T> allRows,
		Map<String, Object> sharedAttributes
) implements Serializable {

	/**
	 * 便捷建構子：僅傳入工作表名稱與全量資料
	 *
	 * @param sheetName 工作表名稱
	 * @param allRows   全量資料列清單
	 */
	public ExcelValidationContext(String sheetName, List<T> allRows) {
		this(sheetName, allRows != null ? List.copyOf(allRows) : Collections.emptyList(), Collections.emptyMap());
	}

	/**
	 * 取得共享屬性值
	 *
	 * @param key 屬性鍵值
	 * @param <V> 期望回傳型別
	 * @return 屬性值
	 */
	@SuppressWarnings("unchecked")
	public <V> V getAttribute(String key) {
		return sharedAttributes != null ? (V) sharedAttributes.get(key) : null;
	}
}

