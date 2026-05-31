package cn.gaifan.douyinOperations.module.ai.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * AI 模块 L1 本地缓存（Caffeine）配置
 * <p>
 * 热点数据缓存，减少 DB/Redis 查询：
 * - aiModels: 模型列表（5min TTL, max 50）
 * - promptTemplates: 提示词模板（10min TTL, max 200）
 * - taskModelConfig: 任务模型路由（5min TTL, max 100）
 * <p>
 * 写入时双写失效：save/update → cache.invalidate(key)
 */
@Configuration
public class AiCacheConfig {

    private static final Logger log = LoggerFactory.getLogger(AiCacheConfig.class);

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    @Bean
    public Cache<String, List<?>> aiModelCache() {
        Cache<String, List<?>> cache = Caffeine.newBuilder()
                .maximumSize(50)
                .expireAfterWrite(Duration.ofMinutes(5))
                .recordStats()
                .build();
        registerMetrics("ai_models_l1", cache);
        log.info("[AiCache] L1 缓存已初始化: aiModels (max=50, ttl=5min)");
        return cache;
    }

    @Bean
    public Cache<String, Object> promptTemplateCache() {
        Cache<String, Object> cache = Caffeine.newBuilder()
                .maximumSize(200)
                .expireAfterWrite(Duration.ofMinutes(10))
                .recordStats()
                .build();
        registerMetrics("ai_prompt_templates_l1", cache);
        log.info("[AiCache] L1 缓存已初始化: promptTemplates (max=200, ttl=10min)");
        return cache;
    }

    @Bean
    public Cache<String, Map<String, Object>> taskModelConfigCache() {
        Cache<String, Map<String, Object>> cache = Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(Duration.ofMinutes(5))
                .recordStats()
                .build();
        registerMetrics("ai_task_model_config_l1", cache);
        log.info("[AiCache] L1 缓存已初始化: taskModelConfig (max=100, ttl=5min)");
        return cache;
    }

    @SuppressWarnings("unchecked")
    private <K, V> void registerMetrics(String cacheName, Cache<K, V> cache) {
        if (meterRegistry != null) {
            try {
                CaffeineCacheMetrics.monitor(meterRegistry, cache, cacheName);
            } catch (Exception e) {
                log.debug("Caffeine metrics 注册失败: {}", e.getMessage());
            }
        }
    }
}
