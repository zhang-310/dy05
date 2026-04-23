package cn.gaifan.douyinOperations.module.benchmark.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Benchmark模块监控指标
 */
@Component
@RequiredArgsConstructor
public class BenchmarkMetrics {

    private final MeterRegistry meterRegistry;

    // ==================== 账号相关指标 ====================

    /**
     * 记录账号搜索次数
     */
    public void recordAccountSearch(String platform, boolean success) {
        Counter.builder("benchmark.account.search")
                .tag("platform", platform)
                .tag("success", String.valueOf(success))
                .description("账号搜索次数")
                .register(meterRegistry)
                .increment();
    }

    /**
     * 记录账号搜索耗时
     */
    public void recordAccountSearchDuration(String platform, long durationMs) {
        Timer.builder("benchmark.account.search.duration")
                .tag("platform", platform)
                .description("账号搜索耗时")
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * 记录账号创建次数
     */
    public void recordAccountCreated(String platform) {
        Counter.builder("benchmark.account.created")
                .tag("platform", platform)
                .description("账号创建次数")
                .register(meterRegistry)
                .increment();
    }

    // ==================== 视频相关指标 ====================

    /**
     * 记录视频采集次数
     */
    public void recordVideoCollected(Long accountId, int count) {
        Counter.builder("benchmark.video.collected")
                .tag("account_id", String.valueOf(accountId))
                .description("视频采集次数")
                .register(meterRegistry)
                .increment(count);
    }

    /**
     * 记录视频采集耗时
     */
    public void recordVideoCollectDuration(Long accountId, long durationMs) {
        Timer.builder("benchmark.video.collect.duration")
                .tag("account_id", String.valueOf(accountId))
                .description("视频采集耗时")
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * 记录视频分析次数
     */
    public void recordVideoAnalyzed(String status) {
        Counter.builder("benchmark.video.analyzed")
                .tag("status", status)
                .description("视频分析次数")
                .register(meterRegistry)
                .increment();
    }

    /**
     * 记录视频分析耗时
     */
    public void recordVideoAnalysisDuration(String analysisType, long durationMs) {
        Timer.builder("benchmark.video.analysis.duration")
                .tag("type", analysisType)
                .description("视频分析耗时")
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    // ==================== Cookie相关指标 ====================

    /**
     * 记录Cookie验证次数
     */
    public void recordCookieValidation(String platform, boolean valid) {
        Counter.builder("benchmark.cookie.validation")
                .tag("platform", platform)
                .tag("valid", String.valueOf(valid))
                .description("Cookie验证次数")
                .register(meterRegistry)
                .increment();
    }

    /**
     * 记录Cookie使用次数
     */
    public void recordCookieUsage(Long cookieId) {
        Counter.builder("benchmark.cookie.usage")
                .tag("cookie_id", String.valueOf(cookieId))
                .description("Cookie使用次数")
                .register(meterRegistry)
                .increment();
    }

    /**
     * 记录可用Cookie数量
     */
    public void recordAvailableCookies(String platform, int count) {
        meterRegistry.gauge("benchmark.cookie.available",
                java.util.Collections.singletonList(io.micrometer.core.instrument.Tag.of("platform", platform)),
                count);
    }

    // ==================== 任务相关指标 ====================

    /**
     * 记录任务创建次数
     */
    public void recordTaskCreated(String taskType) {
        Counter.builder("benchmark.task.created")
                .tag("type", taskType)
                .description("任务创建次数")
                .register(meterRegistry)
                .increment();
    }

    /**
     * 记录任务完成次数
     */
    public void recordTaskCompleted(String taskType, String status) {
        Counter.builder("benchmark.task.completed")
                .tag("type", taskType)
                .tag("status", status)
                .description("任务完成次数")
                .register(meterRegistry)
                .increment();
    }

    /**
     * 记录任务执行耗时
     */
    public void recordTaskDuration(String taskType, long durationMs) {
        Timer.builder("benchmark.task.duration")
                .tag("type", taskType)
                .description("任务执行耗时")
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * 记录任务失败次数
     */
    public void recordTaskFailed(String taskType, String errorType) {
        Counter.builder("benchmark.task.failed")
                .tag("type", taskType)
                .tag("error", errorType)
                .description("任务失败次数")
                .register(meterRegistry)
                .increment();
    }

    // ==================== AI相关指标 ====================

    /**
     * 记录AI分析调用次数
     */
    public void recordAiAnalysis(String model, boolean success) {
        Counter.builder("benchmark.ai.analysis")
                .tag("model", model)
                .tag("success", String.valueOf(success))
                .description("AI分析调用次数")
                .register(meterRegistry)
                .increment();
    }

    /**
     * 记录AI Token消耗
     */
    public void recordAiTokens(String model, long tokens) {
        Counter.builder("benchmark.ai.tokens")
                .tag("model", model)
                .description("AI Token消耗")
                .register(meterRegistry)
                .increment(tokens);
    }

    /**
     * 记录AI分析耗时
     */
    public void recordAiAnalysisDuration(String model, long durationMs) {
        Timer.builder("benchmark.ai.analysis.duration")
                .tag("model", model)
                .description("AI分析耗时")
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    // ==================== 错误相关指标 ====================

    /**
     * 记录Playwright错误
     */
    public void recordPlaywrightError(String operation, String errorType) {
        Counter.builder("benchmark.playwright.error")
                .tag("operation", operation)
                .tag("error", errorType)
                .description("Playwright错误次数")
                .register(meterRegistry)
                .increment();
    }

    /**
     * 记录数据库查询错误
     */
    public void recordDatabaseError(String operation, String errorType) {
        Counter.builder("benchmark.database.error")
                .tag("operation", operation)
                .tag("error", errorType)
                .description("数据库错误次数")
                .register(meterRegistry)
                .increment();
    }

    // ==================== 缓存相关指标 ====================

    /**
     * 记录缓存命中
     */
    public void recordCacheHit(String cacheName) {
        Counter.builder("benchmark.cache.hit")
                .tag("cache", cacheName)
                .description("缓存命中次数")
                .register(meterRegistry)
                .increment();
    }

    /**
     * 记录缓存未命中
     */
    public void recordCacheMiss(String cacheName) {
        Counter.builder("benchmark.cache.miss")
                .tag("cache", cacheName)
                .description("缓存未命中次数")
                .register(meterRegistry)
                .increment();
    }
}
