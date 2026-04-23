package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.ai.service.impl.OpenAiCompatibleLlmClient;
import cn.gaifan.douyinOperations.module.live.service.ScriptQualityEvaluator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ScriptQualityEvaluatorImpl implements ScriptQualityEvaluator {

    private static final Logger log = LoggerFactory.getLogger(ScriptQualityEvaluatorImpl.class);

    @Autowired(required = false)
    private OpenAiCompatibleLlmClient llmClient;
    @Autowired(required = false)
    private AiModelRepository aiModelRepository;
    @Autowired
    private ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
        你是直播话术质量评估专家。请对以下话术进行7维度10分制评分，并给出改进建议。
        必须返回严格JSON格式，不含其他文字。
        """;

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> evaluate(String scriptContent, String ipType, Long userId) {
        if (scriptContent == null || scriptContent.isBlank()) {
            return Map.of("error", "话术内容为空");
        }

        if (llmClient == null || aiModelRepository == null) {
            return buildRuleBasedEvaluation(scriptContent, ipType);
        }

        try {
            String prompt = buildEvaluationPrompt(scriptContent, ipType);
            List<AiModel> models = aiModelRepository.findByStatusAndDeleted(1, 0);
            if (models.isEmpty()) {
                return buildRuleBasedEvaluation(scriptContent, ipType);
            }
            LlmClient.LlmResponse response = llmClient.chatWithFallback(models, SYSTEM_PROMPT, prompt);
            if (!response.success() || response.content() == null) {
                log.warn("AI评估未返回有效结果，降级到规则评估: {}", response.errorMsg());
                return buildRuleBasedEvaluation(scriptContent, ipType);
            }
            String cleaned = extractJson(response.content());
            if (cleaned == null) {
                log.warn("AI评估未返回有效JSON，降级到规则评估，原始内容片段: {}",
                        response.content().length() > 200 ? response.content().substring(0, 200) : response.content());
                return buildRuleBasedEvaluation(scriptContent, ipType);
            }
            Map<String, Object> result = objectMapper.readValue(cleaned, Map.class);
            result.put("evaluationMode", "ai");
            return result;
        } catch (Exception e) {
            log.warn("AI评估失败，降级到规则评估: {}", e.getMessage());
            return buildRuleBasedEvaluation(scriptContent, ipType);
        }
    }

    private String buildEvaluationPrompt(String scriptContent, String ipType) {
        StringBuilder sb = new StringBuilder();
        sb.append("请对以下话术进行评估：\n\n");
        sb.append("【话术内容】\n").append(scriptContent).append("\n\n");
        sb.append("【评分维度】（每个维度0-10分，权重如下）\n");
        sb.append("1. personaMatch（人设匹配度 20%）：是否符合主播人设特征、IP调性\n");
        sb.append("2. sceneAdaptation（场景适配度 15%）：话术是否匹配当前时段/场景（开场/主推/冷场/逼单/收尾）\n");
        sb.append("3. infectiousness（情绪感染力 20%）：情绪是否饱满，能否引发共鸣和带动气氛\n");
        sb.append("4. conversionPotential（转化潜力 25%）：是否有明确的动作指令（关注/点赞/下单），商业转化效果\n");
        sb.append("5. interactionDesign（互动设计 10%）：互动指令密度（每2-3分钟至少1次）、互动形式多样性\n");
        sb.append("6. rhythmControl（节奏控制 5%）：语句长短交替、停顿节奏、语速变化控制\n");
        sb.append("7. naturalness（语言自然度 5%）：是否像真人说话、口语化表达、语气词运用\n");
        sb.append("请按权重计算 totalScore = personaMatch*0.20 + sceneAdaptation*0.15 + infectiousness*0.20 + conversionPotential*0.25 + interactionDesign*0.10 + rhythmControl*0.05 + naturalness*0.05\n\n");

        if ("phenomenal".equals(ipType)) {
            sb.append("【FIRE法则评估】（额外评估现象级IP特征）\n");
            sb.append("- F(Fun趣味)：是否有趣，能否引发笑声或好奇\n");
            sb.append("- I(Interaction互动)：互动指令密度是否足够（每2分钟至少1次）\n");
            sb.append("- R(Reward奖励)：是否有福利/优惠/赠品等奖励机制\n");
            sb.append("- E(Emotion情绪)：情绪能量是否高，能否带动气氛\n\n");
        } else if ("top".equals(ipType)) {
            sb.append("【DEPTH法则评估】（额外评估顶级IP特征）\n");
            sb.append("- D(Depth深度)：内容是否有深度，能否提供独到见解\n");
            sb.append("- E(Education教育)：是否有知识输出，能否让观众学到东西\n");
            sb.append("- P(Professional专业)：专业度是否足够，术语是否准确\n");
            sb.append("- T(Trust信任)：是否建立了信任感，是否有权威背书\n");
            sb.append("- H(Heart走心)：是否真诚走心，能否建立情感连接\n\n");
        }

        sb.append("请输出JSON：\n");
        sb.append("{\n");
        sb.append("  \"dimensions\": { \"colloquial\": N, \"infectiousness\": N, \"guidance\": N, ");
        sb.append("\"compliance\": N, \"personaMatch\": N, \"rhythm\": N, \"valueDensity\": N },\n");
        sb.append("  \"totalScore\": N.N,\n");
        sb.append("  \"grade\": \"S/A/B/C/D\",\n");
        if ("phenomenal".equals(ipType)) {
            sb.append("  \"fireScore\": { \"fun\": N, \"interaction\": N, \"reward\": N, \"emotion\": N },\n");
        } else if ("top".equals(ipType)) {
            sb.append("  \"depthScore\": { \"depth\": N, \"education\": N, \"professional\": N, ");
            sb.append("\"trust\": N, \"heart\": N },\n");
        }
        sb.append("  \"strengths\": [\"...\"],\n");
        sb.append("  \"weaknesses\": [\"...\"],\n");
        sb.append("  \"suggestions\": [\"...\"]\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * 从 LLM 输出中提取第一个完整 JSON 对象，容忍前后缀文本和 markdown 代码块
     */
    private static String extractJson(String raw) {
        if (raw == null || raw.isBlank()) return null;
        // 去除 markdown 代码块
        String s = raw.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
        // 找第一个 { 和最后一个 }
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return s.substring(start, end + 1);
        }
        return null;
    }

    private Map<String, Object> buildRuleBasedEvaluation(String scriptContent, String ipType) {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Integer> dimensions = new LinkedHashMap<>();

        int length = scriptContent.length();
        boolean hasQuestionMark = scriptContent.contains("？") || scriptContent.contains("?");
        boolean hasExclamation = scriptContent.contains("！") || scriptContent.contains("!");
        boolean hasEllipsis = scriptContent.contains("…") || scriptContent.contains("...");
        boolean hasParticle = scriptContent.contains("呀") || scriptContent.contains("哦")
                || scriptContent.contains("啊") || scriptContent.contains("吧")
                || scriptContent.contains("呢") || scriptContent.contains("嘛");
        boolean hasAction = scriptContent.contains("关注") || scriptContent.contains("点赞")
                || scriptContent.contains("下单") || scriptContent.contains("拍");
        boolean hasForbidden = scriptContent.contains("最好") || scriptContent.contains("第一")
                || scriptContent.contains("绝对");

        // 1. 人设匹配度 20%
        int personaScore = 6;
        if ("phenomenal".equals(ipType)) {
            boolean hasEnergy = hasExclamation && hasQuestionMark;
            boolean hasHumor = scriptContent.contains("哈") || scriptContent.contains("笑") || scriptContent.contains("段子");
            personaScore = (hasEnergy ? 3 : 0) + (hasHumor ? 2 : 0) + (hasAction ? 2 : 0) + 3;
        } else if ("top".equals(ipType)) {
            boolean hasDepth = length > 150;
            boolean hasKnowledge = scriptContent.contains("原理") || scriptContent.contains("成分") || scriptContent.contains("数据") || scriptContent.contains("研究");
            personaScore = (hasDepth ? 3 : 0) + (hasKnowledge ? 3 : 0) + (hasForbidden ? 0 : 2) + 2;
        }
        dimensions.put("personaMatch", Math.min(10, personaScore));

        // 2. 场景适配度 15%（按关键词判断话术是否适配场景：开场/冷场/逼单/收尾）
        boolean hasOpeningCue = scriptContent.contains("欢迎") || scriptContent.contains("大家好") || scriptContent.contains("来了");
        boolean hasClosingCue = scriptContent.contains("感谢") || scriptContent.contains("再见") || scriptContent.contains("明天见");
        boolean hasUrgencyCue = scriptContent.contains("最后") || scriptContent.contains("倒计时") || scriptContent.contains("库存");
        int sceneScore = 5 + (hasOpeningCue ? 2 : 0) + (hasClosingCue ? 1 : 0) + (hasUrgencyCue ? 1 : 0) + (hasAction ? 1 : 0);
        dimensions.put("sceneAdaptation", Math.min(10, sceneScore));

        // 3. 情绪感染力 20%
        dimensions.put("infectiousness", Math.min(10,
                (hasExclamation ? 3 : 0) + (hasQuestionMark ? 2 : 0) + Math.min(3, length / 50) + 2));

        // 4. 转化潜力 25%
        dimensions.put("conversionPotential", hasAction ? 8 : 4);

        // 5. 互动设计 10%
        boolean hasInteraction = hasQuestionMark || scriptContent.contains("评论") || scriptContent.contains("扣1") || scriptContent.contains("打字");
        dimensions.put("interactionDesign", Math.min(10,
                (hasInteraction ? 4 : 0) + (hasQuestionMark ? 2 : 0) + (hasAction ? 2 : 0) + 2));

        // 6. 节奏控制 5%
        dimensions.put("rhythmControl", Math.min(10,
                (hasEllipsis ? 2 : 0) + Math.min(5, length / 30) + 3));

        // 7. 语言自然度 5%
        dimensions.put("naturalness", Math.min(10,
                (hasParticle ? 3 : 0) + (hasEllipsis ? 2 : 0) + (hasExclamation ? 2 : 0) + 3));

        // 加权评分
        double totalScore = dimensions.get("personaMatch") * 0.20
                + dimensions.get("sceneAdaptation") * 0.15
                + dimensions.get("infectiousness") * 0.20
                + dimensions.get("conversionPotential") * 0.25
                + dimensions.get("interactionDesign") * 0.10
                + dimensions.get("rhythmControl") * 0.05
                + dimensions.get("naturalness") * 0.05;
        totalScore = Math.round(totalScore * 10) / 10.0;

        String grade;
        if (totalScore >= 9) grade = "S";
        else if (totalScore >= 7.5) grade = "A";
        else if (totalScore >= 6) grade = "B";
        else if (totalScore >= 4) grade = "C";
        else grade = "D";

        result.put("dimensions", dimensions);
        result.put("totalScore", totalScore);
        result.put("grade", grade);
        result.put("evaluationMode", "rule");

        if ("phenomenal".equals(ipType)) {
            Map<String, Integer> fireScore = new LinkedHashMap<>();
            fireScore.put("fun", hasExclamation || hasQuestionMark ? 7 : 4);
            fireScore.put("interaction", hasAction ? 7 : 3);
            fireScore.put("reward", scriptContent.contains("福利") || scriptContent.contains("优惠") ? 7 : 3);
            fireScore.put("emotion", hasExclamation ? 7 : 4);
            result.put("fireScore", fireScore);
        } else if ("top".equals(ipType)) {
            Map<String, Integer> depthScore = new LinkedHashMap<>();
            depthScore.put("depth", Math.min(8, length / 60 + 3));
            depthScore.put("education", 5);
            depthScore.put("professional", 5);
            depthScore.put("trust", 5);
            depthScore.put("heart", hasParticle ? 6 : 4);
            result.put("depthScore", depthScore);
        }

        List<String> suggestions = new ArrayList<>();
        if (!hasAction) suggestions.add("转化潜力偏低：建议增加行动指令（关注/点赞/下单）");
        if (!hasInteraction) suggestions.add("互动设计不足：建议增加互动引导（提问/评论/选择）");
        if (!hasParticle) suggestions.add("语言自然度偏低：建议增加语气词使话术更口语化");
        if (hasForbidden) suggestions.add("注意合规：避免使用绝对化用语");
        if (length < 50) suggestions.add("话术内容较短，建议增加信息密度");
        if (sceneScore < 6) suggestions.add("场景适配度偏低：建议增加与当前场景匹配的关键表达");
        result.put("suggestions", suggestions);

        return result;
    }
}
