package com.example.demo.infrastructure.adapter.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.demo.application.annotation.mapping.ExcelSheet;
import com.example.demo.application.port.out.core.ExcelSheetModelRegistryPort;

/**
 * 宣告式 Excel 模型註冊表適配器 (Outbound Adapter)
 * <p>
 * <b>【架構定位與職責規範】</b><br>
 * 位於 Clean Architecture 基礎設施層 ({@code infrastructure/adapter/core/})。<br>
 * 遵循 {@code AGENTS.md} 命名規範：以 {@code Adapter} 結尾 (3-9 規範)。<br>
 * 實作 {@link ExcelSheetModelRegistryPort} 輸出埠，作為執行緒安全的純 Java 容器，
 * 維護工作表名稱 (SheetName)、別名 (Aliases) 與 Java 模型類別 ({@code Class<?>}) 的高效映射索引。
 * </p>
 */
public class ExcelSheetModelRegistryAdapter implements ExcelSheetModelRegistryPort {

	private static final Logger log = LoggerFactory.getLogger(ExcelSheetModelRegistryAdapter.class);

	/**
	 * 工作表名稱與別名至模型類別之索引查表 (Normalized Sheet Name / Alias -> Model Class)
	 */
	private final Map<String, Class<?>> sheetToModelMap = new ConcurrentHashMap<>();

	/**
	 * 所有已註冊之資料模型類別集合 (保持唯一性)
	 */
	private final Set<Class<?>> registeredModels = ConcurrentHashMap.newKeySet();

	/**
	 * 註冊指定的 Excel 資料模型類別至註冊表中
	 * <p>
	 * 自動讀取類別上的 {@link ExcelSheet} 註解，提取 {@link ExcelSheet#name()}、
	 * {@link ExcelSheet#aliases()} 與類別簡單名稱 (Simple Name)，建立不區分大小寫的查詢索引。
	 * </p>
	 *
	 * @param modelClass 欲註冊之資料模型類別 Class (不得為 null)
	 */
	public void register(Class<?> modelClass) {
		Objects.requireNonNull(modelClass, "modelClass 不得為空");

		ExcelSheet sheetAnno = modelClass.getAnnotation(ExcelSheet.class);
		if (sheetAnno == null) {
			log.debug("[ExcelSheetModelRegistryAdapter] 類別 {} 未標註 @ExcelSheet，略過自動索引註冊", modelClass.getName());
			return;
		}

		registeredModels.add(modelClass);

		// 1. 註冊工作表主名稱 (若有定義)
		if (!sheetAnno.name().isBlank()) {
			addMapping(sheetAnno.name(), modelClass);
		}

		// 2. 註冊所有宣告式別名
		for (String alias : sheetAnno.aliases()) {
			if (alias != null && !alias.isBlank()) {
				addMapping(alias, modelClass);
			}
		}

		// 3. 註冊類別簡單名稱 (如 "MaterialExcelModel")
		addMapping(modelClass.getSimpleName(), modelClass);
	}

	/**
	 * 註冊一組資料模型類別清單
	 *
	 * @param modelClasses 模型類別清單
	 */
	public void registerAll(Iterable<Class<?>> modelClasses) {
		if (modelClasses != null) {
			for (Class<?> clazz : modelClasses) {
				if (clazz != null) {
					register(clazz);
				}
			}
		}
	}

	/**
	 * 新增內部查詢別名映射
	 *
	 * @param rawKey     原始名稱或別名字串
	 * @param modelClass 目標模型類別
	 */
	private void addMapping(String rawKey, Class<?> modelClass) {
		String normalized = rawKey.trim().toLowerCase();
		sheetToModelMap.put(normalized, modelClass);
		log.debug("[ExcelSheetModelRegistryAdapter] 建立工作表別名索引: '{}' -> {}", normalized, modelClass.getSimpleName());
	}

	@Override
	public Optional<Class<?>> findModelClass(String sheetName) {
		if (sheetName == null || sheetName.isBlank()) {
			return Optional.empty();
		}

		String normalized = sheetName.trim().toLowerCase();

		// 1. 精確別名查表 (O(1))
		Class<?> directMatch = sheetToModelMap.get(normalized);
		if (directMatch != null) {
			return Optional.of(directMatch);
		}

		// 2. 寬鬆模糊比對 (去除空白、底線、減號與括號)
		String simplifiedQuery = simplify(sheetName);
		if (!simplifiedQuery.isEmpty()) {
			for (Map.Entry<String, Class<?>> entry : sheetToModelMap.entrySet()) {
				if (simplify(entry.getKey()).equals(simplifiedQuery)) {
					return Optional.of(entry.getValue());
				}
			}
		}

		// 3. 智慧子字串包含比對 (依別名長度降冪優先匹配最長最精確之別名)
		List<Map.Entry<String, Class<?>>> sortedEntries = new ArrayList<>(sheetToModelMap.entrySet());
		sortedEntries.sort((e1, e2) -> Integer.compare(e2.getKey().length(), e1.getKey().length()));
		for (Map.Entry<String, Class<?>> entry : sortedEntries) {
			String key = entry.getKey();
			if (key.length() >= 2 && (normalized.contains(key) || (normalized.length() >= 3 && key.contains(normalized)))) {
				return Optional.of(entry.getValue());
			}
		}

		return Optional.empty();
	}

	@Override
	public List<Class<?>> getAllModelClasses() {
		return Collections.unmodifiableList(new ArrayList<>(registeredModels));
	}

	private String simplify(String name) {
		if (name == null) {
			return "";
		}
		return name.replaceAll("[\\s_\\-()]", "").toLowerCase();
	}
}
