package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.service.ApiKeyEncryptionService;
import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.service.ContentEffectivenessService;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeBaseService;
import cn.gaifan.douyinOperations.module.ai.service.ModelChatStreamService;
import cn.gaifan.douyinOperations.module.config.service.ConfigService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import cn.gaifan.douyinOperations.common.util.AccumulatingSseOutputStream;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.Locale;

/**
 * 模型对话流式服务：调用 LLM stream API，通过 SseEmitter 推送 status/chunk/done
 */
@Service
public class ModelChatStreamServiceImpl implements ModelChatStreamService {

    private static final Logger log = LoggerFactory.getLogger(ModelChatStreamServiceImpl.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final ExecutorService CHAT_STREAM_EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "model-chat-stream");
        t.setDaemon(true);
        return t;
    });
    private static final ScheduledExecutorService KEEPALIVE_SCHEDULER = Executors.newScheduledThreadPool(2, r -> {
        Thread t = new Thread(r, "sse-keepalive");
        t.setDaemon(true);
        return t;
    });
    private static final Set<String> LLM_PROVIDERS = Set.of(
            "ollama", "deepseek", "openai", "anthropic", "glm", "openclaw", "580ai", "minimax", "qwen", "custom");
    /** OpenAI 兼容 API（Spring AI 调用）；智谱 GLM 需在构建 OpenAiApi 时单独指定 completionsPath=/chat/completions */
    private static final Set<String> OPENAI_COMPATIBLE_PROVIDERS = Set.of(
            "deepseek", "openai", "anthropic", "glm", "openclaw", "580ai", "minimax", "qwen", "custom");
    /** 580ai 等第三方 Claude API 可能返回非标准流式格式，Spring AI 无法解析 content，改用 HttpClient 直接读取更可靠 */
    private static final Set<String> HTTP_CLIENT_STREAM_PROVIDERS = Set.of("580ai");

    @Autowired(required = false)
    private ConfigService configService;

    @Autowired(required = false)
    private ApiKeyEncryptionService apiKeyEncryptionService;

    @Autowired(required = false)
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired(required = false)
    private ContentEffectivenessService contentEffectivenessService;

    @jakarta.annotation.Resource
    private AiModelRepository aiModelRepository;

    @Autowired
    private OpenAiCompatibleLlmClient openAiCompatibleLlmClient;

    @Autowired(required = false)
    private OllamaChatModel ollamaChatModel;

    @Override
    public void streamChat(Long modelId, List<Map<String, String>> messages, SseEmitter emitter) {
        streamChat(modelId, messages, emitter, null);
    }

    @Override
    public void streamChat(Long modelId, List<Map<String, String>> messages, SseEmitter emitter, Consumer<String> onCompleteWithContent) {
        CompletableFuture.runAsync(() -> {
            try {
                // 「连接中」已在 Controller 中立即发送，此处直接查模型
                AiModel model = aiModelRepository.findByIdAndDeleted(modelId, 0)
                        .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "AI 模型不存在"));
                String provider = model.getModelProvider() == null ? "" : model.getModelProvider().toLowerCase();
                if (!LLM_PROVIDERS.contains(provider)) {
                    sendError(emitter, "该模型类型不支持流式对话");
                    return;
                }
                if (messages == null || messages.isEmpty()) {
                    sendError(emitter, "messages 不能为空");
                    return;
                }

                sendStatus(emitter, "思考中");
                log.info("模型流式对话: modelId={}, provider={}, model={}", modelId, provider, model.getModelVersion());

                // Ollama：优先使用 Spring AI Flux 流式，实现真正的逐 token 推送
                if ("ollama".equals(provider) && ollamaChatModel != null) {
                    streamOllamaWithSpringAi(model, messages, emitter, onCompleteWithContent);
                    return;
                }
                // 580ai 等：使用 HttpClient 直接读取流，避免 Spring AI 对非标准响应的解析问题
                if (HTTP_CLIENT_STREAM_PROVIDERS.contains(provider)) {
                    streamWithHttpClientForEmitter(model, messages, emitter, onCompleteWithContent);
                    return;
                }
                // OpenAI 兼容（DeepSeek/OpenAI/GLM/Qwen 等）：使用 Spring AI OpenAiChatModel.stream()
                if (OPENAI_COMPATIBLE_PROVIDERS.contains(provider)) {
                    streamOpenAiCompatibleWithSpringAi(model, messages, emitter, onCompleteWithContent);
                    return;
                }
                List<Map<String, Object>> apiMessages = new ArrayList<>();
                for (Map<String, String> m : messages) {
                    String role = m != null && m.get("role") != null ? m.get("role") : "user";
                    String content = m != null && m.get("content") != null ? m.get("content") : "";
                    apiMessages.add(Map.of("role", role, "content", content));
                }

                String baseUrl = resolveBaseUrlForModel(model);
                String apiPath = httpChatCompletionsPath(provider);
                String url = baseUrl + apiPath;

                String apiKey = resolveApiKey(model);
                HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(java.time.Duration.ofMinutes(5))
                        .header("Content-Type", "application/json");

                if (apiKey != null && !apiKey.isBlank()) {
                    reqBuilder.header("Authorization", "Bearer " + apiKey);
                }

                Map<String, Object> body;
                if ("ollama".equals(provider)) {
                    body = Map.of(
                            "model", model.getModelVersion(),
                            "messages", apiMessages,
                            "stream", true,
                            "options", Map.of(
                                    "temperature", model.getTemperature().doubleValue(),
                                    "num_predict", model.getMaxTokens()
                            )
                    );
                } else {
                    body = Map.of(
                            "model", model.getModelVersion(),
                            "messages", apiMessages,
                            "max_tokens", model.getMaxTokens(),
                            "temperature", model.getTemperature().doubleValue(),
                            "stream", true
                    );
                }

                HttpRequest request = reqBuilder
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                        .build();

                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(java.time.Duration.ofSeconds(15))
                        .build();

                AtomicBoolean keepaliveStopped = new AtomicBoolean(false);
                java.util.concurrent.atomic.AtomicInteger waitTick = new java.util.concurrent.atomic.AtomicInteger(0);
                ScheduledFuture<?> keepalive = KEEPALIVE_SCHEDULER.scheduleAtFixedRate(() -> {
                    if (keepaliveStopped.get()) return;
                    try {
                        sendStatus(emitter, "等待模型中 " + waitTick.incrementAndGet());
                    } catch (Exception e) { log.debug("SSE heartbeat发送失败: {}", e.getMessage()); }
                }, 3, 3, TimeUnit.SECONDS);
                try {
                    log.info("模型流式对话: 正在请求 url={}", url);
                    HttpResponse<java.io.InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
                    keepaliveStopped.set(true);
                    keepalive.cancel(false);
                    log.info("模型流式对话: 已收到响应 status={}", response.statusCode());
                    if (response.statusCode() != 200) {
                        sendError(emitter, "LLM 请求失败: HTTP " + response.statusCode());
                        return;
                    }

                sendStatus(emitter, "生成中");
                    boolean hasContent = false;
                    StringBuilder accumulated = onCompleteWithContent != null ? new StringBuilder() : null;
                    String lastOllamaContent = "";
                    // 小缓冲(512)确保 Ollama 每行到达即处理，减少延迟
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8), 512)) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.isBlank()) continue;
                            String content = extractContent(line, provider);
                            if (content != null) {
                                if ("ollama".equals(provider)) {
                                    String delta = content.startsWith(lastOllamaContent) ? content.substring(lastOllamaContent.length()) : content;
                                    lastOllamaContent = content;
                                    if (!delta.isEmpty()) {
                                        hasContent = true;
                                        if (accumulated != null) accumulated.append(delta);
                                        sendChunk(emitter, delta);
                                    }
                                } else if (!content.isEmpty()) {
                                    hasContent = true;
                                    if (accumulated != null) accumulated.append(content);
                                    sendChunk(emitter, content);
                                }
                            }
                            if ("ollama".equals(provider)) {
                                try {
                                    JsonNode node = objectMapper.readTree(line);
                                    if (node.path("done").asBoolean(false)) break;
                                } catch (Exception ignored) {}
                            }
                        }
                    }
                    if (!hasContent) {
                        sendChunk(emitter, "");
                    }
                    sendDone(emitter);
                    if (onCompleteWithContent != null && accumulated != null) {
                        onCompleteWithContent.accept(accumulated.toString());
                    }
                } finally {
                    keepaliveStopped.set(true);
                    keepalive.cancel(false);
                }
            } catch (BusinessException e) {
                log.warn("模型流式对话业务异常: modelId={}, msg={}", modelId, e.getMessage());
                sendError(emitter, e.getMessage());
            } catch (java.net.ConnectException e) {
                log.error("模型流式对话连接失败(请确认 Ollama 已启动): modelId={}, url={}", modelId, resolveDisplayBaseUrl("ollama") + "/api/chat", e);
                sendError(emitter, "无法连接模型服务，请确认 Ollama 已启动（默认 localhost:11434）");
            } catch (java.net.http.HttpTimeoutException e) {
                log.error("模型流式对话超时: modelId={}", modelId, e);
                sendError(emitter, "请求超时，模型可能正在加载，请稍后重试");
            } catch (Exception e) {
                log.error("模型流式对话失败: modelId={}, error={}", modelId, e.getMessage(), e);
                sendError(emitter, e.getMessage() != null ? e.getMessage() : "请求异常");
            }
        }, CHAT_STREAM_EXECUTOR).exceptionally(ex -> {
            log.error("模型流式对话异步异常: modelId={}", modelId, ex);
            sendError(emitter, ex.getMessage() != null ? ex.getMessage() : "异步执行异常");
            return null;
        });
    }

    /** 580ai 等：HttpClient 直接请求 /v1/chat/completions 流式，逐行解析 content，兼容非标准响应格式 */
    private void streamWithHttpClientForEmitter(AiModel model, List<Map<String, String>> messages, SseEmitter emitter, Consumer<String> onCompleteWithContent) {
        String provider = model.getModelProvider() == null ? "" : model.getModelProvider().toLowerCase();
        List<Map<String, Object>> apiMessages = new ArrayList<>();
        for (Map<String, String> m : messages) {
            String role = m != null && m.get("role") != null ? m.get("role") : "user";
            String content = m != null && m.get("content") != null ? m.get("content") : "";
            apiMessages.add(Map.of("role", role, "content", content));
        }
        String baseUrl = resolveBaseUrlForModel(model);
        if (baseUrl.endsWith("/")) baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        String url = baseUrl + httpChatCompletionsPath(provider);
        String apiKey = resolveApiKey(model);
        if (apiKey == null || apiKey.isBlank()) {
            sendError(emitter, "该模型需要配置 API Key");
            return;
        }
        try {
            Map<String, Object> body = Map.of(
                    "model", model.getModelVersion(),
                    "messages", apiMessages,
                    "max_tokens", model.getMaxTokens(),
                    "temperature", model.getTemperature().doubleValue(),
                    "stream", true
            );
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(java.time.Duration.ofMinutes(5))
                    .header("Content-Type", "application/json")
                    .header("Accept", "text/event-stream")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                    .build();
            HttpClient client = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(15)).build();
            AtomicBoolean keepaliveStopped = new AtomicBoolean(false);
            java.util.concurrent.atomic.AtomicInteger waitTick = new java.util.concurrent.atomic.AtomicInteger(0);
            ScheduledFuture<?> keepalive = KEEPALIVE_SCHEDULER.scheduleAtFixedRate(() -> {
                if (keepaliveStopped.get()) return;
                try { sendStatus(emitter, "等待模型中 " + waitTick.incrementAndGet()); } catch (Exception e) { log.debug("SSE heartbeat发送失败: {}", e.getMessage()); }
            }, 3, 3, TimeUnit.SECONDS);
            try {
                log.info("模型流式对话(HttpClient): url={}, model={}", url, model.getModelVersion());
                HttpResponse<java.io.InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
                keepaliveStopped.set(true);
                keepalive.cancel(false);
                if (response.statusCode() != 200) {
                    sendError(emitter, "LLM 请求失败: HTTP " + response.statusCode());
                    return;
                }
                sendStatus(emitter, "生成中");
                boolean hasContent = false;
                StringBuilder accumulated = onCompleteWithContent != null ? new StringBuilder() : null;
                StringBuilder batch = new StringBuilder();
                final int BATCH_SIZE = 24;
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8), 512)) {
                    String line;
                    int lineCount = 0;
                    while ((line = reader.readLine()) != null) {
                        if (line.isBlank()) continue;
                        lineCount++;
                        if (lineCount <= 3) log.debug("580ai 流式行 {}: {}", lineCount, line.length() > 120 ? line.substring(0, 120) + "..." : line);
                        String content = extractContent(line, provider);
                        if (content != null && !content.isEmpty()) {
                            hasContent = true;
                            if (accumulated != null) accumulated.append(content);
                            batch.append(content);
                            if (batch.length() >= BATCH_SIZE) {
                                sendChunk(emitter, batch.toString());
                                batch.setLength(0);
                            }
                        }
                        if (line.startsWith("data: ") && "[DONE]".equals(line.substring(6).trim())) break;
                    }
                    if (batch.length() > 0) sendChunk(emitter, batch.toString());
                    log.info("580ai 流式结束: lineCount={}, hasContent={}", lineCount, hasContent);
                }
                if (!hasContent) sendChunk(emitter, "");
                sendDone(emitter);
                if (onCompleteWithContent != null && accumulated != null) onCompleteWithContent.accept(accumulated.toString());
            } finally {
                keepaliveStopped.set(true);
                keepalive.cancel(false);
            }
        } catch (Exception e) {
            log.error("580ai/HttpClient 流式对话失败: modelId={}, error={}", model.getId(), e.getMessage(), e);
            sendError(emitter, e.getMessage() != null ? e.getMessage() : "请求异常");
        }
    }

    /**
     * 使用 Spring AI OllamaChatModel.stream() 的 Flux 流式，实现逐 token 推送。
     * 参考 Spring AI 文档：ChatClient.prompt().stream().content() 返回 Flux，由框架处理流式。
     */
    private void streamOllamaWithSpringAi(AiModel model, List<Map<String, String>> messages, SseEmitter emitter, Consumer<String> onCompleteWithContent) {
        List<Message> springMessages = new ArrayList<>();
        for (Map<String, String> m : messages) {
            String role = m != null && m.get("role") != null ? m.get("role") : "user";
            String content = m != null && m.get("content") != null ? m.get("content") : "";
            Message msg = switch (role.toLowerCase()) {
                case "system" -> new SystemMessage(content);
                case "assistant" -> new AssistantMessage(content);
                default -> new UserMessage(content);
            };
            springMessages.add(msg);
        }
        OllamaOptions options = OllamaOptions.builder()
                .withModel(model.getModelVersion())
                .withTemperature(model.getTemperature().doubleValue())
                .withNumPredict(model.getMaxTokens());
        Prompt prompt = new Prompt(springMessages, options);
        sendStatus(emitter, "生成中");
        StringBuilder accumulated = onCompleteWithContent != null ? new StringBuilder() : null;
        boolean[] hasContent = {false};
        Flux<ChatResponse> flux = ollamaChatModel.stream(prompt);
        flux.publishOn(Schedulers.boundedElastic())
                .doOnNext(response -> {
                    if (response.getResult() != null && response.getResult().getOutput() != null) {
                        String text = response.getResult().getOutput().getContent();
                        if (text != null && !text.isEmpty()) {
                            hasContent[0] = true;
                            if (accumulated != null) accumulated.append(text);
                            sendChunk(emitter, text);
                        }
                    }
                })
                .doOnComplete(() -> {
                    if (!hasContent[0]) sendChunk(emitter, "");
                    sendDone(emitter);
                    if (onCompleteWithContent != null && accumulated != null) {
                        onCompleteWithContent.accept(accumulated.toString());
                    }
                })
                .doOnError(e -> {
                    log.warn("Spring AI Ollama 流式异常: modelId={}", model.getId(), e);
                    sendError(emitter, e.getMessage() != null ? e.getMessage() : "流式请求异常");
                })
                .subscribe();
    }

    /**
     * OpenAI 兼容 API（DeepSeek/OpenAI/GLM/Qwen 等）：使用 Spring AI OpenAiChatModel.stream() 实现 Flux 流式。
     */
    private void streamOpenAiCompatibleWithSpringAi(AiModel model, List<Map<String, String>> messages, SseEmitter emitter, Consumer<String> onCompleteWithContent) {
        String baseUrl = resolveBaseUrlForModel(model);
        if (baseUrl.endsWith("/")) baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        String apiKey = resolveApiKey(model);
        if (apiKey == null || apiKey.isBlank()) {
            sendError(emitter, "该模型需要配置 API Key");
            return;
        }
        try {
            String prov = model.getModelProvider() == null ? "" : model.getModelProvider().toLowerCase();
            OpenAiApi api = new OpenAiApi(baseUrl, apiKey);
            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .withModel(model.getModelVersion())
                    .withTemperature(model.getTemperature().doubleValue())
                    .withMaxTokens(model.getMaxTokens())
                    .build();
            OpenAiChatModel chatModel = new OpenAiChatModel(api, options);
            List<Message> springMessages = new ArrayList<>();
            for (Map<String, String> m : messages) {
                String role = m != null && m.get("role") != null ? m.get("role") : "user";
                String content = m != null && m.get("content") != null ? m.get("content") : "";
                Message msg = switch (role.toLowerCase()) {
                    case "system" -> new SystemMessage(content);
                    case "assistant" -> new AssistantMessage(content);
                    default -> new UserMessage(content);
                };
                springMessages.add(msg);
            }
            Prompt prompt = new Prompt(springMessages);
            sendStatus(emitter, "生成中");
            StringBuilder accumulated = onCompleteWithContent != null ? new StringBuilder() : null;
            boolean[] hasContent = {false};
            Flux<ChatResponse> flux = chatModel.stream(prompt);
            flux.publishOn(Schedulers.boundedElastic())
                    .doOnNext(response -> {
                        if (response.getResult() != null && response.getResult().getOutput() != null) {
                            String text = response.getResult().getOutput().getContent();
                            if (text != null && !text.isEmpty()) {
                                hasContent[0] = true;
                                if (accumulated != null) accumulated.append(text);
                                sendChunk(emitter, text);
                            }
                        }
                    })
                    .doOnComplete(() -> {
                        if (!hasContent[0]) sendChunk(emitter, "");
                        sendDone(emitter);
                        if (onCompleteWithContent != null && accumulated != null) {
                            onCompleteWithContent.accept(accumulated.toString());
                        }
                    })
                    .doOnError(e -> {
                        log.warn("Spring AI OpenAI 兼容流式异常: modelId={}", model.getId(), e);
                        sendError(emitter, e.getMessage() != null ? e.getMessage() : "流式请求异常");
                    })
                    .subscribe();
        } catch (Exception e) {
            log.error("创建 OpenAiChatModel 失败: modelId={}, provider={}", model.getId(), model.getModelProvider(), e);
            sendError(emitter, e.getMessage() != null ? e.getMessage() : "模型初始化失败");
        }
    }

    @Override
    public void streamChatToOutputStream(Long modelId, List<Map<String, String>> messages, OutputStream out) {
        streamChatToOutputStream(modelId, messages, out, null, null);
    }

    @Override
    public void streamChatToOutputStream(Long modelId, List<Map<String, String>> messages, OutputStream out, Long kbId, Long userId) {
        List<Map<String, String>> effectiveMessages = messages;
        if (kbId != null && userId != null && knowledgeBaseService != null && contentEffectivenessService != null) {
            effectiveMessages = augmentWithRagContext(messages, kbId, userId);
        }
        doStreamChatToOutputStream(modelId, effectiveMessages, out);
    }

    @Override
    public void streamChatToOutputStream(Long modelId, List<Map<String, String>> messages, OutputStream out,
            Consumer<String> onSuccessContent) {
        if (onSuccessContent == null) {
            doStreamChatToOutputStream(modelId, messages, out);
            return;
        }
        AccumulatingSseOutputStream acc = new AccumulatingSseOutputStream(out);
        doStreamChatToOutputStream(modelId, messages, acc);
        if (!acc.isSseErrorSeen()) {
            String full = acc.getAccumulatedContent();
            if (full != null && !full.isBlank()) {
                onSuccessContent.accept(full.trim());
            }
        }
    }

    /** RAG：取最后一条 user 消息检索，注入上下文并打点 */
    private List<Map<String, String>> augmentWithRagContext(List<Map<String, String>> messages, Long kbId, Long userId) {
        String lastUserContent = null;
        for (int i = messages.size() - 1; i >= 0; i--) {
            Map<String, String> m = messages.get(i);
            if (m != null && "user".equalsIgnoreCase(m.getOrDefault("role", ""))) {
                lastUserContent = m.get("content");
                break;
            }
        }
        if (lastUserContent == null || lastUserContent.isBlank()) return messages;

        try {
            var results = knowledgeBaseService.hybridSearch(kbId, lastUserContent, 8, userId, null, true);
            if (results == null || results.isEmpty()) return messages;

            var docIds = results.stream().map(KnowledgeBaseService.SearchResult::docId).filter(java.util.Objects::nonNull).distinct().toList();
            contentEffectivenessService.recordRetrieval(docIds);
            contentEffectivenessService.recordCitation(docIds);

            StringBuilder ctx = new StringBuilder("参考以下知识库内容回答：\n");
            for (var r : results) {
                String title = r.title() != null ? r.title() : "";
                String content = r.content() != null && r.content().length() > 500 ? r.content().substring(0, 500) + "..." : (r.content() != null ? r.content() : "");
                ctx.append("\n【").append(title).append("】").append(content);
            }

            List<Map<String, String>> augmented = new ArrayList<>();
            augmented.add(Map.of("role", "system", "content", ctx.toString()));
            augmented.addAll(messages);
            return augmented;
        } catch (Exception e) {
            log.warn("RAG 检索失败，降级为普通对话: kbId={}, error={}", kbId, e.getMessage());
            return messages;
        }
    }

    private void doStreamChatToOutputStream(Long modelId, List<Map<String, String>> messages, OutputStream out) {
        try {
            AiModel model = aiModelRepository.findByIdAndDeleted(modelId, 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "AI 模型不存在"));
            String provider = model.getModelProvider() == null ? "" : model.getModelProvider().toLowerCase();
            if (!LLM_PROVIDERS.contains(provider)) {
                writeSse(out, "error", Map.of("error", "该模型类型不支持流式对话"));
                return;
            }
            if (messages == null || messages.isEmpty()) {
                writeSse(out, "error", Map.of("error", "messages 不能为空"));
                return;
            }
            log.info("模型流式对话(OutputStream): modelId={}, provider={}, model={}", modelId, provider, model.getModelVersion());

            if ("ollama".equals(provider) && ollamaChatModel != null) {
                streamWithSpringAi(model, messages, out);
            } else {
                streamWithHttpClient(model, messages, provider, out);
            }
        } catch (BusinessException e) {
            log.warn("模型流式对话业务异常: modelId={}, msg={}", modelId, e.getMessage());
            try {
                writeSse(out, "error", Map.of("error", e.getMessage()));
            } catch (IOException ex) { log.debug("SSE error事件发送失败: {}", ex.getMessage()); }
        } catch (java.net.ConnectException e) {
            log.error("模型流式对话连接失败: modelId={}, url={}", modelId, resolveDisplayBaseUrl("ollama") + "/api/chat", e);
            try {
                writeSse(out, "error", Map.of("error", "无法连接模型服务，请确认 Ollama 已启动"));
            } catch (IOException ignored) {}
        } catch (java.net.http.HttpTimeoutException e) {
            log.error("模型流式对话超时: modelId={}", modelId, e);
            try {
                writeSse(out, "error", Map.of("error", "请求超时，模型可能正在加载"));
            } catch (IOException ignored) {}
        } catch (Exception e) {
            log.error("模型流式对话失败: modelId={}, error={}", modelId, e.getMessage(), e);
            try {
                writeSse(out, "error", Map.of("error", e.getMessage() != null ? e.getMessage() : "请求异常"));
            } catch (IOException ignored) {}
        }
    }

    @Override
    public void writeErrorToStream(OutputStream out, String error) {
        try {
            writeSse(out, "error", Map.of("error", error != null ? error : "请求异常"));
        } catch (IOException e) {
            log.warn("写入 error 失败", e);
        }
    }

    /** 使用 Spring AI OllamaChatModel.stream() 流式输出，实现真正的逐 token 推送 */
    private void streamWithSpringAi(AiModel model, List<Map<String, String>> messages, OutputStream out) throws IOException {
        List<Message> springMessages = new ArrayList<>();
        for (Map<String, String> m : messages) {
            String role = m != null && m.get("role") != null ? m.get("role") : "user";
            String content = m != null && m.get("content") != null ? m.get("content") : "";
            Message msg = switch (role.toLowerCase()) {
                case "system" -> new SystemMessage(content);
                case "assistant" -> new AssistantMessage(content);
                default -> new UserMessage(content);
            };
            springMessages.add(msg);
        }
        OllamaOptions options = OllamaOptions.builder()
                .withModel(model.getModelVersion())
                .withTemperature(model.getTemperature().doubleValue())
                .withNumPredict(model.getMaxTokens());
        Prompt prompt = new Prompt(springMessages, options);
        writeSse(out, "status", Map.of("status", "生成中"));
        boolean[] hasContent = {false};
        Flux<ChatResponse> flux = ollamaChatModel.stream(prompt);
        flux.doOnNext(response -> {
            if (response.getResult() != null && response.getResult().getOutput() != null) {
                String content = response.getResult().getOutput().getContent();
                if (content != null && !content.isEmpty()) {
                    hasContent[0] = true;
                    try {
                        writeSse(out, "chunk", Map.of("content", content));
                    } catch (IOException e) {
                        throw new RuntimeException("写入 SSE chunk 失败", e);
                    }
                }
            }
        }).doOnComplete(() -> {
            try {
                if (!hasContent[0]) {
                    writeSse(out, "chunk", Map.of("content", ""));
                }
                writeDoneEvent(out);
            } catch (IOException e) {
                log.warn("写入 done 失败", e);
            }
        }).doOnError(e -> {
            try {
                writeSse(out, "error", Map.of("error", e.getMessage() != null ? e.getMessage() : "请求异常"));
            } catch (IOException ex) {
                log.warn("写入 error 失败", ex);
            }
        }).blockLast();
    }

    /** 使用 HttpClient 流式输出（非 Ollama 或 Spring AI 不可用时回退） */
    private void streamWithHttpClient(AiModel model, List<Map<String, String>> messages, String provider, OutputStream out) throws IOException, InterruptedException {
        List<Map<String, Object>> apiMessages = new ArrayList<>();
        for (Map<String, String> m : messages) {
            String role = m != null && m.get("role") != null ? m.get("role") : "user";
            String content = m != null && m.get("content") != null ? m.get("content") : "";
            apiMessages.add(Map.of("role", role, "content", content));
        }
        String baseUrl = resolveBaseUrlForModel(model);
        String apiPath = httpChatCompletionsPath(provider);
        String url = baseUrl + apiPath;
        String apiKey = resolveApiKey(model);
        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(java.time.Duration.ofMinutes(5))
                .header("Content-Type", "application/json");
        if (apiKey != null && !apiKey.isBlank()) {
            reqBuilder.header("Authorization", "Bearer " + apiKey);
        }
        Map<String, Object> body;
        if ("ollama".equals(provider)) {
            body = Map.of(
                    "model", model.getModelVersion(),
                    "messages", apiMessages,
                    "stream", true,
                    "options", Map.of(
                            "temperature", model.getTemperature().doubleValue(),
                            "num_predict", model.getMaxTokens()
                    )
            );
        } else {
            body = Map.of(
                    "model", model.getModelVersion(),
                    "messages", apiMessages,
                    "max_tokens", model.getMaxTokens(),
                    "temperature", model.getTemperature().doubleValue(),
                    "stream", true
            );
        }
        HttpRequest request = reqBuilder
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                .build();
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(15))
                .build();
        log.info("模型流式对话: 正在请求 url={}", url);
        HttpResponse<java.io.InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        log.info("模型流式对话: 已收到响应 status={}", response.statusCode());
        if (response.statusCode() != 200) {
            writeSse(out, "error", Map.of("error", "LLM 请求失败: HTTP " + response.statusCode()));
            return;
        }
        writeSse(out, "status", Map.of("status", "生成中"));
        boolean hasContent = false;
        String lastOllamaContent = "";
        // 小缓冲(512)确保 Ollama 每行到达即处理，避免默认 8KB 缓冲导致延迟
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8), 512)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String content = extractContent(line, provider);
                if (content != null) {
                    if ("ollama".equals(provider)) {
                        String delta = content.startsWith(lastOllamaContent) ? content.substring(lastOllamaContent.length()) : content;
                        lastOllamaContent = content;
                        if (!delta.isEmpty()) {
                            hasContent = true;
                            writeSse(out, "chunk", Map.of("content", delta));
                        }
                    } else if (!content.isEmpty()) {
                        hasContent = true;
                        writeSse(out, "chunk", Map.of("content", content));
                    }
                }
                if ("ollama".equals(provider)) {
                    try {
                        JsonNode node = objectMapper.readTree(line);
                        if (node.path("done").asBoolean(false)) break;
                    } catch (Exception ignored) {}
                }
            }
        }
        if (!hasContent) {
            writeSse(out, "chunk", Map.of("content", ""));
        }
        writeDoneEvent(out);
    }

    /** 写入 done 事件；若 out 为 AccumulatingSseOutputStream 则附带完整 content，供前端在 chunk 未到达时回退显示 */
    private void writeDoneEvent(OutputStream out) throws IOException {
        Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("status", "ok");
        if (out instanceof AccumulatingSseOutputStream acc) {
            String content = acc.getAccumulatedContent();
            if (content != null && !content.isBlank()) {
                data.put("content", content);
            }
        }
        writeSse(out, "done", data);
    }

    private void writeSse(OutputStream out, String event, Map<String, ?> data) throws IOException {
        String json = objectMapper.writeValueAsString(data);
        String sse = "event: " + event + "\ndata: " + json + "\n\n";
        out.write(sse.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    private String extractContent(String line, String provider) {
        try {
            if ("ollama".equals(provider)) {
                JsonNode node = objectMapper.readTree(line);
                return node.path("message").path("content").asText("");
            }
            String data = line;
            if (line.startsWith("data: ")) {
                data = line.substring(6).trim();
                if ("[DONE]".equals(data)) return null;
            }
            if (!data.startsWith("{")) return null;
            JsonNode node = objectMapper.readTree(data);
            if (node == null || node.isNull()) return null;
            // OpenAI 兼容格式：choices[0].delta.content
            JsonNode content = node.path("choices").path(0).path("delta").path("content");
            if (!content.isMissingNode()) return content.asText("");
            // Claude/Anthropic 原生格式：content_block_delta.delta.text（type=text_delta）
            if ("content_block_delta".equals(node.path("type").asText(""))) {
                JsonNode delta = node.path("delta");
                if ("text_delta".equals(delta.path("type").asText(""))) {
                    return delta.path("text").asText("");
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * HttpClient 直连时的 chat 路径。智谱 base 为 .../api/paas/v4 时须拼接 /chat/completions，不可再用 /v1/chat/completions。
     */
    private static String httpChatCompletionsPath(String provider) {
        if (provider == null) {
            return "/v1/chat/completions";
        }
        String p = provider.toLowerCase(Locale.ROOT);
        if ("ollama".equals(p)) {
            return "/api/chat";
        }
        if ("glm".equals(p) || "openclaw".equals(p)) {
            return "/chat/completions";
        }
        return "/v1/chat/completions";
    }

    private String resolveBaseUrlForModel(AiModel model) {
        return openAiCompatibleLlmClient.resolveBaseUrlForModel(model);
    }

    private String resolveDisplayBaseUrl(String provider) {
        return openAiCompatibleLlmClient.resolveDisplayBaseUrl(provider);
    }

    private String resolveApiKey(AiModel model) {
        if (model.getApiKey() != null && !model.getApiKey().isBlank()) {
            return apiKeyEncryptionService != null ? apiKeyEncryptionService.decryptForUse(model.getApiKey()) : model.getApiKey();
        }
        String p = model.getModelProvider();
        if (p == null) return null;
        return switch (p.toLowerCase()) {
            case "deepseek" -> configOr("ai.deepseek.api_key", System.getenv("DEEPSEEK_API_KEY"));
            case "openai" -> configOr("ai.openai.api_key", System.getenv("OPENAI_API_KEY"));
            case "anthropic" -> configOr("ai.anthropic.api_key", System.getenv("ANTHROPIC_API_KEY"));
            case "glm", "openclaw" -> configOr("ai.glm.api_key", null);
            case "minimax" -> configOr("ai.minimax.api_key", null);
            case "580ai" -> configOr("ai.580ai.api_key", null);
            default -> null;
        };
    }

    private String configOr(String key, String fallback) {
        if (configService == null) return fallback;
        String v = configService.getRawValueByKey(key);
        return (v != null && !v.isBlank()) ? v.trim() : fallback;
    }

    private void sendStatus(SseEmitter emitter, String status) {
        try {
            emitter.send(SseEmitter.event().name("status").data(Map.of("status", status), MediaType.APPLICATION_JSON));
        } catch (Exception e) {
            log.warn("发送 status 失败", e);
        }
    }

    private void sendChunk(SseEmitter emitter, String chunk) {
        try {
            emitter.send(SseEmitter.event().name("chunk").data(Map.of("content", chunk != null ? chunk : ""), MediaType.APPLICATION_JSON));
        } catch (Exception e) {
            log.warn("发送 chunk 失败", e);
        }
    }

    private void sendDone(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().name("done").data(Map.of("status", "ok"), MediaType.APPLICATION_JSON));
            emitter.complete();
        } catch (Exception e) {
            log.warn("发送 done 失败", e);
        }
    }

    private void sendError(SseEmitter emitter, String msg) {
        try {
            emitter.send(SseEmitter.event().name("error").data(Map.of("error", msg), MediaType.APPLICATION_JSON));
            emitter.complete();
        } catch (Exception e) {
            log.warn("发送 error 失败", e);
        }
    }
}

