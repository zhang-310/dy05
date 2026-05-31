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
 * Suno AI BGM 生成 Provider (Phase 7)
 * 基于 sunoapi.org 等兼容 API，需配置 SUNO_API_KEY
 */
@Component
public class SunoMusicProvider implements AiMusicProvider {

    private static final Logger log = LoggerFactory.getLogger(SunoMusicProvider.class);
    private static final int POLL_INTERVAL_MS = 5000;
    private static final int POLL_MAX_ATTEMPTS = 60; // 5 分钟

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30)).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.ai.suno.api-key:}")
    private String apiKey;
    @Value("${app.ai.suno.api-url:}")
    private String apiUrl;
    @Value("${app.ai.suno.callback-url:https://httpbin.org/post}")
    private String callbackUrl;

    @Override
    public String name() {
        return "suno";
    }

    @Override
    public boolean isConfigured() {
        return StringUtils.hasText(apiKey) && StringUtils.hasText(apiUrl);
    }

    @Override
    public MusicGenerationResult generateMusic(MusicGenerationRequest request) {
        if (!isConfigured()) {
            throw new BusinessException(ErrorCode.AI_TASK_MODEL_NOT_CONFIGURED, "Suno 未配置，请设置 SUNO_API_KEY 和 SUNO_API_URL");
        }
        String base = apiUrl.replaceAll("/+$", "");
        String taskId = submitGenerate(base, request);
        return pollForResult(base, taskId, request);
    }

    private String submitGenerate(String baseUrl, MusicGenerationRequest request) {
        String prompt = StringUtils.hasText(request.styleDescription()) ? request.styleDescription() : "cinematic background music";
        if (prompt.length() > 500) prompt = prompt.substring(0, 500);
        int dur = request.durationSec() > 0 ? Math.min(request.durationSec(), 240) : 30;

        String body = String.format("""
                {"customMode":false,"instrumental":%s,"callBackUrl":"%s","model":"V4_5ALL","prompt":"%s"}
                """,
                request.instrumental(),
                callbackUrl.replace("\"", "\\\""),
                prompt.replace("\\", "\\\\").replace("\"", "\\\""));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/v1/generate"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .timeout(Duration.ofSeconds(30))
                .build();
        try {
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() != 200) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Suno 提交失败: " + resp.statusCode() + " " + resp.body());
            }
            JsonNode root = objectMapper.readTree(resp.body());
            JsonNode data = root.get("data");
            if (data != null && data.has("taskId")) {
                return data.get("taskId").asText();
            }
            if (root.has("task_id")) return root.get("task_id").asText();
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Suno 响应无 taskId: " + resp.body());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Suno 提交失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Suno 调用失败: " + e.getMessage());
        }
    }

    private MusicGenerationResult pollForResult(String baseUrl, String taskId, MusicGenerationRequest request) {
        String url = baseUrl + "/api/v1/generate/record-info?taskId=" + taskId;
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
                JsonNode dataArr = root.get("data");
                if (dataArr != null && dataArr.isArray() && dataArr.size() > 0) {
                    JsonNode first = dataArr.get(0);
                    String audioUrl = first.has("audio_url") ? first.get("audio_url").asText() : null;
                    if (StringUtils.hasText(audioUrl)) {
                        double duration = first.has("duration") ? first.get("duration").asDouble() : request.durationSec() * 1000.0;
                        return new MusicGenerationResult(audioUrl, "suno", (int) duration, 120);
                    }
                }
                String status = root.has("status") ? root.get("status").asText() : "";
                if ("FAILED".equalsIgnoreCase(status) || "error".equalsIgnoreCase(status)) {
                    throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Suno 生成失败: " + resp.body());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Suno 轮询被中断");
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.warn("Suno 轮询异常 attempt={}: {}", i + 1, e.getMessage());
            }
        }
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Suno 生成超时，请稍后重试");
    }
}
