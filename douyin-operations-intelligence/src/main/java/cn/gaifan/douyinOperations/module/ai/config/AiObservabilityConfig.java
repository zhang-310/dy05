package cn.gaifan.douyinOperations.module.ai.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * AI LLM 可观测性配置 — Prometheus 指标收集
 * <p>
 * 指标：
 * - ai_llm_request_duration_seconds: LLM 调用延迟直方图
 * - ai_llm_tokens_total: Token 消耗计数
 * - ai_llm_cost_cny_total: 成本累计
 * - ai_evolution_task_duration_seconds: 进化任务延迟
 */
@Component
public class AiObservabilityConfig {

    private final MeterRegistry registry;

    public AiObservabilityConfig(@Autowired(required = false) MeterRegistry registry) {
        this.registry = registry;
    }

    /** 记录 LLM 调用延迟 */
    public void recordLlmLatency(String provider, String model, String status, long durationMs) {
        if (registry == null) return;
        Timer.builder("ai_llm_request_duration_seconds")
                .tag("provider", safe(provider))
                .tag("model", safe(model))
                .tag("status", safe(status))
                .description("LLM API call latency")
                .register(registry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    /** 记录 Token 消耗 */
    public void recordTokens(String provider, String model, String direction, long tokens) {
        if (registry == null || tokens <= 0) return;
        Counter.builder("ai_llm_tokens_total")
                .tag("provider", safe(provider))
                .tag("model", safe(model))
                .tag("direction", direction) // "input" or "output"
                .description("LLM token consumption")
                .register(registry)
                .increment(tokens);
    }

    /** 记录成本 */
    public void recordCost(String provider, String model, double costCny) {
        if (registry == null || costCny <= 0) return;
        Counter.builder("ai_llm_cost_cny_total")
                .tag("provider", safe(provider))
                .tag("model", safe(model))
                .description("LLM cost in CNY")
                .register(registry)
                .increment(costCny);
    }

    /** 记录进化任务执行 */
    public void recordEvolutionTask(String type, String status, long durationMs) {
        if (registry == null) return;
        Timer.builder("ai_evolution_task_duration_seconds")
                .tag("type", safe(type))
                .tag("status", safe(status))
                .description("Evolution task execution latency")
                .register(registry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    /** 记录 Scheduler 执行 */
    public void recordSchedulerExecution(String name, String status, long durationMs) {
        if (registry == null) return;
        Timer.builder("ai_scheduler_execution_duration_seconds")
                .tag("name", safe(name))
                .tag("status", safe(status))
                .register(registry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    private static String safe(String s) {
        return s != null && !s.isBlank() ? s : "unknown";
    }
}
