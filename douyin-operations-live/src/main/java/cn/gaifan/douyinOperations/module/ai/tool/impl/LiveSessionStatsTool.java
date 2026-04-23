package cn.gaifan.douyinOperations.module.ai.tool.impl;

import cn.gaifan.douyinOperations.module.ai.tool.LlmRegisteredTool;
import cn.gaifan.douyinOperations.module.ai.tool.LlmToolContext;
import cn.gaifan.douyinOperations.module.live.service.LiveSessionService;
import cn.gaifan.douyinOperations.module.live.vo.LiveSessionSearchVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveSessionVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 直播场次统计工具：查询历史场次数据，包含 GMV、观看人数、话术数量等关键指标
 */
@Component
public class LiveSessionStatsTool implements LlmRegisteredTool {

    public static final String TOOL_NAME = "live_session_stats";

    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private LiveSessionService liveSessionService;

    public LiveSessionStatsTool(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return TOOL_NAME;
    }

    @Override
    public String description() {
        return "查询直播场次历史数据，包含 GMV（成交额）、观看人数、点赞数、话术数等指标。用于分析直播效果、历史对比与策略优化建议。";
    }

    @Override
    public String parametersJsonSchema() {
        return """
                {
                  "type": "object",
                  "properties": {
                    "limit": { "type": "integer", "description": "返回场次数，默认 5，最大 20" },
                    "status": { "type": "integer", "description": "场次状态：0=准备中 1=直播中 2=已结束，不传则返回全部" }
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
                    "sessions": { "type": "array" },
                    "total": { "type": "integer" },
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
        return 120;
    }

    @Override
    public String execute(String argumentsJson, LlmToolContext ctx) throws Exception {
        if (liveSessionService == null) {
            return objectMapper.writeValueAsString(Map.of("error", "直播场次服务不可用"));
        }
        if (ctx == null || ctx.userId() == null) {
            return objectMapper.writeValueAsString(Map.of("error", "缺少用户上下文"));
        }

        JsonNode args = objectMapper.readTree(argumentsJson != null && !argumentsJson.isBlank() ? argumentsJson : "{}");
        int limit = Math.min(20, Math.max(1, args.path("limit").asInt(5)));

        LiveSessionSearchVO searchVO = new LiveSessionSearchVO();
        searchVO.setPage(0);
        searchVO.setRows(limit);
        searchVO.setUserId(ctx.userId());
        searchVO.setSortName("createTime");
        searchVO.setSortOrder("desc");
        if (args.has("status")) {
            searchVO.setStatus(args.get("status").asInt());
        }

        var result = liveSessionService.search(searchVO);
        if (result == null || result.getList() == null || result.getList().isEmpty()) {
            return objectMapper.writeValueAsString(Map.of("sessions", List.of(), "total", 0, "message", "暂无直播场次数据"));
        }

        List<Map<String, Object>> sessions = result.getList().stream().map(s -> {
            java.util.LinkedHashMap<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id", s.getId());
            m.put("title", s.getLiveTitle() != null ? s.getLiveTitle() : "");
            m.put("status", s.getStatus());
            m.put("viewers", s.getViewers() != null ? s.getViewers() : 0);
            m.put("likes", s.getLikes() != null ? s.getLikes() : 0);
            m.put("startTime", s.getStartTime() != null ? s.getStartTime().toString() : null);
            m.put("endTime", s.getEndTime() != null ? s.getEndTime().toString() : null);
            return (Map<String, Object>) m;
        }).collect(Collectors.toList());

        return objectMapper.writeValueAsString(Map.of("sessions", sessions, "total", result.getTotal()));
    }
}
