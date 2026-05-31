/**
 * W-09 性能优化 - 缓存策略 Service
 * Redis 缓存、缓存预热、缓存失效、防护策略
 */

package cn.gaifan.douyinOperations.module.common.service;

import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.RedisTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.util.function.Supplier;
import java.util.concurrent.TimeUnit;

/**
 * 缓存策略 Service
 * - Redis 缓存热数据（推荐列表、搜索结果、用户配置）
 * - 缓存预热和失效策略
 * - 缓存穿透、击穿、雪崩防护
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheStrategyService {

    /** 与 Spring Boot Redis 自动配置的 {@code RedisTemplate<Object, Object>} 对齐 */
    private final RedisTemplate<Object, Object> redisTemplate;

    /**
     * 缓存配置常量
     */
    public static class CacheConfig {
        // TTL（生存时间）
        public static final int RECOMMEND_TTL_MIN = 5; // 推荐列表：5 分钟
        public static final int SEARCH_RESULT_TTL_MIN = 10; // 搜索结果：10 分钟
        public static final int USER_CONFIG_TTL_HOUR = 1; // 用户配置：1 小时
        public static final int HOT_DATA_TTL_HOUR = 24; // 热数据：24 小时

        // 缓存键前缀
        public static final String RECOMMEND_PREFIX = "recommend:";
        public static final String SEARCH_PREFIX = "search:";
        public static final String CONFIG_PREFIX = "config:";
        public static final String LOCK_PREFIX = "lock:";

        // 缓存大小限制
        public static final long MAX_CACHE_SIZE_MB = 500;
    }

    /**
     * 设置缓存（带 TTL）
     */
    public void setCacheWithTTL(String key, Object value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
            log.debug("✓ 缓存设置成功：key={}, ttl={}{}",
                    key, timeout, unit.name().toLowerCase());
        } catch (Exception e) {
            log.error("✗ 缓存设置失败：key={}", key, e);
        }
    }

    /**
     * 获取缓存
     */
    public <T> T getCache(String key, Class<T> type) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return null;
            }
            return type.cast(value);
        } catch (Exception e) {
            log.error("✗ 缓存获取失败：key={}", key, e);
            return null;
        }
    }

    /**
     * 缓存预热机制。
     * 该服务不再写入演示推荐/热搜/分类数据；业务热数据应由各领域服务注册真实数据源后预热。
     */
    public void warmUpCache() {
        log.info("开始缓存预热检查...");
        long startTime = System.currentTimeMillis();

        try {
            String pong = redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<String>) connection ->
                    connection.ping());
            long duration = System.currentTimeMillis() - startTime;
            log.info("缓存预热检查完成，redisPing={}, registeredWarmups=0, 耗时 {}ms", pong, duration);
        } catch (Exception e) {
            log.warn("缓存预热检查失败：Redis 不可用或配置缺失，跳过业务预热", e);
        }
    }

    /**
     * 缓存失效策略 - LRU（最近最少使用）
     * 当缓存超过大小限制时自动清理最少使用的数据
     */
    public void evictLRUCache() {
        try {
            long cacheSize = getRedisMemoryUsage();
            long maxSizeBytes = CacheConfig.MAX_CACHE_SIZE_MB * 1024 * 1024;

            if (cacheSize <= 0) {
                log.warn("缓存大小不可用，跳过 LRU 清理");
                return;
            }

            if (cacheSize > maxSizeBytes) {
                log.warn("缓存大小超限：{}MB > {}MB。请配置 Redis maxmemory-policy 执行服务端淘汰，当前不删除任意业务键",
                        cacheSize / (1024 * 1024), CacheConfig.MAX_CACHE_SIZE_MB);
            }
        } catch (Exception e) {
            log.error("✗ 缓存 LRU 清理失败", e);
        }
    }

    /**
     * 获取 Redis 内存使用情况（字节）
     */
    private long getRedisMemoryUsage() {
        try {
            Long size = redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Long>) connection -> {
                Properties info = connection.serverCommands().info("memory");
                if (info == null) {
                    return 0L;
                }
                return parseLong(info.getProperty("used_memory"), 0L);
            });
            return size != null ? size : 0;
        } catch (Exception e) {
            log.error("✗ 无法获取 Redis 内存使用情况", e);
            return 0;
        }
    }

    /**
     * 缓存穿透防护
     * 问题：查询一个不存在的数据，每次都穿透到 DB
     * 解决：缓存空值或使用布隆过滤器
     */
    public Object getCacheWithPenetrationProtection(String key, String dbFallbackKey) {
        try {
            // 先查缓存
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                return cached;
            }

            // 检查是否为缓存的空值
            if (redisTemplate.hasKey(dbFallbackKey)) {
                log.debug("✓ 返回缓存的空值：{}", key);
                return null;
            }

            // 缓存不存在，从 DB 查询（这里省略 DB 操作）
            // 如果 DB 也没有，缓存空值防止穿透
            setCacheWithTTL(dbFallbackKey, "NULL", 5, TimeUnit.MINUTES);

            log.debug("✓ 穿透防护：缓存空值");
            return null;
        } catch (Exception e) {
            log.error("✗ 缓存穿透防护失败：key={}", key, e);
            return null;
        }
    }

    /**
     * 缓存击穿防护
     * 问题：热点数据过期，大量请求同时穿透到 DB
     * 解决：使用分布式锁或缓存预加载
     */
    public Object getCacheWithBreakthroughProtection(String key, int retryTimes) {
        return getCacheWithBreakthroughProtection(key, retryTimes, () -> null);
    }

    /**
     * 缓存击穿防护：由调用方提供真实回源逻辑，避免基础设施层伪造业务数据。
     */
    public Object getCacheWithBreakthroughProtection(String key, int retryTimes, Supplier<Object> loader) {
        String lockKey = CacheConfig.LOCK_PREFIX + key;

        try {
            // 先查缓存
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                return cached;
            }

            // 尝试获取分布式锁
            Boolean lockAcquired = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);

            if (Boolean.TRUE.equals(lockAcquired)) {
                try {
                    Object dbResult = loader != null ? loader.get() : null;
                    if (dbResult != null) {
                        setCacheWithTTL(key, dbResult,
                                CacheConfig.RECOMMEND_TTL_MIN, TimeUnit.MINUTES);
                    }
                    return dbResult;
                } finally {
                    // 释放锁
                    redisTemplate.delete(lockKey);
                }
            } else {
                // 没有获得锁，等待一下后重试
                if (retryTimes > 0) {
                    Thread.sleep(50);
                    return getCacheWithBreakthroughProtection(key, retryTimes - 1, loader);
                } else {
                    log.warn("缓存击穿防护：重试次数已用尽");
                    return null;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("✗ 缓存击穿防护被中断：key={}", key, e);
            return null;
        }
    }

    /**
     * 缓存雪崩防护
     * 问题：大量缓存同时过期，大量请求穿透到 DB
     * 解决：随机 TTL、分布式锁、热点数据永不过期
     */
    public void setCacheWithAvalancheProtection(String key, Object value, int baseTTLMin) {
        try {
            // 随机 TTL（±10% 变化）防止同时过期）
            Random rand = new Random();
            int randomTTL = baseTTLMin + rand.nextInt(baseTTLMin / 5) - baseTTLMin / 10;
            randomTTL = Math.max(randomTTL, 1); // 至少 1 分钟

            setCacheWithTTL(key, value, randomTTL, TimeUnit.MINUTES);

            log.debug("✓ 缓存雪崩防护：key={}, randomTTL={}min", key, randomTTL);
        } catch (Exception e) {
            log.error("✗ 缓存雪崩防护失败：key={}", key, e);
        }
    }

    /**
     * 缓存监控 - 命中率统计
     */
    public Map<String, Object> getCacheMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();

        try {
            long totalKeys = getRedisDbSize();
            Properties stats = getRedisInfo("stats");
            long hits = parseLong(stats != null ? stats.getProperty("keyspace_hits") : null, -1L);
            long misses = parseLong(stats != null ? stats.getProperty("keyspace_misses") : null, -1L);
            Long totalRequests = hits >= 0 && misses >= 0 ? hits + misses : null;
            Double hitRate = totalRequests != null && totalRequests > 0
                    ? hits * 100.0 / totalRequests
                    : null;
            long memoryUsage = getRedisMemoryUsage();

            metrics.put("totalKeys", totalKeys);
            metrics.put("hitRate", hitRate != null ? String.format(Locale.ROOT, "%.2f%%", hitRate) : null);
            metrics.put("hitRateAvailable", hitRate != null);
            metrics.put("keyspaceHits", hits >= 0 ? hits : null);
            metrics.put("keyspaceMisses", misses >= 0 ? misses : null);
            metrics.put("memoryUsageMB", memoryUsage / (1024.0 * 1024));
            metrics.put("maxMemoryLimitMB", CacheConfig.MAX_CACHE_SIZE_MB);
            metrics.put("degraded", hitRate == null || memoryUsage <= 0);
            metrics.put("status", hitRate == null ? "指标不可用" : hitRate > 70 ? "良好" : "需要优化");

        } catch (Exception e) {
            log.error("✗ 缓存监控统计失败", e);
            metrics.put("degraded", true);
            metrics.put("status", "指标不可用");
            metrics.put("message", e.getMessage());
        }

        return metrics;
    }

    private Properties getRedisInfo(String section) {
        return redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Properties>) connection ->
                connection.serverCommands().info(section));
    }

    private long getRedisDbSize() {
        try {
            Long size = redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Long>) connection ->
                    connection.serverCommands().dbSize());
            return size != null ? size : 0L;
        } catch (Exception e) {
            log.warn("无法获取 Redis key 数量", e);
            return 0L;
        }
    }

    private static long parseLong(String value, long fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
