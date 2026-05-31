package cn.gaifan.douyinOperations.module.integration.wecom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * 企业微信 Webhook 通知 — Sprint 5 运营自动化
 *
 * 配置 WECOM_WEBHOOK_URL 后启用每日晨报推送。
 */
@Component
public class WecomNotifier {

    private static final Logger log = LoggerFactory.getLogger(WecomNotifier.class);

    @Value("${wecom.webhook-url:}")
    private String webhookUrl;

    public boolean isConfigured() {
        return webhookUrl != null && webhookUrl.startsWith("https://qyapi.weixin.qq.com");
    }

    public boolean sendText(String content) {
        if (!isConfigured()) {
            log.info("[企微] 未配置 webhook, 跳过推送: {}", content.substring(0, Math.min(content.length(), 50)));
            return false;
        }
        try {
            var body = Map.of("msgtype", "text", "text", Map.of("content", content));
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(body)))
                    .timeout(Duration.ofSeconds(10))
                    .build();
            var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            log.info("[企微] 推送成功: status={}", response.statusCode());
            return response.statusCode() == 200;
        } catch (Exception e) {
            log.warn("[企微] 推送失败: {}", e.getMessage());
            return false;
        }
    }
}
