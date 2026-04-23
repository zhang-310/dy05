package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.live.entity.LiveProduct;
import cn.gaifan.douyinOperations.module.live.entity.LiveScript;
import cn.gaifan.douyinOperations.module.live.entity.LiveSession;
import cn.gaifan.douyinOperations.module.live.repository.LiveProductRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
import cn.gaifan.douyinOperations.module.live.service.LiveRhythmOptimizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LiveRhythmOptimizerImpl implements LiveRhythmOptimizer {

    private static final Logger log = LoggerFactory.getLogger(LiveRhythmOptimizerImpl.class);

    @Autowired(required = false)
    private LiveSessionRepository liveSessionRepository;

    @Autowired(required = false)
    private LiveProductRepository liveProductRepository;

    @Autowired(required = false)
    private LiveScriptRepository liveScriptRepository;

    @Autowired(required = false)
    private LlmClient llmClient;

    @Autowired(required = false)
    private AiModelRepository aiModelRepository;

    @Override
    public Map<String, Object> optimizeSchedule(Long userId, Long sessionId) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "rhythm-optimization");

        if (liveSessionRepository == null || sessionId == null) {
            result.put("status", "unavailable");
            result.put("message", "场次数据不可用");
            return result;
        }

        LiveSession session = liveSessionRepository.findByIdAndDeleted(sessionId, 0).orElse(null);
        if (session == null) {
            result.put("status", "not_found");
            result.put("message", "场次不存在");
            return result;
        }

        List<LiveProduct> products = liveProductRepository != null
                ? liveProductRepository.findBySessionIdOrderByPositionAscIdAsc(sessionId)
                : List.of();

        List<LiveScript> scripts = liveScriptRepository != null
                ? liveScriptRepository.findBySessionIdAndDeletedOrderBySequenceNoAsc(sessionId, 0)
                : List.of();

        String productInfo = products.stream()
                .map(p -> String.format("- [位置%d] %s | 类型:%s | 销量:%d",
                        p.getPosition() != null ? p.getPosition() : 0,
                        p.getProductName(),
                        p.getProductType() != null ? p.getProductType() : "未分类",
                        p.getSaleQuantity() != null ? p.getSaleQuantity() : 0))
                .collect(Collectors.joining("\n"));

        String scriptInfo = scripts.stream()
                .map(s -> String.format("- [序号%d] 类型:%s | 得分:%s | 观众变化:%s",
                        s.getSequenceNo() != null ? s.getSequenceNo() : 0,
                        s.getScriptType() != null ? s.getScriptType() : "custom",
                        s.getEffectivenessScore() != null ? s.getEffectivenessScore().toString() : "未评",
                        s.getViewerDelta() != null ? s.getViewerDelta().toString() : "无数据"))
                .collect(Collectors.joining("\n"));

        String system = "你是抖音直播节奏优化专家。请根据场次商品列表和话术效果数据，分析每个时段的最佳安排。" +
                "输出 JSON 格式包含：\n" +
                "- recommendedProductOrder: 推荐商品顺序（数组，含 productName/suggestedDurationSec/role）\n" +
                "- interactionPoints: 穿插互动点（数组，含 afterProduct/interactionType/durationSec/purpose）\n" +
                "- overallDuration: 建议总时长（分钟）\n" +
                "- rhythmPattern: 节奏模式描述\n" +
                "- keyInsights: 关键洞察（数组）";
        String prompt = String.format("场次信息:\n- 商品数: %d\n- 话术数: %d\n\n商品列表:\n%s\n\n话术效果:\n%s",
                products.size(),
                scripts.size(),
                productInfo.isEmpty() ? "暂无商品" : productInfo,
                scriptInfo.isEmpty() ? "暂无话术数据" : scriptInfo);

        if (llmClient == null || aiModelRepository == null) {
            result.put("status", "unavailable");
            result.put("message", "LLM 服务未配置");
            return result;
        }
        try {
            List<AiModel> models = aiModelRepository.findByStatusAndDeleted(1, 0)
                    .stream().limit(3).toList();
            if (models.isEmpty()) {
                result.put("status", "no_model");
                result.put("message", "无可用 AI 模型");
                return result;
            }
            LlmClient.LlmResponse response = llmClient.chatWithFallback(models, system, prompt);
            if (response.success()) {
                result.put("status", "success");
                result.put("optimization", response.content());
                result.put("tokensUsed", response.tokensUsed());
            } else {
                result.put("status", "failed");
                result.put("message", response.errorMsg());
            }
        } catch (Exception e) {
            log.warn("LiveRhythmOptimizer LLM 调用失败: {}", e.getMessage());
            result.put("status", "error");
            result.put("message", e.getMessage());
        }
        return result;
    }
}
