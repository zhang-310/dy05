package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.exception.VideoGenerationException;
import cn.gaifan.douyinOperations.module.ai.service.AiVideoProvider;
import cn.gaifan.douyinOperations.module.config.service.ConfigService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Wan 2.6 (阿里) 视频生成
 *
 * 核心优势:
 *   - 开源模型，可自部署
 *   - 极低成本: $0.05/秒 (通过阿里云 DashScope)
 *   - 适合低成本批量生成
 *
 * API (DashScope): POST .../video-generation/generation
 */
@Component
public class WanVideoProvider implements AiVideoProvider {

    private static final Logger log = LoggerFactory.getLogger(WanVideoProvider.class);
    private static final String DEFAULT_API_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/video-generation/generation";
    private static final String DEFAULT_STATUS_BASE = "https://dashscope.aliyuncs.com/api/v1/tasks";
    private static final int POLL_INTERVAL_MS = 5000;
    private static final int POLL_MAX_ATTEMPTS = 120;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30)).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.ai.wan.api-key:}")
    private String apiKeyFromConfig;
    @Value("${app.ai.wan.api-url:}")
    private String apiUrlFromConfig;

    @Resource
    private ConfigService configService;

    @Override
    public String name() { return "wan"; }

    @Override
    public boolean isConfigured() { return StringUtils.hasText(getApiKey()); }

    @Override
    public String[] contentStrengths() {
        return new String[]{"anime", "batch", "low_cost"};
    }

    @Override
    public VideoGenerationResult generateVideo(VideoGenerationRequest request) {
        try {
            String url = getApiUrl();

            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", "wanx-v2.6");
            var input = objectMapper.createObjectNode();
            input.put("prompt", truncate(request.prompt(), 500));
            if (request.imageUrl() != null) input.put("img_url", request.imageUrl());
            body.set("input", input);

            var params = objectMapper.createObjectNode();
            params.put("duration", Math.min(request.duration(), 10));
            body.set("parameters", params);

            HttpRequest httpReq = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + getApiKey())
                    .header("Content-Type", "application/json")
                    .header("X-DashScope-Async", "enable")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(30)).build();

            HttpResponse<String> resp = httpClient.send(httpReq, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(resp.body());
            String taskId = root.path("output").path("task_id").asText(null);
            if (taskId == null) {
                throw new VideoGenerationException(name(), "Wan 未返回 task_id: " + resp.body());
            }

            String videoUrl = pollWanTask(taskId);
            return new VideoGenerationResult(videoUrl, "wan", request.duration() * 1000, true, false);
        } catch (VideoGenerationException e) {
            throw e;
        } catch (Exception e) {
            throw new VideoGenerationException(name(), "Wan 2.6 生成失败: " + e.getMessage(), e);
        }
    }

    private String pollWanTask(String taskId) throws Exception {
        String statusBase = getStatusBaseUrl();
        for (int i = 0; i < POLL_MAX_ATTEMPTS; i++) {
            Thread.sleep(POLL_INTERVAL_MS);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(statusBase + "/" + taskId))
                    .header("Authorization", "Bearer " + getApiKey())
                    .GET().timeout(Duration.ofSeconds(30)).build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(resp.body());
            String status = root.path("output").path("task_status").asText("");
            if ("SUCCEEDED".equalsIgnoreCase(status)) {
                String url = root.path("output").path("video_url").asText(null);
                if (url != null && !url.isBlank()) return url;
                throw new VideoGenerationException(name(), "Wan 成功但无 video_url: " + resp.body());
            }
            if ("FAILED".equalsIgnoreCase(status)) {
                throw new VideoGenerationException(name(), "Wan 任务失败: " + resp.body());
            }
        }
        throw new VideoGenerationException(name(), "Wan 超时");
    }

    private String getApiKey() {
        String key = configService != null ? configService.getRawValueByKey("ai.wan.api-key") : null;
        if (!StringUtils.hasText(key)) key = apiKeyFromConfig;
        if (!StringUtils.hasText(key)) key = System.getenv("WAN_API_KEY");
        return StringUtils.hasText(key) ? key.trim() : null;
    }

    private String getApiUrl() {
        String url = configService != null ? configService.getRawValueByKey("ai.wan.api-url") : null;
        if (!StringUtils.hasText(url)) url = apiUrlFromConfig;
        return StringUtils.hasText(url) ? url.trim().replaceAll("/$", "") : DEFAULT_API_URL;
    }

    private String getStatusBaseUrl() {
        String url = configService != null ? configService.getRawValueByKey("ai.wan.status-url") : null;
        if (StringUtils.hasText(url)) return url.trim().replaceAll("/$", "");
        return DEFAULT_STATUS_BASE;
    }

    private static String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) : s;
    }
}
