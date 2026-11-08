package com.example.demo.application.shared.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 通用報表裝飾上下文純資料載體 (Report Decoration Context Value Object)
 * <p>
 * 遵循 AGENTS.md 規範，採用 Java 21 Record 作為應用層不可變純資料載體。
 * 統一封裝報表識別鍵、標題、密碼、機密等級、原始資料清單與自訂擴充參數。
 *
 * @param reportKey       報表識別鍵值 (如 "PROCUREMENT_ANALYSIS")
 * @param reportTitle     自訂列印頁首報表標題
 * @param password        工作表防護解鎖密碼
 * @param classification  列印頁尾機密等級標記
 * @param rawDataList     原始業務資料物件清單 (不可變唯讀清單)
 * @param extraParameters 額外自訂擴充參數對照表 (不可變唯讀對照表)
 */
public record ReportDecorationContext(
		String reportKey,
		String reportTitle,
		String password,
		String classification,
		List<?> rawDataList,
		Map<String, Object> extraParameters
) {

	public ReportDecorationContext {
		Objects.requireNonNull(reportKey, "reportKey must not be null");
		reportTitle = (reportTitle != null && !reportTitle.isBlank()) ? reportTitle.trim() : "企業報表";
		password = (password != null && !password.isBlank()) ? password.trim() : "AuditSecure2026";
		classification = (classification != null && !classification.isBlank()) ? classification.trim() : "內部機密";
		rawDataList = (rawDataList != null) ? List.copyOf(rawDataList) : Collections.emptyList();
		extraParameters = (extraParameters != null) ? Map.copyOf(extraParameters) : Collections.emptyMap();
	}

	/**
	 * 便捷靜態工廠方法
	 *
	 * @param reportKey      報表標識鍵
	 * @param reportTitle    報表標題
	 * @param password       解鎖密碼
	 * @param classification 機密等級
	 * @param dataList       原始資料清單
	 * @return 規格化之上下文實例
	 */
	public static ReportDecorationContext of(
			String reportKey,
			String reportTitle,
			String password,
			String classification,
			List<?> dataList
	) {
		return new ReportDecorationContext(reportKey, reportTitle, password, classification, dataList, Map.of());
	}
}

