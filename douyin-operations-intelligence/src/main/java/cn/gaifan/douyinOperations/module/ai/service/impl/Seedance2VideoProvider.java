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
 * Seedance 2.0 (字节跳动) 视频生成
 *
 * 核心优势:
 *   - 音视频联合生成: 对话+音效+背景音一体化
 *   - 12 个参考文件输入: 多角色一致性
 *   - 导演级控制: 运镜指令精确执行
 *   - 最长 15 秒
 *
 * API: POST {baseUrl}/v2/video/generate
 */
@Component
public class Seedance2VideoProvider implements AiVideoProvider {

    private static final Logger log = LoggerFactory.getLogger(Seedance2VideoProvider.class);
    private static final String DEFAULT_API_URL = "https://api.seedance.ai";
    private static final int POLL_INTERVAL_MS = 5000;
    private static final int POLL_MAX_ATTEMPTS = 120;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30)).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.ai.seedance.api-key:}")
    private String apiKeyFromConfig;
    @Value("${app.ai.seedance.api-url:}")
    private String apiUrlFromConfig;

    @Resource
    private ConfigService configService;

    @Override
    public String name() { return "seedance2"; }

    @Override
    public boolean isConfigured() { return StringUtils.hasText(getApiKey()); }

    @Override
    public boolean supportsAudioVideoJoint() { return true; }

    @Override
    public boolean supportsMultiReference() { return true; }

    @Override
    public int maxReferenceImages() { return 12; }

    @Override
    public boolean supportsEndFrame() { return true; }

    @Override
    public String[] contentStrengths() {
        return new String[]{"panorama", "cinematic", "multi_shot", "director_control"};
    }

    @Override
    public VideoGenerationResult generateVideo(VideoGenerationRequest request) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", "seedance-2.0");
            body.put("prompt", truncate(request.prompt(), 500));
            body.put("duration", Math.min(request.duration(), 15));
            body.put("aspect_ratio", request.aspectRatio() != null ? request.aspectRatio() : "9:16");

            if (request.imageUrl() != null) {
                body.put("first_frame_image", request.imageUrl());
            }

            if (request.referenceImageUrls() != null && !request.referenceImageUrls().isEmpty()) {
                var refs = objectMapper.createArrayNode();
                request.referenceImageUrls().stream().limit(12).forEach(refs::add);
                body.set("reference_images", refs);
            }

            if (request.dialogueText() != null || request.sfxHints() != null) {
                var audio = objectMapper.createObjectNode();
                if (request.dialogueText() != null) audio.put("dialogue", request.dialogueText());
                if (request.sfxHints() != null) audio.put("sfx_hints", request.sfxHints());
                body.set("audio", audio);
            }

            String baseUrl = getApiUrl();

            HttpRequest httpReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/v2/video/generate"))
                    .header("Authorization", "Bearer " + getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(30)).build();

            HttpResponse<String> resp = httpClient.send(httpReq, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(resp.body());
            String taskId = root.path("task_id").asText(null);
            if (taskId == null) taskId = root.path("data").path("task_id").asText(null);
            if (taskId == null || taskId.isBlank()) {
                throw new VideoGenerationException(name(), "Seedance 未返回 task_id: " + resp.body());
            }

            String videoUrl = pollTask(baseUrl, taskId);
            boolean hasAudio = request.dialogueText() != null || request.sfxHints() != null;
            return new VideoGenerationResult(videoUrl, "seedance2",
                    request.duration() * 1000, true, hasAudio);
        } catch (VideoGenerationException e) {
            throw e;
        } catch (Exception e) {
            throw new VideoGenerationException(name(), "Seedance 2.0 生成失败: " + e.getMessage(), e);
        }
    }

    private String pollTask(String baseUrl, String taskId) throws Exception {
        for (int i = 0; i < POLL_MAX_ATTEMPTS; i++) {
            Thread.sleep(POLL_INTERVAL_MS);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/v2/video/status/" + taskId))
                    .header("Authorization", "Bearer " + getApiKey())
                    .GET().timeout(Duration.ofSeconds(30)).build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(resp.body());
            String status = root.path("status").asText("");
            if (status.isEmpty()) status = root.path("data").path("task_status").asText("");
            if ("success".equalsIgnoreCase(status) || "Success".equals(status)) {
                String url = root.path("video_url").asText(null);
                if (url == null) url = root.path("data").path("video_url").asText(null);
                if (url != null && !url.isBlank()) return url;
                throw new VideoGenerationException(name(), "Seedance 成功但无 video_url: " + resp.body());
            }
            if ("failed".equalsIgnoreCase(status) || "Failed".equals(status)) {
                throw new VideoGenerationException(name(), "Seedance 任务失败: " + resp.body());
            }
        }
        throw new VideoGenerationException(name(), "Seedance 任务超时");
    }

    private String getApiKey() {
        String key = configService != null ? configService.getRawValueByKey("ai.seedance.api-key") : null;
        if (!StringUtils.hasText(key)) key = apiKeyFromConfig;
        if (!StringUtils.hasText(key)) key = System.getenv("SEEDANCE_API_KEY");
        return StringUtils.hasText(key) ? key.trim() : null;
    }

    private String getApiUrl() {
        String url = configService != null ? configService.getRawValueByKey("ai.seedance.api-url") : null;
        if (!StringUtils.hasText(url)) url = apiUrlFromConfig;
        return StringUtils.hasText(url) ? url.trim().replaceAll("/$", "") : DEFAULT_API_URL;
    }

    private static String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) : s;
    }
}
