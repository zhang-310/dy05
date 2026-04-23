package cn.gaifan.douyinOperations.common.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Micrometer 和分布式追踪配置
 * 配置应用级别的监控指标和分布式追踪支持
 *
 * @author gaifan
 */
@Slf4j
@Configuration
@EnableAspectJAutoProxy
public class MicrometerConfig {

    /**
     * 配置 MeterRegistry
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> meterRegistryCustomizer() {
        return registry -> {
            registry.config()
                    .commonTags(
                            "application", "douyin-operations",
                            "environment", getEnvironment()
                    );
            log.info("MeterRegistry configured with common tags");
        };
    }

    /**
     * 配置 TimedAspect 用于 @Timed 注解
     */
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        log.info("TimedAspect bean initialized");
        return new TimedAspect(registry);
    }

    /**
     * 配置 API 请求计数器
     */
    @Bean
    public Counter apiRequestCounter(MeterRegistry registry) {
        return Counter.builder("api.requests.total")
                .description("Total API requests")
                .tag("service", "douyin")
                .register(registry);
    }

    /**
     * 配置 API 响应时间计时器
     */
    @Bean
    public Timer apiResponseTime(MeterRegistry registry) {
        return Timer.builder("api.response.time")
                .description("API response time")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    /**
     * 配置缓存命中率指标
     */
    @Bean
    public Gauge cacheHitRate(MeterRegistry registry) {
        AtomicInteger hits = new AtomicInteger(0);
        AtomicInteger total = new AtomicInteger(0);

        Gauge.builder("cache.hit.rate", () -> {
            if (total.get() == 0) return 0;
            return (double) hits.get() / total.get();
        })
                .description("Cache hit rate")
                .tag("service", "douyin")
                .register(registry);

        return null;
    }

    /**
     * 配置业务错误计数器
     */
    @Bean
    public Counter businessErrorCounter(MeterRegistry registry) {
        return Counter.builder("business.errors")
                .description("Business errors")
                .tag("service", "douyin")
                .register(registry);
    }

    /**
     * 配置数据操作耗时计时器
     */
    @Bean
    public Timer dataOperationTimer(MeterRegistry registry) {
        return Timer.builder("data.operation.duration")
                .description("Data operation duration")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    /**
     * 获取当前环境
     */
    private String getEnvironment() {
        String profile = System.getProperty("spring.profiles.active", "dev");
        return profile.isEmpty() ? "dev" : profile;
    }
}
