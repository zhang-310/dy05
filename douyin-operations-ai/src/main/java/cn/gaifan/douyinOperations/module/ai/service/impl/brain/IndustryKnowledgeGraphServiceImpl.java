package cn.gaifan.douyinOperations.module.ai.service.impl.brain;

import cn.gaifan.douyinOperations.module.ai.entity.AiGraphEdge;
import cn.gaifan.douyinOperations.module.ai.entity.AiGraphNode;
import cn.gaifan.douyinOperations.module.ai.entity.AiGraphRelationSuggestion;
import cn.gaifan.douyinOperations.module.ai.repository.AiGraphEdgeRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiGraphNodeRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiGraphRelationSuggestionRepository;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeBaseService;
import cn.gaifan.douyinOperations.module.ai.service.brain.IndustryKnowledgeGraphService;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 行业知识图谱服务实现
 * 优先 Neo4j（NEO4J_URI 设置时），否则用知识库混合检索作为实体来源
 */
@Service
public class IndustryKnowledgeGraphServiceImpl implements IndustryKnowledgeGraphService {

    private static final Logger log = LoggerFactory.getLogger(IndustryKnowledgeGraphServiceImpl.class);

    @Value("${app.ai.brain.knowledge-graph.enabled:true}")
    private boolean enabled;

    /**
     * 图谱上下文遍历深度（1=仅直接邻居，2=二度关系链）。过大会增加 DB 压力与 prompt 长度，上限在代码中钳制。
     */
    @Value("${app.ai.brain.knowledge-graph.max-hops:2}")
    private int maxHops;

    @Value("${app.ai.brain.knowledge-graph.max-edges-per-node:5}")
    private int maxEdgesPerNode;

    /** G-4：在 GraphRAG 上下文末尾附带「触及实体」update_time 跨度（天） */
    @Value("${app.ai.brain.knowledge-graph.freshness-hint:true}")
    private boolean freshnessHint;

    /** G-2：TF-IDF 共现补边时，两端 score 须至少有一处 ≥ 该阈值（默认与历史硬编码 0.5 等价）。 */
    @Value("${app.ai.brain.knowledge-graph.co-mention-min-tfidf:0.5}")
    private double coMentionMinTfidf;

    /** G-2：推断共现边的置信度（默认与历史固定 0.35 等价）。 */
    @Value("${app.ai.brain.knowledge-graph.co-mention-inferred-confidence:0.35}")
    private double coMentionInferredConfidence;

    /** G-2：共现传递闭包跳数（0=关闭，1=一步弱边）。 */
    @Value("${app.ai.brain.knowledge-graph.co-mention-transitive-hops:0}")
    private int coMentionTransitiveHops;

    /** G-2：传递弱边置信度 = min(AB,BC)×decay。 */
    @Value("${app.ai.brain.knowledge-graph.co-mention-transitive-decay:0.6}")
    private double coMentionTransitiveDecay;

    /** G-2：参与传递的每条 co_mentioned 边置信度须 ≥ 该值。 */
    @Value("${app.ai.brain.knowledge-graph.co-mention-transitive-min-confidence:0.35}")
    private double coMentionTransitiveMinConfidence;

    @Value("${app.ai.kb.shared-owner-id:0}")
    private Long sharedKbOwnerId;

    @Value("${app.ai.kb.allow-shared-fallback:false}")
    private boolean allowSharedKbFallback;

    @Autowired(required = false)
    private Neo4jClient neo4jClient;

    @Autowired(required = false)
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired(required = false)
    private AiGraphNodeRepository nodeRepository;

    @Autowired(required = false)
    private AiGraphEdgeRepository edgeRepository;

    @Autowired(required = false)
    private AiGraphRelationSuggestionRepository graphRelationSuggestionRepository;

    @Override
    public List<Map<String, Object>> queryEntities(String entityType, String keyword, int limit) {
        return queryEntities(entityType, keyword, limit, null);
    }

    @Override
    public List<Map<String, Object>> queryEntities(String entityType, String keyword, int limit, Long ownerId) {
        if (!enabled) return Collections.emptyList();
        log.debug("[IndustryKG] queryEntities type={} keyword={}", entityType, keyword);

        if (neo4jClient != null) {
            return queryFromNeo4j(entityType, keyword, limit, ownerId);
        }
        return queryFromKnowledgeBase(entityType, keyword, limit, ownerId);
    }

    @Override
    public Map<String, Object> getEntityDetail(String entityId, String entityType) {
        if (!enabled) return Collections.emptyMap();
        Map<String, Object> m = new HashMap<>();
        m.put("entityId", entityId);
        m.put("entityType", entityType);
        m.put("relations", Collections.emptyList());
        if (neo4jClient != null) {
            try {
                int hops = effectiveMaxHops();
                List<Map<String, Object>> relRows;
                if (hops <= 1) {
                    var rels = neo4jClient
                            .query("MATCH (e {id: $id})-[r]->(o) RETURN type(r) as relType, o.id as targetId LIMIT 30")
                            .bindAll(Map.of("id", entityId))
                            .fetch()
                            .all();
                    relRows = rels.stream().map(r -> {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("type", r.get("relType").toString());
                        row.put("targetId", r.get("targetId") != null ? r.get("targetId").toString() : "");
                        return row;
                    }).toList();
                } else {
                    String cypher = """
                            MATCH p = (e {id: $id})-[*1..%d]->(o)
                            WHERE e.id = $id
                            RETURN length(p) AS hopCount,
                              [x IN relationships(p) | type(x)] AS relTypes,
                              o.id AS targetId
                            LIMIT 40
                            """.formatted(hops);
                    var rels = neo4jClient.query(cypher).bindAll(Map.of("id", entityId)).fetch().all();
                    relRows = new ArrayList<>();
                    for (var row : rels) {
                        Map<String, Object> out = new LinkedHashMap<>();
                        out.put("type", row.get("relTypes") != null ? row.get("relTypes").toString() : "");
                        out.put("targetId", row.get("targetId") != null ? row.get("targetId").toString() : "");
                        out.put("hopCount", row.get("hopCount"));
                        relRows.add(out);
                    }
                }
                m.put("relations", relRows);
            } catch (Exception e) {
                log.debug("[IndustryKG] Neo4j 查询详情失败: {}", e.getMessage());
            }
        }
        return m;
    }

    @Override
    public boolean isAvailable() {
        return enabled && (neo4jClient != null || knowledgeBaseService != null || nodeRepository != null);
    }

    @Override
    public String getGraphContextForQuery(String query, Long ownerId, int limit) {
        if (!enabled || query == null || query.isBlank() || nodeRepository == null || edgeRepository == null) {
            return "";
        }
        try {
            LinkedHashSet<String> relations = new LinkedHashSet<>();
            FreshnessSpan freshness = new FreshnessSpan();
            Long oid = ownerId != null ? ownerId : 0L;
            String qLower = query.toLowerCase(Locale.ROOT);
            int hopCap = effectiveMaxHops();
            int perNodeEdges = Math.max(1, Math.min(maxEdgesPerNode, 20));
            for (String token : extractEntityTokens(query)) {
                if (token.length() < 2) continue;
                List<AiGraphNode> nodes = nodeRepository.findByEntityNameContainingIgnoreCaseAndDeleted(token, 0);
                nodes = nodes.stream()
                        .filter(n -> n.getOwnerId() == null || n.getOwnerId().equals(oid) || n.getOwnerId().equals(0L))
                        .collect(Collectors.toList());
                nodes = rankNodesByTfIdfOverlap(qLower, token, nodes);
                for (AiGraphNode node : nodes.stream().limit(3).toList()) {
                    if (relations.size() >= limit) break;
                    freshness.accept(node.getUpdateTime());
                    String startName = node.getEntityName() != null ? node.getEntityName() : "?";
                    Set<Long> stack = new HashSet<>();
                    stack.add(node.getId());
                    collectRelationPathsDfs(node, startName, 0, hopCap, oid, relations, limit, perNodeEdges, stack, freshness, null);
                    stack.remove(node.getId());
                }
                if (relations.size() >= limit) break;
            }
            if (relations.isEmpty()) return "";
            String base = " [相关知识：" + String.join(", ", relations.stream().limit(limit).toList()) + "]";
            if (freshnessHint && freshness.hasSpan()) {
                base += String.format(" [图谱新鲜度：%d～%d 天前更新]",
                        freshness.daysSinceNewest(), freshness.daysSinceOldest());
            }
            return base;
        } catch (Exception e) {
            log.debug("[IndustryKG] getGraphContextForQuery failed: {}", e.getMessage());
            return "";
        }
    }

    @Override
    public Map<String, Object> getGraphJsonForQuery(String query, Long ownerId, int limit) {
        Map<String, Object> empty = new LinkedHashMap<>();
        empty.put("nodes", Collections.emptyList());
        empty.put("edges", Collections.emptyList());
        empty.put("contradictions", Collections.emptyList());
        if (!enabled || query == null || query.isBlank() || nodeRepository == null || edgeRepository == null) {
            return empty;
        }
        try {
            GraphSubgraphBuilder subgraph = new GraphSubgraphBuilder();
            Long oid = ownerId != null ? ownerId : 0L;
            String qLower = query.toLowerCase(Locale.ROOT);
            int hopCap = effectiveMaxHops();
            int perNodeEdges = Math.max(1, Math.min(maxEdgesPerNode, 20));
            LinkedHashSet<String> relationPaths = new LinkedHashSet<>();
            for (String token : extractEntityTokens(query)) {
                if (token.length() < 2) {
                    continue;
                }
                List<AiGraphNode> nodes = nodeRepository.findByEntityNameContainingIgnoreCaseAndDeleted(token, 0);
                nodes = nodes.stream()
                        .filter(n -> n.getOwnerId() == null || n.getOwnerId().equals(oid) || n.getOwnerId().equals(0L))
                        .collect(Collectors.toList());
                nodes = rankNodesByTfIdfOverlap(qLower, token, nodes);
                for (AiGraphNode node : nodes.stream().limit(3).toList()) {
                    if (relationPaths.size() >= limit) {
                        break;
                    }
                    FreshnessSpan freshness = new FreshnessSpan();
                    freshness.accept(node.getUpdateTime());
                    String startName = node.getEntityName() != null ? node.getEntityName() : "?";
                    Set<Long> stack = new HashSet<>();
                    stack.add(node.getId());
                    collectRelationPathsDfs(node, startName, 0, hopCap, oid, relationPaths, limit, perNodeEdges, stack, freshness, subgraph);
                    stack.remove(node.getId());
                }
                appendInferredCooccurrenceEdges(subgraph, qLower, token, nodes);
            }
            if (coMentionTransitiveHops >= 1) {
                subgraph.applyCoMentionTransitiveClosure(coMentionTransitiveDecay, coMentionTransitiveMinConfidence);
            }
            subgraph.detectContradictions();
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("nodes", subgraph.nodesList());
            out.put("edges", subgraph.getEdges());
            out.put("contradictions", subgraph.getContradictions());
            return out;
        } catch (Exception e) {
            log.debug("[IndustryKG] getGraphJsonForQuery failed: {}", e.getMessage());
            return empty;
        }
    }

    /** G-2：同一 token 下排序相邻的实体若无显式边则补一条低置信共现推断边 */
    private void appendInferredCooccurrenceEdges(GraphSubgraphBuilder subgraph,
                                                   String qLower,
                                                   String token,
                                                   List<AiGraphNode> rankedNodes) {
        if (rankedNodes == null || rankedNodes.size() < 2) {
            return;
        }
        List<String> terms = queryKeyTerms(qLower);
        int n = rankedNodes.size();
        AiGraphNode a = rankedNodes.get(0);
        AiGraphNode b = rankedNodes.get(1);
        Map<String, Integer> docFreq = new HashMap<>();
        for (String term : terms) {
            int df = 0;
            for (AiGraphNode node : rankedNodes) {
                String name = node.getEntityName() != null ? node.getEntityName().toLowerCase(Locale.ROOT) : "";
                if (name.contains(term)) {
                    df++;
                }
            }
            if (df > 0) {
                docFreq.put(term, df);
            }
        }
        String tok = token.toLowerCase(Locale.ROOT);
        double sa = scoreNodeTfIdf(a, terms, docFreq, n, tok);
        double sb = scoreNodeTfIdf(b, terms, docFreq, n, tok);
        if (sa < coMentionMinTfidf && sb < coMentionMinTfidf) {
            return;
        }
        if (subgraph.hasUndirectedEdge(a.getId(), b.getId())) {
            return;
        }
        subgraph.addNode(a);
        subgraph.addNode(b);
        subgraph.addInferredCoedge(a.getId(), b.getId(), coMentionInferredConfidence);
    }

    /**
     * G-5 子图收集器 + G-6 矛盾检测（互斥关系类型并存）。
     */
    private static final class GraphSubgraphBuilder {
        private final Map<Long, Map<String, Object>> nodes = new LinkedHashMap<>();
        private final List<Map<String, Object>> edges = new ArrayList<>();
        private final Set<String> edgeKeys = new HashSet<>();
        private List<Map<String, Object>> contradictions = new ArrayList<>();

        void addNode(AiGraphNode n) {
            if (n == null) {
                return;
            }
            nodes.computeIfAbsent(n.getId(), id -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", String.valueOf(id));
                m.put("name", n.getEntityName());
                m.put("type", n.getEntityType());
                m.put("inferred", false);
                m.put("sourceDocId", n.getSourceDocId());
                m.put("confidence", n.getConfidence());
                return m;
            });
        }

        void addEdge(AiGraphEdge e, long sourceId, long targetId, boolean inferred) {
            if (e == null) {
                return;
            }
            String rel = e.getRelationType() != null ? e.getRelationType() : "";
            String key = sourceId + "|" + targetId + "|" + rel + "|" + inferred;
            if (!edgeKeys.add(key)) {
                return;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", e.getId() != null ? String.valueOf(e.getId()) : ("e-" + Integer.toHexString(key.hashCode())));
            m.put("sourceNodeId", sourceId);
            m.put("targetNodeId", targetId);
            m.put("relation", e.getRelationType());
            m.put("confidence", e.getConfidence());
            m.put("sourceDocId", e.getSourceDocId());
            m.put("inferred", inferred);
            edges.add(m);
        }

        void addInferredCoedge(long aId, long bId, double confidence) {
            long x = Math.min(aId, bId);
            long y = Math.max(aId, bId);
            String key = x + "|co_mentioned|" + y + "|true";
            if (!edgeKeys.add(key)) {
                return;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", "inf-" + aId + "-" + bId);
            m.put("sourceNodeId", aId);
            m.put("targetNodeId", bId);
            m.put("relation", "co_mentioned");
            m.put("confidence", confidence);
            m.put("sourceDocId", null);
            m.put("inferred", true);
            edges.add(m);
        }

        /**
         * G-2：若存在 A-B、B-C 且两边 {@code relation=co_mentioned}（含 TF-IDF 推断边，不含 {@code co_mentioned_inferred}）、
         * 置信度均 ≥ 阈值，则增加弱边 A-C（{@code co_mentioned_inferred}，{@code inferred=true}），不参与 {@link #detectContradictions}。
         */
        void applyCoMentionTransitiveClosure(double decay, double minConfidenceForLeg) {
            if (decay <= 0 || minConfidenceForLeg < 0) {
                return;
            }
            Map<Long, List<CoLeg>> adj = new HashMap<>();
            for (Map<String, Object> e : edges) {
                if (!"co_mentioned".equals(String.valueOf(e.get("relation")))) {
                    continue;
                }
                Number confNum = e.get("confidence") instanceof Number ? (Number) e.get("confidence") : null;
                double c = confNum != null ? confNum.doubleValue() : 0;
                if (c < minConfidenceForLeg) {
                    continue;
                }
                Object s = e.get("sourceNodeId");
                Object t = e.get("targetNodeId");
                if (!(s instanceof Number) || !(t instanceof Number)) {
                    continue;
                }
                long a = ((Number) s).longValue();
                long b = ((Number) t).longValue();
                adj.computeIfAbsent(a, k -> new ArrayList<>()).add(new CoLeg(b, c));
                adj.computeIfAbsent(b, k -> new ArrayList<>()).add(new CoLeg(a, c));
            }
            for (Map.Entry<Long, List<CoLeg>> hubEn : adj.entrySet()) {
                List<CoLeg> nbs = hubEn.getValue();
                if (nbs.size() < 2) {
                    continue;
                }
                for (int i = 0; i < nbs.size(); i++) {
                    long na = nbs.get(i).nid;
                    double ca = nbs.get(i).confidence;
                    for (int j = i + 1; j < nbs.size(); j++) {
                        long nc = nbs.get(j).nid;
                        double cc = nbs.get(j).confidence;
                        if (na == nc) {
                            continue;
                        }
                        double newConf = Math.min(ca, cc) * decay;
                        if (newConf <= 0) {
                            continue;
                        }
                        addCoMentionInferredEdge(na, nc, Math.min(1d, newConf));
                    }
                }
            }
        }

        private static final class CoLeg {
            private final long nid;
            private final double confidence;

            private CoLeg(long nid, double confidence) {
                this.nid = nid;
                this.confidence = confidence;
            }
        }

        void addCoMentionInferredEdge(long aId, long bId, double confidence) {
            if (hasUndirectedEdge(aId, bId)) {
                return;
            }
            long x = Math.min(aId, bId);
            long y = Math.max(aId, bId);
            String key = x + "|co_mentioned_inferred|" + y + "|true";
            if (!edgeKeys.add(key)) {
                return;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", "inf-tr-" + aId + "-" + bId);
            m.put("sourceNodeId", aId);
            m.put("targetNodeId", bId);
            m.put("relation", "co_mentioned_inferred");
            m.put("confidence", confidence);
            m.put("sourceDocId", null);
            m.put("inferred", true);
            edges.add(m);
        }

        boolean hasUndirectedEdge(long a, long b) {
            for (Map<String, Object> e : edges) {
                Object s = e.get("sourceNodeId");
                Object t = e.get("targetNodeId");
                if (!(s instanceof Number) || !(t instanceof Number)) {
                    continue;
                }
                long si = ((Number) s).longValue();
                long ti = ((Number) t).longValue();
                if ((si == a && ti == b) || (si == b && ti == a)) {
                    return true;
                }
            }
            return false;
        }

        List<Map<String, Object>> nodesList() {
            return new ArrayList<>(nodes.values());
        }

        List<Map<String, Object>> getEdges() {
            return edges;
        }

        List<Map<String, Object>> getContradictions() {
            return contradictions;
        }

        void detectContradictions() {
            contradictions = new ArrayList<>();
            Map<String, Set<String>> pairToRels = new LinkedHashMap<>();
            for (Map<String, Object> e : edges) {
                if (Boolean.TRUE.equals(e.get("inferred"))) {
                    continue;
                }
                Object s = e.get("sourceNodeId");
                Object t = e.get("targetNodeId");
                if (!(s instanceof Number) || !(t instanceof Number)) {
                    continue;
                }
                long a = ((Number) s).longValue();
                long b = ((Number) t).longValue();
                long x = Math.min(a, b);
                long y = Math.max(a, b);
                String pk = x + ":" + y;
                pairToRels.computeIfAbsent(pk, k -> new LinkedHashSet<>())
                        .add(normalizeRel(String.valueOf(e.get("relation"))));
            }
            for (Map.Entry<String, Set<String>> en : pairToRels.entrySet()) {
                Set<String> rels = en.getValue();
                if (!hasMutexPair(rels)) {
                    continue;
                }
                String[] ab = en.getKey().split(":");
                long na = Long.parseLong(ab[0]);
                long nb = Long.parseLong(ab[1]);
                Map<String, Object> c = new LinkedHashMap<>();
                c.put("nodeIdA", na);
                c.put("nodeIdB", nb);
                c.put("conflictingRelations", new ArrayList<>(rels));
                c.put("reason", "mutually_exclusive_relation_types");
                contradictions.add(c);
            }
        }

        private static String normalizeRel(String r) {
            if (r == null) {
                return "";
            }
            return r.toLowerCase(Locale.ROOT).replace('-', '_').trim();
        }

        private static boolean hasMutexPair(Set<String> rels) {
            boolean comp = rels.stream().anyMatch(x -> x.contains("compete"));
            boolean part = rels.stream().anyMatch(x -> x.contains("partner"));
            return comp && part;
        }
    }

    private int effectiveMaxHops() {
        return Math.max(1, Math.min(maxHops, 4));
    }

    private boolean nodeVisibleToOwner(AiGraphNode n, Long ownerId) {
        Long oid = ownerId != null ? ownerId : 0L;
        Long ow = n.getOwnerId();
        return ow == null || ow.equals(oid) || ow.equals(0L);
    }

    /**
     * 按出边 DFS 收集路径串（entity-rel-entity-…），深度受 {@link #effectiveMaxHops()} 与全局 limit 约束。
     */
    private void collectRelationPathsDfs(AiGraphNode currentNode,
                                         String pathSoFar,
                                         int currentHop,
                                         int hopCap,
                                         Long ownerId,
                                         LinkedHashSet<String> relations,
                                         int globalLimit,
                                         int perNodeEdgeCap,
                                         Set<Long> pathStack,
                                         FreshnessSpan freshness,
                                         GraphSubgraphBuilder subgraph) {
        if (currentNode == null || relations.size() >= globalLimit || currentHop >= hopCap) {
            return;
        }
        if (subgraph != null) {
            subgraph.addNode(currentNode);
        }
        List<AiGraphEdge> edges = edgeRepository.findBySourceNodeIdAndDeleted(currentNode.getId(), 0);
        int edgeIndex = 0;
        for (AiGraphEdge e : edges) {
            if (edgeIndex >= perNodeEdgeCap) {
                break;
            }
            edgeIndex++;
            Optional<AiGraphNode> targetOpt = nodeRepository.findById(e.getTargetNodeId());
            if (targetOpt.isEmpty()) {
                continue;
            }
            AiGraphNode t = targetOpt.get();
            if (!nodeVisibleToOwner(t, ownerId)) {
                continue;
            }
            if (pathStack.contains(t.getId())) {
                continue;
            }
            freshness.accept(t.getUpdateTime());
            String tname = t.getEntityName() != null ? t.getEntityName() : "?";
            String nextPath = pathSoFar + "-" + e.getRelationType() + "-" + tname;
            relations.add(nextPath);
            if (subgraph != null) {
                subgraph.addNode(t);
                subgraph.addEdge(e, currentNode.getId(), t.getId(), false);
            }
            if (relations.size() >= globalLimit) {
                return;
            }
            pathStack.add(t.getId());
            collectRelationPathsDfs(t, nextPath, currentHop + 1, hopCap, ownerId, relations,
                    globalLimit, perNodeEdgeCap, pathStack, freshness, subgraph);
            pathStack.remove(t.getId());
            if (relations.size() >= globalLimit) {
                return;
            }
        }
    }

    /** DFS 触及实体的 update_time 最小/最大跨度，用于 GraphRAG 新鲜度提示 */
    private static final class FreshnessSpan {
        private LocalDateTime newest;
        private LocalDateTime oldest;

        void accept(LocalDateTime t) {
            if (t == null) {
                return;
            }
            if (newest == null || t.isAfter(newest)) {
                newest = t;
            }
            if (oldest == null || t.isBefore(oldest)) {
                oldest = t;
            }
        }

        boolean hasSpan() {
            return newest != null && oldest != null;
        }

        long daysSinceNewest() {
            return Math.max(0, ChronoUnit.DAYS.between(newest, LocalDateTime.now()));
        }

        long daysSinceOldest() {
            return Math.max(0, ChronoUnit.DAYS.between(oldest, LocalDateTime.now()));
        }
    }

    private List<String> extractEntityTokens(String query) {
        if (query == null || query.isBlank()) return List.of();
        return Arrays.stream(query.replaceAll("[，。、；：！？\\s]+", " ").split(" "))
                .filter(s -> s.length() >= 2)
                .distinct()
                .limit(5)
                .toList();
    }

    /**
     * 多节点命中时的轻量消歧：query 词项 TF × 文档频率近似 IDF，优先与完整查询重合度高的实体。
     */
    private List<AiGraphNode> rankNodesByTfIdfOverlap(String queryLower, String token, List<AiGraphNode> nodes) {
        if (nodes == null || nodes.isEmpty()) return List.of();
        if (nodes.size() == 1) return nodes;
        List<String> terms = queryKeyTerms(queryLower);
        int n = nodes.size();
        Map<String, Integer> docFreq = new HashMap<>();
        for (String term : terms) {
            int df = 0;
            for (AiGraphNode node : nodes) {
                String name = node.getEntityName() != null ? node.getEntityName().toLowerCase(Locale.ROOT) : "";
                if (name.contains(term)) {
                    df++;
                }
            }
            if (df > 0) {
                docFreq.put(term, df);
            }
        }
        final String tok = token.toLowerCase(Locale.ROOT);
        return nodes.stream()
                .sorted(Comparator.comparingDouble((AiGraphNode node) -> -scoreNodeTfIdf(node, terms, docFreq, n, tok)))
                .collect(Collectors.toList());
    }

    private List<String> queryKeyTerms(String queryLower) {
        return Arrays.stream(queryLower.replaceAll("[，。、；：！？\\s]+", " ").split(" "))
                .filter(s -> s.length() >= 2)
                .distinct()
                .limit(12)
                .collect(Collectors.toList());
    }

    private double scoreNodeTfIdf(AiGraphNode node, List<String> terms, Map<String, Integer> docFreq, int n, String token) {
        String en = node.getEntityName() != null ? node.getEntityName().toLowerCase(Locale.ROOT) : "";
        double s = 0;
        for (String term : terms) {
            if (!en.contains(term)) {
                continue;
            }
            int df = docFreq.getOrDefault(term, 1);
            double idf = Math.log((double) (n + 1) / (df + 1)) + 1.0;
            int occ = 0;
            int from = 0;
            while (from <= en.length() - term.length() && (from = en.indexOf(term, from)) >= 0) {
                occ++;
                from += Math.max(1, term.length());
            }
            double tf = (occ * (double) term.length()) / Math.max(8, en.length());
            s += tf * idf;
        }
        if (en.equals(token)) {
            s += 2.0;
        } else if (en.contains(token)) {
            s += 1.0;
        }
        return s;
    }

    private List<Map<String, Object>> queryFromNeo4j(String entityType, String keyword, int limit, Long ownerId) {
        try {
            String cypher = "MATCH (e:Entity) WHERE e.type = $type AND (e.name CONTAINS $kw OR e.name IS NULL) RETURN e.id as id, e.name as name, e.type as type LIMIT $limit";
            if (keyword == null || keyword.isBlank()) {
                cypher = "MATCH (e:Entity) WHERE e.type = $type RETURN e.id as id, e.name as name, e.type as type LIMIT $limit";
            }
            var params = new HashMap<String, Object>();
            params.put("type", entityType);
            params.put("kw", keyword != null ? keyword : "");
            params.put("limit", limit);
            var rows = neo4jClient.query(cypher).bindAll(params).fetch().all();
            return rows.stream().map(r -> Map.<String, Object>of(
                    "entityId", r.get("id") != null ? r.get("id").toString() : "",
                    "entityType", r.get("type") != null ? r.get("type").toString() : entityType,
                    "name", r.get("name") != null ? r.get("name").toString() : ""
            )).toList();
        } catch (Exception e) {
            log.debug("[IndustryKG] Neo4j 查询失败，回退知识库: {}", e.getMessage());
            return queryFromKnowledgeBase(entityType, keyword, limit, ownerId);
        }
    }

    private List<Map<String, Object>> queryFromKnowledgeBase(String entityType, String keyword, int limit, Long ownerId) {
        if (knowledgeBaseService == null) return Collections.emptyList();
        try {
            Long effectiveOwnerId = resolveKnowledgeBaseOwner(ownerId);
            if (effectiveOwnerId == null) {
                return Collections.emptyList();
            }
            var kbs = knowledgeBaseService.listKnowledgeBases(effectiveOwnerId);
            if (kbs.isEmpty()) return Collections.emptyList();
            Long kbId = kbs.get(0).getId();
            var results = knowledgeBaseService.hybridSearch(
                    kbId,
                    keyword != null && !keyword.isBlank() ? keyword : entityType,
                    Math.min(limit, 20),
                    effectiveOwnerId);
            return results.stream().map(r -> {
                Map<String, Object> m = new HashMap<>();
                m.put("entityId", "doc_" + r.docId());
                m.put("entityType", entityType);
                String text = r.title() != null && !r.title().isBlank() ? r.title() : (r.content() != null ? r.content() : "");
                m.put("name", text.length() > 100 ? text.substring(0, 100) : text);
                m.put("score", r.score());
                return m;
            }).collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("[IndustryKG] 知识库查询失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private Long resolveKnowledgeBaseOwner(Long ownerId) {
        if (ownerId != null) {
            var ownerKbs = knowledgeBaseService.listKnowledgeBases(ownerId);
            if (ownerKbs != null && !ownerKbs.isEmpty()) {
                return ownerId;
            }
        }
        if (!allowSharedKbFallback || sharedKbOwnerId == null || sharedKbOwnerId < 0) {
            return null;
        }
        if (ownerId != null && ownerId.equals(sharedKbOwnerId)) {
            return ownerId;
        }
        var sharedKbs = knowledgeBaseService.listKnowledgeBases(sharedKbOwnerId);
        return sharedKbs != null && !sharedKbs.isEmpty() ? sharedKbOwnerId : null;
    }

    @Override
    public List<Map<String, Object>> listRelationSuggestions(Long ownerId, String status, int limit) {
        if (graphRelationSuggestionRepository == null) {
            return Collections.emptyList();
        }
        String st = org.springframework.util.StringUtils.hasText(status) ? status.trim() : "pending";
        long oid = ownerId != null ? ownerId : 0L;
        int cap = Math.max(1, Math.min(limit, 50));
        List<AiGraphRelationSuggestion> rows = graphRelationSuggestionRepository
                .findTop50ByOwnerIdAndStatusAndDeletedOrderByCreateTimeDesc(oid, st, 0);
        return rows.stream().limit(cap).map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId());
            m.put("ownerId", s.getOwnerId());
            m.put("sourceEntityKey", s.getSourceEntityKey());
            m.put("targetEntityKey", s.getTargetEntityKey());
            m.put("relationType", s.getRelationType());
            m.put("confidence", s.getConfidence());
            m.put("status", s.getStatus());
            m.put("evidenceJson", s.getEvidenceJson());
            m.put("createTime", s.getCreateTime());
            return m;
        }).toList();
    }

    @Override
    public int materializeRelationSuggestions(Long ownerId, List<Map<String, Object>> pairs) {
        if (graphRelationSuggestionRepository == null) {
            return 0;
        }
        if (ownerId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (pairs == null || pairs.isEmpty()) {
            return 0;
        }
        int n = 0;
        int cap = 0;
        for (Map<String, Object> p : pairs) {
            if (p == null || cap >= 40) {
                break;
            }
            String src = p.get("sourceEntityKey") != null ? p.get("sourceEntityKey").toString().trim() : "";
            String tgt = p.get("targetEntityKey") != null ? p.get("targetEntityKey").toString().trim() : "";
            String rel = p.get("relationType") != null ? p.get("relationType").toString().trim() : "co_mentioned";
            if (src.isEmpty() || tgt.isEmpty() || src.length() > 500 || tgt.length() > 500) {
                continue;
            }
            double conf = 0.5;
            if (p.get("confidence") instanceof Number num) {
                conf = Math.min(1.0, Math.max(0.0, num.doubleValue()));
            }
            boolean exists = graphRelationSuggestionRepository.existsByOwnerIdAndSourceEntityKeyAndTargetEntityKeyAndRelationTypeAndDeleted(
                    ownerId, src, tgt, rel, 0);
            if (exists) {
                continue;
            }
            AiGraphRelationSuggestion row = new AiGraphRelationSuggestion();
            row.setOwnerId(ownerId);
            row.setSourceEntityKey(src);
            row.setTargetEntityKey(tgt);
            row.setRelationType(rel.length() > 60 ? rel.substring(0, 60) : rel);
            row.setConfidence(conf);
            row.setStatus("pending");
            row.setEvidenceJson(pgetEvidenceJson(p.get("evidence")));
            graphRelationSuggestionRepository.save(row);
            n++;
            cap++;
        }
        return n;
    }

    @Override
    public void updateRelationSuggestionStatus(Long ownerId, Long id, String newStatus) {
        if (graphRelationSuggestionRepository == null) {
            return;
        }
        if (ownerId == null || id == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "ownerId/id 不能为空");
        }
        String st = newStatus != null ? newStatus.trim().toLowerCase(Locale.ROOT) : "";
        if (!List.of("pending", "approved", "rejected").contains(st)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "status 须为 pending|approved|rejected");
        }
        AiGraphRelationSuggestion row = graphRelationSuggestionRepository.findByIdAndOwnerIdAndDeleted(id, ownerId, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "建议不存在"));
        row.setStatus(st);
        graphRelationSuggestionRepository.save(row);
    }

    private static String pgetEvidenceJson(Object evidence) {
        if (evidence == null) {
            return null;
        }
        if (evidence instanceof String s) {
            return s.length() > 8000 ? s.substring(0, 8000) : s;
        }
        try {
            return com.alibaba.fastjson2.JSON.toJSONString(evidence);
        } catch (Exception e) {
            return String.valueOf(evidence);
        }
    }
}
