package cn.gaifan.douyinOperations.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;

import java.util.Map;

/**
 * Bean工具类（Facade）
 * 为了向后兼容，将所有功能委托到具体的工具类：
 * - BeanCopier: Bean属性复制
 * - BeanValidator: Bean验证
 * - BeanReflectionUtils: 反射工具
 *
 * @author gaifan
 */
@Slf4j
public class AppBeanUtils extends org.springframework.beans.BeanUtils {

    // ============ Copying Methods (BeanCopier) ============

    public static void copyNotNullProperties(Object source, Object target) throws BeansException {
        BeanCopier.copyNotNullProperties(source, target);
    }

    public static void copyNotNullProperties(Object source, Object target, String[] ignoreProperties)
            throws BeansException {
        BeanCopier.copyNotNullProperties(source, target, ignoreProperties);
    }

    public static void copyNotNullProperties(Object source, Object target, Class<?> editable) throws BeansException {
        BeanCopier.copyNotNullProperties(source, target, editable);
    }

    public static <T> T map2Bean(Class<T> clazz, Map<String, ?> map) throws FatalBeanException {
        return BeanCopier.map2Bean(clazz, map);
    }

    @Deprecated
    @SuppressWarnings("unchecked")
    public static <T> T map2Bean(T t, Map<String, ?> map) throws FatalBeanException {
        if (t == null) {
            return null;
        }
        return BeanCopier.map2Bean((Class<T>) t.getClass(), map);
    }

    // ============ Validation Methods (BeanValidator) ============

    public static boolean isEmpty(Object obj) {
        return BeanValidator.isEmpty(obj);
    }

    public static boolean isNotEmpty(Object obj) {
        return BeanValidator.isNotEmpty(obj);
    }

    public static boolean validate(Object bean) {
        return BeanValidator.validate(bean);
    }

    // ============ Map to Bean Conversion ============

    public static void mapToBean(Map<String, Object> map, Object obj) throws FatalBeanException {
        if (map == null || obj == null) {
            return;
        }

        try {
            java.beans.BeanInfo beanInfo = java.beans.Introspector.getBeanInfo(obj.getClass());
            java.beans.PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();

            Map<String, String> normalizedKeyMap = buildNormalizedKeyMap(map);

            for (java.beans.PropertyDescriptor property : propertyDescriptors) {
                String propertyName = property.getName();

                if ("class".equals(propertyName)) {
                    continue;
                }

                java.lang.reflect.Method setter = property.getWriteMethod();
                if (setter == null) {
                    continue;
                }

                String normalizedPropertyName = normalizeKey(propertyName);
                String mapKey = normalizedKeyMap.get(normalizedPropertyName);
                if (mapKey != null) {
                    Object value = map.get(mapKey);
                    try {
                        Class<?> paramType = setter.getParameterTypes()[0];
                        Object convertedValue = BeanReflectionUtils.convertValue(value, paramType);
                        if (convertedValue == null && value != null) {
                            log.warn("Failed to convert value [{}] from {} to {} for property [{}]",
                                    value, value.getClass().getSimpleName(),
                                    paramType.getSimpleName(), propertyName);
                            continue;
                        }

                        if (!java.lang.reflect.Modifier.isPublic(setter.getDeclaringClass().getModifiers())) {
                            setter.setAccessible(true);
                        }
                        setter.invoke(obj, convertedValue);
                    } catch (Exception e) {
                        log.warn("Failed to set property [{}] with value [{}]: {}",
                                propertyName, value, e.getMessage());
                    }
                }
            }
        } catch (FatalBeanException e) {
            throw e;
        } catch (Exception e) {
            throw new FatalBeanException("Failed to convert Map to Bean: " + e.getMessage(), e);
        }
    }

    @Deprecated
    public static void transMap2Bean(Map<String, Object> map, Object obj) throws FatalBeanException {
        mapToBean(map, obj);
    }

    /**
     * 规范化键名（转大写并移除下划线）
     */
    private static String normalizeKey(String key) {
        return key.toUpperCase().replace("_", "");
    }

    /**
     * 构建Map键的规范化映射
     */
    private static Map<String, String> buildNormalizedKeyMap(Map<String, Object> map) {
        Map<String, String> normalizedKeyMap = new java.util.HashMap<>(map.size());
        for (String key : map.keySet()) {
            normalizedKeyMap.put(normalizeKey(key), key);
        }
        return normalizedKeyMap;
    }
}
