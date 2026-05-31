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
 * Veo 3.1/3.2 (Google) 视频生成
 *
 * 核心优势:
 *   - 照片级真实感 (专业摄影/电影素材训练)
 *   - 业界最佳音效设计 (完整音效+对话生成)
 *   - Veo 3.2 新增: 4K 原生输出
 *
 * 通过中转 API 访问 (需要海外中转)
 * API: POST {baseUrl}/v1/video/generate
 */
@Component
public class VeoVideoProvider implements AiVideoProvider {

    private static final Logger log = LoggerFactory.getLogger(VeoVideoProvider.class);
    private static final String DEFAULT_API_URL = "https://generativelanguage.googleapis.com";
    private static final int POLL_INTERVAL_MS = 5000;
    private static final int POLL_MAX_ATTEMPTS = 120;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30)).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.ai.veo.api-key:}")
    private String apiKeyFromConfig;
    @Value("${app.ai.veo.api-url:}")
    private String apiUrlFromConfig;
    @Value("${app.ai.veo.model:veo-3.1}")
    private String modelCode;

    @Resource
    private ConfigService configService;

    @Override
    public String name() { return "veo"; }

    @Override
    public boolean isConfigured() { return StringUtils.hasText(getApiKey()); }

    @Override
    public boolean supportsAudioVideoJoint() { return true; }

    @Override
    public String[] contentStrengths() {
        return new String[]{"realistic", "photorealistic", "sound_design", "physics"};
    }

    @Override
    public VideoGenerationResult generateVideo(VideoGenerationRequest request) {
        try {
            String baseUrl = getApiUrl();
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", getModel());
            body.put("prompt", truncate(request.prompt(), 500));
            body.put("duration", Math.min(request.duration(), 10));
            body.put("aspect_ratio", request.aspectRatio() != null ? request.aspectRatio() : "9:16");

            if (request.imageUrl() != null) body.put("image", request.imageUrl());

            if (request.dialogueText() != null || request.sfxHints() != null) {
                var audio = objectMapper.createObjectNode();
                if (request.dialogueText() != null) audio.put("dialogue", request.dialogueText());
                audio.put("sound_design", true);
                body.set("audio", audio);
            }

            HttpRequest httpReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/v1/video/generate"))
                    .header("Authorization", "Bearer " + getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(30)).build();

            HttpResponse<String> resp = httpClient.send(httpReq, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(resp.body());
            String taskId = root.path("name").asText(null);
            if (taskId == null) {
                throw new VideoGenerationException(name(), "Veo 未返回任务ID: " + resp.body());
            }

            String videoUrl = pollVeoTask(baseUrl, taskId);
            boolean hasAudio = request.dialogueText() != null;
            return new VideoGenerationResult(videoUrl, "veo", request.duration() * 1000, true, hasAudio);
        } catch (VideoGenerationException e) {
            throw e;
        } catch (Exception e) {
            throw new VideoGenerationException(name(), "Veo 3.1 生成失败: " + e.getMessage(), e);
        }
    }

    private String pollVeoTask(String baseUrl, String taskId) throws Exception {
        for (int i = 0; i < POLL_MAX_ATTEMPTS; i++) {
            Thread.sleep(POLL_INTERVAL_MS);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/v1/operations/" + taskId))
                    .header("Authorization", "Bearer " + getApiKey())
                    .GET().timeout(Duration.ofSeconds(30)).build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(resp.body());
            if (root.path("done").asBoolean(false)) {
                String url = root.path("response").path("video_url").asText(null);
                if (url != null && !url.isBlank()) return url;
                throw new VideoGenerationException(name(), "Veo 成功但无 video_url: " + resp.body());
            }
            if (root.has("error")) {
                throw new VideoGenerationException(name(), "Veo 失败: " + root.path("error").path("message").asText());
            }
        }
        throw new VideoGenerationException(name(), "Veo 超时");
    }

    private String getApiKey() {
        String key = configService != null ? configService.getRawValueByKey("ai.veo.api-key") : null;
        if (!StringUtils.hasText(key)) key = apiKeyFromConfig;
        if (!StringUtils.hasText(key)) key = System.getenv("VEO_API_KEY");
        return StringUtils.hasText(key) ? key.trim() : null;
    }

    private String getApiUrl() {
        String url = configService != null ? configService.getRawValueByKey("ai.veo.api-url") : null;
        if (!StringUtils.hasText(url)) url = apiUrlFromConfig;
        return StringUtils.hasText(url) ? url.trim().replaceAll("/$", "") : DEFAULT_API_URL;
    }

    private String getModel() {
        String m = configService != null ? configService.getRawValueByKey("ai.veo.model") : null;
        if (!StringUtils.hasText(m)) m = modelCode;
        return StringUtils.hasText(m) ? m.trim() : "veo-3.1";
    }

    private static String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) : s;
    }
}
