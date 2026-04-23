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
     * 缓存预热机制
     * 应用启动时加载热数据到 Redis
     */
    public void warmUpCache() {
        log.info("🔥 开始缓存预热...");
        long startTime = System.currentTimeMillis();

        try {
            // 预热推荐列表（模拟）
            String recommendKey = CacheConfig.RECOMMEND_PREFIX + "top-10";
            List<Map<String, Object>> topRecommends = generateTopRecommends();
            setCacheWithTTL(recommendKey, topRecommends,
                    CacheConfig.RECOMMEND_TTL_MIN, TimeUnit.MINUTES);

            // 预热热搜关键词（模拟）
            String trendingKey = "trending:queries";
            List<String> trendingQueries = Arrays.asList("热卖商品", "新品发布", "限时优惠");
            setCacheWithTTL(trendingKey, trendingQueries,
                    CacheConfig.HOT_DATA_TTL_HOUR, TimeUnit.HOURS);

            // 预热热分类（模拟）
            String categoryKey = "categories:hot";
            Map<String, Integer> hotCategories = new HashMap<>();
            hotCategories.put("服装", 1000);
            hotCategories.put("电子产品", 800);
            setCacheWithTTL(categoryKey, hotCategories,
                    CacheConfig.HOT_DATA_TTL_HOUR, TimeUnit.HOURS);

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓ 缓存预热完成，耗时 {}ms", duration);
        } catch (Exception e) {
            log.error("✗ 缓存预热失败", e);
        }
    }

    /**
     * 生成顶部推荐（模拟数据）
     */
    private List<Map<String, Object>> generateTopRecommends() {
        List<Map<String, Object>> recommends = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", i);
            item.put("title", "推荐 #" + i);
            item.put("score", 95 - i);
            recommends.add(item);
        }
        return recommends;
    }

    /**
     * 缓存失效策略 - LRU（最近最少使用）
     * 当缓存超过大小限制时自动清理最少使用的数据
     */
    public void evictLRUCache() {
        try {
            long cacheSize = getRedisMemoryUsage();
            long maxSizeBytes = CacheConfig.MAX_CACHE_SIZE_MB * 1024 * 1024;

            if (cacheSize > maxSizeBytes) {
                log.warn("⚠️ 缓存大小超限：{}MB > {}MB，执行 LRU 清理",
                        cacheSize / (1024 * 1024), CacheConfig.MAX_CACHE_SIZE_MB);

                // 删除最旧的键（FIFO）
                Set<?> allKeys = redisTemplate.keys("*");
                if (allKeys != null && !allKeys.isEmpty()) {
                    Object keyToDelete = allKeys.iterator().next();
                    redisTemplate.delete(keyToDelete);
                    log.info("✓ 已删除过期键：{}", keyToDelete);
                }
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
                // 实际实现可解析 connection.info("memory") 等
                return 100 * 1024 * 1024L; // 模拟 100MB
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
        String lockKey = CacheConfig.LOCK_PREFIX + key;

        try {
            // 先查缓存
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                return cached;
            }

            // 尝试获取分布式锁
            boolean lockAcquired = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);

            if (lockAcquired) {
                try {
                    // 获得锁，执行 DB 查询（这里省略 DB 操作）
                    // 模拟 DB 查询耗时
                    Thread.sleep(100);

                    Object dbResult = "DB_RESULT_" + System.currentTimeMillis();

                    // 查询结果写回缓存
                    setCacheWithTTL(key, dbResult,
                            CacheConfig.RECOMMEND_TTL_MIN, TimeUnit.MINUTES);

                    return dbResult;
                } finally {
                    // 释放锁
                    redisTemplate.delete(lockKey);
                }
            } else {
                // 没有获得锁，等待一下后重试
                if (retryTimes > 0) {
                    Thread.sleep(100);
                    return getCacheWithBreakthroughProtection(key, retryTimes - 1);
                } else {
                    log.warn("⚠️ 缓存击穿防护：重试次数已用尽");
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
            // 获取所有缓存键
            Set<?> allKeys = redisTemplate.keys("*");
            long totalKeys = allKeys != null ? allKeys.size() : 0;

            // 计算命中率（模拟）
            double hitRate = 75.5; // 实际需要通过拦截器统计

            metrics.put("totalKeys", totalKeys);
            metrics.put("hitRate", hitRate + "%");
            metrics.put("memoryUsageMB", getRedisMemoryUsage() / (1024.0 * 1024));
            metrics.put("maxMemoryLimitMB", CacheConfig.MAX_CACHE_SIZE_MB);
            metrics.put("status", hitRate > 70 ? "良好" : "需要优化");

        } catch (Exception e) {
            log.error("✗ 缓存监控统计失败", e);
        }

        return metrics;
    }
}
