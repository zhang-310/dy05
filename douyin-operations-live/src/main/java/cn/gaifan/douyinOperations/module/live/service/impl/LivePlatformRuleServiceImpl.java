package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.module.live.entity.LivePlatform;
import cn.gaifan.douyinOperations.module.live.entity.LiveViolationRule;
import cn.gaifan.douyinOperations.module.live.repository.LivePlatformRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveViolationRuleRepository;
import cn.gaifan.douyinOperations.module.live.service.LivePlatformRuleService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LivePlatformRuleServiceImpl implements LivePlatformRuleService {

    private static final Logger log = LoggerFactory.getLogger(LivePlatformRuleServiceImpl.class);
    private static final long CACHE_TTL_MS = 5 * 60 * 1000L;

    @Resource private LivePlatformRepository platformRepository;
    @Resource private LiveViolationRuleRepository violationRuleRepository;

    private final Map<String, CacheEntry<List<LiveViolationRule>>> ruleCache = new ConcurrentHashMap<>();
    private volatile CacheEntry<List<LivePlatform>> platformCache;

    @Override
    public List<LivePlatform> listActivePlatforms() {
        if (platformCache != null && !platformCache.isExpired()) {
            return platformCache.value;
        }
        List<LivePlatform> platforms = platformRepository.findByActiveAndDeleted(1, 0);
        platformCache = new CacheEntry<>(platforms);
        return platforms;
    }

    @Override
    public LivePlatform getPlatformByCode(String platformCode) {
        if (platformCode == null || platformCode.isBlank()) return null;
        return platformRepository.findByPlatformCodeAndDeleted(platformCode, 0).orElse(null);
    }

    @Override
    public List<LiveViolationRule> getViolationRules(String platformCode) {
        if (platformCode == null || platformCode.isBlank()) return Collections.emptyList();
        CacheEntry<List<LiveViolationRule>> cached = ruleCache.get(platformCode);
        if (cached != null && !cached.isExpired()) return cached.value;

        LivePlatform platform = getPlatformByCode(platformCode);
        if (platform == null) return Collections.emptyList();

        List<LiveViolationRule> rules = violationRuleRepository
                .findByPlatformIdAndActiveAndDeleted(platform.getId(), 1, 0);
        ruleCache.put(platformCode, new CacheEntry<>(rules));
        return rules;
    }

    @Override
    public List<Map<String, Object>> checkPlatformViolation(String text, String platformCode) {
        if (text == null || text.isBlank()) return Collections.emptyList();
        List<LiveViolationRule> rules = getViolationRules(platformCode);
        List<Map<String, Object>> hits = new ArrayList<>();
        String lower = text.toLowerCase();

        for (LiveViolationRule rule : rules) {
            String word = rule.getWord().toLowerCase();
            int idx = lower.indexOf(word);
            while (idx >= 0) {
                Map<String, Object> hit = new LinkedHashMap<>();
                hit.put("word", rule.getWord());
                hit.put("position", idx);
                hit.put("length", rule.getWord().length());
                hit.put("level", rule.getLevel());
                hit.put("reason", rule.getReason());
                hit.put("replacement", rule.getReplacement());
                hit.put("category", rule.getCategory());
                hits.add(hit);
                idx = lower.indexOf(word, idx + 1);
            }
        }
        return hits;
    }

    @Override
    public String getPlatformPromptTemplate(String platformCode) {
        LivePlatform platform = getPlatformByCode(platformCode);
        return platform != null ? platform.getPromptTemplate() : null;
    }

    private static class CacheEntry<T> {
        final T value;
        final long createdAt;

        CacheEntry(T value) {
            this.value = value;
            this.createdAt = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - createdAt > CACHE_TTL_MS;
        }
    }
}
