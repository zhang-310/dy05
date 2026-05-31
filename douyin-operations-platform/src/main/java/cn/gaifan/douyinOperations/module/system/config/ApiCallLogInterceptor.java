package cn.gaifan.douyinOperations.module.system.config;

import cn.gaifan.douyinOperations.common.service.ApiCallLogService;
import cn.gaifan.douyinOperations.module.system.service.SystemService;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * P1-10: 外部 API 调用日志拦截器（使用统一的 ApiCallLogService）
 */
@Component
public class ApiCallLogInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ApiCallLogInterceptor.class);
    private static final int MAX_RESPONSE_LEN = 2000;

    @Resource
    private SystemService systemService;

    @Resource
    private ApiCallLogService apiCallLogService;

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        long startTime = System.currentTimeMillis();
        URI uri = request.getURI();
        String module = resolveModule(uri);
        String apiName = resolveApiName(uri);
        String requestUrl = sanitizeUrl(uri.toString());
        String requestMethod = request.getMethod() != null ? request.getMethod().name() : "GET";

        // P1-10: 使用统一的 ApiCallLogService 脱敏
        String requestParams = apiCallLogService.sanitizeParams(body);
        Integer responseStatus = null;
        String responseBody = null;
        String errorMessage = null;
        int status = 1;

        try {
            ClientHttpResponse response = execution.execute(request, body);
            responseStatus = response.getStatusCode().value();
            // P0-4: 仅读取前 2000 字符，避免大响应体导致内存溢出
            responseBody = readFirst2000Chars(response.getBody());
            // P1-10: 使用统一的 ApiCallLogService 脱敏响应
            responseBody = apiCallLogService.sanitizeResponse(responseBody);
            if (responseStatus < 200 || responseStatus >= 300) {
                status = 0;
            }
            return response;  // 直接返回原始流，不包装
        } catch (Exception e) {
            status = 0;
            errorMessage = e.getClass().getSimpleName() + ": " + (e.getMessage() != null ? e.getMessage() : "");
            throw e;
        } finally {
            long durationMs = System.currentTimeMillis() - startTime;
            Long userId = null;
            try {
                ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attrs != null && attrs.getRequest() != null) {
                    Object uid = attrs.getRequest().getAttribute("userId");
                    if (uid instanceof Long) userId = (Long) uid;
                }
            } catch (Exception ignored) {
            }
            try {
                // P1-10: 使用统一的日志记录入口
                apiCallLogService.logApiCall(module, apiName, requestUrl, requestMethod,
                        requestParams, responseStatus, responseBody,
                        status, errorMessage, durationMs, userId);
                // 实际存储仍由 SystemService 完成
                systemService.saveApiLog(module, apiName, requestUrl, requestMethod,
                        requestParams, responseStatus, responseBody,
                        status, errorMessage, durationMs, userId);
            } catch (Exception ex) {
                log.warn("保存 API 调用日志失败: {}", ex.getMessage());
            }
        }
    }

    private String resolveModule(URI uri) {
        String host = uri.getHost() != null ? uri.getHost().toLowerCase() : "";
        if (host.contains("douyin.com") || host.contains("toutiao.com")) return "douyin";
        if (host.contains("weixin.qq.com") || host.contains("qyapi.weixin.qq.com")) return "wecom";
        if (host.contains("localhost") && uri.getPort() == 11434) return "ai";
        if (host.contains("ollama") || (host.contains("localhost") && uri.getPath() != null && uri.getPath().contains("ollama"))) return "ai";
        if (host.contains("milvus") || host.contains("19530")) return "ai";
        if (host.contains("elasticsearch") || host.contains("9200")) return "ai";
        if (host.contains("openai.com") || host.contains("api.deepseek.com") || host.contains("api.minimax")) return "ai";
        return "unknown";
    }

    private String resolveApiName(URI uri) {
        String path = uri.getPath();
        if (path != null && !path.isBlank()) {
            String p = path.replaceAll("/+", "/");
            return p.length() > 64 ? p.substring(0, 64) : p;
        }
        return uri.getHost() != null ? uri.getHost() : "unknown";
    }

    private String sanitizeUrl(String url) {
        if (url == null || url.isBlank()) {
            return url;
        }
        return url.replaceAll("(?i)([?&](?:key|token|access_token|secret|api_key)=)[^&]*", "$1***");
    }

    /**
     * P0-4: 仅读取响应体前 2000 字符，避免大响应体导致内存溢出
     */
    private String readFirst2000Chars(java.io.InputStream inputStream) {
        try {
            byte[] buffer = new byte[MAX_RESPONSE_LEN];
            int bytesRead = inputStream.read(buffer);
            if (bytesRead > 0) {
                return new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
            }
            return null;
        } catch (Exception e) {
            log.warn("读取响应体失败: {}", e.getMessage());
            return null;
        }
    }
}
