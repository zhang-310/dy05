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
 * Pika 2.2 视频生成 (通过 fal.ai 中转)
 *
 * 核心优势:
 *   - 风格化最强: 动漫、卡通、油画、3D 等多种风格
 *   - 负向 prompt 支持
 *   - 适合非写实类内容
 *
 * API (fal.ai): POST https://queue.fal.run/fal-ai/pika/v2.2
 * 注意: 最长仅 4 秒
 */
@Component
public class PikaVideoProvider implements AiVideoProvider {

    private static final Logger log = LoggerFactory.getLogger(PikaVideoProvider.class);
    private static final String DEFAULT_API_URL = "https://queue.fal.run/fal-ai/pika/v2.2";
    private static final int POLL_INTERVAL_MS = 5000;
    private static final int POLL_MAX_ATTEMPTS = 60;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30)).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.ai.pika.api-key:}")
    private String apiKeyFromConfig;
    @Value("${app.ai.pika.api-url:}")
    private String apiUrlFromConfig;

    @Resource
    private ConfigService configService;

    @Override
    public String name() { return "pika"; }

    @Override
    public boolean isConfigured() { return StringUtils.hasText(getApiKey()); }

    @Override
    public boolean supportsNegativePrompt() { return true; }

    @Override
    public String[] contentStrengths() {
        return new String[]{"anime", "cartoon", "stylized", "3d_render"};
    }

    @Override
    public VideoGenerationResult generateVideo(VideoGenerationRequest request) {
        try {
            String url = getApiUrl();

            ObjectNode body = objectMapper.createObjectNode();
            body.put("image_url", request.imageUrl());
            body.put("prompt", truncate(request.prompt(), 500));
            if (request.negativePrompt() != null) {
                body.put("negative_prompt", request.negativePrompt());
            }
            body.put("duration", Math.min(request.duration(), 4));
            body.put("aspect_ratio", request.aspectRatio() != null ? request.aspectRatio() : "9:16");

            HttpRequest httpReq = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Key " + getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(30)).build();

            HttpResponse<String> resp = httpClient.send(httpReq, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(resp.body());

            // fal.ai 可能同步返回 video.url 或异步返回 request_id
            String videoUrl = root.path("video").path("url").asText(null);
            if (videoUrl == null) videoUrl = root.path("video_url").asText(null);
            if (videoUrl == null) videoUrl = root.path("video").path("url").asText(null);

            String requestId = root.path("request_id").asText(null);
            if (requestId == null) requestId = root.path("id").asText(null);

            if (StringUtils.hasText(videoUrl)) {
                return new VideoGenerationResult(videoUrl, "pika", Math.min(request.duration(), 4) * 1000, true, false);
            }
            if (StringUtils.hasText(requestId)) {
                String polledUrl = pollPikaTask(url, requestId);
                return new VideoGenerationResult(polledUrl, "pika", Math.min(request.duration(), 4) * 1000, true, false);
            }

            throw new VideoGenerationException(name(), "Pika 未返回 video_url 或 request_id: " + resp.body());
        } catch (VideoGenerationException e) {
            throw e;
        } catch (Exception e) {
            throw new VideoGenerationException(name(), "Pika 2.2 生成失败: " + e.getMessage(), e);
        }
    }

    private String pollPikaTask(String baseUrl, String requestId) throws Exception {
        String statusUrl = baseUrl.replace("/v2.2", "") + "/requests/" + requestId + "/status";
        if (!statusUrl.contains("/requests/")) {
            statusUrl = baseUrl + "/requests/" + requestId + "/status";
        }
        for (int i = 0; i < POLL_MAX_ATTEMPTS; i++) {
            Thread.sleep(POLL_INTERVAL_MS);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(statusUrl))
                    .header("Authorization", "Key " + getApiKey())
                    .GET().timeout(Duration.ofSeconds(30)).build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(resp.body());
            String status = root.path("status").asText("");
            if ("COMPLETED".equalsIgnoreCase(status) || "completed".equals(status)) {
                String url = root.path("video").path("url").asText(null);
                if (url == null) url = root.path("video_url").asText(null);
                if (url != null && !url.isBlank()) return url;
                throw new VideoGenerationException(name(), "Pika 成功但无 video_url: " + resp.body());
            }
            if ("FAILED".equalsIgnoreCase(status) || "failed".equals(status)) {
                throw new VideoGenerationException(name(), "Pika 任务失败: " + resp.body());
            }
        }
        throw new VideoGenerationException(name(), "Pika 超时");
    }

    private String getApiKey() {
        String key = configService != null ? configService.getRawValueByKey("ai.pika.api-key") : null;
        if (!StringUtils.hasText(key)) key = apiKeyFromConfig;
        if (!StringUtils.hasText(key)) key = System.getenv("FAL_KEY");
        return StringUtils.hasText(key) ? key.trim() : null;
    }

    private String getApiUrl() {
        String url = configService != null ? configService.getRawValueByKey("ai.pika.api-url") : null;
        if (!StringUtils.hasText(url)) url = apiUrlFromConfig;
        return StringUtils.hasText(url) ? url.trim().replaceAll("/$", "") : DEFAULT_API_URL;
    }

    private static String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) : s;
    }
}
