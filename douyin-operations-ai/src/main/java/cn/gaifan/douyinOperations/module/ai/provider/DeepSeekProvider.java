package cn.gaifan.douyinOperations.module.ai.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * DeepSeek AI Provider — 真实 HTTP 客户端
 *
 * OpenAI-compatible API。配置 DEEPSEEK_API_KEY 后自动启用。
 * 未配置时返回 null，由 MOCK fallback 接管。
 */
@Component
public class DeepSeekProvider {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekProvider.class);

    @Value("${ai.deepseek.api-key:}")
    private String apiKey;

    @Value("${ai.deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${ai.deepseek.model:deepseek-chat}")
    private String model;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && apiKey.length() > 10;
    }

    public String getModel() {
        return model;
    }

    /** @deprecated 使用 {@link #chatWithUsage(String)} */
    public String chat(String prompt) {
        AiProviderResult result = chatWithUsage(prompt);
        return result != null ? result.output() : null;
    }

    public AiProviderResult chatWithUsage(String prompt) {
        if (!isConfigured()) {
            return null;
        }
        try {
            var body = Map.of(
                    "model", model,
                    "messages", List.of(Map.of("role", "user", "content", prompt)),
                    "temperature", 0.7,
                    "max_tokens", 4096
            );
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/v1/chat/completions"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .timeout(Duration.ofSeconds(60))
                    .build();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                @SuppressWarnings("unchecked")
                var map = objectMapper.readValue(response.body(), Map.class);
                var choices = (List<Map<String, Object>>) map.get("choices");
                String content = null;
                if (choices != null && !choices.isEmpty()) {
                    var message = (Map<String, Object>) choices.get(0).get("message");
                    content = (String) message.get("content");
                }
                long promptTokens = 0;
                long completionTokens = 0;
                @SuppressWarnings("unchecked")
                Map<String, Object> usage = (Map<String, Object>) map.get("usage");
                if (usage != null) {
                    promptTokens = toLong(usage.get("prompt_tokens"));
                    completionTokens = toLong(usage.get("completion_tokens"));
                }
                if (promptTokens == 0 && completionTokens == 0 && prompt != null) {
                    promptTokens = Math.max(1, prompt.length() / 4);
                    completionTokens = content != null ? Math.max(1, content.length() / 4) : 0;
                }
                return new AiProviderResult(content, "deepseek", model, promptTokens, completionTokens, false);
            }
            log.warn("DeepSeek API returned status: {}", response.statusCode());
        } catch (Exception e) {
            log.warn("DeepSeek API call failed: {}", e.getMessage());
        }
        return null;
    }

    private static long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return 0L;
    }
}
