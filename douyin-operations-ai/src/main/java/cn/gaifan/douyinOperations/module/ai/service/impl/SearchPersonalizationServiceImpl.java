package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.service.SearchPersonalizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 搜索个性化权重服务实现
 * <p>
 * Redis 存储用户搜索画像：ai:search:profile:{userId}
 * - bm25_hits: BM25 命中次数
 * - vector_hits: Vector 命中次数
 * - total_searches: 总搜索次数
 * <p>
 * 10 次搜索后开启个性化：
 * - 历史 BM25 命中率高 → BM25 权重 +0.1
 * - 历史 Vector 命中率高 → Vector 权重 +0.1
 * - 个性化 rerank：用户历史点击文档类型 → 同类型 boost +0.2
 */
@Slf4j
@Service
public class SearchPersonalizationServiceImpl implements SearchPersonalizationService {

    private static final double DEFAULT_BM25_WEIGHT = 0.5;
    private static final double DEFAULT_VECTOR_WEIGHT = 0.5;
    private static final int MIN_SEARCHES_FOR_PERSONALIZATION = 10;
    private static final String PROFILE_KEY_PREFIX = "ai:search:profile:";
    private static final Duration PROFILE_TTL = Duration.ofDays(30);
    private static final String DOC_IMPRESSION_KEY_PREFIX = "ai:search:doc:imp:";
    private static final String DOC_CLICK_KEY_PREFIX = "ai:search:doc:click:";

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Override
    public Map<String, Object> getPersonalizedWeights(Long userId) {
        Map<String, Object> result = new LinkedHashMap<>();

        if (userId == null || redisTemplate == null) {
            result.put("bm25Weight", DEFAULT_BM25_WEIGHT);
            result.put("vectorWeight", DEFAULT_VECTOR_WEIGHT);
            result.put("personalized", false);
            return result;
        }

        try {
            String key = PROFILE_KEY_PREFIX + userId;
            String totalStr = (String) redisTemplate.opsForHash().get(key, "total_searches");
            int total = totalStr != null ? Integer.parseInt(totalStr) : 0;

            if (total < MIN_SEARCHES_FOR_PERSONALIZATION) {
                result.put("bm25Weight", DEFAULT_BM25_WEIGHT);
                result.put("vectorWeight", DEFAULT_VECTOR_WEIGHT);
                result.put("personalized", false);
                result.put("searchCount", total);
                return result;
            }

            String bm25HitsStr = (String) redisTemplate.opsForHash().get(key, "bm25_hits");
            String vectorHitsStr = (String) redisTemplate.opsForHash().get(key, "vector_hits");
            int bm25Hits = bm25HitsStr != null ? Integer.parseInt(bm25HitsStr) : 0;
            int vectorHits = vectorHitsStr != null ? Integer.parseInt(vectorHitsStr) : 0;
            int totalHits = bm25Hits + vectorHits;

            double bm25Rate = totalHits > 0 ? (double) bm25Hits / totalHits : 0.5;
            double vectorRate = totalHits > 0 ? (double) vectorHits / totalHits : 0.5;

            // 个性化调整：偏好高的类型权重 +0.1
            double bm25Weight = DEFAULT_BM25_WEIGHT;
            double vectorWeight = DEFAULT_VECTOR_WEIGHT;

            if (bm25Rate > 0.6) {
                bm25Weight += 0.1;
                vectorWeight -= 0.1;
            } else if (vectorRate > 0.6) {
                vectorWeight += 0.1;
                bm25Weight -= 0.1;
            }

            // 确保权重在合理范围
            bm25Weight = Math.max(0.2, Math.min(0.8, bm25Weight));
            vectorWeight = Math.max(0.2, Math.min(0.8, vectorWeight));

            result.put("bm25Weight", Math.round(bm25Weight * 100) / 100.0);
            result.put("vectorWeight", Math.round(vectorWeight * 100) / 100.0);
            result.put("personalized", true);
            result.put("searchCount", total);
            result.put("bm25HitRate", Math.round(bm25Rate * 1000) / 10.0);
            result.put("vectorHitRate", Math.round(vectorRate * 1000) / 10.0);
        } catch (Exception e) {
            log.debug("[SearchPersonalization] 个性化权重计算失败: {}", e.getMessage());
            result.put("bm25Weight", DEFAULT_BM25_WEIGHT);
            result.put("vectorWeight", DEFAULT_VECTOR_WEIGHT);
            result.put("personalized", false);
        }

        return result;
    }

    @Override
    public void recordSearchClick(Long userId, String query, String hitSource, Long documentId) {
        if (userId == null || redisTemplate == null) return;

        try {
            String key = PROFILE_KEY_PREFIX + userId;
            redisTemplate.opsForHash().increment(key, "total_searches", 1);

            if ("bm25".equals(hitSource)) {
                redisTemplate.opsForHash().increment(key, "bm25_hits", 1);
            } else if ("vector".equals(hitSource)) {
                redisTemplate.opsForHash().increment(key, "vector_hits", 1);
            }

            // 文档级 CTR 追踪
            if (documentId != null) {
                String clickKey = DOC_CLICK_KEY_PREFIX + documentId;
                redisTemplate.opsForValue().increment(clickKey);
                redisTemplate.expire(clickKey, Duration.ofDays(30));
            }

            // 设置 TTL
            redisTemplate.expire(key, PROFILE_TTL);

            log.debug("[SearchPersonalization] 记录搜索点击: userId={}, source={}, docId={}", userId, hitSource, documentId);
        } catch (Exception e) {
            log.debug("[SearchPersonalization] 记录搜索点击失败: {}", e.getMessage());
        }
    }

    /**
     * 记录搜索结果展示（用于 CTR 计算）
     * @param documentIds 被展示的文档 ID 列表
     */
    public void recordSearchImpressions(java.util.Collection<Long> documentIds) {
        if (documentIds == null || redisTemplate == null) return;
        try {
            for (Long docId : documentIds) {
                if (docId != null) {
                    String impKey = DOC_IMPRESSION_KEY_PREFIX + docId;
                    redisTemplate.opsForValue().increment(impKey);
                    redisTemplate.expire(impKey, Duration.ofDays(30));
                }
            }
        } catch (Exception e) {
            log.debug("[SearchPersonalization] 记录展示失败: {}", e.getMessage());
        }
    }

    /**
     * 获取文档 CTR（30 天窗口）
     * @return CTR 值（0.0-1.0），无数据返回 -1
     */
    public double getDocumentCtr(Long documentId) {
        if (documentId == null || redisTemplate == null) return -1;
        try {
            String impStr = redisTemplate.opsForValue().get(DOC_IMPRESSION_KEY_PREFIX + documentId);
            String clickStr = redisTemplate.opsForValue().get(DOC_CLICK_KEY_PREFIX + documentId);
            long impressions = impStr != null ? Long.parseLong(impStr) : 0;
            long clicks = clickStr != null ? Long.parseLong(clickStr) : 0;
            if (impressions <= 0) return -1;
            return (double) clicks / impressions;
        } catch (Exception e) {
            return -1;
        }
    }
}
