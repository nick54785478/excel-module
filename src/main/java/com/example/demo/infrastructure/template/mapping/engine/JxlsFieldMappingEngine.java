package com.example.demo.infrastructure.template.mapping.engine;

import com.example.demo.application.shared.mapping.FieldMappingRule;
import com.example.demo.application.shared.mapping.TemplateMappingDefinition;
import com.example.demo.infrastructure.template.mapping.converter.FieldConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JXLS 欄位動態映射核心引擎 (Field Mapping Engine)。
 * <p>
 * 遵循 Clean Architecture 規格，座落於基礎設施層內部。
 * 負責執行：
 * <ol>
 *   <li>屬性導航與反射快取解析 (支援巢狀 SpEL / Property Path)。</li>
 *   <li>Domain Value Object 智慧自動開箱 ({@link ValueObjectSmartUnboxer})。</li>
 *   <li>外掛式格式轉換策略調度 ({@link FieldConverter})。</li>
 *   <li>跨欄位虛擬運算求值 (SpEL Virtual Expressions)。</li>
 *   <li>預設值保底與 JXLS 上下文資料組裝。</li>
 * </ol>
 * 本類別為純無狀態設計，所有快取皆為唯讀執行緒安全。
 * </p>
 */
@Component
public class JxlsFieldMappingEngine {

    private static final Logger log = LoggerFactory.getLogger(JxlsFieldMappingEngine.class);

    private final Map<String, FieldConverter> converterRegistry = new ConcurrentHashMap<>();
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final TemplateParserContext templateParserContext = new TemplateParserContext();

    // 巢狀屬性反射 Accessor 快取: Class -> (property -> Method)
    private final Map<Class<?>, Map<String, Method>> propertyAccessorCache = new ConcurrentHashMap<>();
    // SpEL 編譯表達式快取
    private final Map<String, Expression> expressionCache = new ConcurrentHashMap<>();

    /**
     * 建構函式：注入 Spring 容器中所有可用之 {@link FieldConverter} 轉換策略。
     *
     * @param converters 格式轉換策略清單
     */
    public JxlsFieldMappingEngine(List<FieldConverter> converters) {
        if (converters != null) {
            for (FieldConverter converter : converters) {
                if (converter != null && converter.getConverterKey() != null) {
                    this.converterRegistry.put(converter.getConverterKey().toUpperCase(), converter);
                }
            }
        }
    }

    /**
     * 將業務資料來源動態映射並轉換為 JXLS 範本填充上下文 Map。
     *
     * @param sourceData 業務實體、聚合根或其集合清單
     * @param mappingDefinition 範本欄位映射規格定義
     * @return 供 JXLS 渲染直接消費之資料模型 Map
     */
    public Map<String, Object> mapToContext(Object sourceData, TemplateMappingDefinition mappingDefinition) {
        Objects.requireNonNull(mappingDefinition, "mappingDefinition 不得為空");

        log.debug("開始以 JxlsFieldMappingEngine 執行欄位映射，範本鍵: {}", mappingDefinition.templateKey());

        List<?> items = toList(sourceData);
        List<Map<String, Object>> mappedRows = new ArrayList<>(items.size());

        for (Object item : items) {
            if (item == null) {
                continue;
            }
            Map<String, Object> rowMap = mapSingleItem(item, mappingDefinition);
            mappedRows.add(rowMap);
        }

        // 組裝全域 JXLS 上下文
        Map<String, Object> rootContext = new HashMap<>(mappingDefinition.globalMeta());
        rootContext.put(mappingDefinition.rootCollectionVar(), mappedRows);

        return rootContext;
    }

    private Map<String, Object> mapSingleItem(Object item, TemplateMappingDefinition def) {
        Map<String, Object> rowMap = new LinkedHashMap<>();
        StandardEvaluationContext spelContext = null;

        for (FieldMappingRule rule : def.rules()) {
            Object rawValue = null;

            // 1. 若有定義虛擬表達式，優先求值
            if (rule.virtualExpression() != null && !rule.virtualExpression().isBlank()) {
                if (spelContext == null) {
                    spelContext = initSpelContext(item, def.itemAlias());
                }
                rawValue = evaluateVirtualExpression(rule.virtualExpression(), spelContext);
            }
            // 2. 否則依據 sourcePath 導航提取屬性值
            else if (rule.sourcePath() != null && !rule.sourcePath().isBlank()) {
                rawValue = resolveNestedProperty(item, rule.sourcePath());
            }

            // 3. Value Object 智慧自動開箱 (例如 MaterialCode -> String, Money -> BigDecimal)
            Object unboxedValue = ValueObjectSmartUnboxer.unbox(rawValue);

            // 4. 執行 FieldConverter 格式轉換
            Object convertedValue = unboxedValue;
            if (rule.converterKey() != null && !rule.converterKey().isBlank()) {
                FieldConverter converter = converterRegistry.get(rule.converterKey().toUpperCase());
                if (converter != null) {
                    convertedValue = converter.convert(unboxedValue, rule.formatPattern());
                } else {
                    log.warn("找不到指定的 FieldConverter: {}", rule.converterKey());
                }
            }

            // 5. 預設值保底機制
            if (convertedValue == null && rule.defaultValue() != null) {
                convertedValue = rule.defaultValue();
            }

            rowMap.put(rule.targetKey(), convertedValue);
        }

        return rowMap;
    }

    /**
     * 導航解析巢狀屬性 (例如 "code.value" 或 "standardCost.amount")。
     */
    private Object resolveNestedProperty(Object target, String path) {
        if (target == null || path == null || path.isBlank()) {
            return null;
        }

        String[] tokens = path.split("\\.");
        Object current = target;

        for (String token : tokens) {
            if (current == null) {
                return null;
            }
            current = getProperty(current, token.trim());
        }

        return current;
    }

    private Object getProperty(Object obj, String propertyName) {
        if (obj == null) {
            return null;
        }

        // 若當前物件本身即為 Map
        if (obj instanceof Map<?, ?> map) {
            return map.get(propertyName);
        }

        Class<?> clazz = obj.getClass();
        Method accessor = propertyAccessorCache
            .computeIfAbsent(clazz, k -> new ConcurrentHashMap<>())
            .computeIfAbsent(propertyName, prop -> findAccessorMethod(clazz, prop));

        if (accessor != null) {
            try {
                return accessor.invoke(obj);
            } catch (Exception e) {
                log.trace("屬性反射讀取失敗 [{}]: {}", propertyName, e.getMessage());
            }
        }

        return null;
    }

    private Method findAccessorMethod(Class<?> clazz, String propertyName) {
        // 1. 若為 Record 尋找組件 accessor
        if (clazz.isRecord()) {
            for (RecordComponent rc : clazz.getRecordComponents()) {
                if (rc.getName().equalsIgnoreCase(propertyName)) {
                    return rc.getAccessor();
                }
            }
        }

        // 2. 尋找 getXxx() / isXxx() 或直接同名無參方法
        String capitalized = Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
        String[] candidateNames = {
            propertyName,
            "get" + capitalized,
            "is" + capitalized
        };

        for (String name : candidateNames) {
            try {
                Method method = clazz.getMethod(name);
                if (method.getParameterCount() == 0 && method.getReturnType() != void.class) {
                    return method;
                }
            } catch (NoSuchMethodException ignored) {
            }
        }

        return null;
    }

    private StandardEvaluationContext initSpelContext(Object item, String alias) {
        StandardEvaluationContext ctx = new StandardEvaluationContext(item);
        ctx.setVariable("item", item);
        if (alias != null && !alias.isBlank()) {
            ctx.setVariable(alias, item);
        }
        ctx.addPropertyAccessor(new AliasPropertyAccessor(alias, item));
        ctx.addPropertyAccessor(new org.springframework.expression.spel.support.MapAccessor());
        return ctx;
    }

    private Object evaluateVirtualExpression(String expressionString, StandardEvaluationContext ctx) {
        try {
            String cleanExpr = expressionString.trim();
            if (cleanExpr.contains("#{")) {
                if (cleanExpr.contains("+") || cleanExpr.contains("-") || cleanExpr.contains("*") || cleanExpr.contains("/")) {
                    cleanExpr = cleanExpr.replace("#{", "").replace("}", "");
                    Expression expression = expressionCache.computeIfAbsent(cleanExpr, expressionParser::parseExpression);
                    return expression.getValue(ctx);
                } else {
                    Expression expression = expressionCache.computeIfAbsent(cleanExpr, expr -> expressionParser.parseExpression(expr, templateParserContext));
                    return expression.getValue(ctx);
                }
            } else {
                Expression expression = expressionCache.computeIfAbsent(cleanExpr, expressionParser::parseExpression);
                return expression.getValue(ctx);
            }
        } catch (Exception e) {
            log.warn("虛擬欄位表達式求值失敗 [{}]: {}", expressionString, e.getMessage());
            return null;
        }
    }

    private static class AliasPropertyAccessor implements PropertyAccessor {
        private final String alias;
        private final Object item;

        public AliasPropertyAccessor(String alias, Object item) {
            this.alias = alias;
            this.item = item;
        }

        @Override
        public Class<?>[] getSpecificTargetClasses() {
            return null;
        }

        @Override
        public boolean canRead(EvaluationContext context, Object target, String name) {
            return (alias != null && alias.equalsIgnoreCase(name)) || "item".equalsIgnoreCase(name);
        }

        @Override
        public TypedValue read(EvaluationContext context, Object target, String name) {
            return new TypedValue(item);
        }

        @Override
        public boolean canWrite(EvaluationContext context, Object target, String name) {
            return false;
        }

        @Override
        public void write(EvaluationContext context, Object target, String name, Object newValue) {
        }
    }

    private List<?> toList(Object sourceData) {
        if (sourceData == null) {
            return Collections.emptyList();
        }
        if (sourceData instanceof List<?> list) {
            return list;
        }
        if (sourceData instanceof Collection<?> collection) {
            return new ArrayList<>(collection);
        }
        if (sourceData.getClass().isArray()) {
            int length = Array.getLength(sourceData);
            List<Object> list = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                list.add(Array.get(sourceData, i));
            }
            return list;
        }
        // 單一實體包裝為單元素清單
        return List.of(sourceData);
    }
}

