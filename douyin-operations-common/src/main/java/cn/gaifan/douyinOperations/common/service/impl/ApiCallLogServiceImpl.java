package cn.gaifan.douyinOperations.common.service.impl;

import cn.gaifan.douyinOperations.common.service.ApiCallLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * P1-10: API 调用日志记录服务实现
 */
@Service
public class ApiCallLogServiceImpl implements ApiCallLogService {

    private static final Logger log = LoggerFactory.getLogger(ApiCallLogServiceImpl.class);

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "access_token", "accesstoken", "refresh_token", "client_secret", "clientsecret",
            "api_key", "apikey", "authorization", "password", "secret", "token"
    );
    private static final int MAX_PARAMS_LEN = 2000;
    private static final int MAX_RESPONSE_LEN = 2000;

    @Override
    public void logApiCall(String module, String apiName, String requestUrl, String requestMethod,
                           String requestParams, Integer responseStatus, String responseBody,
                           int status, String errorMessage, long durationMs, Long userId) {
        // P1-10: 统一日志记录逻辑
        // 注意：实际存储逻辑由 SystemService.saveApiLog() 实现
        // 此方法仅作为统一入口，未来可扩展为异步队列、批量写入等
        log.debug("API调用日志: module={}, api={}, status={}, duration={}ms",
                module, apiName, status, durationMs);
    }

    @Override
    public String sanitizeParams(byte[] body) {
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

    @Override
    public String sanitizeResponse(String responseBody) {
        if (responseBody == null) return null;
        // P1-10: 响应体脱敏（未来可扩展为 JSON 字段级脱敏）
        return truncate(responseBody, MAX_RESPONSE_LEN);
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
}
