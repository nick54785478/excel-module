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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import com.example.demo.application.annotation.ExcelColumn;
import com.example.demo.application.annotation.ExcelSheet;
import com.example.demo.application.shared.command.WriteExcelCommand;
import com.example.demo.application.shared.command.WriteSheetCommand;
import com.example.demo.application.shared.dto.ExcelGottenResult;
import com.example.demo.application.shared.dto.ExcelSheetGottenResult;

/**
 * Excel 宣告式物件與純資料載體組裝器 (Excel Declarative Bean & Record Assembler)
 * <p>
 * 遵循 Clean Architecture 規範，座落於應用層組裝器套件 ({@code application/assembler/})，
 * 負責在純資料載體 (Excel 活頁簿命令、讀取結果、Map 集合) 與強型別 Java Model 物件 (POJO 或 Java 21 Record) 之間進行雙向轉換。
 * <p>
 * 核心特性：
 * <ul>
 *   <li><b>原生支援 Java 21 Record</b>：自動探測標準建構子 (Canonical Constructor) 與 Record 元件，保證不可變模型的優雅綁定。</li>
 *   <li><b>高效能快取機制</b>：以 {@link ConcurrentHashMap} 快取 Model 類別的反射元數據與屬性存取器，消除原生反射的重複剖析開銷。</li>
 *   <li><b>多維度欄位匹配</b>：支援依照 {@link ExcelColumn} 指定的表頭名稱 (Header Name) 或索引值 (Column Index) 雙重定位。</li>
 *   <li><b>豐富型別安全轉換</b>：涵蓋常見之數值、浮點數、高精度 {@link BigDecimal}、日期時間 (Java 8 Time 與 legacy Date) 及枚舉 (Enum)。</li>
 *   <li><b>雙向相容 Map 結構</b>：完整支援 Bean $\leftrightarrow$ {@code Map<String, Object>} 互相轉換，滿足靈活與強型別雙軌需求。</li>
 * </ul>
 */
public final class ExcelBeanAssembler {

	private static final Map<Class<?>, ModelMetadata> METADATA_CACHE = new ConcurrentHashMap<>();
	private static final DateTimeFormatter DEFAULT_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final DateTimeFormatter DEFAULT_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	private ExcelBeanAssembler() {
		// 工具類禁止實例化
	}

	// =========================================================================
	// 1. Model $\to$ WriteExcelCommand / WriteSheetCommand
	// =========================================================================

	/**
	 * 將強型別 Model 清單組裝為 Excel 活頁簿寫入命令 (單一工作表)。
	 *
	 * @param <T>        模型型別
	 * @param dataList   資料物件清單
	 * @param modelClass 模型類別 Class (支援 POJO 與 Record)
	 * @return Excel 寫入命令 {@link WriteExcelCommand}
	 */
	public static <T> WriteExcelCommand toWriteCommand(List<T> dataList, Class<T> modelClass) {
		return toWriteCommand(dataList, modelClass, null);
	}

	/**
	 * 將強型別 Model 清單組裝為 Excel 活頁簿寫入命令 (可指定工作表名稱覆蓋註解預設)。
	 *
	 * @param <T>             模型型別
	 * @param dataList        資料物件清單
	 * @param modelClass      模型類別 Class
	 * @param customSheetName 自訂工作表名稱 (若為 null 則套用註解或類別名稱)
	 * @return Excel 寫入命令 {@link WriteExcelCommand}
	 */
	public static <T> WriteExcelCommand toWriteCommand(List<T> dataList, Class<T> modelClass, String customSheetName) {
		WriteSheetCommand sheetCommand = toSheetCommand(dataList, modelClass, customSheetName);
		return WriteExcelCommand.of(sheetCommand);
	}

	/**
	 * 將強型別 Model 清單組裝為單一工作表寫入命令 {@link WriteSheetCommand}。
	 *
	 * @param <T>             模型型別
	 * @param dataList        資料物件清單
	 * @param modelClass      模型類別 Class
	 * @param customSheetName 自訂工作表名稱
	 * @return 工作表寫入命令 {@link WriteSheetCommand}
	 */
	public static <T> WriteSheetCommand toSheetCommand(List<T> dataList, Class<T> modelClass, String customSheetName) {
		Objects.requireNonNull(modelClass, "modelClass 不得為空");
		ModelMetadata metadata = getMetadata(modelClass);

		String sheetName = (customSheetName != null && !customSheetName.isBlank())
				? customSheetName.trim()
				: metadata.sheetName();

		List<String> headers = metadata.columns().stream()
				.map(ColumnMetadata::columnName)
				.toList();

		if (dataList == null || dataList.isEmpty()) {
			return WriteSheetCommand.of(sheetName, headers, Collections.emptyList());
		}

		List<List<Object>> rows = new ArrayList<>(dataList.size());
		for (T item : dataList) {
			if (item == null) {
				continue;
			}
			List<Object> row = new ArrayList<>(metadata.columns().size());
			for (ColumnMetadata col : metadata.columns()) {
				Object rawValue = col.getValue(item);
				Object formattedValue = formatValueForWrite(rawValue, col);
				row.add(formattedValue);
			}
			rows.add(row);
		}

		return WriteSheetCommand.of(sheetName, headers, rows);
	}

	/**
	 * 將資料字典清單 (Map List) 與標題清單組裝為 Excel 活頁簿寫入命令 {@link WriteExcelCommand}。
	 *
	 * @param sheetName 工作表名稱
	 * @param headers   標題列清單
	 * @param mapRows   資料字典清單 (Key 為 Header 名稱)
	 * @return Excel 寫入命令 {@link WriteExcelCommand}
	 */
	public static WriteExcelCommand toWriteCommandFromMaps(String sheetName, List<String> headers, List<Map<String, Object>> mapRows) {
		WriteSheetCommand sheet = toSheetCommandFromMaps(sheetName, headers, mapRows);
		return WriteExcelCommand.of(sheet);
	}

	/**
	 * 將資料字典清單 (Map List) 與標題清單組裝為工作表寫入命令 {@link WriteSheetCommand}。
	 *
	 * @param sheetName 工作表名稱
	 * @param headers   標題列清單
	 * @param mapRows   資料字典清單 (Key 為 Header 名稱)
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
	// 2. ExcelGottenResult / ExcelSheetGottenResult $\to$ List<T>
	// =========================================================================

	/**
	 * 將整份 Excel 讀取結果的第一個工作表解析並組裝為強型別 Model 清單。
	 *
	 * @param <T>        目標模型型別
	 * @param result     Excel 讀取結果物件
	 * @param modelClass 目標模型類別 Class
	 * @return 解析後之 Model 物件清單
	 */
	public static <T> List<T> toBeanList(ExcelGottenResult result, Class<T> modelClass) {
		Objects.requireNonNull(modelClass, "modelClass 不得為空");
		if (result == null || result.getFirstSheet().isEmpty()) {
			return Collections.emptyList();
		}
		return toBeanList(result.getFirstSheet().get(), modelClass);
	}

	/**
	 * 將特定工作表讀取結果解析並組裝為強型別 Model 清單。
	 *
	 * @param <T>         目標模型型別
	 * @param sheetResult 工作表讀取結果物件
	 * @param modelClass  目標模型類別 Class
	 * @return 解析後之 Model 物件清單
	 */
	public static <T> List<T> toBeanList(ExcelSheetGottenResult sheetResult, Class<T> modelClass) {
		Objects.requireNonNull(modelClass, "modelClass 不得為空");
		if (sheetResult == null || sheetResult.isEmpty()) {
			return Collections.emptyList();
		}

		ModelMetadata metadata = getMetadata(modelClass);
		List<String> sheetHeaders = sheetResult.getHeaders();
		List<List<Object>> dataRows = sheetResult.getRows();

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

	// =========================================================================
	// 3. Map $\leftrightarrow$ Bean 雙向組裝支援 (保留並相容 Map 方法)
	// =========================================================================

	/**
	 * 將以標題名稱為鍵的資料字典清單 (Map List) 轉換為強型別 Model 清單。
	 *
	 * @param <T>        目標模型型別
	 * @param mapList    資料字典清單 (Key: 標題欄位名, Value: 儲存格值)
	 * @param modelClass 目標模型類別 Class
	 * @return 強型別 Model 清單
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
	 *
	 * @param <T>        模型型別
	 * @param dataList   Model 物件清單
	 * @param modelClass 模型類別 Class
	 * @return 鍵值字典清單 (保留欄位順序之 LinkedHashMap)
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
	// 4. 核心反射與模型實例化引擎
	// =========================================================================

	@SuppressWarnings("unchecked")
	private static <T> T instantiateModel(ModelMetadata metadata, int[] colIndices, List<Object> row, Class<T> modelClass) {
		try {
			int colCount = metadata.columns().size();
			Object[] convertedValues = new Object[colCount];

			for (int i = 0; i < colCount; i++) {
				ColumnMetadata col = metadata.columns().get(i);
				int targetIdx = colIndices[i];
				Object rawVal = (targetIdx >= 0 && targetIdx < row.size()) ? row.get(targetIdx) : null;
				convertedValues[i] = convertValue(rawVal, col);
			}

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
	 * 計算模型屬性與 Excel 表頭之映射索引。
	 */
	private static int[] resolveColumnIndices(ModelMetadata metadata, List<String> sheetHeaders) {
		int colCount = metadata.columns().size();
		int[] indices = new int[colCount];

		for (int i = 0; i < colCount; i++) {
			ColumnMetadata col = metadata.columns().get(i);
			int matchedIndex = -1;

			// 優先依據標題文字比對 (忽略前後空格與大小寫)
			if (sheetHeaders != null && !sheetHeaders.isEmpty()) {
				for (int h = 0; h < sheetHeaders.size(); h++) {
					String header = sheetHeaders.get(h);
					if (header != null && (header.trim().equalsIgnoreCase(col.columnName().trim())
							|| (col.fieldName() != null && header.trim().equalsIgnoreCase(col.fieldName().trim())))) {
						matchedIndex = h;
						break;
					}
				}
			}

			// 若表頭無法比對，且註解有宣告 index >= 0，則退回以固定 index 提取
			if (matchedIndex == -1 && col.explicitIndex() >= 0) {
				matchedIndex = col.explicitIndex();
			} else if (matchedIndex == -1 && (sheetHeaders == null || sheetHeaders.isEmpty())) {
				matchedIndex = i;
			}

			indices[i] = matchedIndex;
		}
		return indices;
	}

	// =========================================================================
	// 5. 型別轉換與格式化處理
	// =========================================================================

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

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static Object convertValue(Object rawVal, ColumnMetadata col) {
		Class<?> targetType = col.type();

		if (rawVal == null || (rawVal instanceof String s && s.trim().isEmpty())) {
			if (col.defaultValue() != null && !col.defaultValue().isBlank()) {
				return parseStringToType(col.defaultValue().trim(), targetType, col.format());
			}
			if (targetType.isPrimitive()) {
				return getPrimitiveDefault(targetType);
			}
			return null;
		}

		if (targetType.isInstance(rawVal)) {
			return rawVal;
		}

		String strVal = String.valueOf(rawVal).trim();
		return parseStringToType(strVal, targetType, col.format());
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static Object parseStringToType(String str, Class<?> targetType, String format) {
		if (String.class.equals(targetType)) {
			return str;
		}

		try {
			if (Integer.class.equals(targetType) || int.class.equals(targetType)) {
				String clean = str.replace(",", "").replace("$", "").replace("¥", "").trim();
				return (int) Math.round(Double.parseDouble(clean));
			}
			if (Long.class.equals(targetType) || long.class.equals(targetType)) {
				String clean = str.replace(",", "").replace("$", "").replace("¥", "").trim();
				return Math.round(Double.parseDouble(clean));
			}
			if (Double.class.equals(targetType) || double.class.equals(targetType)) {
				String clean = str.replace(",", "").replace("$", "").replace("¥", "").trim();
				return Double.parseDouble(clean);
			}
			if (Float.class.equals(targetType) || float.class.equals(targetType)) {
				String clean = str.replace(",", "").replace("$", "").replace("¥", "").trim();
				return Float.parseFloat(clean);
			}
			if (BigDecimal.class.equals(targetType)) {
				String clean = str.replace(",", "").replace("$", "").replace("¥", "").trim();
				return new BigDecimal(clean);
			}
			if (BigInteger.class.equals(targetType)) {
				String clean = str.replace(",", "").replace("$", "").replace("¥", "").trim();
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

	private static LocalDate parseLocalDate(String str, String format) {
		if (format != null && !format.isBlank()) {
			return LocalDate.parse(str, DateTimeFormatter.ofPattern(format.trim()));
		}
		if (str.length() >= 10 && str.charAt(4) == '-' && str.charAt(7) == '-') {
			return LocalDate.parse(str.substring(0, 10), DEFAULT_DATE_FORMATTER);
		}
		if (str.length() >= 10 && str.charAt(4) == '/' && str.charAt(7) == '/') {
			return LocalDate.parse(str.substring(0, 10), DateTimeFormatter.ofPattern("yyyy/MM/dd"));
		}
		return LocalDate.parse(str, DEFAULT_DATE_FORMATTER);
	}

	private static LocalDateTime parseLocalDateTime(String str, String format) {
		if (format != null && !format.isBlank()) {
			return LocalDateTime.parse(str, DateTimeFormatter.ofPattern(format.trim()));
		}
		if (str.length() == 10) {
			return parseLocalDate(str, null).atStartOfDay();
		}
		if (str.contains("T")) {
			return LocalDateTime.parse(str);
		}
		if (str.length() >= 19 && str.charAt(4) == '-' && str.charAt(7) == '-') {
			return LocalDateTime.parse(str.substring(0, 19), DEFAULT_DATE_TIME_FORMATTER);
		}
		return LocalDateTime.parse(str, DEFAULT_DATE_TIME_FORMATTER);
	}

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
	// 6. 模型元數據剖析與快取
	// =========================================================================

	private static ModelMetadata getMetadata(Class<?> modelClass) {
		return METADATA_CACHE.computeIfAbsent(modelClass, ExcelBeanAssembler::buildMetadata);
	}

	private static ModelMetadata buildMetadata(Class<?> clazz) {
		ExcelSheet sheetAnno = clazz.getAnnotation(ExcelSheet.class);
		String sheetName = clazz.getSimpleName();
		if (sheetAnno != null) {
			if (!sheetAnno.value().isBlank()) {
				sheetName = sheetAnno.value().trim();
			} else if (!sheetAnno.name().isBlank()) {
				sheetName = sheetAnno.name().trim();
			}
		}

		boolean isRecord = clazz.isRecord();
		List<ColumnMetadata> columns = new ArrayList<>();
		Constructor<?> canonicalConstructor = null;
		Constructor<?> noArgConstructor = null;

		if (isRecord) {
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

				if (colAnno != null) {
					if (!colAnno.value().isBlank()) colName = colAnno.value().trim();
					else if (!colAnno.name().isBlank()) colName = colAnno.name().trim();
					explicitIndex = colAnno.index();
					format = colAnno.format().trim();
					defaultValue = colAnno.defaultValue().trim();
				}

				Method accessor = rc.getAccessor();
				accessor.setAccessible(true);
				columns.add(new ColumnMetadata(colName, rc.getName(), rc.getType(), explicitIndex, i, format, defaultValue,
						accessor, null, null));
			}

			try {
				canonicalConstructor = clazz.getDeclaredConstructor(paramTypes);
				canonicalConstructor.setAccessible(true);
			} catch (NoSuchMethodException e) {
				throw new IllegalStateException("無法解析 Record 的標準建構子: " + clazz.getName(), e);
			}
		} else {
			// 標準 JavaBean
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

				if (colAnno != null) {
					if (!colAnno.value().isBlank()) colName = colAnno.value().trim();
					else if (!colAnno.name().isBlank()) colName = colAnno.name().trim();
					explicitIndex = colAnno.index();
					format = colAnno.format().trim();
					defaultValue = colAnno.defaultValue().trim();
				}

				field.setAccessible(true);
				columns.add(new ColumnMetadata(colName, field.getName(), field.getType(), explicitIndex, naturalOrder++,
						format, defaultValue, null, null, field));
			}
		}

		// 排序欄位：若有 explicitIndex >= 0 依其排序，否則依宣告順序排序
		columns.sort(Comparator.comparingInt((ColumnMetadata c) -> c.explicitIndex() >= 0 ? c.explicitIndex() : (10000 + c.naturalOrder())));

		return new ModelMetadata(sheetName, isRecord, columns, canonicalConstructor, noArgConstructor);
	}

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
	// 7. 內部記錄元數據 Record
	// =========================================================================

	private record ModelMetadata(
			String sheetName,
			boolean isRecord,
			List<ColumnMetadata> columns,
			Constructor<?> canonicalConstructor,
			Constructor<?> noArgConstructor
	) {}

	private record ColumnMetadata(
			String columnName,
			String fieldName,
			Class<?> type,
			int explicitIndex,
			int naturalOrder,
			String format,
			String defaultValue,
			Method accessor,
			Method setter,
			Field field
	) {
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

