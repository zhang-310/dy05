package cn.gaifan.douyinOperations.module.ai.tool.impl;

import cn.gaifan.douyinOperations.module.ai.service.brain.TrendMonitorService;
import cn.gaifan.douyinOperations.module.ai.tool.LlmRegisteredTool;
import cn.gaifan.douyinOperations.module.ai.tool.LlmToolContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 趋势查询工具：查询当前行业热点趋势，辅助内容策划和话题借势
 */
@Component
public class TrendQueryLlmTool implements LlmRegisteredTool {

    public static final String TOOL_NAME = "trend_query";

    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private TrendMonitorService trendMonitorService;

    public TrendQueryLlmTool(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return TOOL_NAME;
    }

    @Override
    public String description() {
        return "查询当前行业热点趋势列表，包含趋势话题名称、热度、来源。用于内容策划借势、话术融入热点、抢占流量先机。";
    }

    @Override
    public String parametersJsonSchema() {
        return """
                {
                  "type": "object",
                  "properties": {
                    "category": { "type": "string", "description": "品类过滤，如 护肤/彩妆，空表示全行业趋势" },
                    "limit": { "type": "integer", "description": "返回趋势数量，默认 10，最大 30" }
                  }
                }
                """;
    }

    @Override
    public String outputJsonSchema() {
        return """
                {
                  "type": "object",
                  "properties": {
                    "trends": { "type": "array" },
                    "total": { "type": "integer" },
                    "available": { "type": "boolean" },
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
        return 300;
    }

    @Override
    public String execute(String argumentsJson, LlmToolContext ctx) throws Exception {
        if (trendMonitorService == null || !trendMonitorService.isAvailable()) {
            return objectMapper.writeValueAsString(Map.of(
                    "available", false,
                    "trends", List.of(),
                    "total", 0,
                    "message", "趋势监控服务未启用"
            ));
        }

        JsonNode args = objectMapper.readTree(argumentsJson != null && !argumentsJson.isBlank() ? argumentsJson : "{}");
        String category = args.path("category").asText(null);
        if (category != null && category.isBlank()) {
            category = null;
        }
        int limit = Math.min(30, Math.max(1, args.path("limit").asInt(10)));

        List<TrendMonitorService.TrendSignal> signals;
        try {
            signals = trendMonitorService.getCurrentTrends(category, limit);
        } catch (Exception e) {
            return objectMapper.writeValueAsString(Map.of("error", "趋势查询失败：" + e.getMessage(), "trends", List.of()));
        }

        if (signals == null || signals.isEmpty()) {
            return objectMapper.writeValueAsString(Map.of(
                    "available", true,
                    "trends", List.of(),
                    "total", 0,
                    "message", "当前暂无" + (category != null ? category + "品类" : "") + "热点趋势"
            ));
        }

        List<Map<String, Object>> trends = signals.stream().map(t -> Map.<String, Object>of(
                "id", t.id(),
                "title", t.title(),
                "category", t.category() != null ? t.category() : "",
                "heatScore", t.heatScore(),
                "source", t.source() != null ? t.source() : "",
                "description", t.description() != null ? t.description() : ""
        )).collect(Collectors.toList());

        return objectMapper.writeValueAsString(Map.of(
                "available", true,
                "trends", trends,
                "total", trends.size()
        ));
    }
}
