package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.ai.service.DigitalHumanProvider;
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
 * HeyGen 数字人提供者 (Phase 8)
 * Avatar IV, 1 credit/10s
 */
@Component
public class HeyGenProvider implements DigitalHumanProvider {

    private static final Logger log = LoggerFactory.getLogger(HeyGenProvider.class);
    private static final String DEFAULT_API = "https://api.heygen.com/v2";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30)).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.ai.heygen.api-key:}")
    private String apiKeyFromConfig;

    @Resource
    private ConfigService configService;

    @Override
    public String name() { return "heygen"; }

    @Override
    public boolean isConfigured() { return StringUtils.hasText(getApiKey()); }

    @Override
    public String generateTalkingHead(String avatarId, String scriptText, String voiceId) {
        if (!isConfigured()) {
            throw new BusinessException(ErrorCode.AI_TASK_MODEL_NOT_CONFIGURED, "HeyGen 未配置");
        }
        try {
            String baseUrl = getApiUrl();
            ObjectNode body = objectMapper.createObjectNode();
            body.put("avatar_id", avatarId != null ? avatarId : "default");
            body.put("script", scriptText != null ? scriptText : "");
            if (StringUtils.hasText(voiceId)) body.put("voice_id", voiceId);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/video/generate"))
                    .header("X-Api-Key", getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(60))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() != 200) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "HeyGen 失败: " + resp.statusCode() + " " + resp.body());
            }
            JsonNode root = objectMapper.readTree(resp.body());
            String videoUrl = root.path("data").path("video_url").asText(null);
            if (videoUrl == null) videoUrl = root.path("video_url").asText(null);
            if (videoUrl == null || videoUrl.isBlank()) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "HeyGen 未返回视频 URL");
            }
            return videoUrl;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("HeyGen 数字人生成失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "HeyGen 失败: " + e.getMessage());
        }
    }

    private String getApiKey() {
        String key = configService != null ? configService.getRawValueByKey("ai.heygen.api-key") : null;
        if (!StringUtils.hasText(key)) key = apiKeyFromConfig;
        if (!StringUtils.hasText(key)) key = System.getenv("HEYGEN_API_KEY");
        return StringUtils.hasText(key) ? key.trim() : null;
    }

    private String getApiUrl() {
        String url = configService != null ? configService.getRawValueByKey("ai.heygen.api-url") : null;
        if (!StringUtils.hasText(url)) url = System.getenv("HEYGEN_API_URL");
        return StringUtils.hasText(url) ? url.trim().replaceAll("/$", "") : DEFAULT_API;
    }
}
