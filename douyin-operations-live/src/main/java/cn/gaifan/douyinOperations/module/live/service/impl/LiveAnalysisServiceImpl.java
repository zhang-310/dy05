package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.ai.entity.AiLiveReview;
import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.repository.AiLiveReviewRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.live.entity.*;
import cn.gaifan.douyinOperations.module.live.repository.*;
import cn.gaifan.douyinOperations.module.live.service.LiveAnalysisService;
import cn.gaifan.douyinOperations.module.live.vo.LiveAnalysisVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveReviewVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class LiveAnalysisServiceImpl implements LiveAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(LiveAnalysisServiceImpl.class);
    private static final String SYSTEM_PROMPT = """
            你是直播数据分析专家。根据提供的直播数据，输出严格的 JSON 格式分析报告。
            输出格式（不要其他说明，仅 JSON）：
            {
              "rating": "A/B+/B/C/D",
              "summary": "200字以内总结",
              "highlights": ["亮点1", "亮点2"],
              "issues": ["问题1", "问题2"],
              "suggestions": ["建议1", "建议2"]
            }
            """;

    @Resource
    private LiveSessionRepository sessionRepository;
    @Resource
    private LiveSessionDataRepository sessionDataRepository;
    @Resource
    private LiveProductDataRepository productDataRepository;
    @Resource
    private LiveScriptRepository scriptRepository;
    @Resource
    private LiveMonitorRepository monitorRepository;
    @Resource
    private AiModelRepository aiModelRepository;
    @Resource
    private AiLiveReviewRepository aiLiveReviewRepository;
    @Resource
    private LlmClient llmClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LiveAnalysisVO generate(Long sessionId) {
        LiveSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND, "直播场次不存在"));

        LiveSessionData sessionData = sessionDataRepository.findBySessionId(sessionId).orElse(null);
        List<LiveProductData> productDataList = productDataRepository.findBySessionId(sessionId);
        List<LiveScript> scripts = scriptRepository.findBySessionIdAndDeleted(sessionId, 0);
        List<LiveMonitor> monitors = monitorRepository.findBySessionIdOrderByTimestampAsc(sessionId);

        LiveAnalysisVO result = tryLlmAnalysis(session, sessionData, productDataList, scripts, monitors);
        if (result == null) {
            result = buildRuleBasedAnalysis(session, sessionData, productDataList, scripts, monitors);
        }

        String json = toJson(result);
        LiveSessionData entity = sessionDataRepository.findBySessionId(sessionId)
                .orElseGet(() -> {
                    LiveSessionData d = new LiveSessionData();
                    d.setSessionId(sessionId);
                    return d;
                });
        entity.setAiAnalysis(json);
        sessionDataRepository.save(entity);

        return result;
    }

    @Override
    public LiveAnalysisVO get(Long sessionId) {
        LiveSessionData data = sessionDataRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "场次数据不存在"));
        if (data.getAiAnalysis() == null || data.getAiAnalysis().isBlank()) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "暂无 AI 分析报告，请先生成");
        }
        return parseAnalysis(data.getAiAnalysis());
    }

    @Override
    public LiveReviewVO getReview(Long sessionId) {
        AiLiveReview review = null;
        LiveSessionData sessionData = sessionDataRepository.findBySessionId(sessionId).orElse(null);
        if (sessionData != null && sessionData.getAiReviewId() != null && sessionData.getAiReviewId() > 0) {
            review = aiLiveReviewRepository.findByIdAndDeleted(sessionData.getAiReviewId(), 0).orElse(null);
        }
        if (review == null) {
            review = aiLiveReviewRepository.findBySessionIdAndDeleted(sessionId, 0).orElse(null);
        }
        if (review == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "暂无 AI 复盘报告，请等待 AI 模块生成或联系管理员");
        }
        return toLiveReviewVO(review);
    }

    private LiveReviewVO toLiveReviewVO(AiLiveReview e) {
        LiveReviewVO vo = new LiveReviewVO();
        vo.setId(e.getId());
        vo.setSessionId(e.getSessionId());
        vo.setTotalViewers(e.getTotalViewers());
        vo.setTotalGmv(e.getTotalGmv());
        vo.setConversionRate(e.getConversionRate());
        vo.setPeakViewers(e.getPeakViewers());
        vo.setReportContent(e.getReportContent());
        vo.setTopScripts(e.getTopScripts());
        vo.setWeakPoints(e.getWeakPoints());
        vo.setStatus(e.getStatus());
        vo.setCreateTime(e.getCreateTime());
        return vo;
    }

    private LiveAnalysisVO tryLlmAnalysis(LiveSession session, LiveSessionData sessionData,
                                          List<LiveProductData> productDataList, List<LiveScript> scripts,
                                          List<LiveMonitor> monitors) {
        AiModel model = aiModelRepository.findByStatusAndDeleted(1, 0).stream().findFirst().orElse(null);
        if (model == null) return null;

        StringBuilder prompt = new StringBuilder("直播场次：").append(session.getLiveTitle()).append("\n");
        if (sessionData != null) {
            prompt.append("汇总：观众峰值=").append(sessionData.getPeakViewers())
                    .append(", 总点赞=").append(sessionData.getTotalLikes())
                    .append(", 总评论=").append(sessionData.getTotalComments())
                    .append(", 销售额=").append(sessionData.getTotalRevenue())
                    .append(", 订单=").append(sessionData.getTotalOrders()).append("\n");
        }
        if (!productDataList.isEmpty()) {
            prompt.append("商品数据：");
            for (LiveProductData p : productDataList) {
                prompt.append(" 商品ID").append(p.getProductId()).append(": 销售额=").append(p.getRevenue())
                        .append(", 转化率=").append(p.getConversionRate()).append(";");
            }
            prompt.append("\n");
        }
        prompt.append("话术条数：").append(scripts.size()).append("，监控数据点：").append(monitors.size());
        prompt.append("\n请基于以上数据生成分析报告。");

        try {
            LlmClient.LlmResponse resp = llmClient.chat(model, SYSTEM_PROMPT, prompt.toString());
            if (resp.success() && resp.content() != null && !resp.content().isBlank()) {
                String content = extractJson(resp.content());
                if (content != null) {
                    return parseAnalysis(content);
                }
            }
        } catch (Exception e) {
            log.warn("LLM 分析失败, 使用规则分析", e);
        }
        return null;
    }

    private String extractJson(String content) {
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return content.substring(start, end + 1);
        }
        return null;
    }

    private LiveAnalysisVO parseAnalysis(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            LiveAnalysisVO vo = new LiveAnalysisVO();
            vo.setRating(root.path("rating").asText("B"));
            vo.setSummary(root.path("summary").asText(""));
            vo.setHighlights(toList(root.path("highlights")));
            vo.setIssues(toList(root.path("issues")));
            vo.setSuggestions(toList(root.path("suggestions")));
            return vo;
        } catch (Exception e) {
            log.warn("解析分析 JSON 失败", e);
            return null;
        }
    }

    private List<String> toList(JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode n : node) {
                list.add(n.asText());
            }
        }
        return list;
    }

    private LiveAnalysisVO buildRuleBasedAnalysis(LiveSession session, LiveSessionData sessionData,
                                                   List<LiveProductData> productDataList,
                                                   List<LiveScript> scripts, List<LiveMonitor> monitors) {
        LiveAnalysisVO vo = new LiveAnalysisVO();
        StringBuilder summary = new StringBuilder();
        List<String> highlights = new ArrayList<>();
        List<String> issues = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();

        if (sessionData != null) {
            int peak = sessionData.getPeakViewers() != null ? sessionData.getPeakViewers() : 0;
            BigDecimal revenue = sessionData.getTotalRevenue() != null ? sessionData.getTotalRevenue() : BigDecimal.ZERO;
            int orders = sessionData.getTotalOrders() != null ? sessionData.getTotalOrders() : 0;

            summary.append("本场直播").append(session.getLiveTitle()).append("，");
            summary.append("峰值观众").append(peak).append("人，");
            summary.append("销售额").append(revenue).append("元，订单").append(orders).append("单。");

            if (peak > 1000) highlights.add("观众峰值表现良好");
            if (revenue.compareTo(BigDecimal.valueOf(5000)) > 0) highlights.add("销售额达标");
            if (peak < 100) issues.add("观众规模较小");
            if (scripts.isEmpty()) issues.add("未配置话术");
            suggestions.add("建议完善话术和选品");
        } else {
            summary.append("暂无汇总数据，请先同步直播数据。");
            issues.add("缺少直播汇总数据");
            suggestions.add("直播结束后点击「同步数据」");
        }

        vo.setRating(highlights.size() >= 2 ? "B+" : highlights.isEmpty() ? "C" : "B");
        vo.setSummary(summary.toString());
        vo.setHighlights(highlights.isEmpty() ? List.of("数据收集中") : highlights);
        vo.setIssues(issues.isEmpty() ? List.of("暂无") : issues);
        vo.setSuggestions(suggestions);
        return vo;
    }

    private String toJson(LiveAnalysisVO vo) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("rating", vo.getRating());
            root.put("summary", vo.getSummary());
            root.set("highlights", objectMapper.valueToTree(vo.getHighlights()));
            root.set("issues", objectMapper.valueToTree(vo.getIssues()));
            root.set("suggestions", objectMapper.valueToTree(vo.getSuggestions()));
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_BUSY, "序列化失败");
        }
    }
}
