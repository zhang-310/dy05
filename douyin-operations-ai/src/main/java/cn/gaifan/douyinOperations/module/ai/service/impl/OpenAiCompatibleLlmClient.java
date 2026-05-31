package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.config.service.ConfigService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

import java.util.LinkedHashMap;

/**
 * OpenAI 兼容 API 客户端（支持 Ollama / DeepSeek / GLM / Minimax / Qwen / OpenAI 等）
 * API 地址与 Key 优先从后台「配置管理」读取，其次环境变量/yml 兜底
 */
@Component
public class OpenAiCompatibleLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleLlmClient.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired(required = false)
    private ConfigService configService;

    @Value("${app.ai.ollama-url:http://localhost:11434}")
    private String ollamaUrlDefault;

    @Value("${app.ai.openai-url:https://api.openai.com}")
    private String openaiUrlDefault;

    @Value("${app.ai.deepseek-url:https://api.deepseek.com}")
    private String deepseekUrlDefault;

    @Value("${DEEPSEEK_API_KEY:}")
    private String deepseekApiKeyEnv;

    @Value("${app.ai.timeout:30000}")
    private int timeout;

    @Value("${app.ai.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${app.ai.retry.429-backoff-seconds:60,90,120}")
    private String backoff429Seconds;

    private final RestTemplate restTemplate = createRestTemplate();

    private static RestTemplate createRestTemplate() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15000);
        factory.setReadTimeout(300000); // 5min for evolve
        return new RestTemplate(factory);
    }

    private String configOr(String key, String fallback) {
        if (configService == null) return fallback;
        String v = configService.getRawValueByKey(key);
        return (v != null && !v.isBlank()) ? v.trim() : fallback;
    }

    @Override
    public LlmResponse chat(AiModel model, String system, String prompt) {
        return chatWithImage(model, system, prompt, null);
    }

    @Override
    public LlmResponse chatWithImage(AiModel model, String system, String prompt, List<String> imageUrls) {
        Objects.requireNonNull(model, "AiModel 不能为空，请先通过后台模型配置或 LiveAiModelResolver 解析默认模型");
        String baseUrl = resolveBaseUrlForModel(model);
        String provider = model.getModelProvider() != null ? model.getModelProvider().toLowerCase() : "";
        String apiPath = isOllama(provider) ? "/api/chat"
                : (baseUrl.contains("/api/paas/v") ? "/chat/completions" : "/v1/chat/completions");
        String url = baseUrl + apiPath;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (!isOllama(model.getModelProvider())) {
            String apiKey = resolveApiKey(model);
            if (apiKey != null && !apiKey.isBlank()) {
                headers.setBearerAuth(apiKey);
            }
        }

        List<Map<String, Object>> messages = buildMessages(system, prompt, imageUrls);
        Map<String, Object> body;
        if (isOllama(model.getModelProvider())) {
            body = Map.of(
                "model", model.getModelVersion(),
                "messages", messages,
                "stream", false,
                "options", Map.of(
                    "temperature", model.getTemperature().doubleValue(),
                    "num_predict", model.getMaxTokens()
                )
            );
        } else {
            body = Map.of(
                "model", model.getModelVersion(),
                "messages", messages,
                "max_tokens", model.getMaxTokens(),
                "temperature", model.getTemperature().doubleValue(),
                "stream", false
            );
        }

        String[] backoffArr = backoff429Seconds.split(",");
        int attempt = 0;
        while (true) {
            try {
                HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    return parseResponse(response.getBody(), model.getModelProvider());
                }
                return new LlmResponse(null, 0, false, "HTTP " + response.getStatusCode());
            } catch (HttpStatusCodeException e) {
                int code = e.getStatusCode().value();
                if (code == 429 && attempt < backoffArr.length) {
                    int waitSec = Integer.parseInt(backoffArr[attempt].trim());
                    log.warn("LLM 429 限流，{}s 后重试 (attempt {})", waitSec, attempt + 1);
                    sleep(waitSec * 1000L);
                    attempt++;
                } else if (code >= 500 && attempt < 2) {
                    long waitMs = (long) (5000 * Math.pow(2, attempt));
                    log.warn("LLM 5xx 错误，{}ms 后重试 (attempt {})", waitMs, attempt + 1);
                    sleep(waitMs);
                    attempt++;
                } else {
                    log.error("LLM 调用失败: provider={}, model={}, status={}", model.getModelProvider(), model.getModelVersion(), code, e);
                    return new LlmResponse(null, 0, false, "HTTP " + code + ": " + e.getMessage());
                }
            } catch (ResourceAccessException e) {
                if (attempt < 2) {
                    long waitMs = (long) (3000 * Math.pow(2, attempt));
                    log.warn("LLM 超时/网络错误，{}ms 后重试: {}", waitMs, e.getMessage());
                    sleep(waitMs);
                    attempt++;
                } else {
                    log.error("LLM 调用失败: provider={}, model={}", model.getModelProvider(), model.getModelVersion(), e);
                    return new LlmResponse(null, 0, false, e.getMessage());
                }
            } catch (Exception e) {
                log.error("LLM 调用失败: provider={}, model={}", model.getModelProvider(), model.getModelVersion(), e);
                return new LlmResponse(null, 0, false, e.getMessage());
            }
        }
    }

    @Override
    public LlmToolResponse chatWithToolsStructured(AiModel model, List<Map<String, Object>> messages, String toolsJson) {
        if (model == null) {
            return new LlmToolResponse(null, 0, false, "model 为空", null, null);
        }
        if (isOllama(model.getModelProvider())) {
            return new LlmToolResponse(null, 0, false, "ollama 暂不支持 tools 调用", null, null);
        }
        if (messages == null || messages.isEmpty()) {
            return new LlmToolResponse(null, 0, false, "messages 为空", null, null);
        }
        if (toolsJson == null || toolsJson.isBlank()) {
            return new LlmToolResponse(null, 0, false, "tools 为空", null, null);
        }

        String baseUrl = resolveBaseUrlForModel(model);
        String apiPath = baseUrl.contains("/api/paas/v") ? "/chat/completions" : "/v1/chat/completions";
        String url = baseUrl + apiPath;

        HttpHeaders headers = buildHeaders(model);
        String[] backoffArr = backoff429Seconds.split(",");
        int attempt = 0;

        try {
            JsonNode toolsNode = objectMapper.readTree(toolsJson);
            if (!toolsNode.isArray()) {
                return new LlmToolResponse(null, 0, false, "toolsJson 不是数组", null, null);
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> normalizedMessages =
                    objectMapper.convertValue(messages, new TypeReference<List<Map<String, Object>>>() {});

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model.getModelVersion());
            body.put("messages", normalizedMessages);
            body.put("tools", objectMapper.convertValue(toolsNode, List.class));
            body.put("tool_choice", "auto");
            body.put("stream", false);
            if (model.getMaxTokens() != null) {
                body.put("max_tokens", model.getMaxTokens());
            }
            if (model.getTemperature() != null) {
                body.put("temperature", model.getTemperature().doubleValue());
            }

            while (true) {
                try {
                    HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
                    ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

                    if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                        return parseToolResponse(response.getBody());
                    }
                    return new LlmToolResponse(null, 0, false, "HTTP " + response.getStatusCode(), null, null);
                } catch (HttpStatusCodeException e) {
                    int code = e.getStatusCode().value();
                    if (code == 429 && attempt < backoffArr.length) {
                        int waitSec = Integer.parseInt(backoffArr[attempt].trim());
                        log.warn("LLM tools 429 限流，{}s 后重试 (attempt {})", waitSec, attempt + 1);
                        sleep(waitSec * 1000L);
                        attempt++;
                    } else if (code >= 500 && attempt < 2) {
                        long waitMs = (long) (5000 * Math.pow(2, attempt));
                        log.warn("LLM tools 5xx 错误，{}ms 后重试 (attempt {})", waitMs, attempt + 1);
                        sleep(waitMs);
                        attempt++;
                    } else {
                        log.error("LLM tools 调用失败: provider={}, model={}, status={}",
                                model.getModelProvider(), model.getModelVersion(), code, e);
                        return new LlmToolResponse(null, 0, false, "HTTP " + code + ": " + e.getMessage(), null, null);
                    }
                } catch (ResourceAccessException e) {
                    if (attempt < 2) {
                        long waitMs = (long) (3000 * Math.pow(2, attempt));
                        log.warn("LLM tools 超时/网络错误，{}ms 后重试: {}", waitMs, e.getMessage());
                        sleep(waitMs);
                        attempt++;
                    } else {
                        log.error("LLM tools 调用失败: provider={}, model={}",
                                model.getModelProvider(), model.getModelVersion(), e);
                        return new LlmToolResponse(null, 0, false, e.getMessage(), null, null);
                    }
                }
            }
        } catch (Exception e) {
            log.error("LLM tools 调用失败: provider={}, model={}",
                    model.getModelProvider(), model.getModelVersion(), e);
            return new LlmToolResponse(null, 0, false, e.getMessage(), null, null);
        }
    }

    /** 构建 messages：无图时纯文本，有图时 content 为数组（text + image_url） */
    private List<Map<String, Object>> buildMessages(String system, String prompt, List<String> imageUrls) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", system != null ? system : ""));

        if (imageUrls != null && !imageUrls.isEmpty()) {
            List<Map<String, Object>> content = new ArrayList<>();
            content.add(Map.of("type", "text", "text", prompt != null ? prompt : ""));
            for (String imgUrl : imageUrls) {
                if (imgUrl != null && !imgUrl.isBlank()) {
                    content.add(Map.of("type", "image_url", "image_url", Map.of("url", imgUrl)));
                }
            }
            messages.add(Map.of("role", "user", "content", content));
        } else {
            messages.add(Map.of("role", "user", "content", prompt != null ? prompt : ""));
        }
        return messages;
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted during backoff", ie);
        }
    }

    private HttpHeaders buildHeaders(AiModel model) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (!isOllama(model.getModelProvider())) {
            String apiKey = resolveApiKey(model);
            if (apiKey != null && !apiKey.isBlank()) {
                headers.setBearerAuth(apiKey);
            }
        }
        return headers;
    }

    private LlmResponse parseResponse(String responseBody, String provider) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            if (isOllama(provider)) {
                String content = root.path("message").path("content").asText("");
                long tokens = root.path("eval_count").asLong(0);
                return new LlmResponse(content, tokens, true, null);
            }

            // OpenAI 兼容格式
            JsonNode message = root.path("choices").path(0).path("message");
            String content = extractContentText(message.path("content"));
            long tokens = extractTotalTokens(root);
            return new LlmResponse(content, tokens, true, null);
        } catch (Exception e) {
            log.error("解析 LLM 响应失败", e);
            return new LlmResponse(null, 0, false, "响应解析失败: " + e.getMessage());
        }
    }

    private LlmToolResponse parseToolResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode message = root.path("choices").path(0).path("message");
            String content = extractContentText(message.path("content"));
            long tokens = extractTotalTokens(root);

            JsonNode toolCalls = message.path("tool_calls");
            String toolCallsJson = null;
            if (toolCalls.isArray() && !toolCalls.isEmpty()) {
                toolCallsJson = toolCalls.toString();
            } else {
                toolCallsJson = wrapLegacyFunctionCall(message.path("function_call"));
            }

            return new LlmToolResponse(content, tokens, true, null, toolCallsJson, responseBody);
        } catch (Exception e) {
            log.error("解析 LLM tools 响应失败", e);
            return new LlmToolResponse(null, 0, false, "响应解析失败: " + e.getMessage(), null, responseBody);
        }
    }

    private long extractTotalTokens(JsonNode root) {
        JsonNode usage = root.path("usage");
        long total = usage.path("total_tokens").asLong(0);
        if (total > 0) {
            return total;
        }
        long input = usage.path("prompt_tokens").asLong(0) + usage.path("input_tokens").asLong(0);
        long output = usage.path("completion_tokens").asLong(0) + usage.path("output_tokens").asLong(0);
        return input + output;
    }

    private String extractContentText(JsonNode contentNode) {
        if (contentNode == null || contentNode.isMissingNode() || contentNode.isNull()) {
            return "";
        }
        if (contentNode.isTextual()) {
            return contentNode.asText("");
        }
        if (contentNode.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode item : contentNode) {
                if (item == null || item.isNull()) {
                    continue;
                }
                if (item.isTextual()) {
                    appendContentPiece(sb, item.asText(""));
                    continue;
                }
                if (item.isObject()) {
                    appendContentPiece(sb, item.path("text").asText(""));
                    appendContentPiece(sb, item.path("content").asText(""));
                }
            }
            return sb.toString().trim();
        }
        return contentNode.toString();
    }

    private void appendContentPiece(StringBuilder sb, String piece) {
        if (piece == null || piece.isBlank()) {
            return;
        }
        if (sb.length() > 0) {
            sb.append('\n');
        }
        sb.append(piece.trim());
    }

    private String wrapLegacyFunctionCall(JsonNode functionCallNode) {
        if (functionCallNode == null || functionCallNode.isMissingNode() || functionCallNode.isNull()) {
            return null;
        }
        String name = functionCallNode.path("name").asText("").trim();
        if (name.isEmpty()) {
            return null;
        }
        String arguments = "{}";
        JsonNode argumentsNode = functionCallNode.get("arguments");
        if (argumentsNode != null && !argumentsNode.isNull()) {
            arguments = argumentsNode.isTextual() ? argumentsNode.asText("{}") : argumentsNode.toString();
        }
        LinkedHashMap<String, Object> wrapped = new LinkedHashMap<>();
        wrapped.put("id", "call_legacy_0");
        wrapped.put("type", "function");
        wrapped.put("function", Map.of("name", name, "arguments", arguments));
        try {
            return objectMapper.writeValueAsString(List.of(wrapped));
        } catch (Exception e) {
            log.debug("legacy function_call 包装失败: {}", e.getMessage());
            return null;
        }
    }

    /** 供管理端展示：与 chat() 使用的 Base URL 一致（含单模型自定义） */
    public String resolveDisplayBaseUrl(AiModel model) {
        return resolveBaseUrlForModel(model);
    }

    /** 供管理端展示：仅按 provider 解析（无模型行时用） */
    public String resolveDisplayBaseUrl(String provider) {
        return resolveBaseUrl(provider);
    }

    /**
     * 实际请求使用的 Base URL：若模型配置了 api_base_url 则优先，否则按 provider 读配置。
     */
    public String resolveBaseUrlForModel(AiModel model) {
        if (model == null) {
            return resolveBaseUrl((String) null);
        }
        String override = model.getApiBaseUrl();
        if (override != null && !override.isBlank()) {
            return trimTrailingSlashes(override.trim());
        }
        return resolveBaseUrl(model.getModelProvider());
    }

    private static String trimTrailingSlashes(String u) {
        String s = u;
        while (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    private String resolveBaseUrl(String provider) {
        if (provider == null) return ollamaUrlDefault;
        return switch (provider.toLowerCase()) {
            case "ollama" -> configOr("ai.ollama.url", ollamaUrlDefault);
            case "openai" -> configOr("ai.openai.api_url", openaiUrlDefault);
            case "anthropic" -> configOr("ai.anthropic.api_url", openaiUrlDefault);
            case "deepseek" -> configOr("ai.deepseek.api_url", deepseekUrlDefault);
            case "glm", "openclaw" -> configOr("ai.glm.api_url", "https://open.bigmodel.cn/api/paas/v4");
            case "minimax" -> configOr("ai.minimax.api_url", "https://api.minimaxi.com/v1");
            case "qwen" -> "https://dashscope.aliyuncs.com/compatible-mode";
            case "580ai" -> configOr("ai.580ai.api_url", "https://cc.580ai.net");
            case "custom" -> openaiUrlDefault;
            default -> openaiUrlDefault;
        };
    }

    private String resolveApiKey(AiModel model) {
        if (model.getApiKey() != null && !model.getApiKey().isBlank()) {
            return model.getApiKey();
        }
        String p = model.getModelProvider();
        if (p == null) return null;
        switch (p.toLowerCase()) {
            case "deepseek" -> {
                String k = configOr("ai.deepseek.api_key", null);
                return (k != null && !k.isBlank()) ? k : deepseekApiKeyEnv;
            }
            case "glm", "openclaw" -> { return configOr("ai.glm.api_key", null); }
            case "minimax" -> { return configOr("ai.minimax.api_key", null); }
            case "580ai" -> { return configOr("ai.580ai.api_key", null); }
            default -> { return null; }
        }
    }

    private boolean isOllama(String provider) {
        return "ollama".equalsIgnoreCase(provider);
    }
}
