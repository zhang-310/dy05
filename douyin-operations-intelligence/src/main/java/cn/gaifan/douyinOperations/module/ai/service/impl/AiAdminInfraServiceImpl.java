package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.ai.entity.AiKbDocument;
import cn.gaifan.douyinOperations.module.ai.entity.AiIndexQueue;
import cn.gaifan.douyinOperations.module.ai.repository.AiIndexQueueRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiKbDocumentRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiKnowledgeBaseRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.ai.config.SearchMetricsCollector;
import cn.gaifan.douyinOperations.module.ai.service.AiAdminInfraService;
import cn.gaifan.douyinOperations.module.ai.service.SearchService;
import cn.gaifan.douyinOperations.module.ai.service.VectorService;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.HealthStatus;
import co.elastic.clients.elasticsearch.cluster.HealthRequest;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.R;
import io.milvus.param.collection.GetCollectionStatisticsParam;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.response.GetCollStatResponseWrapper;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Value;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AiAdminInfraServiceImpl implements AiAdminInfraService {

    @Resource
    private AiKbDocumentRepository documentRepository;

    @Resource
    private AiIndexQueueRepository indexQueueRepository;

    @Resource
    private AiKnowledgeBaseRepository knowledgeBaseRepository;

    @Resource
    private SearchService searchService;

    @Resource
    private VectorService vectorService;

    @Autowired(required = false)
    private MilvusServiceClient milvusClient;

    @Autowired(required = false)
    private ElasticsearchClient esClient;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    @Autowired(required = false)
    private SearchMetricsCollector searchMetricsCollector;

    @Resource
    private DataSource dataSource;

    @Autowired(required = false)
    private LlmClient llmClient;

    @Resource
    private AiModelRepository aiModelRepository;

    @Value("${app.milvus.host:localhost}")
    private String milvusHost;

    @Value("${app.milvus.port:19530}")
    private int milvusPort;

    @Value("${app.elasticsearch.uris:http://localhost:9200}")
    private String esUris;

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6380}")
    private int redisPort;

    @Override
    public List<InfraHealthItem> checkHealth() {
        List<InfraHealthItem> items = new ArrayList<>();

        // Milvus
        if (milvusClient == null) {
            items.add(new InfraHealthItem("milvus", false, "未配置"));
        } else {
            try {
                milvusClient.hasCollection(HasCollectionParam.newBuilder().withCollectionName("__health__").build());
                items.add(new InfraHealthItem("milvus", true, null));
            } catch (Exception e) {
                items.add(new InfraHealthItem("milvus", false, e.getMessage()));
            }
        }

        // Elasticsearch
        if (esClient == null) {
            items.add(new InfraHealthItem("elasticsearch", false, "未配置"));
        } else {
            try {
                esClient.cluster().health(HealthRequest.of(h -> h));
                items.add(new InfraHealthItem("elasticsearch", true, null));
            } catch (Exception e) {
                items.add(new InfraHealthItem("elasticsearch", false, e.getMessage()));
            }
        }

        // Redis
        if (stringRedisTemplate == null) {
            items.add(new InfraHealthItem("redis", false, "未配置"));
        } else {
            try {
                stringRedisTemplate.getConnectionFactory().getConnection().ping();
                items.add(new InfraHealthItem("redis", true, null));
            } catch (Exception e) {
                items.add(new InfraHealthItem("redis", false, e.getMessage()));
            }
        }

        // LLM 网关：客户端 Bean 存在且库中至少有一条启用模型（与 OpenAiCompatibleLlmClient 实际调用条件一致）
        if (llmClient == null) {
            items.add(new InfraHealthItem("llm", false, "未配置"));
        } else {
            try {
                var activeModels = aiModelRepository.findByStatusAndDeleted(1, 0);
                if (activeModels != null && !activeModels.isEmpty()) {
                    items.add(new InfraHealthItem("llm", true, null));
                } else {
                    items.add(new InfraHealthItem("llm", false, "无启用模型，请在模型配置中添加"));
                }
            } catch (Exception e) {
                items.add(new InfraHealthItem("llm", false, e.getMessage()));
            }
        }

        return items;
    }

    @Override
    public PageResultVO<Map<String, Object>> pagePgDocuments(Long kbId, String keyword, int page, int rows) {
        Specification<AiKbDocument> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), 0));
            if (kbId != null) {
                predicates.add(cb.equal(root.get("kbId"), kbId));
            }
            if (keyword != null && !keyword.isBlank()) {
                predicates.add(cb.like(root.get("title"), "%" + keyword.trim() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        PageRequest pageable = PageRequest.of(page, rows, Sort.by(Sort.Direction.DESC, "createTime"));
        var slice = documentRepository.findAll(spec, pageable);
        long total = documentRepository.count(spec);

        List<Map<String, Object>> list = slice.getContent().stream()
                .map(this::docToMap)
                .collect(Collectors.toList());

        return PageResultVO.of(total, list, page, rows);
    }

    @Override
    public PageResultVO<Map<String, Object>> pageEsDocuments(Long kbId, String keyword, int page, int rows) {
        if (kbId == null) {
            return PageResultVO.of(0L, Collections.emptyList(), page, rows);
        }
        String indexName = "kb_" + kbId;
        String query = (keyword != null && !keyword.isBlank()) ? keyword.trim() : "";
        Map<String, Object> filters = null;

        int from = page * rows;
        var results = searchService.search(indexName, query, from, rows, filters);

        List<Map<String, Object>> list = results.stream()
                .map(r -> {
                    Map<String, Object> m = new HashMap<>(r.source());
                    m.put("_id", r.id());
                    m.put("_score", r.score());
                    return m;
                })
                .collect(Collectors.toList());

        // ES 分页 total 近似：若本页满则 total >= (page+1)*rows
        long total = list.size() < rows ? (long) page * rows + list.size() : (long) (page + 2) * rows;
        return PageResultVO.of(total, list, page, rows);
    }

    @Override
    public Map<String, Object> getMilvusStats(Long kbId) {
        Map<String, Object> result = new HashMap<>();
        result.put("kbId", kbId);
        result.put("collectionName", "kb_" + kbId);
        result.put("available", false);
        result.put("rowCount", 0L);

        if (milvusClient == null || kbId == null) {
            return result;
        }

        try {
            String collectionName = "kb_" + kbId;
            R<Boolean> has = milvusClient.hasCollection(
                    io.milvus.param.collection.HasCollectionParam.newBuilder()
                            .withCollectionName(collectionName)
                            .build()
            );
            if (has.getData() == null || !has.getData()) {
                return result;
            }

            result.put("available", true);
            R<io.milvus.grpc.GetCollectionStatisticsResponse> statResp = milvusClient.getCollectionStatistics(
                    GetCollectionStatisticsParam.newBuilder()
                            .withCollectionName(collectionName)
                            .build()
            );
            if (statResp.getStatus() == R.Status.Success.getCode() && statResp.getData() != null) {
                GetCollStatResponseWrapper wrapper = new GetCollStatResponseWrapper(statResp.getData());
                result.put("rowCount", wrapper.getRowCount());
            }
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        return result;
    }

    @Override
    public PageResultVO<Map<String, Object>> pageIndexQueue(Long kbId, String status, String keyword, int page, int rows) {
        Specification<AiIndexQueue> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (kbId != null) {
                predicates.add(cb.equal(root.get("targetKbId"), kbId));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status.trim()));
            }
            if (keyword != null && !keyword.isBlank()) {
                String k = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("content"), k),
                        cb.like(root.get("sourceType"), k)
                ));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };

        PageRequest pageable = PageRequest.of(page, rows, Sort.by(Sort.Direction.DESC, "createTime"));
        var slice = indexQueueRepository.findAll(spec, pageable);
        long total = indexQueueRepository.count(spec);

        List<Map<String, Object>> list = slice.getContent().stream()
                .map(this::queueToMap)
                .collect(Collectors.toList());

        return PageResultVO.of(total, list, page, rows);
    }

    private Map<String, Object> docToMap(AiKbDocument d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", d.getId());
        m.put("kbId", d.getKbId());
        m.put("title", d.getTitle());
        m.put("fileType", d.getFileType());
        m.put("chunkCount", d.getChunkCount());
        m.put("tokenCount", d.getTokenCount());
        m.put("status", d.getStatus());
        m.put("createTime", d.getCreateTime());
        return m;
    }

    private Map<String, Object> queueToMap(AiIndexQueue q) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", q.getId());
        m.put("sourceType", q.getSourceType());
        m.put("sourceId", q.getSourceId());
        m.put("targetKbId", q.getTargetKbId());
        m.put("priority", q.getPriority());
        m.put("status", q.getStatus());
        m.put("retryCount", q.getRetryCount());
        m.put("errorMsg", q.getErrorMsg());
        m.put("createTime", q.getCreateTime());
        m.put("contentPreview", q.getContent() != null && q.getContent().length() > 200
                ? q.getContent().substring(0, 200) + "..." : q.getContent());
        return m;
    }

    @Override
    public Map<String, Object> getCacheStats() {
        Map<String, Object> result = new LinkedHashMap<>();
        if (stringRedisTemplate == null) {
            result.put("hit", 0);
            result.put("miss", 0);
            result.put("hitRate", 0.0);
            result.put("message", "Redis 未配置");
            return result;
        }
        try {
            String hitStr = stringRedisTemplate.opsForValue().get("stats:kb:cache:hit");
            String missStr = stringRedisTemplate.opsForValue().get("stats:kb:cache:miss");
            long hit = hitStr != null ? Long.parseLong(hitStr) : 0;
            long miss = missStr != null ? Long.parseLong(missStr) : 0;
            long total = hit + miss;
            double hitRate = total > 0 ? (double) hit / total : 0.0;
            result.put("hit", hit);
            result.put("miss", miss);
            result.put("total", total);
            result.put("hitRate", Math.round(hitRate * 10000) / 100.0);
            // keyCount: 与 KnowledgeBaseServiceImpl.cacheKey() 一致，前缀为 cache:kb:{kbId}:{hash}
            try {
                var keys = stringRedisTemplate.keys("cache:kb:*");
                result.put("keyCount", keys != null ? keys.size() : 0);
            } catch (Exception ignored) {
                result.put("keyCount", 0);
            }
        } catch (Exception e) {
            result.put("hit", 0);
            result.put("miss", 0);
            result.put("hitRate", 0.0);
            result.put("message", "读取失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> getSearchStats() {
        if (searchMetricsCollector == null) {
            return Map.of("message", "SearchMetricsCollector 未配置");
        }
        return searchMetricsCollector.getStats();
    }

    @Override
    public Map<String, Object> getInfraDetail() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("lastUpdate", java.time.LocalDateTime.now().toString());
        result.put("postgresql", buildPostgresqlDetail());
        result.put("milvus", buildMilvusDetail());
        result.put("elasticsearch", buildElasticsearchDetail());
        result.put("redis", buildRedisDetail());
        result.put("suggestions", buildSuggestions(result));
        return result;
    }

    private Map<String, Object> buildPostgresqlDetail() {
        Map<String, Object> m = new LinkedHashMap<>();
        try {
            String url = "";
            int poolActive = 0, poolTotal = 20;
            long responseMs = 0;
            if (dataSource != null) {
                try (Connection c = dataSource.getConnection()) {
                    url = c.getMetaData().getURL();
                    if (dataSource instanceof com.zaxxer.hikari.HikariDataSource hds) {
                        var pool = hds.getHikariPoolMXBean();
                        if (pool != null) {
                            poolActive = pool.getActiveConnections();
                            poolTotal = pool.getTotalConnections();
                        }
                    }
                    long t0 = System.currentTimeMillis();
                    c.createStatement().executeQuery("SELECT 1").close();
                    responseMs = System.currentTimeMillis() - t0;
                }
            }
            String host = "localhost";
            String database = "douyin_operations";
            if (url != null && url.startsWith("jdbc:postgresql://")) {
                String part = url.substring("jdbc:postgresql://".length());
                int slash = part.indexOf('/');
                if (slash > 0) {
                    host = part.substring(0, slash);
                    int q = part.indexOf('?', slash);
                    database = q > 0 ? part.substring(slash + 1, q) : part.substring(slash + 1);
                }
            }
            m.put("ok", true);
            m.put("host", host);
            m.put("database", database);
            m.put("poolActive", poolActive);
            m.put("poolTotal", poolTotal);
            m.put("responseMs", responseMs);
        } catch (Exception e) {
            m.put("ok", false);
            m.put("error", e.getMessage());
        }
        return m;
    }

    private Map<String, Object> buildMilvusDetail() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("host", milvusHost + ":" + milvusPort);
        if (milvusClient == null) {
            m.put("ok", false);
            m.put("error", "未配置");
            return m;
        }
        try {
            List<Map<String, Object>> collections = new ArrayList<>();
            long totalVectors = 0;
            var kbIds = knowledgeBaseRepository.findAll().stream().map(kb -> kb.getId()).filter(Objects::nonNull).toList();
            for (Object id : kbIds) {
                Long kbId = id instanceof Long l ? l : Long.valueOf(id.toString());
                var stat = getMilvusStats(kbId);
                if (Boolean.TRUE.equals(stat.get("available"))) {
                    long rows = ((Number) stat.getOrDefault("rowCount", 0L)).longValue();
                    totalVectors += rows;
                    collections.add(Map.of("name", "kb_" + kbId, "rowCount", rows));
                }
            }
            m.put("ok", true);
            m.put("collections", collections);
            m.put("collectionCount", collections.size());
            m.put("totalVectors", totalVectors);
        } catch (Exception e) {
            m.put("ok", false);
            m.put("error", e.getMessage());
        }
        return m;
    }

    private Map<String, Object> buildElasticsearchDetail() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("cluster", esUris);
        if (esClient == null) {
            m.put("ok", false);
            m.put("error", "未配置");
            return m;
        }
        try {
            var health = esClient.cluster().health();
            m.put("ok", health.status() != HealthStatus.Red);
            m.put("status", health.status().jsonValue());
            m.put("clusterName", health.clusterName());
            try {
                var indicesResp = esClient.cat().indices();
                List<Map<String, Object>> indexList = new ArrayList<>();
                for (var r : indicesResp.valueBody()) {
                    String idx = r.index();
                    if (idx != null && !idx.startsWith(".")) {
                        String docsStr = r.docsCount();
                        long docs = 0;
                        if (docsStr != null && !docsStr.isBlank()) {
                            try {
                                docs = Long.parseLong(docsStr.replace("k", "000").replace("m", "000000").replaceAll("[^0-9]", ""));
                            } catch (NumberFormatException ignored) {}
                        }
                        indexList.add(Map.<String, Object>of("name", idx, "docs", docs, "store", r.storeSize() != null ? r.storeSize() : "-"));
                    }
                }
                m.put("indexCount", indexList.size());
                m.put("totalDocs", indexList.stream().mapToLong(x -> ((Number) x.get("docs")).longValue()).sum());
                m.put("indices", indexList.size() > 10 ? indexList.subList(0, 10) : indexList);
            } catch (Exception ie) {
                m.put("indicesError", ie.getMessage());
            }
        } catch (Exception e) {
            m.put("ok", false);
            m.put("error", e.getMessage());
        }
        return m;
    }

    private Map<String, Object> buildRedisDetail() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("host", redisHost + ":" + redisPort);
        if (stringRedisTemplate == null) {
            m.put("ok", false);
            m.put("error", "未配置");
            return m;
        }
        try {
            stringRedisTemplate.getConnectionFactory().getConnection().ping();
            m.put("ok", true);
            Long dbsize = stringRedisTemplate.getConnectionFactory().getConnection().dbSize();
            m.put("keys", dbsize != null ? dbsize : 0);
            var cacheStats = getCacheStats();
            m.put("hitRate", cacheStats.get("hitRate"));
            m.put("hit", cacheStats.get("hit"));
            m.put("miss", cacheStats.get("miss"));
        } catch (Exception e) {
            m.put("ok", false);
            m.put("error", e.getMessage());
        }
        return m;
    }

    private List<Map<String, Object>> buildSuggestions(Map<String, Object> detail) {
        List<Map<String, Object>> list = new ArrayList<>();
        var pg = (Map<String, Object>) detail.get("postgresql");
        if (pg != null && Boolean.TRUE.equals(pg.get("ok"))) {
            Object poolTotal = pg.get("poolTotal");
            if (poolTotal instanceof Number pt && pt.intValue() >= 25) {
                list.add(Map.of("component", "PostgreSQL", "message", "连接池使用率较高，建议检查慢查询或增加 maximum-pool-size"));
            }
        }
        var milvus = (Map<String, Object>) detail.get("milvus");
        if (milvus != null && Boolean.TRUE.equals(milvus.get("ok"))) {
            var colls = (List<?>) milvus.get("collections");
            if (colls != null && colls.isEmpty()) {
                list.add(Map.of("component", "Milvus", "message", "暂无向量集合，创建知识库并导入文档后可生成"));
            }
        }
        var redis = (Map<String, Object>) detail.get("redis");
        if (redis != null && Boolean.TRUE.equals(redis.get("ok"))) {
            Object hitRate = redis.get("hitRate");
            if (hitRate instanceof Number hr && hr.doubleValue() < 50 && hr.doubleValue() > 0) {
                list.add(Map.of("component", "Redis", "message", "缓存命中率偏低 (" + hr + "%)，建议检查热点 key 或增加缓存 TTL"));
            }
        }
        return list;
    }
}
