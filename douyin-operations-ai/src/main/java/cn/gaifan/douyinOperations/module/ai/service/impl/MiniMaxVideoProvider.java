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
 * MiniMax Hailuo 视频生成
 *
 * API 文档: https://platform.minimax.io/docs/guides/video-generation
 *
 * 支持模式:
 *   - 文生视频 (text-to-video)
 *   - 图生视频 (image-to-video): first_frame_image
 *   - 首尾帧视频: first_frame_image + last_frame_image
 *
 * 参数:
 *   model: "MiniMax-Hailuo-2.3" 或 "MiniMax-Hailuo-2.3-Fast"
 *   prompt: 场景描述
 *   first_frame_image: 首帧图片 URL
 *   last_frame_image: 尾帧图片 URL (可选)
 *   duration: 6 (固定 6 秒)
 *   resolution: "1080P"
 */
@Component
public class MiniMaxVideoProvider implements AiVideoProvider {

    private static final Logger log = LoggerFactory.getLogger(MiniMaxVideoProvider.class);
    private static final String DEFAULT_API_URL = "https://api.minimax.io/v1";
    private static final int POLL_INTERVAL_MS = 5000;
    private static final int POLL_MAX_ATTEMPTS = 120; // 10 分钟

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30)).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.ai.minimax.api-key:}")
    private String apiKeyFromConfig;
    @Value("${app.ai.minimax.api-url:}")
    private String apiUrlFromConfig;

    @Resource
    private ConfigService configService;

    @Override
    public String name() { return "minimax"; }

    @Override
    public boolean isConfigured() {
        return StringUtils.hasText(getApiKey());
    }

    @Override
    public boolean supportsNegativePrompt() { return false; }

    @Override
    public boolean supportsEndFrame() { return true; }

    @Override
    public String[] contentStrengths() {
        return new String[]{"action", "dance", "physics", "body_movement"};
    }

    @Override
    public VideoGenerationResult generateVideo(VideoGenerationRequest request) {
        String apiKey = getApiKey();
        String baseUrl = getApiUrl();

        try {
            // 1. 构建请求体 - Image to Video
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", "MiniMax-Hailuo-2.3");
            body.put("prompt", truncate(request.prompt(), 500));

            ObjectNode firstFrame = objectMapper.createObjectNode();
            firstFrame.put("type", "image_url");
            firstFrame.put("image_url", request.imageUrl());
            body.set("first_frame_image", firstFrame);

            if (StringUtils.hasText(request.endFrameUrl())) {
                ObjectNode lastFrame = objectMapper.createObjectNode();
                lastFrame.put("type", "image_url");
                lastFrame.put("image_url", request.endFrameUrl());
                body.set("last_frame_image", lastFrame);
            }

            // 2. 提交任务
            String createUrl = baseUrl + "/video_generation";
            HttpRequest httpReq = HttpRequest.newBuilder()
                    .uri(URI.create(createUrl))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> resp = httpClient.send(httpReq, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(resp.body());
            String taskId = root.path("task_id").asText(null);
            if (taskId == null) {
                taskId = root.path("data").path("task_id").asText(null);
            }
            if (taskId == null || taskId.isBlank()) {
                throw new VideoGenerationException(name(), "MiniMax 未返回 task_id: " + resp.body());
            }

            // 3. 轮询结果
            String videoUrl = pollMiniMaxTask(apiKey, baseUrl, taskId);
            return new VideoGenerationResult(videoUrl, "minimax", 6000, true);

        } catch (VideoGenerationException e) {
            throw e;
        } catch (Exception e) {
            throw new VideoGenerationException(name(), "MiniMax 视频生成失败: " + e.getMessage(), e);
        }
    }

    private String pollMiniMaxTask(String apiKey, String baseUrl, String taskId) throws Exception {
        String statusUrl = baseUrl + "/video_generation/" + taskId;
        for (int i = 0; i < POLL_MAX_ATTEMPTS; i++) {
            Thread.sleep(POLL_INTERVAL_MS);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(statusUrl))
                    .header("Authorization", "Bearer " + apiKey)
                    .GET().timeout(Duration.ofSeconds(30)).build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(resp.body());
            JsonNode data = root.path("data");
            String status = data.path("task_status").asText(null);
            if (status == null) status = root.path("status").asText("");

            if ("Success".equalsIgnoreCase(status) || "Finished".equalsIgnoreCase(status) || "success".equalsIgnoreCase(status)) {
                String url = data.path("video_url").asText(null);
                if (url == null) url = data.path("file_id").asText(null);
                if (url == null) url = root.path("video_url").asText(null);
                if (url == null) url = root.path("file_id").asText(null);
                if (url != null && !url.isBlank()) return url;
                throw new VideoGenerationException(name(), "MiniMax 成功但无视频 URL: " + resp.body());
            }
            if ("Fail".equalsIgnoreCase(status) || "Failed".equalsIgnoreCase(status)) {
                String err = data.path("task_status_msg").asText("");
                if (err.isEmpty()) err = root.path("message").asText("");
                throw new VideoGenerationException(name(), "MiniMax 任务失败: " + err);
            }
        }
        throw new VideoGenerationException(name(), "MiniMax 任务超时");
    }

    private String getApiKey() {
        String key = configService != null ? configService.getRawValueByKey("ai.minimax.api-key") : null;
        if (!StringUtils.hasText(key)) key = apiKeyFromConfig;
        if (!StringUtils.hasText(key)) key = System.getenv("MINIMAX_API_KEY");
        return StringUtils.hasText(key) ? key.trim() : null;
    }

    private String getApiUrl() {
        String url = configService != null ? configService.getRawValueByKey("ai.minimax.api-url") : null;
        if (!StringUtils.hasText(url)) url = apiUrlFromConfig;
        return StringUtils.hasText(url) ? url.trim().replaceAll("/$", "") : DEFAULT_API_URL;
    }

    private static String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) : s;
    }
}
