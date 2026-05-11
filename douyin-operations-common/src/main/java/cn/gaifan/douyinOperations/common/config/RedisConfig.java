package cn.gaifan.douyinOperations.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis Cache Configuration with multi-level TTL support
 */
@Configuration
@EnableCaching
public class RedisConfig {

    /**
     * 仅当 spring.cache.type=redis 时注册；dev 设为 simple 时由 Spring Boot 提供 ConcurrentMapCacheManager，
     * 避免未启动 Redis 时 @Cacheable 仍走 RedisCache 刷连接错误。
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        // Default TTL: 10 minutes
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .disableCachingNullValues();

        // Cache-specific TTL configurations
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();

        // Dashboard caches - 5 minutes (frequent updates)
        cacheConfigs.put("dashboard:admin", ttlConfig(5));
        cacheConfigs.put("dashboard:org", ttlConfig(5));

        // Config caches - 10 minutes (balance between consistency and performance)
        cacheConfigs.put("config", ttlConfig(10));

        // Auth caches - 10 minutes
        cacheConfigs.put("users", ttlConfig(10));
        cacheConfigs.put("auth:roleResources", ttlConfig(10));

        // Storage caches - 30 minutes (immutable URLs)
        cacheConfigs.put("storage:url", ttlConfig(30));

        // Wecom caches - 30 minutes (low-change configuration data)
        cacheConfigs.put("wecom:robot", ttlConfig(30));
        cacheConfigs.put("wecom:rules", ttlConfig(30));

        // AbTest caches - 10 minutes (frontend polling)
        cacheConfigs.put("abtest:experiment", ttlConfig(10));

        // Douyin account statistics - 5 minutes (updated on video sync)
        cacheConfigs.put("accountStatistics", ttlConfig(5));

        // Agent caches - 5 minutes (P0-6: 智能体列表查询缓存)
        cacheConfigs.put("agent:list", ttlConfig(5));

        // Live caches - 5 minutes (P0-3: Live 模块缓存策略)
        cacheConfigs.put("live:session", ttlConfig(5));
        cacheConfigs.put("live:script", ttlConfig(5));
        cacheConfigs.put("live:template", ttlConfig(10));
        cacheConfigs.put("live:product", ttlConfig(5));

        return RedisCacheManager.builder(factory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigs)
            .build();
    }

    /**
     * Helper method to create RedisCacheConfiguration with specified TTL
     */
    private RedisCacheConfiguration ttlConfig(int minutes) {
        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(minutes))
            .disableCachingNullValues();
    }

    /**
     * spring.cache.type=simple 时注册：项目内已有多个 CacheManager Bean，Boot 不会自动装配 ConcurrentMap；
     * 无 Redis 时作为 @Primary，供未指定 cacheManager 的 @Cacheable（如 config）使用。
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "simple")
    public CacheManager simplePrimaryCacheManager() {
        return new ConcurrentMapCacheManager();
    }
}
