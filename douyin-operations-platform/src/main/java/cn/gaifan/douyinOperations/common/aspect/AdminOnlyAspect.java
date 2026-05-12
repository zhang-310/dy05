package cn.gaifan.douyinOperations.common.aspect;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 管理员权限校验切面
 *
 * <p>使用方法：在 Controller 方法上添加 @AdminOnly 注解
 * <pre>
 * {@code
 * @PostMapping("/save")
 * @AdminOnly
 * public RESTResult<Long> save(@RequestBody ConfigSaveVO vo) {
 *     // 方法体无需再写权限校验代码
 * }
 * }
 * </pre>
 *
 * <p>切面会自动校验：
 * <ul>
 *   <li>用户是否已登录（userId != null）</li>
 *   <li>用户角色是否为 admin</li>
 * </ul>
 *
 * <p>校验失败时抛出 BusinessException：
 * <ul>
 *   <li>未登录 → ErrorCode.UNAUTHORIZED</li>
 *   <li>非管理员 → ErrorCode.FORBIDDEN</li>
 * </ul>
 */
@Aspect
@Component
@Order(1) // 优先级高于其他切面，确保权限校验最先执行
public class AdminOnlyAspect {

    private static final Logger log = LoggerFactory.getLogger(AdminOnlyAspect.class);
    private static final String ROLE_ADMIN = "admin";

    /**
     * 管理员权限注解
     *
     * <p>标记在 Controller 方法上，表示该接口仅管理员可访问
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface AdminOnly {
    }

    /**
     * 权限校验切面
     *
     * <p>在标记了 @AdminOnly 的方法执行前，自动校验用户登录状态和管理员权限
     *
     * @param joinPoint 切入点
     * @throws BusinessException 权限校验失败时抛出
     */
    @Before("@annotation(cn.gaifan.douyinOperations.common.aspect.AdminOnlyAspect.AdminOnly)")
    public void checkAdminPermission(JoinPoint joinPoint) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            log.warn("AdminOnly 切面无法获取 HttpServletRequest，跳过权限校验");
            return;
        }

        HttpServletRequest request = attributes.getRequest();
        Long userId = AuthTokenFilter.getUserId(request);
        String roleCode = AuthTokenFilter.getRoleCode(request);

        // 校验登录状态
        if (userId == null) {
            log.warn("未登录用户尝试访问管理员接口: {} {}", request.getMethod(), request.getRequestURI());
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }

        // 校验管理员权限
        if (!ROLE_ADMIN.equals(roleCode)) {
            log.warn("非管理员用户 {} (role={}) 尝试访问管理员接口: {} {}",
                    userId, roleCode, request.getMethod(), request.getRequestURI());
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限访问");
        }

        log.debug("管理员权限校验通过: userId={}, method={}, uri={}",
                userId, request.getMethod(), request.getRequestURI());
    }
}
