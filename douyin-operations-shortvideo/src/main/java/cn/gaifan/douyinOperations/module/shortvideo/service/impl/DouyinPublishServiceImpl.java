package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.module.douyinapi.service.OAuthTokenService;
import cn.gaifan.douyinOperations.module.shortvideo.service.DouyinPublishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 抖音开放平台视频发布实现。
 * <p>
 * 依赖配置项：
 * <ul>
 *   <li>{@code app.douyin.open-platform.client-key} — 抖音开放平台 Client Key</li>
 *   <li>{@code app.douyin.open-platform.client-secret} — 抖音开放平台 Client Secret</li>
 * </ul>
 * 未配置时所有发布调用返回明确错误提示，不会静默失败。
 */
@Slf4j
@Service
public class DouyinPublishServiceImpl implements DouyinPublishService {

    @Autowired(required = false)
    private OAuthTokenService oAuthTokenService;

    @Value("${app.douyin.open-platform.client-key:}")
    private String clientKey;

    @Value("${app.douyin.open-platform.client-secret:}")
    private String clientSecret;

    @Value("${app.douyin.open-platform.api-base:https://open.douyin.com}")
    private String apiBase;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public boolean isConfigured() {
        return StringUtils.hasText(clientKey) && StringUtils.hasText(clientSecret);
    }

    @Override
    public boolean isUserAuthorized(Long userId) {
        if (oAuthTokenService == null) return false;
        return oAuthTokenService.isTokenValid(userId, "douyin");
    }

    @Override
    public PublishResult publish(String videoUrl, String title, Long userId) {
        if (!isConfigured()) {
            return PublishResult.fail(
                    "抖音开放平台未配置。请在「系统配置」中设置 DOUYIN_CLIENT_KEY 和 DOUYIN_CLIENT_SECRET，"
                    + "并完成 OAuth 授权后重试。");
        }
        if (!StringUtils.hasText(videoUrl)) {
            return PublishResult.fail("视频 URL 为空，无法发布");
        }
        if (!isUserAuthorized(userId)) {
            return PublishResult.fail(
                    "当前用户尚未完成抖音 OAuth 授权。请先在「账号管理」中授权抖音账号。");
        }

        try {
            // Step 1: 上传视频到抖音
            String uploadUrl = apiBase + "/api/douyin/v1/video/upload/";
            HttpHeaders headers = new HttpHeaders();
            headers.set("access-token", getAccessToken(userId));
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 使用 URL 方式上传（open_item_id 模式）
            Map<String, Object> uploadBody = Map.of(
                    "video_url", videoUrl
            );
            HttpEntity<Map<String, Object>> uploadReq = new HttpEntity<>(uploadBody, headers);
            ResponseEntity<Map> uploadResp = restTemplate.exchange(uploadUrl, HttpMethod.POST, uploadReq, Map.class);

            if (!uploadResp.getStatusCode().is2xxSuccessful() || uploadResp.getBody() == null) {
                return PublishResult.fail("视频上传到抖音失败：" + uploadResp.getStatusCode());
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> uploadData = (Map<String, Object>) uploadResp.getBody().get("data");
            if (uploadData == null) {
                return PublishResult.fail("抖音上传响应格式异常");
            }
            String videoId = String.valueOf(uploadData.getOrDefault("video", Map.of()));

            // Step 2: 创建抖音视频
            String createUrl = apiBase + "/api/douyin/v1/video/create/";
            Map<String, Object> createBody = Map.of(
                    "video_id", videoId,
                    "text", title != null ? title : ""
            );
            HttpEntity<Map<String, Object>> createReq = new HttpEntity<>(createBody, headers);
            ResponseEntity<Map> createResp = restTemplate.exchange(createUrl, HttpMethod.POST, createReq, Map.class);

            if (!createResp.getStatusCode().is2xxSuccessful() || createResp.getBody() == null) {
                return PublishResult.fail("创建抖音视频失败：" + createResp.getStatusCode());
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> createData = (Map<String, Object>) createResp.getBody().get("data");
            String itemId = createData != null ? String.valueOf(createData.getOrDefault("item_id", "")) : "";

            log.info("[DouyinPublish] 发布成功: userId={}, itemId={}", userId, itemId);
            return PublishResult.ok(itemId);

        } catch (Exception e) {
            log.error("[DouyinPublish] 发布失败: userId={}, err={}", userId, e.getMessage(), e);
            return PublishResult.fail("发布异常：" + e.getMessage());
        }
    }

    /**
     * 获取用户的抖音 access_token（自动刷新过期 token）。
     */
    private String getAccessToken(Long userId) {
        if (oAuthTokenService == null) return "";
        String token = oAuthTokenService.getValidAccessToken(userId, "douyin");
        return token != null ? token : "";
    }
}
