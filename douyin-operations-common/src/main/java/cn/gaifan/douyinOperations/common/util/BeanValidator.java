package cn.gaifan.douyinOperations.common.util;

import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Map;

/**
 * Bean 验证工具类
 * 负责Bean对象的验证和检查
 *
 * @author gaifan
 */
@Slf4j
public class BeanValidator {

    /**
     * 检查对象是否为空
     *
     * @param obj 对象
     * @return 如果对象为空返回true，否则返回false
     */
    public static boolean isEmpty(Object obj) {
        if (obj == null) {
            return true;
        }

        if (obj instanceof String) {
            return ((String) obj).trim().isEmpty();
        }

        if (obj instanceof Collection) {
            return ((Collection<?>) obj).isEmpty();
        }

        if (obj instanceof Map) {
            return ((Map<?, ?>) obj).isEmpty();
        }

        return false;
    }

    /**
     * 检查对象是否非空
     *
     * @param obj 对象
     * @return 如果对象非空返回true，否则返回false
     */
    public static boolean isNotEmpty(Object obj) {
        return !isEmpty(obj);
    }

    /**
     * 验证Bean对象（基础验证）
     *
     * @param bean Bean对象
     * @return 如果Bean有效返回true，否则返回false
     */
    public static boolean validate(Object bean) {
        return bean != null;
    }
}
