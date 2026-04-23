package cn.gaifan.douyinOperations.module.system.config;

import cn.gaifan.douyinOperations.module.system.service.SystemService;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import jakarta.annotation.Resource;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 外部 API 调用日志拦截器：自动记录 RestTemplate 请求到 sys_api_call_log
 * 参数脱敏：access_token、client_secret、api_key 等替换为 ***
 */
@Component
public class ApiCallLogInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ApiCallLogInterceptor.class);
    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "access_token", "accesstoken", "refresh_token", "client_secret", "clientsecret",
            "api_key", "apikey", "authorization", "password", "secret", "token"
    );
    private static final int MAX_PARAMS_LEN = 2000;
    private static final int MAX_RESPONSE_LEN = 2000;

    @Resource
    private SystemService systemService;

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        long startTime = System.currentTimeMillis();
        URI uri = request.getURI();
        String module = resolveModule(uri);
        String apiName = resolveApiName(uri);
        String requestUrl = uri.toString();
        String requestMethod = request.getMethod() != null ? request.getMethod().name() : "GET";
        String requestParams = sanitizeParams(body);
        Integer responseStatus = null;
        String responseBody = null;
        String errorMessage = null;
        int status = 1;

        try {
            ClientHttpResponse response = execution.execute(request, body);
            responseStatus = response.getStatusCode().value();
            byte[] respBytes = StreamUtils.copyToByteArray(response.getBody());
            responseBody = truncate(new String(respBytes, StandardCharsets.UTF_8), MAX_RESPONSE_LEN);
            if (responseStatus < 200 || responseStatus >= 300) {
                status = 0;
            }
            return new BufferedClientHttpResponse(response, respBytes);
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

    private String sanitizeParams(byte[] body) {
        if (body == null || body.length == 0) return null;
        String raw = new String(body, StandardCharsets.UTF_8);
        try {
            if (raw.trim().startsWith("{")) {
                return sanitizeJson(raw);
            }
            if (raw.contains("=")) {
                return sanitizeFormData(raw);
            }
        } catch (Exception ignored) {
        }
        return truncate(raw, MAX_PARAMS_LEN);
    }

    private String sanitizeJson(String json) {
        StringBuilder sb = new StringBuilder(json);
        for (String key : SENSITIVE_KEYS) {
            Pattern p = Pattern.compile("(\"" + key + "\"\\s*:\\s*)\"[^\"]*\"", Pattern.CASE_INSENSITIVE);
            sb = new StringBuilder(p.matcher(sb).replaceAll("$1\"***\""));
        }
        return truncate(sb.toString(), MAX_PARAMS_LEN);
    }

    private String sanitizeFormData(String form) {
        StringBuilder sb = new StringBuilder();
        for (String pair : form.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                String k = pair.substring(0, eq).toLowerCase().replace("-", "");
                String v = pair.substring(eq + 1);
                if (SENSITIVE_KEYS.stream().anyMatch(s -> k.contains(s.replace("_", "")))) {
                    v = "***";
                }
                if (sb.length() > 0) sb.append("&");
                sb.append(pair.substring(0, eq + 1)).append(v);
            } else {
                if (sb.length() > 0) sb.append("&");
                sb.append(pair);
            }
        }
        return truncate(sb.toString(), MAX_PARAMS_LEN);
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    /** 包装响应以便重复读取 body */
    private static class BufferedClientHttpResponse implements ClientHttpResponse {
        private final ClientHttpResponse delegate;
        private final byte[] body;

        BufferedClientHttpResponse(ClientHttpResponse delegate, byte[] body) {
            this.delegate = delegate;
            this.body = body;
        }

        @Override
        public HttpStatusCode getStatusCode() throws IOException {
            return delegate.getStatusCode();
        }

        @Override
        public int getRawStatusCode() throws IOException {
            return delegate.getStatusCode().value();
        }

        @Override
        public String getStatusText() throws IOException {
            return delegate.getStatusCode().toString();
        }

        @Override
        public void close() {
            delegate.close();
        }

        @Override
        public java.io.InputStream getBody() {
            return new java.io.ByteArrayInputStream(body);
        }

        @Override
        public org.springframework.http.HttpHeaders getHeaders() {
            return delegate.getHeaders();
        }
    }
}
