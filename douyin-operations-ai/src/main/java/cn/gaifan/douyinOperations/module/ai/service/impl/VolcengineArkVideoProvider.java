package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.exception.VideoGenerationException;
import cn.gaifan.douyinOperations.module.ai.service.AiVideoProvider;
import cn.gaifan.douyinOperations.module.config.service.ConfigService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
 * 火山方舟视频生成（Seedance 等），与 Kling/MiniMax 并列接入 {@link cn.gaifan.douyinOperations.module.ai.service.IntelligentModelRouter}。
 * <p>
 * 官方 API 参考：<a href="https://www.volcengine.com/docs/82379/1520757">创建视频生成任务</a>、
 * <a href="https://www.volcengine.com/docs/82379/1521309">查询任务</a>。
 * 默认 {@code POST /contents/generations/tasks}、{@code GET /contents/generations/tasks/{id}}，与控制台地域 Base URL 一致。
 */
@Component
public class VolcengineArkVideoProvider implements AiVideoProvider {

    private static final Logger log = LoggerFactory.getLogger(VolcengineArkVideoProvider.class);
    private static final int POLL_INTERVAL_MS = 4000;
    private static final int POLL_MAX_ATTEMPTS = 180;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.ai.volcengine-ark.api-url:https://ark.cn-beijing.volces.com/api/v3}")
    private String apiUrlDefault;

    @Value("${ARK_API_KEY:}")
    private String arkApiKeyEnv;

    @Value("${app.ai.volcengine-ark.video.enabled:false}")
    private boolean videoEnabled;

    @Value("${app.ai.volcengine-ark.video.model:}")
    private String videoModelFromYml;

    @Resource
    private ConfigService configService;

    @Override
    public String name() {
        return "arkvideo";
    }

    private String configOr(String key, String fallback) {
        if (configService == null) return fallback;
        String v = configService.getRawValueByKey(key);
        return (v != null && !v.isBlank()) ? v.trim() : fallback;
    }

    private String baseUrl() {
        String u = configOr("ai.volcengine_ark.api_url", apiUrlDefault);
        if (u == null || u.isBlank()) return apiUrlDefault;
        return u.endsWith("/") ? u.substring(0, u.length() - 1) : u;
    }

    private String apiKey() {
        String k = configOr("ai.volcengine_ark.api_key", null);
        if (k != null && !k.isBlank()) return k;
        return arkApiKeyEnv != null ? arkApiKeyEnv : "";
    }

    private String videoModel() {
        String m = configOr("ai.volcengine_ark.video_model", null);
        if (m != null && !m.isBlank()) return m;
        return videoModelFromYml != null ? videoModelFromYml.trim() : "";
    }

    @Override
    public boolean isConfigured() {
        return videoEnabled && StringUtils.hasText(apiKey()) && StringUtils.hasText(videoModel());
    }

    @Override
    public boolean supportsEndFrame() {
        return true;
    }

    @Override
    public VideoGenerationResult generateVideo(VideoGenerationRequest request) {
        if (!isConfigured()) {
            throw new VideoGenerationException(name(), "方舟视频未启用或未配置 model/api-key（app.ai.volcengine-ark.video）");
        }
        if (!StringUtils.hasText(request.imageUrl())) {
            throw new VideoGenerationException(name(), "图生视频需要首帧 imageUrl");
        }
        try {
            String model = videoModel();
            ObjectNode root = objectMapper.createObjectNode();
            root.put("model", model);

            ArrayNode content = objectMapper.createArrayNode();
            String ratio = request.aspectRatio() != null ? request.aspectRatio() : "9:16";
            int dur = Math.min(Math.max(request.duration(), 3), 12);
            String text = buildPromptWithHints(request.prompt(), ratio, dur);
            ObjectNode textPart = objectMapper.createObjectNode();
            textPart.put("type", "text");
            textPart.put("text", text);
            content.add(textPart);

            ObjectNode imgPart = objectMapper.createObjectNode();
            imgPart.put("type", "image_url");
            ObjectNode iu = objectMapper.createObjectNode();
            iu.put("url", request.imageUrl());
            imgPart.set("image_url", iu);
            content.add(imgPart);

            if (StringUtils.hasText(request.endFrameUrl())) {
                ObjectNode endPart = objectMapper.createObjectNode();
                endPart.put("type", "image_url");
                ObjectNode iuEnd = objectMapper.createObjectNode();
                iuEnd.put("url", request.endFrameUrl());
                endPart.set("image_url", iuEnd);
                content.add(endPart);
            }

            root.set("content", content);

            String base = baseUrl();
            String createUrl = base + "/contents/generations/tasks";
            HttpRequest httpReq = HttpRequest.newBuilder()
                    .uri(URI.create(createUrl))
                    .header("Authorization", "Bearer " + apiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(root.toString(), StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(60))
                    .build();

            HttpResponse<String> resp = httpClient.send(httpReq, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                throw new VideoGenerationException(name(), "创建任务失败 HTTP " + resp.statusCode() + ": " + resp.body());
            }
            JsonNode createJson = objectMapper.readTree(resp.body());
            String taskId = extractTaskId(createJson);
            if (!StringUtils.hasText(taskId)) {
                throw new VideoGenerationException(name(), "未返回任务 ID: " + resp.body());
            }

            String videoUrl = pollTask(base, taskId);
            int durationMs = dur * 1000;
            return new VideoGenerationResult(videoUrl, name(), durationMs, true, false);
        } catch (VideoGenerationException e) {
            throw e;
        } catch (Exception e) {
            log.error("方舟视频生成失败", e);
            throw new VideoGenerationException(name(), e.getMessage());
        }
    }

    private static String buildPromptWithHints(String prompt, String ratio, int dur) {
        String p = prompt != null ? prompt : "";
        return p + " --ratio " + ratio + " --dur " + dur;
    }

    private static String extractTaskId(JsonNode root) {
        if (root == null) return null;
        String id = root.path("id").asText(null);
        if (StringUtils.hasText(id)) return id;
        id = root.path("task_id").asText(null);
        if (StringUtils.hasText(id)) return id;
        id = root.path("data").path("id").asText(null);
        if (StringUtils.hasText(id)) return id;
        return root.path("data").path("task_id").asText(null);
    }

    private String pollTask(String base, String taskId) throws Exception {
        String queryUrl = base + "/contents/generations/tasks/" + taskId;
        for (int i = 0; i < POLL_MAX_ATTEMPTS; i++) {
            HttpRequest get = HttpRequest.newBuilder()
                    .uri(URI.create(queryUrl))
                    .header("Authorization", "Bearer " + apiKey())
                    .GET()
                    .timeout(Duration.ofSeconds(60))
                    .build();
            HttpResponse<String> r = httpClient.send(get, HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() < 200 || r.statusCode() >= 300) {
                throw new VideoGenerationException(name(), "查询任务 HTTP " + r.statusCode() + ": " + r.body());
            }
            JsonNode node = objectMapper.readTree(r.body());
            JsonNode task = node.has("data") ? node.get("data") : node;
            String status = task.path("status").asText(task.path("task_status").asText("")).toLowerCase();
            if ("succeeded".equals(status) || "success".equals(status) || "completed".equals(status)) {
                String url = extractVideoUrl(task);
                if (StringUtils.hasText(url)) return url;
                url = extractVideoUrl(node);
                if (StringUtils.hasText(url)) return url;
                throw new VideoGenerationException(name(), "任务成功但未解析到视频 URL: " + r.body());
            }
            if ("failed".equals(status) || "cancelled".equals(status) || "canceled".equals(status)) {
                String err = task.path("error").path("message").asText(task.path("failure_reason").asText(status));
                throw new VideoGenerationException(name(), "任务失败: " + err);
            }
            Thread.sleep(POLL_INTERVAL_MS);
        }
        throw new VideoGenerationException(name(), "方舟视频任务轮询超时");
    }

    private static String extractVideoUrl(JsonNode task) {
        if (task == null) return null;
        String u = task.path("video_url").asText(null);
        if (StringUtils.hasText(u)) return u;
        u = task.path("output").path("video_url").asText(null);
        if (StringUtils.hasText(u)) return u;
        JsonNode content = task.path("content");
        if (content.isArray()) {
            for (JsonNode c : content) {
                u = c.path("video_url").asText(null);
                if (StringUtils.hasText(u)) return u;
                JsonNode video = c.path("video");
                if (video.isObject()) {
                    u = video.path("url").asText(null);
                    if (StringUtils.hasText(u)) return u;
                }
            }
        }
        u = task.path("result").path("url").asText(null);
        if (StringUtils.hasText(u)) return u;
        return task.path("artifacts").path(0).path("url").asText(null);
    }
}
