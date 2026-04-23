package cn.gaifan.douyinOperations.common.aspect;

import cn.gaifan.douyinOperations.common.metrics.BusinessMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 性能日志切面
 * 记录接口响应时间、数据库查询时间、Redis操作时间等性能指标
 *
 * @author gaifan
 */
@Slf4j
@Aspect
@Component
public class PerformanceLogAspect {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PerformanceLogAspect.class);

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    @Autowired(required = false)
    private BusinessMetrics businessMetrics;

    /**
     * 切点：所有Controller方法
     */
    @Pointcut("execution(* cn.gaifan.douyinOperations.module.*.controller.*Controller.*(..))")
    public void controllerMethods() {
    }

    /**
     * 切点：所有Service方法
     */
    @Pointcut("execution(* cn.gaifan.douyinOperations.module.*.service.*Service.*(..))")
    public void serviceMethods() {
    }

    /**
     * 切点：所有Repository方法
     */
    @Pointcut("execution(* cn.gaifan.douyinOperations.module.*.repository.*Repository.*(..))")
    public void repositoryMethods() {
    }

    /**
     * Controller方法性能监控
     */
    @Around("controllerMethods()")
    public Object aroundControllerMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        return measurePerformance(joinPoint, "api");
    }

    /**
     * Service方法性能监控
     */
    @Around("serviceMethods()")
    public Object aroundServiceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        return measurePerformance(joinPoint, "service");
    }

    /**
     * Repository方法性能监控（数据库查询）
     */
    @Around("repositoryMethods()")
    public Object aroundRepositoryMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        return measurePerformance(joinPoint, "database");
    }

    /**
     * 测量方法执行时间
     */
    private Object measurePerformance(ProceedingJoinPoint joinPoint, String category) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodSignature = className + "." + methodName;

        try {
            Object result = joinPoint.proceed();
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            logPerformance(methodSignature, category, duration, null);

            // 记录到 Micrometer
            if (meterRegistry != null) {
                Timer.builder("method.execution.time")
                        .description("Method execution time")
                        .tag("method", methodSignature)
                        .tag("category", category)
                        .publishPercentiles(0.5, 0.95, 0.99)
                        .register(meterRegistry)
                        .record(duration, java.util.concurrent.TimeUnit.MILLISECONDS);
            }

            // 记录到业务指标
            if (businessMetrics != null && "service".equals(category)) {
                businessMetrics.recordDataOperation("execute", className, duration);
            }

            return result;
        } catch (Throwable ex) {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            logPerformance(methodSignature, category, duration, ex);

            // 记录异常到 Micrometer
            if (meterRegistry != null) {
                meterRegistry.counter("method.execution.error", "method", methodSignature, "category", category).increment();
            }

            // 记录业务错误
            if (businessMetrics != null) {
                businessMetrics.recordBusinessError(ex.getClass().getSimpleName(), className);
            }

            throw ex;
        }
    }

    /**
     * 记录性能日志
     */
    private void logPerformance(String methodSignature, String category, long duration, Throwable exception) {
        try {
            Map<String, Object> performanceLog = new HashMap<>();
            performanceLog.put("method", methodSignature);
            performanceLog.put("category", category);
            performanceLog.put("duration_ms", duration);
            performanceLog.put("request_url", getRequestUrl());
            performanceLog.put("status", exception == null ? "SUCCESS" : "FAILURE");

            if (exception != null) {
                performanceLog.put("error_message", exception.getMessage());
                logger.warn("PERFORMANCE_LOG: {}", performanceLog);
            } else {
                // 只有慢查询（>500ms）才记录到WARN级别
                if (duration > 500) {
                    performanceLog.put("level", "SLOW_QUERY");
                    logger.warn("PERFORMANCE_LOG: {}", performanceLog);
                } else {
                    logger.info("PERFORMANCE_LOG: {}", performanceLog);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to log performance: {}", e.getMessage(), e);
        }
    }

    /**
     * 获取当前请求的URL
     */
    private String getRequestUrl() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String url = request.getRequestURI();
                String queryString = request.getQueryString();
                return queryString != null ? url + "?" + queryString : url;
            }
        } catch (Exception e) {
            logger.debug("Failed to get request URL: {}", e.getMessage());
        }
        return "UNKNOWN";
    }
}
