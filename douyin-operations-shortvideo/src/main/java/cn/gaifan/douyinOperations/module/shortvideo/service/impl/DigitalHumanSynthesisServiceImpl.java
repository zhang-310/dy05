package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvDigitalHumanTask;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvDigitalHumanTaskRepository;
import cn.gaifan.douyinOperations.module.shortvideo.service.DigitalHumanSynthesisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 数字人合成服务：支持 stub（本地测试）和 api（HeyGen/D-ID）两种模式。
 * <ul>
 *   <li>{@code app.shortvideo.digital-human.mode=stub} — 返回配置的 stub-video-url</li>
 *   <li>{@code app.shortvideo.digital-human.mode=api} — 调用数字人 API（HeyGen 优先）</li>
 * </ul>
 */
@Slf4j
@Service
public class DigitalHumanSynthesisServiceImpl implements DigitalHumanSynthesisService {

    @Autowired(required = false)
    private SvDigitalHumanTaskRepository taskRepository;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.shortvideo.digital-human.enabled:false}")
    private boolean enabled;

    /** stub | api */
    @Value("${app.shortvideo.digital-human.mode:stub}")
    private String mode;

    /** 测试/联调：直接回写公网可访问的视频 URL */
    @Value("${app.shortvideo.digital-human.stub-video-url:}")
    private String stubVideoUrl;

    /** HeyGen API Key（mode=api 时使用） */
    @Value("${app.shortvideo.digital-human.heygen-api-key:}")
    private String heygenApiKey;

    /** HeyGen API Base URL */
    @Value("${app.shortvideo.digital-human.heygen-api-base:https://api.heygen.com}")
    private String heygenApiBase;

    /** D-ID API Key（HeyGen 不可用时 fallback） */
    @Value("${app.shortvideo.digital-human.did-api-key:}")
    private String didApiKey;

    /** D-ID API Base URL */
    @Value("${app.shortvideo.digital-human.did-api-base:https://api.d-id.com}")
    private String didApiBase;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public boolean isConfigured() {
        if (!enabled) return false;
        if ("api".equalsIgnoreCase(mode)) {
            return StringUtils.hasText(heygenApiKey) || StringUtils.hasText(didApiKey);
        }
        // stub mode
        return StringUtils.hasText(stubVideoUrl);
    }

    @Override
    public String synthesizePlaceholder(Map<String, Object> params, Long userId, Long projectId) {
        if (!enabled) return null;

        if ("api".equalsIgnoreCase(mode)) {
            return synthesizeViaApi(params, userId, projectId);
        }

        // Stub mode fallback
        if (StringUtils.hasText(stubVideoUrl)) {
            log.info("[DigitalHuman] stub 模式: projectId={}, url={}", projectId, stubVideoUrl.trim());
            return stubVideoUrl.trim();
        }
        return null;
    }

    private String synthesizeViaApi(Map<String, Object> params, Long userId, Long projectId) {
        // Try HeyGen first
        if (StringUtils.hasText(heygenApiKey)) {
            try {
                return synthesizeViaHeygen(params, userId, projectId);
            } catch (Exception e) {
                log.warn("[DigitalHuman] HeyGen 调用失败，尝试 D-ID fallback: {}", e.getMessage());
            }
        }

        // Fallback to D-ID
        if (StringUtils.hasText(didApiKey)) {
            try {
                return synthesizeViaDid(params, userId, projectId);
            } catch (Exception e) {
                log.error("[DigitalHuman] D-ID 调用也失败: {}", e.getMessage());
            }
        }

        // Final fallback to stub if configured
        if (StringUtils.hasText(stubVideoUrl)) {
            log.warn("[DigitalHuman] API 不可用，降级到 stub: projectId={}", projectId);
            return stubVideoUrl.trim();
        }

        log.error("[DigitalHuman] 无可用数字人服务: projectId={}", projectId);
        return null;
    }

    /**
     * HeyGen Video Generate API。
     * 文档：POST /v2/video/generate
     */
    @SuppressWarnings("unchecked")
    private String synthesizeViaHeygen(Map<String, Object> params, Long userId, Long projectId) {
        String scriptText = params != null ? String.valueOf(params.getOrDefault("scriptText", "")) : "";
        String avatarId = params != null ? String.valueOf(params.getOrDefault("avatarId", "")) : "";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", heygenApiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "video_inputs", java.util.List.of(Map.of(
                        "character", Map.of(
                                "type", "avatar",
                                "avatar_id", StringUtils.hasText(avatarId) ? avatarId : "default"
                        ),
                        "voice", Map.of(
                                "type", "text",
                                "input_text", scriptText
                        )
                )),
                "dimension", Map.of("width", 1080, "height", 1920)
        );

        HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);
        ResponseEntity<Map> resp = restTemplate.exchange(
                heygenApiBase + "/v2/video/generate", HttpMethod.POST, req, Map.class);

        if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
            Map<String, Object> data = (Map<String, Object>) resp.getBody().get("data");
            if (data != null) {
                String videoId = String.valueOf(data.getOrDefault("video_id", ""));
                log.info("[DigitalHuman] HeyGen 任务已提交: projectId={}, videoId={}", projectId, videoId);
                persistTask("heygen", videoId, params, userId, projectId);
                return "heygen:pending:" + videoId;
            }
        }
        throw new RuntimeException("HeyGen 响应异常: " + resp.getStatusCode());
    }

    /**
     * D-ID Talks API。
     * 文档：POST /talks
     */
    @SuppressWarnings("unchecked")
    private String synthesizeViaDid(Map<String, Object> params, Long userId, Long projectId) {
        String scriptText = params != null ? String.valueOf(params.getOrDefault("scriptText", "")) : "";
        String imageUrl = params != null ? String.valueOf(params.getOrDefault("imageUrl", "")) : "";

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(didApiKey, "");
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "script", Map.of(
                        "type", "text",
                        "input", scriptText
                ),
                "source_url", StringUtils.hasText(imageUrl) ? imageUrl : "https://d-id-public-bucket.s3.us-west-2.amazonaws.com/alice.jpg"
        );

        HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);
        ResponseEntity<Map> resp = restTemplate.exchange(
                didApiBase + "/talks", HttpMethod.POST, req, Map.class);

        if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
            String talkId = String.valueOf(resp.getBody().getOrDefault("id", ""));
            log.info("[DigitalHuman] D-ID 任务已提交: projectId={}, talkId={}", projectId, talkId);
            persistTask("did", talkId, params, userId, projectId);
            return "did:pending:" + talkId;
        }
        throw new RuntimeException("D-ID 响应异常: " + resp.getStatusCode());
    }

    private void persistTask(String provider, String externalId, Map<String, Object> params, Long userId, Long projectId) {
        if (taskRepository == null) return;
        try {
            SvDigitalHumanTask task = new SvDigitalHumanTask();
            task.setUserId(userId);
            task.setProjectId(projectId);
            task.setProvider(provider);
            task.setExternalTaskId(externalId);
            task.setStatus("SUBMITTED");
            task.setParamsJson(objectMapper.writeValueAsString(params));
            taskRepository.save(task);
            log.info("[DigitalHuman] 任务已持久化: provider={}, externalId={}, projectId={}", provider, externalId, projectId);
        } catch (Exception e) {
            log.warn("[DigitalHuman] 任务持久化失败: {}", e.getMessage());
        }
    }
}
