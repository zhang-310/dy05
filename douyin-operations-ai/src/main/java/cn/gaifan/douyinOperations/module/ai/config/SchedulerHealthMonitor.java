package cn.gaifan.douyinOperations.module.ai.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.*;

/**
 * Scheduler 健康监控 + 超时保护
 * <p>
 * 功能：
 * 1. 任务级超时：包裹 Runnable，超时后取消任务
 * 2. 健康记录：每次执行 startTime/endTime/status
 * 3. Prometheus 指标：ai_scheduler_last_success_timestamp{name}
 * 4. 连续失败告警：3次连续失败 → WARN 日志
 */
@Component
public class SchedulerHealthMonitor {

    private static final Logger log = LoggerFactory.getLogger(SchedulerHealthMonitor.class);
    private static final int MAX_CONSECUTIVE_FAILURES = 3;

    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "ai-scheduler-timeout");
        t.setDaemon(true);
        return t;
    });

    private final Map<String, SchedulerHealth> healthMap = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    @Autowired(required = false)
    private AiObservabilityConfig observability;

    /**
     * 带超时保护执行任务
     * @param name 任务名
     * @param task 要执行的任务
     * @param timeoutMinutes 超时分钟数
     */
    public void executeWithTimeout(String name, Runnable task, int timeoutMinutes) {
        long startMs = System.currentTimeMillis();
        SchedulerHealth health = healthMap.computeIfAbsent(name, k -> new SchedulerHealth(k, meterRegistry));
        health.markStarted();

        Future<?> future = executor.submit(task);
        try {
            future.get(timeoutMinutes, TimeUnit.MINUTES);
            long durationMs = System.currentTimeMillis() - startMs;
            health.markSuccess(durationMs);
            if (observability != null) {
                observability.recordSchedulerExecution(name, "success", durationMs);
            }
            log.info("[Scheduler] {} 执行完成: duration={}ms", name, durationMs);
        } catch (TimeoutException e) {
            future.cancel(true);
            long durationMs = System.currentTimeMillis() - startMs;
            health.markFailure("timeout");
            if (observability != null) {
                observability.recordSchedulerExecution(name, "timeout", durationMs);
            }
            log.error("[Scheduler] {} 执行超时({}分钟)，已取消任务", name, timeoutMinutes);
        } catch (ExecutionException e) {
            long durationMs = System.currentTimeMillis() - startMs;
            health.markFailure(e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
            if (observability != null) {
                observability.recordSchedulerExecution(name, "error", durationMs);
            }
            log.error("[Scheduler] {} 执行失败: {}", name, e.getMessage(), e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            health.markFailure("interrupted");
            log.warn("[Scheduler] {} 执行被中断", name);
        }
    }

    /** 获取所有 Scheduler 健康状态 */
    public Map<String, Map<String, Object>> getAllHealth() {
        Map<String, Map<String, Object>> result = new ConcurrentHashMap<>();
        for (Map.Entry<String, SchedulerHealth> entry : healthMap.entrySet()) {
            result.put(entry.getKey(), entry.getValue().toMap());
        }
        return result;
    }

    /**
     * 单个 Scheduler 的健康状态
     */
    static class SchedulerHealth {
        private final String name;
        private volatile long lastStartTime;
        private volatile long lastSuccessTime;
        private volatile long lastDurationMs;
        private volatile int consecutiveFailures;
        private volatile String lastError;

        SchedulerHealth(String name, MeterRegistry registry) {
            this.name = name;
            if (registry != null) {
                Gauge.builder("ai_scheduler_last_success_timestamp", this, h -> h.lastSuccessTime / 1000.0)
                        .tag("name", name)
                        .description("Last successful execution timestamp (epoch seconds)")
                        .register(registry);
                Gauge.builder("ai_scheduler_consecutive_failures", this, h -> (double) h.consecutiveFailures)
                        .tag("name", name)
                        .description("Consecutive failure count")
                        .register(registry);
            }
        }

        void markStarted() {
            this.lastStartTime = System.currentTimeMillis();
        }

        void markSuccess(long durationMs) {
            this.lastSuccessTime = System.currentTimeMillis();
            this.lastDurationMs = durationMs;
            this.consecutiveFailures = 0;
            this.lastError = null;
        }

        void markFailure(String error) {
            this.consecutiveFailures++;
            this.lastError = error;
            if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                log.warn("[Scheduler 告警] {} 连续失败 {} 次，最后错误: {}", name, consecutiveFailures, error);
            }
        }

        Map<String, Object> toMap() {
            Map<String, Object> m = new ConcurrentHashMap<>();
            m.put("name", name);
            m.put("lastStartTime", lastStartTime);
            m.put("lastSuccessTime", lastSuccessTime);
            m.put("lastDurationMs", lastDurationMs);
            m.put("consecutiveFailures", consecutiveFailures);
            if (lastError != null) m.put("lastError", lastError);
            return m;
        }
    }
}
