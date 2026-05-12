package cn.gaifan.douyinOperations.module.system.service.impl;

import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.service.ApiCallLogService;
import cn.gaifan.douyinOperations.common.service.ApiKeyEncryptionService;
import cn.gaifan.douyinOperations.module.system.entity.ExternalApiConfig;
import cn.gaifan.douyinOperations.module.system.service.ExternalApiConfigService;
import cn.gaifan.douyinOperations.module.system.service.ExternalApiGateway;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class ExternalApiGatewayImpl implements ExternalApiGateway {

    private static final Logger log = LoggerFactory.getLogger(ExternalApiGatewayImpl.class);

    // P1-7: 响应体大小限制（10MB）
    private static final long MAX_RESPONSE_SIZE = 10 * 1024 * 1024;

    // P1-7: Content-Type 白名单
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/json",
            "application/xml",
            "text/plain",
            "text/xml",
            "text/html"
    );

    // P1-7: 域名白名单（示例，实际应从配置读取）
    private static final Set<String> ALLOWED_DOMAINS = Set.of(
            "api.openai.com",
            "api.anthropic.com",
            "api.douyin.com",
            "open.douyin.com"
    );

    // P1-3: 配置连接池，最大 50 个并发连接
    private final ExecutorService executor = Executors.newFixedThreadPool(50);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))  // P1-3: 连接超时 5 秒
            .executor(executor)  // P1-3: 使用固定线程池限制并发
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private ExternalApiConfigService externalApiConfigService;

    @Resource
    private ApiKeyEncryptionService apiKeyEncryptionService;

    @Resource
    private ApiCallLogService apiCallLogService;

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("外部 API 网关连接池已关闭");
    }

    @Override
    public ResponseEntity<String> call(String providerCode, String endpoint, String method, Map<String, Object> params) {
        long startTime = System.currentTimeMillis();
        ExternalApiConfig config = externalApiConfigService.getByProviderCode(providerCode);
        String decryptedApiKey = apiKeyEncryptionService.decryptForUse(config.getApiKeyEncrypted());

        // P1-7: 验证 baseUrl 域名白名单（防 SSRF）
        String baseUrl = config.getBaseUrl();
        if (!isAllowedDomain(baseUrl)) {
            log.warn("外部 API 域名不在白名单: {}", baseUrl);
            throw new BusinessException(ErrorCode.SYSTEM_BUSY, "外部 API 域名不在白名单");
        }

        String url = baseUrl + endpoint;
        String requestBody = null;
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + decryptedApiKey);

            if ("GET".equalsIgnoreCase(method)) {
                requestBuilder.GET();
            } else {
                requestBody = (params != null && !params.isEmpty())
                        ? objectMapper.writeValueAsString(params)
                        : "{}";
                requestBuilder.method(method.toUpperCase(), HttpRequest.BodyPublishers.ofString(requestBody));
            }

            HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            int latencyMs = (int) (System.currentTimeMillis() - startTime);

            // P1-7: 验证响应体大小（防内存溢出）
            String responseBody = response.body();
            if (responseBody != null && responseBody.length() > MAX_RESPONSE_SIZE) {
                log.warn("外部 API 响应体超过限制: {} bytes", responseBody.length());
                throw new BusinessException(ErrorCode.SYSTEM_BUSY, "响应体大小超过限制");
            }

            // P1-7: 验证 Content-Type（防 XSS）
            String contentType = response.headers().firstValue("Content-Type").orElse("");
            if (!isAllowedContentType(contentType)) {
                log.warn("外部 API Content-Type 不在白名单: {}", contentType);
                throw new BusinessException(ErrorCode.SYSTEM_BUSY, "响应类型不支持");
            }

            // P1-7: HTML 转义响应体（防 XSS）
            String sanitizedBody = sanitizeResponse(responseBody, contentType);

            // P1-10: 使用统一的 ApiCallLogService 记录日志
            String sanitizedParams = apiCallLogService.sanitizeParams(requestBody != null ? requestBody.getBytes() : null);
            String sanitizedResponse = apiCallLogService.sanitizeResponse(sanitizedBody);
            apiCallLogService.logApiCall(
                    "external", endpoint, url, method,
                    sanitizedParams, response.statusCode(), sanitizedResponse,
                    1, null, latencyMs, null);

            externalApiConfigService.logApiCall(
                    providerCode, endpoint, method,
                    response.statusCode(), latencyMs, null,
                    "ExternalApiGateway", null);

            return ResponseEntity.status(response.statusCode()).body(sanitizedBody);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            int latencyMs = (int) (System.currentTimeMillis() - startTime);
            log.error("外部API调用失败: provider={}, endpoint={}", providerCode, endpoint, e);

            // P1-10: 使用统一的 ApiCallLogService 记录失败日志
            apiCallLogService.logApiCall(
                    "external", endpoint, url, method,
                    null, 0, null,
                    0, e.getMessage(), latencyMs, null);

            externalApiConfigService.logApiCall(
                    providerCode, endpoint, method,
                    0, latencyMs, e.getMessage(),
                    "ExternalApiGateway", null);

            try {
                externalApiConfigService.updateHealthStatus(providerCode, "degraded", latencyMs, null);
            } catch (Exception ex) {
                log.warn("更新健康状态失败: provider={}", providerCode, ex);
            }

            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(e.getMessage());
        }
    }

    /**
     * P1-7: 验证域名是否在白名单
     */
    private boolean isAllowedDomain(String baseUrl) {
        try {
            URI uri = URI.create(baseUrl);
            String host = uri.getHost();
            return host != null && ALLOWED_DOMAINS.stream().anyMatch(host::endsWith);
        } catch (Exception e) {
            log.warn("解析 baseUrl 失败: {}", baseUrl, e);
            return false;
        }
    }

    /**
     * P1-7: 验证 Content-Type 是否在白名单
     */
    private boolean isAllowedContentType(String contentType) {
        if (contentType == null || contentType.isEmpty()) {
            return false;
        }
        String baseType = contentType.split(";")[0].trim().toLowerCase();
        return ALLOWED_CONTENT_TYPES.contains(baseType);
    }

    /**
     * P1-7: 对响应体进行安全处理（HTML 转义）
     */
    private String sanitizeResponse(String responseBody, String contentType) {
        if (responseBody == null) {
            return null;
        }
        // 对 HTML 类型响应进行转义，防止 XSS
        if (contentType.toLowerCase().contains("text/html")) {
            return HtmlUtils.htmlEscape(responseBody);
        }
        // JSON/XML 不需要转义
        return responseBody;
    }
}
