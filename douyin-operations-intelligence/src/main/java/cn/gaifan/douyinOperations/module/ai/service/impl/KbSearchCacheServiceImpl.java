package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.service.KbSearchCacheService;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeBaseService.SearchResult;
import com.alibaba.fastjson2.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class KbSearchCacheServiceImpl implements KbSearchCacheService {

    private static final Logger log = LoggerFactory.getLogger(KbSearchCacheServiceImpl.class);
    private static final long DEFAULT_TTL_SECONDS = 3600;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public String buildKey(Long kbId, String query, int topK) {
        return buildKey(kbId, query, topK, null);
    }

    @Override
    public String buildKey(Long kbId, String query, int topK, Long personalizeUserId) {
        return buildKey(kbId, query, topK, personalizeUserId, "GENERAL");
    }

    @Override
    public String buildKey(Long kbId, String query, int topK, Long personalizeUserId, String queryIntentTag) {
        String tag = queryIntentTag != null && !queryIntentTag.isBlank() ? queryIntentTag : "GENERAL";
        String raw = "kb:search:" + kbId + ":" + query + ":" + topK + ":u=" + personalizeUserId + ":intent=" + tag;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return "cache:kb:" + kbId + ":" + sb.substring(0, 32);
        } catch (Exception e) {
            return "cache:kb:" + kbId + ":" + raw.hashCode();
        }
    }

    @Override
    public List<SearchResult> get(String key) {
        if (stringRedisTemplate == null || key == null) return null;
        try {
            String cached = stringRedisTemplate.opsForValue().get(key);
            if (cached == null) return null;
            return JSON.parseArray(cached, SearchResult.class);
        } catch (Exception e) {
            log.debug("缓存解析失败: key={}", key, e);
            return null;
        }
    }

    @Override
    public void put(String key, List<SearchResult> results, long ttlSeconds) {
        if (stringRedisTemplate == null || key == null || results == null || results.isEmpty()) return;
        try {
            long ttl = ttlSeconds > 0 ? ttlSeconds : DEFAULT_TTL_SECONDS;
            stringRedisTemplate.opsForValue().set(key, JSON.toJSONString(results), ttl, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("缓存写入失败: key={}", key, e);
        }
    }

    @Override
    public void invalidateByKbId(Long kbId) {
        if (kbId == null || stringRedisTemplate == null) return;
        try {
            Set<String> keys = stringRedisTemplate.keys("cache:kb:" + kbId + ":*");
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
                log.debug("已清除知识库 {} 检索缓存，共 {} 条", kbId, keys.size());
            }
        } catch (Exception e) {
            log.warn("清除知识库检索缓存失败: kbId={}, {}", kbId, e.getMessage());
        }
    }

    @Override
    public void incrementStat(String type) {
        if (stringRedisTemplate == null || type == null) return;
        try {
            stringRedisTemplate.opsForValue().increment("stats:kb:cache:" + type);
        } catch (Exception ignored) {
        }
    }
}
