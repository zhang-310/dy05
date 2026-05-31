package cn.gaifan.douyinOperations.module.ai.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.ai.service.CompetitorInsightService;
import cn.gaifan.douyinOperations.module.ai.service.brain.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import org.slf4j.MDC;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 行业大脑 API（Phase1/2）
 * 知识图谱、因果推理、趋势感知、用户画像、账号诊断、战略规划、风险预警、增长路径
 */
@RestController
@RequestMapping("/api/v1/ai/brain")
@Tag(name = "AI 行业大脑", description = "认知升级与决策赋能")
public class IndustryBrainController {

    @Value("${app.ai.brain.enabled:true}")
    private boolean brainEnabled;

    @Value("${app.ai.brain.knowledge-graph.max-hops:2}")
    private int knowledgeGraphMaxHops;

    @Resource
    private IndustryKnowledgeGraphService knowledgeGraphService;
    @Resource
    private IndustryCausalEngine causalEngine;
    @Resource
    private TrendMonitorService trendMonitorService;
    @Resource
    private UserCognitiveProfileService userProfileService;
    @Resource
    private AccountDiagnosisService accountDiagnosisService;
    @Resource
    private StrategicPlanningService strategicPlanningService;
    @Resource
    private RiskWarningService riskWarningService;
    @Resource
    private GrowthPathService growthPathService;
    @Resource
    private HostPersonaService hostPersonaService;
    @Resource
    private HostStyleConsistencyService hostStyleConsistencyService;
    @Resource
    private FiveHostsSynergyService fiveHostsSynergyService;
    @Resource
    private IpGrowthStageService ipGrowthStageService;
    @Resource
    private CompetitorInsightService competitorInsightService;

    private Long requireUserId(HttpServletRequest request) {
        Long uid = AuthTokenFilter.getUserId(request);
        if (uid == null) throw new cn.gaifan.douyinOperations.common.exception.BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        return uid;
    }

    @PostMapping("/knowledge-graph/query")
    @Operation(summary = "知识图谱查询")
    public RESTResult<List<Map<String, Object>>> knowledgeGraphQuery(
            HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long uid = requireUserId(request);
        if (!brainEnabled || !knowledgeGraphService.isAvailable()) {
            return RESTResult.getSuccess(List.of());
        }
        String entityType = body != null && body.get("entityType") != null ? body.get("entityType").toString() : "topic";
        String keyword = body != null && body.get("keyword") != null ? body.get("keyword").toString() : "";
        int limit = body != null && body.get("limit") instanceof Number n ? n.intValue() : 20;
        return RESTResult.getSuccess(knowledgeGraphService.queryEntities(entityType, keyword, limit, uid));
    }

    @PostMapping("/knowledge-graph/subgraph-json")
    @Operation(summary = "查询相关子图 JSON（G-5 nodes/edges + G-6 contradictions + G-2 推断边）")
    public RESTResult<Map<String, Object>> knowledgeGraphSubgraphJson(
            HttpServletRequest request, @RequestBody(required = false) Map<String, Object> body) {
        Long uid = requireUserId(request);
        if (!brainEnabled || !knowledgeGraphService.isAvailable()) {
            return RESTResult.getSuccess(Map.of("nodes", List.of(), "edges", List.of(), "contradictions", List.of()));
        }
        String query = body != null && body.get("query") != null ? body.get("query").toString() : "";

        // P0-2: 防止 Prompt 注入攻击
        query = cn.gaifan.douyinOperations.common.util.PromptInjectionDetector.sanitize(query);
        if (cn.gaifan.douyinOperations.common.util.PromptInjectionDetector.isSuspicious(query)) {
            org.slf4j.LoggerFactory.getLogger(IndustryBrainController.class)
                .warn("检测到疑似 Prompt 注入: userId={}, query={}", uid, query);
            throw new cn.gaifan.douyinOperations.common.exception.BusinessException(ErrorCode.INVALID_PARAMS, "输入包含不安全内容");
        }

        int limit = body != null && body.get("limit") instanceof Number n ? n.intValue() : 30;
        if (limit <= 0 || limit > 200) {
            limit = 30;
        }
        return RESTResult.getSuccess(knowledgeGraphService.getGraphJsonForQuery(query, uid, limit));
    }

    @PostMapping("/knowledge-graph/graphrag-context")
    @Operation(summary = "GraphRAG 多跳路径上下文（供 RAG 增强）")
    public RESTResult<Map<String, Object>> graphRagContext(
            HttpServletRequest request, @RequestBody(required = false) Map<String, Object> body) {
        Long uid = requireUserId(request);
        if (!brainEnabled || !knowledgeGraphService.isAvailable()) {
            return RESTResult.getSuccess(Map.of("context", "", "available", false));
        }
        String query = body != null && body.get("query") != null ? body.get("query").toString() : "";

        // P0-2: 防止 Prompt 注入攻击
        query = cn.gaifan.douyinOperations.common.util.PromptInjectionDetector.sanitize(query);
        if (cn.gaifan.douyinOperations.common.util.PromptInjectionDetector.isSuspicious(query)) {
            org.slf4j.LoggerFactory.getLogger(IndustryBrainController.class)
                .warn("检测到疑似 Prompt 注入: userId={}, query={}", uid, query);
            throw new cn.gaifan.douyinOperations.common.exception.BusinessException(ErrorCode.INVALID_PARAMS, "输入包含不安全内容");
        }

        int limit = body != null && body.get("limit") instanceof Number n ? n.intValue() : 20;
        String context = knowledgeGraphService.getGraphContextForQuery(query, uid, limit);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("context", context != null ? context : "");
        payload.put("available", true);
        payload.put("hops", knowledgeGraphMaxHops);
        return RESTResult.getSuccess(payload);
    }

    @PostMapping("/knowledge-graph/relation-suggestions/list")
    @Operation(summary = "G-2：图谱关系建议/人工校验队列")
    public RESTResult<List<Map<String, Object>>> relationSuggestionsList(
            HttpServletRequest request, @RequestBody(required = false) Map<String, Object> body) {
        Long uid = requireUserId(request);
        if (!brainEnabled) {
            return RESTResult.getSuccess(List.of());
        }
        String status = body != null && body.get("status") instanceof String s ? s : "pending";
        int limit = body != null && body.get("limit") instanceof Number n ? n.intValue() : 20;
        return RESTResult.getSuccess(knowledgeGraphService.listRelationSuggestions(uid, status, limit));
    }

    @PostMapping("/knowledge-graph/relation-suggestions/materialize")
    @Operation(summary = "G-2：将共现/规则边写入建议队列（pending，去重）")
    public RESTResult<java.util.Map<String, Object>> relationSuggestionsMaterialize(
            HttpServletRequest request, @RequestBody(required = false) Map<String, Object> body) {
        Long uid = requireUserId(request);
        if (!brainEnabled) {
            return RESTResult.getSuccess(Map.of("inserted", 0));
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> pairs = body != null && body.get("pairs") instanceof List<?> pl
                ? (List<Map<String, Object>>) pl : List.of();
        int inserted = knowledgeGraphService.materializeRelationSuggestions(uid, pairs);
        return RESTResult.getSuccess(Map.of("inserted", inserted));
    }

    @PostMapping("/knowledge-graph/relation-suggestions/update-status")
    @Operation(summary = "G-2：审核关系建议（approved / rejected）")
    public RESTResult<Void> relationSuggestionsUpdateStatus(
            HttpServletRequest request, @RequestBody(required = false) Map<String, Object> body) {
        Long uid = requireUserId(request);
        if (!brainEnabled) {
            return RESTResult.getSuccess(null);
        }
        Long id = body != null && body.get("id") instanceof Number n ? n.longValue() : null;
        String status = body != null && body.get("status") instanceof String s ? s : null;
        knowledgeGraphService.updateRelationSuggestionStatus(uid, id, status);
        RESTResult<Void> r = RESTResult.getSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/causal/infer")
    @Operation(summary = "因果推理")
    public RESTResult<IndustryCausalEngine.CausalInferenceResult> causalInfer(
            HttpServletRequest request, @RequestBody Map<String, Object> body) {
        requireUserId(request);
        if (!brainEnabled || !causalEngine.isAvailable()) {
            return RESTResult.getSuccess(new IndustryCausalEngine.CausalInferenceResult(0, List.of(), List.of(), "未启用"));
        }
        return RESTResult.getSuccess(causalEngine.infer(body != null ? body : Map.of()));
    }

    @PostMapping("/host-personas")
    @Operation(summary = "五位主播人设列表")
    public RESTResult<List<cn.gaifan.douyinOperations.module.ai.entity.AiHostPersona>> hostPersonas(
            HttpServletRequest request) {
        requireUserId(request);
        if (!brainEnabled || hostPersonaService == null || !hostPersonaService.isAvailable()) {
            return RESTResult.getSuccess(List.of());
        }
        return RESTResult.getSuccess(hostPersonaService.listAll());
    }

    @PostMapping("/trends/for-host")
    @Operation(summary = "五位主播个性化趋势推荐")
    public RESTResult<List<TrendMonitorService.TrendSignal>> trendsForHost(
            HttpServletRequest request, @RequestBody(required = false) Map<String, Object> body) {
        requireUserId(request);
        if (!brainEnabled || !trendMonitorService.isAvailable()) {
            return RESTResult.getSuccess(List.of());
        }
        String hostCode = body != null && body.get("hostCode") != null ? body.get("hostCode").toString() : null;
        int limit = body != null && body.get("limit") instanceof Number n ? n.intValue() : 20;
        return RESTResult.getSuccess(trendMonitorService.getTrendsForHost(hostCode, limit));
    }

    @PostMapping("/trends/current")
    @Operation(summary = "当前趋势列表")
    public RESTResult<List<TrendMonitorService.TrendSignal>> trendsCurrent(
            HttpServletRequest request, @RequestBody(required = false) Map<String, Object> body) {
        requireUserId(request);
        if (!brainEnabled || !trendMonitorService.isAvailable()) {
            return RESTResult.getSuccess(List.of());
        }
        String category = body != null && body.get("category") != null ? body.get("category").toString() : null;
        int limit = body != null && body.get("limit") instanceof Number n ? n.intValue() : 30;
        return RESTResult.getSuccess(trendMonitorService.getCurrentTrends(category, limit));
    }

    @PostMapping("/user-profile")
    @Operation(summary = "用户认知画像")
    public RESTResult<UserCognitiveProfileService.UserProfile> userProfile(
            HttpServletRequest request, @RequestBody(required = false) Map<String, Object> body) {
        Long userId = requireUserId(request);
        if (!brainEnabled || !userProfileService.isAvailable()) {
            return RESTResult.getSuccess(new UserCognitiveProfileService.UserProfile(userId, Map.of(), List.of(), 0, Map.of(), 0));
        }
        // Support optional accountId parameter
        Long targetUserId = body != null && body.get("accountId") != null
            ? ((Number) body.get("accountId")).longValue()
            : userId;
        return RESTResult.getSuccess(userProfileService.getProfile(targetUserId));
    }

    @PostMapping("/user-profile/{userId}")
    @Operation(summary = "用户认知画像（路径参数版本）")
    public RESTResult<UserCognitiveProfileService.UserProfile> userProfileById(
            HttpServletRequest request, @PathVariable Long userId) {
        Long me = requireUserId(request);
        if (!brainEnabled || !userProfileService.isAvailable()) {
            return RESTResult.getSuccess(new UserCognitiveProfileService.UserProfile(userId, Map.of(), List.of(), 0, Map.of(), 0));
        }
        if (!me.equals(userId)) {
            throw new cn.gaifan.douyinOperations.common.exception.BusinessException(ErrorCode.FORBIDDEN, "无权限查看他人画像");
        }
        return RESTResult.getSuccess(userProfileService.getProfile(userId));
    }

    @PostMapping("/industry/insights")
    @Operation(summary = "行业洞察")
    public RESTResult<Map<String, Object>> industryInsights(
            HttpServletRequest request, @RequestBody(required = false) Map<String, Object> body) {
        Long userId = requireUserId(request);
        if (!brainEnabled) {
            return RESTResult.getSuccess(Map.of());
        }
        String category = body != null && body.get("category") != null ? body.get("category").toString() : "护肤";
        String normalizedCategory = "全品类".equals(category) ? null : category;

        List<TrendMonitorService.TrendSignal> trendSignals =
                trendMonitorService != null && trendMonitorService.isAvailable()
                        ? trendMonitorService.getCurrentTrends(normalizedCategory, 8)
                        : List.of();
        List<Map<String, Object>> recentInsights = competitorInsightService != null
                ? competitorInsightService.getRecentInsights(category, 7)
                : List.of();
        String differentiationAdvice = competitorInsightService != null
                ? competitorInsightService.getDifferentiationAdvice(category)
                : "";
        UserCognitiveProfileService.UserProfile profile =
                userProfileService != null && userProfileService.isAvailable()
                        ? userProfileService.getProfile(userId)
                        : null;

        List<String> hotTopics = trendSignals.stream()
                .map(TrendMonitorService.TrendSignal::title)
                .filter(s -> s != null && !s.isBlank())
                .limit(6)
                .toList();
        List<String> trendSources = trendSignals.stream()
                .map(TrendMonitorService.TrendSignal::source)
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .toList();
        List<String> competitorSummaries = recentInsights.stream()
                .map(m -> String.valueOf(m.getOrDefault("summary", m.getOrDefault("content", ""))))
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.length() > 120 ? s.substring(0, 120) + "..." : s)
                .limit(6)
                .toList();
        List<String> profileFocus = profile != null && profile.contentPreferences() != null
                ? profile.contentPreferences().entrySet().stream()
                .filter(e -> !String.valueOf(e.getKey()).startsWith("_"))
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .limit(4)
                .collect(Collectors.toList())
                : List.of();

        List<String> actions = new java.util.ArrayList<>();
        if (!hotTopics.isEmpty()) actions.add("围绕最近高热词快速产出选题与直播切片");
        if (!competitorSummaries.isEmpty()) actions.add("针对近 7 天竞品洞察做差异化表达与定价策略复核");
        if (!profileFocus.isEmpty()) actions.add("优先围绕用户当前偏好的内容方向进行选题与脚本优化");
        if (actions.isEmpty()) actions.add("当前缺少足够趋势/竞品/画像数据，优先补齐样本后再制定策略");

        Map<String, Object> insights = new LinkedHashMap<>();
        insights.put("行业分类", category);
        insights.put("趋势热点", hotTopics);
        insights.put("趋势来源", trendSources);
        insights.put("近7天竞品洞察", competitorSummaries);
        insights.put("差异化建议", differentiationAdvice != null && !differentiationAdvice.isBlank()
                ? differentiationAdvice
                : "暂无足够竞品洞察，建议先补齐竞品样本与热点数据");
        insights.put("用户偏好焦点", profileFocus);
        insights.put("优先动作", actions);
        insights.put("数据口径", "基于实时趋势、竞品洞察与当前用户画像聚合，不再返回固定模拟市场数据");
        return RESTResult.getSuccess(insights);
    }

    @PostMapping("/account/diagnose")
    @Operation(summary = "账号诊断")
    public RESTResult<AccountDiagnosisService.DiagnosisResult> accountDiagnose(
            HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long userId = requireUserId(request);
        if (!brainEnabled || !accountDiagnosisService.isAvailable()) {
            return RESTResult.error(ErrorCode.INVALID_PARAMS, "账号诊断未启用");
        }
        Object v = body != null ? body.get("accountId") : null;
        if (v == null) throw new cn.gaifan.douyinOperations.common.exception.BusinessException(ErrorCode.INVALID_PARAMS, "accountId 不能为空");
        Long accountId = Long.parseLong(v.toString());
        return RESTResult.getSuccess(accountDiagnosisService.diagnose(accountId, userId));
    }

    @PostMapping("/strategic/plan")
    @Operation(summary = "战略规划")
    public RESTResult<StrategicPlanningService.StrategicPlan> strategicPlan(
            HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long userId = requireUserId(request);
        if (!brainEnabled || !strategicPlanningService.isAvailable()) {
            return RESTResult.error(ErrorCode.INVALID_PARAMS, "战略规划未启用");
        }
        Long accountId = body != null && body.get("accountId") != null ? Long.parseLong(body.get("accountId").toString()) : null;
        @SuppressWarnings("unchecked")
        List<String> goals = body != null && body.get("goals") instanceof List ? (List<String>) body.get("goals") : List.of("万粉突破", "5万粉");
        return RESTResult.getSuccess(strategicPlanningService.generate(accountId, userId, goals, body));
    }

    @PostMapping("/risk/warn")
    @Operation(summary = "风险预警")
    public RESTResult<List<RiskWarningService.RiskItem>> riskWarn(
            HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long userId = requireUserId(request);
        if (!brainEnabled || !riskWarningService.isAvailable()) {
            return RESTResult.getSuccess(List.of());
        }
        String content = body != null && body.get("content") != null ? body.get("content").toString() : "";
        return RESTResult.getSuccess(riskWarningService.warn(content, userId));
    }

    @PostMapping("/style-consistency")
    @Operation(summary = "多模态风格一致性")
    public RESTResult<Map<String, Object>> styleConsistency(
            HttpServletRequest request, @RequestBody(required = false) Map<String, Object> body) {
        requireUserId(request);
        if (!brainEnabled || !hostStyleConsistencyService.isAvailable()) {
            return RESTResult.getSuccess(Map.of("prompt", "", "score", 1.0));
        }
        String hostCode = body != null && body.get("hostCode") != null ? body.get("hostCode").toString() : null;
        String content = body != null && body.get("content") != null ? body.get("content").toString() : null;
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("prompt", hostStyleConsistencyService.getTextStylePrompt(hostCode));
        result.put("styleVector", hostStyleConsistencyService.getStyleVector(hostCode));
        result.put("score", content != null ? hostStyleConsistencyService.getStyleConsistencyScore(hostCode, content) : 1.0);
        return RESTResult.getSuccess(result);
    }

    @PostMapping("/synergy")
    @Operation(summary = "五位主播协同摘要")
    public RESTResult<Map<String, Object>> synergy(HttpServletRequest request) {
        requireUserId(request);
        if (!brainEnabled || !fiveHostsSynergyService.isAvailable()) {
            return RESTResult.getSuccess(Map.of());
        }
        return RESTResult.getSuccess(fiveHostsSynergyService.getSynergySummary());
    }

    @PostMapping("/growth-path")
    @Operation(summary = "增长路径生成")
    public RESTResult<GrowthPathService.GrowthPathResult> growthPath(
            HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long userId = requireUserId(request);
        if (!brainEnabled || !growthPathService.isAvailable()) {
            return RESTResult.error(ErrorCode.INVALID_PARAMS, "增长路径未启用");
        }
        Long accountId = body != null && body.get("accountId") != null ? Long.parseLong(body.get("accountId").toString()) : null;
        @SuppressWarnings("unchecked")
        Map<String, Object> currentState = body != null && body.get("currentState") instanceof Map ? (Map<String, Object>) body.get("currentState") : Map.of();
        long targetFans = body != null && body.get("targetFans") instanceof Number n ? n.longValue() : 100000;
        return RESTResult.getSuccess(growthPathService.generate(accountId, userId, currentState, targetFans));
    }

    @PostMapping("/content-diagnosis")
    @Operation(summary = "内容诊断：分析话术效果与行业对比")
    public RESTResult<Map<String, Object>> contentDiagnosis(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = requireUserId(request);
        if (!brainEnabled || !accountDiagnosisService.isAvailable()) {
            return RESTResult.error(ErrorCode.INVALID_PARAMS, "行业大脑未启用");
        }
        String category = (String) body.getOrDefault("category", "general");
        Map<String, Object> result = accountDiagnosisService.diagnoseContent(userId, category);
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(result);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/product-diagnosis")
    @Operation(summary = "选品诊断：分析商品讲解策略")
    public RESTResult<Map<String, Object>> productDiagnosis(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = requireUserId(request);
        if (!brainEnabled || !accountDiagnosisService.isAvailable()) {
            return RESTResult.error(ErrorCode.INVALID_PARAMS, "行业大脑未启用");
        }
        Map<String, Object> result = accountDiagnosisService.diagnoseProductStrategy(userId);
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(result);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/rhythm-diagnosis")
    @Operation(summary = "节奏诊断：分析直播时段观众流失")
    public RESTResult<Map<String, Object>> rhythmDiagnosis(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = requireUserId(request);
        if (!brainEnabled || !accountDiagnosisService.isAvailable()) {
            return RESTResult.error(ErrorCode.INVALID_PARAMS, "行业大脑未启用");
        }
        Map<String, Object> result = accountDiagnosisService.diagnoseRhythm(userId);
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(result);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/ip-growth-stage")
    @Operation(summary = "IP增长阶段判断与策略建议")
    public RESTResult<Map<String, Object>> ipGrowthStage(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        requireUserId(request);
        String ipType = body.get("ipType") instanceof String s ? s : "phenomenal";
        long followerCount = body.get("followerCount") instanceof Number n ? n.longValue() : 0;
        int operatingMonths = body.get("operatingMonths") instanceof Number n ? n.intValue() : 0;

        String stage = "phenomenal".equals(ipType)
                ? ipGrowthStageService.getPhenomenalStage(followerCount)
                : ipGrowthStageService.getTopStage(operatingMonths);

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("stage", stage);
        result.put("strategy", ipGrowthStageService.getStageStrategy(ipType, stage));
        result.put("metricsBaseline", ipGrowthStageService.getMetricsBaseline(ipType));
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(result);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/ip-metrics-baseline")
    @Operation(summary = "IP运营指标基线（效果评估参考）")
    public RESTResult<Map<String, Object>> ipMetricsBaseline(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        requireUserId(request);
        String ipType = body.get("ipType") instanceof String s ? s : "phenomenal";
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(ipGrowthStageService.getMetricsBaseline(ipType));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    // ─── P1 补全：遗漏的 Brain 端点 ───

    /**
     * 因果反事实推理
     * POST /api/v1/ai/brain/causal/counterfactual
     * { "currentState": {...}, "intervention": {...} }
     */
    @PostMapping("/causal/counterfactual")
    @Operation(summary = "因果反事实推理：改变某因素后的预期转化率变化")
    public RESTResult<cn.gaifan.douyinOperations.module.ai.vo.CounterfactualResultVO> causalCounterfactual(
            HttpServletRequest request, @RequestBody(required = false) Map<String, Object> body) {
        requireUserId(request);
        if (!brainEnabled || !causalEngine.isAvailable()) {
            return RESTResult.error(ErrorCode.INVALID_PARAMS, "因果引擎未启用");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> currentState = body != null && body.get("currentState") instanceof Map m
                ? (Map<String, Object>) m : Map.of();
        @SuppressWarnings("unchecked")
        Map<String, Object> intervention = body != null && body.get("intervention") instanceof Map m
                ? (Map<String, Object>) m : Map.of();
        return RESTResult.getSuccess(causalEngine.counterfactual(currentState, intervention));
    }

    /**
     * 策略解释
     * POST /api/v1/ai/brain/causal/explain-strategy
     * { "strategyId": "xxx", "context": {...} }
     */
    @PostMapping("/causal/explain-strategy")
    @Operation(summary = "策略解释：说明某组合效果好/差的原因")
    public RESTResult<Map<String, Object>> causalExplainStrategy(
            HttpServletRequest request, @RequestBody(required = false) Map<String, Object> body) {
        requireUserId(request);
        if (!brainEnabled || !causalEngine.isAvailable()) {
            return RESTResult.error(ErrorCode.INVALID_PARAMS, "因果引擎未启用");
        }
        String strategyId = body != null && body.get("strategyId") instanceof String s ? s : "";
        @SuppressWarnings("unchecked")
        Map<String, Object> context = body != null && body.get("context") instanceof Map m
                ? (Map<String, Object>) m : Map.of();
        String explanation = causalEngine.explainStrategy(strategyId, context);
        return RESTResult.getSuccess(Map.of("strategyId", strategyId, "explanation", explanation));
    }

    /**
     * 带生命周期的趋势列表
     * POST /api/v1/ai/brain/trends/with-lifecycle
     */
    @PostMapping("/trends/with-lifecycle")
    @Operation(summary = "趋势列表（含生命周期与时间窗口，Phase 3.4）")
    public RESTResult<List<cn.gaifan.douyinOperations.module.ai.vo.TrendPredictionVO>> trendsWithLifecycle(
            HttpServletRequest request, @RequestBody(required = false) Map<String, Object> body) {
        requireUserId(request);
        if (!brainEnabled || !trendMonitorService.isAvailable()) {
            return RESTResult.getSuccess(List.of());
        }
        String category = body != null && body.get("category") instanceof String s ? s : null;
        int limit = body != null && body.get("limit") instanceof Number n ? n.intValue() : 20;
        return RESTResult.getSuccess(trendMonitorService.getTrendsWithLifecycle(category, limit));
    }

    /**
     * 检测新趋势
     * POST /api/v1/ai/brain/trends/detect-new
     */
    @PostMapping("/trends/detect-new")
    @Operation(summary = "检测新出现的趋势（可触发知识进化）")
    public RESTResult<List<TrendMonitorService.TrendSignal>> detectNewTrends(HttpServletRequest request) {
        requireUserId(request);
        if (!brainEnabled || !trendMonitorService.isAvailable()) {
            return RESTResult.getSuccess(List.of());
        }
        return RESTResult.getSuccess(trendMonitorService.detectNewTrends());
    }

    /**
     * 批量账号诊断
     * POST /api/v1/ai/brain/account/diagnose-batch
     * { "accountIds": [1,2,3] }
     */
    @PostMapping("/account/diagnose-batch")
    @Operation(summary = "批量账号诊断（机构多账号）")
    public RESTResult<List<AccountDiagnosisService.DiagnosisResult>> accountDiagnoseBatch(
            HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long userId = requireUserId(request);
        if (!brainEnabled || !accountDiagnosisService.isAvailable()) {
            return RESTResult.error(ErrorCode.INVALID_PARAMS, "账号诊断未启用");
        }
        @SuppressWarnings("unchecked")
        List<Number> rawIds = body.get("accountIds") instanceof List l ? (List<Number>) l : List.of();
        List<Long> accountIds = rawIds.stream().map(Number::longValue).toList();
        if (accountIds.isEmpty()) return RESTResult.getSuccess(List.of());
        return RESTResult.getSuccess(accountDiagnosisService.diagnoseBatch(accountIds, userId));
    }

    /**
     * 批量风险检测
     * POST /api/v1/ai/brain/risk/warn-batch
     * { "contents": ["话术1", "话术2"] }
     */
    @PostMapping("/risk/warn-batch")
    @Operation(summary = "批量风险检测（多条话术同时检测）")
    public RESTResult<List<RiskWarningService.RiskItem>> riskWarnBatch(
            HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long userId = requireUserId(request);
        if (!brainEnabled || !riskWarningService.isAvailable()) {
            return RESTResult.getSuccess(List.of());
        }
        @SuppressWarnings("unchecked")
        List<String> contents = body.get("contents") instanceof List l ? (List<String>) l : List.of();
        return RESTResult.getSuccess(riskWarningService.warnBatch(contents, userId));
    }

    /**
     * 风险预警统计
     * POST /api/v1/ai/brain/risk/stats
     */
    @PostMapping("/risk/stats")
    @Operation(summary = "风险预警准确率等统计（监控用）")
    public RESTResult<RiskWarningService.RiskStats> riskStats(HttpServletRequest request) {
        requireUserId(request);
        if (!brainEnabled || !riskWarningService.isAvailable()) {
            return RESTResult.getSuccess(new RiskWarningService.RiskStats(0, 0, 0.0));
        }
        return RESTResult.getSuccess(riskWarningService.getStats());
    }
}
