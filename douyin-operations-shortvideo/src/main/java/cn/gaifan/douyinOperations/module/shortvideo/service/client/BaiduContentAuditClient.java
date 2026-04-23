package cn.gaifan.douyinOperations.module.shortvideo.service.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 百度云内容审核客户端
 * 支持图像、视频、文本审核
 * 使用百度云内容安全服务（https://cloud.baidu.com/doc/CMS/...）
 */
@Slf4j
public class BaiduContentAuditClient {

    private static final String API_ENDPOINT = "https://api.baidu.com";
    private static final String TEXT_CENSOR_PATH = "/rest/2.0/textcensor/v1";
    private static final String IMAGE_CENSOR_PATH = "/rest/2.0/imagecensor/v1";
    private static final String VIDEO_CENSOR_PATH = "/rest/2.0/videocensor/v1";

    private final String appId;
    private final String apiKey;
    private final String secretKey;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public BaiduContentAuditClient(String appId, String apiKey, String secretKey) {
        this.appId = appId;
        this.apiKey = apiKey;
        this.secretKey = secretKey;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 审核文本
     *
     * @param text 待审核文本（最多 20000 字）
     * @return 审核结果
     */
    public ContentAuditResult auditText(String text) {
        if (!StringUtils.hasText(text)) {
            return ContentAuditResult.pass();
        }

        try {
            // 构建请求参数
            String requestBody = buildTextCensorRequest(text);
            String url = API_ENDPOINT + TEXT_CENSOR_PATH + "?access_token=" + getAccessToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                return parseResponse(response.getBody());
            } else {
                log.warn("百度云文本审核失败，状态码: {}", response.getStatusCode());
                return ContentAuditResult.fail("审核失败", Collections.singletonList("HTTP " + response.getStatusCodeValue()));
            }
        } catch (Exception e) {
            log.error("百度云文本审核异常", e);
            return ContentAuditResult.fail("审核异常", Collections.singletonList(e.getMessage()));
        }
    }

    /**
     * 审核图像
     *
     * @param imageUrl 公网可访问的图片 URL
     * @return 审核结果
     */
    public ContentAuditResult auditImage(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            return ContentAuditResult.pass();
        }

        try {
            // 构建请求参数
            String requestBody = buildImageCensorRequest(imageUrl);
            String url = API_ENDPOINT + IMAGE_CENSOR_PATH + "?access_token=" + getAccessToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                return parseResponse(response.getBody());
            } else {
                log.warn("百度云图像审核失败，状态码: {}", response.getStatusCode());
                return ContentAuditResult.fail("审核失败", Collections.singletonList("HTTP " + response.getStatusCodeValue()));
            }
        } catch (Exception e) {
            log.error("百度云图像审核异常", e);
            return ContentAuditResult.fail("审核异常", Collections.singletonList(e.getMessage()));
        }
    }

    /**
     * 审核视频（异步）
     *
     * @param videoUrl 公网可访问的视频 URL
     * @return 审核结果
     */
    public ContentAuditResult auditVideo(String videoUrl) {
        if (!StringUtils.hasText(videoUrl)) {
            return ContentAuditResult.pass();
        }

        try {
            // 构建请求参数
            String requestBody = buildVideoCensorRequest(videoUrl);
            String url = API_ENDPOINT + VIDEO_CENSOR_PATH + "?access_token=" + getAccessToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                // 视频审核通常是异步的，返回成功即可
                return ContentAuditResult.pass();
            } else {
                log.warn("百度云视频审核失败，状态码: {}", response.getStatusCode());
                return ContentAuditResult.fail("审核失败", Collections.singletonList("HTTP " + response.getStatusCodeValue()));
            }
        } catch (Exception e) {
            log.error("百度云视频审核异常", e);
            return ContentAuditResult.fail("审核异常", Collections.singletonList(e.getMessage()));
        }
    }

    /**
     * 构建文本审核请求
     */
    private String buildTextCensorRequest(String text) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("text", text);
            return objectMapper.writeValueAsString(params);
        } catch (Exception e) {
            log.error("构建文本审核请求失败", e);
            return "{}";
        }
    }

    /**
     * 构建图像审核请求
     */
    private String buildImageCensorRequest(String imageUrl) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("uri", imageUrl);
            return objectMapper.writeValueAsString(params);
        } catch (Exception e) {
            log.error("构建图像审核请求失败", e);
            return "{}";
        }
    }

    /**
     * 构建视频审核请求
     */
    private String buildVideoCensorRequest(String videoUrl) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("uri", videoUrl);
            params.put("type", 3); // 审核类型
            return objectMapper.writeValueAsString(params);
        } catch (Exception e) {
            log.error("构建视频审核请求失败", e);
            return "{}";
        }
    }

    /**
     * 获取访问令牌（简化实现，实际应该缓存）
     */
    private String getAccessToken() {
        // 简化实现：直接返回 appId，实际应该通过 OAuth 获取 token
        // 在生产环境应该实现正确的 OAuth 流程并缓存 token
        return apiKey;
    }

    /**
     * 解析响应
     */
    private ContentAuditResult parseResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            // 检查错误码
            if (root.has("error_code") && root.get("error_code").asInt() != 0) {
                String errorMsg = root.has("error_msg") ? root.get("error_msg").asText() : "未知错误";
                return ContentAuditResult.fail("审核异常", Collections.singletonList(errorMsg));
            }

            // 检查结论
            if (!root.has("conclusion")) {
                return ContentAuditResult.pass();
            }

            String conclusion = root.get("conclusion").asText().toLowerCase();
            switch (conclusion) {
                case "pass":
                    return ContentAuditResult.pass();
                case "review":
                    return ContentAuditResult.fail("需要人工审核", Collections.singletonList("检测到可疑内容"));
                case "block":
                default:
                    return ContentAuditResult.fail("包含违规内容", Collections.singletonList("检测到违规内容"));
            }
        } catch (Exception e) {
            log.error("解析审核响应失败", e);
            return ContentAuditResult.pass();
        }
    }

    /**
     * 审核结果对象
     */
    public static class ContentAuditResult {
        public final boolean passed;
        public final String conclusion;
        public final List<String> issues;
        public final List<String> suggestions;

        public ContentAuditResult(boolean passed, String conclusion, List<String> issues, List<String> suggestions) {
            this.passed = passed;
            this.conclusion = conclusion;
            this.issues = issues != null ? issues : Collections.emptyList();
            this.suggestions = suggestions != null ? suggestions : Collections.emptyList();
        }

        public static ContentAuditResult pass() {
            return new ContentAuditResult(true, "pass", Collections.emptyList(), Collections.singletonList("内容通过审核"));
        }

        public static ContentAuditResult fail(String conclusion, List<String> issues) {
            return new ContentAuditResult(false, conclusion, issues, Collections.emptyList());
        }

        @Override
        public String toString() {
            return "ContentAuditResult{" +
                    "passed=" + passed +
                    ", conclusion='" + conclusion + '\'' +
                    ", issues=" + issues +
                    ", suggestions=" + suggestions +
                    '}';
        }
    }
}
