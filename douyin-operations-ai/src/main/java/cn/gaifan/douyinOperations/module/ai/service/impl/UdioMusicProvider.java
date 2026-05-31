package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.ai.service.AiMusicProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Udio AI BGM 生成 Provider (Phase 7)
 * 基于 udioapi.pro 等兼容 API，需配置 UDIO_API_KEY
 */
@Component
public class UdioMusicProvider implements AiMusicProvider {

    private static final Logger log = LoggerFactory.getLogger(UdioMusicProvider.class);
    private static final int POLL_INTERVAL_MS = 5000;
    private static final int POLL_MAX_ATTEMPTS = 60;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30)).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.ai.udio.api-key:}")
    private String apiKey;
    @Value("${app.ai.udio.api-url:}")
    private String apiUrl;

    @Override
    public String name() {
        return "udio";
    }

    @Override
    public boolean isConfigured() {
        return StringUtils.hasText(apiKey) && StringUtils.hasText(apiUrl);
    }

    @Override
    public MusicGenerationResult generateMusic(MusicGenerationRequest request) {
        if (!isConfigured()) {
            throw new BusinessException(ErrorCode.AI_TASK_MODEL_NOT_CONFIGURED, "Udio 未配置，请设置 UDIO_API_KEY 和 UDIO_API_URL");
        }
        String base = apiUrl.replaceAll("/+$", "");
        String taskId = submitGenerate(base, request);
        return pollForResult(base, taskId, request);
    }

    private String submitGenerate(String baseUrl, MusicGenerationRequest request) {
        String prompt = StringUtils.hasText(request.styleDescription()) ? request.styleDescription() : "cinematic background music";
        if (prompt.length() > 400) prompt = prompt.substring(0, 400);

        String body = String.format("""
                {"model":"chirp-v4-5","gpt_description_prompt":"%s","make_instrumental":%s}
                """,
                prompt.replace("\\", "\\\\").replace("\"", "\\\""),
                request.instrumental());

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/v2/generate"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .timeout(Duration.ofSeconds(30))
                .build();
        try {
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() != 200) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Udio 提交失败: " + resp.statusCode() + " " + resp.body());
            }
            JsonNode root = objectMapper.readTree(resp.body());
            String tid = null;
            if (root.has("data") && root.get("data").has("task_id")) {
                tid = root.get("data").get("task_id").asText();
            }
            if (tid == null && root.has("workId")) tid = root.get("workId").asText();
            if (tid == null && root.has("task_id")) tid = root.get("task_id").asText();
            if (StringUtils.hasText(tid)) return tid;
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Udio 响应无 taskId: " + resp.body());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Udio 提交失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Udio 调用失败: " + e.getMessage());
        }
    }

    private MusicGenerationResult pollForResult(String baseUrl, String taskId, MusicGenerationRequest request) {
        String url = baseUrl + "/api/v2/generate/status?task_id=" + taskId;
        for (int i = 0; i < POLL_MAX_ATTEMPTS; i++) {
            try {
                Thread.sleep(POLL_INTERVAL_MS);
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Authorization", "Bearer " + apiKey)
                        .GET()
                        .timeout(Duration.ofSeconds(30))
                        .build();
                HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (resp.statusCode() != 200) continue;
                JsonNode root = objectMapper.readTree(resp.body());
                JsonNode data = root.get("data");
                if (data != null) {
                    String audioUrl = data.has("audio_url") ? data.get("audio_url").asText() : null;
                    if (audioUrl == null && data.has("url")) audioUrl = data.get("url").asText();
                    if (StringUtils.hasText(audioUrl)) {
                        double dur = data.has("duration") ? data.get("duration").asDouble() : request.durationSec() * 1000.0;
                        return new MusicGenerationResult(audioUrl, "udio", (int) dur, 120);
                    }
                }
                JsonNode list = root.get("songs");
                if (list != null && list.isArray() && list.size() > 0) {
                    JsonNode first = list.get(0);
                    String u = first.has("audio_url") ? first.get("audio_url").asText() : first.has("url") ? first.get("url").asText() : null;
                    if (StringUtils.hasText(u)) {
                        return new MusicGenerationResult(u, "udio", request.durationSec() * 1000, 120);
                    }
                }
                String status = root.has("status") ? root.get("status").asText() : "";
                if ("failed".equalsIgnoreCase(status) || "error".equalsIgnoreCase(status)) {
                    throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Udio 生成失败: " + resp.body());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Udio 轮询被中断");
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.warn("Udio 轮询异常 attempt={}: {}", i + 1, e.getMessage());
            }
        }
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Udio 生成超时，请稍后重试");
    }
}
