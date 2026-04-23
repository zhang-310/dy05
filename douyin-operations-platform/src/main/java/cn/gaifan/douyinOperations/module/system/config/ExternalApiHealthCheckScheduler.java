package cn.gaifan.douyinOperations.module.system.config;

import cn.gaifan.douyinOperations.module.system.entity.ExternalApiConfig;
import cn.gaifan.douyinOperations.module.system.service.ExternalApiConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * 外部 API 供应商健康检查定时任务
 * 每 5 分钟对所有已启用的供应商执行 HTTP 探活，更新 health_status / avg_latency_ms / success_rate_pct
 */
@Component
public class ExternalApiHealthCheckScheduler {

    private static final Logger log = LoggerFactory.getLogger(ExternalApiHealthCheckScheduler.class);
    private static final int CONNECT_TIMEOUT_SECONDS = 10;
    private static final int REQUEST_TIMEOUT_SECONDS = 15;

    @Resource
    private ExternalApiConfigService externalApiConfigService;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS))
            .build();

    @Scheduled(cron = "${app.system.external-api-health-check.cron:0 */5 * * * ?}")
    public void checkAll() {
        List<ExternalApiConfig> configs = externalApiConfigService.getAllEnabled();
        if (configs.isEmpty()) return;

        log.debug("开始外部 API 健康检查，共 {} 个供应商", configs.size());
        int healthy = 0, degraded = 0, down = 0;

        for (ExternalApiConfig config : configs) {
            try {
                checkOne(config);
                healthy++;
            } catch (Exception e) {
                log.warn("健康检查异常: provider={}, error={}", config.getProviderCode(), e.getMessage());
                down++;
            }
        }

        log.info("外部 API 健康检查完成: healthy={}, degraded={}, down={}, total={}",
                healthy, degraded, down, configs.size());
    }

    private void checkOne(ExternalApiConfig config) {
        String baseUrl = config.getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            externalApiConfigService.updateHealthStatus(config.getProviderCode(), "unknown", null, null);
            return;
        }

        long start = System.currentTimeMillis();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl))
                    .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            int latencyMs = (int) (System.currentTimeMillis() - start);
            int statusCode = response.statusCode();

            String status;
            float successRate;
            if (statusCode >= 200 && statusCode < 400) {
                status = "healthy";
                successRate = 100.0f;
            } else if (statusCode >= 400 && statusCode < 500) {
                status = "degraded";
                successRate = 50.0f;
            } else {
                status = "down";
                successRate = 0.0f;
            }

            externalApiConfigService.updateHealthStatus(config.getProviderCode(), status, latencyMs, successRate);
            log.debug("健康检查: provider={}, status={}, latency={}ms, httpStatus={}",
                    config.getProviderCode(), status, latencyMs, statusCode);

        } catch (java.net.http.HttpTimeoutException e) {
            int latencyMs = (int) (System.currentTimeMillis() - start);
            externalApiConfigService.updateHealthStatus(config.getProviderCode(), "down", latencyMs, 0.0f);
            log.warn("健康检查超时: provider={}, latency={}ms", config.getProviderCode(), latencyMs);

        } catch (Exception e) {
            int latencyMs = (int) (System.currentTimeMillis() - start);
            externalApiConfigService.updateHealthStatus(config.getProviderCode(), "down", latencyMs, 0.0f);
            log.warn("健康检查失败: provider={}, error={}", config.getProviderCode(), e.getMessage());
        }
    }
}
