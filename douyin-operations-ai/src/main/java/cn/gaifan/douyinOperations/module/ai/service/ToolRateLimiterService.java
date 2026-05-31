package cn.gaifan.douyinOperations.module.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * LLM 工具调用限流服务（Redis 滑动窗口）
 * 从 ai_tool_rate_limit_config 表读取配置，使用 Redis ZSET 实现滑动窗口计数。
 * Redis 不可用时回退到本地 AtomicInteger 计数（单实例限流）。
 */
@Service
public class ToolRateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(ToolRateLimiterService.class);
    private static final String RATE_KEY_PREFIX = "ai:tool:rate:";

    // 滑动窗口 Lua 脚本：ZSET 中添加当前时间戳，移除窗口外的元素，返回窗口内计数
    private static final String SLIDING_WINDOW_LUA =
            "local key = KEYS[1] " +
            "local now = tonumber(ARGV[1]) " +
            "local window = tonumber(ARGV[2]) " +
            "local limit = tonumber(ARGV[3]) " +
            "redis.call('ZREMRANGEBYSCORE', key, 0, now - window) " +
            "local count = redis.call('ZCARD', key) " +
            "if count < limit then " +
            "  redis.call('ZADD', key, now, now .. ':' .. math.random(100000)) " +
            "  redis.call('EXPIRE', key, math.ceil(window / 1000) + 10) " +
            "  return 1 " +
            "else " +
            "  return 0 " +
            "end";

    private final DefaultRedisScript<Long> slidingWindowScript;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    // 本地回退计数器（Redis 不可用时）
    private final Map<String, AtomicInteger> localCounters = new ConcurrentHashMap<>();
    private final Map<String, Long> localWindowStart = new ConcurrentHashMap<>();

    // 默认限流配置（工具名 → 每分钟最大调用数）
    private static final int DEFAULT_MAX_PER_MINUTE = 60;

    // 可通过 DB 配置覆盖的限流参数缓存
    private final Map<String, RateLimitConfig> configCache = new ConcurrentHashMap<>();

    public ToolRateLimiterService() {
        this.slidingWindowScript = new DefaultRedisScript<>(SLIDING_WINDOW_LUA, Long.class);
    }

    /**
     * 检查工具调用是否被允许（未超限）
     * @param toolName 工具名称
     * @return true=允许调用，false=已限流
     */
    public boolean tryAcquire(String toolName) {
        if (toolName == null || toolName.isBlank()) return true;

        RateLimitConfig config = configCache.getOrDefault(toolName,
                new RateLimitConfig(DEFAULT_MAX_PER_MINUTE, DEFAULT_MAX_PER_MINUTE * 10, 0));

        // 优先 Redis 滑动窗口
        if (stringRedisTemplate != null) {
            try {
                long now = System.currentTimeMillis();
                Long result = stringRedisTemplate.execute(slidingWindowScript,
                        Collections.singletonList(RATE_KEY_PREFIX + toolName),
                        String.valueOf(now),
                        String.valueOf(60_000L), // 1 分钟窗口
                        String.valueOf(config.maxPerMinute));
                boolean allowed = result != null && result == 1L;
                if (!allowed) {
                    log.warn("工具调用限流: tool={}, limit={}/min", toolName, config.maxPerMinute);
                }
                return allowed;
            } catch (Exception e) {
                log.warn("Redis 限流异常，回退到本地计数: {}", e.getMessage());
            }
        }

        // 本地回退
        return tryAcquireLocal(toolName, config.maxPerMinute);
    }

    /**
     * 注册/更新工具限流配置
     */
    public void updateConfig(String toolName, int maxPerMinute, int maxPerHour, int cooldownSeconds) {
        configCache.put(toolName, new RateLimitConfig(maxPerMinute, maxPerHour, cooldownSeconds));
    }

    /**
     * 获取工具当前窗口内的调用次数
     */
    public long getCurrentCount(String toolName) {
        if (stringRedisTemplate != null) {
            try {
                Long count = stringRedisTemplate.opsForZSet().zCard(RATE_KEY_PREFIX + toolName);
                return count != null ? count : 0;
            } catch (Exception e) {
                // fall through
            }
        }
        AtomicInteger counter = localCounters.get(toolName);
        return counter != null ? counter.get() : 0;
    }

    private boolean tryAcquireLocal(String toolName, int maxPerMinute) {
        long now = System.currentTimeMillis();
        Long windowStart = localWindowStart.get(toolName);

        if (windowStart == null || now - windowStart > 60_000L) {
            localWindowStart.put(toolName, now);
            localCounters.put(toolName, new AtomicInteger(1));
            return true;
        }

        AtomicInteger counter = localCounters.computeIfAbsent(toolName, _k -> new AtomicInteger(0));
        int current = counter.incrementAndGet();
        if (current > maxPerMinute) {
            log.warn("工具调用限流(本地): tool={}, count={}, limit={}/min", toolName, current, maxPerMinute);
            return false;
        }
        return true;
    }

    public record RateLimitConfig(int maxPerMinute, int maxPerHour, int cooldownSeconds) {}
}
