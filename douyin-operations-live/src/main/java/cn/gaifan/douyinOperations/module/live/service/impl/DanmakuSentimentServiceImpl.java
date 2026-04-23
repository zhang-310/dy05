package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.module.live.config.LiveDanmakuSentimentProperties;
import cn.gaifan.douyinOperations.module.live.entity.LiveDanmakuRecord;
import cn.gaifan.douyinOperations.module.live.repository.LiveDanmakuRecordRepository;
import cn.gaifan.douyinOperations.module.live.service.DanmakuSentimentService;
import cn.gaifan.douyinOperations.module.live.vo.DanmakuSentimentSnapshotVO;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 进程内滚动窗口实现（单实例/开发环境默认）。
 * <p>多实例部署时应启用 Redis 分布式实现：设置 {@code app.live.danmaku-sentiment.distributed=true}。
 * 参见 {@link DanmakuSentimentRedisServiceImpl}。
 */
@Service("danmakuSentimentInMemoryService")
@Primary
@ConditionalOnProperty(name = "app.live.danmaku-sentiment.distributed", havingValue = "false", matchIfMissing = true)
public class DanmakuSentimentServiceImpl implements DanmakuSentimentService {

    private static final Logger log = LoggerFactory.getLogger(DanmakuSentimentServiceImpl.class);

    private static final int POLARITY_NEGATIVE = -1;
    private static final int POLARITY_NEUTRAL = 0;
    private static final int POLARITY_POSITIVE = 1;

    @Resource
    private LiveDanmakuSentimentProperties properties;

    @Autowired(required = false)
    private LiveDanmakuRecordRepository danmakuRecordRepository;

    private final ConcurrentHashMap<Long, ArrayDeque<Sample>> buffers = new ConcurrentHashMap<>();

    // P2-1: 关键词频率追踪（关键词 → 时间戳队列，用于统计窗口内高频词）
    private static final List<String> TRACKED_KEYWORDS = List.of(
            "太贵了", "好贵", "贵", "买不起",
            "链接在哪", "怎么买", "在哪里买", "链接",
            "质量怎么样", "质量好吗", "会不会假",
            "发货多久", "几天到", "发货快吗",
            "有优惠吗", "有折扣吗", "优惠码",
            "好用吗", "效果怎么样", "真的有效吗"
    );
    private final ConcurrentHashMap<Long, ConcurrentHashMap<String, ArrayDeque<Long>>> keywordBuffers = new ConcurrentHashMap<>();

    private static final class Sample {
        final long tsMillis;
        final int polarity;

        Sample(long tsMillis, int polarity) {
            this.tsMillis = tsMillis;
            this.polarity = polarity;
        }
    }

    @Override
    public void ingest(Long liveSessionId, String content) {
        if (liveSessionId == null || !properties.isEnabled()) {
            return;
        }
        if (content == null || content.isBlank()) {
            return;
        }
        long now = System.currentTimeMillis();
        int polarity = classify(content.trim());
        buffers.computeIfAbsent(liveSessionId, k -> new ArrayDeque<>()).addLast(new Sample(now, polarity));
        prune(liveSessionId, now);

        // P2-1: 追踪关键词频率
        String trimmed = content.trim();
        for (String keyword : TRACKED_KEYWORDS) {
            if (trimmed.contains(keyword)) {
                keywordBuffers.computeIfAbsent(liveSessionId, k -> new ConcurrentHashMap<>())
                        .computeIfAbsent(keyword, k -> new ArrayDeque<>())
                        .addLast(now);
            }
        }
        pruneKeywords(liveSessionId, now);
    }

    @Override
    public void ingestBatch(Long liveSessionId, List<String> contents) {
        if (contents == null || contents.isEmpty()) {
            return;
        }
        for (String c : contents) {
            ingest(liveSessionId, c);
        }
    }

    @Override
    public DanmakuSentimentSnapshotVO getSnapshot(Long liveSessionId) {
        long now = System.currentTimeMillis();
        int windowSec = properties.getWindowSeconds();
        if (liveSessionId == null || !properties.isEnabled()) {
            return emptySnapshot(windowSec, now);
        }
        prune(liveSessionId, now);
        ArrayDeque<Sample> q = buffers.get(liveSessionId);
        int pos = 0, neu = 0, neg = 0;
        if (q != null) {
            for (Sample s : q) {
                if (s.polarity == POLARITY_POSITIVE) {
                    pos++;
                } else if (s.polarity == POLARITY_NEGATIVE) {
                    neg++;
                } else {
                    neu++;
                }
            }
        }
        int total = pos + neu + neg;
        String dominant = total == 0 ? "none" : dominantLabel(pos, neu, neg);
        return new DanmakuSentimentSnapshotVO(pos, neu, neg, total, windowSec, dominant, now);
    }

    private void prune(Long liveSessionId, long nowMillis) {
        long cutoff = nowMillis - properties.getWindowSeconds() * 1000L;
        ArrayDeque<Sample> q = buffers.get(liveSessionId);
        if (q == null) {
            return;
        }
        while (!q.isEmpty() && q.peekFirst().tsMillis < cutoff) {
            q.pollFirst();
        }
        if (q.isEmpty()) {
            buffers.remove(liveSessionId, q);
        }
    }

    private void pruneKeywords(Long liveSessionId, long nowMillis) {
        long cutoff = nowMillis - properties.getWindowSeconds() * 1000L;
        var kwMap = keywordBuffers.get(liveSessionId);
        if (kwMap == null) return;
        kwMap.forEach((_kw, q) -> {
            while (!q.isEmpty() && q.peekFirst() < cutoff) {
                q.pollFirst();
            }
        });
    }

    @Override
    public Map<String, Integer> getKeywordFrequency(Long liveSessionId) {
        if (liveSessionId == null) return Collections.emptyMap();
        long now = System.currentTimeMillis();
        pruneKeywords(liveSessionId, now);
        var kwMap = keywordBuffers.get(liveSessionId);
        if (kwMap == null || kwMap.isEmpty()) return Collections.emptyMap();
        Map<String, Integer> result = new HashMap<>();
        kwMap.forEach((kw, q) -> {
            if (!q.isEmpty()) result.put(kw, q.size());
        });
        return result;
    }

    private DanmakuSentimentSnapshotVO emptySnapshot(int windowSec, long now) {
        return new DanmakuSentimentSnapshotVO(0, 0, 0, 0, windowSec, "none", now);
    }

    /**
     * 并列时优先负向（风险提示），其次正向，再次中性。
     */
    private static String dominantLabel(int pos, int neu, int neg) {
        int max = Math.max(pos, Math.max(neu, neg));
        if (max <= 0) {
            return "none";
        }
        if (neg == max) {
            return "negative";
        }
        if (pos == max) {
            return "positive";
        }
        return "neutral";
    }

    /**
     * 负向优先（同句同时命中正负词时按负向计）
     */
    private int classify(String text) {
        for (String w : properties.getNegativeWords()) {
            if (w != null && !w.isBlank() && text.contains(w)) {
                return POLARITY_NEGATIVE;
            }
        }
        for (String w : properties.getPositiveWords()) {
            if (w != null && !w.isBlank() && text.contains(w)) {
                return POLARITY_POSITIVE;
            }
        }
        return POLARITY_NEUTRAL;
    }

    // ─── 弹幕持久化（Phase 1.5 升级） ──────────────────────────────

    /**
     * 持久化弹幕记录（去重）
     */
    public void persistDanmaku(Long sessionId, String content, String sentiment, String authorNickname,
                                String douyinCommentId, java.sql.Timestamp danmakuTime) {
        if (danmakuRecordRepository == null) return;
        // 去重检查
        if (douyinCommentId != null && !douyinCommentId.isBlank()) {
            var existing = danmakuRecordRepository.findByDouyinCommentIdAndDeleted(douyinCommentId, 0);
            if (existing.isPresent()) return;
        }
        try {
            LiveDanmakuRecord record = new LiveDanmakuRecord();
            record.setSessionId(sessionId);
            record.setContent(content);
            record.setSentiment(sentiment != null ? sentiment : classifyToLabel(content));
            record.setAuthorNickname(authorNickname);
            record.setDouyinCommentId(douyinCommentId);
            record.setDanmakuTime(danmakuTime != null ? danmakuTime : new java.sql.Timestamp(System.currentTimeMillis()));
            danmakuRecordRepository.save(record);
        } catch (Exception e) {
            log.warn("弹幕持久化失败: sessionId={}, content={}", sessionId, content, e);
        }
    }

    /**
     * 批量持久化弹幕
     */
    public void persistDanmakuBatch(Long sessionId, List<Map<String, String>> danmakuList) {
        if (danmakuRecordRepository == null || danmakuList == null) return;
        for (Map<String, String> d : danmakuList) {
            persistDanmaku(sessionId,
                    d.getOrDefault("content", ""),
                    d.get("sentiment"),
                    d.get("authorNickname"),
                    d.get("douyinCommentId"),
                    null);
        }
    }

    /**
     * 简易情感标签分类（复用词典分类逻辑，返回字符串标签）
     */
    private String classifyToLabel(String text) {
        if (text == null || text.isBlank()) return "neutral";
        int polarity = classify(text.trim());
        if (polarity == POLARITY_NEGATIVE) return "negative";
        if (polarity == POLARITY_POSITIVE) return "positive";
        return "neutral";
    }
}
