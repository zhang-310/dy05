package cn.gaifan.douyinOperations.module.benchmark.config;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Benchmark模块重试配置
 * 使用Resilience4j实现自动重试机制
 */
@Slf4j
@Configuration
public class BenchmarkRetryConfig {

    /**
     * 视频下载重试配置
     * - 最多重试3次
     * - 固定延迟：1秒
     * - 只重试特定异常
     */
    @Bean
    public Retry videoDownloadRetry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofSeconds(1))
                .retryExceptions(
                        java.io.IOException.class,
                        java.net.SocketTimeoutException.class,
                        java.net.ConnectException.class
                )
                .ignoreExceptions(
                        IllegalArgumentException.class,
                        SecurityException.class
                )
                .build();

        return RetryRegistry.of(config).retry("videoDownload");
    }

    /**
     * Cookie验证重试配置
     * - 最多重试2次
     * - 固定延迟：500ms
     */
    @Bean
    public Retry cookieValidationRetry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(2)
                .waitDuration(Duration.ofMillis(500))
                .retryExceptions(
                        java.net.SocketTimeoutException.class,
                        java.net.ConnectException.class
                )
                .build();

        return RetryRegistry.of(config).retry("cookieValidation");
    }

    /**
     * Playwright操作重试配置
     * - 最多重试3次
     * - 固定延迟：2秒
     */
    @Bean
    public Retry playwrightRetry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofSeconds(2))
                .retryExceptions(
                        RuntimeException.class
                )
                .ignoreExceptions(
                        IllegalArgumentException.class,
                        NullPointerException.class
                )
                .build();

        return RetryRegistry.of(config).retry("playwright");
    }

    /**
     * AI分析重试配置
     * - 最多重试2次
     * - 固定延迟：3秒
     * - 只重试网络相关异常
     */
    @Bean
    public Retry aiAnalysisRetry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(2)
                .waitDuration(Duration.ofSeconds(3))
                .retryExceptions(
                        java.net.SocketTimeoutException.class,
                        java.net.ConnectException.class,
                        java.io.IOException.class
                )
                .build();

        return RetryRegistry.of(config).retry("aiAnalysis");
    }

    /**
     * BOS上传重试配置
     * - 最多重试3次
     * - 固定延迟：2秒
     */
    @Bean
    public Retry bosUploadRetry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofSeconds(2))
                .retryExceptions(
                        java.io.IOException.class,
                        java.net.SocketTimeoutException.class
                )
                .build();

        return RetryRegistry.of(config).retry("bosUpload");
    }

    /**
     * 数据库操作重试配置
     * - 最多重试2次
     * - 固定延迟：100ms
     * - 只重试事务相关异常
     */
    @Bean
    public Retry databaseRetry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(2)
                .waitDuration(Duration.ofMillis(100))
                .retryExceptions(
                        org.springframework.dao.TransientDataAccessException.class,
                        org.springframework.dao.ConcurrencyFailureException.class
                )
                .build();

        return RetryRegistry.of(config).retry("database");
    }
}
