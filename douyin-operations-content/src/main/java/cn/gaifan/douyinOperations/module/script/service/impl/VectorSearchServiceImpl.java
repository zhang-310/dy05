package cn.gaifan.douyinOperations.module.script.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.script.entity.*;
import cn.gaifan.douyinOperations.module.script.repository.*;
import cn.gaifan.douyinOperations.module.script.service.VectorSearchService;
import cn.gaifan.douyinOperations.module.script.service.VectorEmbeddingService;
import cn.gaifan.douyinOperations.module.script.service.SearchSuggestionService;
import cn.gaifan.douyinOperations.module.script.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 向量搜索服务实现
 * 支持混合搜索、RRF 融合、缓存优化
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class VectorSearchServiceImpl implements VectorSearchService {

    private static final int RRF_K = 60;                    // RRF 超参数
    private static final String CACHE_PREFIX = "search:";
    private static final long CACHE_TTL = 3600;             // 缓存 1 小时

    @Resource
    private ScriptLibraryRepository scriptLibraryRepository;

    @Resource
    private ScriptVectorEmbeddingRepository scriptVectorEmbeddingRepository;

    @Resource
    private SearchResultRepository searchResultRepository;

    @Resource
    private SearchSuggestionRepository searchSuggestionRepository;

    @Resource
    private SearchAnalyticsRepository searchAnalyticsRepository;

    @Resource
    private VectorEmbeddingService vectorEmbeddingService;

    @Resource
    private SearchSuggestionService searchSuggestionService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 混合搜索：向量搜索 + BM25 + RRF 融合
     */
    @Override
    public HybridSearchResultVO hybridSearch(HybridSearchRequestVO request, Long userId) {
        if (request == null || request.getQuery() == null || request.getQuery().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "搜索查询不能为空");
        }
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "用户 ID 无效");
        }

        // 检查缓存
        String cacheKey = CACHE_PREFIX + userId + ":" + request.getQuery() + ":hybrid";
        HybridSearchResultVO cached = (HybridSearchResultVO) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("命中缓存: {}", cacheKey);
            return cached;
        }

        long startTime = System.currentTimeMillis();
        try {
            // 获取向量搜索结果
            List<Map<String, Object>> vectorResults = searchByVector(request.getQuery(), 100, userId);
            // 获取 BM25 搜索结果
            List<Map<String, Object>> bm25Results = searchByBM25(request.getQuery(), 100, userId,
                    request.getCategory(), request.getStyle());

            // RRF 融合
            Map<Long, BigDecimal> fusedScores = fuseResults(vectorResults, bm25Results,
                    request.getVectorWeight(), request.getVectorWeight());

            // 构建返回结果
            int pageStart = request.getPage() * request.getRows();
            int pageEnd = pageStart + request.getRows();
            List<Long> sortedScriptIds = fusedScores.entrySet().stream()
                    .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            List<HybridSearchResultVO.SearchItemVO> items = new ArrayList<>();
            for (int i = pageStart; i < Math.min(pageEnd, sortedScriptIds.size()); i++) {
                Long scriptId = sortedScriptIds.get(i);
                HybridSearchResultVO.SearchItemVO item = buildSearchItem(scriptId, userId);
                if (item != null) {
                    item.setHybridScore(fusedScores.get(scriptId));
                    items.add(item);
                }
            }

            long executionTime = System.currentTimeMillis() - startTime;
            HybridSearchResultVO result = HybridSearchResultVO.builder()
                    .total((long) sortedScriptIds.size())
                    .pageNum(request.getPage())
                    .pageSize(request.getRows())
                    .list(items)
                    .searchTime(executionTime)
                    .executedAt(LocalDateTime.now().toString())
                    .build();

            // 缓存结果
            redisTemplate.opsForValue().set(cacheKey, result, CACHE_TTL, java.util.concurrent.TimeUnit.SECONDS);

            // 记录搜索结果
            recordSearchResult(request.getQuery(), "HYBRID", sortedScriptIds.size(),
                    items.isEmpty() ? null : sortedScriptIds.get(0), executionTime, userId.toString(), null);

            // 更新搜索建议
            searchSuggestionService.recordSearchQuery(request.getQuery(), userId);

            return result;
        } catch (Exception e) {
            log.error("混合搜索失败: {}", request.getQuery(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "搜索失败: " + e.getMessage());
        }
    }

    /**
     * 语义搜索（纯向量搜索）
     */
    @Override
    public HybridSearchResultVO semanticSearch(String query, Integer topK, Long userId, Boolean withCrossEncoder) {
        if (query == null || query.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "查询不能为空");
        }
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "用户 ID 无效");
        }

        topK = topK == null || topK <= 0 ? 20 : Math.min(topK, 100);
        long startTime = System.currentTimeMillis();

        try {
            List<Map<String, Object>> results = searchByVector(query, topK, userId);
            List<HybridSearchResultVO.SearchItemVO> items = results.stream()
                    .map(r -> buildSearchItem((Long) r.get("scriptId"), userId))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            long executionTime = System.currentTimeMillis() - startTime;
            return HybridSearchResultVO.builder()
                    .total((long) items.size())
                    .pageNum(0)
                    .pageSize(topK)
                    .list(items)
                    .searchTime(executionTime)
                    .executedAt(LocalDateTime.now().toString())
                    .build();
        } catch (Exception e) {
            log.error("语义搜索失败: {}", query, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "搜索失败");
        }
    }

    /**
     * 关键词搜索（BM25）
     */
    @Override
    public HybridSearchResultVO lexicalSearch(String query, Integer topK, Long userId) {
        if (query == null || query.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "查询不能为空");
        }
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "用户 ID 无效");
        }

        topK = topK == null || topK <= 0 ? 20 : Math.min(topK, 100);
        long startTime = System.currentTimeMillis();

        try {
            List<Map<String, Object>> results = searchByBM25(query, topK, userId, null, null);
            List<HybridSearchResultVO.SearchItemVO> items = results.stream()
                    .map(r -> buildSearchItem((Long) r.get("scriptId"), userId))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            long executionTime = System.currentTimeMillis() - startTime;
            return HybridSearchResultVO.builder()
                    .total((long) items.size())
                    .pageNum(0)
                    .pageSize(topK)
                    .list(items)
                    .searchTime(executionTime)
                    .executedAt(LocalDateTime.now().toString())
                    .build();
        } catch (Exception e) {
            log.error("BM25 搜索失败: {}", query, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "搜索失败");
        }
    }

    /**
     * 获取搜索建议
     */
    @Override
    public SearchSuggestionVO getSearchSuggestions(String prefix, Long userId, Integer limit) {
        if (prefix == null || prefix.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "前缀不能为空");
        }
        limit = limit == null || limit <= 0 ? 10 : Math.min(limit, 50);
        return searchSuggestionService.getSuggestions(prefix, userId, limit);
    }

    /**
     * 记录搜索结果
     */
    @Override
    public void recordSearchResult(String query, String searchType, Integer totalResults, Long topResultId,
                                   Long executionTimeMs, String userId, String ipAddress) {
        try {
            SearchResult result = new SearchResult();
            result.setOwnerId(Long.parseLong(userId));
            result.setQueryText(query);
            result.setSearchType(searchType);
            result.setTotalResults(totalResults);
            result.setTopResultId(topResultId);
            result.setExecutionTimeMs(executionTimeMs != null ? executionTimeMs.intValue() : 0);
            result.setUserAgent(System.getProperty("user.agent"));
            result.setIpAddress(ipAddress);
            searchResultRepository.save(result);
        } catch (Exception e) {
            log.warn("记录搜索结果失败", e);
        }
    }

    /**
     * 记录点击反馈
     */
    @Override
    public void recordClickFeedback(Long searchResultId, Long clickedScriptId, Boolean isSatisfied) {
        try {
            SearchResult result = searchResultRepository.findByIdAndDeleted(searchResultId, 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "搜索记录不存在"));
            result.setClickedResultId(clickedScriptId);
            result.setClickedAt(new Timestamp(System.currentTimeMillis()));
            result.setIsSatisfied(isSatisfied);
            result.setFeedbackAt(new Timestamp(System.currentTimeMillis()));
            searchResultRepository.save(result);
        } catch (Exception e) {
            log.warn("记录点击反馈失败", e);
        }
    }

    /**
     * 获取搜索分析数据
     */
    @Override
    public List<SearchAnalyticsVO> getSearchAnalytics(Long userId, String startDate, String endDate) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "用户 ID 无效");
        }

        try {
            LocalDate start = LocalDate.parse(startDate);
            List<SearchAnalytics> analytics = searchAnalyticsRepository.findByOwnerAndDateRange(userId, java.sql.Date.valueOf(start));

            return analytics.stream()
                    .map(a -> SearchAnalyticsVO.builder()
                            .analyticsDate(a.getAnalyticsDate().toString())
                            .searchQuery(a.getSearchQuery())
                            .searchType(a.getSearchType())
                            .searchCount(a.getSearchCount())
                            .avgExecutionTimeMs(a.getAvgExecutionTimeMs().doubleValue())
                            .clickThroughRate(a.getClickThroughRate().doubleValue())
                            .satisfactionScore(a.getSatisfactionScore().doubleValue())
                            .topResultId(a.getTopResultId())
                            .topResultClickCount(a.getTopResultClickCount())
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取搜索分析失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 生成向量嵌入
     */
    @Override
    public byte[] generateEmbedding(String text) {
        return vectorEmbeddingService.generateEmbedding(text);
    }

    /**
     * 批量生成向量嵌入
     */
    @Override
    public List<byte[]> batchGenerateEmbeddings(List<String> texts) {
        return vectorEmbeddingService.batchGenerateEmbeddings(texts);
    }

    /**
     * 更新话术向量
     */
    @Override
    public void updateScriptVector(Long scriptId, String newTitle, String newContent, Long userId) {
        vectorEmbeddingService.updateEmbedding(scriptId, newTitle + " " + newContent, userId);
    }

    // ========== 私有方法 ==========

    /**
     * 向量搜索
     */
    private List<Map<String, Object>> searchByVector(String query, int topK, Long userId) {
        // 生成查询向量
        byte[] queryVector = generateEmbedding(query);

        // 从数据库获取用户的所有向量嵌入
        List<ScriptVectorEmbedding> embeddings = scriptVectorEmbeddingRepository
                .findByOwnerIdAndDeletedOrderByCreatedAtDesc(userId, 0);

        // 计算相似度（余弦相似度）
        return embeddings.stream()
                .map(e -> {
                    double similarity = cosineSimilarity(queryVector, e.getVectorEmbedding());
                    return Map.of(
                            "scriptId", e.getScriptId(),
                            "similarity", similarity,
                            "vectorEmbedding", e
                    );
                })
                .sorted((a, b) -> Double.compare((double) b.get("similarity"), (double) a.get("similarity")))
                .limit(topK)
                .collect(Collectors.toList());
    }

    /**
     * BM25 搜索（基于内存的简单实现）
     */
    private List<Map<String, Object>> searchByBM25(String query, int topK, Long userId,
                                                    String category, String style) {
        // 简单的关键词匹配实现（实际生产可用 Elasticsearch）
        String[] keywords = query.toLowerCase().split("\\s+");

        List<ScriptLibrary> scripts = scriptLibraryRepository.findAll((root, q, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), 0));
            predicates.add(cb.equal(root.get("userId"), userId));

            // 关键词匹配
            for (String kw : keywords) {
                if (!kw.isBlank()) {
                    String kwLower = "%" + kw + "%";
                    predicates.add(cb.or(
                            cb.like(cb.lower(root.get("title")), kwLower),
                            cb.like(cb.lower(root.get("content")), kwLower)
                    ));
                }
            }

            if (category != null && !category.isBlank()) {
                predicates.add(cb.equal(root.get("category"), category));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        }).stream().limit(topK).collect(Collectors.toList());

        return scripts.stream()
                .map(s -> Map.of(
                        "scriptId", s.getId(),
                        "bm25Score", calculateBM25Score(s.getTitle() + " " + s.getContent(), keywords),
                        "script", s
                ))
                .sorted((a, b) -> Double.compare((double) b.get("bm25Score"), (double) a.get("bm25Score")))
                .collect(Collectors.toList());
    }

    /**
     * RRF 融合
     */
    private Map<Long, BigDecimal> fuseResults(List<Map<String, Object>> vectorResults,
                                              List<Map<String, Object>> bm25Results,
                                              BigDecimal vectorWeight,
                                              BigDecimal bm25Weight) {
        Map<Long, BigDecimal> fusedScores = new HashMap<>();

        // 处理向量搜索结果
        for (int i = 0; i < vectorResults.size(); i++) {
            Long scriptId = (Long) vectorResults.get(i).get("scriptId");
            double rank = i + 1;
            double score = 60.0 / (RRF_K + rank);
            fusedScores.put(scriptId, new BigDecimal(score));
        }

        // 融合 BM25 结果
        for (int i = 0; i < bm25Results.size(); i++) {
            Long scriptId = (Long) bm25Results.get(i).get("scriptId");
            double rank = i + 1;
            double score = 60.0 / (RRF_K + rank);

            if (fusedScores.containsKey(scriptId)) {
                fusedScores.put(scriptId, fusedScores.get(scriptId).add(new BigDecimal(score)));
            } else {
                fusedScores.put(scriptId, new BigDecimal(score));
            }
        }

        return fusedScores;
    }

    /**
     * 构建搜索项目
     */
    private HybridSearchResultVO.SearchItemVO buildSearchItem(Long scriptId, Long userId) {
        try {
            ScriptLibrary script = scriptLibraryRepository.findAll((root, q, cb) -> {
                List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
                predicates.add(cb.equal(root.get("id"), scriptId));
                predicates.add(cb.equal(root.get("deleted"), 0));
                predicates.add(cb.equal(root.get("userId"), userId));
                return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
            }).stream().findFirst().orElse(null);

            if (script == null) return null;

            return HybridSearchResultVO.SearchItemVO.builder()
                    .scriptId(script.getId())
                    .title(script.getTitle())
                    .content(script.getContent())
                    .category(script.getCategory())
                    .author(String.valueOf(script.getUserId()))
                    .usageCount(script.getUseCount())
                    .createdAt(script.getCreateTime().toString())
                    .build();
        } catch (Exception e) {
            log.warn("构建搜索项目失败: {}", scriptId, e);
            return null;
        }
    }

    /**
     * 计算余弦相似度
     */
    private double cosineSimilarity(byte[] vector1, byte[] vector2) {
        if (vector1 == null || vector2 == null || vector1.length != vector2.length) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vector1.length; i += 4) {
            float f1 = bytesToFloat(vector1, i);
            float f2 = bytesToFloat(vector2, i);
            dotProduct += f1 * f2;
            norm1 += f1 * f1;
            norm2 += f2 * f2;
        }

        double denominator = Math.sqrt(norm1) * Math.sqrt(norm2);
        return denominator == 0 ? 0.0 : dotProduct / denominator;
    }

    /**
     * 字节转浮点数
     */
    private float bytesToFloat(byte[] bytes, int offset) {
        int intBits = 0;
        for (int i = 0; i < 4; i++) {
            intBits |= (bytes[offset + i] & 0xFF) << (8 * i);
        }
        return Float.intBitsToFloat(intBits);
    }

    /**
     * 计算 BM25 分数（简化版本）
     */
    private double calculateBM25Score(String text, String[] keywords) {
        double score = 0.0;
        String textLower = text.toLowerCase();
        for (String keyword : keywords) {
            if (!keyword.isBlank() && textLower.contains(keyword.toLowerCase())) {
                score += 10.0;
            }
        }
        return score;
    }
}
