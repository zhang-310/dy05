package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.service.CompetitorDataParser;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 竞品数据解析器实现：通过 LLM 将原始文本结构化为竞品洞察
 */
@Slf4j
@Service
public class CompetitorDataParserImpl implements CompetitorDataParser {

    private static final String PARSE_SYSTEM_PROMPT = """
            你是竞品分析专家。将以下原始数据解析为结构化的竞品洞察。
            返回 JSON 数组，每个洞察包含：
            {
              "insightType": "price_strategy|speech_style|product_rhythm|content_form|audience_positioning",
              "content": "具体洞察内容",
              "qualityScore": 0.8,
              "dimension": "价格策略|话术风格|推品节奏|内容形式|受众定位"
            }
            仅提取有价值的、可操作的洞察，忽略无效或重复信息。
            """;

    @Autowired(required = false)
    private LlmClient llmClient;

    @Autowired(required = false)
    private AiModelRepository modelRepository;

    @Override
    public List<Map<String, Object>> parseToInsights(String rawText, String dataSource, String category) {
        if (rawText == null || rawText.isBlank()) return List.of();
        if (llmClient == null || modelRepository == null) {
            return fallbackParse(rawText, dataSource, category);
        }

        List<AiModel> models = modelRepository.findByStatusAndDeleted(1, 0);
        if (models.isEmpty()) return fallbackParse(rawText, dataSource, category);

        String prompt = String.format("【数据源】%s\n【品类】%s\n【原始数据】\n%s", dataSource, category, rawText);
        LlmClient.LlmResponse response = llmClient.chatWithFallback(models, PARSE_SYSTEM_PROMPT, prompt);

        if (!response.success() || response.content() == null) {
            log.warn("[CompetitorDataParser] LLM 解析失败: {}", response.errorMsg());
            return fallbackParse(rawText, dataSource, category);
        }

        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var tree = mapper.readTree(response.content());
            List<Map<String, Object>> results = new ArrayList<>();
            if (tree.isArray()) {
                for (var node : tree) {
                    Map<String, Object> insight = new LinkedHashMap<>();
                    insight.put("insightType", node.path("insightType").asText("trend"));
                    insight.put("content", node.path("content").asText());
                    insight.put("qualityScore", node.path("qualityScore").asDouble(0.5));
                    insight.put("dimension", node.path("dimension").asText());
                    results.add(insight);
                }
            }
            return results;
        } catch (Exception e) {
            log.warn("[CompetitorDataParser] JSON 解析失败: {}", e.getMessage());
            return fallbackParse(rawText, dataSource, category);
        }
    }

    @Override
    public double scoreQuality(String content) {
        if (content == null || content.isBlank()) return 0.0;
        double score = 0.3; // 基础分
        if (content.length() > 50) score += 0.1;
        if (content.length() > 200) score += 0.1;
        if (content.contains("数据") || content.contains("趋势")) score += 0.1;
        if (content.contains("策略") || content.contains("建议")) score += 0.1;
        if (content.contains("对比") || content.contains("分析")) score += 0.1;
        return Math.min(score, 1.0);
    }

    private List<Map<String, Object>> fallbackParse(String rawText, String dataSource, String category) {
        Map<String, Object> insight = new LinkedHashMap<>();
        insight.put("insightType", "trend");
        insight.put("content", rawText.length() > 500 ? rawText.substring(0, 500) : rawText);
        insight.put("qualityScore", scoreQuality(rawText));
        insight.put("dimension", "综合");
        return List.of(insight);
    }
}
