package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.service.RerankerService;
import cn.gaifan.douyinOperations.module.config.service.ConfigService;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * 重排序服务实现：支持 Ollama、Cohere、自定义 HTTP（BGE-reranker 等）
 * provider: ollama | cohere | custom
 * 未配置时 isAvailable=false，检索流程自动跳过重排
 */
@Service
public class RerankerServiceImpl implements RerankerService {

    private static final Logger log = LoggerFactory.getLogger(RerankerServiceImpl.class);

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ConfigService configService;

    @Value("${app.ai.ollama-url:http://localhost:11434}")
    private String ollamaUrlDefault;

    @Value("${app.ai.reranker.enabled:false}")
    private boolean rerankerEnabled;

    @Value("${app.ai.reranker.provider:ollama}")
    private String rerankerProvider;

    @Value("${app.ai.reranker.model:qwen3-embedding:4b}")
    private String rerankerModelDefault;

    @Value("${app.ai.reranker.cohere.model:rerank-v3.5}")
    private String cohereModelDefault;

    @Value("${app.ai.reranker.custom.url:}")
    private String customRerankerUrlDefault;

    private final HttpClient httpClient = HttpClient.newBuilder().build();

    private String configOr(String key, String fallback) {
        if (configService == null) return fallback;
        String v = configService.getRawValueByKey(key);
        return (v != null && !v.isBlank()) ? v.trim() : fallback;
    }

    @Override
    public boolean isAvailable() {
        return rerankerEnabled;
    }

    @Override
    public List<Float> rerank(String query, List<String> candidates) {
        if (!isAvailable() || query == null || candidates == null || candidates.isEmpty()) {
            return null;
        }
        try {
            String provider = configOr("ai.reranker.provider", rerankerProvider);
            List<Float> scores = null;
            if ("cohere".equalsIgnoreCase(provider)) {
                scores = tryCohereRerank(query, candidates);
            } else if ("custom".equalsIgnoreCase(provider)) {
                scores = tryCustomRerank(query, candidates);
            }
            if (scores != null) return scores;

            String baseUrl = configOr("ai.reranker.url", configOr("ai.ollama.url", ollamaUrlDefault));
            String model = configOr("ai.reranker.model", "bge-reranker-v2-m3");
            scores = tryOllamaRerankApi(baseUrl, model, query, candidates);
            if (scores != null) return scores;

            model = configOr("ai.embedding.model", configOr("ai.reranker.model", rerankerModelDefault));
            List<Float> fallback = new ArrayList<>(candidates.size());
            for (String cand : candidates) {
                fallback.add(scorePair(baseUrl, model, query, cand));
            }
            return fallback;
        } catch (Exception e) {
            log.warn("重排序失败，跳过: {}", e.getMessage());
            return null;
        }
    }

    /** Cohere Rerank API v2: https://api.cohere.com/v2/rerank */
    private List<Float> tryCohereRerank(String query, List<String> candidates) {
        String apiKey = configService != null ? configService.getRawValueByKey("ai.reranker.cohere.api-key") : null;
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = System.getenv("COHERE_API_KEY");
        }
        if (apiKey == null || apiKey.isBlank()) {
            log.debug("Cohere API Key 未配置，跳过");
            return null;
        }
        try {
            String model = configOr("ai.reranker.cohere.model", cohereModelDefault);
            java.util.Map<String, Object> body = new java.util.HashMap<>();
            body.put("model", model);
            body.put("query", query);
            body.put("documents", candidates);
            body.put("top_n", Math.min(candidates.size(), 100));

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.cohere.com/v2/rerank"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(JSON.toJSONString(body)))
                    .timeout(java.time.Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                log.warn("Cohere Rerank 失败: {} {}", resp.statusCode(), resp.body());
                return null;
            }
            JSONObject json = JSON.parseObject(resp.body());
            JSONArray results = json.getJSONArray("results");
            if (results == null || results.isEmpty()) return null;
            float[] scoreByIndex = new float[candidates.size()];
            for (int i = 0; i < results.size(); i++) {
                JSONObject r = results.getJSONObject(i);
                int idx = r.getIntValue("index");
                double s = r.getDoubleValue("relevance_score");
                if (idx >= 0 && idx < candidates.size()) scoreByIndex[idx] = (float) s;
            }
            List<Float> out = new ArrayList<>(candidates.size());
            for (float s : scoreByIndex) out.add(s > 0 ? s : 0.5f);
            return out;
        } catch (Exception e) {
            log.debug("Cohere Rerank 不可用: {}", e.getMessage());
            return null;
        }
    }

    /** 自定义 HTTP Reranker（BGE-reranker 等）：POST {query, documents} 返回 {scores: [0.9,0.7,...]} */
    private List<Float> tryCustomRerank(String query, List<String> candidates) {
        String url = configOr("ai.reranker.custom.url", customRerankerUrlDefault != null ? customRerankerUrlDefault : "");
        if (url == null || url.isBlank()) return null;
        try {
            java.util.Map<String, Object> body = new java.util.HashMap<>();
            body.put("query", query);
            body.put("documents", candidates);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(JSON.toJSONString(body)))
                    .timeout(java.time.Duration.ofSeconds(60))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return null;
            JSONObject json = JSON.parseObject(resp.body());
            JSONArray scores = json.getJSONArray("scores");
            if (scores == null || scores.size() != candidates.size()) return null;
            List<Float> out = new ArrayList<>(candidates.size());
            for (int i = 0; i < scores.size(); i++) out.add(scores.getFloat(i));
            return out;
        } catch (Exception e) {
            log.debug("Custom Reranker 不可用: {}", e.getMessage());
            return null;
        }
    }

    /** P2 真正的 Cross-Encoder：Ollama /api/rerank，返回 results 按 document 匹配回原序 */
    private List<Float> tryOllamaRerankApi(String baseUrl, String model, String query, List<String> candidates) {
        try {
            java.util.Map<String, Object> body = new java.util.HashMap<>();
            body.put("model", model);
            body.put("query", query);
            body.put("documents", candidates);
            body.put("top_n", candidates.size());

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/rerank"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(JSON.toJSONString(body)))
                    .timeout(java.time.Duration.ofSeconds(60))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return null;

            JSONObject json = JSON.parseObject(resp.body());
            JSONArray results = json.getJSONArray("results");
            if (results == null || results.isEmpty()) return null;

            java.util.Map<String, Float> docToScore = new java.util.HashMap<>();
            for (int i = 0; i < results.size(); i++) {
                JSONObject r = results.getJSONObject(i);
                String doc = r.getString("document");
                double s = r.getDoubleValue("relevance_score");
                if (doc != null) docToScore.put(doc, (float) s);
            }
            List<Float> out = new ArrayList<>(candidates.size());
            for (String cand : candidates) {
                out.add(docToScore.getOrDefault(cand, 0.5f));
            }
            return out;
        } catch (Exception e) {
            log.debug("Ollama /api/rerank 不可用，降级为 embedding 相似度: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 对单对 (query, doc) 打分
     * 使用 embeddings 接口：embed(query) 与 embed(doc) 的余弦相似度作为相关性
     * 注：真正的 Cross-Encoder 应使用 [query, doc] 联合编码，此处为兼容 Ollama 的简化实现
     */
    private float scorePair(String baseUrl, String model, String query, String doc) {
        try {
            // 简化：用 query 和 doc 的 embedding 余弦相似度近似
            JSONObject req1 = new JSONObject();
            req1.put("model", model);
            req1.put("prompt", query);
            HttpRequest r1 = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/embeddings"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(req1.toJSONString()))
                    .build();
            HttpResponse<String> resp1 = httpClient.send(r1, HttpResponse.BodyHandlers.ofString());
            if (resp1.statusCode() != 200) return 0.5f;

            JSONObject req2 = new JSONObject();
            req2.put("model", model);
            req2.put("prompt", doc.length() > 512 ? doc.substring(0, 512) : doc);
            HttpRequest r2 = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/embeddings"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(req2.toJSONString()))
                    .build();
            HttpResponse<String> resp2 = httpClient.send(r2, HttpResponse.BodyHandlers.ofString());
            if (resp2.statusCode() != 200) return 0.5f;

            JSONArray emb1 = JSON.parseObject(resp1.body()).getJSONArray("embedding");
            JSONArray emb2 = JSON.parseObject(resp2.body()).getJSONArray("embedding");
            return cosineSimilarity(emb1, emb2);
        } catch (Exception e) {
            log.debug("重排序单对失败: {}", e.getMessage());
            return 0.5f;
        }
    }

    private static float cosineSimilarity(JSONArray a, JSONArray b) {
        if (a == null || b == null || a.size() != b.size()) return 0f;
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.size(); i++) {
            double va = a.getDoubleValue(i);
            double vb = b.getDoubleValue(i);
            dot += va * vb;
            normA += va * va;
            normB += vb * vb;
        }
        if (normA <= 0 || normB <= 0) return 0f;
        return (float) (dot / (Math.sqrt(normA) * Math.sqrt(normB)));
    }
}
