package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.live.entity.LiveLearningMemory;
import cn.gaifan.douyinOperations.module.live.entity.LiveSession;
import cn.gaifan.douyinOperations.module.live.repository.LiveLearningMemoryRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
import cn.gaifan.douyinOperations.module.live.service.CrossSessionLearningService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 跨场次学习服务实现
 * <p>
 * 场次结束 → LLM 总结本场学习（高效话术模式、推品时机、观众偏好）
 * 下一场开播 → 加载同品类最近 5 场 learning_memory → 注入初始 prompt
 * confidence 衰减：引用但无正效果 → confidence *= 0.9；< 0.3 自动归档
 */
@Slf4j
@Service
public class CrossSessionLearningServiceImpl implements CrossSessionLearningService {

    private static final BigDecimal MIN_CONFIDENCE = new BigDecimal("0.30");
    private static final String EXTRACT_SYSTEM_PROMPT = """
            你是直播运营分析专家。分析以下直播场次的实时学习上下文，提取可跨场次复用的学习洞察。
            请返回 JSON 数组，每个洞察包含：
            {
              "insight_type": "effective_pattern|audience_preference|timing_insight|anti_pattern",
              "content": "具体的学习内容",
              "confidence": 0.8
            }
            说明：
            - effective_pattern: 效果好的话术模式
            - audience_preference: 观众偏好特征
            - timing_insight: 推品时机洞察
            - anti_pattern: 需要避免的模式
            """;

    @Autowired
    private LiveLearningMemoryRepository memoryRepository;

    @Autowired
    private LiveSessionRepository sessionRepository;

    @Autowired(required = false)
    private LlmClient llmClient;

    @Autowired(required = false)
    private AiModelRepository modelRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void extractAndPersistInsights(Long sessionId) {
        if (llmClient == null || modelRepository == null) {
            log.debug("[CrossSessionLearning] LLM 不可用，跳过学习提取");
            return;
        }

        var sessionOpt = sessionRepository.findById(sessionId);
        if (sessionOpt.isEmpty()) return;
        LiveSession session = sessionOpt.get();

        Long userId = session.getUserId();
        if (userId == null) {
            log.debug("[CrossSessionLearning] 场次 {} 无 userId，跳过", sessionId);
            return;
        }

        // 获取场次学习上下文
        String contextSummary = buildSessionSummary(session);
        if (contextSummary.isBlank()) {
            log.debug("[CrossSessionLearning] 场次 {} 无有效上下文，跳过", sessionId);
            return;
        }

        List<AiModel> models = modelRepository.findByStatusAndDeleted(1, 0);
        if (models.isEmpty()) return;

        LlmClient.LlmResponse response = llmClient.chatWithFallback(
                models, EXTRACT_SYSTEM_PROMPT, contextSummary);
        if (!response.success() || response.content() == null) {
            log.warn("[CrossSessionLearning] LLM 提取失败: {}", response.errorMsg());
            return;
        }

        // 解析并保存洞察
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var insights = mapper.readTree(response.content());
            if (insights.isArray()) {
                for (var node : insights) {
                    LiveLearningMemory memory = new LiveLearningMemory();
                    memory.setUserId(userId);
                    memory.setCategory(session.getLiveFormat());
                    memory.setInsightType(node.path("insight_type").asText("effective_pattern"));
                    memory.setContent(node.path("content").asText());
                    BigDecimal conf = new BigDecimal(node.path("confidence").asDouble(0.8));
                    memory.setConfidence(conf);
                    memory.setSourceSessionId(sessionId);
                    memoryRepository.save(memory);
                }
                log.info("[CrossSessionLearning] 场次 {} 提取 {} 条学习洞察", sessionId, insights.size());
            }
        } catch (Exception e) {
            log.warn("[CrossSessionLearning] 解析洞察失败: {}", e.getMessage());
        }
    }

    @Override
    public String loadCrossSessionContext(Long userId, String category) {
        if (userId == null) return "";

        List<LiveLearningMemory> memories = memoryRepository
                .findTop5ByUserIdAndCategoryAndConfidenceGreaterThanAndDeletedOrderByConfidenceDesc(
                        userId, category, MIN_CONFIDENCE, 0);

        if (memories.isEmpty()) return "";

        StringBuilder sb = new StringBuilder("【跨场次学习记忆】\n");
        for (LiveLearningMemory m : memories) {
            String typeLabel = switch (m.getInsightType()) {
                case "effective_pattern" -> "有效模式";
                case "audience_preference" -> "观众偏好";
                case "timing_insight" -> "时机洞察";
                case "anti_pattern" -> "避免模式";
                default -> m.getInsightType();
            };
            sb.append(String.format("- [%s](置信度%.0f%%): %s\n",
                    typeLabel, m.getConfidence().doubleValue() * 100, m.getContent()));

            // 增加使用次数
            memoryRepository.incrementUsageCount(m.getId());
        }
        return sb.toString();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void decayIneffectiveMemories() {
        List<LiveLearningMemory> lowConfidence = memoryRepository
                .findByConfidenceLessThanAndDeleted(MIN_CONFIDENCE, 0);
        for (LiveLearningMemory m : lowConfidence) {
            m.setDeleted(1); // 归档
            memoryRepository.save(m);
        }
        if (!lowConfidence.isEmpty()) {
            log.info("[CrossSessionLearning] 归档 {} 条低置信度记忆", lowConfidence.size());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reinforceMemory(Long userId, String liveFormat, String insightType, boolean positive) {
        if (userId == null) return;
        try {
            List<LiveLearningMemory> memories = memoryRepository
                    .findTop5ByUserIdAndCategoryAndConfidenceGreaterThanAndDeletedOrderByConfidenceDesc(
                            userId, liveFormat, MIN_CONFIDENCE, 0);
            for (LiveLearningMemory m : memories) {
                if (insightType != null && !insightType.equals(m.getInsightType())) continue;
                double factor = positive ? 1.1 : 0.8;
                double newConf = Math.min(1.0, Math.max(0.1, m.getConfidence().doubleValue() * factor));
                m.setConfidence(new BigDecimal(String.format("%.2f", newConf)));
                memoryRepository.save(m);
            }
            log.debug("[CrossSessionLearning] 记忆置信度{}调整完成: userId={}, format={}, type={}",
                    positive ? "强化" : "弱化", userId, liveFormat, insightType);
        } catch (Exception e) {
            log.debug("[CrossSessionLearning] 记忆调整失败: {}", e.getMessage());
        }
    }

    private String buildSessionSummary(LiveSession session) {
        StringBuilder sb = new StringBuilder();
        sb.append("场次ID: ").append(session.getId());
        if (session.getLiveTitle() != null) sb.append("，标题: ").append(session.getLiveTitle());
        if (session.getLiveFormat() != null) sb.append("，形式: ").append(session.getLiveFormat());
        if (session.getViewers() != null) sb.append("，观众: ").append(session.getViewers());
        if (session.getLikes() != null) sb.append("，点赞: ").append(session.getLikes());
        return sb.toString();
    }
}
