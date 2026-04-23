package cn.gaifan.douyinOperations.module.script.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.script.entity.SearchResult;
import cn.gaifan.douyinOperations.module.script.entity.SearchSuggestion;
import cn.gaifan.douyinOperations.module.script.repository.SearchResultRepository;
import cn.gaifan.douyinOperations.module.script.repository.SearchSuggestionRepository;
import cn.gaifan.douyinOperations.module.script.service.SearchSuggestionService;
import cn.gaifan.douyinOperations.module.script.vo.SearchSuggestionVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 搜索建议服务实现
 * 支持自动补全、热点推荐、趋势分析
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class SearchSuggestionServiceImpl implements SearchSuggestionService {

    private static final String SUGGESTION_CACHE_PREFIX = "suggestion:";
    private static final String HOT_TOPICS_CACHE_KEY = "hot:topics";
    private static final long SUGGESTION_CACHE_TTL = 3600;  // 1 小时

    @Resource
    private SearchSuggestionRepository searchSuggestionRepository;

    @Resource
    private SearchResultRepository searchResultRepository;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 获取搜索建议（自动补全）
     */
    @Override
    public SearchSuggestionVO getSuggestions(String prefix, Long userId, Integer limit) {
        if (prefix == null || prefix.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "前缀不能为空");
        }
        limit = limit == null || limit <= 0 ? 10 : Math.min(limit, 50);

        try {
            // 检查缓存
            String cacheKey = SUGGESTION_CACHE_PREFIX + userId + ":" + prefix;
            SearchSuggestionVO cached = (SearchSuggestionVO) redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return cached;
            }

            // 获取用户历史搜索建议
            List<SearchSuggestion> userSuggestions = searchSuggestionRepository
                    .findSuggestionsByPrefix(prefix, userId, limit);

            // 获取全局建议词
            List<SearchSuggestion> globalSuggestions = searchSuggestionRepository
                    .findSuggestionsByPrefix(prefix, null, limit / 2);

            // 合并并去重
            Set<String> seenTexts = new HashSet<>();
            List<SearchSuggestionVO.SuggestionItemVO> suggestions = new ArrayList<>();

            // 先添加用户建议
            for (SearchSuggestion s : userSuggestions) {
                if (!seenTexts.contains(s.getSuggestionText())) {
                    suggestions.add(SearchSuggestionVO.SuggestionItemVO.builder()
                            .text(s.getSuggestionText())
                            .type(s.getSuggestionType())
                            .popularity(s.getSearchCount())
                            .resultCount(s.getResultCount())
                            .build());
                    seenTexts.add(s.getSuggestionText());
                }
            }

            // 再添加全局建议
            for (SearchSuggestion s : globalSuggestions) {
                if (!seenTexts.contains(s.getSuggestionText()) && suggestions.size() < limit) {
                    suggestions.add(SearchSuggestionVO.SuggestionItemVO.builder()
                            .text(s.getSuggestionText())
                            .type(s.getSuggestionType())
                            .popularity(s.getSearchCount())
                            .resultCount(s.getResultCount())
                            .build());
                    seenTexts.add(s.getSuggestionText());
                }
            }

            // 获取热点话题
            List<SearchSuggestionVO.HotTopicVO> hotTopics = getHotTopics(userId, 5);

            SearchSuggestionVO result = SearchSuggestionVO.builder()
                    .suggestions(suggestions)
                    .hotTopics(hotTopics)
                    .build();

            // 缓存结果
            redisTemplate.opsForValue().set(cacheKey, result, SUGGESTION_CACHE_TTL,
                    java.util.concurrent.TimeUnit.SECONDS);

            return result;
        } catch (Exception e) {
            log.error("获取搜索建议失败: {}", prefix, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取建议失败");
        }
    }

    /**
     * 获取热点话题
     */
    @Override
    public List<SearchSuggestionVO.HotTopicVO> getHotTopics(Long userId, Integer limit) {
        limit = limit == null || limit <= 0 ? 10 : Math.min(limit, 50);

        try {
            // 优先从缓存获取全局热点
            @SuppressWarnings("unchecked")
            List<SearchSuggestionVO.HotTopicVO> cached = (List<SearchSuggestionVO.HotTopicVO>)
                    redisTemplate.opsForValue().get(HOT_TOPICS_CACHE_KEY);
            if (cached != null) {
                return cached.stream().limit(limit).collect(Collectors.toList());
            }

            // 从数据库获取热点话题
            List<SearchSuggestion> hotTopicSuggestions = searchSuggestionRepository
                    .findByTypeOrderByTrendingScore("HOT_TOPIC", null, limit);

            List<SearchSuggestionVO.HotTopicVO> hotTopics = hotTopicSuggestions.stream()
                    .map(s -> SearchSuggestionVO.HotTopicVO.builder()
                            .topic(s.getSuggestionText())
                            .trendingScore(s.getTrendingScore().doubleValue())
                            .relatedScripts(s.getResultCount())
                            .build())
                    .collect(Collectors.toList());

            // 缓存热点话题（1 小时）
            redisTemplate.opsForValue().set(HOT_TOPICS_CACHE_KEY, hotTopics,
                    3600, java.util.concurrent.TimeUnit.SECONDS);

            return hotTopics;
        } catch (Exception e) {
            log.warn("获取热点话题失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 更新搜索建议词库
     * 从搜索记录中提取热门词
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSuggestions() {
        try {
            // 获取最近 7 天的搜索记录
            Timestamp sevenDaysAgo = new Timestamp(System.currentTimeMillis() - 7 * 24 * 3600 * 1000L);
            List<SearchResult> recentSearches = searchResultRepository.findAll((root, q, cb) -> {
                List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
                predicates.add(cb.equal(root.get("deleted"), 0));
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), sevenDaysAgo));
                return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
            });

            // 统计搜索词频率
            Map<String, Integer> queryFrequency = new HashMap<>();
            for (SearchResult sr : recentSearches) {
                queryFrequency.merge(sr.getQueryText(), 1, Integer::sum);
            }

            // 更新或创建建议词
            for (Map.Entry<String, Integer> entry : queryFrequency.entrySet()) {
                String query = entry.getKey();
                int count = entry.getValue();

                Optional<SearchSuggestion> existing = searchSuggestionRepository
                        .findGlobalSuggestion(query);

                SearchSuggestion suggestion = existing.orElse(new SearchSuggestion());
                suggestion.setSuggestionText(query);
                suggestion.setSuggestionType("HISTORY");
                suggestion.setSearchCount(count);
                suggestion.setResultCount(countResultsByQuery(query));
                suggestion.setLastSearchedAt(new Timestamp(System.currentTimeMillis()));
                suggestion.setLastUpdated(new Timestamp(System.currentTimeMillis()));

                searchSuggestionRepository.save(suggestion);
            }

            log.info("更新搜索建议词库完成: {} 条建议词", queryFrequency.size());

            // 清除缓存
            safeRedisDelete(HOT_TOPICS_CACHE_KEY);
        } catch (Exception e) {
            log.error("更新搜索建议词库失败", e);
        }
    }

    /**
     * 更新热度评分
     * 基于搜索频率、点击率、满意度综合计算
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTrendingScores() {
        try {
            List<SearchSuggestion> suggestions = searchSuggestionRepository.findAll((root, q, cb) -> {
                List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
                predicates.add(cb.equal(root.get("deleted"), 0));
                return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
            });

            for (SearchSuggestion suggestion : suggestions) {
                // 计算热度评分：searchCount * 0.5 + resultCount * 0.3 + satisfactionScore * 0.2
                double trendingScore = Math.min(1.0,
                        (suggestion.getSearchCount() / 100.0) * 0.5 +
                        (suggestion.getResultCount() / 100.0) * 0.3);

                suggestion.setTrendingScore(BigDecimal.valueOf(Math.max(0, Math.min(1, trendingScore))));
                suggestion.setLastUpdated(new Timestamp(System.currentTimeMillis()));

                searchSuggestionRepository.save(suggestion);
            }

            log.info("更新热度评分完成: {} 条建议词", suggestions.size());

            // 清除缓存
            safeRedisDelete(HOT_TOPICS_CACHE_KEY);
        } catch (Exception e) {
            log.error("更新热度评分失败", e);
        }
    }

    /**
     * 记录搜索查询
     */
    @Override
    public void recordSearchQuery(String query, Long userId) {
        try {
            Optional<SearchSuggestion> existing = userId != null ?
                    searchSuggestionRepository.findByTextAndOwner(query, userId) :
                    searchSuggestionRepository.findGlobalSuggestion(query);

            SearchSuggestion suggestion = existing.orElse(new SearchSuggestion());
            suggestion.setSuggestionText(query);
            suggestion.setOwnerId(userId);
            suggestion.setSuggestionType("HISTORY");
            suggestion.setSearchCount((suggestion.getSearchCount() == null ? 0 : suggestion.getSearchCount()) + 1);
            suggestion.setLastSearchedAt(new Timestamp(System.currentTimeMillis()));
            suggestion.setLastUpdated(new Timestamp(System.currentTimeMillis()));

            searchSuggestionRepository.save(suggestion);

            // 清除相关缓存（Redis 未启动时忽略）
            if (userId != null) {
                try {
                    redisTemplate.delete(SUGGESTION_CACHE_PREFIX + userId + "*");
                } catch (Exception ex) {
                    log.debug("清除 suggestion 缓存跳过: {}", ex.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("记录搜索查询失败: {}", query, e);
        }
    }

    /**
     * 添加自定义建议词
     */
    @Override
    public void addCustomSuggestion(String text, Long userId) {
        if (text == null || text.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "建议词不能为空");
        }

        try {
            SearchSuggestion suggestion = new SearchSuggestion();
            suggestion.setSuggestionText(text);
            suggestion.setOwnerId(userId);
            suggestion.setSuggestionType("RECOMMENDED");
            suggestion.setTrendingScore(BigDecimal.valueOf(0.5));

            searchSuggestionRepository.save(suggestion);
            log.info("添加自定义建议词: {}", text);
        } catch (Exception e) {
            log.error("添加自定义建议词失败: {}", text, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "添加建议词失败");
        }
    }

    /**
     * 删除建议词
     */
    @Override
    public void removeSuggestion(Long suggestionId) {
        if (suggestionId == null || suggestionId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "建议词 ID 无效");
        }

        try {
            SearchSuggestion suggestion = searchSuggestionRepository.findByIdAndDeleted(suggestionId, 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "建议词不存在"));
            suggestion.setDeleted(1);
            searchSuggestionRepository.save(suggestion);
            log.info("删除建议词: {}", suggestionId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除建议词失败: {}", suggestionId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除失败");
        }
    }

    // ========== 私有方法 ==========

    private void safeRedisDelete(String key) {
        if (redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.debug("Redis 不可用，跳过删除缓存键 {}: {}", key, e.getMessage());
        }
    }

    /**
     * 统计查询词的结果数
     */
    private Integer countResultsByQuery(String query) {
        try {
            long count = searchResultRepository.findAll((root, q, cb) -> {
                List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
                predicates.add(cb.equal(root.get("deleted"), 0));
                predicates.add(cb.like(root.get("queryText"), "%" + query + "%"));
                return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
            }).stream().count();
            return (int) Math.min(count, Integer.MAX_VALUE);
        } catch (Exception e) {
            log.warn("统计查询词结果数失败: {}", query, e);
            return 0;
        }
    }
}
