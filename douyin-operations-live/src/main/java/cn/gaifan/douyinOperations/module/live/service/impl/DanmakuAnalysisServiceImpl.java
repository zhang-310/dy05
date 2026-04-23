package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.live.service.DanmakuAnalysisService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.Duration;
import java.util.*;

@Service
public class DanmakuAnalysisServiceImpl implements DanmakuAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(DanmakuAnalysisServiceImpl.class);

    private static final String INTENT_ANALYSIS_SYSTEM_PROMPT =
            "你是一个直播弹幕分析助手。请分析以下弹幕文本，将每条弹幕分类为以下意图之一：" +
            "purchase_intent（购买意向）、question（疑问）、interaction（互动）、negative（负面）。" +
            "返回 JSON 格式：{\"purchase_intent\": [...], \"question\": [...], \"interaction\": [...], \"negative\": [...], " +
            "\"summary\": {\"total\": N, \"purchase_intent_count\": N, \"question_count\": N, \"interaction_count\": N, \"negative_count\": N}}";

    private static final String SCRIPT_ADJUSTMENT_SYSTEM_PROMPT =
            "你是一个直播话术优化助手。根据弹幕意图分析结果，给出话术调整建议。" +
            "如果购买意向多，建议加强促单话术；如果疑问多，建议增加产品解答；" +
            "如果负面评论多，建议安抚和转移话题；如果互动多，建议趁热打铁推荐产品。" +
            "返回 JSON 数组，每项为一条调整建议字符串。";

    @Resource
    private LlmClient llmClient;

    @Resource
    private AiModelRepository aiModelRepository;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, Object> analyzeIntents(Long sessionId, List<String> danmakuTexts) {
        if (danmakuTexts == null || danmakuTexts.isEmpty()) {
            return Map.of("summary", Map.of("total", 0));
        }

        String prompt = "弹幕列表（共" + danmakuTexts.size() + "条）：\n" + String.join("\n", danmakuTexts);

        AiModel model = getAvailableModel();
        if (model == null) {
            log.warn("无可用AI模型，跳过弹幕意图分析, sessionId={}", sessionId);
            return buildFallbackIntents(danmakuTexts);
        }

        LlmClient.LlmResponse response = llmClient.chat(model, INTENT_ANALYSIS_SYSTEM_PROMPT, prompt);
        if (!response.success()) {
            log.warn("弹幕意图分析LLM调用失败: sessionId={}, error={}", sessionId, response.errorMsg());
            return buildFallbackIntents(danmakuTexts);
        }

        try {
            return objectMapper.readValue(response.content(), new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("解析弹幕意图分析结果失败, sessionId={}", sessionId, e);
            return buildFallbackIntents(danmakuTexts);
        }
    }

    @Override
    public List<String> suggestScriptAdjustments(Long sessionId, Map<String, Object> intents) {
        if (intents == null || intents.isEmpty()) {
            return Collections.emptyList();
        }

        String prompt;
        try {
            prompt = "弹幕意图分析结果：\n" + objectMapper.writeValueAsString(intents);
        } catch (Exception e) {
            log.warn("序列化意图数据失败", e);
            return Collections.emptyList();
        }

        AiModel model = getAvailableModel();
        if (model == null) {
            log.warn("无可用AI模型，跳过话术调整建议, sessionId={}", sessionId);
            return Collections.emptyList();
        }

        LlmClient.LlmResponse response = llmClient.chat(model, SCRIPT_ADJUSTMENT_SYSTEM_PROMPT, prompt);
        if (!response.success()) {
            log.warn("话术调整建议LLM调用失败: sessionId={}, error={}", sessionId, response.errorMsg());
            return Collections.emptyList();
        }

        try {
            return objectMapper.readValue(response.content(), new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("解析话术调整建议失败, sessionId={}", sessionId, e);
            return Collections.singletonList(response.content());
        }
    }

    private AiModel getAvailableModel() {
        List<AiModel> models = aiModelRepository.findByStatusAndDeleted(1, 0);
        return models.isEmpty() ? null : models.get(0);
    }

    private Map<String, Object> buildFallbackIntents(List<String> danmakuTexts) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("purchase_intent", Collections.emptyList());
        result.put("question", Collections.emptyList());
        result.put("interaction", danmakuTexts);
        result.put("negative", Collections.emptyList());
        result.put("summary", Map.of(
                "total", danmakuTexts.size(),
                "purchase_intent_count", 0,
                "question_count", 0,
                "interaction_count", danmakuTexts.size(),
                "negative_count", 0
        ));
        return result;
    }

    /**
     * 批量分析窗口内弹幕意图分布（30秒窗口，Redis缓存60s）
     */
    public Map<String, Object> analyzeBatchIntents(Long sessionId, List<String> danmakuTexts) {
        if (danmakuTexts == null || danmakuTexts.isEmpty()) {
            return Map.of("purchase_intent", 0, "question", 0, "interaction", 0, "negative", 0, "total", 0);
        }

        // Try Redis cache
        String cacheKey = "live:danmaku:intent:" + sessionId;
        if (redisTemplate != null) {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> cachedResult = objectMapper.readValue(cached, Map.class);
                    return cachedResult;
                } catch (Exception ignored) {}
            }
        }

        // LLM batch analysis
        Map<String, Object> result = performBatchIntentAnalysis(danmakuTexts);

        // Cache result 60s
        if (redisTemplate != null) {
            try {
                String json = objectMapper.writeValueAsString(result);
                redisTemplate.opsForValue().set(cacheKey, json, Duration.ofSeconds(60));
            } catch (Exception ignored) {}
        }

        return result;
    }

    private Map<String, Object> performBatchIntentAnalysis(List<String> danmakuTexts) {
        int purchaseIntent = 0, question = 0, interaction = 0, negative = 0;

        AiModel model = getAvailableModel();
        if (model != null) {
            try {
                String combined = String.join("\n", danmakuTexts);
                String systemPrompt = "你是直播弹幕分析师。将每条弹幕分类为以下4类之一：purchase_intent(购买意向)、question(提问)、interaction(互动)、negative(负面)。返回JSON格式：{\"purchase_intent\":数量,\"question\":数量,\"interaction\":数量,\"negative\":数量}";
                var resp = llmClient.chat(model, systemPrompt, "弹幕列表:\n" + combined);
                if (resp != null && resp.success() && resp.content() != null) {
                    String content = resp.content().trim();
                    // Strip JSON fence if present
                    if (content.startsWith("```")) {
                        content = content.replaceAll("```json\\s*", "").replaceAll("```\\s*$", "").trim();
                    }
                    @SuppressWarnings("unchecked")
                    Map<String, Object> parsed = objectMapper.readValue(content, Map.class);
                    return Map.of(
                            "purchase_intent", parsed.getOrDefault("purchase_intent", 0),
                            "question", parsed.getOrDefault("question", 0),
                            "interaction", parsed.getOrDefault("interaction", 0),
                            "negative", parsed.getOrDefault("negative", 0),
                            "total", danmakuTexts.size()
                    );
                }
            } catch (Exception e) {
                log.warn("[DanmakuAnalysis] LLM 批量分析失败，使用关键词兜底: {}", e.getMessage());
            }
        }

        // Keyword fallback
        for (String text : danmakuTexts) {
            String lower = text.toLowerCase();
            if (lower.contains("买") || lower.contains("下单") || lower.contains("链接") || lower.contains("价格") || lower.contains("多少钱")) {
                purchaseIntent++;
            } else if (lower.contains("？") || lower.contains("?") || lower.contains("怎么") || lower.contains("什么") || lower.contains("吗")) {
                question++;
            } else if (lower.contains("差") || lower.contains("假") || lower.contains("骗") || lower.contains("退") || lower.contains("不好")) {
                negative++;
            } else {
                interaction++;
            }
        }

        return Map.of(
                "purchase_intent", purchaseIntent,
                "question", question,
                "interaction", interaction,
                "negative", negative,
                "total", danmakuTexts.size()
        );
    }
}
