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
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Value;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AiAdminInfraServiceImpl implements AiAdminInfraService {

    private static final Logger log = LoggerFactory.getLogger(AiAdminInfraServiceImpl.class);

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

    @Value("${app.ai.kb.cache-ttl-seconds:21600}")
    private long kbCacheTtlSeconds;

    @Value("${app.ai.embedding.cache-ttl-days:14}")
    private int embeddingCacheTtlDays;

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
                log.warn("Milvus健康检查失败: {}", e.getMessage());
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
                log.warn("Elasticsearch健康检查失败: {}", e.getMessage());
                items.add(new InfraHealthItem("elasticsearch", false, e.getMessage()));
            }
        }

        // Redis
        if (stringRedisTemplate == null) {
            items.add(new InfraHealthItem("redis", false, "未配置"));
        } else {
            try {
                try (RedisConnection connection = stringRedisTemplate.getConnectionFactory().getConnection()) {
                    connection.ping();
                }
                items.add(new InfraHealthItem("redis", true, null));
            } catch (Exception e) {
                log.warn("Redis健康检查失败: {}", e.getMessage());
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
                log.warn("LLM健康检查失败: {}", e.getMessage());
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
            log.warn("Milvus统计查询失败 kbId={}: {}", kbId, e.getMessage());
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
            result.put("keyCount", countKeysByScan("cache:kb:*", 10000));
        } catch (Exception e) {
            log.warn("缓存统计查询失败: {}", e.getMessage());
            result.put("hit", 0);
            result.put("miss", 0);
            result.put("hitRate", 0.0);
            result.put("message", "读取失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> getCacheDiagnostics() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("host", redisHost + ":" + redisPort);
        result.put("lastUpdate", java.time.LocalDateTime.now().toString());
        result.put("kbCacheTtlSeconds", Math.max(300, kbCacheTtlSeconds));
        result.put("embeddingCacheTtlDays", Math.max(1, embeddingCacheTtlDays));

        if (stringRedisTemplate == null || stringRedisTemplate.getConnectionFactory() == null) {
            result.put("ok", false);
            result.put("message", "Redis 未配置");
            result.put("suggestions", List.of("检查 spring.data.redis.host/port，并确认 Docker 容器可访问 Redis"));
            return result;
        }

        try (RedisConnection connection = stringRedisTemplate.getConnectionFactory().getConnection()) {
            connection.ping();
            result.put("ok", true);
            result.put("dbSize", safeNumber(connection.serverCommands().dbSize()));

            Properties statsInfo = safeInfo(connection, "stats");
            Properties keyspaceInfo = safeInfo(connection, "keyspace");
            Properties memoryInfo = safeInfo(connection, "memory");

            long keyspaceHits = propertyLong(statsInfo, "keyspace_hits");
            long keyspaceMisses = propertyLong(statsInfo, "keyspace_misses");
            long globalTotal = keyspaceHits + keyspaceMisses;
            long expiredKeys = propertyLong(statsInfo, "expired_keys");
            long evictedKeys = propertyLong(statsInfo, "evicted_keys");

            Map<String, Object> redisStats = new LinkedHashMap<>();
            redisStats.put("keyspaceHits", keyspaceHits);
            redisStats.put("keyspaceMisses", keyspaceMisses);
            redisStats.put("globalHitRate", percent(keyspaceHits, globalTotal));
            redisStats.put("expiredKeys", expiredKeys);
            redisStats.put("evictedKeys", evictedKeys);
            redisStats.put("usedMemoryHuman", propertyString(memoryInfo, "used_memory_human"));
            redisStats.put("usedMemoryPeakHuman", propertyString(memoryInfo, "used_memory_peak_human"));
            redisStats.put("maxMemoryHuman", propertyString(memoryInfo, "maxmemory_human"));
            redisStats.put("memFragmentationRatio", propertyDouble(memoryInfo, "mem_fragmentation_ratio"));
            redisStats.put("keyspace", summarizeKeyspace(keyspaceInfo));
            result.put("redisStats", redisStats);

            Map<String, Object> businessStats = new LinkedHashMap<>(getCacheStats());
            businessStats.put("scope", "stats:kb:cache:*");
            businessStats.put("description", "仅统计知识库混合检索 cache:kb:* 的业务命中/未命中，不等同于 Redis 全局命中率");
            result.put("businessStats", businessStats);

            ScanSummary scanSummary = scanRedisKeys(connection);
            result.put("scan", scanSummary.toMap());
            result.put("suggestions", buildCacheDiagnosticSuggestions(
                    keyspaceHits,
                    keyspaceMisses,
                    expiredKeys,
                    evictedKeys,
                    businessStats,
                    scanSummary
            ));
        } catch (Exception e) {
            log.warn("Redis缓存诊断失败: {}", e.getMessage());
            result.put("ok", false);
            result.put("message", "读取失败: " + e.getMessage());
            result.put("suggestions", List.of("确认 Redis 服务、网络和认证配置正常后重试诊断"));
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
            log.warn("PostgreSQL详情查询失败: {}", e.getMessage());
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
            log.warn("Milvus详情查询失败: {}", e.getMessage());
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
                            } catch (NumberFormatException ignored) {
                                log.debug("ES文档数解析失败: {}", docsStr);
                            }
                        }
                        indexList.add(Map.<String, Object>of("name", idx, "docs", docs, "store", r.storeSize() != null ? r.storeSize() : "-"));
                    }
                }
                m.put("indexCount", indexList.size());
                m.put("totalDocs", indexList.stream().mapToLong(x -> ((Number) x.get("docs")).longValue()).sum());
                m.put("indices", indexList.size() > 10 ? indexList.subList(0, 10) : indexList);
            } catch (Exception ie) {
                log.warn("ES索引列表查询失败: {}", ie.getMessage());
                m.put("indicesError", ie.getMessage());
            }
        } catch (Exception e) {
            log.warn("Elasticsearch详情查询失败: {}", e.getMessage());
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
            try (RedisConnection connection = stringRedisTemplate.getConnectionFactory().getConnection()) {
                connection.ping();
                m.put("ok", true);
                Long dbsize = connection.serverCommands().dbSize();
                m.put("keys", dbsize != null ? dbsize : 0);
            }
            var cacheStats = getCacheStats();
            m.put("hitRate", cacheStats.get("hitRate"));
            m.put("hit", cacheStats.get("hit"));
            m.put("miss", cacheStats.get("miss"));
        } catch (Exception e) {
            log.warn("Redis详情查询失败: {}", e.getMessage());
            m.put("ok", false);
            m.put("error", e.getMessage());
        }
        return m;
    }

    private Properties safeInfo(RedisConnection connection, String section) {
        try {
            Properties props = connection.serverCommands().info(section);
            return props != null ? props : new Properties();
        } catch (Exception e) {
            log.debug("Redis INFO {} 查询失败: {}", section, e.getMessage());
            return new Properties();
        }
    }

    private Map<String, Object> summarizeKeyspace(Properties keyspaceInfo) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String name : keyspaceInfo.stringPropertyNames()) {
            if (!name.startsWith("db")) {
                continue;
            }
            Map<String, Object> db = new LinkedHashMap<>();
            String value = keyspaceInfo.getProperty(name, "");
            for (String part : value.split(",")) {
                int idx = part.indexOf('=');
                if (idx <= 0) {
                    continue;
                }
                String key = part.substring(0, idx).trim();
                String raw = part.substring(idx + 1).trim();
                db.put(key, parseLongOrString(raw));
            }
            result.put(name, db);
        }
        return result;
    }

    private ScanSummary scanRedisKeys(RedisConnection connection) {
        ScanSummary summary = new ScanSummary();
        ScanOptions options = ScanOptions.scanOptions().match("*").count(500).build();
        int maxSamples = 5000;
        try (Cursor<byte[]> cursor = connection.keyCommands().scan(options)) {
            while (cursor.hasNext() && summary.scanned < maxSamples) {
                byte[] rawKey = cursor.next();
                if (rawKey == null || rawKey.length == 0) {
                    continue;
                }
                summary.scanned++;
                String key = new String(rawKey, StandardCharsets.UTF_8);
                String prefix = classifyRedisKey(key);
                summary.prefixCounts.merge(prefix, 1L, Long::sum);
                if (summary.examples.size() < 20) {
                    summary.examples.add(maskRedisKey(key));
                }

                Long ttl = null;
                try {
                    ttl = connection.keyCommands().ttl(rawKey);
                } catch (Exception e) {
                    log.debug("Redis TTL 查询失败 key={}: {}", maskRedisKey(key), e.getMessage());
                }
                summary.recordTtl(ttl);
            }
            summary.truncated = cursor.hasNext();
        } catch (Exception e) {
            log.warn("Redis SCAN 诊断失败: {}", e.getMessage());
            summary.error = e.getMessage();
        }
        return summary;
    }

    private long countKeysByScan(String pattern, int maxSamples) {
        if (stringRedisTemplate == null || stringRedisTemplate.getConnectionFactory() == null) {
            return 0L;
        }
        long count = 0L;
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(500).build();
        try (RedisConnection connection = stringRedisTemplate.getConnectionFactory().getConnection();
             Cursor<byte[]> cursor = connection.keyCommands().scan(options)) {
            while (cursor.hasNext() && count < maxSamples) {
                cursor.next();
                count++;
            }
        } catch (Exception e) {
            log.debug("Redis keyCount SCAN失败 pattern={}: {}", pattern, e.getMessage());
            return 0L;
        }
        return count;
    }

    private String classifyRedisKey(String key) {
        if (key == null || key.isBlank()) {
            return "unknown";
        }
        if (key.startsWith("cache:embedding:")) {
            return "cache:embedding";
        }
        if (key.startsWith("cache:kb:")) {
            return "cache:kb";
        }
        if (key.startsWith("stats:kb:")) {
            return "stats:kb";
        }
        if (key.startsWith("ai:evolve:")) {
            return "ai:evolve";
        }
        if (key.startsWith("live:")) {
            return "live";
        }
        if (key.startsWith("douyin:")) {
            return "douyin";
        }
        if (key.startsWith("shortvideo:")) {
            return "shortvideo";
        }
        int idx = key.indexOf(':');
        return idx > 0 ? key.substring(0, idx) : "other";
    }

    private String maskRedisKey(String key) {
        if (key == null || key.length() <= 80) {
            return key;
        }
        return key.substring(0, 40) + "..." + key.substring(key.length() - 16);
    }

    private List<String> buildCacheDiagnosticSuggestions(
            long keyspaceHits,
            long keyspaceMisses,
            long expiredKeys,
            long evictedKeys,
            Map<String, Object> businessStats,
            ScanSummary scanSummary
    ) {
        List<String> suggestions = new ArrayList<>();
        long globalTotal = keyspaceHits + keyspaceMisses;
        double globalHitRate = percent(keyspaceHits, globalTotal);
        double businessHitRate = numberAsDouble(businessStats.get("hitRate"));
        long businessTotal = numberAsLong(businessStats.get("total"));
        long kbKeys = scanSummary.prefixCounts.getOrDefault("cache:kb", 0L);
        long embeddingKeys = scanSummary.prefixCounts.getOrDefault("cache:embedding", 0L);

        if (businessTotal > 0 && businessHitRate < 50) {
            suggestions.add("KB 检索业务缓存命中率偏低：已将默认 TTL 提升为 " + Math.max(300, kbCacheTtlSeconds)
                    + " 秒，可继续做查询归一化、热点问题预热、按 kbId/topK 固定常用查询入口");
        }
        if (globalTotal > 0 && globalHitRate < 50 && businessTotal == 0) {
            suggestions.add("Redis 全局命中率偏低但 KB 业务计数为空：优先排查非 KB 模块的 Redis 调用和无效 key 访问");
        }
        if (globalTotal > 0 && globalHitRate < 50 && businessHitRate >= 50) {
            suggestions.add("Redis 全局命中率低于业务缓存命中率：当前告警应拆分看待，低分可能来自其他模块或历史累计 miss");
        }
        if (kbKeys == 0 && businessTotal > 20) {
            suggestions.add("存在 KB 检索 miss/hit 计数但 cache:kb:* 样本较少：检查缓存是否被频繁过期、清理或写入条件过窄");
        }
        if (embeddingKeys > kbKeys * 5 && embeddingKeys > 100) {
            suggestions.add("cache:embedding:* 占比较高属于正常现象，embedding 是稳定高价值缓存；已支持 app.ai.embedding.cache-ttl-days 配置");
        }
        if (scanSummary.ttlBuckets.get("noExpire") > 0) {
            suggestions.add("发现无过期时间的 Redis key，建议补齐 TTL 或确认它们是必须长期保留的状态键");
        }
        if (expiredKeys > 0 && scanSummary.ttlBuckets.get("lt5m") > scanSummary.scanned / 3) {
            suggestions.add("短 TTL key 占比较高且 Redis 已有过期键累计，建议把高频检索类缓存 TTL 调到 6-24 小时并观察命中率");
        }
        if (evictedKeys > 0) {
            suggestions.add("Redis 已发生内存淘汰，优先检查大 key、maxmemory-policy，并考虑提高 Redis 内存上限");
        }
        if (suggestions.isEmpty()) {
            suggestions.add("Redis 缓存结构正常；继续观察业务命中率、热点 query 和 P95/P99 检索耗时");
        }
        return suggestions;
    }

    private long propertyLong(Properties props, String key) {
        Object value = props != null ? props.get(key) : null;
        if (value == null && props != null) {
            value = props.getProperty(key);
        }
        return numberAsLong(value);
    }

    private String propertyString(Properties props, String key) {
        Object value = props != null ? props.get(key) : null;
        if (value == null && props != null) {
            value = props.getProperty(key);
        }
        return value != null ? String.valueOf(value) : "";
    }

    private double propertyDouble(Properties props, String key) {
        Object value = props != null ? props.get(key) : null;
        if (value == null && props != null) {
            value = props.getProperty(key);
        }
        return numberAsDouble(value);
    }

    private Object parseLongOrString(String raw) {
        if (raw == null) {
            return "";
        }
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException ignored) {
            return raw;
        }
    }

    private long safeNumber(Long value) {
        return value != null ? value : 0L;
    }

    private long numberAsLong(Object value) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        if (value instanceof String s && !s.isBlank()) {
            try {
                return Long.parseLong(s.trim());
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        }
        return 0L;
    }

    private double numberAsDouble(Object value) {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        if (value instanceof String s && !s.isBlank()) {
            try {
                return Double.parseDouble(s.trim());
            } catch (NumberFormatException ignored) {
                return 0.0;
            }
        }
        return 0.0;
    }

    private double percent(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0.0;
        }
        return Math.round(((double) numerator / denominator) * 10000.0) / 100.0;
    }

    private static final class ScanSummary {
        private int scanned;
        private boolean truncated;
        private String error;
        private final Map<String, Long> prefixCounts = new LinkedHashMap<>();
        private final Map<String, Long> ttlBuckets = new LinkedHashMap<>();
        private final List<String> examples = new ArrayList<>();

        private ScanSummary() {
            ttlBuckets.put("noExpire", 0L);
            ttlBuckets.put("expiredOrMissing", 0L);
            ttlBuckets.put("lt5m", 0L);
            ttlBuckets.put("lt1h", 0L);
            ttlBuckets.put("lt1d", 0L);
            ttlBuckets.put("gte1d", 0L);
        }

        private void recordTtl(Long ttl) {
            String bucket;
            if (ttl == null || ttl == -2) {
                bucket = "expiredOrMissing";
            } else if (ttl == -1) {
                bucket = "noExpire";
            } else if (ttl < 300) {
                bucket = "lt5m";
            } else if (ttl < 3600) {
                bucket = "lt1h";
            } else if (ttl < 86400) {
                bucket = "lt1d";
            } else {
                bucket = "gte1d";
            }
            ttlBuckets.merge(bucket, 1L, Long::sum);
        }

        private Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("scanned", scanned);
            map.put("truncated", truncated);
            if (error != null && !error.isBlank()) {
                map.put("error", error);
            }
            map.put("prefixCounts", prefixCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (a, b) -> a,
                            LinkedHashMap::new
                    )));
            map.put("ttlBuckets", ttlBuckets);
            map.put("examples", examples);
            return map;
        }
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
                list.add(Map.of("component", "Redis", "message", "KB 业务缓存命中率偏低 (" + hr + "%)，请查看 Redis 诊断中的全局命中率、Key 前缀和 TTL 分布，并优先做热点 query 预热与查询归一化"));
            }
        }
        return list;
    }
}
