package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.module.ai.service.ContentEffectivenessService;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeBaseService;
import cn.gaifan.douyinOperations.module.ai.service.QueryRewriteService;
import cn.gaifan.douyinOperations.module.live.service.LiveScriptPromptService;
import cn.gaifan.douyinOperations.module.product.entity.DyProduct;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * 直播话术 Prompt 构建服务实现：RAG 上下文、知识库引用。
 */
@Service
public class LiveScriptPromptServiceImpl implements LiveScriptPromptService {

    private static final Logger log = LoggerFactory.getLogger(LiveScriptPromptServiceImpl.class);

    @Resource
    private KnowledgeBaseService knowledgeBaseService;

    @Resource
    private LiveKnowledgeBaseAccessResolver knowledgeBaseAccessResolver;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private QueryRewriteService queryRewriteService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ContentEffectivenessService contentEffectivenessService;

    @Value("${app.ai.kb.rag.enabled:true}")
    private boolean ragEnabled;
    @Value("${app.ai.kb.rag.top-k:8}")
    private int ragTopK;
    @Value("${app.ai.kb.rag.min-score:0.5}")
    private double ragMinScore;
    @Value("${app.ai.kb.rag.max-context-chars:2000}")
    private int ragMaxContextChars;
    @Value("${app.ai.kb.rag.token-budget-ratio:0.25}")
    private double ragTokenBudgetRatio;
    @Value("${app.ai.kb.rag.parallel-timeout-sec:5}")
    private int ragParallelTimeoutSec;

    @Override
    public RagContextResult buildRagContext(Long userId, DyProduct product, String scriptType,
                                            String style, int promptLength, String requirement) {
        if (!ragEnabled || userId == null) return null;
        LiveKnowledgeBaseAccessResolver.ResolvedKnowledgeBase resolvedKb =
                knowledgeBaseAccessResolver.resolveHuashu(userId);
        if (resolvedKb == null) return null;
        if (resolvedKb.sharedFallback()) {
            log.debug("直播 Prompt RAG 使用显式共享 huashu 知识库: requestUserId={}, kbOwnerId={}, kbId={}",
                    userId, resolvedKb.accessUserId(), resolvedKb.kbId());
        }
        final Long kbId = resolvedKb.kbId();
        final Long ragUserId = resolvedKb.accessUserId();
        int budget = (int) Math.max(200, ragMaxContextChars * ragTokenBudgetRatio);
        if (promptLength > 0) budget = Math.min(ragMaxContextChars, (int) (ragMaxContextChars - promptLength * 0.3));
        if (budget < 200) return null;

        List<String> queries = buildCategoryQueries(product, scriptType, style, requirement);
        if (queryRewriteService != null) {
            List<String> extra = new ArrayList<>();
            for (String q : queries) {
                if (q == null || q.isBlank()) continue;
                try {
                    List<String> rewritten = queryRewriteService.rewrite(userId, q);
                    if (rewritten != null) extra.addAll(rewritten);
                } catch (Exception ignored) { }
            }
            queries = new ArrayList<>(queries);
            queries.addAll(extra);
        }
        queries = queries.stream().filter(q -> q != null && !q.isBlank()).distinct().limit(12).toList();
        if (queries.isEmpty()) return null;

        Set<Long> seenChunkIds = ConcurrentHashMap.newKeySet();
        List<KnowledgeBaseService.SearchResult> allResults = Collections.synchronizedList(new ArrayList<>());
        String baseSourceFilter = "(metadata[\"source_type\"] == \"live_script\") || (metadata[\"source_type\"] == \"manual\") || (metadata[\"source_type\"] == \"evolved_script\")";
        Map<String, Object> huashuEsFilters = new java.util.HashMap<>(Map.of("source_type", List.of("live_script", "manual", "evolved_script")));
        if (scriptType != null && !scriptType.isBlank() && !"custom".equals(scriptType) && !"chat".equals(scriptType)) {
            huashuEsFilters.put("script_type", List.of(scriptType));
        }
        final String huashuMilvusFilter = (scriptType != null && !scriptType.isBlank() && !"custom".equals(scriptType) && !"chat".equals(scriptType))
                ? baseSourceFilter + " && (metadata[\"script_type\"] == \"" + scriptType + "\")"
                : baseSourceFilter;
        List<CompletableFuture<Void>> futures = queries.stream()
                .map(q -> CompletableFuture.runAsync(() -> {
                    List<KnowledgeBaseService.SearchResult> list = knowledgeBaseService.hybridSearch(kbId, q, ragTopK + 5, ragUserId, huashuMilvusFilter, huashuEsFilters, true);
                    if (list != null) {
                        for (KnowledgeBaseService.SearchResult r : list) {
                            Long cid = r.chunkId() != null ? r.chunkId() : r.docId();
                            if (cid != null && seenChunkIds.add(cid)) allResults.add(r);
                        }
                    }
                }))
                .toList();
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(ragParallelTimeoutSec, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("RAG 并行检索部分超时，已收集 {} 条", allResults.size());
        } catch (Exception ignored) { }

        List<KnowledgeBaseService.SearchResult> filtered = allResults.stream()
                .filter(r -> r.score() >= ragMinScore)
                .sorted(Comparator.comparingDouble(KnowledgeBaseService.SearchResult::score).reversed())
                .limit(ragTopK)
                .toList();
        if (filtered.isEmpty()) return null;

        StringBuilder sb = new StringBuilder();
        sb.append("<reference_scripts>\n<note>仅参考风格和技巧，禁止照搬内容</note>\n");
        int totalLen = 0;
        List<KnowledgeBaseService.SearchResult> includedRefs = new ArrayList<>();
        for (int i = 0; i < filtered.size(); i++) {
            KnowledgeBaseService.SearchResult r = filtered.get(i);
            if (totalLen + r.content().length() > budget) break;
            String category = (r.labels() != null && !r.labels().isEmpty())
                    ? r.labels().stream().filter(l -> l != null && l.startsWith("type:")).findFirst().orElse("").replace("type:", "")
                    : "话术";
            sb.append(String.format("<script id=\"%d\" category=\"%s\" score=\"%.2f\">", i + 1, category, r.score()));
            sb.append(r.content());
            sb.append("</script>\n");
            totalLen += r.content().length();
            includedRefs.add(r);
        }
        sb.append("</reference_scripts>");
        if (contentEffectivenessService != null && !includedRefs.isEmpty()) {
            List<Long> citedDocIds = includedRefs.stream().map(KnowledgeBaseService.SearchResult::docId).filter(Objects::nonNull).distinct().toList();
            if (!citedDocIds.isEmpty()) contentEffectivenessService.recordCitation(citedDocIds);
        }
        return new RagContextResult(sb.toString(), includedRefs);
    }

    @Override
    public BatchRagContextResult buildRagContextBatch(Long userId, String style,
                                                       List<String> scriptTypes, DyProduct productForRag) {
        if (!ragEnabled || userId == null || scriptTypes == null || scriptTypes.isEmpty()) {
            return new BatchRagContextResult(Map.of());
        }

        // 去重话术类型，分为「需要商品上下文」与「不需要商品上下文」两组
        List<String> productRelated = scriptTypes.stream()
                .filter(t -> t != null && ("product".equals(t) || "closing_deal".equals(t)
                        || "pain_point".equals(t) || "testimony".equals(t) || "deep_sell".equals(t)))
                .distinct().toList();
        List<String> nonProduct = scriptTypes.stream()
                .filter(t -> t != null && !productRelated.contains(t))
                .distinct().toList();

        Map<String, RagContextResult> results = new java.util.concurrent.ConcurrentHashMap<>();

        // 并发构建各类型 RAG（避免全串行）
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // 商品类话术：共用 productForRag 对象，合并检索
        if (!productRelated.isEmpty()) {
            for (String t : productRelated) {
                DyProduct prod = "product".equals(t) ? productForRag : null;
                futures.add(CompletableFuture.runAsync(() -> {
                    try {
                        RagContextResult r = buildRagContext(userId, prod, t, style, 0, null);
                        if (r != null) results.put(t, r);
                    } catch (Exception e) {
                        log.debug("[BatchRAG] {} 检索失败: {}", t, e.getMessage());
                    }
                }));
            }
        }

        // 非商品类话术：各自独立检索
        for (String t : nonProduct) {
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    RagContextResult r = buildRagContext(userId, null, t, style, 0, null);
                    if (r != null) results.put(t, r);
                } catch (Exception e) {
                    log.debug("[BatchRAG] {} 检索失败: {}", t, e.getMessage());
                }
            }));
        }

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(ragParallelTimeoutSec * 2L, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("[BatchRAG] 批量 RAG 检索超时/异常，已收集 {} 类型结果: {}", results.size(), e.getMessage());
        }

        log.debug("[BatchRAG] 批量 RAG 完成，scriptTypes={}, 命中={}", scriptTypes.size(), results.size());
        return new BatchRagContextResult(results);
    }

    private List<String> buildCategoryQueries(DyProduct product, String scriptType, String style, String requirement) {
        List<String> queries = new ArrayList<>();
        if ("chat".equals(scriptType) && product == null) {
            queries.add("聊家常 夫妻感情 婚姻 恋爱 话术");
            queries.add("婆媳关系 家庭 女性 话术");
            queries.add("人生励志 成长 逆袭 金句");
            queries.add("歇后语 俗语 幽默 接地气");
            queries.add("名言金句 警句 语录 格调");
            queries.add("古诗 诗词 唐诗 宋词 格调");
            if (requirement != null && !requirement.isBlank()) {
                String topic = requirement.replace("聊家常", "").replace("（", "").replace("）", "").replace("/", " ").trim();
                if (!topic.isEmpty()) queries.add(topic + " 话术");
            }
        } else {
            String typeTerm = mapScriptTypeToQuery(scriptType);
            StringBuilder q1 = new StringBuilder();
            if (product != null && product.getProductCategory() != null) q1.append(product.getProductCategory()).append(" ");
            q1.append(typeTerm);
            if (style != null && !style.isBlank()) q1.append(" ").append(style);
            queries.add(q1.toString().trim());
            if (product != null && product.getProductName() != null) {
                StringBuilder q2 = new StringBuilder(product.getProductName());
                if (product.getAiSellingPoints() != null && !product.getAiSellingPoints().isBlank()) {
                    String sp = product.getAiSellingPoints().split("[,，]")[0].trim();
                    if (!sp.isEmpty()) q2.append(" ").append(sp);
                }
                queries.add(q2.toString().trim());
            }
            if (style != null && !style.isBlank()) queries.add(style + " " + typeTerm + " 案例");
        }
        return queries.stream().filter(q -> !q.isEmpty()).distinct().toList();
    }

    private static String mapScriptTypeToQuery(String scriptType) {
        if (scriptType == null) return "话术";
        return switch (scriptType) {
            case "seed" -> "种草话术";
            case "promotion" -> "促销话术";
            case "formal" -> "产品介绍";
            case "emotional" -> "情绪价值话术";
            case "chat" -> "聊家常话术";
            case "transition" -> "过渡话术";
            case "opening" -> "开场话术";
            case "closing" -> "收尾话术";
            case "interaction" -> "互动引导话术";
            case "welfare" -> "福利话术";
            case "closing_deal" -> "逼单促单话术";
            case "hold_back" -> "憋单蓄水话术";
            case "rapid_intro" -> "快速过品话术";
            case "deep_sell" -> "深度单品话术";
            case "pain_point" -> "痛点放大话术";
            case "testimony" -> "用户证言话术";
            default -> "话术";
        };
    }
}
