package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.ai.entity.AiLiveReview;
import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.repository.AiLiveReviewRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.ai.service.OperationalStrategyKnowledgeService;
import cn.gaifan.douyinOperations.module.live.entity.*;
import cn.gaifan.douyinOperations.module.live.repository.*;
import cn.gaifan.douyinOperations.module.live.service.LiveAnalysisService;
import cn.gaifan.douyinOperations.module.live.vo.LiveAnalysisVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveReviewVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    @Autowired(required = false)
    private OperationalStrategyKnowledgeService operationalStrategyKnowledgeService;

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
        writeSessionReflection(session, entity, productDataList, scripts, monitors, result);

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

    private void writeSessionReflection(LiveSession session, LiveSessionData sessionData,
                                        List<LiveProductData> productDataList, List<LiveScript> scripts,
                                        List<LiveMonitor> monitors, LiveAnalysisVO result) {
        if (operationalStrategyKnowledgeService == null || session == null || session.getUserId() == null
                || session.getUserId() <= 0 || result == null) {
            return;
        }
        try {
            String templateType = isSuccessful(result, sessionData) ? "live_success_template" : "live_failure_reason";
            String title = ("live_success_template".equals(templateType) ? "直播整场复盘-成功模板-" : "直播整场复盘-失败原因-")
                    + safe(session.getLiveTitle(), String.valueOf(session.getId()));
            String content = buildSessionReflectionContent(session, sessionData, productDataList, scripts, monitors, result, templateType);
            Map<String, String> metadata = new LinkedHashMap<>();
            metadata.put("source", "live_session_analysis");
            metadata.put("sessionId", String.valueOf(session.getId()));
            metadata.put("rating", safe(result.getRating(), ""));
            metadata.put("templateType", templateType);
            metadata.put("scriptCount", String.valueOf(scripts != null ? scripts.size() : 0));
            metadata.put("monitorPointCount", String.valueOf(monitors != null ? monitors.size() : 0));
            metadata.put("peakViewers", String.valueOf(sessionData != null && sessionData.getPeakViewers() != null ? sessionData.getPeakViewers() : 0));
            metadata.put("totalRevenue", String.valueOf(sessionData != null && sessionData.getTotalRevenue() != null ? sessionData.getTotalRevenue() : BigDecimal.ZERO));
            operationalStrategyKnowledgeService.writePerformanceReflection(session.getUserId(), title, content, metadata);
        } catch (Exception e) {
            log.debug("直播整场复盘知识回流跳过 sessionId={}, err={}", session.getId(), e.getMessage());
        }
    }

    private String buildSessionReflectionContent(LiveSession session, LiveSessionData sessionData,
                                                 List<LiveProductData> productDataList, List<LiveScript> scripts,
                                                 List<LiveMonitor> monitors, LiveAnalysisVO result, String templateType) {
        StringBuilder sb = new StringBuilder();
        sb.append("# 直播整场复盘：").append(safe(session.getLiveTitle(), "未命名场次")).append("\n\n");
        sb.append("## 复盘结论\n");
        sb.append("- 类型：").append("live_success_template".equals(templateType) ? "成功模板" : "失败原因").append("\n");
        sb.append("- 评分：").append(safe(result.getRating(), "未评分")).append("\n");
        sb.append("- 总结：").append(safe(result.getSummary(), "无")).append("\n\n");
        sb.append("## 核心数据\n");
        if (sessionData != null) {
            sb.append("- 总观看：").append(num(sessionData.getTotalViewers())).append("\n");
            sb.append("- 峰值在线：").append(num(sessionData.getPeakViewers())).append("\n");
            sb.append("- GMV：").append(money(sessionData.getTotalRevenue())).append("\n");
            sb.append("- 订单：").append(num(sessionData.getTotalOrders())).append("\n");
            sb.append("- 评论：").append(num(sessionData.getTotalComments())).append("\n");
            sb.append("- 点赞：").append(num(sessionData.getTotalLikes())).append("\n");
            sb.append("- 新增粉丝：").append(num(sessionData.getNewFollowers())).append("\n");
        } else {
            sb.append("- 暂无汇总数据\n");
        }
        sb.append("\n## 高分话术样本\n");
        List<LiveScript> topScripts = scripts == null ? List.of() : scripts.stream()
                .filter(s -> s.getEffectivenessScore() != null || StringUtils.hasText(s.getScriptContent()))
                .sorted(Comparator.comparing((LiveScript s) -> s.getEffectivenessScore() == null ? BigDecimal.ZERO : s.getEffectivenessScore()).reversed())
                .limit(5)
                .toList();
        if (topScripts.isEmpty()) {
            sb.append("- 无可用话术样本\n");
        } else {
            for (LiveScript script : topScripts) {
                sb.append("- 分数 ").append(money(script.getEffectivenessScore()))
                        .append(" / 类型 ").append(safe(script.getScriptType(), "unknown"))
                        .append(" / 内容：").append(truncate(script.getScriptContent(), 180)).append("\n");
            }
        }
        sb.append("\n## 商品表现\n");
        List<LiveProductData> products = productDataList == null ? List.of() : productDataList.stream()
                .sorted(Comparator.comparing((LiveProductData p) -> p.getRevenue() == null ? BigDecimal.ZERO : p.getRevenue()).reversed())
                .limit(8)
                .toList();
        if (products.isEmpty()) {
            sb.append("- 无商品数据\n");
        } else {
            for (LiveProductData product : products) {
                sb.append("- 商品 ").append(product.getProductId())
                        .append("：GMV ").append(money(product.getRevenue()))
                        .append("，订单 ").append(num(product.getOrders()))
                        .append("，转化率 ").append(money(product.getConversionRate())).append("\n");
            }
        }
        appendList(sb, "亮点", result.getHighlights());
        appendList(sb, "问题", result.getIssues());
        appendList(sb, "下一轮生成策略", result.getSuggestions());
        sb.append("\n## 监控数据轮廓\n");
        sb.append("- 监控点：").append(monitors != null ? monitors.size() : 0).append("\n");
        if (monitors != null && !monitors.isEmpty()) {
            LiveMonitor peak = monitors.stream()
                    .max(Comparator.comparing(m -> m.getOnlineCount() != null ? m.getOnlineCount() : 0))
                    .orElse(null);
            if (peak != null) {
                sb.append("- 最高在线点：").append(num(peak.getOnlineCount()))
                        .append("，GMV ").append(money(peak.getGmv()))
                        .append("，订单 ").append(num(peak.getOrders())).append("\n");
            }
        }
        return sb.toString();
    }

    private boolean isSuccessful(LiveAnalysisVO result, LiveSessionData sessionData) {
        String rating = result.getRating();
        if ("A".equalsIgnoreCase(rating) || "A+".equalsIgnoreCase(rating) || "B+".equalsIgnoreCase(rating)) {
            return true;
        }
        BigDecimal revenue = sessionData != null && sessionData.getTotalRevenue() != null ? sessionData.getTotalRevenue() : BigDecimal.ZERO;
        Integer orders = sessionData != null && sessionData.getTotalOrders() != null ? sessionData.getTotalOrders() : 0;
        return revenue.compareTo(BigDecimal.valueOf(5000)) >= 0 || orders >= 50;
    }

    private static void appendList(StringBuilder sb, String title, List<String> values) {
        sb.append("\n## ").append(title).append("\n");
        if (values == null || values.isEmpty()) {
            sb.append("- 无\n");
            return;
        }
        for (String value : values) {
            sb.append("- ").append(safe(value, "无")).append("\n");
        }
    }

    private static String safe(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private static String truncate(String value, int maxLength) {
        if (!StringUtils.hasText(value)) return "无";
        String text = value.trim().replaceAll("\\s+", " ");
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }

    private static String money(BigDecimal value) {
        return value != null ? value.stripTrailingZeros().toPlainString() : "0";
    }

    private static String num(Number value) {
        return value != null ? String.valueOf(value) : "0";
    }
}
