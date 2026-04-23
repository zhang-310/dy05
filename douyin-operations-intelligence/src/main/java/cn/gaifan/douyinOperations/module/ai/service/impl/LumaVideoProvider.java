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
 * Luma Ray2/Ray3 视频生成 (通过海外中转 API)
 *
 * 特点:
 *   - 支持首尾帧 (key_frames)
 *   - 支持 9:16 竖屏
 *   - 电影感强，适合短剧
 *
 * API:
 *   POST /v1/generations
 *   Body: { prompt, aspect_ratio, key_frames: { frame0: {type,url}, frame1: {type,url} } }
 */
@Component
public class LumaVideoProvider implements AiVideoProvider {

    private static final Logger log = LoggerFactory.getLogger(LumaVideoProvider.class);
    private static final String DEFAULT_API_URL = "https://api.lumalabs.ai/dream-machine/v1";
    private static final int POLL_INTERVAL_MS = 5000;
    private static final int POLL_MAX_ATTEMPTS = 120;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30)).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.ai.luma.api-key:}")
    private String apiKeyFromConfig;
    @Value("${app.ai.luma.api-url:}")
    private String apiUrlFromConfig;

    @Resource
    private ConfigService configService;

    @Override
    public String name() { return "luma"; }

    @Override
    public boolean isConfigured() {
        return StringUtils.hasText(getApiKey());
    }

    @Override
    public boolean supportsEndFrame() { return true; }

    @Override
    public boolean supportsAspectRatio(String ratio) {
        return ratio == null || "9:16".equals(ratio) || "16:9".equals(ratio);
    }

    @Override
    public String[] contentStrengths() {
        return new String[]{"cinematic", "keyframe_control", "film_look"};
    }

    @Override
    public VideoGenerationResult generateVideo(VideoGenerationRequest request) {
        try {
            String apiKey = getApiKey();
            String apiUrl = getApiUrl();

            ObjectNode body = objectMapper.createObjectNode();
            body.put("prompt", truncate(request.prompt(), 300));
            body.put("aspect_ratio", request.aspectRatio() != null ? request.aspectRatio() : "9:16");

            ObjectNode keyFrames = objectMapper.createObjectNode();
            ObjectNode frame0 = objectMapper.createObjectNode();
            frame0.put("type", "image");
            frame0.put("url", request.imageUrl());
            keyFrames.set("frame0", frame0);

            if (StringUtils.hasText(request.endFrameUrl())) {
                ObjectNode frame1 = objectMapper.createObjectNode();
                frame1.put("type", "image");
                frame1.put("url", request.endFrameUrl());
                keyFrames.set("frame1", frame1);
            }
            body.set("key_frames", keyFrames);

            HttpRequest httpReq = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + "/generations"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(30)).build();

            HttpResponse<String> resp = httpClient.send(httpReq, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(resp.body());
            String taskId = root.path("id").asText(null);
            if (taskId == null) {
                throw new VideoGenerationException(name(), "Luma 未返回任务 ID: " + resp.body());
            }

            String videoUrl = pollLumaTask(apiKey, apiUrl, taskId);
            return new VideoGenerationResult(videoUrl, "luma", 5000, true);

        } catch (VideoGenerationException e) {
            throw e;
        } catch (Exception e) {
            throw new VideoGenerationException(name(), "Luma 视频生成失败: " + e.getMessage(), e);
        }
    }

    private String pollLumaTask(String apiKey, String apiUrl, String taskId) throws Exception {
        for (int i = 0; i < POLL_MAX_ATTEMPTS; i++) {
            Thread.sleep(POLL_INTERVAL_MS);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + "/generations/" + taskId))
                    .header("Authorization", "Bearer " + apiKey)
                    .GET().timeout(Duration.ofSeconds(30)).build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(resp.body());
            String state = root.path("state").asText("");
            if ("completed".equalsIgnoreCase(state)) {
                JsonNode assets = root.path("assets");
                String url = assets.path("video").asText(null);
                if (url != null && !url.isBlank()) return url;
                throw new VideoGenerationException(name(), "Luma 成功但无视频: " + resp.body());
            }
            if ("failed".equalsIgnoreCase(state)) {
                throw new VideoGenerationException(name(), "Luma 任务失败: " + root.path("failure_reason").asText("unknown"));
            }
        }
        throw new VideoGenerationException(name(), "Luma 任务超时");
    }

    private String getApiKey() {
        String key = configService != null ? configService.getRawValueByKey("ai.luma.api-key") : null;
        if (!StringUtils.hasText(key)) key = apiKeyFromConfig;
        if (!StringUtils.hasText(key)) key = System.getenv("LUMA_API_KEY");
        return StringUtils.hasText(key) ? key.trim() : null;
    }

    private String getApiUrl() {
        String url = configService != null ? configService.getRawValueByKey("ai.luma.api-url") : null;
        if (!StringUtils.hasText(url)) url = apiUrlFromConfig;
        return StringUtils.hasText(url) ? url.trim().replaceAll("/$", "") : DEFAULT_API_URL;
    }

    private static String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) : s;
    }
}
