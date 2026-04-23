package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.module.ai.service.PromptSelfOptimizationService;
import cn.gaifan.douyinOperations.module.live.entity.LiveSession;
import cn.gaifan.douyinOperations.module.live.repository.LiveDanmakuRecordRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
import cn.gaifan.douyinOperations.module.live.service.CrossSessionLearningService;
import cn.gaifan.douyinOperations.module.live.service.DanmakuAnalysisService;
import cn.gaifan.douyinOperations.module.live.service.LiveOnlineLearningService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LiveOnlineLearningServiceImpl implements LiveOnlineLearningService {

    @Autowired
    private LiveSessionRepository sessionRepository;

    @Autowired(required = false)
    private LiveDanmakuRecordRepository danmakuRecordRepository;

    @Autowired(required = false)
    private DanmakuAnalysisService danmakuAnalysisService;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Autowired(required = false)
    private CrossSessionLearningService crossSessionLearningService;

    @Autowired(required = false)
    private PromptSelfOptimizationService promptSelfOptimizationService;

    @Override
    public void updateLearningContext(Long sessionId) {
        var sessionOpt = sessionRepository.findById(sessionId);
        if (sessionOpt.isEmpty()) return;
        LiveSession session = sessionOpt.get();

        StringBuilder context = new StringBuilder("【实时反馈】");

        // Current metrics
        long viewers = session.getViewers() != null ? session.getViewers() : 0;
        long likes = session.getLikes() != null ? session.getLikes() : 0;
        context.append(String.format("当前观众 %d，点赞 %d。", viewers, likes));

        // Danmaku sentiment distribution (last 10 min)
        if (danmakuRecordRepository != null) {
            Timestamp tenMinAgo = new Timestamp(System.currentTimeMillis() - 10 * 60 * 1000);
            var recentDanmaku = danmakuRecordRepository
                    .findBySessionIdAndDanmakuTimeAfterAndDeleted(sessionId, tenMinAgo, 0);
            if (!recentDanmaku.isEmpty()) {
                long positive = recentDanmaku.stream().filter(d -> "positive".equals(d.getSentiment())).count();
                long negative = recentDanmaku.stream().filter(d -> "negative".equals(d.getSentiment())).count();
                long total = recentDanmaku.size();
                int positivePct = (int) (positive * 100 / total);
                int negativePct = (int) (negative * 100 / total);
                context.append(String.format("过去10分钟弹幕 %d 条，正面 %d%%，负面 %d%%。", total, positivePct, negativePct));

                // Intent analysis
                if (danmakuAnalysisService != null) {
                    List<String> texts = recentDanmaku.stream().map(d -> d.getContent()).collect(Collectors.toList());
                    var intents = danmakuAnalysisService.analyzeBatchIntents(sessionId, texts);
                    int purchaseIntent = ((Number) intents.getOrDefault("purchase_intent", 0)).intValue();
                    if (purchaseIntent > total * 0.3) {
                        context.append("购买意向弹幕较多，建议加大推品力度。");
                    }
                    if (negativePct > 20) {
                        context.append("负面情绪偏高，建议安抚或切品。");
                    }
                }
            }
        }

        String contextStr = context.toString();
        // Store in Redis with session TTL
        if (redisTemplate != null) {
            String key = "live:online-learning:" + sessionId;
            redisTemplate.opsForValue().set(key, contextStr, Duration.ofHours(8));
        }

        log.debug("[OnlineLearning] 更新实时上下文: sessionId={}, context={}", sessionId, contextStr);
    }

    @Override
    public String getLearningContext(Long sessionId) {
        if (redisTemplate != null) {
            String key = "live:online-learning:" + sessionId;
            return redisTemplate.opsForValue().get(key);
        }
        return null;
    }

    @Override
    public void extractAndPersistInsights(Long sessionId) {
        if (crossSessionLearningService != null) {
            try {
                crossSessionLearningService.extractAndPersistInsights(sessionId);
            } catch (Exception e) {
                log.warn("[OnlineLearning] 跨场次学习提取失败: sessionId={}, err={}", sessionId, e.getMessage());
            }
        }
        // 场次结束时触发 prompt 自优化评估
        if (promptSelfOptimizationService != null) {
            var sessionOpt = sessionRepository.findById(sessionId);
            sessionOpt.ifPresent(session -> {
                try {
                    promptSelfOptimizationService.evaluateAndOptimize("live_script", session.getUserId());
                } catch (Exception e) {
                    log.debug("[OnlineLearning] prompt 自优化评估跳过: {}", e.getMessage());
                }
            });
        }
    }
}
