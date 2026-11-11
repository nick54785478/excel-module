package com.example.demo.application.assembler;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.example.demo.application.annotation.mapping.ExcelColumn;
import com.example.demo.application.annotation.mapping.ExcelSheet;
import com.example.demo.application.shared.command.WriteExcelCommand;
import com.example.demo.application.shared.command.WriteSheetCommand;
import com.example.demo.application.shared.dto.BeanSheetGottenResult;
import com.example.demo.application.shared.dto.ExcelGottenResult;
import com.example.demo.application.shared.dto.ExcelSheetGottenResult;
import com.example.demo.application.shared.model.CellCoordinate;
import com.example.demo.application.shared.model.ExcelRowError;
import com.example.demo.application.shared.model.SheetHeaderFooterConfig;
import com.example.demo.application.shared.model.SheetProtectionConfig;

/**
 * Excel 宣告式物件與純資料載體組裝器 (Excel Declarative Bean & Record Assembler)
 * <p>
 * <b>【架構定位與職責規範】</b><br>
 * 本類別位於 Clean Architecture 與六角形架構 (Hexagonal Architecture) 的 <b>Application Assembler 層</b>
 * ({@code application/assembler/})，遵循 {@code AGENTS.md} 規則 21 與 33~35 規範。<br>
 * 嚴守單一職責原則 (Single Responsibility Principle, SRP)，作為防腐層 (Anti-Corruption Layer, ACL) 的重要基石，
 * 負責將純資料載體 (Excel 活頁簿寫入命令 {@link WriteExcelCommand}、讀取結果 DTO {@link ExcelGottenResult}、資料字典 Map)
 * 與強型別領域/應用層資料模型 (Java 21 Record 或標準 JavaBean POJO) 進行雙向安全轉換，
 * 確保 DTO 與 Resource 保持純淨資料載體屬性，不侵入任何物件轉換與反射解析邏輯。
 * </p>
 * <p>
 * <b>【核心特性與技術亮點】</b>
 * <ul>
 *   <li><b>原生支援 Java 21 Record</b>：
 *       自動探測 Record 的標準規範建構子 (Canonical Constructor) 與 Record Components，
 *       透過參數型別陣列精準實例化不可變 (Immutable) 資料物件，完美契合現代 Java 函式庫風格。</li>
 *   <li><b>高效能元數據快取 (Concurrent Metadata Cache)</b>：
 *       以類別 {@link Class} 為鍵，使用 {@link ConcurrentHashMap} 快取剖析後的 {@link ModelMetadata} 與存取器 (Method / Field)，
 *       消除原生反射機制每次呼叫 {@code getDeclaredFields()} 或 {@code getAnnotation()} 的重複運算與性能瓶頸。</li>
 *   <li><b>雙模智慧欄位比對 (Intelligent Column Matching)</b>：
 *       匯入時採用雙向寬鬆比對機制：優先依據 {@link ExcelColumn} 指定的表頭名稱 (忽略大小寫與前後空白) 比對，
 *       若表頭順序更換或未匹配，則自動回退至 {@code index} 絕對索引，保證高度的檔案相容性。</li>
 *   <li><b>多維度型別安全轉換與清洗 (Robust Type Coercion)</b>：
 *       內建支援整數、長整數、浮點數、高精度 {@link BigDecimal}、貨幣符號清洗 (自動剔除 {@code $}, {@code ¥}, 千分位逗號)、
 *       多元日期時間解析 ({@link LocalDate}, {@link LocalDateTime}, {@link Date} 支援多種常見日期樣式)、
 *       枚舉 ({@link Enum}) 名稱對應，以及布林值多語系寬鬆識別 ({@code true}, {@code 1}, {@code y}, {@code 是})。</li>
 *   <li><b>自訂格式化與預設值防禦 (Formatting & Default Value Fallback)</b>：
 *       支援 {@link ExcelColumn#format()} 搭配 {@link DecimalFormat} 或 {@link DateTimeFormatter} 於寫入時格式化；
 *       當來源為 {@code null} 或空白時，自動套用 {@link ExcelColumn#defaultValue()} 或型別安全初值，杜絕空指標例外。</li>
 *   <li><b>強型別與弱型別雙軌支援 (Map $\leftrightarrow$ Bean Interchangeability)</b>：
 *       完整支援與 {@code List<Map<String, Object>>} 的互相轉換，並以 {@link LinkedHashMap} 嚴格保留欄位宣告順序。</li>
 * </ul>
 * </p>
 *
 * @see ExcelSheet
 * @see ExcelColumn
 * @see WriteExcelCommand
 * @see WriteSheetCommand
 * @see ExcelGottenResult
 * @see ExcelSheetGottenResult
 */
public final class ExcelBeanAssembler {

	/**
	 * 模型元數據全域執行緒安全快取 (類別 Class $\to$ 模型元數據 ModelMetadata)
	 */
	private static final Map<Class<?>, ModelMetadata> METADATA_CACHE = new ConcurrentHashMap<>();

	/**
	 * 預設標準日期時間格式化器 (yyyy-MM-dd HH:mm:ss)
	 */
	private static final DateTimeFormatter DEFAULT_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	/**
	 * 預設標準日期格式化器 (yyyy-MM-dd)
	 */
	private static final DateTimeFormatter DEFAULT_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	/**
	 * 工具類別私有建構子，防止外部直接實例化
	 */
	private ExcelBeanAssembler() {
		// 工具類禁止實例化
	}

	// =========================================================================
	// 1. Model $\to$ WriteExcelCommand / WriteSheetCommand (寫入命令轉換管線)
	// =========================================================================

	/**
	 * 將強型別 Model 清單組裝為 Excel 活頁簿寫入命令 (預設包含單一工作表)。
	 * <p>
	 * 工作表名稱依據 {@link ExcelSheet} 註解解析；若未宣告則預設為模型類別名稱 (Simple Name)。
	 * </p>
	 *
	 * @param <T>        模型型別
	 * @param dataList   資料物件清單 (可為空清單，此時僅產出表頭列)
	 * @param modelClass 模型類別 Class (支援 POJO 與 Java 21 Record)
	 * @return 封裝完整活頁簿資料之 Excel 寫入命令 {@link WriteExcelCommand}
	 * @throws NullPointerException 若 {@code modelClass} 為 {@code null}
	 */
	public static <T> WriteExcelCommand toWriteCommand(List<T> dataList, Class<T> modelClass) {
		return toWriteCommand(dataList, modelClass, null);
	}

	/**
	 * 將強型別 Model 清單組裝為 Excel 活頁簿寫入命令 (可指定工作表名稱覆蓋註解預設值)。
	 *
	 * @param <T>             模型型別
	 * @param dataList        資料物件清單
	 * @param modelClass      模型類別 Class (支援 POJO 與 Java 21 Record)
	 * @param customSheetName 自訂工作表名稱 (若為 {@code null} 或空白，則自動套用 {@link ExcelSheet} 註解設定)
	 * @return 封裝完整活頁簿資料之 Excel 寫入命令 {@link WriteExcelCommand}
	 * @throws NullPointerException 若 {@code modelClass} 為 {@code null}
	 */
	public static <T> WriteExcelCommand toWriteCommand(List<T> dataList, Class<T> modelClass, String customSheetName) {
		WriteSheetCommand sheetCommand = toSheetCommand(dataList, modelClass, customSheetName);
		return WriteExcelCommand.of(sheetCommand);
	}

	/**
	 * 將強型別 Model 清單組裝為單一工作表寫入命令 {@link WriteSheetCommand}。
	 * <p>
	 * <b>轉換管線細節：</b>
	 * <ol>
	 *   <li>自快取取得或解析 {@code modelClass} 之 {@link ModelMetadata}。</li>
	 *   <li>決定工作表名稱 (優先順序: 自訂名稱 $\to$ {@link ExcelSheet} 註解 $\to$ 類別名稱)。</li>
	 *   <li>依據欄位設定產製標題列文字清單 (Headers)。</li>
	 *   <li>遍歷每一筆實體物件，透過反射存取器抽取屬性值，並套用 {@link #formatValueForWrite(Object, ColumnMetadata)} 格式化。</li>
	 *   <li>組裝並回傳不可變之 {@link WriteSheetCommand}。</li>
	 * </ol>
	 * </p>
	 *
	 * @param <T>             模型型別
	 * @param dataList        資料物件清單
	 * @param modelClass      模型類別 Class
	 * @param customSheetName 自訂工作表名稱 (可為 {@code null})
	 * @return 單一工作表寫入命令 {@link WriteSheetCommand}
	 * @throws NullPointerException 若 {@code modelClass} 為 {@code null}
	 */
	public static <T> WriteSheetCommand toSheetCommand(List<T> dataList, Class<T> modelClass, String customSheetName) {
		Objects.requireNonNull(modelClass, "modelClass 不得為空");
		ModelMetadata metadata = getMetadata(modelClass);

		// 1. 決定工作表名稱
		String sheetName = (customSheetName != null && !customSheetName.isBlank())
				? customSheetName.trim()
				: metadata.sheetName();

		// 2. 產製表頭名稱清單
		List<String> headers = metadata.columns().stream()
				.map(ColumnMetadata::columnName)
				.toList();

		// 3. 防禦空清單：若無資料則直接產出僅含表頭的工作表
		if (dataList == null || dataList.isEmpty()) {
			return WriteSheetCommand.builder()
					.sheetName(sheetName)
					.headers(headers)
					.rows(Collections.emptyList())
					.protectionConfig(metadata.protectionConfig())
					.headerFooterConfig(metadata.headerFooterConfig())
					.build();
		}

		// 4. 逐筆遍歷資料列並透過反射存取器抽取數值，同時解析並注入宣告式動態公式
		List<List<Object>> rows = new ArrayList<>(dataList.size());
		Map<String, String> cellFormulas = new LinkedHashMap<>();
		for (int r = 0; r < dataList.size(); r++) {
			T item = dataList.get(r);
			if (item == null) {
				continue;
			}
			int excelRowNumber = r + 2; // 表頭為第 1 列 (Excel 1-based)，資料自第 2 列起算
			List<Object> row = new ArrayList<>(metadata.columns().size());
			for (int c = 0; c < metadata.columns().size(); c++) {
				ColumnMetadata col = metadata.columns().get(c);
				Object rawValue = col.getValue(item);
				Object formattedValue = formatValueForWrite(rawValue, col);
				row.add(formattedValue);

				if (col.formula() != null && !col.formula().isBlank()) {
					String cellRef = CellCoordinate.format(c, r + 1);
					String formulaExpr = col.formula().replace("{row}", String.valueOf(excelRowNumber));
					cellFormulas.put(cellRef, formulaExpr);
				}
			}
			rows.add(row);
		}

		return WriteSheetCommand.builder()
				.sheetName(sheetName)
				.headers(headers)
				.rows(rows)
				.cellFormulas(cellFormulas)
				.protectionConfig(metadata.protectionConfig())
				.headerFooterConfig(metadata.headerFooterConfig())
				.build();
	}

	/**
	 * 將資料字典清單 (Map List) 與標題清單組裝為 Excel 活頁簿寫入命令 {@link WriteExcelCommand}。
	 * <p>
	 * 適用於無法預先定義 Java Model 或動態生成欄位之匯出場景。
	 * </p>
	 *
	 * @param sheetName 工作表名稱
	 * @param headers   標題列清單 (作為對應 Map 欄位鍵值的基準)
	 * @param mapRows   資料字典清單 (Key 需對應 headers 之文字)
	 * @return Excel 寫入命令 {@link WriteExcelCommand}
	 */
	public static WriteExcelCommand toWriteCommandFromMaps(String sheetName, List<String> headers, List<Map<String, Object>> mapRows) {
		WriteSheetCommand sheet = toSheetCommandFromMaps(sheetName, headers, mapRows);
		return WriteExcelCommand.of(sheet);
	}

	/**
	 * 將資料字典清單 (Map List) 與標題清單組裝為工作表寫入命令 {@link WriteSheetCommand}。
	 *
	 * @param sheetName 工作表名稱 (為空時預設為 "Sheet1")
	 * @param headers   標題列清單
	 * @param mapRows   資料字典清單
	 * @return 工作表寫入命令 {@link WriteSheetCommand}
	 */
	public static WriteSheetCommand toSheetCommandFromMaps(String sheetName, List<String> headers, List<Map<String, Object>> mapRows) {
		String effectiveSheetName = (sheetName != null && !sheetName.isBlank()) ? sheetName.trim() : "Sheet1";
		List<String> effectiveHeaders = (headers != null) ? headers : Collections.emptyList();
		List<List<Object>> rows = new ArrayList<>(mapRows != null ? mapRows.size() : 0);

		if (mapRows != null) {
			for (Map<String, Object> map : mapRows) {
				List<Object> row = new ArrayList<>(effectiveHeaders.size());
				for (String header : effectiveHeaders) {
					row.add(map != null ? map.get(header) : null);
				}
				rows.add(row);
			}
		}

		return WriteSheetCommand.of(effectiveSheetName, effectiveHeaders, rows);
	}

	// =========================================================================
	// 2. ExcelGottenResult / ExcelSheetGottenResult $\to$ List<T> (讀取結果解析管線)
	// =========================================================================

	/**
	 * 將整份 Excel 讀取結果的第一個工作表解析並組裝為強型別 Model 清單。
	 *
	 * @param <T>        目標模型型別
	 * @param result     Excel 讀取結果 DTO 物件 (若為 {@code null} 或無工作表則回傳空清單)
	 * @param modelClass 目標模型類別 Class (支援 POJO 與 Record)
	 * @return 解析後之不可變 Model 物件清單
	 * @throws NullPointerException 若 {@code modelClass} 為 {@code null}
	 */
	public static <T> List<T> toBeanList(ExcelGottenResult result, Class<T> modelClass) {
		return toBeanList(result, modelClass, null);
	}

	/**
	 * 將活頁簿讀取結果依據自訂工作表名稱或註解設定智慧探測解析並組裝為強型別 Model 清單。
	 *
	 * @param <T>             目標模型型別
	 * @param result          Excel 讀取結果 DTO 物件 (若為 {@code null} 或無工作表則回傳空清單)
	 * @param modelClass      目標模型類別 Class (支援 POJO 與 Record)
	 * @param customSheetName 自訂工作表名稱 (若為 null 則自動依據 {@link ExcelSheet} 註解設定進行智慧探測)
	 * @return 解析後之不可變 Model 物件清單
	 * @throws NullPointerException 若 {@code modelClass} 為 {@code null}
	 */
	public static <T> List<T> toBeanList(ExcelGottenResult result, Class<T> modelClass, String customSheetName) {
		Objects.requireNonNull(modelClass, "modelClass 不得為空");
		if (result == null || result.isEmpty()) {
			return Collections.emptyList();
		}
		ExcelSheetGottenResult targetSheet = resolveTargetSheet(result, modelClass, customSheetName);
		if (targetSheet == null || targetSheet.isEmpty()) {
			return Collections.emptyList();
		}
		return toBeanList(targetSheet, modelClass);
	}

	/**
	 * 將活頁簿讀取結果容錯組裝為強型別 Model 清單，並主動收集儲存格型別不合之錯誤事實。
	 *
	 * @param <T>        目標模型型別
	 * @param result     Excel 活頁簿讀取結果 DTO 物件
	 * @param modelClass 目標模型類別 Class
	 * @return 包含模型清單與型別轉換錯誤事實之組裝結果 {@link BeanAssemblyResult}
	 * @throws NullPointerException 若 {@code modelClass} 為 {@code null}
	 */
	public static <T> BeanAssemblyResult<T> toBeanListWithErrors(ExcelGottenResult result, Class<T> modelClass) {
		return toBeanListWithErrors(result, modelClass, null);
	}

	/**
	 * 將活頁簿讀取結果依指定工作表名稱容錯組裝為強型別 Model 清單，並主動收集儲存格型別不合之錯誤事實。
	 *
	 * @param <T>             目標模型型別
	 * @param result          Excel 活頁簿讀取結果 DTO 物件
	 * @param modelClass      目標模型類別 Class
	 * @param customSheetName 自訂工作表名稱 (可為 null)
	 * @return 包含模型清單與型別轉換錯誤事實之組裝結果 {@link BeanAssemblyResult}
	 * @throws NullPointerException 若 {@code modelClass} 為 {@code null}
	 */
	public static <T> BeanAssemblyResult<T> toBeanListWithErrors(ExcelGottenResult result, Class<T> modelClass, String customSheetName) {
		Objects.requireNonNull(modelClass, "modelClass 不得為空");
		if (result == null || result.isEmpty()) {
			return new BeanAssemblyResult<>(Collections.emptyList(), Collections.emptyList(), 0);
		}
		ExcelSheetGottenResult targetSheet = resolveTargetSheet(result, modelClass, customSheetName);
		if (targetSheet == null || targetSheet.isEmpty()) {
			return new BeanAssemblyResult<>(Collections.emptyList(), Collections.emptyList(), 0);
		}
		return toBeanListWithErrors(targetSheet, modelClass);
	}

	/**
	 * 依據自訂工作表名稱或 Model 之 {@link ExcelSheet} 註解設定，智慧探測並解析活頁簿中對應的工作表。
	 * <p>
	 * <b>工作表探測優先順序：</b>
	 * <ol>
	 *   <li>若呼叫端傳入有效之 {@code customSheetName}，優先尋找該名稱之工作表 (支援忽略大小寫與部分包含)。</li>
	 *   <li>依據模型上之 {@link ExcelSheet} 註解設定之工作表名稱進行精準比對。</li>
	 *   <li>進行語意簡化與去除後綴之模糊比對 (例如「單階BOM清單」能自動匹配「單階BOM匯入清單」)。</li>
	 *   <li>若皆無匹配，則退回預設第 0 個工作表。</li>
	 * </ol>
	 * </p>
	 *
	 * @param result          Excel 活頁簿讀取結果 DTO
	 * @param modelClass      目標映射模型 Class
	 * @param customSheetName 自訂工作表名稱 (可為 {@code null})
	 * @return 匹配之工作表讀取結果；若活頁簿為空則回傳 {@code null}
	 */
	public static ExcelSheetGottenResult resolveTargetSheet(ExcelGottenResult result, Class<?> modelClass, String customSheetName) {
		if (result == null || result.isEmpty()) {
			return null;
		}

		// 1. 若呼叫端明確指定特定工作表名稱
		if (customSheetName != null && !customSheetName.isBlank()) {
			String trimmed = customSheetName.trim();
			Optional<ExcelSheetGottenResult> explicit = result.getSheet(trimmed);
			if (explicit.isPresent()) {
				return explicit.get();
			}
			for (ExcelSheetGottenResult sheet : result.getSheets()) {
				if (sheet.sheetName().trim().equalsIgnoreCase(trimmed)
						|| sheet.sheetName().contains(trimmed)
						|| trimmed.contains(sheet.sheetName())) {
					return sheet;
				}
			}
			// 明確指定特定工作表名稱但未比對成功，直接返回 null，嚴禁保底降級至其他工作表
			return null;
		}

		// 2. 依據 Model 上的 @ExcelSheet 註解工作表名稱進行探測
		if (modelClass != null) {
			ModelMetadata metadata = getMetadata(modelClass);
			String targetSheetName = metadata.sheetName();
			if (targetSheetName != null && !targetSheetName.isBlank() && !targetSheetName.equals(modelClass.getSimpleName())) {
				String trimmed = targetSheetName.trim();
				// 2-1. 精準比對
				Optional<ExcelSheetGottenResult> matched = result.getSheet(trimmed);
				if (matched.isPresent()) {
					return matched.get();
				}
				// 2-2. 註解別名比對 (Sheet Aliases Match)
				if (metadata.sheetAliases() != null) {
					for (String alias : metadata.sheetAliases()) {
						if (alias != null && !alias.isBlank()) {
							Optional<ExcelSheetGottenResult> aliasMatched = result.getSheet(alias.trim());
							if (aliasMatched.isPresent()) {
								return aliasMatched.get();
							}
						}
					}
				}
				// 2-3. 語意簡化模糊比對 (例如 "單階BOM清單" 與 "單階BOM匯入清單" 同屬單階BOM)
				String simplifiedTarget = simplifySheetName(trimmed);
				for (ExcelSheetGottenResult sheet : result.getSheets()) {
					String simplifiedSheet = simplifySheetName(sheet.sheetName());
					if (!simplifiedTarget.isEmpty() && !simplifiedSheet.isEmpty()) {
						if (simplifiedSheet.equalsIgnoreCase(simplifiedTarget)
								|| sheet.sheetName().contains(simplifiedTarget)
								|| trimmed.contains(simplifiedSheet)
								|| (simplifiedTarget.length() >= 3 && simplifiedSheet.contains(simplifiedTarget))) {
							return sheet;
						}
					}
				}
			}
		}

		// 3. 預設退回第一個工作表
		return result.getFirstSheet().orElse(null);
	}

	/**
	 * 判斷指定工作表是否匹配特定模型類別 (依據表名、@ExcelSheet 別名與欄位表頭匹配度)。
	 *
	 * @param <T>        模型型別
	 * @param sheet      工作表讀取結果物件
	 * @param modelClass 模型類別 Class
	 * @return 若該工作表相符則回傳 {@code true}，否則為 {@code false}
	 */
	public static <T> boolean isSheetMatchingModel(ExcelSheetGottenResult sheet, Class<T> modelClass) {
		if (sheet == null || modelClass == null) {
			return false;
		}
		ModelMetadata metadata = getMetadata(modelClass);
		String sheetName = sheet.sheetName() != null ? sheet.sheetName().trim() : "";

		// 1. 工作表名稱精確比對 (@ExcelSheet name 與 aliases)
		if (!sheetName.isEmpty()) {
			if (metadata.sheetName() != null && metadata.sheetName().equalsIgnoreCase(sheetName)) {
				return true;
			}
			if (metadata.sheetAliases() != null) {
				for (String alias : metadata.sheetAliases()) {
					if (alias != null && !alias.isBlank() && alias.trim().equalsIgnoreCase(sheetName)) {
						return true;
					}
				}
			}
			// 語意模糊比對 (例如 "單階BOM清單" 與 "單階BOM匯入清單" 同屬單階BOM)
			String simplifiedTarget = simplifySheetName(metadata.sheetName());
			String simplifiedSheet = simplifySheetName(sheetName);
			if (!simplifiedTarget.isEmpty() && !simplifiedSheet.isEmpty()) {
				if (simplifiedSheet.equalsIgnoreCase(simplifiedTarget)
						|| simplifiedSheet.contains(simplifiedTarget)
						|| simplifiedTarget.contains(simplifiedSheet)) {
					return true;
				}
			}
			if (metadata.sheetAliases() != null) {
				for (String alias : metadata.sheetAliases()) {
					if (alias != null && !alias.isBlank()) {
						String simplifiedAlias = simplifySheetName(alias);
						if (!simplifiedAlias.isEmpty()
								&& (simplifiedSheet.equalsIgnoreCase(simplifiedAlias)
										|| simplifiedSheet.contains(simplifiedAlias)
										|| simplifiedAlias.contains(simplifiedSheet))) {
							return true;
						}
					}
				}
			}
		}

		// 2. 若工作表名稱為具體非通用名稱 (非 Sheet1, 工作表1 等)，且模型定義了明確的 @ExcelSheet 名稱，
		// 則表示該工作表屬於其他具體業務領域，嚴禁跨領域誤判
		boolean hasConfiguredSheetName = metadata.sheetName() != null && !metadata.sheetName().isBlank()
				&& !metadata.sheetName().equals(modelClass.getSimpleName());
		boolean isGenericSheetName = sheetName.isEmpty()
				|| sheetName.matches("^(?i)(sheet|工作表|table|data|頁|page)\\s*\\d*$");

		if (hasConfiguredSheetName && !isGenericSheetName) {
			return false;
		}

		// 3. 通用工作表名稱或未指定 sheetName 時，進行表頭欄位匹配度檢查
		List<String> headers = sheet.headers();
		if (headers != null && !headers.isEmpty()) {
			int[] colIndices = resolveColumnIndices(metadata, headers);
			int matchedCount = 0;
			for (int idx : colIndices) {
				if (idx >= 0) {
					matchedCount++;
				}
			}
			int threshold = Math.max(3, (int) Math.ceil(metadata.columns().size() * 0.5));
			return matchedCount >= threshold;
		}

		return false;
	}

	/**
	 * 走訪活頁簿中所有與目標模型相符之工作表，並將解析所得之模型集合全部合併回傳。
	 *
	 * @param <T>        目標模型型別
	 * @param result     Excel 活頁簿讀取結果 DTO
	 * @param modelClass 目標模型類別 Class
	 * @return 包含所有相符工作表合併名稱與全部解析物件之 {@link BeanSheetGottenResult}
	 */
	public static <T> BeanSheetGottenResult<T> toAllBeans(ExcelGottenResult result, Class<T> modelClass) {
		Objects.requireNonNull(modelClass, "modelClass 不得為空");
		if (result == null || result.isEmpty()) {
			return new BeanSheetGottenResult<>("無資料", Collections.emptyList());
		}

		List<T> allItems = new ArrayList<>();
		List<String> matchedSheetNames = new ArrayList<>();

		for (ExcelSheetGottenResult sheet : result.getSheets()) {
			if (isSheetMatchingModel(sheet, modelClass)) {
				List<T> sheetItems = toBeanList(sheet, modelClass);
				if (!sheetItems.isEmpty()) {
					allItems.addAll(sheetItems);
					matchedSheetNames.add(sheet.sheetName());
				}
			}
		}

		// 若沒有任何工作表經由規則匹配，但活頁簿只有一個工作表，則保底嘗試解析該唯一工作表
		if (matchedSheetNames.isEmpty() && result.getSheetCount() == 1) {
			ExcelSheetGottenResult firstSheet = result.getSheets().get(0);
			List<T> items = toBeanList(firstSheet, modelClass);
			return new BeanSheetGottenResult<>(firstSheet.sheetName(), items);
		}

		String aggregatedSheetName;
		if (matchedSheetNames.isEmpty()) {
			aggregatedSheetName = "無相符工作表";
		} else if (matchedSheetNames.size() == 1) {
			aggregatedSheetName = matchedSheetNames.get(0);
		} else {
			aggregatedSheetName = "全部工作表 (" + String.join(", ", matchedSheetNames) + ")";
		}

		return new BeanSheetGottenResult<>(aggregatedSheetName, allItems);
	}

	private static String simplifySheetName(String sheetName) {
		if (sheetName == null) return "";
		return sheetName.replaceAll("[\\s_\\-()]", "")
				.replace("清單", "")
				.replace("匯入", "")
				.replace("對照表", "")
				.replace("主檔", "")
				.replace("明細表", "")
				.replace("展開", "")
				.replace("表", "")
				.trim();
	}

	/**
	 * 將特定工作表讀取結果解析並組裝為強型別 Model 清單。
	 * <p>
	 * <b>解析管線細節：</b>
	 * <ol>
	 *   <li>提取工作表讀取結果的標題 (Headers) 與原始資料列 (Rows)。</li>
	 *   <li>調用 {@link #resolveColumnIndices(ModelMetadata, List)} 計算模型屬性與 Excel 表頭位置的映射關係。</li>
	 *   <li>針對每一筆資料列調用 {@link #instantiateModel(ModelMetadata, int[], List, Class)} 實例化強型別物件。</li>
	 *   <li>自動套用數值清洗、字串轉型、日期時間與枚舉解析。</li>
	 * </ol>
	 * </p>
	 *
	 * @param <T>         目標模型型別
	 * @param sheetResult 工作表讀取結果 DTO 物件
	 * @param modelClass  目標模型類別 Class
	 * @return 解析後之不可變 Model 物件清單
	 * @throws NullPointerException 若 {@code modelClass} 為 {@code null}
	 */
	public static <T> List<T> toBeanList(ExcelSheetGottenResult sheetResult, Class<T> modelClass) {
		Objects.requireNonNull(modelClass, "modelClass 不得為空");
		if (sheetResult == null || sheetResult.isEmpty()) {
			return Collections.emptyList();
		}

		ModelMetadata metadata = getMetadata(modelClass);
		List<String> sheetHeaders = sheetResult.headers();
		List<List<Object>> dataRows = sheetResult.rows();

		// 建立欄位位置映射索引表 (Column Index Mapping)
		int[] colIndices = resolveColumnIndices(metadata, sheetHeaders);

		List<T> resultList = new ArrayList<>(dataRows.size());
		for (List<Object> row : dataRows) {
			if (row == null || row.isEmpty()) {
				continue;
			}
			T instance = instantiateModel(metadata, colIndices, row, modelClass);
			if (instance != null) {
				resultList.add(instance);
			}
		}

		return Collections.unmodifiableList(resultList);
	}

	/**
	 * 模型組裝結果資料載體 (封裝成功模型清單、儲存格轉換錯誤事實與資料總列數)。
	 * <p>
	 * 用於資料校驗與匯入管線中，將型別不相容之儲存格收集為 {@link ExcelRowError}，
	 * 避免因個別儲存格錯誤而中斷整份試算表之校驗流程。
	 * </p>
	 *
	 * @param <T>              目標模型型別
	 * @param items            完全合格且成功實例化之模型物件清單
	 * @param conversionErrors 儲存格型別轉換失敗之錯誤事實清單
	 * @param totalRows        工作表資料總列數
	 */
	public record BeanAssemblyResult<T>(
			List<T> items,
			List<ExcelRowError> conversionErrors,
			int totalRows
	) {
		/**
		 * 判斷組裝過程中是否存在儲存格型別轉換錯誤
		 *
		 * @return 若存在錯誤則為 {@code true}
		 */
		public boolean hasErrors() {
			return conversionErrors != null && !conversionErrors.isEmpty();
		}
	}

	/**
	 * 將工作表讀取結果容錯組裝為強型別 Model 清單，並主動收集儲存格型別不合之錯誤事實。
	 * <p>
	 * <b>【容錯組裝管線細節】</b>
	 * <ol>
	 *   <li>逐一走訪每筆資料列與目標欄位。</li>
	 *   <li>當儲存格數值無法轉為目標欄位型別時，主動封裝為 {@link ExcelRowError} 收集入清單，不拋出中斷例外。</li>
	 *   <li>若該列無任何型別錯誤，實例化模型並納入合格清單；若有錯誤則排除該筆物件並精準記錄錯誤座標。</li>
	 * </ol>
	 * </p>
	 *
	 * @param <T>         目標模型型別
	 * @param sheetResult 工作表讀取結果 DTO 物件
	 * @param modelClass  目標模型類別 Class
	 * @return 包含模型清單與型別轉換錯誤事實之組裝結果 {@link BeanAssemblyResult}
	 * @throws NullPointerException 若 {@code modelClass} 為 {@code null}
	 */
	@SuppressWarnings("unchecked")
	public static <T> BeanAssemblyResult<T> toBeanListWithErrors(ExcelSheetGottenResult sheetResult, Class<T> modelClass) {
		Objects.requireNonNull(modelClass, "modelClass 不得為空");
		if (sheetResult == null || sheetResult.isEmpty()) {
			return new BeanAssemblyResult<>(Collections.emptyList(), Collections.emptyList(), 0);
		}

		ModelMetadata metadata = getMetadata(modelClass);
		List<String> sheetHeaders = sheetResult.headers();
		List<List<Object>> dataRows = sheetResult.rows();

		int[] colIndices = resolveColumnIndices(metadata, sheetHeaders);
		int colCount = metadata.columns().size();

		List<T> resultList = new ArrayList<>(dataRows.size());
		List<ExcelRowError> conversionErrors = new ArrayList<>();

		for (int r = 0; r < dataRows.size(); r++) {
			List<Object> row = dataRows.get(r);
			if (row == null || row.isEmpty()) {
				continue;
			}
			int excelRowIndex = r + 2; // 表頭為第 1 列，資料自第 2 列起 (1-based)
			Object[] convertedValues = new Object[colCount];
			boolean rowHasConversionError = false;

			for (int i = 0; i < colCount; i++) {
				ColumnMetadata col = metadata.columns().get(i);
				int targetIdx = colIndices[i];
				Object rawVal = (targetIdx >= 0 && targetIdx < row.size()) ? row.get(targetIdx) : null;
				try {
					convertedValues[i] = convertValue(rawVal, col);
				} catch (Exception ex) {
					rowHasConversionError = true;
					conversionErrors.add(ExcelRowError.ofCell(
							excelRowIndex,
							col.explicitIndex() >= 0 ? col.explicitIndex() : targetIdx,
							col.columnName(),
							rawVal,
							"儲存格格式不相容 (無法轉換為 " + col.type().getSimpleName() + "): " + ex.getMessage(),
							"TYPE_CONVERSION_ERROR"
					));
					convertedValues[i] = col.type().isPrimitive() ? getPrimitiveDefault(col.type()) : null;
				}
			}

			if (!rowHasConversionError) {
				try {
					if (metadata.isRecord()) {
						T instance = (T) metadata.canonicalConstructor().newInstance(convertedValues);
						resultList.add(instance);
					} else {
						T instance = (T) metadata.noArgConstructor().newInstance();
						for (int i = 0; i < colCount; i++) {
							ColumnMetadata col = metadata.columns().get(i);
							col.setValue(instance, convertedValues[i]);
						}
						resultList.add(instance);
					}
				} catch (Exception e) {
					conversionErrors.add(ExcelRowError.ofRow(
							excelRowIndex,
							"資料列實例化失敗: " + e.getMessage(),
							"ROW_INSTANTIATION_ERROR"
					));
				}
			}
		}

		return new BeanAssemblyResult<>(
				Collections.unmodifiableList(resultList),
				Collections.unmodifiableList(conversionErrors),
				dataRows.size()
		);
	}

	// =========================================================================
	// 3. Map $\leftrightarrow$ Bean 雙向組裝支援 (資料字典與強型別互轉)
	// =========================================================================

	/**
	 * 將以標題名稱為鍵的資料字典清單 (Map List) 轉換為強型別 Model 清單。
	 * <p>
	 * 支援從 Map 中依據表頭名稱 (Header Name) 或類別屬性名稱 (Field Name) 雙重匹配資料鍵。
	 * </p>
	 *
	 * @param <T>        目標模型型別
	 * @param mapList    資料字典清單 (Key: 標題欄位名, Value: 儲存格值)
	 * @param modelClass 目標模型類別 Class
	 * @return 強型別 Model 清單 (不可變清單)
	 * @throws NullPointerException 若 {@code modelClass} 為 {@code null}
	 */
	public static <T> List<T> fromMaps(List<Map<String, Object>> mapList, Class<T> modelClass) {
		Objects.requireNonNull(modelClass, "modelClass 不得為空");
		if (mapList == null || mapList.isEmpty()) {
			return Collections.emptyList();
		}

		ModelMetadata metadata = getMetadata(modelClass);
		List<T> list = new ArrayList<>(mapList.size());

		for (Map<String, Object> map : mapList) {
			if (map == null) {
				continue;
			}
			T instance = instantiateFromMap(metadata, map, modelClass);
			if (instance != null) {
				list.add(instance);
			}
		}
		return Collections.unmodifiableList(list);
	}

	/**
	 * 將強型別 Model 清單轉換為以標題名稱為鍵的資料字典清單 (Map List)。
	 * <p>
	 * 內部使用 {@link LinkedHashMap}，確保產出的鍵值順序完全符合 Model 元數據之欄位排序宣告。
	 * </p>
	 *
	 * @param <T>        模型型別
	 * @param dataList   Model 物件清單
	 * @param modelClass 模型類別 Class
	 * @return 鍵值字典清單 (保留欄位排序之不可變 Map 清單)
	 * @throws NullPointerException 若 {@code modelClass} 為 {@code null}
	 */
	public static <T> List<Map<String, Object>> toMaps(List<T> dataList, Class<T> modelClass) {
		Objects.requireNonNull(modelClass, "modelClass 不得為空");
		if (dataList == null || dataList.isEmpty()) {
			return Collections.emptyList();
		}

		ModelMetadata metadata = getMetadata(modelClass);
		List<Map<String, Object>> list = new ArrayList<>(dataList.size());

		for (T item : dataList) {
			if (item == null) {
				continue;
			}
			Map<String, Object> map = new LinkedHashMap<>();
			for (ColumnMetadata col : metadata.columns()) {
				map.put(col.columnName(), col.getValue(item));
			}
			list.add(Collections.unmodifiableMap(map));
		}
		return Collections.unmodifiableList(list);
	}

	// =========================================================================
	// 4. 核心反射與模型實例化引擎 (Reflection & Instantiation Engine)
	// =========================================================================

	/**
	 * 依據資料列與位置對照表實例化強型別 Model 物件。
	 * <p>
	 * <b>分流機制：</b>
	 * <ul>
	 *   <li><b>Java 21 Record</b>：將所有轉換後的欄位參數收集為 {@code Object[]} 陣列，調用標準建構子 (Canonical Constructor) 進行不可變實例化。</li>
	 *   <li><b>標準 JavaBean</b>：調用公開無參建構子產生實體後，逐一透過 Field 或 Setter 注入轉換後的屬性值。</li>
	 * </ul>
	 * </p>
	 *
	 * @param <T>        模型型別
	 * @param metadata   模型元數據
	 * @param colIndices 欄位位置映射表
	 * @param row        Excel 資料列
	 * @param modelClass 模型類別 Class
	 * @return 實例化完成之強型別物件
	 * @throws IllegalArgumentException 若實例化失敗或型別不相容
	 */
	@SuppressWarnings("unchecked")
	private static <T> T instantiateModel(ModelMetadata metadata, int[] colIndices, List<Object> row, Class<T> modelClass) {
		try {
			int colCount = metadata.columns().size();
			Object[] convertedValues = new Object[colCount];

			// 1. 逐一提取並安全轉換儲存格數值
			for (int i = 0; i < colCount; i++) {
				ColumnMetadata col = metadata.columns().get(i);
				int targetIdx = colIndices[i];
				Object rawVal = (targetIdx >= 0 && targetIdx < row.size()) ? row.get(targetIdx) : null;
				convertedValues[i] = convertValue(rawVal, col);
			}

			// 2. 依據是否為 Java Record 進行實例化分流
			if (metadata.isRecord()) {
				// Java 21 Record：調用標準建構子 (Canonical Constructor)
				return (T) metadata.canonicalConstructor().newInstance(convertedValues);
			} else {
				// 標準 JavaBean：調用無參建構子後透過欄位設定值
				T instance = (T) metadata.noArgConstructor().newInstance();
				for (int i = 0; i < colCount; i++) {
					ColumnMetadata col = metadata.columns().get(i);
					col.setValue(instance, convertedValues[i]);
				}
				return instance;
			}
		} catch (Exception e) {
			throw new IllegalArgumentException("建立 Excel 模型實例失敗 [" + modelClass.getName() + "]: " + e.getMessage(), e);
		}
	}

	/**
	 * 從單筆資料字典 Map 實例化強型別 Model 物件。
	 *
	 * @param <T>        模型型別
	 * @param metadata   模型元數據
	 * @param map        單筆資料 Map
	 * @param modelClass 模型類別 Class
	 * @return 實例化完成之強型別物件
	 * @throws IllegalArgumentException 若實例化失敗
	 */
	@SuppressWarnings("unchecked")
	private static <T> T instantiateFromMap(ModelMetadata metadata, Map<String, Object> map, Class<T> modelClass) {
		try {
			int colCount = metadata.columns().size();
			Object[] convertedValues = new Object[colCount];

			for (int i = 0; i < colCount; i++) {
				ColumnMetadata col = metadata.columns().get(i);
				// 支援以表頭名稱或屬性名稱查詢
				Object rawVal = map.get(col.columnName());
				if (rawVal == null && col.fieldName() != null) {
					rawVal = map.get(col.fieldName());
				}
				convertedValues[i] = convertValue(rawVal, col);
			}

			if (metadata.isRecord()) {
				return (T) metadata.canonicalConstructor().newInstance(convertedValues);
			} else {
				T instance = (T) metadata.noArgConstructor().newInstance();
				for (int i = 0; i < colCount; i++) {
					ColumnMetadata col = metadata.columns().get(i);
					col.setValue(instance, convertedValues[i]);
				}
				return instance;
			}
		} catch (Exception e) {
			throw new IllegalArgumentException("從 Map 轉換 Excel 模型實例失敗 [" + modelClass.getName() + "]: " + e.getMessage(), e);
		}
	}

	/**
	 * 計算模型屬性與 Excel 表頭之映射索引 (Column Index Resolution)。
	 * <p>
	 * <b>比對策略：</b>
	 * <ol>
	 *   <li><b>名稱模糊比對 (優先)</b>：將模型中的 {@code columnName} 或 {@code fieldName} 與 Excel 實際表頭逐一比對 (忽略空白與大小寫)。</li>
	 *   <li><b>絕對索引比對 (次優)</b>：若表頭比對未中且宣告了 {@link ExcelColumn#index()} (大於等於 0)，則套用該絕對索引。</li>
	 *   <li><b>自然順序回退</b>：若完全無表頭，則依欄位自然宣告順序映射索引。</li>
	 * </ol>
	 * </p>
	 *
	 * @param metadata     模型元數據
	 * @param sheetHeaders Excel 工作表實際表頭清單
	 * @return 與 {@code metadata.columns()} 長度一致的整數陣列，內容為對應之 Excel 欄位索引 (未匹配則為 -1)
	 */
	private static int[] resolveColumnIndices(ModelMetadata metadata, List<String> sheetHeaders) {
		int colCount = metadata.columns().size();
		int[] indices = new int[colCount];
		Arrays.fill(indices, -1);
		boolean hasHeaders = sheetHeaders != null && !sheetHeaders.isEmpty();
		boolean[] headerClaimed = hasHeaders ? new boolean[sheetHeaders.size()] : new boolean[0];

		if (hasHeaders) {
			// Pass 1: 精準表頭名稱與屬性名稱比對 (Exact Column Name & Field Name Match)
			for (int i = 0; i < colCount; i++) {
				ColumnMetadata col = metadata.columns().get(i);
				for (int h = 0; h < sheetHeaders.size(); h++) {
					if (headerClaimed[h]) continue;
					String header = sheetHeaders.get(h);
					if (header != null && (header.trim().equalsIgnoreCase(col.columnName().trim())
							|| (col.fieldName() != null && header.trim().equalsIgnoreCase(col.fieldName().trim())))) {
						indices[i] = h;
						headerClaimed[h] = true;
						break;
					}
				}
			}

			// Pass 2: 註解別名比對 (Aliases Match)
			for (int i = 0; i < colCount; i++) {
				if (indices[i] != -1) continue;
				ColumnMetadata col = metadata.columns().get(i);
				if (col.aliases() != null && col.aliases().length > 0) {
					for (String alias : col.aliases()) {
						if (alias == null || alias.isBlank()) continue;
						String normAlias = alias.trim();
						for (int h = 0; h < sheetHeaders.size(); h++) {
							if (headerClaimed[h]) continue;
							String header = sheetHeaders.get(h);
							if (header != null && header.trim().equalsIgnoreCase(normAlias)) {
								indices[i] = h;
								headerClaimed[h] = true;
								break;
							}
						}
						if (indices[i] != -1) break;
					}
				}
			}

			// Pass 3: 智慧模糊比對 (Fuzzy / Normalized Match - 去除符號、括號單位與常用後綴)
			for (int i = 0; i < colCount; i++) {
				if (indices[i] != -1) continue;
				ColumnMetadata col = metadata.columns().get(i);
				String normCol = normalizeHeader(col.columnName());
				if (normCol.isEmpty()) continue;
				for (int h = 0; h < sheetHeaders.size(); h++) {
					if (headerClaimed[h]) continue;
					String header = sheetHeaders.get(h);
					if (header != null) {
						String normHeader = normalizeHeader(header);
						if (!normHeader.isEmpty() && (normCol.equalsIgnoreCase(normHeader)
								|| normCol.contains(normHeader)
								|| normHeader.contains(normCol))) {
							indices[i] = h;
							headerClaimed[h] = true;
							break;
						}
					}
				}
			}
		}

		// Pass 4: Fallback 處理
		for (int i = 0; i < colCount; i++) {
			if (indices[i] == -1) {
				ColumnMetadata col = metadata.columns().get(i);
				if (!hasHeaders) {
					// 完全無表頭情況：使用宣告 index 或自然順序
					indices[i] = col.explicitIndex() >= 0 ? col.explicitIndex() : i;
				} else if (col.explicitIndex() >= 0 && col.explicitIndex() < headerClaimed.length && !headerClaimed[col.explicitIndex()]) {
					// 有表頭但該 index 未被任何欄位認領
					indices[i] = col.explicitIndex();
					headerClaimed[col.explicitIndex()] = true;
				}
			}
		}

		return indices;
	}

	private static String normalizeHeader(String header) {
		if (header == null) return "";
		return header.replaceAll("[\\s_\\-()%（）\\[\\]#*]", "")
				.replace("代碼", "")
				.replace("編號", "")
				.replace("料號", "")
				.replace("清單", "")
				.replace("說明", "")
				.trim();
	}

	// =========================================================================
	// 5. 型別轉換與格式化處理 (Type Coercion & Formatting)
	// =========================================================================

	/**
	 * 匯出時格式化儲存格數值。
	 * <p>
	 * 支援日期時間樣式格式化 (如 {@code "yyyy-MM-dd"})、數值千分位或小數格式化 (如 {@code "#,##0.00"})、
	 * 以及 Enum 名稱序列化。
	 * </p>
	 *
	 * @param rawValue 原始屬性值
	 * @param col      欄位元數據設定
	 * @return 格式化後的文字或物件；若為空且有預設值則回傳預設值
	 */
	private static Object formatValueForWrite(Object rawValue, ColumnMetadata col) {
		if (rawValue == null) {
			return (col.defaultValue() != null && !col.defaultValue().isBlank()) ? col.defaultValue() : null;
		}

		if (col.format() != null && !col.format().isBlank()) {
			String fmt = col.format().trim();
			if (rawValue instanceof LocalDate date) {
				return date.format(DateTimeFormatter.ofPattern(fmt));
			} else if (rawValue instanceof LocalDateTime dateTime) {
				return dateTime.format(DateTimeFormatter.ofPattern(fmt));
			} else if (rawValue instanceof Date date) {
				LocalDateTime ldt = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
				return ldt.format(DateTimeFormatter.ofPattern(fmt));
			} else if (rawValue instanceof Number num) {
				try {
					DecimalFormat df = new DecimalFormat(fmt);
					return df.format(num);
				} catch (Exception ignored) {
					// 格式不符則回退原值
				}
			}
		}

		if (rawValue instanceof Enum<?> enumVal) {
			return enumVal.name();
		}

		return rawValue;
	}

	/**
	 * 匯入時將 Excel 原始儲存格數值轉換為目標屬性型別。
	 * <p>
	 * 包含空值防禦、預設值套用與基本型別安全初值處理。
	 * </p>
	 *
	 * @param rawVal 原始儲存格數值 (如 String, Double, Date)
	 * @param col    目標欄位元數據
	 * @return 轉換後之目標型別實體
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	private static Object convertValue(Object rawVal, ColumnMetadata col) {
		Class<?> targetType = col.type();

		// 1. 空值或空白字串防禦
		if (rawVal == null || (rawVal instanceof String s && s.trim().isEmpty())) {
			if (col.defaultValue() != null && !col.defaultValue().isBlank()) {
				return parseStringToType(col.defaultValue().trim(), targetType, col.format());
			}
			if (targetType.isPrimitive()) {
				return getPrimitiveDefault(targetType);
			}
			return null;
		}

		// 2. 若型別已完全相符，直接返回
		if (targetType.isInstance(rawVal)) {
			return rawVal;
		}

		// 3. 日期時間與數值型別原生交錯互轉 (防禦 Excel 序號與 Date/Temporal 互相指派)
		if (LocalDate.class.equals(targetType)) {
			if (rawVal instanceof LocalDateTime ldt) {
				return ldt.toLocalDate();
			}
			if (rawVal instanceof Date d) {
				return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
			}
			if (rawVal instanceof Number num) {
				return parseExcelSerialDate(num.doubleValue());
			}
		} else if (LocalDateTime.class.equals(targetType)) {
			if (rawVal instanceof LocalDate ld) {
				return ld.atStartOfDay();
			}
			if (rawVal instanceof Date d) {
				return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
			}
			if (rawVal instanceof Number num) {
				return parseExcelSerialDateTime(num.doubleValue());
			}
		} else if (Date.class.equals(targetType)) {
			if (rawVal instanceof LocalDate ld) {
				return Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
			}
			if (rawVal instanceof LocalDateTime ldt) {
				return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
			}
			if (rawVal instanceof Number num) {
				return Date.from(parseExcelSerialDateTime(num.doubleValue()).atZone(ZoneId.systemDefault()).toInstant());
			}
		}

		// 4. 轉為字串後調用智慧型別解析器
		String strVal = String.valueOf(rawVal).trim();
		return parseStringToType(strVal, targetType, col.format());
	}

	/**
	 * 字串至強型別之核心解析器。
	 * <p>
	 * <b>支援轉型類型：</b>
	 * <ul>
	 *   <li><b>整數 / 浮點數</b>：{@link Integer}, {@link Long}, {@link Double}, {@link Float} (自動清除貨幣符號 {@code $}, {@code ¥} 與千分位逗號)。</li>
	 *   <li><b>高精度數值</b>：{@link BigDecimal}, {@link BigInteger}。</li>
	 *   <li><b>布林值</b>：寬鬆識別 {@code true}, {@code 1}, {@code y}, {@code yes}, {@code 是}。</li>
	 *   <li><b>日期時間</b>：{@link LocalDate}, {@link LocalDateTime}, {@link Date}。</li>
	 *   <li><b>枚舉</b>：{@link Enum} 依名稱匹配。</li>
	 * </ul>
	 * </p>
	 *
	 * @param str        待解析字串
	 * @param targetType 目標型別 Class
	 * @param format     自訂日期或數值格式樣式
	 * @return 解析後之目標型別實例
	 * @throws IllegalArgumentException 若無法解析為目標型別
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	private static Object parseStringToType(String str, Class<?> targetType, String format) {
		if (String.class.equals(targetType)) {
			return str;
		}

		try {
			if (Integer.class.equals(targetType) || int.class.equals(targetType)) {
				String clean = str.replace(",", "").replace("$", "").replace("¥", "").replace("%", "").trim();
				return (int) Math.round(Double.parseDouble(clean));
			}
			if (Long.class.equals(targetType) || long.class.equals(targetType)) {
				String clean = str.replace(",", "").replace("$", "").replace("¥", "").replace("%", "").trim();
				return Math.round(Double.parseDouble(clean));
			}
			if (Double.class.equals(targetType) || double.class.equals(targetType)) {
				String clean = str.replace(",", "").replace("$", "").replace("¥", "").replace("%", "").trim();
				return Double.parseDouble(clean);
			}
			if (Float.class.equals(targetType) || float.class.equals(targetType)) {
				String clean = str.replace(",", "").replace("$", "").replace("¥", "").replace("%", "").trim();
				return Float.parseFloat(clean);
			}
			if (BigDecimal.class.equals(targetType)) {
				String clean = str.replace(",", "").replace("$", "").replace("¥", "").replace("%", "").trim();
				return new BigDecimal(clean);
			}
			if (BigInteger.class.equals(targetType)) {
				String clean = str.replace(",", "").replace("$", "").replace("¥", "").replace("%", "").trim();
				return new BigDecimal(clean).toBigInteger();
			}
			if (Boolean.class.equals(targetType) || boolean.class.equals(targetType)) {
				return "true".equalsIgnoreCase(str) || "1".equals(str) || "y".equalsIgnoreCase(str)
						|| "yes".equalsIgnoreCase(str) || "是".equals(str);
			}
			if (LocalDate.class.equals(targetType)) {
				return parseLocalDate(str, format);
			}
			if (LocalDateTime.class.equals(targetType)) {
				return parseLocalDateTime(str, format);
			}
			if (Date.class.equals(targetType)) {
				LocalDateTime ldt = parseLocalDateTime(str, format);
				return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
			}
			if (targetType.isEnum()) {
				return Enum.valueOf((Class<Enum>) targetType, str);
			}
		} catch (Exception e) {
			throw new IllegalArgumentException("無法將值 '" + str + "' 轉換為型別 " + targetType.getSimpleName(), e);
		}

		return str;
	}

	/**
	 * 解析 {@link LocalDate} 本地日期。
	 * <p>
	 * 支援 Excel 浮點數日期序號 (如 {@code 46327.0})、自訂格式、標準橫線格式 (yyyy-MM-dd)、
	 * 斜線格式 (yyyy/MM/dd)、點號格式 (yyyy.MM.dd) 與緊密數字格式 (yyyyMMdd)。
	 * </p>
	 *
	 * @param str    日期字串
	 * @param format 自訂格式 (可為空)
	 * @return 解析後之 {@link LocalDate}
	 */
	private static LocalDate parseLocalDate(String str, String format) {
		String clean = str.trim();

		// 1. 檢查是否為 Excel 數值序號或純數字日期
		if (isNumeric(clean)) {
			// 若為純 8 碼整數 (如 20261101)，優先以 yyyyMMdd 解析
			if (clean.length() == 8 && !clean.contains(".")) {
				try {
					return LocalDate.parse(clean, DateTimeFormatter.ofPattern("yyyyMMdd"));
				} catch (Exception ignored) {
				}
			}
			try {
				double serial = Double.parseDouble(clean);
				return parseExcelSerialDate(serial);
			} catch (Exception ignored) {
			}
		}

		// 2. 若指定格式，優先嘗試自訂格式
		if (format != null && !format.isBlank()) {
			try {
				return LocalDate.parse(clean, DateTimeFormatter.ofPattern(format.trim()));
			} catch (Exception ignored) {
				// 自訂格式失敗時容錯向下匹配常見格式
			}
		}

		// 3. 常見日期格式容錯匹配
		if (clean.length() >= 10 && clean.charAt(4) == '-' && clean.charAt(7) == '-') {
			return LocalDate.parse(clean.substring(0, 10), DEFAULT_DATE_FORMATTER);
		}
		if (clean.length() >= 10 && clean.charAt(4) == '/' && clean.charAt(7) == '/') {
			return LocalDate.parse(clean.substring(0, 10), DateTimeFormatter.ofPattern("yyyy/MM/dd"));
		}
		if (clean.length() >= 10 && clean.charAt(4) == '.' && clean.charAt(7) == '.') {
			return LocalDate.parse(clean.substring(0, 10), DateTimeFormatter.ofPattern("yyyy.MM.dd"));
		}
		if (clean.contains("T")) {
			return LocalDateTime.parse(clean).toLocalDate();
		}

		return LocalDate.parse(clean, DEFAULT_DATE_FORMATTER);
	}

	/**
	 * 解析 {@link LocalDateTime} 本地日期時間。
	 * <p>
	 * 支援 Excel 浮點數日期時間序號、自訂格式、僅日期補齊至當天 00:00:00、
	 * ISO-8601 (包含 'T')、標準 (yyyy-MM-dd HH:mm:ss) 與斜線樣式。
	 * </p>
	 *
	 * @param str    日期時間字串
	 * @param format 自訂格式 (可為空)
	 * @return 解析後之 {@link LocalDateTime}
	 */
	private static LocalDateTime parseLocalDateTime(String str, String format) {
		String clean = str.trim();

		// 1. 檢查是否為 Excel 數值序號或純數字日期
		if (isNumeric(clean)) {
			if (clean.length() == 8 && !clean.contains(".")) {
				try {
					return LocalDate.parse(clean, DateTimeFormatter.ofPattern("yyyyMMdd")).atStartOfDay();
				} catch (Exception ignored) {
				}
			}
			try {
				double serial = Double.parseDouble(clean);
				return parseExcelSerialDateTime(serial);
			} catch (Exception ignored) {
			}
		}

		// 2. 若指定格式，優先嘗試自訂格式
		if (format != null && !format.isBlank()) {
			try {
				return LocalDateTime.parse(clean, DateTimeFormatter.ofPattern(format.trim()));
			} catch (Exception ignored) {
				// 自訂格式失敗時容錯向下匹配常見格式
			}
		}

		// 3. 常見格式容錯匹配
		if (clean.length() == 10) {
			return parseLocalDate(clean, null).atStartOfDay();
		}
		if (clean.contains("T")) {
			return LocalDateTime.parse(clean);
		}
		if (clean.length() >= 19 && clean.charAt(4) == '-' && clean.charAt(7) == '-') {
			return LocalDateTime.parse(clean.substring(0, 19), DEFAULT_DATE_TIME_FORMATTER);
		}
		if (clean.length() >= 19 && clean.charAt(4) == '/' && clean.charAt(7) == '/') {
			return LocalDateTime.parse(clean.substring(0, 19), DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
		}

		return LocalDateTime.parse(clean, DEFAULT_DATE_TIME_FORMATTER);
	}

	/**
	 * 將 Excel 數值序號 (OADate / Serial Number) 轉換為 {@link LocalDate}。
	 * <p>
	 * Excel 日期系統以 1899-12-30 為基準日 (校正 Lotus 1-2-3 歷史 1900 閏年偏差)，
	 * 浮點數之整數部分代表自基準日起之天數。
	 * </p>
	 *
	 * @param serial Excel 日期浮點數序號 (如 46327.0 對應 2026-11-01)
	 * @return 解析後之 {@link LocalDate}
	 */
	public static LocalDate parseExcelSerialDate(double serial) {
		long days = (long) serial;
		if (days >= 61) {
			return LocalDate.of(1899, 12, 30).plusDays(days);
		} else if (days > 0) {
			return LocalDate.of(1899, 12, 31).plusDays(days);
		} else {
			return LocalDate.of(1899, 12, 30);
		}
	}

	/**
	 * 將 Excel 數值序號 (OADate / Serial Number) 轉換為 {@link LocalDateTime}。
	 * <p>
	 * 浮點數之整數部分為天數，小數部分為一天中之時間比率 (24 小時制)。
	 * </p>
	 *
	 * @param serial Excel 日期時間浮點數序號 (如 46327.5 對應 2026-11-01 12:00:00)
	 * @return 解析後之 {@link LocalDateTime}
	 */
	public static LocalDateTime parseExcelSerialDateTime(double serial) {
		LocalDate date = parseExcelSerialDate(serial);
		double fraction = serial - (long) serial;
		if (fraction <= 0.0) {
			return date.atStartOfDay();
		}
		long nanoOfDay = Math.round(fraction * 86_400_000_000_000L);
		nanoOfDay = Math.max(0, Math.min(86_399_999_999_999L, nanoOfDay));
		return date.atTime(LocalTime.ofNanoOfDay(nanoOfDay));
	}

	/**
	 * 判斷輸入字串是否為十進位數字 (可含可選符號與小數點)。
	 *
	 * @param str 待檢驗字串
	 * @return 若為數值格式則為 {@code true}
	 */
	private static boolean isNumeric(String str) {
		if (str == null || str.isEmpty()) {
			return false;
		}
		int start = 0;
		if (str.charAt(0) == '-' || str.charAt(0) == '+') {
			start = 1;
		}
		boolean hasDot = false;
		boolean hasDigit = false;
		for (int i = start; i < str.length(); i++) {
			char c = str.charAt(i);
			if (c >= '0' && c <= '9') {
				hasDigit = true;
			} else if (c == '.' && !hasDot) {
				hasDot = true;
			} else {
				return false;
			}
		}
		return hasDigit;
	}

	/**
	 * 取得 Java 原生基本型別之預設安全初值。
	 *
	 * @param type 基本型別 Class
	 * @return 預設初值 (如 int 回傳 0, boolean 回傳 false)
	 */
	private static Object getPrimitiveDefault(Class<?> type) {
		if (type == int.class) return 0;
		if (type == long.class) return 0L;
		if (type == double.class) return 0.0d;
		if (type == float.class) return 0.0f;
		if (type == boolean.class) return false;
		if (type == short.class) return (short) 0;
		if (type == byte.class) return (byte) 0;
		if (type == char.class) return '\0';
		return null;
	}

	// =========================================================================
	// 6. 模型元數據剖析與快取 (Metadata Parsing & Reflection Cache)
	// =========================================================================

	/**
	 * 取得模型元數據 (若快取無記錄則自動剖析並快取)。
	 *
	 * @param modelClass 模型類別 Class
	 * @return 已剖析完畢之 {@link ModelMetadata}
	 */
	private static ModelMetadata getMetadata(Class<?> modelClass) {
		return METADATA_CACHE.computeIfAbsent(modelClass, ExcelBeanAssembler::buildMetadata);
	}

	/**
	 * 深入剖析模型類別結構並建構元數據物件。
	 * <p>
	 * <b>剖析流程：</b>
	 * <ol>
	 *   <li>讀取類別層級 {@link ExcelSheet} 決定工作表名稱。</li>
	 *   <li>判斷是否為 Java 21 Record (透過 {@link Class#isRecord()})。</li>
	 *   <li>若為 Record：讀取 {@link RecordComponent}，尋找 {@link ExcelColumn} 註解並取得 Accessor 方法與標準建構子。</li>
	 *   <li>若為 JavaBean：讀取無參建構子與所有宣告欄位 (包含父類別)，尋找 {@link ExcelColumn} 註解。</li>
	 *   <li>排序欄位：若有宣告 {@code explicitIndex >= 0} 則優先按其排序，否則依類別宣告自然順序排序。</li>
	 * </ol>
	 * </p>
	 *
	 * @param clazz 模型類別 Class
	 * @return 完整解析後之 {@link ModelMetadata}
	 */
	private static ModelMetadata buildMetadata(Class<?> clazz) {
		ExcelSheet sheetAnno = clazz.getAnnotation(ExcelSheet.class);
		String sheetName = clazz.getSimpleName();
		String[] sheetAliases = new String[0];
		SheetProtectionConfig protectionConfig = null;
		SheetHeaderFooterConfig headerFooterConfig = null;

		if (sheetAnno != null) {
			if (!sheetAnno.value().isBlank()) {
				sheetName = sheetAnno.value().trim();
			} else if (!sheetAnno.name().isBlank()) {
				sheetName = sheetAnno.name().trim();
			}
			if (sheetAnno.aliases() != null && sheetAnno.aliases().length > 0) {
				sheetAliases = sheetAnno.aliases();
			}

			if (sheetAnno.protect() || !sheetAnno.protectPassword().isBlank()) {
				protectionConfig = SheetProtectionConfig.ofPassword(sheetAnno.protectPassword());
			}

			boolean hasHeader = !sheetAnno.leftHeader().isBlank() || !sheetAnno.centerHeader().isBlank() || !sheetAnno.rightHeader().isBlank();
			boolean hasFooter = !sheetAnno.leftFooter().isBlank() || !sheetAnno.centerFooter().isBlank() || !sheetAnno.rightFooter().isBlank();
			if (hasHeader || hasFooter) {
				headerFooterConfig = new SheetHeaderFooterConfig(
						sheetAnno.leftHeader().isBlank() ? null : sheetAnno.leftHeader(),
						sheetAnno.centerHeader().isBlank() ? null : sheetAnno.centerHeader(),
						sheetAnno.rightHeader().isBlank() ? null : sheetAnno.rightHeader(),
						sheetAnno.leftFooter().isBlank() ? null : sheetAnno.leftFooter(),
						sheetAnno.centerFooter().isBlank() ? null : sheetAnno.centerFooter(),
						sheetAnno.rightFooter().isBlank() ? null : sheetAnno.rightFooter()
				);
			}
		}

		boolean isRecord = clazz.isRecord();
		List<ColumnMetadata> columns = new ArrayList<>();
		Constructor<?> canonicalConstructor = null;
		Constructor<?> noArgConstructor = null;

		if (isRecord) {
			// ==================== Java 21 Record 剖析流程 ====================
			RecordComponent[] components = clazz.getRecordComponents();
			Class<?>[] paramTypes = new Class<?>[components.length];
			for (int i = 0; i < components.length; i++) {
				RecordComponent rc = components[i];
				paramTypes[i] = rc.getType();
				ExcelColumn colAnno = rc.getAnnotation(ExcelColumn.class);
				if (colAnno == null) {
					try {
						Field f = clazz.getDeclaredField(rc.getName());
						colAnno = f.getAnnotation(ExcelColumn.class);
					} catch (NoSuchFieldException ignored) {
					}
				}
				if (colAnno != null && colAnno.ignore()) {
					continue;
				}

				String colName = rc.getName();
				int explicitIndex = -1;
				String format = "";
				String defaultValue = "";
				String formula = "";
				String[] aliases = new String[0];

				if (colAnno != null) {
					if (!colAnno.value().isBlank()) colName = colAnno.value().trim();
					else if (!colAnno.name().isBlank()) colName = colAnno.name().trim();
					explicitIndex = colAnno.index();
					format = colAnno.format().trim();
					defaultValue = colAnno.defaultValue().trim();
					formula = colAnno.formula().trim();
					if (colAnno.aliases() != null && colAnno.aliases().length > 0) {
						aliases = colAnno.aliases();
					}
				}

				Method accessor = rc.getAccessor();
				accessor.setAccessible(true);
				columns.add(new ColumnMetadata(colName, rc.getName(), rc.getType(), explicitIndex, i, format, defaultValue,
						formula, aliases, accessor, null, null));
			}

			try {
				canonicalConstructor = clazz.getDeclaredConstructor(paramTypes);
				canonicalConstructor.setAccessible(true);
			} catch (NoSuchMethodException e) {
				throw new IllegalStateException("無法解析 Record 的標準建構子: " + clazz.getName(), e);
			}
		} else {
			// ==================== 標準 JavaBean POJO 剖析流程 ====================
			try {
				noArgConstructor = clazz.getDeclaredConstructor();
				noArgConstructor.setAccessible(true);
			} catch (NoSuchMethodException e) {
				throw new IllegalStateException("Excel 模型類別必須具備公開無參建構子: " + clazz.getName(), e);
			}

			List<Field> fields = getAllDeclaredFields(clazz);
			int naturalOrder = 0;
			for (Field field : fields) {
				if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) {
					continue;
				}

				ExcelColumn colAnno = field.getAnnotation(ExcelColumn.class);
				if (colAnno != null && colAnno.ignore()) {
					continue;
				}

				String colName = field.getName();
				int explicitIndex = -1;
				String format = "";
				String defaultValue = "";
				String formula = "";
				String[] aliases = new String[0];

				if (colAnno != null) {
					if (!colAnno.value().isBlank()) colName = colAnno.value().trim();
					else if (!colAnno.name().isBlank()) colName = colAnno.name().trim();
					explicitIndex = colAnno.index();
					format = colAnno.format().trim();
					defaultValue = colAnno.defaultValue().trim();
					formula = colAnno.formula().trim();
					if (colAnno.aliases() != null && colAnno.aliases().length > 0) {
						aliases = colAnno.aliases();
					}
				}

				field.setAccessible(true);
				columns.add(new ColumnMetadata(colName, field.getName(), field.getType(), explicitIndex, naturalOrder++,
						format, defaultValue, formula, aliases, null, null, field));
			}
		}

		// 排序欄位：若有 explicitIndex >= 0 依其排序，否則依宣告順序排序
		columns.sort(Comparator.comparingInt((ColumnMetadata c) -> c.explicitIndex() >= 0 ? c.explicitIndex() : (10000 + c.naturalOrder())));

		return new ModelMetadata(sheetName, sheetAliases, isRecord, columns, canonicalConstructor, noArgConstructor, protectionConfig, headerFooterConfig);
	}

	/**
	 * 遞迴獲取類別及其所有父類別所宣告之所有屬性欄位。
	 *
	 * @param clazz 目標類別
	 * @return 欄位清單
	 */
	private static List<Field> getAllDeclaredFields(Class<?> clazz) {
		List<Field> fields = new ArrayList<>();
		Class<?> current = clazz;
		while (current != null && current != Object.class) {
			Field[] declared = current.getDeclaredFields();
			Collections.addAll(fields, declared);
			current = current.getSuperclass();
		}
		return fields;
	}

	// =========================================================================
	// 7. 內部記錄元數據 Record (Internal Metadata Structures)
	// =========================================================================

	/**
	 * 模型層級元數據記錄
	 *
	 * @param sheetName            預設工作表名稱
	 * @param sheetAliases         工作表相容別名清單
	 * @param isRecord             是否為 Java 21 Record
	 * @param columns              欄位元數據排序清單
	 * @param canonicalConstructor 若為 Record 時的標準建構子
	 * @param noArgConstructor     若為 POJO 時的無參建構子
	 * @param protectionConfig     工作表安全防護設定 (可為 null)
	 * @param headerFooterConfig   列印頁首頁尾設定 (可為 null)
	 */
	private record ModelMetadata(
			String sheetName,
			String[] sheetAliases,
			boolean isRecord,
			List<ColumnMetadata> columns,
			Constructor<?> canonicalConstructor,
			Constructor<?> noArgConstructor,
			SheetProtectionConfig protectionConfig,
			SheetHeaderFooterConfig headerFooterConfig
	) {}

	/**
	 * 欄位層級元數據記錄
	 *
	 * @param columnName    Excel 表頭對外名稱
	 * @param fieldName     Java 屬性欄位名稱
	 * @param type          屬性型別 Class
	 * @param explicitIndex 註解明確指定之絕對索引 (-1 代表未指定)
	 * @param naturalOrder  類別中宣告的自然先後順序
	 * @param format        數值或日期自訂格式化字串
	 * @param defaultValue  為空時之預設值字串
	 * @param formula       宣告式動態計算公式模板 (如 "C{row}*D{row}")
	 * @param aliases       容錯別名陣列
	 * @param accessor      Record 專用屬性存取方法 (Getter)
	 * @param setter        JavaBean 屬性設定方法 (可選)
	 * @param field         JavaBean 直接欄位反射參考
	 */
	private record ColumnMetadata(
			String columnName,
			String fieldName,
			Class<?> type,
			int explicitIndex,
			int naturalOrder,
			String format,
			String defaultValue,
			String formula,
			String[] aliases,
			Method accessor,
			Method setter,
			Field field
	) {
		/**
		 * 從目標物件實體中讀取該欄位數值。
		 *
		 * @param target 目標物件實例
		 * @return 欄位值
		 */
		public Object getValue(Object target) {
			try {
				if (accessor != null) {
					return accessor.invoke(target);
				}
				if (field != null) {
					return field.get(target);
				}
			} catch (Exception e) {
				throw new IllegalStateException("無法從欄位讀取數值 [" + fieldName + "]: " + e.getMessage(), e);
			}
			return null;
		}

		/**
		 * 將數值寫入目標物件實體中。
		 *
		 * @param target 目標物件實例
		 * @param value  欲寫入之屬性值
		 */
		public void setValue(Object target, Object value) {
			try {
				if (setter != null) {
					setter.invoke(target, value);
				} else if (field != null) {
					field.set(target, value);
				}
			} catch (Exception e) {
				throw new IllegalStateException("無法設定數值至欄位 [" + fieldName + "]: " + e.getMessage(), e);
			}
		}
	}
}
