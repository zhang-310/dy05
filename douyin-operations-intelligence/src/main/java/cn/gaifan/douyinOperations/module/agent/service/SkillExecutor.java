package cn.gaifan.douyinOperations.module.agent.service;

import cn.gaifan.douyinOperations.module.agent.entity.Agent;
import cn.gaifan.douyinOperations.module.agent.repository.AgentRepository;
import cn.gaifan.douyinOperations.module.agent.skill.Skill;
import cn.gaifan.douyinOperations.module.agent.skill.SkillRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.*;
import java.util.function.BiConsumer;

/**
 * 技能执行器：负责检测用户输入是否匹配技能，并执行匹配的技能
 */
@Service
public class SkillExecutor {

    private static final Logger log = LoggerFactory.getLogger(SkillExecutor.class);

    @Resource
    private SkillRegistry skillRegistry;

    @Resource
    private AgentRepository agentRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 执行技能调用的结果（与 AgentFunctionCallingService 统一的工具调用记录类型）
     */
    public static class ToolCallResult {
        /** 工具名称，如 "kb_rag_search" */
        public String toolName;
        /** 调用的参数（JSON） */
        public String arguments;
        /** 执行结果文本 */
        public String result;
        /** 是否成功 */
        public boolean success;
        /** 错误信息 */
        public String error;
        /** 所在调用轮次（用于多轮工具调用场景） */
        public int round;

        public static ToolCallResult success(String toolName, String arguments, String result) {
            ToolCallResult r = new ToolCallResult();
            r.toolName = toolName;
            r.arguments = arguments;
            r.result = result;
            r.success = true;
            r.round = 0;
            return r;
        }

        public static ToolCallResult success(String toolName, String arguments, String result, int round) {
            ToolCallResult r = new ToolCallResult();
            r.toolName = toolName;
            r.arguments = arguments;
            r.result = result;
            r.success = true;
            r.round = round;
            return r;
        }

        public static ToolCallResult error(String toolName, String error) {
            return error(toolName, error, 0);
        }

        public static ToolCallResult error(String toolName, String error, int round) {
            ToolCallResult r = new ToolCallResult();
            r.toolName = toolName;
            r.success = false;
            r.error = error;
            r.round = round;
            return r;
        }
    }

    /**
     * 检测用户输入是否需要调用技能，并执行
     *
     * @param agentId 智能体 ID
     * @param userId 用户 ID
     * @param conversationId 对话 ID
     * @param userInput 用户输入
     * @return 技能执行结果列表（可能为空）
     */
    public List<ToolCallResult> detectAndExecute(Long agentId, Long userId, Long conversationId, String userInput) {
        return detectAndExecute(agentId, userId, conversationId, userInput, null);
    }

    /**
     * 检测用户输入是否需要调用技能，并执行
     *
     * @param agentId 智能体 ID
     * @param userId 用户 ID
     * @param conversationId 对话 ID
     * @param userInput 用户输入
     * @param statusCallback 可选回调，用于发送技能生命周期事件 (eventName, eventData)
     *                        支持事件: "skill_start" → skillName|description, "skill_end" → skillName|success/error
     * @return 技能执行结果列表（可能为空）
     */
    public List<ToolCallResult> detectAndExecute(Long agentId, Long userId, Long conversationId, String userInput,
            BiConsumer<String, String> statusCallback) {
        List<ToolCallResult> results = new ArrayList<>();

        // 1. 获取智能体的可用工具列表
        Agent agent = agentRepository.findByIdAndDeleted(agentId, 0).orElse(null);
        if (agent == null || agent.getAvailableTools() == null) {
            return results;
        }

        // 2. 解析可用工具列表
        List<String> availableToolNames = parseToolNames(agent.getAvailableTools());
        if (availableToolNames.isEmpty()) {
            return results;
        }

        // 3. 遍历可用工具，检测是否有匹配的技能
        for (String toolName : availableToolNames) {
            Optional<Skill> skillOpt = skillRegistry.findByName(toolName);
            if (skillOpt.isEmpty()) {
                log.warn("[SkillExecutor] Skill not found: {}", toolName);
                continue;
            }

            Skill skill = skillOpt.get();

            // 4. 检测用户输入是否匹配此技能
            if (skill.matches(userInput)) {
                log.info("[SkillExecutor] User input matches skill: {} -> {}", toolName, userInput);

                // 发送 skill_start 事件
                emitStatus(statusCallback, "skill_start", toolName + "|" + skill.getDescription());

                try {
                    // 5. 构建技能上下文
                    Map<String, Object> params = extractParams(userInput, toolName);
                    Skill.SkillContext ctx = new Skill.SkillContext(
                            userId, agentId, conversationId, userInput, params
                    );

                    // 6. 执行技能
                    String skillResult = skill.execute(ctx);

                    // 7. 记录执行结果
                    results.add(ToolCallResult.success(
                            toolName,
                            objectMapper.writeValueAsString(params),
                            skillResult
                    ));

                    log.info("[SkillExecutor] Skill executed successfully: {}", toolName);

                    // 发送 skill_end 事件（成功）
                    emitStatus(statusCallback, "skill_end", toolName + "|success");
                } catch (Exception e) {
                    log.error("[SkillExecutor] Skill execution failed: " + toolName, e);
                    results.add(ToolCallResult.error(toolName, e.getMessage()));

                    // 发送 skill_end 事件（失败）
                    emitStatus(statusCallback, "skill_end", toolName + "|error|" + e.getMessage());
                }
            }
        }

        return results;
    }

    private void emitStatus(BiConsumer<String, String> callback, String eventName, String eventData) {
        if (callback != null) {
            try {
                callback.accept(eventName, eventData);
            } catch (Exception e) {
                log.warn("[SkillExecutor] Failed to emit status: {} -> {}", eventName, eventData, e);
            }
        }
    }

    /**
     * 解析可用工具列表 JSON
     */
    private List<String> parseToolNames(String availableToolsJson) {
        List<String> tools = new ArrayList<>();
        try {
            List<String> parsed = objectMapper.readValue(availableToolsJson, List.class);
            if (parsed != null) {
                tools.addAll(parsed);
            }
        } catch (Exception e) {
            log.warn("[SkillExecutor] Failed to parse available_tools: {}", availableToolsJson);
        }
        return tools;
    }

    /**
     * 获取智能体 ID 对应的可用工具名称列表
     */
    public List<String> getAvailableToolNames(Long agentId) {
        Agent agent = agentRepository.findByIdAndDeleted(agentId, 0).orElse(null);
        if (agent == null || agent.getAvailableTools() == null) {
            return Collections.emptyList();
        }
        return parseToolNames(agent.getAvailableTools());
    }

    /**
     * 从用户输入中提取技能参数
     * 这是一个简化版本，直接将整个用户输入作为查询参数
     * 后续可以改进为从用户输入中提取关键实体
     */
    private Map<String, Object> extractParams(String userInput, String toolName) {
        Map<String, Object> params = new HashMap<>();

        // 根据技能类型提取不同的参数
        switch (toolName) {
            case "kb_rag_search":
            case "product_search":
            case "live_session_query":
                // 将整个输入作为查询关键词
                params.put("query", userInput);
                break;

            case "compliance_check":
                // 将整个输入作为待检测文本
                params.put("text", userInput);
                break;

            case "script_generate":
                // 尝试从输入中提取参数
                params.put("type", detectScriptType(userInput));
                params.put("context", userInput);
                break;

            default:
                params.put("input", userInput);
        }

        return params;
    }

    /**
     * 从用户输入中检测话术类型
     */
    private String detectScriptType(String input) {
        String lower = input.toLowerCase();
        if (lower.contains("开场") || lower.contains("开场白")) {
            return "opening";
        } else if (lower.contains("产品") || lower.contains("介绍")) {
            return "product";
        } else if (lower.contains("促销") || lower.contains("优惠") || lower.contains("活动")) {
            return "promotion";
        } else if (lower.contains("逼单") || lower.contains("成交") || lower.contains("结束")) {
            return "closing";
        }
        return "general";
    }

    /**
     * 格式化工具调用结果为文本
     */
    public String formatToolCallResults(List<ToolCallResult> results) {
        if (results == null || results.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n\n--- 工具调用结果 ---\n");

        for (ToolCallResult result : results) {
            sb.append("【").append(result.toolName).append("】\n");
            if (result.success) {
                sb.append(result.result).append("\n");
            } else {
                sb.append("调用失败: ").append(result.error).append("\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * 将工具调用结果序列化为 JSON（用于存储到数据库）
     */
    public String serializeToolCalls(List<ToolCallResult> results) {
        if (results == null || results.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(results);
        } catch (Exception e) {
            log.error("[SkillExecutor] Failed to serialize tool calls", e);
            return null;
        }
    }
}
