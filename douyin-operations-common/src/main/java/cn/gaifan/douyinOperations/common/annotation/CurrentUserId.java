package cn.gaifan.douyinOperations.common.annotation;

import java.lang.annotation.*;

/**
 * 注入当前登录用户的 userId 到方法参数
 * 配合 @RequireAuth 使用
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUserId {
}
