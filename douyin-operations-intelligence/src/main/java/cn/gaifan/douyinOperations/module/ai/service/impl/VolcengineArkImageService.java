package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.service.ImageGenerationService;
import cn.gaifan.douyinOperations.module.config.service.ConfigService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 火山方舟图片生成（Seedream 等）：与视频同源 {@code POST/GET /contents/generations/tasks}，
 * 使用<strong>图片类</strong>推理接入点 ep（与视频 ep 区分）。
 *
 * @see <a href="https://www.volcengine.com/docs/82379/1541523">图片生成 API</a>
 */
@Service
public class VolcengineArkImageService {

    private static final int POLL_INTERVAL_MS = 2500;
    private static final int POLL_MAX_ATTEMPTS = 120;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.ai.volcengine-ark.api-url:https://ark.cn-beijing.volces.com/api/v3}")
    private String apiUrlDefault;

    @Value("${ARK_API_KEY:}")
    private String arkApiKeyEnv;

    @Value("${app.ai.volcengine-ark.image.enabled:false}")
    private boolean imageEnabled;

    @Value("${app.ai.volcengine-ark.image.model:}")
    private String imageModelFromYml;

    @Resource
    private ConfigService configService;

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

    private String imageModel() {
        String m = configOr("ai.volcengine_ark.image_model", null);
        if (m != null && !m.isBlank()) return m;
        return imageModelFromYml != null ? imageModelFromYml.trim() : "";
    }

    public boolean isConfigured() {
        return imageEnabled && StringUtils.hasText(apiKey()) && StringUtils.hasText(imageModel());
    }

    public ImageGenerationService.ImageResult textToImage(ImageGenerationService.TextToImageRequest request,
                                                          long startTimeMs) throws Exception {
        if (!isConfigured()) {
            throw new IllegalStateException("方舟图片未配置");
        }
        int w = request.width() != null ? request.width() : 1024;
        int h = request.height() != null ? request.height() : 1024;
        String prompt = request.prompt() != null ? request.prompt() : "";
        String neg = request.negativePrompt() != null ? request.negativePrompt() : "";
        String text = prompt;
        if (StringUtils.hasText(neg)) {
            text = text + "\nNegative: " + neg;
        }
        text = text + " --size " + w + "x" + h;
        return submitImageTask(text, null, request.prompt(), startTimeMs, Map.of(
                "provider", "volcengine-ark",
                "width", w,
                "height", h,
                "mode", "text2img"
        ));
    }

    public ImageGenerationService.ImageResult imageToImage(ImageGenerationService.ImageToImageRequest request,
                                                           long startTimeMs) throws Exception {
        if (!isConfigured()) {
            throw new IllegalStateException("方舟图片未配置");
        }
        if (!StringUtils.hasText(request.imageUrl())) {
            throw new IllegalArgumentException("参考图 URL 不能为空");
        }
        String prompt = request.prompt() != null && !request.prompt().isBlank()
                ? request.prompt()
                : "参考图生成高质量图像，保持主体一致";
        String neg = request.negativePrompt() != null ? request.negativePrompt() : "";
        String text = prompt;
        if (StringUtils.hasText(neg)) {
            text = text + "\nNegative: " + neg;
        }
        if (request.strength() != null) {
            text = text + " --strength " + request.strength();
        }
        return submitImageTask(text, request.imageUrl(), request.prompt(), startTimeMs, Map.of(
                "provider", "volcengine-ark",
                "mode", "img2img"
        ));
    }

    private ImageGenerationService.ImageResult submitImageTask(String textPayload, String refImageUrl,
                                                               String promptForRecord, long startTimeMs,
                                                               Map<String, Object> extraParams) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", imageModel());

        ArrayNode content = objectMapper.createArrayNode();
        ObjectNode textPart = objectMapper.createObjectNode();
        textPart.put("type", "text");
        textPart.put("text", textPayload);
        content.add(textPart);

        if (StringUtils.hasText(refImageUrl)) {
            ObjectNode imgPart = objectMapper.createObjectNode();
            imgPart.put("type", "image_url");
            ObjectNode iu = objectMapper.createObjectNode();
            iu.put("url", refImageUrl);
            imgPart.set("image_url", iu);
            content.add(imgPart);
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
            throw new RuntimeException("方舟图片创建任务 HTTP " + resp.statusCode() + ": " + resp.body());
        }
        JsonNode createJson = objectMapper.readTree(resp.body());
        String taskId = extractTaskId(createJson);
        if (!StringUtils.hasText(taskId)) {
            throw new RuntimeException("方舟图片未返回任务 ID: " + resp.body());
        }

        String imageUrl = pollImageTask(base, taskId);
        if (!StringUtils.hasText(imageUrl)) {
            throw new RuntimeException("方舟图片结果为空");
        }

        long generationTime = System.currentTimeMillis() - startTimeMs;
        Map<String, Object> parameters = new HashMap<>(extraParams);
        return new ImageGenerationService.ImageResult(imageUrl, promptForRecord, parameters, generationTime);
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

    private String pollImageTask(String base, String taskId) throws Exception {
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
                throw new RuntimeException("查询图片任务 HTTP " + r.statusCode() + ": " + r.body());
            }
            JsonNode node = objectMapper.readTree(r.body());
            JsonNode task = node.has("data") ? node.get("data") : node;
            String status = task.path("status").asText(task.path("task_status").asText("")).toLowerCase();
            if ("succeeded".equals(status) || "success".equals(status) || "completed".equals(status)) {
                String url = extractImageUrl(task);
                if (StringUtils.hasText(url)) return url;
                url = extractImageUrl(node);
                if (StringUtils.hasText(url)) return url;
                throw new RuntimeException("任务成功但未解析到图片 URL: " + r.body());
            }
            if ("failed".equals(status) || "cancelled".equals(status) || "canceled".equals(status)) {
                String err = task.path("error").path("message").asText(task.path("failure_reason").asText(status));
                throw new RuntimeException("方舟图片任务失败: " + err);
            }
            Thread.sleep(POLL_INTERVAL_MS);
        }
        throw new RuntimeException("方舟图片任务轮询超时");
    }

    private static String extractImageUrl(JsonNode task) {
        if (task == null) return null;
        String u = task.path("image_url").asText(null);
        if (StringUtils.hasText(u)) return u;
        u = task.path("output").path("image_url").asText(null);
        if (StringUtils.hasText(u)) return u;
        u = task.path("output").path("url").asText(null);
        if (StringUtils.hasText(u)) return u;
        JsonNode content = task.path("content");
        if (content.isArray()) {
            for (JsonNode c : content) {
                u = c.path("image_url").asText(null);
                if (StringUtils.hasText(u)) return u;
                JsonNode img = c.path("image");
                if (img.isObject()) {
                    u = img.path("url").asText(null);
                    if (StringUtils.hasText(u)) return u;
                }
            }
        }
        u = task.path("result").path("url").asText(null);
        if (StringUtils.hasText(u)) return u;
        JsonNode arts = task.path("artifacts");
        if (arts.isArray() && arts.size() > 0) {
            u = arts.path(0).path("url").asText(null);
            if (StringUtils.hasText(u)) return u;
        }
        return task.path("urls").path(0).asText(null);
    }
}
