package cn.gaifan.douyinOperations.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 业务指标记录组件
 * 记录用户登录、数据操作、业务错误等关键业务指标
 *
 * @author gaifan
 */
@Slf4j
@Component
public class BusinessMetrics {

    private final MeterRegistry meterRegistry;

    public BusinessMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * 记录用户登录尝试
     *
     * @param loginType 登录类型（password, oauth, etc）
     * @param success   是否成功
     */
    public void recordLogin(String loginType, boolean success) {
        try {
            Counter.builder("user.login.attempts")
                    .description("User login attempts")
                    .tag("type", loginType)
                    .tag("success", String.valueOf(success))
                    .register(meterRegistry)
                    .increment();
            log.debug("Recorded login attempt: type={}, success={}", loginType, success);
        } catch (Exception e) {
            log.warn("Failed to record login metric: {}", e.getMessage());
        }
    }

    /**
     * 记录数据操作耗时
     *
     * @param operation 操作类型（save, update, delete, query）
     * @param entity    实体类型
     * @param duration  耗时（毫秒）
     */
    public void recordDataOperation(String operation, String entity, long duration) {
        try {
            Timer.builder("data.operation.duration")
                    .description("Data operation duration")
                    .tag("operation", operation)
                    .tag("entity", entity)
                    .publishPercentiles(0.5, 0.95, 0.99)
                    .register(meterRegistry)
                    .record(duration, TimeUnit.MILLISECONDS);
            log.debug("Recorded data operation: operation={}, entity={}, duration={}ms", operation, entity, duration);
        } catch (Exception e) {
            log.warn("Failed to record data operation metric: {}", e.getMessage());
        }
    }

    /**
     * 记录业务错误
     *
     * @param errorCode 错误代码
     * @param module    模块名称
     */
    public void recordBusinessError(String errorCode, String module) {
        try {
            Counter.builder("business.errors")
                    .description("Business errors")
                    .tag("code", errorCode)
                    .tag("module", module)
                    .register(meterRegistry)
                    .increment();
            log.debug("Recorded business error: code={}, module={}", errorCode, module);
        } catch (Exception e) {
            log.warn("Failed to record business error metric: {}", e.getMessage());
        }
    }

    /**
     * 记录缓存操作
     *
     * @param operation 操作类型（hit, miss, put）
     * @param cacheType 缓存类型（redis, caffeine）
     * @param duration  耗时（毫秒）
     */
    public void recordCacheOperation(String operation, String cacheType, long duration) {
        try {
            Counter.builder("cache.operations")
                    .description("Cache operations")
                    .tag("operation", operation)
                    .tag("type", cacheType)
                    .register(meterRegistry)
                    .increment();

            Timer.builder("cache.operation.duration")
                    .description("Cache operation duration")
                    .tag("operation", operation)
                    .tag("type", cacheType)
                    .register(meterRegistry)
                    .record(duration, TimeUnit.MILLISECONDS);
            log.debug("Recorded cache operation: operation={}, type={}, duration={}ms", operation, cacheType, duration);
        } catch (Exception e) {
            log.warn("Failed to record cache operation metric: {}", e.getMessage());
        }
    }

    /**
     * 记录API请求
     *
     * @param endpoint 端点路径
     * @param method   HTTP方法
     * @param status   HTTP状态码
     * @param duration 耗时（毫秒）
     */
    public void recordApiRequest(String endpoint, String method, int status, long duration) {
        try {
            Counter.builder("api.requests.total")
                    .description("Total API requests")
                    .tag("endpoint", endpoint)
                    .tag("method", method)
                    .tag("status", String.valueOf(status))
                    .register(meterRegistry)
                    .increment();

            Timer.builder("api.response.time")
                    .description("API response time")
                    .tag("endpoint", endpoint)
                    .tag("method", method)
                    .tag("status", String.valueOf(status))
                    .publishPercentiles(0.5, 0.95, 0.99)
                    .register(meterRegistry)
                    .record(duration, TimeUnit.MILLISECONDS);
            log.debug("Recorded API request: endpoint={}, method={}, status={}, duration={}ms", endpoint, method, status, duration);
        } catch (Exception e) {
            log.warn("Failed to record API request metric: {}", e.getMessage());
        }
    }

    /**
     * 记录数据库连接池状态
     *
     * @param activeConnections 活跃连接数
     * @param maxConnections    最大连接数
     */
    public void recordDatabasePoolStatus(int activeConnections, int maxConnections) {
        try {
            meterRegistry.gauge("database.pool.active", activeConnections);
            meterRegistry.gauge("database.pool.max", maxConnections);
            log.debug("Recorded database pool status: active={}, max={}", activeConnections, maxConnections);
        } catch (Exception e) {
            log.warn("Failed to record database pool metric: {}", e.getMessage());
        }
    }

    /**
     * 记录业务事件计数
     *
     * @param eventType 事件类型
     * @param tags      标签（key-value对）
     */
    public void recordBusinessEvent(String eventType, String... tags) {
        try {
            Counter.builder("business.events")
                    .description("Business events")
                    .tag("type", eventType);
            // 添加额外标签
            if (tags != null && tags.length > 0) {
                for (int i = 0; i < tags.length - 1; i += 2) {
                    // 这里需要在构建器中添加标签
                }
            }
            log.debug("Recorded business event: type={}", eventType);
        } catch (Exception e) {
            log.warn("Failed to record business event metric: {}", e.getMessage());
        }
    }
}
