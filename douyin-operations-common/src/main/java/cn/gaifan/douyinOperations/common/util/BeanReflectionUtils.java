package cn.gaifan.douyinOperations.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.FatalBeanException;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bean 反射工具类
 * 负责利用反射获取和操作Bean的属性和方法
 *
 * @author gaifan
 */
@Slf4j
public class BeanReflectionUtils {

    /**
     * 需要过滤的属性名集合
     */
    private static final Set<String> FILTERED_PROPERTIES = new HashSet<>(java.util.Arrays.asList("class"));

    /**
     * PropertyDescriptor 缓存
     */
    private static final Map<Class<?>, PropertyDescriptor[]> PROPERTY_DESCRIPTOR_CACHE = new ConcurrentHashMap<>();

    /**
     * BeanInfo 缓存
     */
    private static final Map<Class<?>, BeanInfo> BEAN_INFO_CACHE = new ConcurrentHashMap<>();

    /**
     * Field 信息缓存
     */
    private static final Map<Class<?>, Map<String, Field>> FIELD_CACHE = new ConcurrentHashMap<>();

    /**
     * 获取缓存的 PropertyDescriptor 数组
     *
     * @param clazz 类对象
     * @return PropertyDescriptor 数组
     */
    public static PropertyDescriptor[] getCachedPropertyDescriptors(Class<?> clazz) {
        return PROPERTY_DESCRIPTOR_CACHE.computeIfAbsent(clazz, BeanReflectionUtils::getPropertyDescriptors);
    }

    /**
     * 获取属性描述符数组
     *
     * @param clazz 类对象
     * @return PropertyDescriptor 数组
     */
    private static PropertyDescriptor[] getPropertyDescriptors(Class<?> clazz) {
        BeanInfo beanInfo = getCachedBeanInfo(clazz);
        return beanInfo.getPropertyDescriptors();
    }

    /**
     * 获取属性描述符
     *
     * @param clazz 类对象
     * @param propertyName 属性名
     * @return PropertyDescriptor 对象，不存在返回null
     */
    public static PropertyDescriptor getPropertyDescriptor(Class<?> clazz, String propertyName) {
        PropertyDescriptor[] pds = getCachedPropertyDescriptors(clazz);
        for (PropertyDescriptor pd : pds) {
            if (propertyName.equals(pd.getName())) {
                return pd;
            }
        }
        return null;
    }

    /**
     * 获取缓存的 BeanInfo
     *
     * @param clazz 类对象
     * @return BeanInfo 对象
     */
    private static BeanInfo getCachedBeanInfo(Class<?> clazz) {
        try {
            return BEAN_INFO_CACHE.computeIfAbsent(clazz, c -> {
                try {
                    return Introspector.getBeanInfo(c);
                } catch (Exception e) {
                    log.error("Failed to get BeanInfo for class [{}]: {}", c.getName(), e.getMessage());
                    throw new FatalBeanException("Failed to get BeanInfo for class: " + c.getName(), e);
                }
            });
        } catch (FatalBeanException e) {
            throw e;
        } catch (Exception e) {
            throw new FatalBeanException("Failed to get BeanInfo for class: " + clazz.getName(), e);
        }
    }

    /**
     * 获取缓存的字段映射
     *
     * @param clazz 类对象
     * @return 字段名到Field对象的映射
     */
    public static Map<String, Field> getCachedFields(Class<?> clazz) {
        return FIELD_CACHE.computeIfAbsent(clazz, c -> {
            Map<String, Field> fieldMap = new HashMap<>();
            Field[] fields = c.getDeclaredFields();
            for (Field field : fields) {
                if (!FILTERED_PROPERTIES.contains(field.getName())) {
                    fieldMap.put(field.getName(), field);
                }
            }
            return fieldMap;
        });
    }

    /**
     * 类型转换
     *
     * @param value 待转换的值
     * @param targetType 目标类型
     * @return 转换后的值，如果无法转换返回null
     */
    public static Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }

        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }

        if (targetType.isPrimitive()) {
            return convertToPrimitive(value, targetType);
        }

        if (targetType == String.class) {
            return value.toString();
        }

        if (Number.class.isAssignableFrom(value.getClass()) && Number.class.isAssignableFrom(targetType)) {
            return convertNumber((Number) value, targetType);
        }

        return null;
    }

    /**
     * 转换为基本类型
     *
     * @param value 值对象
     * @param targetType 目标基本类型
     * @return 转换后的值
     */
    private static Object convertToPrimitive(Object value, Class<?> targetType) {
        String typeName = targetType.getName();
        try {
            if ("int".equals(typeName)) {
                return Integer.parseInt(value.toString());
            } else if ("long".equals(typeName)) {
                return Long.parseLong(value.toString());
            } else if ("double".equals(typeName)) {
                return Double.parseDouble(value.toString());
            } else if ("float".equals(typeName)) {
                return Float.parseFloat(value.toString());
            } else if ("boolean".equals(typeName)) {
                return Boolean.parseBoolean(value.toString());
            } else if ("byte".equals(typeName)) {
                return Byte.parseByte(value.toString());
            } else if ("short".equals(typeName)) {
                return Short.parseShort(value.toString());
            } else if ("char".equals(typeName)) {
                String str = value.toString();
                return str.isEmpty() ? '\0' : str.charAt(0);
            }
        } catch (Exception e) {
            log.debug("Failed to convert [{}] to primitive type [{}]: {}", value, typeName, e.getMessage());
            return null;
        }
        return null;
    }

    /**
     * 数字类型转换
     *
     * @param value 数字值
     * @param targetType 目标类型
     * @return 转换后的值
     */
    private static Object convertNumber(Number value, Class<?> targetType) {
        if (targetType == Integer.class || targetType == int.class) {
            return value.intValue();
        } else if (targetType == Long.class || targetType == long.class) {
            return value.longValue();
        } else if (targetType == Double.class || targetType == double.class) {
            return value.doubleValue();
        } else if (targetType == Float.class || targetType == float.class) {
            return value.floatValue();
        } else if (targetType == Short.class || targetType == short.class) {
            return value.shortValue();
        } else if (targetType == Byte.class || targetType == byte.class) {
            return value.byteValue();
        }
        return null;
    }
}
