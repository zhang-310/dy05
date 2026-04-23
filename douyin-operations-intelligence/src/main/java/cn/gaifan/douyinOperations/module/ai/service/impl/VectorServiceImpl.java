package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.util.MilvusUserHint;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.ai.service.VectorService;
import cn.gaifan.douyinOperations.module.config.service.ConfigService;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.grpc.SearchResults;
import io.milvus.param.R;
import io.milvus.param.collection.*;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.param.MetricType;
import io.milvus.param.IndexType;
import io.milvus.param.dml.DeleteParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.response.SearchResultsWrapper;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class VectorServiceImpl implements VectorService {

    private static final Logger log = LoggerFactory.getLogger(VectorServiceImpl.class);

    @Autowired(required = false)
    private MilvusServiceClient milvusClient;

    @Resource
    private LlmClient llmClient;

    @Autowired(required = false)
    private ConfigService configService;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    @Value("${app.ai.ollama-url:http://localhost:11434}")
    private String ollamaUrlDefault;

    @Value("${app.ai.embedding.batch-size:16}")
    private int embeddingBatchSize;

    @Value("${app.ai.embedding.parallelism:4}")
    private int embeddingParallelism;

    private static final String DEFAULT_EMBEDDING_MODEL = "qwen3-embedding:0.6b";
    private static final Gson GSON = new Gson();
    private static final String CACHE_PREFIX = "cache:embedding:";
    private static final int CACHE_TTL_DAYS = 7;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(input.hashCode());
        }
    }

    private String configOr(String key, String fallback) {
        if (configService == null) return fallback;
        String v = configService.getRawValueByKey(key);
        return (v != null && !v.isBlank()) ? v.trim() : fallback;
    }

    private void requireMilvus() {
        if (milvusClient == null) {
            throw new BusinessException(ErrorCode.AI_MILVUS_UNAVAILABLE, "Milvus 未启用，请在配置中设置 app.milvus.enabled=true");
        }
    }

    @Override
    public void createCollection(String collectionName, int dimension) {
        requireMilvus();
        try {
            // 检查 Collection 是否存在
            R<Boolean> hasCollection = milvusClient.hasCollection(
                    HasCollectionParam.newBuilder()
                            .withCollectionName(collectionName)
                            .build()
            );

            if (hasCollection.getData()) {
                log.info("Collection {} 已存在", collectionName);
                return;
            }

            // 创建 Collection
            FieldType idField = FieldType.newBuilder()
                    .withName("id")
                    .withDataType(DataType.Int64)
                    .withPrimaryKey(true)
                    .withAutoID(false)
                    .build();

            FieldType vectorField = FieldType.newBuilder()
                    .withName("embedding")
                    .withDataType(DataType.FloatVector)
                    .withDimension(dimension)
                    .build();

            FieldType textField = FieldType.newBuilder()
                    .withName("text")
                    .withDataType(DataType.VarChar)
                    .withMaxLength(65535)
                    .build();

            FieldType metadataField = FieldType.newBuilder()
                    .withName("metadata")
                    .withDataType(DataType.JSON)
                    .build();

            CreateCollectionParam createParam = CreateCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withDescription("Knowledge base collection")
                    .addFieldType(idField)
                    .addFieldType(vectorField)
                    .addFieldType(textField)
                    .addFieldType(metadataField)
                    .build();

            R<?> response = milvusClient.createCollection(createParam);
            if (response.getStatus() != R.Status.Success.getCode()) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建 Collection 失败: " + response.getMessage());
            }

            // 创建索引
            CreateIndexParam indexParam = CreateIndexParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withFieldName("embedding")
                    .withIndexType(IndexType.IVF_FLAT)
                    .withMetricType(MetricType.COSINE)
                    .withExtraParam("{\"nlist\":1024}")
                    .build();

            R<?> indexResponse = milvusClient.createIndex(indexParam);
            if (indexResponse.getStatus() != R.Status.Success.getCode()) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建索引失败: " + indexResponse.getMessage());
            }

            // 加载 Collection
            milvusClient.loadCollection(
                    LoadCollectionParam.newBuilder()
                            .withCollectionName(collectionName)
                            .build()
            );

            log.info("Collection {} 创建成功", collectionName);
        } catch (Exception e) {
            log.error("创建 Collection 失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建 Collection 失败: " + e.getMessage());
        }
    }

    @Override
    public void dropCollection(String collectionName) {
        requireMilvus();
        try {
            R<?> response = milvusClient.dropCollection(
                    DropCollectionParam.newBuilder()
                            .withCollectionName(collectionName)
                            .build()
            );

            if (response.getStatus() != R.Status.Success.getCode()) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "删除 Collection 失败: " + response.getMessage());
            }

            log.info("Collection {} 删除成功", collectionName);
        } catch (Exception e) {
            log.error("删除 Collection 失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "删除 Collection 失败: " + e.getMessage());
        }
    }

    private static final int WRITE_MAX_RETRIES = 3;
    private static final int[] WRITE_BACKOFF_MS = {1000, 2000, 4000};

    @Override
    public void insertVectors(String collectionName, List<Long> ids, List<List<Float>> vectors, List<Map<String, Object>> metadata) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        requireMilvus();
        Exception lastEx = null;
        for (int attempt = 0; attempt < WRITE_MAX_RETRIES; attempt++) {
            try {
                doInsertVectors(collectionName, ids, vectors, metadata);
                return;
            } catch (Exception e) {
                lastEx = e;
                if (attempt < WRITE_MAX_RETRIES - 1) {
                    int backoff = WRITE_BACKOFF_MS[Math.min(attempt, WRITE_BACKOFF_MS.length - 1)];
                    log.warn("Milvus 插入失败(attempt={}), {}ms 后重试: {}", attempt + 1, backoff, e.getMessage());
                    try { Thread.sleep(backoff); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                }
            }
        }
        log.error("Milvus 插入失败: {}", lastEx != null ? lastEx.getMessage() : "unknown");
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "插入向量失败: " + (lastEx != null ? lastEx.getMessage() : "unknown"));
    }

    private void doInsertVectors(String collectionName, List<Long> ids, List<List<Float>> vectors, List<Map<String, Object>> metadata) {
        List<String> texts = metadata.stream()
                .map(m -> (String) m.getOrDefault("text", ""))
                .collect(Collectors.toList());

        List<JsonObject> jsonMetadata = metadata.stream()
                .map(m -> GSON.fromJson(GSON.toJson(m), JsonObject.class))
                .collect(Collectors.toList());

        List<InsertParam.Field> fields = new ArrayList<>();
        fields.add(new InsertParam.Field("id", ids));
        fields.add(new InsertParam.Field("embedding", vectors));
        fields.add(new InsertParam.Field("text", texts));
        fields.add(new InsertParam.Field("metadata", jsonMetadata));

        InsertParam insertParam = InsertParam.newBuilder()
                .withCollectionName(collectionName)
                .withFields(fields)
                .build();

        R<io.milvus.grpc.MutationResult> response = milvusClient.insert(insertParam);
        if (response.getStatus() != R.Status.Success.getCode()) {
            throw new RuntimeException("Milvus: " + response.getMessage());
        }
        log.info("插入 {} 条向量到 {}", ids.size(), collectionName);
    }

    @Override
    public List<VectorSearchResult> search(String collectionName, List<Float> queryVector, int topK, String filter) {
        requireMilvus();
        try {
            SearchParam.Builder searchBuilder = SearchParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withMetricType(MetricType.COSINE)
                    .withTopK(topK)
                    .withVectors(Collections.singletonList(queryVector))
                    .withVectorFieldName("embedding")
                    .withOutFields(Arrays.asList("id", "text", "metadata"));

            if (filter != null && !filter.isEmpty()) {
                searchBuilder.withExpr(filter);
            }

            R<SearchResults> response = milvusClient.search(searchBuilder.build());
            if (response.getStatus() != R.Status.Success.getCode()) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "向量搜索失败: " + response.getMessage());
            }

            SearchResultsWrapper wrapper = new SearchResultsWrapper(response.getData().getResults());
            List<VectorSearchResult> results = new ArrayList<>();

            for (int i = 0; i < wrapper.getIDScore(0).size(); i++) {
                long id = (long) wrapper.getIDScore(0).get(i).getLongID();
                float score = wrapper.getIDScore(0).get(i).getScore();

                Map<String, Object> metadata = new HashMap<>();
                Object metadataObj = wrapper.getFieldData("metadata", 0).get(i);
                if (metadataObj instanceof JSONObject jo) {
                    metadata.putAll(new HashMap<>(jo));
                } else if (metadataObj instanceof JsonObject go) {
                    metadata.putAll(GSON.fromJson(go, Map.class));
                }

                results.add(new VectorSearchResult(id, score, metadata));
            }

            return results;
        } catch (Exception e) {
            log.error("向量搜索失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    MilvusUserHint.appendRecoveryHint("向量搜索失败: " + e.getMessage()));
        }
    }

    @Override
    public List<List<VectorSearchResult>> batchSearch(String collectionName, List<List<Float>> queryVectors, int topKPerQuery, String filter) {
        if (queryVectors == null || queryVectors.isEmpty()) return Collections.emptyList();
        requireMilvus();
        try {
            SearchParam.Builder searchBuilder = SearchParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withMetricType(MetricType.COSINE)
                    .withTopK(topKPerQuery)
                    .withVectors(queryVectors)
                    .withVectorFieldName("embedding")
                    .withOutFields(Arrays.asList("id", "text", "metadata"));
            if (filter != null && !filter.isEmpty()) searchBuilder.withExpr(filter);

            R<SearchResults> response = milvusClient.search(searchBuilder.build());
            if (response.getStatus() != R.Status.Success.getCode()) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                        MilvusUserHint.appendRecoveryHint("向量批量搜索失败: " + response.getMessage()));
            }
            SearchResultsWrapper wrapper = new SearchResultsWrapper(response.getData().getResults());
            List<List<VectorSearchResult>> allResults = new ArrayList<>();
            for (int q = 0; q < queryVectors.size(); q++) {
                List<VectorSearchResult> oneQuery = new ArrayList<>();
                int n = wrapper.getIDScore(q).size();
                for (int i = 0; i < n; i++) {
                    long id = (long) wrapper.getIDScore(q).get(i).getLongID();
                    float score = wrapper.getIDScore(q).get(i).getScore();
                    Map<String, Object> metadata = new HashMap<>();
                    Object metadataObj = wrapper.getFieldData("metadata", q).get(i);
                    if (metadataObj instanceof JSONObject jo) metadata.putAll(new HashMap<>(jo));
                    else if (metadataObj instanceof JsonObject go) metadata.putAll(GSON.fromJson(go, Map.class));
                    oneQuery.add(new VectorSearchResult(id, score, metadata));
                }
                allResults.add(oneQuery);
            }
            return allResults;
        } catch (Exception e) {
            log.error("向量批量搜索失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    MilvusUserHint.appendRecoveryHint("向量批量搜索失败: " + e.getMessage()));
        }
    }

    @Override
    public void deleteVectors(String collectionName, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        requireMilvus();
        try {
            String expr = "id in [" + ids.stream().map(String::valueOf).collect(Collectors.joining(",")) + "]";

            DeleteParam deleteParam = DeleteParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withExpr(expr)
                    .build();

            R<io.milvus.grpc.MutationResult> response = milvusClient.delete(deleteParam);
            if (response.getStatus() != R.Status.Success.getCode()) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "删除向量失败: " + response.getMessage());
            }

            log.info("删除 {} 条向量从 {}", ids.size(), collectionName);
        } catch (Exception e) {
            log.error("删除向量失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "删除向量失败: " + e.getMessage());
        }
    }

    private static final int EMBEDDING_MAX_RETRIES = 3;
    private static final int[] EMBEDDING_BACKOFF_SEC = {60, 90, 120};

    @Override
    public List<Float> generateEmbedding(String text) {
        String model = configOr("ai.embedding.model", DEFAULT_EMBEDDING_MODEL);
        String cacheKey = CACHE_PREFIX + sha256(model + ":" + text);

        if (stringRedisTemplate != null) {
            try {
                String cached = stringRedisTemplate.opsForValue().get(cacheKey);
                if (cached != null && !cached.isBlank()) {
                    List<Float> vec = JSON.parseObject(cached, new TypeReference<List<Float>>() {});
                    if (vec != null && !vec.isEmpty()) return vec;
                }
            } catch (Exception ignored) { /* 缓存解析失败则继续请求 */ }
        }

        Exception lastEx = null;
        for (int attempt = 0; attempt < EMBEDDING_MAX_RETRIES; attempt++) {
            try {
                List<Float> result = doGenerateEmbedding(model, text);
                if (stringRedisTemplate != null && result != null && !result.isEmpty()) {
                    stringRedisTemplate.opsForValue().set(cacheKey, JSON.toJSONString(result), CACHE_TTL_DAYS, TimeUnit.DAYS);
                }
                return result;
            } catch (Exception e) {
                lastEx = e;
                boolean retryable = isRetryableEmbeddingError(e);
                if (!retryable || attempt >= EMBEDDING_MAX_RETRIES - 1) break;
                int backoff = EMBEDDING_BACKOFF_SEC[Math.min(attempt, EMBEDDING_BACKOFF_SEC.length - 1)];
                log.warn("Embedding 请求失败(attempt={}), {}s 后重试: {}", attempt + 1, backoff, e.getMessage());
                try { Thread.sleep(backoff * 1000L); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
            }
        }
        String detail = formatEmbeddingFailureDetail(lastEx);
        log.error("生成嵌入向量失败: {}", detail);
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "生成嵌入向量失败: " + detail);
    }

    /** Ollama/HTTP 异常常无 message，避免界面只显示「null」 */
    private static String formatEmbeddingFailureDetail(Throwable t) {
        if (t == null) {
            return "unknown（无异常对象，请检查 Ollama 是否启动、ai.ollama.url 与 ai.embedding.model）";
        }
        StringBuilder sb = new StringBuilder();
        Throwable cur = t;
        int depth = 0;
        while (cur != null && depth < 5) {
            String cls = cur.getClass().getSimpleName();
            String msg = cur.getMessage();
            if (depth > 0) sb.append(" | caused by: ");
            sb.append(cls);
            if (msg != null && !msg.isBlank()) {
                sb.append(": ").append(msg);
            }
            cur = cur.getCause();
            depth++;
        }
        return sb.toString();
    }

    private boolean isRetryableEmbeddingError(Exception e) {
        String msg = e.getMessage();
        if (msg == null) return false;
        return msg.contains("502") || msg.contains("503") || msg.contains("504")
                || msg.contains("timeout") || msg.contains("Timeout")
                || msg.contains("Connection") || msg.contains("connection reset");
    }

    private List<Float> doGenerateEmbedding(String model, String text) throws Exception {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", model);
        requestBody.put("prompt", text);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(configOr("ai.ollama.url", ollamaUrlDefault) + "/api/embeddings"))
                .header("Content-Type", "application/json")
                .timeout(java.time.Duration.ofSeconds(120))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toJSONString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
        }

        JSONObject responseJson = JSON.parseObject(response.body());
        JSONArray embedding = responseJson.getJSONArray("embedding");
        if (embedding == null) {
            String bodyPreview = response.body() != null && response.body().length() > 400
                    ? response.body().substring(0, 400) + "..."
                    : String.valueOf(response.body());
            throw new RuntimeException("Ollama 响应中 embedding 缺失；请确认模型支持 /api/embeddings，模型="
                    + model + "；响应片段: " + bodyPreview);
        }

        return embedding.stream()
                .map(obj -> ((Number) obj).floatValue())
                .collect(Collectors.toList());
    }

    @Override
    public List<List<Float>> generateEmbeddings(List<String> texts) {
        if (texts == null || texts.isEmpty()) return Collections.emptyList();
        int parallelism = Math.max(1, Math.min(embeddingParallelism, 16));
        java.util.concurrent.Semaphore semaphore = new java.util.concurrent.Semaphore(parallelism);
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(parallelism);
        try {
            List<java.util.concurrent.Future<List<Float>>> futures = new ArrayList<>();
            for (String text : texts) {
                futures.add(executor.submit(() -> {
                    semaphore.acquire();
                    try {
                        return generateEmbedding(text);
                    } finally {
                        semaphore.release();
                    }
                }));
            }
            List<List<Float>> result = new ArrayList<>(texts.size());
            for (java.util.concurrent.Future<List<Float>> f : futures) {
                try {
                    result.add(f.get(180, java.util.concurrent.TimeUnit.SECONDS));
                } catch (Exception e) {
                    executor.shutdownNow();
                    Throwable root = e;
                    if (e instanceof ExecutionException && e.getCause() != null) {
                        root = e.getCause();
                    }
                    String detail = formatEmbeddingFailureDetail(root);
                    throw new BusinessException(ErrorCode.INTERNAL_ERROR, "批量嵌入失败: " + detail);
                }
            }
            return result;
        } finally {
            executor.shutdown();
        }
    }
}
