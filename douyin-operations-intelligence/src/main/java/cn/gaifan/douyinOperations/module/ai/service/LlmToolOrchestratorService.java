package cn.gaifan.douyinOperations.module.ai.service;

import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.tool.LlmRegisteredTool;
import cn.gaifan.douyinOperations.module.ai.tool.LlmToolContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 工具注册 + 多轮 function calling 编排（方舟 / OpenAI 兼容 Chat Completions）
 */
@Service
public class LlmToolOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(LlmToolOrchestratorService.class);
    private static final JsonSchemaFactory JSON_SCHEMA_FACTORY = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);

    private final LlmClient llmClient;
    private final List<LlmRegisteredTool> tools;
    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private ToolRateLimiterService rateLimiterService;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    @Value("${app.ai.llm-tools.max-rounds:8}")
    private int maxRounds;

    @Value("${app.ai.llm-tools.retry-max:2}")
    private int retryMax;

    @Value("${app.ai.llm-tools.retry-delay-ms:500}")
    private long retryDelayMs;

    @Value("${app.ai.llm-tools.result-cache.enabled:true}")
    private boolean toolResultCacheEnabled;

    @Value("${app.ai.llm-tools.result-cache.ttl-seconds:3600}")
    private int toolResultCacheTtlSeconds;

    @Value("${app.ai.llm-tools.result-cache.max-value-chars:200000}")
    private int toolResultCacheMaxChars;

    public LlmToolOrchestratorService(LlmClient llmClient, ObjectMapper objectMapper,
                                      List<LlmRegisteredTool> registeredTools) {
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
        this.tools = registeredTools != null ? registeredTools : List.of();
    }

    public boolean hasRegisteredTools() {
        return !tools.isEmpty();
    }

    public static boolean modelSupportsTools(AiModel model) {
        if (model == null || model.getModelProvider() == null) return false;
        String p = model.getModelProvider().toLowerCase(Locale.ROOT);
        return !"ollama".equals(p);
    }

    /**
     * @param messages 含 system/user/assistant 的文本消息；编排过程中会追加 assistant(tool_calls) 与 tool 消息
     */
    public ToolOrchestrationResult run(AiModel model, List<Map<String, Object>> messages, LlmToolContext ctx) {
        long startNs = System.nanoTime();
        ToolOrchestrationResult result = runInternal(model, messages, ctx);
        recordOrchestrationMetrics(result, System.nanoTime() - startNs);
        return result;
    }

    private ToolOrchestrationResult runInternal(AiModel model, List<Map<String, Object>> messages, LlmToolContext ctx) {
        if (model == null) {
            return new ToolOrchestrationResult(null, 0, false, "model 为空", null);
        }
        if (messages == null || messages.isEmpty()) {
            return new ToolOrchestrationResult(null, 0, false, "messages 为空", null);
        }
        if (!modelSupportsTools(model)) {
            return new ToolOrchestrationResult(null, 0, false, "当前模型 provider 不支持 tools（如 Ollama）", null);
        }
        if (tools.isEmpty()) {
            return new ToolOrchestrationResult(null, 0, false, "未注册任何 LlmRegisteredTool", null);
        }
        String toolsJson;
        try {
            toolsJson = buildToolsJsonArray();
        } catch (Exception e) {
            return new ToolOrchestrationResult(null, 0, false, "构建 tools JSON 失败: " + e.getMessage(), null);
        }

        List<Map<String, Object>> working = deepCopyMessages(messages);
        long totalTokens = 0;
        String modelUsed = model.getModelProvider() + "/" + model.getModelVersion();

        for (int round = 0; round < maxRounds; round++) {
            LlmClient.LlmToolResponse resp = llmClient.chatWithToolsStructured(model, working, toolsJson);
            if (!resp.success()) {
                return new ToolOrchestrationResult(null, totalTokens, false,
                        resp.errorMsg() != null ? resp.errorMsg() : "LLM 调用失败", modelUsed);
            }
            totalTokens += resp.tokensUsed();
            if (resp.toolCallsJson() == null || resp.toolCallsJson().isBlank()) {
                String text = resp.content() != null ? resp.content() : "";
                return new ToolOrchestrationResult(text, totalTokens, true, null, modelUsed);
            }
            JsonNode calls;
            try {
                calls = objectMapper.readTree(resp.toolCallsJson());
            } catch (Exception e) {
                return new ToolOrchestrationResult(null, totalTokens, false, "解析 tool_calls 失败: " + e.getMessage(), modelUsed);
            }
            if (!calls.isArray() || calls.isEmpty()) {
                return new ToolOrchestrationResult(resp.content() != null ? resp.content() : "", totalTokens, true, null, modelUsed);
            }

            List<Object> toolCallsForApi = objectMapper.convertValue(calls, List.class);
            LinkedHashMap<String, Object> asst = new LinkedHashMap<>();
            asst.put("role", "assistant");
            asst.put("content", resp.content() != null ? resp.content() : "");
            asst.put("tool_calls", toolCallsForApi);
            working.add(asst);

            List<JsonNode> orderedCalls = orderToolCallsByPrerequisites(calls);
            for (JsonNode call : orderedCalls) {
                String id = call.path("id").asText("");
                String name = call.path("function").path("name").asText("");
                String args = call.path("function").path("arguments").asText("{}");
                String toolResult = executeRegisteredTool(name, args, ctx);
                LinkedHashMap<String, Object> tr = new LinkedHashMap<>();
                tr.put("role", "tool");
                tr.put("tool_call_id", id);
                tr.put("content", toolResult);
                working.add(tr);
            }
        }
        return new ToolOrchestrationResult(null, totalTokens, false, "超过最大工具轮次 " + maxRounds, modelUsed);
    }

    /**
     * T-1：按各工具声明的 {@link LlmRegisteredTool#prerequisiteToolNames()} 对同轮 tool_calls 做拓扑排序；
     * 成环或无法排序时 WARN 并保留模型原始顺序。
     */
    private List<JsonNode> orderToolCallsByPrerequisites(JsonNode callsArray) {
        int n = callsArray.size();
        Map<String, LlmRegisteredTool> byName = tools.stream()
                .collect(Collectors.toMap(LlmRegisteredTool::name, t -> t, (a, b) -> a));
        List<String> names = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            names.add(callsArray.get(i).path("function").path("name").asText(""));
        }
        Map<Integer, List<Integer>> adj = new HashMap<>();
        int[] indeg = new int[n];
        for (int j = 0; j < n; j++) {
            LlmRegisteredTool tool = byName.get(names.get(j));
            Set<String> pre = tool != null ? tool.prerequisiteToolNames() : Set.of();
            for (String p : pre) {
                for (int i = 0; i < n; i++) {
                    if (i != j && p.equals(names.get(i))) {
                        adj.computeIfAbsent(i, k -> new ArrayList<>()).add(j);
                        indeg[j]++;
                    }
                }
            }
        }
        PriorityQueue<Integer> q = new PriorityQueue<>(Comparator.comparingInt(i -> i));
        for (int i = 0; i < n; i++) {
            if (indeg[i] == 0) {
                q.add(i);
            }
        }
        List<Integer> order = new ArrayList<>(n);
        while (!q.isEmpty()) {
            int u = q.poll();
            order.add(u);
            for (int v : adj.getOrDefault(u, List.of())) {
                indeg[v]--;
                if (indeg[v] == 0) {
                    q.add(v);
                }
            }
        }
        if (order.size() != n) {
            log.warn("T-1: 本轮 tool_calls 静态依赖存在环，保持模型原始执行顺序");
            List<JsonNode> orig = new ArrayList<>(n);
            callsArray.forEach(orig::add);
            return orig;
        }
        List<Integer> natural = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            natural.add(i);
        }
        if (!natural.equals(order)) {
            log.warn("T-1: 已按静态工具依赖重排本轮 tool 执行顺序（模型顺序与拓扑不一致）");
        }
        List<JsonNode> sorted = new ArrayList<>(n);
        for (int idx : order) {
            sorted.add(callsArray.get(idx));
        }
        return sorted;
    }

    private void recordOrchestrationMetrics(ToolOrchestrationResult r, long durationNs) {
        if (meterRegistry == null || r == null) {
            return;
        }
        String outcome = r.success() ? "success" : "error";
        String errorType = "none";
        if (!r.success()) {
            String err = r.errorMsg() != null ? r.errorMsg() : "";
            if (err.contains("超过最大工具轮次")) {
                errorType = "max_rounds";
            } else if (err.contains("解析 tool_calls")) {
                errorType = "parse";
            } else if (err.contains("LLM 调用失败")) {
                errorType = "llm";
            } else {
                errorType = "other";
            }
        }
        meterRegistry.counter("ai.llm_tool.orchestration.total", "outcome", outcome, "error_type", errorType).increment();
        meterRegistry.timer("ai.llm_tool.orchestration.duration", "outcome", outcome, "error_type", errorType)
                .record(durationNs, TimeUnit.NANOSECONDS);
    }

    private void recordToolInvocation(String toolName, String result, long durationMs) {
        if (meterRegistry == null) {
            return;
        }
        String name = toolName != null && !toolName.isBlank() ? toolName : "_invalid_";
        meterRegistry.counter("ai.llm_tool.invocations", "tool", name, "result", result).increment();
        if (durationMs >= 0) {
            meterRegistry.timer("ai.llm_tool.latency", "tool", name, "result", result)
                    .record(durationMs, TimeUnit.MILLISECONDS);
        }
    }

    private String executeRegisteredTool(String name, String argumentsJson, LlmToolContext ctx) {
        if (name == null || name.isBlank()) {
            recordToolInvocation("_invalid_", "empty_name", -1);
            return "{\"error\":\"empty tool name\"}";
        }

        // 限流检查
        if (rateLimiterService != null && !rateLimiterService.tryAcquire(name)) {
            recordToolInvocation(name, "rate_limited", -1);
            log.warn("工具调用被限流 name={}", name);
            try {
                return objectMapper.writeValueAsString(Map.of("error", "rate limited", "tool", name, "retry_after_seconds", 5));
            } catch (Exception e2) {
                return "{\"error\":\"rate limited\"}";
            }
        }

        for (LlmRegisteredTool t : tools) {
            if (name.equals(t.name())) {
                return executeWithRetry(t, name, argumentsJson != null ? argumentsJson : "{}", ctx);
            }
        }
        recordToolInvocation(name, "unknown_tool", -1);
        try {
            return objectMapper.writeValueAsString(Map.of("error", "unknown tool: " + name));
        } catch (Exception e) {
            return "{\"error\":\"unknown tool\"}";
        }
    }

    /**
     * 带重试的工具执行：指数退避，最多 retryMax 次重试，失败后跳过（返回错误 JSON）
     */
    private String executeWithRetry(LlmRegisteredTool tool, String name, String args, LlmToolContext ctx) {
        Exception lastException = null;
        for (int attempt = 0; attempt <= retryMax; attempt++) {
            try {
                long startNs = System.nanoTime();
                String cacheKey = toolResultCacheKey(ctx, name, args);
                if (attempt == 0 && toolResultCacheEnabled && tool.resultCacheable()
                        && stringRedisTemplate != null && cacheKey != null) {
                    String cached = stringRedisTemplate.opsForValue().get(cacheKey);
                    if (cached != null && !cached.isBlank()) {
                        try {
                            validateToolOutput(tool, cached);
                            log.debug("llm tool cache hit name={}", name);
                            long cacheMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);
                            recordToolInvocation(name, "cache_hit", cacheMs);
                            return cached;
                        } catch (Exception ex) {
                            log.warn("llm tool cache evict (schema/invalid) name={}: {}", name, ex.getMessage());
                            stringRedisTemplate.delete(cacheKey);
                        }
                    }
                }

                String toolResult = tool.execute(args, ctx);
                validateToolOutput(tool, toolResult);
                persistToolResultCache(tool, name, cacheKey, toolResult);
                long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);
                String metricResult = attempt > 0 ? "success_after_retry" : "success";
                recordToolInvocation(name, metricResult, elapsedMs);
                if (attempt > 0) {
                    log.info("工具重试成功 name={}, attempt={}, latency={}ms", name, attempt, elapsedMs);
                    if (meterRegistry != null) {
                        meterRegistry.counter("ai.llm_tool.retries_recovered", "tool", name).increment();
                    }
                }
                return toolResult;
            } catch (Exception e) {
                lastException = e;
                log.warn("工具执行失败 name={}, attempt={}/{}: {}", name, attempt + 1, retryMax + 1, e.getMessage());
                if (attempt < retryMax) {
                    try {
                        long delay = retryDelayMs * (1L << attempt); // 指数退避
                        Thread.sleep(Math.min(delay, 5000));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        // 所有重试失败，跳过该工具（返回错误信息给 LLM，不中断编排）
        recordToolInvocation(name, "failure", -1);
        log.error("工具执行最终失败 name={}, 已重试{}次，跳过", name, retryMax);
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "error", lastException != null ? lastException.getMessage() : "tool error",
                    "skipped", true,
                    "retries_exhausted", retryMax));
        } catch (Exception e2) {
            return "{\"error\":\"tool execution failed after retries\",\"skipped\":true}";
        }
    }

    private void persistToolResultCache(LlmRegisteredTool tool, String name, String cacheKey, String toolResult) {
        if (!toolResultCacheEnabled || !tool.resultCacheable() || stringRedisTemplate == null
                || cacheKey == null || toolResult == null) {
            return;
        }
        int len = toolResult.length();
        if (len > toolResultCacheMaxChars) {
            log.debug("llm tool cache skip (too large) name={}, chars={}", name, len);
            return;
        }
        int ttl = tool.resultCacheTtlSeconds() > 0 ? tool.resultCacheTtlSeconds() : toolResultCacheTtlSeconds;
        if (ttl <= 0) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue().set(cacheKey, toolResult, Duration.ofSeconds(ttl));
        } catch (Exception e) {
            log.warn("llm tool cache set failed name={}: {}", name, e.getMessage());
        }
    }

    /**
     * 稳定缓存键：用户 + 工具名 + 参数 JSON（对象键递归排序）
     */
    private String toolResultCacheKey(LlmToolContext ctx, String name, String argsJson) {
        try {
            long uid = ctx != null && ctx.userId() != null ? ctx.userId() : 0L;
            String canon = canonicalArgsJson(argsJson);
            String payload = uid + "|" + name + "|" + canon;
            return "ai:llm-tool:result:v1:" + sha256Hex(payload);
        } catch (Exception e) {
            log.debug("llm tool cache key skip: {}", e.getMessage());
            return null;
        }
    }

    private String canonicalArgsJson(String argsJson) throws Exception {
        if (argsJson == null || argsJson.isBlank()) {
            return "{}";
        }
        JsonNode tree = objectMapper.readTree(argsJson);
        JsonNode sorted = sortJsonKeysDeep(tree);
        return objectMapper.writeValueAsString(sorted);
    }

    private JsonNode sortJsonKeysDeep(JsonNode node) {
        if (node == null || !node.isObject()) {
            if (node != null && node.isArray()) {
                ArrayNode out = objectMapper.createArrayNode();
                for (JsonNode item : node) {
                    out.add(sortJsonKeysDeep(item));
                }
                return out;
            }
            return node;
        }
        TreeMap<String, JsonNode> map = new TreeMap<>();
        for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> e = it.next();
            map.put(e.getKey(), sortJsonKeysDeep(e.getValue()));
        }
        ObjectNode out = objectMapper.createObjectNode();
        for (Map.Entry<String, JsonNode> e : map.entrySet()) {
            out.set(e.getKey(), e.getValue());
        }
        return out;
    }

    private static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format(Locale.ROOT, "%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256", e);
        }
    }

    private void validateToolOutput(LlmRegisteredTool tool, String resultJson) throws Exception {
        String schemaStr = tool.outputJsonSchema();
        if (schemaStr == null || schemaStr.isBlank()) {
            return;
        }
        JsonNode data = objectMapper.readTree(resultJson != null && !resultJson.isBlank() ? resultJson : "{}");
        JsonSchema schema = JSON_SCHEMA_FACTORY.getSchema(objectMapper.readTree(schemaStr));
        Set<ValidationMessage> errors = schema.validate(data);
        if (!errors.isEmpty()) {
            throw new IllegalStateException("工具输出 JSON Schema 校验失败: " + errors.iterator().next().getMessage());
        }
    }

    private String buildToolsJsonArray() throws Exception {
        ArrayNode arr = objectMapper.createArrayNode();
        for (LlmRegisteredTool t : tools) {
            ObjectNode tool = objectMapper.createObjectNode();
            tool.put("type", "function");
            ObjectNode fn = objectMapper.createObjectNode();
            fn.put("name", t.name());
            fn.put("description", t.description());
            fn.set("parameters", objectMapper.readTree(t.parametersJsonSchema()));
            tool.set("function", fn);
            arr.add(tool);
        }
        return objectMapper.writeValueAsString(arr);
    }

    private static List<Map<String, Object>> deepCopyMessages(List<Map<String, Object>> messages) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> m : messages) {
            if (m == null) continue;
            out.add(new LinkedHashMap<>(m));
        }
        return out;
    }

    public String toolNamesSummary() {
        return tools.stream().map(LlmRegisteredTool::name).collect(Collectors.joining(", "));
    }

    public record ToolOrchestrationResult(
            String finalContent,
            long totalTokens,
            boolean success,
            String errorMsg,
            String modelUsed
    ) {}
}
