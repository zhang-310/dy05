package cn.gaifan.douyinOperations.common.annotation;

import cn.gaifan.douyinOperations.common.validator.PasswordValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 密码验证注解
 * 密码必须包含大小写字母、数字和特殊字符，长度 8-32
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordValidator.class)
@Documented
public @interface ValidPassword {
    String message() default "密码必须包含大小写字母、数字和特殊字符，长度 8-32";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
