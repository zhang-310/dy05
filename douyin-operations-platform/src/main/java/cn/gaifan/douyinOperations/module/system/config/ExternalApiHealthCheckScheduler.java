package cn.gaifan.douyinOperations.module.system.config;

import cn.gaifan.douyinOperations.module.system.entity.ExternalApiConfig;
import cn.gaifan.douyinOperations.module.system.service.ExternalApiConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 外部 API 供应商健康检查定时任务
 * 每 5 分钟对所有已启用的供应商执行 HTTP 探活，更新 health_status / avg_latency_ms / success_rate_pct
 */
@Component
public class ExternalApiHealthCheckScheduler {

    private static final Logger log = LoggerFactory.getLogger(ExternalApiHealthCheckScheduler.class);
    private static final int CONNECT_TIMEOUT_SECONDS = 10;
    private static final int REQUEST_TIMEOUT_SECONDS = 15;
    private static final Pattern HEALTH_PATH_PATTERN = Pattern.compile("\"healthPath\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern METHOD_PATTERN = Pattern.compile("\"method\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern API_KEY_ENV_PATTERN = Pattern.compile("\"apiKeyEnv\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern SUCCESS_JSON_CODE_PATTERN = Pattern.compile("\"successJsonCode\"\\s*:\\s*(\\d+)");
    private static final Pattern RESPONSE_CODE_PATTERN = Pattern.compile("\"code\"\\s*:\\s*(\\d+)");

    @Resource
    private ExternalApiConfigService externalApiConfigService;

    @Value("${app.system.external-api-health-check.enabled:true}")
    private boolean schedulerEnabled;

    @Value("${tianapi.api-key:}")
    private String tianApiKey;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS))
            .build();

    @Scheduled(cron = "${app.system.external-api-health-check.cron:0 */5 * * * ?}")
    public void checkAll() {
        if (!schedulerEnabled) {
            log.debug("外部 API 健康检查定时任务已禁用，跳过");
            return;
        }
        List<ExternalApiConfig> configs = externalApiConfigService.getAllEnabled();
        if (configs.isEmpty()) return;

        log.debug("开始外部 API 健康检查，共 {} 个供应商", configs.size());
        int healthy = 0, degraded = 0, down = 0;

        for (ExternalApiConfig config : configs) {
            try {
                String status = checkOne(config);
                if ("healthy".equals(status)) {
                    healthy++;
                } else if ("degraded".equals(status) || "unknown".equals(status)) {
                    degraded++;
                } else {
                    down++;
                }
            } catch (Exception e) {
                log.warn("健康检查异常: provider={}, error={}", config.getProviderCode(), e.getMessage());
                down++;
            }
        }

        log.info("外部 API 健康检查完成: healthy={}, degraded={}, down={}, total={}",
                healthy, degraded, down, configs.size());
    }

    public String checkProvider(String providerCode) {
        if (!schedulerEnabled) {
            log.debug("外部 API 健康检查已禁用，手动探测跳过 provider={}", providerCode);
            return "unknown";
        }
        ExternalApiConfig config = externalApiConfigService.getRawByProviderCode(providerCode);
        return checkOne(config);
    }

    private String checkOne(ExternalApiConfig config) {
        String baseUrl = config.getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            externalApiConfigService.updateHealthStatus(config.getProviderCode(), "unknown", null, null);
            return "unknown";
        }

        long start = System.currentTimeMillis();
        try {
            String url = healthUrl(baseUrl, healthPath(config.getExtraConfig()));
            url = appendApiKeyIfConfigured(url, config.getExtraConfig());
            String method = method(config.getExtraConfig());
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
                    .method(method, HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int latencyMs = (int) (System.currentTimeMillis() - start);
            int statusCode = response.statusCode();

            String status = statusFromResponse(config.getExtraConfig(), statusCode, response.body());
            float successRate;
            if ("healthy".equals(status)) {
                status = "healthy";
                successRate = 100.0f;
            } else if ("degraded".equals(status)) {
                status = "degraded";
                successRate = 50.0f;
            } else {
                status = "down";
                successRate = 0.0f;
            }

            externalApiConfigService.updateHealthStatus(config.getProviderCode(), status, latencyMs, successRate);
            externalApiConfigService.logApiCall(config.getProviderCode(), safeLogUrl(url), method, statusCode, latencyMs, null,
                    "ExternalApiHealthCheckScheduler", null);
            log.debug("健康检查: provider={}, status={}, latency={}ms, httpStatus={}, url={}",
                    config.getProviderCode(), status, latencyMs, statusCode, safeLogUrl(url));
            return status;

        } catch (java.net.http.HttpTimeoutException e) {
            int latencyMs = (int) (System.currentTimeMillis() - start);
            externalApiConfigService.updateHealthStatus(config.getProviderCode(), "down", latencyMs, 0.0f);
            externalApiConfigService.logApiCall(config.getProviderCode(), baseUrl, method(config.getExtraConfig()), 0, latencyMs,
                    e.getMessage(), "ExternalApiHealthCheckScheduler", null);
            log.warn("健康检查超时: provider={}, latency={}ms", config.getProviderCode(), latencyMs);
            return "down";

        } catch (Exception e) {
            int latencyMs = (int) (System.currentTimeMillis() - start);
            externalApiConfigService.updateHealthStatus(config.getProviderCode(), "down", latencyMs, 0.0f);
            externalApiConfigService.logApiCall(config.getProviderCode(), baseUrl, method(config.getExtraConfig()), 0, latencyMs,
                    e.getMessage(), "ExternalApiHealthCheckScheduler", null);
            log.warn("健康检查失败: provider={}, error={}", config.getProviderCode(), e.getMessage());
            return "down";
        }
    }

    private static String healthUrl(String baseUrl, String healthPath) {
        String trimmedBase = baseUrl.trim();
        if (!StringUtils.hasText(healthPath)) {
            return trimmedBase;
        }
        if (healthPath.startsWith("http://") || healthPath.startsWith("https://")) {
            return healthPath;
        }
        String base = trimmedBase.endsWith("/") ? trimmedBase.substring(0, trimmedBase.length() - 1) : trimmedBase;
        String path = healthPath.startsWith("/") ? healthPath : "/" + healthPath;
        return base + path;
    }

    private static String healthPath(String extraConfig) {
        return extract(extraConfig, HEALTH_PATH_PATTERN, "");
    }

    private static String method(String extraConfig) {
        String value = extract(extraConfig, METHOD_PATTERN, "HEAD").toUpperCase(Locale.ROOT);
        return "GET".equals(value) || "HEAD".equals(value) ? value : "HEAD";
    }

    private String appendApiKeyIfConfigured(String url, String extraConfig) {
        String envName = extract(extraConfig, API_KEY_ENV_PATTERN, "");
        if (!StringUtils.hasText(envName)) {
            return url;
        }
        String apiKey = apiKeyValue(envName);
        if (!StringUtils.hasText(apiKey)) {
            return url;
        }
        String separator = url.contains("?") ? "&" : "?";
        return url + separator + "key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
    }

    private String apiKeyValue(String envName) {
        if ("TIANAPI_API_KEY".equals(envName) && StringUtils.hasText(tianApiKey)) {
            return tianApiKey;
        }
        String value = System.getenv(envName);
        return StringUtils.hasText(value) ? value : "";
    }

    private static String statusFromResponse(String extraConfig, int statusCode, String body) {
        Integer expectedJsonCode = successJsonCode(extraConfig);
        if (statusCode < 200 || statusCode >= 400) {
            return statusCode >= 400 && statusCode < 500 ? "degraded" : "down";
        }
        if (expectedJsonCode == null) {
            return "healthy";
        }
        Integer actualCode = responseCode(body);
        if (actualCode == null) {
            return "degraded";
        }
        return actualCode.equals(expectedJsonCode) ? "healthy" : "degraded";
    }

    private static Integer successJsonCode(String extraConfig) {
        String value = extract(extraConfig, SUCCESS_JSON_CODE_PATTERN, "");
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer responseCode(String body) {
        if (!StringUtils.hasText(body)) {
            return null;
        }
        Matcher matcher = RESPONSE_CODE_PATTERN.matcher(body);
        if (!matcher.find()) {
            return null;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String safeLogUrl(String url) {
        return url.replaceAll("([?&]key=)[^&]+", "$1****");
    }

    private static String extract(String json, Pattern pattern, String fallback) {
        if (!StringUtils.hasText(json)) {
            return fallback;
        }
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : fallback;
    }
}
