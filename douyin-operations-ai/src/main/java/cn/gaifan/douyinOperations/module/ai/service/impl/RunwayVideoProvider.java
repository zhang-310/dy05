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
 * Runway Gen-4 Turbo 视频生成 (通过海外中转 API)
 *
 * 注意: Runway Gen-4 仅支持 Image-to-Video (I2V)，不支持 Text-to-Video (T2V)。
 *       生成速度约为 Gen-3 的 5 倍 (10s 视频约 30s 完成)。
 *
 * API: image_to_video.create()
 * 参数:
 *   model: "gen4_turbo"
 *   prompt_image: 图片 URL
 *   prompt_text: 文本 prompt (< 512 字符)
 *   duration: 5 或 10 (秒)
 *   ratio: "16:9" 或 "9:16"
 *
 * 中转配置: app.ai.runway.api-url (默认 https://api.dev.runwayml.com/v1)
 *           app.ai.runway.api-key (RUNWAYML_API_SECRET)
 */
@Component
public class RunwayVideoProvider implements AiVideoProvider {

    private static final Logger log = LoggerFactory.getLogger(RunwayVideoProvider.class);
    private static final String DEFAULT_API_URL = "https://api.dev.runwayml.com/v1";
    private static final int POLL_INTERVAL_MS = 5000;
    private static final int POLL_MAX_ATTEMPTS = 120;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30)).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.ai.runway.api-key:}")
    private String apiKeyFromConfig;
    @Value("${app.ai.runway.api-url:}")
    private String apiUrlFromConfig;

    @Resource
    private ConfigService configService;

    @Override
    public String name() { return "runway"; }

    @Override
    public boolean isConfigured() {
        return StringUtils.hasText(getApiKey());
    }

    @Override
    public boolean supportsNegativePrompt() { return false; }

    @Override
    public boolean supportsAspectRatio(String ratio) {
        return ratio == null || "9:16".equals(ratio) || "16:9".equals(ratio);
    }

    @Override
    public String[] contentStrengths() {
        return new String[]{"vfx", "motion", "effects", "stylized"};
    }

    @Override
    public VideoGenerationResult generateVideo(VideoGenerationRequest request) {
        try {
            String apiUrl = getApiUrl();
            String apiKey = getApiKey();

            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", "gen4_turbo");
            body.put("prompt_image", request.imageUrl());
            body.put("prompt_text", truncate(request.prompt(), 500));
            body.put("duration", Math.min(Math.max(request.duration(), 5), 10));
            body.put("ratio", request.aspectRatio() != null ? request.aspectRatio() : "9:16");
            body.put("watermark", false);

            HttpRequest httpReq = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + "/image_to_video"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("X-Runway-Version", "2024-11-06")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(30)).build();

            HttpResponse<String> resp = httpClient.send(httpReq, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(resp.body());
            String taskId = root.path("id").asText(null);
            if (taskId == null) {
                throw new VideoGenerationException(name(), "Runway 未返回任务 ID: " + resp.body());
            }

            String videoUrl = pollRunwayTask(apiKey, apiUrl, taskId);
            return new VideoGenerationResult(videoUrl, "runway", request.duration() * 1000, true);

        } catch (VideoGenerationException e) {
            throw e;
        } catch (Exception e) {
            throw new VideoGenerationException(name(), "Runway 视频生成失败: " + e.getMessage(), e);
        }
    }

    private String pollRunwayTask(String apiKey, String apiUrl, String taskId) throws Exception {
        for (int i = 0; i < POLL_MAX_ATTEMPTS; i++) {
            Thread.sleep(POLL_INTERVAL_MS);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + "/tasks/" + taskId))
                    .header("Authorization", "Bearer " + apiKey)
                    .GET().timeout(Duration.ofSeconds(30)).build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(resp.body());
            String status = root.path("status").asText("");
            if ("SUCCEEDED".equalsIgnoreCase(status)) {
                JsonNode output = root.path("output");
                if (output.isArray() && output.size() > 0) {
                    return output.get(0).asText();
                }
                throw new VideoGenerationException(name(), "Runway 成功但无输出: " + resp.body());
            }
            if ("FAILED".equalsIgnoreCase(status)) {
                throw new VideoGenerationException(name(), "Runway 任务失败: " + root.path("failure").asText("unknown"));
            }
        }
        throw new VideoGenerationException(name(), "Runway 任务超时");
    }

    private String getApiKey() {
        String key = configService != null ? configService.getRawValueByKey("ai.runway.api-key") : null;
        if (!StringUtils.hasText(key)) key = apiKeyFromConfig;
        if (!StringUtils.hasText(key)) key = System.getenv("RUNWAY_API_KEY");
        return StringUtils.hasText(key) ? key.trim() : null;
    }

    private String getApiUrl() {
        String url = configService != null ? configService.getRawValueByKey("ai.runway.api-url") : null;
        if (!StringUtils.hasText(url)) url = apiUrlFromConfig;
        return StringUtils.hasText(url) ? url.trim().replaceAll("/$", "") : DEFAULT_API_URL;
    }

    private static String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) : s;
    }
}
