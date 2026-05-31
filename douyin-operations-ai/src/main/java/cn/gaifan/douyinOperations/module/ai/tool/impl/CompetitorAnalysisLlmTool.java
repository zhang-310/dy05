package cn.gaifan.douyinOperations.module.ai.tool.impl;

import cn.gaifan.douyinOperations.module.ai.service.CompetitorInsightService;
import cn.gaifan.douyinOperations.module.ai.tool.LlmRegisteredTool;
import cn.gaifan.douyinOperations.module.ai.tool.LlmToolContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 竞品分析工具：获取同品类竞品洞察与差异化建议，辅助直播策略制定
 */
@Component
public class CompetitorAnalysisLlmTool implements LlmRegisteredTool {

    public static final String TOOL_NAME = "competitor_analysis";

    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private CompetitorInsightService competitorInsightService;

    public CompetitorAnalysisLlmTool(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return TOOL_NAME;
    }

    @Override
    public String description() {
        return "分析指定品类的竞品动态，返回近期竞品洞察摘要与差异化建议。用于制定竞争策略、话术差异化、选品规避同质化。";
    }

    @Override
    public String parametersJsonSchema() {
        return """
                {
                  "type": "object",
                  "properties": {
                    "category": { "type": "string", "description": "品类名称，如 护肤/面膜/彩妆/香水" },
                    "days": { "type": "integer", "description": "分析最近 N 天的数据，默认 7，最大 30" }
                  },
                  "required": ["category"]
                }
                """;
    }

    @Override
    public String outputJsonSchema() {
        return """
                {
                  "type": "object",
                  "properties": {
                    "differentiationAdvice": { "type": "string" },
                    "recentInsights": { "type": "array" },
                    "category": { "type": "string" },
                    "message": { "type": "string" }
                  }
                }
                """;
    }

    @Override
    public boolean resultCacheable() {
        return true;
    }

    @Override
    public int resultCacheTtlSeconds() {
        return 1800;
    }

    @Override
    public String execute(String argumentsJson, LlmToolContext ctx) throws Exception {
        if (competitorInsightService == null) {
            return objectMapper.writeValueAsString(Map.of("message", "竞品分析服务未启用，请检查配置", "recentInsights", List.of()));
        }

        JsonNode args = objectMapper.readTree(argumentsJson != null && !argumentsJson.isBlank() ? argumentsJson : "{}");
        String category = args.path("category").asText("").trim();
        if (category.isEmpty()) {
            return objectMapper.writeValueAsString(Map.of("error", "category 不能为空"));
        }
        int days = Math.min(30, Math.max(1, args.path("days").asInt(7)));

        String differentiationAdvice = "";
        try {
            differentiationAdvice = competitorInsightService.getDifferentiationAdvice(category);
        } catch (Exception e) {
            differentiationAdvice = "差异化建议暂时不可用";
        }

        List<Map<String, Object>> recentInsights = List.of();
        try {
            recentInsights = competitorInsightService.getRecentInsights(category, days);
            if (recentInsights == null) {
                recentInsights = List.of();
            }
        } catch (Exception e) {
            // 降级返回空列表
        }

        return objectMapper.writeValueAsString(Map.of(
                "category", category,
                "differentiationAdvice", differentiationAdvice != null ? differentiationAdvice : "",
                "recentInsights", recentInsights,
                "days", days
        ));
    }
}
