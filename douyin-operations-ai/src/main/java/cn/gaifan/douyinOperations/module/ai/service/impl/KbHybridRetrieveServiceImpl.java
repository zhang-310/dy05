package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.module.ai.config.SearchMetricsCollector;
import cn.gaifan.douyinOperations.module.ai.entity.AiKbDocument;
import cn.gaifan.douyinOperations.module.ai.entity.AiKnowledgeBase;
import cn.gaifan.douyinOperations.module.ai.entity.AiUserCognitiveProfile;
import cn.gaifan.douyinOperations.module.ai.search.QueryIntent;
import cn.gaifan.douyinOperations.module.ai.repository.AiKbDocumentRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiUserCognitiveProfileRepository;
import cn.gaifan.douyinOperations.module.ai.service.HydeExpansionService;
import cn.gaifan.douyinOperations.module.ai.service.KbHybridRetrieveService;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeBaseService.SearchResult;
import cn.gaifan.douyinOperations.module.ai.service.RerankerService;
import cn.gaifan.douyinOperations.module.ai.service.SearchPersonalizationService;
import cn.gaifan.douyinOperations.module.ai.service.SearchService;
import cn.gaifan.douyinOperations.module.ai.service.VectorService;
import cn.gaifan.douyinOperations.module.ai.service.brain.IndustryKnowledgeGraphService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
public class KbHybridRetrieveServiceImpl implements KbHybridRetrieveService {

    private static final Logger log = LoggerFactory.getLogger(KbHybridRetrieveServiceImpl.class);
    private static final int RRF_K = 60;

    @Resource
    private VectorService vectorService;

    @Resource
    private SearchService searchService;

    @Resource
    private AiKbDocumentRepository documentRepository;

    @Autowired(required = false)
    private AiUserCognitiveProfileRepository cognitiveProfileRepository;

    @Autowired(required = false)
    private RerankerService rerankerService;

    @Autowired(required = false)
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired(required = false)
    private SearchMetricsCollector searchMetricsCollector;

    @Autowired(required = false)
    private IndustryKnowledgeGraphService knowledgeGraphService;

    @Autowired(required = false)
    private HydeExpansionService hydeExpansionService;

    @Autowired(required = false)
    private SearchPersonalizationService searchPersonalizationService;

    @Value("${app.ai.graphrag.enabled:true}")
    private boolean graphRagEnabled;

    @Value("${app.ai.kb.hybrid-dedup-threshold:0}")
    private double hybridDedupThreshold;

    @Value("${app.ai.kb.effectiveness-boost-enabled:true}")
    private boolean effectivenessBoostEnabled;

    @Value("${app.ai.kb.effectiveness-boost-max:1.15}")
    private double effectivenessBoostMax;

    /** E-3：quality_tier=2 的文档在融合排序上的额外惩罚系数（在 boost × 效果加成之后） */
    @Value("${app.ai.kb-quality.retrieve-penalty:0.92}")
    private double qualityLowRetrievePenalty;

    @Value("${app.ai.hybrid.fallback-to-es-only:true}")
    private boolean fallbackToEsOnly;

    @Value("${app.ai.hybrid.fallback-to-milvus-only:true}")
    private boolean fallbackToMilvusOnly;

    @Value("${app.ai.search.adaptive-weights.enabled:true}")
    private boolean adaptiveWeightsEnabled;

    @Value("${app.ai.search.adaptive-weights.short-query.vector:0.4}")
    private double shortQueryVector;

    @Value("${app.ai.search.adaptive-weights.short-query.keyword:0.6}")
    private double shortQueryKeyword;

    @Value("${app.ai.search.adaptive-weights.long-query.vector:0.8}")
    private double longQueryVector;

    @Value("${app.ai.search.adaptive-weights.long-query.keyword:0.2}")
    private double longQueryKeyword;

    @Value("${app.ai.search.adaptive-weights.technical.vector:0.3}")
    private double technicalVector;

    @Value("${app.ai.search.adaptive-weights.technical.keyword:0.7}")
    private double technicalKeyword;

    @Value("${app.ai.search.adaptive-weights.question.vector:0.75}")
    private double questionVector;

    @Value("${app.ai.search.adaptive-weights.question.keyword:0.25}")
    private double questionKeyword;

    @Value("${app.ai.search.adaptive-weights.default.vector:0.7}")
    private double defaultVector;

    @Value("${app.ai.search.adaptive-weights.default.keyword:0.3}")
    private double defaultKeyword;

    @Value("${app.ai.search.personalization.enabled:true}")
    private boolean personalizationEnabled;

    /** 对 vector 权重的最大绝对偏移（与 keyword 镜像），总调节幅度 ≤ 2×maxShift */
    @Value("${app.ai.search.personalization.max-weight-shift:0.15}")
    private double personalizationMaxShift;

    /** HyDE：假设文档向量在融合中的权重（其余为原查询向量），仅影响 Milvus 侧 */
    @Value("${app.ai.search.hyde.vector-blend-hypothesis:0.55}")
    private double hydeVectorBlendHypothesis;

    /** S-4：DEFINITION 意图下 HyDE 混合权重下限（强化语义对齐） */
    @Value("${app.ai.search.intent.definition-hyde-min-blend:0.72}")
    private double intentDefinitionHydeMinBlend;

    @Value("${app.ai.search.intent.how-to.vector:0.72}")
    private double intentHowToVector;
    @Value("${app.ai.search.intent.how-to.keyword:0.28}")
    private double intentHowToKeyword;

    @Value("${app.ai.search.intent.definition.vector:0.88}")
    private double intentDefinitionVector;
    @Value("${app.ai.search.intent.definition.keyword:0.12}")
    private double intentDefinitionKeyword;

    @Value("${app.ai.search.intent.compare.vector:0.45}")
    private double intentCompareVector;
    @Value("${app.ai.search.intent.compare.keyword:0.55}")
    private double intentCompareKeyword;

    private static final java.util.Set<String> TECHNICAL_TERMS = java.util.Set.of(
            "玻尿酸", "烟酰胺", "视黄醇", "胜肽", "神经酰胺", "水杨酸", "果酸", "维C", "维生素C",
            "SK-II", "雅诗兰黛", "兰蔻", "欧莱雅", "成分", "功效", "肤质", "敏感肌", "油皮", "干皮"
    );

    private record SearchWeights(double vector, double keyword) {}

    private SearchWeights determineWeights(String query) {
        return determineWeights(query, QueryIntent.GENERAL);
    }

    /** S-4：在意图分支上覆盖自适应权重（GENERAL 走原逻辑）。 */
    private SearchWeights determineWeights(String query, QueryIntent intent) {
        if (intent != null && intent != QueryIntent.GENERAL) {
            return switch (intent) {
                case HOW_TO -> clampWeights(intentHowToVector, intentHowToKeyword);
                case DEFINITION -> clampWeights(intentDefinitionVector, intentDefinitionKeyword);
                case COMPARE -> clampWeights(intentCompareVector, intentCompareKeyword);
                default -> determineAdaptiveWeights(query);
            };
        }
        return determineAdaptiveWeights(query);
    }

    private SearchWeights determineAdaptiveWeights(String query) {
        if (!adaptiveWeightsEnabled || query == null) {
            return new SearchWeights(defaultVector, defaultKeyword);
        }
        String q = query.trim();
        int len = q.length();
        if (len < 5) return new SearchWeights(shortQueryVector, shortQueryKeyword);
        if (len > 20) return new SearchWeights(longQueryVector, longQueryKeyword);
        if (containsTechnicalTerms(q)) return new SearchWeights(technicalVector, technicalKeyword);
        if (isNaturalQuestion(q)) return new SearchWeights(questionVector, questionKeyword);
        return new SearchWeights(defaultVector, defaultKeyword);
    }

    private static SearchWeights clampWeights(double vector, double keyword) {
        double v = Math.min(0.95, Math.max(0.05, vector));
        double k = Math.min(0.95, Math.max(0.05, keyword));
        double sum = v + k;
        if (sum <= 0) {
            return new SearchWeights(0.7, 0.3);
        }
        return new SearchWeights(v / sum, k / sum);
    }

    /** 基于用户认知画像 + 搜索行为历史微调向量/关键词 RRF 权重 */
    private SearchWeights applyPersonalization(SearchWeights base, Long ownerId) {
        if (!personalizationEnabled || ownerId == null) {
            return base;
        }

        double shift = 0;

        // Layer 1: 认知画像（优化焦点、编辑比例、风险偏好）
        if (cognitiveProfileRepository != null) {
            AiUserCognitiveProfile p = cognitiveProfileRepository.findByOwnerIdAndDeleted(ownerId, 0).orElse(null);
            if (p != null) {
                String focus = p.getOptimizationFocus() != null ? p.getOptimizationFocus().toLowerCase(Locale.ROOT) : "";
                if (focus.contains("转化") || focus.contains("成交") || focus.contains("conversion")) {
                    shift -= 0.06;
                }
                if (focus.contains("创意") || focus.contains("风格") || focus.contains("creative")) {
                    shift += 0.06;
                }
                Double ar = p.getAvgEditRatio();
                if (ar != null) {
                    if (ar > 0.55) shift -= 0.05;
                    else if (ar < 0.35) shift += 0.05;
                }
                Double rt = p.getRiskTolerance();
                if (rt != null) {
                    if (rt > 0.6) shift += 0.04;
                    else if (rt < 0.35) shift -= 0.03;
                }
            }
        }

        // Layer 2: 搜索行为历史（BM25/Vector 命中率）
        if (searchPersonalizationService != null) {
            try {
                Map<String, Object> spWeights = searchPersonalizationService.getPersonalizedWeights(ownerId);
                if (Boolean.TRUE.equals(spWeights.get("personalized"))) {
                    double spBm25 = ((Number) spWeights.get("bm25Weight")).doubleValue();
                    // spBm25 > 0.5 说明用户历史偏好关键词 → keyword 加权 → vector shift 负
                    double spShift = (0.5 - spBm25) * 0.2; // 最多 ±0.06
                    shift += spShift;
                }
            } catch (Exception e) {
                log.debug("[Personalization] 搜索行为个性化加载失败: {}", e.getMessage());
            }
        }

        shift = Math.max(-personalizationMaxShift, Math.min(personalizationMaxShift, shift));
        double v = Math.min(0.95, Math.max(0.05, base.vector() + shift));
        double k = Math.min(0.95, Math.max(0.05, base.keyword() - shift));
        return new SearchWeights(v, k);
    }

    private boolean containsTechnicalTerms(String q) {
        for (String t : TECHNICAL_TERMS) {
            if (q.contains(t)) return true;
        }
        return false;
    }

    private boolean isNaturalQuestion(String q) {
        return q.startsWith("如何") || q.startsWith("怎么") || q.startsWith("为什么")
                || q.startsWith("什么") || q.startsWith("怎样") || q.startsWith("哪些");
    }

    @Override
    public List<SearchResult> retrieve(AiKnowledgeBase kb, List<String> queries, int topK,
                                       String metadataFilter, Map<String, Object> esFilters, int candidateTop) {
        return retrieve(kb, queries, topK, metadataFilter, esFilters, candidateTop, null);
    }

    @Override
    public List<SearchResult> retrieve(AiKnowledgeBase kb, List<String> queries, int topK,
                                       String metadataFilter, Map<String, Object> esFilters, int candidateTop,
                                       Long personalizeUserId) {
        return retrieve(kb, queries, topK, metadataFilter, esFilters, candidateTop, personalizeUserId, QueryIntent.GENERAL);
    }

    @Override
    public List<SearchResult> retrieve(AiKnowledgeBase kb, List<String> queries, int topK,
                                       String metadataFilter, Map<String, Object> esFilters, int candidateTop,
                                       Long personalizeUserId, QueryIntent queryIntent) {
        QueryIntent intent = queryIntent != null ? queryIntent : QueryIntent.GENERAL;
        long kbId = kb.getId();
        String collectionName = "kb_" + kbId;
        String indexName = "kb_" + kbId;

        // GraphRAG 融合：图谱为空时降级为普通混合检索
        List<String> enrichedQueries = queries;
        if (graphRagEnabled && knowledgeGraphService != null && knowledgeGraphService.isAvailable() && !queries.isEmpty()) {
            String firstQuery = queries.get(0);
            String graphCtx = knowledgeGraphService.getGraphContextForQuery(firstQuery, kb.getUserId(), 5);
            if (graphCtx != null && !graphCtx.isBlank()) {
                enrichedQueries = queries.stream()
                        .map(q -> q + graphCtx)
                        .collect(Collectors.toList());
                log.debug("[GraphRAG] 查询已融合图谱上下文: {}", graphCtx);
            }
        }

        Map<String, RRFEntry> rrfMap = new ConcurrentHashMap<>();
        AtomicBoolean anyVector = new AtomicBoolean(false);
        AtomicBoolean anyFulltext = new AtomicBoolean(false);

        CircuitBreaker milvusCb = circuitBreakerRegistry != null ? circuitBreakerRegistry.circuitBreaker("milvus") : null;
        CircuitBreaker esCb = circuitBreakerRegistry != null ? circuitBreakerRegistry.circuitBreaker("elasticsearch") : null;

        SearchWeights rawWeights = determineWeights(enrichedQueries.isEmpty() ? "" : enrichedQueries.get(0), intent);
        SearchWeights weights = applyPersonalization(rawWeights, personalizeUserId);

        for (String q : enrichedQueries) {
            final QueryIntent intentFinal = intent;
            CompletableFuture<List<VectorService.VectorSearchResult>> vectorFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    List<Float> queryVector = vectorService.generateEmbedding(q);
                    List<Float> vectorForSearch = resolveVectorWithOptionalHyde(q, queryVector, intentFinal);
                    return (milvusCb != null ? milvusCb.executeSupplier(() -> vectorService.search(collectionName, vectorForSearch, candidateTop, metadataFilter)) : vectorService.search(collectionName, vectorForSearch, candidateTop, metadataFilter));
                } catch (CallNotPermittedException e) {
                    log.warn("Milvus 熔断打开，降级为仅 ES");
                    return Collections.<VectorService.VectorSearchResult>emptyList();
                } catch (Exception e) {
                    if (queries.size() == 1 && !fallbackToEsOnly) {
                        throw new BusinessException(ErrorCode.AI_KNOWLEDGE_SEARCH_FAIL, "向量检索失败: " + e.getMessage());
                    }
                    return Collections.<VectorService.VectorSearchResult>emptyList();
                }
            });
            CompletableFuture<List<SearchService.SearchResult>> esFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return (esCb != null ? esCb.executeSupplier(() -> searchService.search(indexName, q, 0, candidateTop, esFilters)) : searchService.search(indexName, q, 0, candidateTop, esFilters));
                } catch (CallNotPermittedException e) {
                    log.warn("ES 熔断打开，降级为仅 Milvus");
                    return Collections.<SearchService.SearchResult>emptyList();
                } catch (Exception e) {
                    if (queries.size() == 1 && !fallbackToMilvusOnly) {
                        throw new BusinessException(ErrorCode.AI_KNOWLEDGE_SEARCH_FAIL, "全文检索失败: " + e.getMessage());
                    }
                    return Collections.<SearchService.SearchResult>emptyList();
                }
            });

            List<VectorService.VectorSearchResult> vectorResults;
            List<SearchService.SearchResult> fullTextResults;
            try {
                vectorResults = vectorFuture.get(60, TimeUnit.SECONDS);
                fullTextResults = esFuture.get(60, TimeUnit.SECONDS);
            } catch (Exception e) {
                if (e.getCause() instanceof BusinessException be) throw be;
                throw new BusinessException(ErrorCode.AI_KNOWLEDGE_SEARCH_FAIL, "检索超时或失败: " + e.getMessage());
            }
            if (!vectorResults.isEmpty()) anyVector.set(true);
            if (!fullTextResults.isEmpty()) anyFulltext.set(true);
            if (vectorResults.isEmpty() && searchMetricsCollector != null) searchMetricsCollector.recordVectorEmpty();

            for (int rank = 0; rank < vectorResults.size(); rank++) {
                VectorService.VectorSearchResult r = vectorResults.get(rank);
                Object docIdObj = r.metadata().get("doc_id");
                if (docIdObj == null) docIdObj = r.metadata().get("docId");
                if (docIdObj == null) continue;
                Long docId = ((Number) docIdObj).longValue();
                int chunkIdx = ((Number) r.metadata().getOrDefault("chunk_index", 0)).intValue();
                String key = chunkKey(docId, chunkIdx);
                String title = (String) r.metadata().get("title");
                String text = (String) r.metadata().get("text");
                List<String> labels = getLabelsFromMeta(r.metadata());
                double rrfScore = weights.vector() * (1.0 / (RRF_K + rank + 1));
                rrfMap.merge(key, new RRFEntry(docId, chunkIdx, title, text, rrfScore, 1, labels, rank, -1),
                        RRFEntry::merge);
            }
            for (int rank = 0; rank < fullTextResults.size(); rank++) {
                SearchService.SearchResult r = fullTextResults.get(rank);
                Map<String, Object> src = r.source();
                Object docIdObj = src.get("doc_id");
                if (docIdObj == null) docIdObj = src.get("docId");
                if (docIdObj == null) continue;
                Long docId = ((Number) docIdObj).longValue();
                int chunkIdx = ((Number) src.getOrDefault("chunk_index", 0)).intValue();
                String key = chunkKey(docId, chunkIdx);
                String title = (String) src.get("title");
                String text = (String) src.get("text");
                List<String> labels = getLabelsFromMeta(src);
                double rrfScore = weights.keyword() * (1.0 / (RRF_K + rank + 1));
                rrfMap.merge(key, new RRFEntry(docId, chunkIdx, title, text, rrfScore, 2, labels, -1, rank),
                        RRFEntry::merge);
            }
        }

        if (searchMetricsCollector != null) {
            if (!anyVector.get()) searchMetricsCollector.recordFallbackEsOnly();
            if (!anyFulltext.get()) searchMetricsCollector.recordFallbackMilvusOnly();
        }

        List<SearchResult> merged;
        if (!anyVector.get() && !anyFulltext.get()) {
            log.warn("知识库混合检索无结果（向量与全文均无数据）kbId={}, query={}", kbId, queries.isEmpty() ? "" : queries.get(0));
            merged = Collections.emptyList();
        } else {
            merged = rrfMap.values().stream()
                    .sorted((a, b) -> Double.compare(b.score, a.score))
                    .map(e -> new SearchResult(e.docId, e.title, e.text, e.score,
                            (e.sourceMask & 3) == 3 ? "hybrid" : ((e.sourceMask & 1) != 0 ? "vector" : "fulltext"),
                            Long.valueOf(e.docId * 10000L + e.chunkIndex),
                            e.labels != null ? e.labels : List.of(),
                            truncateExplain(buildRetrieveExplain(e, weights), 480)))
                    .limit(rerankerService != null && rerankerService.isAvailable() ? candidateTop : topK)
                    .collect(Collectors.toList());

            if (hybridDedupThreshold > 0 && merged.size() > 1) {
                merged = applySemanticDedup(merged);
            }
        }

        Set<Long> docIds = merged.stream().map(SearchResult::docId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (!docIds.isEmpty()) {
            List<AiKbDocument> docs = documentRepository.findByIdIn(List.copyOf(docIds));
            Map<Long, Double> boostMap = docs.stream()
                    .filter(d -> d.getBoostFactor() != null && d.getBoostFactor().doubleValue() != 1.0)
                    .collect(Collectors.toMap(AiKbDocument::getId, d -> d.getBoostFactor().doubleValue()));
            Map<Long, Double> effBoostMap = new HashMap<>();
            Map<Long, Double> qualityPenaltyMap = new HashMap<>();
            for (AiKbDocument d : docs) {
                if (d.getQualityTier() != null && d.getQualityTier() == 2) {
                    qualityPenaltyMap.put(d.getId(), Math.min(1.0, Math.max(0.5, qualityLowRetrievePenalty)));
                }
            }
            if (effectivenessBoostEnabled) {
                for (AiKbDocument d : docs) {
                    long retrieval = d.getRetrievalCount() != null ? d.getRetrievalCount() : 0;
                    long citation = d.getCitationCount() != null ? d.getCitationCount() : 0;
                    if (retrieval > 0 || citation > 0) {
                        double eff = Math.log(1 + retrieval + citation * 2) * 0.03;
                        effBoostMap.put(d.getId(), Math.min(effectivenessBoostMax, 1.0 + eff));
                    }
                }
            }
            merged = merged.stream()
                    .map(r -> {
                        double boost = boostMap.getOrDefault(r.docId(), 1.0);
                        double effBoost = effBoostMap.getOrDefault(r.docId(), 1.0);
                        double qPen = qualityPenaltyMap.getOrDefault(r.docId(), 1.0);
                        return new SearchResult(r.docId(), r.title(), r.content(), r.score() * boost * effBoost * qPen, r.source(), r.chunkId(), r.labels(), r.explain());
                    })
                    .sorted((a, b) -> Double.compare(b.score(), a.score()))
                    .collect(Collectors.toList());
        }

        if (rerankerService != null && rerankerService.isAvailable() && !merged.isEmpty()) {
            String query = queries.isEmpty() ? "" : queries.get(0);
            List<String> candidates = merged.stream().map(SearchResult::content).collect(Collectors.toList());
            List<Float> rerankScores = rerankerService.rerank(query, candidates);
            if (rerankScores != null && rerankScores.size() == merged.size()) {
                List<SearchResult> withRerank = new ArrayList<>();
                for (int i = 0; i < merged.size(); i++) {
                    SearchResult orig = merged.get(i);
                    float rs = rerankScores.get(i);
                    String ex = orig.explain() != null && !orig.explain().isBlank()
                            ? orig.explain() + String.format(Locale.ROOT, "; 重排分=%.5f", rs)
                            : String.format(Locale.ROOT, "重排分=%.5f", rs);
                    withRerank.add(new SearchResult(orig.docId(), orig.title(), orig.content(),
                            rs, "reranked", orig.chunkId(), orig.labels(), truncateExplain(ex, 480)));
                }
                withRerank.sort((a, b) -> Double.compare(b.score(), a.score()));
                merged = withRerank.stream().limit(topK).collect(Collectors.toList());
            }
        }
        return merged;
    }

    private List<Float> resolveVectorWithOptionalHyde(String fullQuery, List<Float> queryVector, QueryIntent intent) {
        if (hydeExpansionService == null || queryVector == null || queryVector.isEmpty()) {
            return queryVector;
        }
        try {
            var hypoOpt = hydeExpansionService.expandHypotheticalPassage(fullQuery);
            if (hypoOpt.isEmpty()) {
                return queryVector;
            }
            List<Float> hypoVec = vectorService.generateEmbedding(hypoOpt.get());
            if (hypoVec == null || hypoVec.size() != queryVector.size()) {
                return queryVector;
            }
            double baseBlend = hydeVectorBlendHypothesis;
            if (intent == QueryIntent.DEFINITION) {
                baseBlend = Math.max(baseBlend, intentDefinitionHydeMinBlend);
            }
            double hypoW = Math.min(0.95, Math.max(0.05, baseBlend));
            List<Float> blended = blendEmbeddingsL2Normalized(queryVector, hypoVec, hypoW);
            if (searchMetricsCollector != null) {
                searchMetricsCollector.recordHydeUsed();
            }
            return blended;
        } catch (Exception e) {
            log.debug("HyDE 向量融合跳过: {}", e.getMessage());
            return queryVector;
        }
    }

    private static List<Float> blendEmbeddingsL2Normalized(List<Float> queryVec, List<Float> hypoVec, double hypoWeight) {
        double qw = 1.0 - hypoWeight;
        List<Float> out = new ArrayList<>(queryVec.size());
        double normSq = 0;
        for (int i = 0; i < queryVec.size(); i++) {
            float v = (float) (hypoWeight * hypoVec.get(i) + qw * queryVec.get(i));
            out.add(v);
            normSq += (double) v * v;
        }
        if (normSq <= 1e-12) {
            return queryVec;
        }
        double inv = 1.0 / Math.sqrt(normSq);
        for (int i = 0; i < out.size(); i++) {
            out.set(i, (float) (out.get(i) * inv));
        }
        return out;
    }

    private List<SearchResult> applySemanticDedup(List<SearchResult> merged) {
        if (merged.size() <= 1) return merged;
        List<String> texts = merged.stream().map(SearchResult::content).map(c -> c != null ? c : "").toList();
        List<List<Float>> embeddings;
        try {
            embeddings = vectorService.generateEmbeddings(texts);
        } catch (Exception e) {
            log.warn("语义去重 embedding 失败，跳过: {}", e.getMessage());
            return merged;
        }
        if (embeddings == null || embeddings.size() != merged.size()) return merged;
        List<Integer> keepIndex = new ArrayList<>();
        for (int i = 0; i < merged.size(); i++) {
            boolean duplicate = false;
            for (int j : keepIndex) {
                if (cosine(embeddings.get(j), embeddings.get(i)) >= hybridDedupThreshold) {
                    duplicate = true;
                    break;
                }
            }
            if (!duplicate) keepIndex.add(i);
        }
        return keepIndex.stream().map(merged::get).collect(Collectors.toList());
    }

    private static double cosine(List<Float> a, List<Float> b) {
        if (a == null || b == null || a.size() != b.size()) return 0;
        double sum = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.size(); i++) {
            float x = a.get(i), y = b.get(i);
            sum += x * y;
            normA += x * x;
            normB += y * y;
        }
        if (normA <= 0 || normB <= 0) return 0;
        return sum / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private static String chunkKey(Long docId, int chunkIndex) {
        return docId + "_" + chunkIndex;
    }

    @SuppressWarnings("unchecked")
    private static List<String> getLabelsFromMeta(Map<String, Object> meta) {
        if (meta == null) return List.of();
        Object o = meta.get("labels");
        if (o instanceof List<?> list) {
            return list.stream().filter(x -> x != null).map(String::valueOf).toList();
        }
        return List.of();
    }

    private static String truncateExplain(String s, int max) {
        if (s == null || s.length() <= max) {
            return s;
        }
        return s.substring(0, max - 3) + "...";
    }

    private static String buildRetrieveExplain(RRFEntry e, SearchWeights w) {
        StringBuilder sb = new StringBuilder();
        if (e.vectorRank >= 0) {
            double contrib = w.vector() * (1.0 / (RRF_K + e.vectorRank + 1));
            sb.append(String.format(Locale.ROOT, "向量榜位=%d RRF分量=%.5f", e.vectorRank + 1, contrib));
        }
        if (e.esRank >= 0) {
            if (sb.length() > 0) {
                sb.append("; ");
            }
            double contrib = w.keyword() * (1.0 / (RRF_K + e.esRank + 1));
            sb.append(String.format(Locale.ROOT, "BM25榜位=%d RRF分量=%.5f", e.esRank + 1, contrib));
        }
        if (sb.length() > 0) {
            sb.append("; ");
        }
        sb.append(String.format(Locale.ROOT, "融合RRF=%.5f; 通道=%s", e.score, sourceChannelLabel(e.sourceMask)));
        return sb.toString();
    }

    private static String sourceChannelLabel(int mask) {
        if ((mask & 3) == 3) {
            return "hybrid";
        }
        if ((mask & 1) != 0) {
            return "vector";
        }
        return "fulltext";
    }

    private record RRFEntry(long docId, int chunkIndex, String title, String text, double score, int sourceMask, List<String> labels,
                            int vectorRank, int esRank) {

        private static int mergeRank(int a, int b) {
            if (a < 0) {
                return b;
            }
            if (b < 0) {
                return a;
            }
            return Math.min(a, b);
        }

        private static String pickText(String a, String b) {
            return (a != null && !a.isBlank()) ? a : (b != null ? b : "");
        }

        static RRFEntry merge(RRFEntry old, RRFEntry inc) {
            return new RRFEntry(
                    old.docId,
                    old.chunkIndex,
                    pickText(old.title, inc.title),
                    pickText(old.text, inc.text),
                    old.score + inc.score,
                    old.sourceMask | inc.sourceMask,
                    (old.labels != null && !old.labels.isEmpty()) ? old.labels : (inc.labels != null ? inc.labels : List.of()),
                    mergeRank(old.vectorRank, inc.vectorRank),
                    mergeRank(old.esRank, inc.esRank));
        }
    }
}
