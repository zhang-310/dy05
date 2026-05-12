package cn.gaifan.douyinOperations.common.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * P1-3: API 限流配置
 * 使用 Resilience4j RateLimiter 保护敏感接口
 */
@Configuration
public class RateLimitConfig {

    /**
     * OAuth 授权接口限流器
     * 限制：每分钟 10 次请求
     */
    @Bean
    public RateLimiter oauthRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(10)                              // 每个周期允许 10 次请求
                .limitRefreshPeriod(Duration.ofMinutes(1))       // 周期为 1 分钟
                .timeoutDuration(Duration.ofSeconds(5))          // 等待许可的超时时间
                .build();

        return RateLimiterRegistry.of(config).rateLimiter("oauth");
    }

    /**
     * 视频同步接口限流器
     * 限制：每分钟 5 次请求（避免频繁调用抖音 API）
     */
    @Bean
    public RateLimiter videoSyncRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(5)
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ofSeconds(5))
                .build();

        return RateLimiterRegistry.of(config).rateLimiter("videoSync");
    }

    /**
     * 粉丝画像同步接口限流器
     * 限制：每分钟 3 次请求（粉丝画像数据量大，限制更严格）
     */
    @Bean
    public RateLimiter fanProfileSyncRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(3)
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ofSeconds(5))
                .build();

        return RateLimiterRegistry.of(config).rateLimiter("fanProfileSync");
    }

    /**
     * 通用 API 限流器
     * 限制：每分钟 60 次请求
     */
    @Bean
    public RateLimiter generalApiRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(60)
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ofSeconds(5))
                .build();

        return RateLimiterRegistry.of(config).rateLimiter("generalApi");
    }

    /**
     * P1-2: Webhook 接口限流器
     * 限制：每分钟 30 次请求（防止恶意重放攻击）
     */
    @Bean
    public RateLimiter webhookRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(30)
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ofSeconds(5))
                .build();

        return RateLimiterRegistry.of(config).rateLimiter("webhook");
    }
}
