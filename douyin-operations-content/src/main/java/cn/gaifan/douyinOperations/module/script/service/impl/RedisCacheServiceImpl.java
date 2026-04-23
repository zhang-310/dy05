package cn.gaifan.douyinOperations.module.script.service.impl;

import cn.gaifan.douyinOperations.module.script.service.ScriptCacheService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class RedisCacheServiceImpl implements ScriptCacheService {
    private static final Logger logger = LoggerFactory.getLogger(RedisCacheServiceImpl.class);
    private static final String CACHE_PREFIX = "script:";

    private final RedisTemplate<String, String> redisTemplate;
    private final AtomicLong hits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);

    public RedisCacheServiceImpl(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Optional<String> getScript(String key) {
        try {
            String cacheKey = CACHE_PREFIX + key;
            String value = redisTemplate.opsForValue().get(cacheKey);
            
            if (value != null) {
                hits.incrementAndGet();
                logger.debug("缓存命中: {}", key);
                return Optional.of(value);
            } else {
                misses.incrementAndGet();
                logger.debug("缓存未命中: {}", key);
                return Optional.empty();
            }
        } catch (Exception e) {
            logger.error("获取缓存失败: {}", e.getMessage());
            misses.incrementAndGet();
            return Optional.empty();
        }
    }

    @Override
    public void cacheScript(String key, String content, long ttlSeconds) {
        try {
            String cacheKey = CACHE_PREFIX + key;
            redisTemplate.opsForValue().set(cacheKey, content, ttlSeconds, TimeUnit.SECONDS);
            logger.debug("缓存已保存: {} (TTL: {}s)", key, ttlSeconds);
        } catch (Exception e) {
            logger.error("保存缓存失败: {}", e.getMessage());
        }
    }

    @Override
    public void clearCache(String key) {
        try {
            String cacheKey = CACHE_PREFIX + key;
            redisTemplate.delete(cacheKey);
            logger.debug("缓存已清除: {}", key);
        } catch (Exception e) {
            logger.error("清除缓存失败: {}", e.getMessage());
        }
    }

    @Override
    public void clearAllCache() {
        try {
            redisTemplate.delete(redisTemplate.keys(CACHE_PREFIX + "*"));
            logger.info("所有缓存已清除");
        } catch (Exception e) {
            logger.error("清除所有缓存失败: {}", e.getMessage());
        }
    }

    @Override
    public CacheStats getStats() {
        try {
            java.util.Set<String> keys = redisTemplate.keys(CACHE_PREFIX + "*");
            long size = keys != null ? keys.size() : 0L;
            return new CacheStats(hits.get(), misses.get(), size);
        } catch (Exception e) {
            logger.error("获取缓存统计失败: {}", e.getMessage());
            return new CacheStats(0, 0, 0);
        }
    }
}
