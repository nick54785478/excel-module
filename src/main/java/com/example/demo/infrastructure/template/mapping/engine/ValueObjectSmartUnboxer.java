package com.example.demo.infrastructure.template.mapping.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 領域實質物件智慧自動開箱器 (Value Object Smart Unboxer)。
 * <p>
 * 針對 DDD 領域層封裝型 Value Object（例如 {@code MaterialCode(value)}, {@code Money(amount, currency)},
 * {@code Quantity(value, unit)}, {@code BomVersion(value)} 等），
 * 自動探測並提取其核心業務基本型別值，避免使用者或範本必須手寫深層路徑。
 * </p>
 */
public final class ValueObjectSmartUnboxer {

    private static final Logger log = LoggerFactory.getLogger(ValueObjectSmartUnboxer.class);

    private static final Map<Class<?>, Method> UNBOX_METHOD_CACHE = new ConcurrentHashMap<>();

    private ValueObjectSmartUnboxer() {
        // 私有建構函式
    }

    /**
     * 嘗試對輸入物件進行智慧開箱。
     *
     * @param target 待開箱物件
     * @return 開箱後之基本型別值或原始物件
     */
    public static Object unbox(Object target) {
        if (target == null) {
            return null;
        }

        Class<?> clazz = target.getClass();

        // 原生包裝型別、字串或常用時間直接回傳
        if (isPrimitiveOrCommon(clazz)) {
            return target;
        }

        Method unboxMethod = UNBOX_METHOD_CACHE.computeIfAbsent(clazz, ValueObjectSmartUnboxer::findUnboxMethod);
        if (unboxMethod != null) {
            try {
                return unboxMethod.invoke(target);
            } catch (Exception e) {
                log.trace("ValueObjectSmartUnboxer 開箱調用失敗: {}", e.getMessage());
            }
        }

        return target;
    }

    private static Method findUnboxMethod(Class<?> clazz) {
        // 1. 若為 Java Record 優先檢查通用屬性名稱
        if (clazz.isRecord()) {
            RecordComponent[] components = clazz.getRecordComponents();
            if (components != null && components.length > 0) {
                // 優先尋找 amount (如 Money)
                for (RecordComponent rc : components) {
                    if ("amount".equals(rc.getName())) {
                        return rc.getAccessor();
                    }
                }
                // 尋找 value (如 MaterialCode, BomId, Quantity, ScrapRate)
                for (RecordComponent rc : components) {
                    if ("value".equals(rc.getName())) {
                        return rc.getAccessor();
                    }
                }
                // 尋找 name (如 UnitOfMeasure)
                for (RecordComponent rc : components) {
                    if ("name".equals(rc.getName())) {
                        return rc.getAccessor();
                    }
                }
                // 若只有單一組件，直接開箱該組件
                if (components.length == 1) {
                    return components[0].getAccessor();
                }
            }
        }

        // 2. 尋找公有無參 value(), amount(), getValue(), getAmount() 方法
        String[] candidateMethodNames = {"value", "amount", "getValue", "getAmount"};
        for (String methodName : candidateMethodNames) {
            try {
                Method method = clazz.getMethod(methodName);
                if (method.getParameterCount() == 0 && method.getReturnType() != void.class) {
                    return method;
                }
            } catch (NoSuchMethodException ignored) {
                // 忽略無此方法
            }
        }

        return null;
    }

    private static boolean isPrimitiveOrCommon(Class<?> clazz) {
        return clazz.isPrimitive()
            || clazz.getName().startsWith("java.lang.")
            || clazz.getName().startsWith("java.math.")
            || clazz.getName().startsWith("java.time.")
            || clazz.isEnum();
    }
}

