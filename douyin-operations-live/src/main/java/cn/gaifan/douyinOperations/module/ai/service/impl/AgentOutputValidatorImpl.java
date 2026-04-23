package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.service.AgentOutputValidator;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.live.repository.LiveProductRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Agent 输出校验与自修复实现
 * <p>
 * 三层校验：
 * 1. 结构校验：输出必须为合法 JSON + 必含指定字段
 * 2. 事实校验：提及商品 ID/名称 → 查 LiveProduct 表确认存在
 * 3. 合规校验：复用合规服务检查违规词
 * <p>
 * 自修复：校验失败 → 将 {original_output, violations[]} 重新喂给同一 Agent → 最多 2 轮
 * 2 轮仍失败 → 返回 {error: "QUALITY_FAILED", fallback: true} + 降级输出
 */
@Slf4j
@Service
public class AgentOutputValidatorImpl implements AgentOutputValidator {

    private static final int MAX_REPAIR_ROUNDS = 2;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 每个 Agent 角色必须包含的 JSON 字段 */
    private static final Map<String, List<String>> REQUIRED_FIELDS = Map.of(
            "ProductAnalyst", List.of("recommendation"),
            "ScriptWriter", List.of("scripts"),
            "ComplianceChecker", List.of("passed"),
            "ScheduleOptimizer", List.of("schedule")
    );

    @Autowired(required = false)
    private LlmClient llmClient;

    @Autowired(required = false)
    private LiveProductRepository liveProductRepository;

    @Autowired(required = false)
    private cn.gaifan.douyinOperations.module.script.service.IndustryComplianceService complianceService;

    @Override
    public String validateAndRepair(String agentRole, String output, String prompt, AiModel model, Long sessionId) {
        if (output == null || output.isBlank()) {
            return buildFallbackOutput(agentRole, "输出为空");
        }

        String current = output;
        for (int round = 0; round < MAX_REPAIR_ROUNDS; round++) {
            List<String> violations = validate(agentRole, current, sessionId);
            if (violations.isEmpty()) {
                if (round > 0) {
                    log.info("[AgentValidator] {} 第{}轮自修复成功", agentRole, round);
                }
                return current;
            }

            log.info("[AgentValidator] {} 校验发现 {} 个问题，尝试第{}轮自修复", agentRole, violations.size(), round + 1);

            if (llmClient == null) break;

            // 自修复：将问题反馈给 Agent
            String repairPrompt = String.format("""
                    你之前的输出存在以下问题，请修正后重新输出：

                    【问题列表】
                    %s

                    【原始输出】
                    %s

                    请修正以上问题，保持 JSON 格式输出。
                    """,
                    String.join("\n", violations.stream().map(v -> "- " + v).toList()),
                    current);

            try {
                var resp = llmClient.chat(model,
                        "你是" + agentRole + "Agent，请修正输出中的问题。", repairPrompt);
                if (resp != null && resp.success() && resp.content() != null) {
                    current = resp.content();
                }
            } catch (Exception e) {
                log.warn("[AgentValidator] {} 自修复调用失败: {}", agentRole, e.getMessage());
                break;
            }
        }

        // 最终校验
        List<String> finalViolations = validate(agentRole, current, sessionId);
        if (finalViolations.isEmpty()) {
            return current;
        }

        log.warn("[AgentValidator] {} 自修复失败，降级输出", agentRole);
        return buildFallbackOutput(agentRole, String.join("; ", finalViolations));
    }

    /**
     * 三层校验
     */
    private List<String> validate(String agentRole, String output, Long sessionId) {
        List<String> violations = new ArrayList<>();

        // 1. 结构校验：JSON 合法性 + 必含字段
        try {
            JsonNode tree = MAPPER.readTree(output);
            List<String> required = REQUIRED_FIELDS.getOrDefault(agentRole, List.of());
            for (String field : required) {
                if (tree.path(field).isMissingNode()) {
                    violations.add("缺少必要字段: " + field);
                }
            }
        } catch (Exception e) {
            violations.add("输出不是合法 JSON 格式");
            return violations; // JSON 不合法则跳过后续校验
        }

        // 2. 事实校验：如果提到商品名，验证是否存在
        if (sessionId != null && liveProductRepository != null) {
            try {
                JsonNode tree = MAPPER.readTree(output);
                List<String> productNames = extractProductNames(tree);
                for (String name : productNames) {
                    if (name != null && !name.isBlank() && !"unknown".equals(name)) {
                        // 简单检查是否该场次有此商品（模糊匹配）
                        boolean exists = liveProductRepository.findBySessionId(sessionId)
                                .stream()
                                .anyMatch(p -> p.getProductName() != null
                                        && p.getProductName().contains(name.substring(0, Math.min(4, name.length()))));
                        if (!exists && !name.isEmpty()) {
                            violations.add("提及的商品可能不存在: " + name);
                        }
                    }
                }
            } catch (Exception ignored) {
                // 事实校验非关键
            }
        }

        // 3. 合规校验
        if (complianceService != null && ("ScriptWriter".equals(agentRole) || "ProductAnalyst".equals(agentRole))) {
            try {
                String textToCheck = output.length() > 2000 ? output.substring(0, 2000) : output;
                var result = complianceService.checkCompliance(textToCheck, "cosmetics");
                if (result != null && !result.isEmpty()) {
                    violations.add("合规检查发现 " + result.size() + " 个违规项");
                }
            } catch (Exception e) {
                // 合规服务不可用时跳过
            }
        }

        return violations;
    }

    private List<String> extractProductNames(JsonNode tree) {
        List<String> names = new ArrayList<>();
        JsonNode recommendation = tree.path("recommendation");
        if (!recommendation.isMissingNode()) {
            JsonNode mainProduct = recommendation.path("mainProduct");
            if (!mainProduct.isMissingNode() && mainProduct.has("name")) {
                names.add(mainProduct.get("name").asText());
            }
            for (String arrayField : List.of("trafficProducts", "profitProducts")) {
                JsonNode arr = recommendation.path(arrayField);
                if (arr.isArray()) {
                    for (JsonNode item : arr) {
                        if (item.has("name")) names.add(item.get("name").asText());
                        else if (item.has("productName")) names.add(item.get("productName").asText());
                    }
                }
            }
        }
        JsonNode scripts = tree.path("scripts");
        if (scripts.isArray()) {
            for (JsonNode script : scripts) {
                if (script.has("productName")) names.add(script.get("productName").asText());
            }
        }
        return names;
    }

    private String buildFallbackOutput(String agentRole, String reason) {
        return "{\"error\":\"QUALITY_FAILED\",\"fallback\":true,\"role\":\"" + agentRole + "\",\"reason\":\"" + reason.replace("\"", "'") + "\"}";
    }
}
