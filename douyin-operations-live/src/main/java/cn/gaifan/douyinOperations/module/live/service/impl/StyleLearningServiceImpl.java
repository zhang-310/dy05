package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.douyin.entity.DyPersona;
import cn.gaifan.douyinOperations.module.douyin.repository.DyPersonaRepository;
import cn.gaifan.douyinOperations.module.live.entity.LiveScript;
import cn.gaifan.douyinOperations.module.live.entity.LiveSession;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
import cn.gaifan.douyinOperations.module.live.service.StyleLearningService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StyleLearningServiceImpl implements StyleLearningService {

    private static final Logger log = LoggerFactory.getLogger(StyleLearningServiceImpl.class);
    private static final BigDecimal SCORE_THRESHOLD = new BigDecimal("70.00");
    private static final int SAMPLE_LIMIT = 30;

    @Resource
    private LiveScriptRepository liveScriptRepository;

    @Resource
    private LiveSessionRepository liveSessionRepository;

    @Resource
    private DyPersonaRepository dyPersonaRepository;

    @Resource
    private LlmClient llmClient;

    @Resource
    private AiModelRepository aiModelRepository;

    @Override
    public String learnPersonaStyle(Long personaId, Long userId) {
        if (personaId == null || userId == null) {
            return null;
        }

        DyPersona persona = dyPersonaRepository.findByIdAndDeleted(personaId, 0).orElse(null);
        if (persona == null) {
            log.warn("人设不存在: personaId={}", personaId);
            return null;
        }

        List<Long> sessionIds = liveSessionRepository.findByPersonaIdAndUserIdAndDeleted(personaId, userId, 0)
                .stream().map(LiveSession::getId).toList();

        if (sessionIds.isEmpty()) {
            log.info("人设 {} 无关联场次，跳过风格学习", personaId);
            return null;
        }

        List<LiveScript> highScoreScripts = liveScriptRepository
                .findByUserIdAndEffectivenessScoreGte(userId, SCORE_THRESHOLD)
                .stream()
                .filter(s -> sessionIds.contains(s.getSessionId()))
                .limit(SAMPLE_LIMIT)
                .toList();

        if (highScoreScripts.isEmpty()) {
            log.info("人设 {} 无高分话术样本（score >= {}），跳过风格学习", personaId, SCORE_THRESHOLD);
            return null;
        }

        String samples = highScoreScripts.stream()
                .map(s -> String.format("【评分 %s | 类型 %s】\n%s",
                        s.getEffectivenessScore(), s.getScriptType(), s.getScriptContent()))
                .collect(Collectors.joining("\n\n---\n\n"));

        String systemPrompt = "你是话术风格分析专家。请根据以下高效话术样本，提炼出该主播的核心话术风格特征摘要，" +
                "包括语气、用词习惯、节奏特点、情感表达方式等。输出 200 字以内的简洁摘要。";

        String userPrompt = String.format("主播人设：%s（%s）\n\n以下是该人设最近的 %d 条高分话术样本：\n\n%s",
                persona.getPersonaName(),
                persona.getPersonaType() != null ? persona.getPersonaType() : "通用",
                highScoreScripts.size(),
                samples);

        List<AiModel> models = aiModelRepository.findByStatusAndDeleted(1, 0).stream().limit(3).toList();
        if (models.isEmpty()) {
            log.warn("无可用 AI 模型，跳过风格学习");
            return null;
        }

        LlmClient.LlmResponse resp = llmClient.chatWithFallback(models, systemPrompt, userPrompt);
        if (!resp.success() || resp.content() == null || resp.content().isBlank()) {
            log.warn("风格学习 LLM 调用失败: {}", resp.errorMsg());
            return null;
        }

        log.info("人设 {} 风格学习完成，样本数={}，摘要长度={}", personaId, highScoreScripts.size(), resp.content().length());
        return resp.content();
    }
}
