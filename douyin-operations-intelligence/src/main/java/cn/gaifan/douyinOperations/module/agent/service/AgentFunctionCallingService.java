package cn.gaifan.douyinOperations.module.agent.service;

import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.agent.skill.Skill;
import cn.gaifan.douyinOperations.module.agent.skill.SkillRegistry;
import cn.gaifan.douyinOperations.module.agent.service.SkillExecutor.ToolCallResult;
import cn.gaifan.douyinOperations.module.agent.util.PromptInjectionDetector;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.*;
import java.util.function.BiConsumer;

/**
 * 智能体 Function Calling 服务
 * <p>
 * 负责 LLM 驱动的工具调用循环：
 * 1. 根据智能体可用工具生成 OpenAI 格式 tools JSON
 * 2. 调用 LLM，解析 tool_calls
 * 3. 执行 Skill，更新对话上下文
 * 4. 继续调用 LLM 直到无工具调用或达到最大轮次（3轮）
 */
@Service
public class AgentFunctionCallingService {

    private static final Logger log = LoggerFactory.getLogger(AgentFunctionCallingService.class);
    private static final int MAX_TOOL_CALL_ROUNDS = 3;

    @Resource
    private LlmClient llmClient;

    @Resource
    private SkillRegistry skillRegistry;

    @Resource
    private SkillExecutor skillExecutor;

    @Resource
    private AiModelRepository aiModelRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== 模型解析 ====================

    /**
     * 获取默认 AI 模型（按 is_default=1 优先，否则取第一个可用的）
     */
    private AiModel resolveDefaultModel() {
        List<AiModel> models = aiModelRepository.findByStatusAndDeleted(1, 0);
        return models.stream()
                .filter(m -> m.getIsDefault() != null && m.getIsDefault() == 1)
                .findFirst()
                .orElse(models.isEmpty() ? null : models.get(0));
    }

    // ==================== 对外接口 ====================

    /**
     * 执行 Function Calling 并返回最终文本回复
     *
     * @param agentId        智能体 ID
     * @param userId         用户 ID
     * @param conversationId  对话 ID
     * @param systemPrompt   系统提示词
     * @param userMessage    用户当前消息
     * @param statusCallback SSE 状态回调（eventName, eventData）
     * @return 最终文本回复（可能是 LLM 生成或降级回复）
     */
    public FunctionCallingResult execute(Long agentId, Long userId, Long conversationId,
            String systemPrompt, String userMessage,
            java.util.function.BiConsumer<String, String> statusCallback) {
        try {
            return doExecute(agentId, userId, conversationId, systemPrompt, userMessage, statusCallback);
        } catch (Exception e) {
            log.error("[AgentFC] Function Calling 执行失败: agentId={}", agentId, e);
            return FunctionCallingResult.fallback("抱歉，发生了错误：" + e.getMessage());
        }
    }

    private FunctionCallingResult doExecute(Long agentId, Long userId, Long conversationId,
            String systemPrompt, String userMessage,
            java.util.function.BiConsumer<String, String> statusCallback) {

        // 1. 获取可用工具定义
        List<ToolDefinition> tools = buildToolDefinitions(agentId);
        if (tools.isEmpty()) {
            // 无可用工具，降级为普通 LLM 对话
            log.info("[AgentFC] 智能体无可用工具，降级为普通对话: agentId={}", agentId);
            return callLlmWithoutTools(systemPrompt, userMessage);
        }

        String toolsJson = buildToolsJson(tools);
        log.info("[AgentFC] 生成工具定义 {} 个: {}", tools.size(),
                tools.stream().map(t -> t.name).toList());

        // 2. 构建初始消息列表（P0-3: 使用结构化 Prompt 包装用户输入）
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt != null ? systemPrompt : "你是智能助手"));
        messages.add(Map.of("role", "user", "content", PromptInjectionDetector.wrapUserInput(userMessage)));

        // 3. LLM 工具调用循环（最多 MAX_TOOL_CALL_ROUNDS 轮）
        long totalTokens = 0;
        List<ToolCallResult> allToolCalls = new ArrayList<>();

        // 解析默认模型（用于工具调用循环）
        AiModel model = resolveDefaultModel();

        for (int round = 1; round <= MAX_TOOL_CALL_ROUNDS; round++) {
            log.info("[AgentFC] 第 {} 轮工具调用", round);

            // 3.1 调用 LLM
            sendStatus(statusCallback, "正在思考... (第" + round + "轮)");
            LlmClient.LlmToolResponse response = llmClient.chatWithToolsStructured(
                    model, messages, toolsJson);

            totalTokens += response.tokensUsed();

            if (!response.success()) {
                log.error("[AgentFC] LLM 调用失败: {}", response.errorMsg());
                // 降级：返回错误提示或尝试普通对话
                return FunctionCallingResult.fallback("AI 服务调用失败: " + response.errorMsg());
            }

            // 3.2 检查是否有 tool_calls
            String toolCallsJson = response.toolCallsJson();
            if (toolCallsJson == null || toolCallsJson.isBlank() || toolCallsJson.equals("null")) {
                // 无工具调用，返回 LLM 文本回复
                log.info("[AgentFC] 第 {} 轮无工具调用，返回文本回复", round);
                return new FunctionCallingResult(response.content(), totalTokens, true, allToolCalls);
            }

            // 3.3 解析 tool_calls
            List<Map<String, Object>> parsedToolCalls;
            try {
                parsedToolCalls = objectMapper.readValue(toolCallsJson,
                        new TypeReference<List<Map<String, Object>>>() {});
            } catch (Exception e) {
                log.error("[AgentFC] 解析 tool_calls JSON 失败: {}", toolCallsJson, e);
                return FunctionCallingResult.fallback("工具调用解析失败: " + e.getMessage());
            }

            if (parsedToolCalls.isEmpty()) {
                return new FunctionCallingResult(response.content(), totalTokens, true, allToolCalls);
            }

            // 3.4 将 LLM 的 assistant 消息加入历史
            Map<String, Object> assistantMessage = new LinkedHashMap<>();
            assistantMessage.put("role", "assistant");
            assistantMessage.put("content", response.content() != null ? response.content() : "");
            assistantMessage.put("tool_calls", parsedToolCalls);
            messages.add(assistantMessage);

            // 3.5 执行每个 tool_call
            for (Map<String, Object> tc : parsedToolCalls) {
                int toolIdx = parsedToolCalls.indexOf(tc);
                String toolResult = executeToolCall(agentId, userId, conversationId,
                        userMessage, tc, statusCallback, round, allToolCalls);
                // 添加 tool result 消息
                String callId = tc.containsKey("id") ? tc.get("id").toString() : "call_" + toolIdx;
                Map<String, Object> resultMsg = new LinkedHashMap<>();
                resultMsg.put("role", "tool");
                resultMsg.put("tool_call_id", callId);
                resultMsg.put("content", toolResult);
                messages.add(resultMsg);
            }
        }

        // 超过最大轮次，发送结束信号
        log.warn("[AgentFC] 达到最大工具调用轮次 {}，强制结束", MAX_TOOL_CALL_ROUNDS);
        sendStatus(statusCallback, "工具调用已达上限，生成最终回复");
        return callLlmFinalResponse(model, messages, toolsJson);
    }

    /**
     * 执行单个工具调用
     */
    private String executeToolCall(Long agentId, Long userId, Long conversationId,
            String userMessage, Map<String, Object> toolCall,
            BiConsumer<String, String> statusCallback,
            int round, List<ToolCallResult> allToolCalls) {

        // 解析 tool_call 结构
        Map<String, Object> function = (Map<String, Object>) toolCall.get("function");
        if (function == null) {
            log.warn("[AgentFC] tool_call 缺少 function 字段: {}", toolCall);
            return "工具调用格式错误：缺少 function 字段";
        }

        String toolName = (String) function.get("name");
        String argumentsJson = function.get("arguments") != null ? function.get("arguments").toString() : "{}";

        if (toolName == null || toolName.isBlank()) {
            return "工具名称为空";
        }

        log.info("[AgentFC] 轮次{} 执行工具: {}", round, toolName);

        // SSE 事件：tool_start
        sendToolEvent(statusCallback, "tool_start", toolName, argumentsJson, null, null);

        try {
            // 解析参数
            Map<String, Object> params = parseArguments(argumentsJson);

            // 使用 SkillExecutor 执行（复用现有基础设施）
            // 注意：我们用 SkillRegistry 直接执行，跳过 matches() 检测
            Optional<Skill> skillOpt = skillRegistry.findByName(toolName);
            if (skillOpt.isEmpty()) {
                String err = "未找到工具: " + toolName;
                sendToolEvent(statusCallback, "tool_end", toolName, argumentsJson, err, null);
                return err;
            }

            Skill skill = skillOpt.get();
            Skill.SkillContext ctx = new Skill.SkillContext(
                    userId, agentId, conversationId, userMessage, params);

            String result = skill.execute(ctx);

            // 截断过长结果（LLM 上下文限制）
            String displayResult = truncateResult(result, 2000);

            // SSE 事件：tool_end（成功）
            sendToolEvent(statusCallback, "tool_end", toolName, argumentsJson, null, displayResult);

            // 记录
            allToolCalls.add(ToolCallRecord.success(toolName, argumentsJson, result, round));

            return result;

        } catch (Exception e) {
            log.error("[AgentFC] 工具 {} 执行失败", toolName, e);
            String err = "执行失败: " + e.getMessage();
            sendToolEvent(statusCallback, "tool_end", toolName, argumentsJson, err, null);
            allToolCalls.add(ToolCallRecord.error(toolName, argumentsJson, err, round));
            return err;
        }
    }

    /**
     * 工具调用达上限后，用已有上下文再调一次 LLM 获取最终回复
     */
    private FunctionCallingResult callLlmFinalResponse(AiModel model, List<Map<String, Object>> messages, String toolsJson) {
        LlmClient.LlmToolResponse response = llmClient.chatWithToolsStructured(model, messages, toolsJson);
        if (response.success()) {
            return new FunctionCallingResult(response.content(), response.tokensUsed(), true,
                    Collections.emptyList());
        }
        return FunctionCallingResult.fallback("工具调用循环结束，但获取最终回复失败: " + response.errorMsg());
    }

    /**
     * 无可用工具时，降级为普通 LLM 对话
     */
    private FunctionCallingResult callLlmWithoutTools(String systemPrompt, String userMessage) {
        AiModel model = resolveDefaultModel();
        String content = "系统: " + (systemPrompt != null ? systemPrompt : "") + "\n\n用户: " + userMessage;
        LlmClient.LlmResponse resp = llmClient.chat(model, systemPrompt, userMessage);
        if (resp.success()) {
            return new FunctionCallingResult(resp.content(), resp.tokensUsed(), true, Collections.emptyList());
        }
        return FunctionCallingResult.fallback("AI 服务不可用: " + resp.errorMsg());
    }

    // ==================== 工具定义构建 ====================

    /**
     * 根据智能体可用工具生成 OpenAI 格式的 tools JSON
     */
    public String buildToolsJson(List<ToolDefinition> tools) {
        List<Map<String, Object>> functions = new ArrayList<>();
        for (ToolDefinition tool : tools) {
            functions.add(tool.toOpenAiFormat());
        }
        try {
            return objectMapper.writeValueAsString(functions);
        } catch (Exception e) {
            log.error("[AgentFC] 序列化 tools JSON 失败", e);
            return "[]";
        }
    }

    /**
     * 从智能体 ID 获取其可用工具定义列表（只包含该智能体配置的工具）
     */
    private List<ToolDefinition> buildToolDefinitions(Long agentId) {
        // 获取智能体的 available_tools
        List<String> availableToolNames = skillExecutor.getAvailableToolNames(agentId);
        if (availableToolNames == null || availableToolNames.isEmpty()) {
            return Collections.emptyList();
        }

        List<ToolDefinition> tools = new ArrayList<>();
        for (String toolName : availableToolNames) {
            Optional<Skill> skillOpt = skillRegistry.findByName(toolName);
            if (skillOpt.isEmpty()) {
                log.warn("[AgentFC] 智能体 {} 的工具 {} 未找到 Skill", agentId, toolName);
                continue;
            }
            Skill skill = skillOpt.get();
            tools.add(new ToolDefinition(
                    skill.getName(),
                    skill.getDescription(),
                    buildParametersForSkill(skill.getName())
            ));
        }
        return tools;
    }

    /**
     * 根据技能名称构建参数 schema
     */
    private Map<String, Object> buildParametersForSkill(String skillName) {
        switch (skillName) {
            case "kb_rag_search":
                return Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "query", Map.of(
                                        "type", "string",
                                        "description", "搜索关键词，如产品名称、概念或问题"
                                )
                        ),
                        "required", List.of("query")
                );
            case "product_search":
                return Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "keyword", Map.of(
                                        "type", "string",
                                        "description", "商品搜索关键词，如商品名称、品牌或品类"
                                ),
                                "category", Map.of(
                                        "type", "string",
                                        "description", "商品品类（可选），如 护肤品、彩妆、面膜等"
                                )
                        ),
                        "required", List.of("keyword")
                );
            case "compliance_check":
                return Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "text", Map.of(
                                        "type", "string",
                                        "description", "待检测的话术文本，需要检查是否包含敏感词、极限词或违规内容"
                                )
                        ),
                        "required", List.of("text")
                );
            case "live_session_query":
                return Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "query", Map.of(
                                        "type", "string",
                                        "description", "场次查询条件，如日期范围、场次主题或数据指标"
                                )
                        ),
                        "required", List.of("query")
                );
            case "script_generate":
                return Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "type", Map.of(
                                        "type", "string",
                                        "description", "话术类型",
                                        "enum", List.of("opening", "product", "promotion", "closing", "general")
                                ),
                                "context", Map.of(
                                        "type", "string",
                                        "description", "话术生成的上下文信息，如产品名称、目标人群、促销信息等"
                                )
                        ),
                        "required", List.of("type")
                );
            default:
                return Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "input", Map.of(
                                        "type", "string",
                                        "description", "输入内容"
                                )
                        ),
                        "required", List.of("input")
                );
        }
    }

    // ==================== 辅助方法 ====================

    private Map<String, Object> parseArguments(String argumentsJson) {
        Map<String, Object> params = new HashMap<>();
        if (argumentsJson == null || argumentsJson.isBlank()) {
            return params;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = objectMapper.readValue(argumentsJson,
                    new TypeReference<Map<String, Object>>() {});
            if (parsed != null) {
                params.putAll(parsed);
            }
        } catch (Exception e) {
            log.warn("[AgentFC] 解析 arguments JSON 失败: {}", argumentsJson);
        }
        return params;
    }

    private String truncateResult(String result, int maxLen) {
        if (result == null) return "";
        if (result.length() <= maxLen) return result;
        return result.substring(0, maxLen) + "...(已截断，长度 " + result.length() + ")";
    }

    private void sendStatus(java.util.function.BiConsumer<String, String> callback, String message) {
        if (callback != null) {
            try {
                callback.accept("status", message);
            } catch (Exception e) { log.debug("SSE status回调失败: {}", e.getMessage()); }
        }
    }

    private void sendToolEvent(java.util.function.BiConsumer<String, String> callback,
            String eventType, String toolName, String argumentsJson,
            String error, String result) {
        if (callback == null) return;
        try {
            String eventData;
            if ("tool_start".equals(eventType)) {
                // Controller 只读取 toolName，argumentsJson 不需要传
                eventData = toolName;
            } else if ("tool_end".equals(eventType)) {
                // Controller 只读取 toolName，结果已通过 SSE 内容传输
                eventData = toolName;
            } else if ("skill_start".equals(eventType)) {
                eventData = toolName + "|" + (argumentsJson != null ? argumentsJson : "");
            } else if ("skill_end".equals(eventType)) {
                String status = (error != null) ? "error" : "success";
                String message = (error != null) ? error : (result != null ? truncateResult(result, 500) : "");
                eventData = toolName + "|" + status + "|" + message;
            } else {
                eventData = toolName;
            }
            callback.accept(eventType, eventData);
        } catch (Exception e) { log.debug("SSE tool事件回调失败: {}", e.getMessage()); }
    }

    // ==================== 内部类 ====================

    /**
     * 工具定义
     */
    public static class ToolDefinition {
        public final String name;
        public final String description;
        public final Map<String, Object> parameters;

        public ToolDefinition(String name, String description, Map<String, Object> parameters) {
            this.name = name;
            this.description = description;
            this.parameters = parameters;
        }

        public Map<String, Object> toOpenAiFormat() {
            Map<String, Object> function = new LinkedHashMap<>();
            function.put("type", "function");
            function.put("function", Map.of(
                    "name", name,
                    "description", description,
                    "parameters", parameters
            ));
            return function;
        }
    }

    /**
     * Function Calling 执行结果
     * toolCalls 类型为 List<ToolCallResult>（与 SkillExecutor.ToolCallResult 统一）
     */
    public static class FunctionCallingResult {
        /** 最终文本回复 */
        public final String content;
        /** 总消耗 tokens */
        public final long tokensUsed;
        /** 是否成功 */
        public final boolean success;
        /** 所有工具调用记录（使用统一的 SkillExecutor.ToolCallResult 类型） */
        public final List<ToolCallResult> toolCalls;
        /** 错误信息（失败时） */
        public final String error;

        public FunctionCallingResult(String content, long tokensUsed, boolean success,
                List<ToolCallResult> toolCalls) {
            this.content = content;
            this.tokensUsed = tokensUsed;
            this.success = success;
            this.toolCalls = toolCalls;
            this.error = null;
        }

        private FunctionCallingResult(String content, long tokensUsed, boolean success,
                List<ToolCallResult> toolCalls, String error) {
            this.content = content;
            this.tokensUsed = tokensUsed;
            this.success = success;
            this.toolCalls = toolCalls;
            this.error = error;
        }

        public static FunctionCallingResult fallback(String message) {
            return new FunctionCallingResult(message, 0, false, Collections.emptyList(), message);
        }
    }

    /**
     * 统一工具调用记录：直接使用 {@link SkillExecutor.ToolCallResult}，保持跨模块类型一致
     */
    private static class ToolCallRecord {
        static ToolCallResult success(String toolName, String arguments, String result, int round) {
            return ToolCallResult.success(toolName, arguments, result, round);
        }
        static ToolCallResult error(String toolName, String arguments, String error, int round) {
            // arguments 字段不用于错误记录，仅传 null
            return ToolCallResult.error(toolName, error, round);
        }
    }
}
