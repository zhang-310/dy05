package cn.gaifan.douyinOperations.module.ai.tool.impl;

import cn.gaifan.douyinOperations.module.ai.service.ContentEffectivenessService;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeBaseService;
import cn.gaifan.douyinOperations.module.ai.tool.LlmRegisteredTool;
import cn.gaifan.douyinOperations.module.ai.tool.LlmToolContext;
import cn.gaifan.douyinOperations.module.ai.vo.RagRetrieveItemVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 知识库混合检索工具：供 Agent / 话术 / 直播优化等场景由模型按需调用
 */
@Component
public class KnowledgeBaseRagLlmTool implements LlmRegisteredTool {

    public static final String TOOL_NAME = "kb_rag_search";

    private final KnowledgeBaseService knowledgeBaseService;
    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private ContentEffectivenessService contentEffectivenessService;

    public KnowledgeBaseRagLlmTool(KnowledgeBaseService knowledgeBaseService, ObjectMapper objectMapper) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return TOOL_NAME;
    }

    @Override
    public String description() {
        return "检索当前用户在系统中的知识库（抖音素材/话术/知识等），返回与查询相关的文档片段。需要事实依据、话术范例、产品资料时调用。";
    }

    @Override
    public String outputJsonSchema() {
        return """
                {
                  "type": "object",
                  "minProperties": 1,
                  "properties": {
                    "items": { "type": "array" },
                    "message": { "type": "string" },
                    "error": { "type": "string" }
                  }
                }
                """;
    }

    @Override
    public String parametersJsonSchema() {
        return """
                {
                  "type": "object",
                  "properties": {
                    "query": { "type": "string", "description": "检索查询，用自然语言描述要找的内容" },
                    "top_k": { "type": "integer", "description": "返回条数，默认 8，最大 20" },
                    "scope": { "type": "string", "description": "可选：douyin / zhishi / huashu，空表示按系统默认跨库检索" }
                  },
                  "required": ["query"]
                }
                """;
    }

    @Override
    public String execute(String argumentsJson, LlmToolContext ctx) throws Exception {
        if (ctx == null || ctx.userId() == null) {
            return objectMapper.writeValueAsString(Map.of("error", "缺少 userId 上下文"));
        }
        JsonNode args = objectMapper.readTree(argumentsJson != null && !argumentsJson.isBlank() ? argumentsJson : "{}");
        String query = args.path("query").asText("").trim();
        if (query.isEmpty()) {
            return objectMapper.writeValueAsString(Map.of("error", "query 不能为空"));
        }
        int topK = args.has("top_k") ? Math.min(20, Math.max(1, args.get("top_k").asInt(8))) : 8;
        String scope = args.path("scope").asText(null);
        if (scope != null && scope.isBlank()) {
            scope = null;
        }

        List<RagRetrieveItemVO> items = knowledgeBaseService.hybridSearchAllKbs(ctx.userId(), query, topK, scope, null);
        if (items == null || items.isEmpty()) {
            return objectMapper.writeValueAsString(Map.of("message", "未检索到相关文档", "items", List.of()));
        }

        if (contentEffectivenessService != null) {
            List<Long> docIds = items.stream()
                    .map(RagRetrieveItemVO::getDocId)
                    .filter(id -> id != null && id > 0)
                    .distinct()
                    .collect(Collectors.toList());
            if (!docIds.isEmpty()) {
                contentEffectivenessService.recordRetrieval(docIds);
                contentEffectivenessService.recordCitation(docIds);
            }
        }

        List<Map<String, Object>> compact = items.stream().map(it -> {
            String content = it.getContent();
            if (content != null && content.length() > 800) {
                content = content.substring(0, 800) + "...";
            }
            return Map.<String, Object>of(
                    "docId", it.getDocId() != null ? it.getDocId() : 0,
                    "kbName", it.getKbName() != null ? it.getKbName() : "",
                    "title", it.getTitle() != null ? it.getTitle() : "",
                    "score", it.getScore(),
                    "content", content != null ? content : ""
            );
        }).collect(Collectors.toList());

        return objectMapper.writeValueAsString(Map.of("items", compact));
    }
}
