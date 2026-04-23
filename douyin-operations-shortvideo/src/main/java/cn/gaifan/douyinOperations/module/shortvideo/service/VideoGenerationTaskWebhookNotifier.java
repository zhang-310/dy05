package cn.gaifan.douyinOperations.module.shortvideo.service;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvVideoGenerationTask;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvWebhookDlq;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvWebhookDlqRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * T-5：图生视频异步任务终态（completed / failed / cancelled）Webhook 通知（可选全局 URL 或单次提交 webhookUrl）。
 */
@Service
public class VideoGenerationTaskWebhookNotifier {

    private static final Logger log = LoggerFactory.getLogger(VideoGenerationTaskWebhookNotifier.class);
    private static final int MAX_RESULT_JSON_IN_PAYLOAD = 48 * 1024;

    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private SvWebhookDlqRepository webhookDlqRepository;

    @Value("${app.shortvideo.video-task.webhook.enabled:false}")
    private boolean webhookEnabled;

    @Value("${app.shortvideo.video-task.webhook.url:}")
    private String defaultWebhookUrl;

    @Value("${app.shortvideo.video-task.webhook.secret:}")
    private String webhookSecret;

    @Value("${app.shortvideo.video-task.webhook.allow-request-override:true}")
    private boolean allowRequestOverride;

    @Value("${app.shortvideo.video-task.webhook.connect-timeout-seconds:5}")
    private int connectTimeoutSeconds;

    @Value("${app.shortvideo.video-task.webhook.read-timeout-seconds:20}")
    private int readTimeoutSeconds;

    @Value("${app.shortvideo.video-task.webhook.max-attempts:4}")
    private int webhookMaxAttempts;

    @Value("${app.shortvideo.video-task.webhook.retry-delay-ms:500}")
    private long webhookRetryDelayMs;

    public VideoGenerationTaskWebhookNotifier(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Async
    public void notifyTaskEnded(SvVideoGenerationTask task) {
        if (task == null || task.getId() == null) {
            return;
        }
        String perTask = extractRequestWebhookUrl(task);
        String def = trimUrl(defaultWebhookUrl);
        String url;
        if (allowRequestOverride && StringUtils.hasText(perTask) && isAllowedWebhookUrl(perTask)) {
            url = perTask;
        } else if (webhookEnabled && StringUtils.hasText(def) && isAllowedWebhookUrl(def)) {
            url = def;
        } else {
            return;
        }
        try {
            String body = buildPayloadJson(task);
            int max = Math.max(1, webhookMaxAttempts);
            Exception lastEx = null;
            Integer lastHttp = null;
            for (int attempt = 0; attempt < max; attempt++) {
                try {
                    HttpClient client = HttpClient.newBuilder()
                            .connectTimeout(Duration.ofSeconds(Math.max(1, connectTimeoutSeconds)))
                            .build();
                    HttpRequest.Builder rb = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .timeout(Duration.ofSeconds(Math.max(1, readTimeoutSeconds)))
                            .header("Content-Type", "application/json; charset=UTF-8")
                            .header("User-Agent", "dy01-sv-video-task-webhook/1.0")
                            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
                    if (StringUtils.hasText(webhookSecret)) {
                        String sig = hmacSha256Hex(webhookSecret, body);
                        rb.header("X-SV-Task-Signature", "sha256=" + sig);
                    }
                    HttpResponse<String> resp = client.send(rb.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                    int code = resp.statusCode();
                    if (code >= 200 && code < 300) {
                        return;
                    }
                    lastHttp = code;
                    lastEx = null;
                    if (attempt < max - 1 && shouldRetryWebhookStatus(code)) {
                        webhookBackoffSleep(attempt);
                        continue;
                    }
                    log.warn("视频任务 Webhook 非 2xx: taskId={} status={} bodyPrefix={}",
                            task.getId(), code, abbrev(resp.body(), 200));
                    persistWebhookDlq(task, url, max, lastHttp, null);
                    return;
                } catch (Exception e) {
                    lastEx = e;
                    lastHttp = null;
                    if (attempt < max - 1 && shouldRetryWebhookException(e)) {
                        webhookBackoffSleep(attempt);
                    } else {
                        break;
                    }
                }
            }
            if (lastEx != null) {
                log.warn("视频任务 Webhook 发送失败 taskId={} after {} attempts: {}", task.getId(), max, lastEx.getMessage());
            }
            persistWebhookDlq(task, url, max, lastHttp, lastEx);
        } catch (Exception e) {
            log.warn("视频任务 Webhook 构建失败 taskId={}: {}", task.getId(), e.getMessage());
        }
    }

    private void persistWebhookDlq(SvVideoGenerationTask task, String webhookUrl, int attempts, Integer lastHttp, Throwable error) {
        if (webhookDlqRepository == null || task == null || task.getId() == null || !StringUtils.hasText(webhookUrl)) {
            return;
        }
        try {
            SvWebhookDlq row = new SvWebhookDlq();
            row.setOwnerId(task.getOwnerId());
            row.setTaskId(task.getId());
            row.setWebhookUrlSha256(sha256HexUtf8(webhookUrl.trim()));
            row.setLastHttpStatus(lastHttp);
            row.setAttemptCount(attempts);
            row.setEventCode("sv.video_generation.task.ended");
            if (error != null) {
                row.setErrorPreview(abbrev(error.getMessage(), 500));
            } else if (lastHttp != null) {
                row.setErrorPreview("http_" + lastHttp);
            }
            webhookDlqRepository.save(row);
        } catch (Exception e) {
            log.debug("Webhook DLQ 落库跳过: {}", e.getMessage());
        }
    }

    private static String sha256HexUtf8(String s) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] raw = md.digest(s.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(raw.length * 2);
        for (byte b : raw) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private void webhookBackoffSleep(int attemptIndex) {
        long base = Math.max(100L, webhookRetryDelayMs);
        long jitter = ThreadLocalRandom.current().nextLong(0, Math.min(250L, base));
        try {
            Thread.sleep(base * (1L << Math.min(attemptIndex, 4)) + jitter);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private static boolean shouldRetryWebhookStatus(int code) {
        return code == 429 || code == 502 || code == 503 || code == 504 || (code >= 500 && code < 600);
    }

    private static boolean shouldRetryWebhookException(Throwable t) {
        if (t instanceof java.net.http.HttpTimeoutException) {
            return true;
        }
        if (t instanceof java.io.IOException) {
            return true;
        }
        String m = String.valueOf(t.getMessage()).toLowerCase();
        return m.contains("timeout") || m.contains("connection reset");
    }

    private String extractRequestWebhookUrl(SvVideoGenerationTask task) {
        if (!StringUtils.hasText(task.getRequestJson())) {
            return null;
        }
        try {
            Map<String, Object> req = objectMapper.readValue(task.getRequestJson(), new TypeReference<>() {});
            if (req.get("webhookUrl") instanceof String s && StringUtils.hasText(s)) {
                return trimUrl(s);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String trimUrl(String s) {
        return s == null ? "" : s.trim();
    }

    private static boolean isAllowedWebhookUrl(String u) {
        if (!StringUtils.hasText(u)) {
            return false;
        }
        String lower = u.toLowerCase();
        return lower.startsWith("https://") || lower.startsWith("http://");
    }

    private String buildPayloadJson(SvVideoGenerationTask task) throws Exception {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("event", "sv.video_generation.task.ended");
        m.put("taskId", task.getId());
        m.put("ownerId", task.getOwnerId());
        m.put("status", task.getStatus());
        m.put("projectId", task.getProjectId());
        m.put("shotListId", task.getShotListId());
        m.put("progressCurrent", task.getProgressCurrent());
        m.put("progressTotal", task.getProgressTotal());
        if (StringUtils.hasText(task.getErrorMessage())) {
            m.put("errorMessage", task.getErrorMessage());
        }
        String rj = task.getResultJson();
        if (StringUtils.hasText(rj)) {
            if (rj.length() <= MAX_RESULT_JSON_IN_PAYLOAD) {
                try {
                    m.put("result", objectMapper.readTree(rj));
                } catch (Exception e) {
                    m.put("resultRaw", rj);
                }
            } else {
                m.put("resultTruncated", true);
                m.put("resultByteLength", rj.length());
            }
        }
        m.put("updateTime", task.getUpdateTime() != null ? task.getUpdateTime().getTime() : null);
        return objectMapper.writeValueAsString(m);
    }

    private static String hmacSha256Hex(String secret, String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(raw.length * 2);
        for (byte b : raw) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static String abbrev(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
