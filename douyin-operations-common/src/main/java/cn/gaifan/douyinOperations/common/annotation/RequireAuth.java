package cn.gaifan.douyinOperations.common.annotation;

import java.lang.annotation.*;

/**
 * 标注需要登录认证的 Controller 方法
 * 自动从请求中提取 userId 并注入到方法参数
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireAuth {
}
