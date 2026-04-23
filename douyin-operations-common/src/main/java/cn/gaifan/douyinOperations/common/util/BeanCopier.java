package cn.gaifan.douyinOperations.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.util.Assert;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Bean 属性复制工具类
 * 负责Bean之间的属性复制和转换
 *
 * @author gaifan
 */
@Slf4j
public class BeanCopier {

    /**
     * 需要过滤的属性名集合
     */
    private static final Set<String> FILTERED_PROPERTIES = new HashSet<>(Arrays.asList("class"));

    /**
     * 复制非空属性
     *
     * @param source 源对象
     * @param target 目标对象
     * @throws BeansException Bean操作异常
     */
    public static void copyNotNullProperties(Object source, Object target) throws BeansException {
        copyNotNullProperties(source, target, null, null);
    }

    /**
     * 复制非空属性，忽略指定属性
     *
     * @param source 源对象
     * @param target 目标对象
     * @param ignoreProperties 忽略的属性名数组
     * @throws BeansException Bean操作异常
     */
    public static void copyNotNullProperties(Object source, Object target, String[] ignoreProperties)
            throws BeansException {
        copyNotNullProperties(source, target, null, ignoreProperties);
    }

    /**
     * 复制非空属性，限制可编辑类型
     *
     * @param source 源对象
     * @param target 目标对象
     * @param editable 可编辑的类型限制
     * @throws BeansException Bean操作异常
     */
    public static void copyNotNullProperties(Object source, Object target, Class<?> editable) throws BeansException {
        copyNotNullProperties(source, target, editable, null);
    }

    /**
     * 复制非空属性（核心实现方法）
     *
     * @param source 源对象
     * @param target 目标对象
     * @param editable 可编辑的类型限制
     * @param ignoreProperties 忽略的属性名数组
     * @throws BeansException Bean操作异常
     */
    private static void copyNotNullProperties(Object source, Object target, Class<?> editable,
                                              String[] ignoreProperties) throws BeansException {
        Assert.notNull(source, "Source must not be null");
        Assert.notNull(target, "Target must not be null");

        Class<?> actualEditable = target.getClass();
        if (editable != null) {
            if (!editable.isInstance(target)) {
                throw new IllegalArgumentException(
                        String.format("Target class [%s] not assignable to Editable class [%s]",
                                target.getClass().getName(), editable.getName()));
            }
            actualEditable = editable;
        }

        PropertyDescriptor[] targetPds = BeanReflectionUtils.getCachedPropertyDescriptors(actualEditable);
        Set<String> ignoreSet = ignoreProperties != null ? new HashSet<>(Arrays.asList(ignoreProperties)) : Collections.emptySet();

        for (PropertyDescriptor targetPd : targetPds) {
            String propertyName = targetPd.getName();

            if (FILTERED_PROPERTIES.contains(propertyName) || ignoreSet.contains(propertyName)) {
                continue;
            }

            Method writeMethod = targetPd.getWriteMethod();
            if (writeMethod == null) {
                continue;
            }

            PropertyDescriptor sourcePd = BeanReflectionUtils.getPropertyDescriptor(source.getClass(), propertyName);
            if (sourcePd == null || sourcePd.getReadMethod() == null) {
                continue;
            }

            try {
                Method readMethod = sourcePd.getReadMethod();
                setAccessibleIfNeeded(readMethod);
                Object value = readMethod.invoke(source);

                if (value == null) {
                    continue;
                }

                if (isEmptyCollection(value)) {
                    continue;
                }

                Object convertedValue = BeanReflectionUtils.convertValue(value, writeMethod.getParameterTypes()[0]);
                if (convertedValue == null) {
                    log.warn("Failed to convert value [{}] from {} to {} for property [{}]",
                            value, value.getClass().getSimpleName(),
                            writeMethod.getParameterTypes()[0].getSimpleName(), propertyName);
                    continue;
                }

                setAccessibleIfNeeded(writeMethod);
                writeMethod.invoke(target, convertedValue);
            } catch (IllegalArgumentException | ReflectiveOperationException e) {
                log.warn("Failed to copy property [{}] from {} to {}: {}",
                        propertyName, source.getClass().getSimpleName(),
                        target.getClass().getSimpleName(), e.getMessage());
            } catch (Throwable ex) {
                throw new FatalBeanException(
                        String.format("Could not copy properties from %s to %s",
                                source.getClass().getName(), target.getClass().getName()), ex);
            }
        }
    }

    /**
     * 设置方法可访问性
     *
     * @param method 方法对象
     */
    private static void setAccessibleIfNeeded(Method method) {
        if (!Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
            method.setAccessible(true);
        }
    }

    /**
     * 检查集合是否为空
     *
     * @param value 待检查的值
     * @return 如果集合为空返回true，否则返回false
     */
    private static boolean isEmptyCollection(Object value) {
        if (value == null) {
            return false;
        }

        if (value instanceof Collection) {
            return ((Collection<?>) value).isEmpty();
        }

        if (value instanceof Map) {
            return ((Map<?, ?>) value).isEmpty();
        }

        return false;
    }

    /**
     * 将Map转换为指定类型的Bean实例
     *
     * @param clazz 目标类型的Class对象
     * @param map Map对象
     * @param <T> 返回类型
     * @return 转换后的Bean实例
     * @throws FatalBeanException 转换失败时抛出异常
     */
    public static <T> T map2Bean(Class<T> clazz, Map<String, ?> map) throws FatalBeanException {
        if (map == null || clazz == null) {
            return null;
        }

        T entity;
        try {
            entity = clazz.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException e) {
            throw new FatalBeanException(
                    String.format("Class [%s] must have a no-args constructor", clazz.getName()), e);
        } catch (Exception e) {
            throw new FatalBeanException(
                    String.format("Failed to create instance of class [%s]: %s", clazz.getName(), e.getMessage()), e);
        }

        Map<String, java.lang.reflect.Field> fieldMap = BeanReflectionUtils.getCachedFields(clazz);

        for (String key : map.keySet()) {
            String fieldName = findMatchingFieldName(clazz, fieldMap, key);
            if (fieldName == null) {
                continue;
            }

            java.lang.reflect.Field field = fieldMap.get(fieldName);
            if (field == null) {
                continue;
            }

            try {
                Class<?> paramClass = field.getType();
                String methodName = "set" + capitalize(fieldName);
                Method method = clazz.getMethod(methodName, paramClass);

                Object value = map.get(key);
                Object convertedValue = BeanReflectionUtils.convertValue(value, paramClass);
                if (convertedValue == null && value != null) {
                    log.warn("Failed to convert value [{}] from {} to {} for field [{}]",
                            value, value.getClass().getSimpleName(),
                            paramClass.getSimpleName(), fieldName);
                    continue;
                }

                setAccessibleIfNeeded(method);
                method.invoke(entity, convertedValue);
            } catch (NoSuchMethodException e) {
                log.debug("Setter method for field [{}] not found in class [{}]", fieldName, clazz.getName());
            } catch (Exception e) {
                log.warn("Failed to set field [{}] with value [{}]: {}", fieldName, map.get(key), e.getMessage());
            }
        }

        return entity;
    }

    /**
     * 查找匹配的字段名
     *
     * @param clazz 类对象
     * @param fieldMap 字段映射
     * @param key Map键
     * @return 匹配的字段名，如果未找到返回null
     */
    private static String findMatchingFieldName(Class<?> clazz, Map<String, java.lang.reflect.Field> fieldMap, String key) {
        if (fieldMap.containsKey(key)) {
            return key;
        }

        if (key.equals(key.toUpperCase())) {
            for (String fieldName : fieldMap.keySet()) {
                if (fieldName.toUpperCase().equals(key)) {
                    return fieldName;
                }
            }
        }

        return null;
    }

    /**
     * 首字母大写
     *
     * @param str 字符串
     * @return 首字母大写后的字符串
     */
    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
