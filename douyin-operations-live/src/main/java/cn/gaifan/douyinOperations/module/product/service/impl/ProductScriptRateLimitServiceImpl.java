package cn.gaifan.douyinOperations.module.product.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.product.service.ProductScriptRateLimitService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 产品话术生成限流：每用户 60 秒内最多 5 次
 * 优先 Redis 滑动窗口（分布式），Redis 不可用时降级 Caffeine 本地
 */
@Service
public class ProductScriptRateLimitServiceImpl implements ProductScriptRateLimitService {

    private static final Logger log = LoggerFactory.getLogger(ProductScriptRateLimitServiceImpl.class);
    private static final int MAX_PER_WINDOW = 5;
    private static final long WINDOW_SECONDS = 60;
    private static final String REDIS_KEY_PREFIX = "script_gen:rate:";

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    /** Caffeine 降级（Redis 不可用时） */
    private final Cache<String, AtomicInteger> caffeineCache = Caffeine.newBuilder()
            .expireAfterWrite(2, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build();

    @Override
    public void tryAcquire(Long userId) {
        if (userId == null) return;
        if (stringRedisTemplate != null) {
            try {
                tryAcquireRedis(userId);
                return;
            } catch (Exception e) {
                log.warn("Redis 限流失败，降级 Caffeine: {}", e.getMessage());
            }
        }
        tryAcquireCaffeine(userId);
    }

    /** Redis 滑动窗口：ZADD + ZREMRANGEBYSCORE + ZCARD */
    private void tryAcquireRedis(Long userId) {
        String key = REDIS_KEY_PREFIX + userId;
        long now = System.currentTimeMillis();
        long windowStart = now - WINDOW_SECONDS * 1000;
        String member = UUID.randomUUID().toString();

        var zSet = stringRedisTemplate.opsForZSet();
        zSet.removeRangeByScore(key, Double.NEGATIVE_INFINITY, windowStart);
        zSet.add(key, member, now);
        Long count = zSet.size(key);
        if (count != null && count > MAX_PER_WINDOW) {
            zSet.remove(key, member);
            throw new BusinessException(ErrorCode.PRODUCT_SCRIPT_RATE_LIMITED,
                    "生成频率超限，" + WINDOW_SECONDS + " 秒内最多 " + MAX_PER_WINDOW + " 次，请稍后再试");
        }
        stringRedisTemplate.expire(key, WINDOW_SECONDS + 10, TimeUnit.SECONDS);
    }

    /** Caffeine 固定窗口降级 */
    private void tryAcquireCaffeine(Long userId) {
        String key = key(userId);
        AtomicInteger counter = caffeineCache.get(key, k -> new AtomicInteger(0));
        int v = counter.incrementAndGet();
        if (v > MAX_PER_WINDOW) {
            throw new BusinessException(ErrorCode.PRODUCT_SCRIPT_RATE_LIMITED,
                    "生成频率超限，每分钟最多 " + MAX_PER_WINDOW + " 次，请稍后再试");
        }
    }

    private static String key(Long userId) {
        return "script_gen:" + userId + ":" + (System.currentTimeMillis() / 60_000);
    }
}
