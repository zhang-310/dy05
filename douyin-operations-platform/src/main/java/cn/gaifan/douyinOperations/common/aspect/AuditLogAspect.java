package cn.gaifan.douyinOperations.common.aspect;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.entity.AuditLog;
import cn.gaifan.douyinOperations.common.repository.AuditLogRepository;
import cn.gaifan.douyinOperations.common.util.DateUtil;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 审计日志切面
 * 记录所有创建、编辑、删除操作的详细信息
 *
 * @author gaifan
 */
@Slf4j
@Aspect
@Component
public class AuditLogAspect {

    @Autowired
    private AuditLogRepository auditLogRepository;

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AuditLogAspect.class);

    /**
     * 切点：标记创建操作的方法（通过方法名匹配）
     */
    @Pointcut("execution(* cn.gaifan.douyinOperations.module.*.service.*Service.save*(..))")
    public void createOperation() {
    }

    /**
     * 切点：标记删除操作的方法
     */
    @Pointcut("execution(* cn.gaifan.douyinOperations.module.*.service.*Service.delete*(..))")
    public void deleteOperation() {
    }

    /**
     * 切点：标记编辑操作的方法
     */
    @Pointcut("execution(* cn.gaifan.douyinOperations.module.*.service.*Service.update*(..))")
    public void updateOperation() {
    }

    /**
     * 创建操作后置通知
     */
    @AfterReturning(pointcut = "createOperation()", returning = "result")
    public void afterCreateOperation(JoinPoint joinPoint, Object result) {
        logAuditEvent("CREATE", joinPoint, result, null);
    }

    /**
     * 编辑操作后置通知
     */
    @AfterReturning(pointcut = "updateOperation()", returning = "result")
    public void afterUpdateOperation(JoinPoint joinPoint, Object result) {
        logAuditEvent("UPDATE", joinPoint, result, null);
    }

    /**
     * 删除操作后置通知
     */
    @AfterReturning(pointcut = "deleteOperation()", returning = "result")
    public void afterDeleteOperation(JoinPoint joinPoint, Object result) {
        logAuditEvent("DELETE", joinPoint, result, null);
    }

    /**
     * 操作异常时记录
     */
    @AfterThrowing(pointcut = "createOperation() || updateOperation() || deleteOperation()", throwing = "exception")
    public void afterThrowingOperation(JoinPoint joinPoint, Exception exception) {
        logAuditEvent(getOperationType(joinPoint), joinPoint, null, exception);
    }

    /**
     * 记录审计事件
     */
    private void logAuditEvent(String operationType, JoinPoint joinPoint, Object result, Exception exception) {
        try {
            Long userId = getCurrentUserId();
            String username = getCurrentUsername();
            String methodName = joinPoint.getSignature().getName();
            String className = joinPoint.getTarget().getClass().getSimpleName();
            Object[] args = joinPoint.getArgs();
            String ip = getClientIp();
            String userAgent = getUserAgent();

            Map<String, Object> auditLog = new HashMap<>();
            auditLog.put("timestamp", DateUtil.dateToString(DateUtil.getLocalTimestamp()));
            auditLog.put("operationType", operationType);
            auditLog.put("username", username);
            auditLog.put("className", className);
            auditLog.put("methodName", methodName);
            auditLog.put("args", Arrays.toString(args));
            auditLog.put("result", result != null ? result.toString() : "null");
            auditLog.put("status", exception == null ? "SUCCESS" : "FAILED");
            auditLog.put("errorMessage", exception != null ? exception.getMessage() : null);
            auditLog.put("ip", ip);

            logger.info("AUDIT_LOG: {}", auditLog);

            // 保存到数据库
            AuditLog dbLog = AuditLog.builder()
                .userId(userId)
                .username(username)
                .action(operationType)
                .entity(className)
                .entityId(extractEntityId(args))
                .newValue(result != null ? JSON.toJSONString(result) : null)
                .ip(ip)
                .userAgent(userAgent)
                .createTime(LocalDateTime.now())
                .status(exception == null ? 1 : 0)
                .errorMsg(exception != null ? exception.getMessage() : null)
                .build();

            auditLogRepository.save(dbLog);
        } catch (Exception e) {
            logger.error("Failed to log audit event: {}", e.getMessage(), e);
        }
    }

    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                Long requestUserId = AuthTokenFilter.getUserId(request);
                if (requestUserId != null) {
                    return requestUserId;
                }
            }

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof UserDetails) {
                    String username = ((UserDetails) principal).getUsername();
                    try {
                        return Long.parseLong(username);
                    } catch (NumberFormatException ignored) {
                        logger.debug("UserDetails username 不是数值 userId: {}", username);
                    }
                } else if (principal instanceof String principalStr) {
                    try {
                        return Long.parseLong(principalStr);
                    } catch (NumberFormatException ignored) {
                        logger.debug("Authentication principal 不是数值 userId: {}", principalStr);
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to get current user ID: {}", e.getMessage());
        }
        // 非请求线程 / 系统任务使用系统用户 0 记账，而不是伪造为业务用户。
        return 0L;
    }

    /**
     * 获取当前登录用户名
     */
    private String getCurrentUsername() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                Long requestUserId = AuthTokenFilter.getUserId(request);
                if (requestUserId != null) {
                    return String.valueOf(requestUserId);
                }
            }

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof UserDetails) {
                    return ((UserDetails) principal).getUsername();
                } else if (principal instanceof String) {
                    return (String) principal;
                } else if (principal != null) {
                    return principal.toString();
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to get current username: {}", e.getMessage());
        }
        return "SYSTEM";
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();

                // 尝试从 X-Forwarded-For 头获取（代理情况）
                String ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("Proxy-Client-IP");
                }
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("WL-Proxy-Client-IP");
                }
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getRemoteAddr();
                }

                // 处理 X-Forwarded-For 包含多个 IP 的情况
                if (ip != null && ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }

                return ip != null ? ip : "UNKNOWN";
            }
        } catch (Exception e) {
            logger.debug("Failed to get client IP: {}", e.getMessage());
        }
        return "UNKNOWN";
    }

    /**
     * 获取 User-Agent
     */
    private String getUserAgent() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String userAgent = request.getHeader("User-Agent");
                return userAgent != null ? userAgent : "UNKNOWN";
            }
        } catch (Exception e) {
            logger.debug("Failed to get user agent: {}", e.getMessage());
        }
        return "UNKNOWN";
    }

    /**
     * 从方法参数中提取实体ID
     */
    private Long extractEntityId(Object[] args) {
        if (args == null || args.length == 0) {
            return 0L;
        }
        Object arg = args[0];
        if (arg instanceof Long) {
            return (Long) arg;
        }
        // 可以根据需要添加更多的类型处理
        return 0L;
    }

    /**
     * 从方法名推断操作类型
     */
    private String getOperationType(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        if (methodName.startsWith("delete")) {
            return "DELETE";
        } else if (methodName.startsWith("update")) {
            return "UPDATE";
        } else if (methodName.startsWith("save")) {
            return "CREATE";
        }
        return "UNKNOWN";
    }
}

