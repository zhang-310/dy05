package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.service.ImportRequirementsService;
import io.milvus.client.MilvusServiceClient;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 文档导入前置依赖检查：Milvus、Ollama、Elasticsearch
 * 不依赖 Redis/ConfigService，确保环境检测始终可用
 */
@Service
public class ImportRequirementsServiceImpl implements ImportRequirementsService {

    @Autowired(required = false)
    private MilvusServiceClient milvusClient;

    @Resource
    private co.elastic.clients.elasticsearch.ElasticsearchClient esClient;

    @Value("${app.ai.ollama-url:http://localhost:11434}")
    private String ollamaUrlDefault;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    @Override
    public Map<String, String> checkImportRequirements() {
        Map<String, String> result = new LinkedHashMap<>();

        // Milvus
        if (milvusClient == null) {
            result.put("milvus", "未启用。执行：docker compose -f docker/docker-compose.yml --profile ai-builtin up -d");
        } else {
            try {
                milvusClient.hasCollection(io.milvus.param.collection.HasCollectionParam.newBuilder().withCollectionName("__health_check__").build());
                result.put("milvus", null);
            } catch (Exception e) {
                result.put("milvus", "连接失败，请确保容器已启动");
            }
        }

        // Ollama（仅用 @Value 默认值，不依赖 Redis/ConfigService）
        try {
            String url = ollamaUrlDefault.replaceAll("/$", "") + "/api/tags";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                result.put("ollama", "响应异常，请启动 Ollama 并拉取 qwen3-embedding:0.6b");
            } else {
                result.put("ollama", null);
            }
        } catch (Exception e) {
            result.put("ollama", "无法连接，请启动 Ollama（默认 localhost:11434）");
        }

        // Elasticsearch
        try {
            esClient.cluster().health(co.elastic.clients.elasticsearch.cluster.HealthRequest.of(h -> h));
            result.put("elasticsearch", null);
        } catch (Exception e) {
            result.put("elasticsearch", "连接失败，请执行 docker compose up -d 启动 ES");
        }

        return result;
    }
}
