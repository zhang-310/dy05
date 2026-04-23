package cn.gaifan.douyinOperations.common.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 速率限制和熔断器配置
 */
@Configuration
public class RateLimiterConfiguration {

    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        return RateLimiterRegistry.ofDefaults();
    }

    @Bean
    public RateLimiter apiRateLimiter(RateLimiterRegistry registry,
            @Value("${app.rate-limit.api-per-minute:100}") int limitPerMinute) {
        io.github.resilience4j.ratelimiter.RateLimiterConfig config =
            io.github.resilience4j.ratelimiter.RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofMinutes(1))
            .limitForPeriod(limitPerMinute)
            .timeoutDuration(Duration.ofSeconds(5))
            .build();

        return registry.rateLimiter("api-limiter", config);
    }

    @Bean
    public RateLimiter loginRateLimiter(RateLimiterRegistry registry) {
        io.github.resilience4j.ratelimiter.RateLimiterConfig config =
            io.github.resilience4j.ratelimiter.RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofMinutes(1))
            .limitForPeriod(5)  // 每分钟 5 次登录尝试
            .timeoutDuration(Duration.ofSeconds(5))
            .build();

        return registry.rateLimiter("login-limiter", config);
    }

    /**
     * 与 {@link cn.gaifan.douyinOperations.module.ai.config.AiCircuitBreakerConfig} 共用同一 {@link CircuitBreakerRegistry}，
     * api-circuit-breaker 已在该处注册。
     */
    @Bean
    public CircuitBreaker circuitBreaker(CircuitBreakerRegistry circuitBreakerRegistry) {
        return circuitBreakerRegistry.circuitBreaker("api-circuit-breaker");
    }
}
