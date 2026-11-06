package com.example.demo.application.shared.context;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.example.demo.application.shared.dto.ExcelValidationGottenResult;

/**
 * Excel 資料檢核執行緒上下文保持器 (Excel Validation Context Holder)
 * <p>
 * <b>【架構定位與職責說明】</b><br>
 * 位於 Clean Architecture 應用層共用上下文套件 ({@code application/shared/context/})，
 * 遵循六角形架構跨層資料透明傳遞規範（與 {@link ExcelTaskContextHolder} 架構風格一致）。<br>
 * 本類別利用 {@link ThreadLocal} 在當前執行緒生命週期中存放通過 {@link com.example.demo.application.annotation.validation.ValidateExcel}
 * 切面檢核後之結果物件 {@link ExcelValidationGottenResult}。
 * 讓下層業務邏輯或方法呼叫無需顯式層層傳遞檢核結果，即可取得 100% 合格的領域模型資料。
 * </p>
 *
 */
public final class ExcelValidationContextHolder {

	private static final ThreadLocal<ExcelValidationGottenResult<?>> CONTEXT = new ThreadLocal<>();

	private ExcelValidationContextHolder() {
		// 工具類私有化建構子
	}

	/**
	 * 綁定當前執行緒之檢核結果上下文
	 *
	 * @param result 檢核結果資料載體 (不得為 null)
	 */
	public static void bind(ExcelValidationGottenResult<?> result) {
		Objects.requireNonNull(result, "ExcelValidationGottenResult 不得為 null");
		CONTEXT.set(result);
	}

	/**
	 * 獲取當前執行緒之檢核結果
	 *
	 * @return 檢核結果 Optional 封裝
	 */
	public static Optional<ExcelValidationGottenResult<?>> get() {
		return Optional.ofNullable(CONTEXT.get());
	}

	/**
	 * 獲取當前執行緒中已檢核通過之合格資料模型清單
	 *
	 * @param targetType 目標模型型別 Class
	 * @param <T>        模型型別
	 * @return 合格模型清單 (若無資料則回傳空清單)
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<T> getValidItems(Class<T> targetType) {
		Objects.requireNonNull(targetType, "targetType 不得為 null");
		return get()
				.map(res -> (List<T>) res.validItems())
				.orElse(Collections.emptyList());
	}

	/**
	 * 清除當前執行緒之檢核上下文，防止線程池資源洩漏 (ThreadLocal Memory Leak)
	 */
	public static void clear() {
		CONTEXT.remove();
	}
}
