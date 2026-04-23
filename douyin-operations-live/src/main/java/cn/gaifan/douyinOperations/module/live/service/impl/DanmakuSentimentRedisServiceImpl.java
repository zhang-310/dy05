package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.module.live.config.LiveDanmakuSentimentProperties;
import cn.gaifan.douyinOperations.module.live.service.DanmakuSentimentService;
import cn.gaifan.douyinOperations.module.live.vo.DanmakuSentimentSnapshotVO;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis Sorted Set 的弹幕情绪滚动窗口实现（支持多实例部署）。
 *
 * <h3>数据结构设计</h3>
 * <pre>
 * Key: danmaku:sentiment:{sessionId}:{polarity}
 *      polarity: positive | negative | neutral
 * Score: 时间戳（ms）
 * Member: UUID（唯一性保证，防止重复计数）
 * </pre>
 *
 * <h3>窗口聚合</h3>
 * <p>通过 {@code ZRANGEBYSCORE} 筛选 {@code [now - windowSec * 1000, now]} 的 member 数，
 * 过期数据通过 {@code ZREMRANGEBYSCORE} 清理，无需额外 TTL 管理。
 *
 * <h3>启用条件</h3>
 * <p>当 {@code app.live.danmaku-sentiment.distributed=true} 时激活此实现，
 * 取代 {@link DanmakuSentimentServiceImpl}（进程内实现）。
 */
@Service("danmakuSentimentRedisService")
@ConditionalOnProperty(name = "app.live.danmaku-sentiment.distributed", havingValue = "true")
public class DanmakuSentimentRedisServiceImpl implements DanmakuSentimentService {

    private static final Logger log = LoggerFactory.getLogger(DanmakuSentimentRedisServiceImpl.class);

    private static final String KEY_PREFIX = "danmaku:sentiment:";
    private static final String POSITIVE = "positive";
    private static final String NEGATIVE = "negative";
    private static final String NEUTRAL = "neutral";

    private static final List<String> TRACKED_KEYWORDS = List.of(
            "太贵了", "好贵", "贵", "买不起",
            "链接在哪", "怎么买", "在哪里买", "链接",
            "质量怎么样", "质量好吗", "会不会假",
            "发货多久", "几天到", "发货快吗",
            "有优惠吗", "有折扣吗", "优惠码",
            "好用吗", "效果怎么样", "真的有效吗"
    );

    @Resource
    private LiveDanmakuSentimentProperties properties;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Value("${spring.redis.database:0}")
    private int redisDb;

    @Override
    public void ingest(Long liveSessionId, String content) {
        if (liveSessionId == null || !properties.isEnabled()) return;
        if (content == null || content.isBlank()) return;

        long now = System.currentTimeMillis();
        int polarity = classify(content.trim());
        String polarityKey = polarityLabel(polarity);
        String key = sentimentKey(liveSessionId, polarityKey);

        try {
            // member = UUID 保证唯一性
            String member = java.util.UUID.randomUUID().toString();
            redisTemplate.opsForZSet().add(key, member, now);
            // 设置 key TTL（窗口秒数 × 3 的 buffer，防止长期数据堆积）
            long ttlSecs = properties.getWindowSeconds() * 3L;
            redisTemplate.expire(key, ttlSecs, TimeUnit.SECONDS);
            // 清理过期数据
            pruneRedis(liveSessionId, now);
        } catch (Exception e) {
            log.warn("[DanmakuRedis] ingest 失败 sessionId={}: {}", liveSessionId, e.getMessage());
        }

        // 关键词频率追踪
        String trimmed = content.trim();
        for (String keyword : TRACKED_KEYWORDS) {
            if (trimmed.contains(keyword)) {
                try {
                    String kwKey = keywordKey(liveSessionId, keyword);
                    redisTemplate.opsForZSet().add(kwKey, String.valueOf(now) + "_" + java.util.UUID.randomUUID(), now);
                    redisTemplate.expire(kwKey, (long) properties.getWindowSeconds() * 3, TimeUnit.SECONDS);
                } catch (Exception e) {
                    log.debug("[DanmakuRedis] 关键词追踪失败: {}", e.getMessage());
                }
            }
        }
    }

    @Override
    public void ingestBatch(Long liveSessionId, List<String> contents) {
        if (contents == null || contents.isEmpty()) return;
        for (String c : contents) {
            ingest(liveSessionId, c);
        }
    }

    @Override
    public DanmakuSentimentSnapshotVO getSnapshot(Long liveSessionId) {
        int windowSec = properties.getWindowSeconds();
        long now = System.currentTimeMillis();
        if (liveSessionId == null || !properties.isEnabled()) {
            return emptySnapshot(windowSec, now);
        }
        try {
            pruneRedis(liveSessionId, now);
            long windowStart = now - windowSec * 1000L;
            int pos = countInWindow(sentimentKey(liveSessionId, POSITIVE), windowStart, now);
            int neg = countInWindow(sentimentKey(liveSessionId, NEGATIVE), windowStart, now);
            int neu = countInWindow(sentimentKey(liveSessionId, NEUTRAL), windowStart, now);
            int total = pos + neg + neu;
            String dominant = total == 0 ? "none" : dominantLabel(pos, neu, neg);
            return new DanmakuSentimentSnapshotVO(pos, neu, neg, total, windowSec, dominant, now);
        } catch (Exception e) {
            log.warn("[DanmakuRedis] getSnapshot 失败 sessionId={}: {}", liveSessionId, e.getMessage());
            return emptySnapshot(windowSec, now);
        }
    }

    @Override
    public Map<String, Integer> getKeywordFrequency(Long liveSessionId) {
        if (liveSessionId == null) return Collections.emptyMap();
        long now = System.currentTimeMillis();
        long windowStart = now - (long) properties.getWindowSeconds() * 1000;
        Map<String, Integer> result = new HashMap<>();
        for (String keyword : TRACKED_KEYWORDS) {
            try {
                String kwKey = keywordKey(liveSessionId, keyword);
                int count = countInWindow(kwKey, windowStart, now);
                if (count > 0) result.put(keyword, count);
            } catch (Exception e) {
                log.debug("[DanmakuRedis] 关键词查询失败: {}", e.getMessage());
            }
        }
        return result;
    }

    // ==================== 私有辅助方法 ====================

    private void pruneRedis(Long liveSessionId, long nowMillis) {
        long cutoff = nowMillis - (long) properties.getWindowSeconds() * 1000;
        for (String polarity : List.of(POSITIVE, NEGATIVE, NEUTRAL)) {
            try {
                redisTemplate.opsForZSet().removeRangeByScore(
                        sentimentKey(liveSessionId, polarity), 0, cutoff);
            } catch (Exception e) {
                log.debug("[DanmakuRedis] prune 失败: {}", e.getMessage());
            }
        }
    }

    private int countInWindow(String key, long windowStart, long now) {
        Long count = redisTemplate.opsForZSet().count(key, windowStart, now);
        return count == null ? 0 : count.intValue();
    }

    private int classify(String text) {
        for (String w : properties.getNegativeWords()) {
            if (w != null && !w.isBlank() && text.contains(w)) return -1;
        }
        for (String w : properties.getPositiveWords()) {
            if (w != null && !w.isBlank() && text.contains(w)) return 1;
        }
        return 0;
    }

    private static String polarityLabel(int polarity) {
        if (polarity == 1) return POSITIVE;
        if (polarity == -1) return NEGATIVE;
        return NEUTRAL;
    }

    private static String sentimentKey(Long sessionId, String polarity) {
        return KEY_PREFIX + sessionId + ":" + polarity;
    }

    private static String keywordKey(Long sessionId, String keyword) {
        return "danmaku:kw:" + sessionId + ":" + keyword;
    }

    private static DanmakuSentimentSnapshotVO emptySnapshot(int windowSec, long now) {
        return new DanmakuSentimentSnapshotVO(0, 0, 0, 0, windowSec, "none", now);
    }

    private static String dominantLabel(int pos, int neu, int neg) {
        int max = Math.max(pos, Math.max(neu, neg));
        if (max <= 0) return "none";
        if (neg == max) return "negative";
        if (pos == max) return "positive";
        return "neutral";
    }
}
