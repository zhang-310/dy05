package cn.gaifan.douyinOperations.module.system.service.impl;

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

import jakarta.annotation.Resource;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Service
public class ExternalApiGatewayImpl implements ExternalApiGateway {

    private static final Logger log = LoggerFactory.getLogger(ExternalApiGatewayImpl.class);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private ExternalApiConfigService externalApiConfigService;

    @Resource
    private ApiKeyEncryptionService apiKeyEncryptionService;

    @Override
    public ResponseEntity<String> call(String providerCode, String endpoint, String method, Map<String, Object> params) {
        long startTime = System.currentTimeMillis();
        ExternalApiConfig config = externalApiConfigService.getByProviderCode(providerCode);
        String decryptedApiKey = apiKeyEncryptionService.decryptForUse(config.getApiKeyEncrypted());

        String url = config.getBaseUrl() + endpoint;
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + decryptedApiKey);

            if ("GET".equalsIgnoreCase(method)) {
                requestBuilder.GET();
            } else {
                String body = (params != null && !params.isEmpty())
                        ? objectMapper.writeValueAsString(params)
                        : "{}";
                requestBuilder.method(method.toUpperCase(), HttpRequest.BodyPublishers.ofString(body));
            }

            HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            int latencyMs = (int) (System.currentTimeMillis() - startTime);

            externalApiConfigService.logApiCall(
                    providerCode, endpoint, method,
                    response.statusCode(), latencyMs, null,
                    "ExternalApiGateway", null);

            return ResponseEntity.status(response.statusCode()).body(response.body());

        } catch (Exception e) {
            int latencyMs = (int) (System.currentTimeMillis() - startTime);
            log.error("外部API调用失败: provider={}, endpoint={}", providerCode, endpoint, e);

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
}
