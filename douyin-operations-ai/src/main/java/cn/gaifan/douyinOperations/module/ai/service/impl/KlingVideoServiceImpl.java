package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.exception.VideoGenerationException;
import cn.gaifan.douyinOperations.module.ai.service.KlingVideoService;
import cn.gaifan.douyinOperations.module.ai.service.VideoEditService;
import cn.gaifan.douyinOperations.module.config.service.ConfigService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

/**
 * Kling（快手可灵）图生视频服务实现
 * 配置 ai.kling.api-key 或 KLING_API_KEY 后可用
 */
@Service
public class KlingVideoServiceImpl implements KlingVideoService {

    private static final Logger log = LoggerFactory.getLogger(KlingVideoServiceImpl.class);
    private static final String API_BASE_DEFAULT = "https://api-beijing.klingai.com/v1/videos";
    private static final int POLL_INTERVAL_MS = 5000;
    private static final int POLL_MAX_ATTEMPTS = 60; // 5 分钟超时

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.video-analysis.work-dir:/tmp/video-edit}")
    private String workDir;

    @Resource
    private ConfigService configService;

    @Value("${app.ai.kling.api-key:}")
    private String klingApiKeyFromConfig;

    @Value("${app.ai.kling.api-secret:}")
    private String klingApiSecretFromConfig;

    @Value("${app.ai.kling.video-api-url:}")
    private String klingVideoApiUrlFromConfig;

    @Override
    public boolean isConfigured() {
        String key = getApiKey();
        return StringUtils.hasText(key);
    }

    private String getApiKey() {
        String key = configService != null ? configService.getRawValueByKey("ai.kling.api-key") : null;
        if (key == null || key.isBlank()) key = klingApiKeyFromConfig;
        if (key == null || key.isBlank()) key = System.getenv("KLING_API_KEY");
        return StringUtils.hasText(key) ? key.trim() : null;
    }

    private String getApiSecret() {
        String secret = configService != null ? configService.getRawValueByKey("ai.kling.api-secret") : null;
        if (secret == null || secret.isBlank()) secret = klingApiSecretFromConfig;
        if (secret == null || secret.isBlank()) secret = System.getenv("KLING_API_SECRET");
        return StringUtils.hasText(secret) ? secret.trim() : null;
    }

    private String getApiBase() {
        String url = configService != null ? configService.getRawValueByKey("ai.kling.video-api-url") : null;
        if (url == null || url.isBlank()) url = klingVideoApiUrlFromConfig;
        if (url == null || url.isBlank()) url = System.getenv("KLING_VIDEO_API_URL");
        return StringUtils.hasText(url) ? url.trim().replaceAll("/$", "") : API_BASE_DEFAULT;
    }

    /** 有 Secret 时用 JWT（官方平台），否则用 API Key */
    private String getBearerToken() {
        String key = getApiKey();
        String secret = getApiSecret();
        if (StringUtils.hasText(secret)) {
            try {
                long nowSec = System.currentTimeMillis() / 1000;
                SecretKey sk = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
                return Jwts.builder()
                        .issuer(key)
                        .issuedAt(Date.from(java.time.Instant.ofEpochSecond(nowSec - 5)))
                        .notBefore(Date.from(java.time.Instant.ofEpochSecond(nowSec - 5)))
                        .expiration(Date.from(java.time.Instant.ofEpochSecond(nowSec + 1800)))
                        .signWith(sk)
                        .compact();
            } catch (Exception e) {
                log.warn("可灵视频 JWT 生成失败，回退 API Key: {}", e.getMessage());
            }
        }
        return key;
    }

    @Override
    public VideoEditService.VideoResult img2video(String imageUrl, int duration, String prompt, Long ownerId) {
        String apiKey = getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Kling API Key 未配置，请在 ai.kling.api-key 或 KLING_API_KEY 中配置");
        }
        if (!StringUtils.hasText(imageUrl)) {
            throw new IllegalArgumentException("图片 URL 不能为空");
        }
        if (duration < 1 || duration > 10) {
            duration = 5;
        }
        String promptText = StringUtils.hasText(prompt) ? prompt : "自然运镜，电影级质感";

        try {
            String bearer = getBearerToken();
            String taskId = submitTask(bearer, imageUrl, duration, promptText);
            String videoUrl = pollTask(bearer, taskId);
            if (videoUrl == null || videoUrl.isBlank()) {
                throw new VideoGenerationException("kling","Kling 返回的视频 URL 为空");
            }
            Path localPath = downloadToTemp(videoUrl);
            File f = localPath.toFile();
            long size = f.length();
            return new VideoEditService.VideoResult(
                    localPath.toString(),
                    (long) duration * 1000,
                    size,
                    "mp4"
            );
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg == null || msg.isBlank()) msg = e.getClass().getSimpleName();
            log.warn("Kling 图生视频失败: {}", msg, e);
            throw new VideoGenerationException("kling","Kling 图生视频失败: " + msg, e);
        }
    }

    @Override
    public String img2videoUrl(String imageUrl, int duration, String prompt, Long ownerId) {
        return img2videoUrl(imageUrl, duration, prompt, ownerId, null);
    }

    @Override
    public String img2videoUrl(String imageUrl, int duration, String prompt, Long ownerId, String mode) {
        String apiKey = getApiKey();
        if (apiKey == null || apiKey.isBlank()) return null;
        if (!StringUtils.hasText(imageUrl)) return null;
        if (duration < 1 || duration > 10) duration = 5;
        String promptText = StringUtils.hasText(prompt) ? prompt : "自然运镜，电影级质感";
        String modeVal = StringUtils.hasText(mode) ? mode : "pro";
        try {
            String bearer = getBearerToken();
            String taskId = submitTask(bearer, imageUrl, duration, promptText, modeVal);
            return pollTask(bearer, taskId);
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg == null || msg.isBlank()) msg = e.getClass().getSimpleName();
            log.warn("Kling 图生视频失败: {}", msg);
            return null;
        }
    }

    private String submitTask(String bearerToken, String imageUrl, int duration, String prompt) throws Exception {
        return submitTask(bearerToken, imageUrl, duration, prompt, "pro");
    }

    private String submitTask(String bearerToken, String imageUrl, int duration, String prompt, String mode) throws Exception {
        String body = String.format(
                "{\"image_url\":\"%s\",\"duration\":%d,\"prompt\":\"%s\",\"mode\":\"%s\",\"fps\":24}",
                escapeJson(imageUrl), duration, escapeJson(prompt), escapeJson(mode != null ? mode : "pro")
        );
        String base = getApiBase();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(base + "/image2video"))
                .header("Authorization", "Bearer " + bearerToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .timeout(Duration.ofSeconds(30))
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() != 200 && resp.statusCode() != 201) {
            log.warn("Kling 视频提交失败: status={} body={}", resp.statusCode(), resp.body());
            throw new VideoGenerationException("kling","Kling 提交失败: " + resp.statusCode() + " " + resp.body());
        }
        JsonNode root = objectMapper.readTree(resp.body());
        JsonNode taskIdNode = root.path("task_id");
        if (taskIdNode.isMissingNode()) {
            taskIdNode = root.path("data").path("task_id");
        }
        String taskId = taskIdNode.asText(null);
        if (taskId == null || taskId.isBlank()) {
            throw new VideoGenerationException("kling","Kling 未返回 task_id: " + resp.body());
        }
        return taskId;
    }

    private String pollTask(String bearerToken, String taskId) throws Exception {
        String statusUrl = getApiBase() + "/image2video/" + taskId;
        for (int i = 0; i < POLL_MAX_ATTEMPTS; i++) {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(statusUrl))
                    .header("Authorization", "Bearer " + bearerToken)
                    .GET()
                    .timeout(Duration.ofSeconds(30))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() != 200) {
                throw new VideoGenerationException("kling","Kling 查询状态失败: " + resp.statusCode() + " " + resp.body());
            }
            JsonNode root = objectMapper.readTree(resp.body());
            String state = root.path("data").path("task_status").asText(null);
            if (state == null) {
                state = root.path("task_status").asText(null);
            }
            if (state == null) {
                state = root.path("status").asText(null);
            }
            if ("succeeded".equals(state) || "completed".equals(state) || "done".equals(state)) {
                JsonNode data = root.path("data");
                String url = extractVideoUrl(data);
                if (url == null) url = root.path("video_url").asText(null);
                if (url == null) url = root.path("url").asText(null);
                if (url != null && !url.isBlank()) return url;
                throw new VideoGenerationException("kling","Kling 返回成功但无视频 URL: " + resp.body());
            }
            if ("failed".equals(state) || "error".equals(state)) {
                String err = root.path("data").path("task_status_msg").asText("");
                if (err.isEmpty()) err = root.path("message").asText("");
                throw new VideoGenerationException("kling","Kling 任务失败: " + err);
            }
            Thread.sleep(POLL_INTERVAL_MS);
        }
        throw new VideoGenerationException("kling","Kling 任务超时");
    }

    private Path downloadToTemp(String videoUrl) throws Exception {
        Path workPath = Path.of(workDir);
        if (!Files.exists(workPath)) {
            Files.createDirectories(workPath);
        }
        String name = "kling_" + UUID.randomUUID().toString().substring(0, 8) + ".mp4";
        Path dest = workPath.resolve(name);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(videoUrl))
                .GET()
                .timeout(Duration.ofSeconds(120))
                .build();
        HttpResponse<InputStream> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofInputStream());
        if (resp.statusCode() != 200) {
            throw new VideoGenerationException("kling","下载 Kling 视频失败: " + resp.statusCode());
        }
        try (InputStream in = resp.body()) {
            Files.copy(in, dest);
        }
        return dest;
    }

    private static String extractVideoUrl(JsonNode data) {
        if (data == null || data.isMissingNode()) return null;
        JsonNode videos = data.path("task_result").path("videos");
        if (videos.isArray() && videos.size() > 0) {
            String u = videos.get(0).path("url").asText(null);
            if (u != null && !u.isBlank()) return u;
        }
        return data.path("video_url").asText(null);
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
