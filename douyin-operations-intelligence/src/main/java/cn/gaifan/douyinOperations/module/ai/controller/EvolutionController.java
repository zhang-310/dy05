package cn.gaifan.douyinOperations.module.ai.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.ai.entity.AiEvolvePendingDeepen;
import cn.gaifan.douyinOperations.module.ai.entity.AiEvolveTopic;
import cn.gaifan.douyinOperations.module.ai.repository.AiEvolvePendingDeepenRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiEvolveReportRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiEvolveTaskRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiEvolveTopicRepository;
import cn.gaifan.douyinOperations.module.ai.repository.EvolveTaskSpecifications;
import cn.gaifan.douyinOperations.module.ai.service.DeepEvolveService;
import cn.gaifan.douyinOperations.module.ai.service.EvolutionService;
import cn.gaifan.douyinOperations.module.ai.service.EvolveEngineService;
import cn.gaifan.douyinOperations.module.ai.service.EvolveRoiService;
import cn.gaifan.douyinOperations.module.ai.service.EvolveTopicImportService;
import cn.gaifan.douyinOperations.module.ai.vo.VideoCompareRequestVO;
import cn.gaifan.douyinOperations.module.ai.vo.VideoCompareResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.sql.Timestamp;
import java.util.*;

import cn.gaifan.douyinOperations.module.ai.entity.AiEvolveTask;

import org.springframework.util.StringUtils;

@RestController
@RequestMapping("/api/v1/ai/evolution")
@Tag(name = "AI 进化引擎 / Evolution", description = "爆款拆解、直播复盘、索引队列（需登录）")
public class EvolutionController {

    private static final Logger log = LoggerFactory.getLogger(EvolutionController.class);

    @Resource
    private EvolutionService evolutionService;

    @Resource
    private EvolveEngineService evolveEngineService;

    @Resource
    private EvolveRoiService evolveRoiService;

    @Resource
    private EvolveTopicImportService evolveTopicImportService;

    @Resource
    private DeepEvolveService deepEvolveService;

    @Resource
    private AiEvolvePendingDeepenRepository pendingDeepenRepository;

    @Resource
    private AiEvolveTopicRepository evolveTopicRepository;

    @Resource
    private AiEvolveReportRepository evolveReportRepository;

    @Resource
    private AiEvolveTaskRepository aiEvolveTaskRepository;

    /** 与请求头 {@value #HEADER_VIRAL_CALLBACK} 比对；非空时允许无登录调用 {@code /viral/complete} */
    @Value("${app.ai.evolution.viral-callback-secret:}")
    private String viralCallbackSecret;

    private static final String HEADER_VIRAL_CALLBACK = "X-Viral-Callback-Secret";

    private static final String[] DEFAULT_EVOLVE_ANGLES = {
            "gap", "timeliness", "quality", "classify", "share", "deepen"
    };

    // ─── 爆款拆解 ────────────────────────────────────────────────

    @PostMapping("/viral/list")
    @Operation(summary = "爆款拆解列表")
    public RESTResult<PageResultVO<Map<String, Object>>> viralList(
            HttpServletRequest request,
            @RequestBody(required = false) Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        body = body == null ? Map.of() : body;
        Integer status = body.get("status") != null ? ((Number) body.get("status")).intValue() : null;
        int page = body.get("page") != null ? ((Number) body.get("page")).intValue() : 0;
        int rows = body.get("rows") != null ? Math.min(((Number) body.get("rows")).intValue(), 1000) : 20;
        RESTResult<PageResultVO<Map<String, Object>>> r = RESTResult.getSuccess(
                evolutionService.searchViralAnalysis(userId, status, page, rows));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/viral/get")
    @Operation(summary = "爆款拆解详情")
    public RESTResult<Map<String, Object>> viralGet(HttpServletRequest request, @RequestParam Long id) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(evolutionService.getViralAnalysis(id, userId));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/viral/trigger")
    @Operation(summary = "触发爆款拆解")
    public RESTResult<Long> viralTrigger(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Object v = body != null ? body.get("videoId") : null;
        if (v == null) return RESTResult.error(ErrorCode.INVALID_PARAMS, "videoId 不能为空");
        Long videoId = Long.parseLong(v.toString());
        Long accountId = body != null && body.get("accountId") != null ? Long.parseLong(body.get("accountId").toString()) : null;
        RESTResult<Long> r = RESTResult.addSuccess(evolutionService.triggerViralAnalysis(videoId, userId, accountId));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/viral/complete")
    @Operation(summary = "完成爆款拆解（登录用户须为记录 owner；或请求头 X-Viral-Callback-Secret 与 app.ai.evolution.viral-callback-secret 一致）")
    public RESTResult<Void> viralComplete(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        boolean secretOk = StringUtils.hasText(viralCallbackSecret)
                && viralCallbackSecret.equals(request.getHeader(HEADER_VIRAL_CALLBACK));
        Long userId = AuthTokenFilter.getUserId(request);
        if (!secretOk && userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录或无效回调密钥");
        }
        if (body == null || body.get("id") == null) {
            return RESTResult.error(ErrorCode.INVALID_PARAMS, "id 不能为空");
        }
        Long id = Long.parseLong(body.get("id").toString());
        Long tokens = body.get("tokensUsed") != null ? Long.parseLong(body.get("tokensUsed").toString()) : 0L;
        Integer quality = body.get("qualityScore") != null ? ((Number) body.get("qualityScore")).intValue() : 0;
        evolutionService.completeViralAnalysis(id, (String) body.get("reportContent"),
                (String) body.get("successFactors"), (String) body.get("replicableMethods"),
                quality, tokens, (String) body.get("modelUsed"), userId, secretOk);
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/viral/delete")
    @Operation(summary = "删除爆款拆解")
    public RESTResult<Void> viralDelete(HttpServletRequest request, @RequestParam Long id) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        evolutionService.deleteViralAnalysis(id, userId);
        RESTResult<Void> r = RESTResult.deleteSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    // ─── 进化任务队列 ────────────────────────────────────────────────

    @PostMapping("/task/list")
    @Operation(summary = "进化任务列表")
    public RESTResult<PageResultVO<Map<String, Object>>> taskList(
            HttpServletRequest request,
            @RequestBody(required = false) Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");

        body = body == null ? Map.of() : body;
        int page = body.get("page") != null ? ((Number) body.get("page")).intValue() : 0;
        int rows = body.get("rows") != null ? Math.min(((Number) body.get("rows")).intValue(), 100) : 20;
        String taskType = body.get("taskType") instanceof String s ? s : null;
        Long kbId = parseKbIdFromBody(body);
        String statusExact = null;
        Integer uiStatusCategory = null;
        Object st = body.get("status");
        if (st instanceof String s && !s.isBlank()) {
            statusExact = s;
        } else if (st instanceof Number n) {
            uiStatusCategory = n.intValue();
        }

        try {
            var spec = EvolveTaskSpecifications.listFilter(kbId, taskType, statusExact, uiStatusCategory);
            Page<AiEvolveTask> taskPage = aiEvolveTaskRepository.findAll(spec,
                    PageRequest.of(page, rows, Sort.by(Sort.Direction.DESC, "createTime")));
            List<AiEvolveTask> pageData = taskPage.getContent();
            long filteredTotal = taskPage.getTotalElements();

            // 转换为Map
            List<Map<String, Object>> result = pageData.stream().map(t -> {
                Map<String, Object> map = new java.util.LinkedHashMap<>();
                map.put("id", t.getId());
                map.put("taskNo", t.getTaskNo());
                map.put("taskType", t.getEvolveAngle());
                map.put("status", mapTaskStatus(t.getStatus()));
                map.put("progress", calculateProgress(t.getStatus()));
                map.put("kbId", t.getKbId());
                map.put("targetId", t.getKbId());
                map.put("createTime", t.getCreateTime());
                map.put("scoreTotal", t.getScoreTotal());
                map.put("scoreDetail", t.getScoreDetail());
                map.put("topicTexts", t.getTopicTexts());
                map.put("errorMessage", t.getErrorMessage());
                map.put("result", t.getErrorMessage());
                return map;
            }).toList();

            RESTResult<PageResultVO<Map<String, Object>>> r = RESTResult.getSuccess(
                    PageResultVO.of(filteredTotal, result, page, rows));
            r.setTraceId(MDC.get("traceId"));
            return r;
        } catch (Exception e) {
            log.error("查询进化任务列表失败", e);
            return RESTResult.error(ErrorCode.INTERNAL_ERROR, "查询失败: " + e.getMessage());
        }
    }

    private int mapTaskStatus(String status) {
        return switch (status) {
            case "pending", "gathering" -> 0;
            case "generating", "scoring", "expanding", "indexing", "pending_review" -> 1;
            case "completed" -> 2;
            case "failed", "blocked" -> 3;
            case "canceled" -> 4;
            default -> 0;
        };
    }

    private int calculateProgress(String status) {
        return switch (status) {
            case "pending" -> 0;
            case "gathering" -> 20;
            case "generating" -> 50;
            case "scoring" -> 70;
            case "expanding" -> 85;
            case "indexing" -> 95;
            case "pending_review" -> 98;
            case "completed" -> 100;
            case "canceled" -> 0;
            default -> 0;
        };
    }

    @PostMapping("/task/trigger")
    @Operation(summary = "触发进化任务")
    public RESTResult<Map<String, Object>> taskTrigger(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");

        String taskType = body != null && body.get("taskType") != null ? body.get("taskType").toString() : null;
        if (taskType == null || taskType.isBlank()) {
            return RESTResult.error(ErrorCode.INVALID_PARAMS, "taskType 不能为空");
        }

        Long targetKbId = parseLongBodyField(body, "targetKbId");
        if (targetKbId == null) {
            targetKbId = parseLongBodyField(body, "targetId");
        }

        try {
            // 调用进化引擎服务触发任务
            String jobId = UUID.randomUUID().toString().substring(0, 8);
            evolveEngineService.runEvolution(targetKbId, taskType, jobId);

            RESTResult<Map<String, Object>> r = RESTResult.getSuccess(Map.of(
                "taskId", jobId,
                "message", "进化任务已触发",
                "taskType", taskType
            ));
            r.setTraceId(MDC.get("traceId"));
            return r;
        } catch (Exception e) {
            log.error("触发进化任务失败: taskType={}, error={}", taskType, e.getMessage(), e);
            return RESTResult.error(ErrorCode.INTERNAL_ERROR, "触发失败: " + e.getMessage());
        }
    }

    @PostMapping("/task/cancel")
    @Operation(summary = "取消进化任务")
    public RESTResult<Void> taskCancel(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long taskId = body != null && body.get("taskId") != null ? ((Number) body.get("taskId")).longValue() : null;
        if (taskId == null) {
            return RESTResult.error(ErrorCode.INVALID_PARAMS, "taskId 不能为空");
        }
        evolveEngineService.cancelTask(taskId);
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    // ─── 直播复盘 ────────────────────────────────────────────────

    @PostMapping("/live-review/list")
    @Operation(summary = "直播复盘列表")
    public RESTResult<PageResultVO<Map<String, Object>>> liveReviewList(
            HttpServletRequest request,
            @RequestBody(required = false) Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        body = body == null ? Map.of() : body;
        Integer status = body.get("status") != null ? ((Number) body.get("status")).intValue() : null;
        int page = body.get("page") != null ? ((Number) body.get("page")).intValue() : 0;
        int rows = body.get("rows") != null ? Math.min(((Number) body.get("rows")).intValue(), 1000) : 20;
        RESTResult<PageResultVO<Map<String, Object>>> r = RESTResult.getSuccess(
                evolutionService.searchLiveReviews(userId, status, page, rows));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/live-review/get")
    @Operation(summary = "直播复盘详情")
    public RESTResult<Map<String, Object>> liveReviewGet(HttpServletRequest request, @RequestParam Long id) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(evolutionService.getLiveReview(id));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/live-review/trigger")
    @Operation(summary = "触发直播复盘")
    public RESTResult<Long> liveReviewTrigger(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Object v = body != null ? body.get("sessionId") : null;
        if (v == null) return RESTResult.error(ErrorCode.INVALID_PARAMS, "sessionId 不能为空");
        Long sessionId = Long.parseLong(v.toString());
        Long accountId = body != null && body.get("accountId") != null ? Long.parseLong(body.get("accountId").toString()) : null;
        RESTResult<Long> r = RESTResult.addSuccess(evolutionService.triggerLiveReview(sessionId, userId, accountId));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/live-review/complete")
    @Operation(summary = "完成直播复盘（内部回调）")
    public RESTResult<Void> liveReviewComplete(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long id = Long.parseLong(body.get("id").toString());
        Long tokens = body.get("tokensUsed") != null ? Long.parseLong(body.get("tokensUsed").toString()) : 0L;
        evolutionService.completeLiveReview(id, (String) body.get("reportContent"),
                (String) body.get("topScripts"), (String) body.get("weakPoints"),
                tokens, (String) body.get("modelUsed"));
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/live-review/delete")
    @Operation(summary = "删除直播复盘")
    public RESTResult<Void> liveReviewDelete(HttpServletRequest request, @RequestParam Long id) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        evolutionService.deleteLiveReview(id);
        RESTResult<Void> r = RESTResult.deleteSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    // ─── 视频对比分析 ────────────────────────────────────────────────

    @PostMapping("/video/compare")
    @Operation(summary = "视频对比分析")
    public RESTResult<VideoCompareResultVO> videoCompare(
            HttpServletRequest request,
            @Valid @RequestBody VideoCompareRequestVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        RESTResult<VideoCompareResultVO> r = RESTResult.getSuccess(evolutionService.compareVideos(vo, userId));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    // ─── 统计 ────────────────────────────────────────────────────

    @PostMapping("/stats")
    @Operation(summary = "进化引擎统计")
    public RESTResult<Map<String, Object>> stats(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(evolutionService.getEvolutionStats(userId));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    // ─── 知识进化 ROI & 趋势 ────────────────────────────────────────────────────

    @PostMapping("/roi")
    @Operation(summary = "知识进化 ROI 指标（body.kbId 可选，与趋势/热力图知识库范围一致）")
    public RESTResult<Map<String, Object>> roi(
            HttpServletRequest request,
            @RequestBody(required = false) Map<String, Object> body) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");

        try {
            Long kbId = parseKbIdFromBody(body);
            var spec = EvolveTaskSpecifications.kbIdOptional(kbId);
            Page<AiEvolveTask> slice = aiEvolveTaskRepository.findAll(spec,
                    PageRequest.of(0, 2000, Sort.by(Sort.Direction.DESC, "createTime")));
            List<AiEvolveTask> tasks = slice.getContent();

            // 计算 ROI 指标
            long totalTasks = tasks.size();
            long completedTasks = tasks.stream().filter(t -> "completed".equals(t.getStatus())).count();
            long newKnowledge = tasks.stream()
                .filter(t -> "completed".equals(t.getStatus()) && t.getScoreTotal() != null && t.getScoreTotal() >= 50)
                .count();

            double avgScore = tasks.stream()
                .filter(t -> t.getScoreTotal() != null && t.getScoreTotal() > 0)
                .mapToInt(AiEvolveTask::getScoreTotal)
                .average()
                .orElse(0.0);

            // 统计覆盖的知识库数量
            long coveredKbs = tasks.stream()
                .filter(t -> t.getKbId() != null)
                .map(AiEvolveTask::getKbId)
                .distinct()
                .count();

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("newKnowledge", newKnowledge);
            data.put("avgScore", Math.round(avgScore * 10) / 10.0);
            data.put("totalRuns", completedTasks);
            data.put("coveredDocs", coveredKbs);
            data.put("totalTasks", totalTasks);
            data.put("successRate", totalTasks > 0 ? Math.round((double) completedTasks / totalTasks * 100) : 0);

            RESTResult<Map<String, Object>> r = RESTResult.getSuccess(data);
            r.setTraceId(MDC.get("traceId"));
            return r;
        } catch (Exception e) {
            log.error("获取 ROI 指标失败: {}", e.getMessage(), e);
            return RESTResult.error(ErrorCode.INTERNAL_ERROR, "获取 ROI 指标失败: " + e.getMessage());
        }
    }

    @PostMapping("/score-trend")
    @Operation(summary = "知识质量分趋势")
    public RESTResult<List<Map<String, Object>>> scoreTrend(
            HttpServletRequest request,
            @RequestBody(required = false) Map<String, Object> body) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");

        try {
            int days = body != null && body.get("days") instanceof Number n ? n.intValue() : 7;
            if (days <= 0 || days > 90) days = 7;
            Long kbId = parseKbIdFromBody(body);
            List<Map<String, Object>> trend = buildDailyScoreTrend(days, kbId);
            RESTResult<List<Map<String, Object>>> r = RESTResult.getSuccess(trend);
            r.setTraceId(MDC.get("traceId"));
            return r;
        } catch (Exception e) {
            log.error("获取质量分趋势失败: {}", e.getMessage(), e);
            return RESTResult.error(ErrorCode.INTERNAL_ERROR, "获取质量分趋势失败: " + e.getMessage());
        }
    }

    /**
     * 按任务得分聚合的日历趋势（与 score-trend、quality-score/history 共用）。
     * kbId 非空时仅统计该知识库下的任务。
     */
    private List<Map<String, Object>> buildDailyScoreTrend(int days, Long kbId) {
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
        java.time.LocalDate endDate = java.time.LocalDate.now();
        java.time.LocalDate startDate = endDate.minusDays(days - 1);
        Timestamp since = Timestamp.valueOf(startDate.atStartOfDay());

        List<AiEvolveTask> tasks = evolveEngineService.listScoredTasksForTrendSince(since, kbId);
        Map<String, List<Integer>> scoresByDate = new LinkedHashMap<>();

        for (AiEvolveTask task : tasks) {
            if (task.getCreateTime() == null) {
                continue;
            }
            String date = task.getCreateTime().toLocalDateTime().toLocalDate().format(formatter);
            scoresByDate.computeIfAbsent(date, k -> new ArrayList<>()).add(task.getScoreTotal());
        }

        List<Map<String, Object>> trend = new ArrayList<>();
        for (java.time.LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            String dateStr = date.format(formatter);
            List<Integer> scores = scoresByDate.getOrDefault(dateStr, new ArrayList<>());
            double avgScore = scores.isEmpty() ? 0
                    : scores.stream().mapToInt(Integer::intValue).average().orElse(0.0);
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", dateStr);
            point.put("score", Math.round(avgScore * 10) / 10.0);
            point.put("count", scores.size());
            trend.add(point);
        }
        return trend;
    }

    private static Long parseLongBodyField(Map<String, Object> body, String key) {
        if (body == null || body.get(key) == null) {
            return null;
        }
        Object v = body.get(key);
        if (v instanceof Number n) {
            return n.longValue();
        }
        if (v instanceof String s && !s.isBlank()) {
            try {
                return Long.parseLong(s.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private static Long parseKbIdFromBody(Map<String, Object> body) {
        return parseLongBodyField(body, "kbId");
    }

    // ─── 门面：与前端 /ai/evolution/* 对齐（委托 admin/evolve 能力）────────────────

    @PostMapping("/status")
    @Operation(summary = "进化引擎仪表盘状态（供管理页头部）")
    public RESTResult<Map<String, Object>> evolveDashboardStatus(HttpServletRequest request) {
        if (AuthTokenFilter.getUserId(request) == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        try {
            List<AiEvolveTask> recent = evolveEngineService.listRecentTasks(30);
            boolean running = recent.stream().anyMatch(t -> {
                String s = t.getStatus() != null ? t.getStatus().trim().toLowerCase(Locale.ROOT) : "";
                return List.of("gathering", "generating", "scoring", "expanding", "indexing", "pending_review")
                        .contains(s);
            });
            String lastRunTime = "—";
            for (AiEvolveTask t : recent) {
                if ("completed".equalsIgnoreCase(String.valueOf(t.getStatus()).trim())) {
                    if (t.getUpdateTime() != null) {
                        lastRunTime = t.getUpdateTime().toString();
                    } else if (t.getCreateTime() != null) {
                        lastRunTime = t.getCreateTime().toString();
                    }
                    break;
                }
            }
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("running", running);
            data.put("circuitBroken", false);
            data.put("lastRunTime", lastRunTime);
            data.put("nextRunTime", "—");
            data.put("topicCount", evolveEngineService.listTopics(null, null, false).size());
            data.put("recentTasks", recent);
            data.put("scoreTrend", evolveEngineService.getScoreTrend(7));
            data.put("roiMetrics", evolveRoiService.getRoiMetrics(10));
            RESTResult<Map<String, Object>> r = RESTResult.getSuccess(data);
            r.setTraceId(MDC.get("traceId"));
            return r;
        } catch (Exception e) {
            log.error("进化状态概览失败: {}", e.getMessage(), e);
            return RESTResult.error(ErrorCode.INTERNAL_ERROR, "获取状态失败: " + e.getMessage());
        }
    }

    @PostMapping("/topic/list")
    @Operation(summary = "主题池列表（门面）")
    public RESTResult<List<AiEvolveTopic>> topicList(
            HttpServletRequest request,
            @RequestBody(required = false) Map<String, Object> body) {
        if (AuthTokenFilter.getUserId(request) == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        body = body == null ? Map.of() : body;
        Long kbId = parseKbIdFromBody(body);
        Long accountId = parseLongBodyField(body, "accountId");
        boolean scopeGlobal = Boolean.TRUE.equals(body.get("scopeGlobal"));
        return RESTResult.getSuccess(evolveEngineService.listTopics(kbId, accountId, scopeGlobal));
    }

    @PostMapping("/topic/save")
    @Operation(summary = "新增/编辑主题（门面）")
    public RESTResult<AiEvolveTopic> topicSave(HttpServletRequest request, @RequestBody AiEvolveTopic topic) {
        if (AuthTokenFilter.getUserId(request) == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        return RESTResult.addSuccess(evolveEngineService.saveTopic(topic));
    }

    @PostMapping("/topic/delete")
    @Operation(summary = "删除主题（门面）")
    public RESTResult<Void> topicDelete(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        if (AuthTokenFilter.getUserId(request) == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        Long id = body != null && body.get("id") instanceof Number n ? n.longValue() : null;
        if (id == null) {
            return RESTResult.error(ErrorCode.INVALID_PARAMS, "id 不能为空");
        }
        evolveEngineService.deleteTopic(id);
        RESTResult<Void> r = RESTResult.success("删除成功", null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/topic/import")
    @Operation(summary = "从文件导入主题（门面，filePath 即服务端 sourcePath）")
    public RESTResult<EvolveTopicImportService.TopicImportResult> topicImport(
            HttpServletRequest request,
            @RequestBody Map<String, Object> body) {
        if (AuthTokenFilter.getUserId(request) == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        String filePath = body != null && body.get("filePath") instanceof String s ? s : null;
        Long kbId = body != null && body.get("kbId") instanceof Number n ? n.longValue() : null;
        if (filePath == null || filePath.isBlank()) {
            return RESTResult.error(ErrorCode.INVALID_PARAMS, "filePath 不能为空");
        }
        EvolveTopicImportService.TopicImportResult result = evolveTopicImportService.importFromFile(filePath, kbId);
        RESTResult<EvolveTopicImportService.TopicImportResult> r = RESTResult.getSuccess(result);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/execution/execute")
    @Operation(summary = "立即运行进化（单 Agent 或 all）")
    public RESTResult<Map<String, Object>> executionExecute(
            HttpServletRequest request,
            @RequestBody Map<String, Object> body) {
        if (AuthTokenFilter.getUserId(request) == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        String agentType = body != null && body.get("agentType") != null ? body.get("agentType").toString() : null;
        if (agentType == null || agentType.isBlank()) {
            return RESTResult.error(ErrorCode.INVALID_PARAMS, "agentType 不能为空");
        }
        Long targetKbId = parseLongBodyField(body, "targetKbId");
        try {
            List<String> angles = "all".equalsIgnoreCase(agentType)
                    ? List.of(DEFAULT_EVOLVE_ANGLES)
                    : List.of(agentType);
            int started = 0;
            for (String angle : angles) {
                String jobId = UUID.randomUUID().toString().substring(0, 8);
                evolveEngineService.runEvolution(targetKbId, angle, jobId);
                started++;
            }
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("executionId", started);
            out.put("started", started);
            out.put("message", "已触发 " + started + " 个进化任务");
            RESTResult<Map<String, Object>> r = RESTResult.getSuccess(out);
            r.setTraceId(MDC.get("traceId"));
            return r;
        } catch (Exception e) {
            log.error("execution/execute 失败: {}", e.getMessage(), e);
            return RESTResult.error(ErrorCode.INTERNAL_ERROR, "执行失败: " + e.getMessage());
        }
    }

    @PostMapping("/execution/cancel")
    @Operation(summary = "取消运行中的进化任务（同 task/cancel）")
    public RESTResult<Void> executionCancel(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        return taskCancel(request, body);
    }

    @PostMapping("/execution/list")
    @Operation(summary = "执行记录列表（与任务队列同数据源）")
    public RESTResult<PageResultVO<Map<String, Object>>> executionList(
            HttpServletRequest request,
            @RequestBody(required = false) Map<String, Object> body) {
        return taskList(request, body);
    }

    @PostMapping("/execution/report")
    @Operation(summary = "按任务 ID 取进化报告摘要")
    public RESTResult<Map<String, Object>> executionReport(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        if (AuthTokenFilter.getUserId(request) == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        Long executionId = body != null && body.get("executionId") instanceof Number n ? n.longValue() : null;
        if (executionId == null) {
            return RESTResult.error(ErrorCode.INVALID_PARAMS, "executionId 不能为空");
        }
        return evolveReportRepository.findByTaskId(executionId)
                .map(rep -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rep.getId());
                    m.put("taskId", rep.getTaskId());
                    m.put("title", rep.getReportTitle());
                    m.put("summary", rep.getMethodologySection());
                    m.put("content", rep.getFullContent());
                    m.put("createTime", rep.getCreateTime());
                    RESTResult<Map<String, Object>> r = RESTResult.getSuccess(m);
                    r.setTraceId(MDC.get("traceId"));
                    return r;
                })
                .orElseGet(() -> RESTResult.error(ErrorCode.DATA_NOT_FOUND, "报告不存在"));
    }

    @PostMapping("/pending-deepen/list")
    @Operation(summary = "待深化问题分页列表")
    public RESTResult<PageResultVO<Map<String, Object>>> pendingDeepenList(
            HttpServletRequest request,
            @RequestBody(required = false) Map<String, Object> body) {
        if (AuthTokenFilter.getUserId(request) == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        body = body == null ? Map.of() : body;
        int page = body.get("page") instanceof Number n ? n.intValue() : 0;
        int rows = body.get("rows") instanceof Number n ? Math.min(n.intValue(), 100) : 20;
        Integer st = body.get("status") instanceof Number n ? n.intValue() : null;
        String statusFilter = st != null && st == 0 ? "pending" : st != null && st == 1 ? "done" : null;

        Page<AiEvolvePendingDeepen> pg;
        if (statusFilter != null) {
            pg = pendingDeepenRepository.findByDeletedAndStatusOrderByPriorityLevelAscCreateTimeAsc(
                    0, statusFilter, PageRequest.of(page, rows));
        } else {
            pg = pendingDeepenRepository.findByDeletedOrderByPriorityLevelAscCreateTimeAsc(0, PageRequest.of(page, rows));
        }
        List<Map<String, Object>> list = pg.getContent().stream().map(pd -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", pd.getId());
            m.put("taskId", pd.getTaskId());
            m.put("kbId", pd.getKbId());
            m.put("questionText", pd.getQuestionText());
            m.put("status", pd.getStatus());
            m.put("priorityLevel", pd.getPriorityLevel());
            m.put("createTime", pd.getCreateTime());
            return m;
        }).toList();
        RESTResult<PageResultVO<Map<String, Object>>> r = RESTResult.getSuccess(
                PageResultVO.of(pg.getTotalElements(), list, page, rows));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/pending-deepen/trigger")
    @Operation(summary = "按主题 ID 触发深度进化（使用主题归属 kbId，否则取默认可进化库）")
    public RESTResult<Map<String, Object>> pendingDeepenTrigger(
            HttpServletRequest request,
            @RequestBody Map<String, Object> body) {
        if (AuthTokenFilter.getUserId(request) == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        Long id = body != null && body.get("id") instanceof Number n ? n.longValue() : null;
        if (id == null) {
            return RESTResult.error(ErrorCode.INVALID_PARAMS, "id 不能为空");
        }
        AiEvolveTopic topic = evolveTopicRepository.findById(id).orElse(null);
        if (topic == null) {
            return RESTResult.error(ErrorCode.DATA_NOT_FOUND, "主题不存在");
        }
        Long kbId = topic.getKbId();
        if (kbId == null) {
            List<Long> kbs = evolveEngineService.resolveEvolveKbIds();
            kbId = kbs.isEmpty() ? null : kbs.get(0);
        }
        if (kbId == null) {
            return RESTResult.error(ErrorCode.INVALID_PARAMS, "无法解析知识库，请为主题绑定 kbId");
        }
        try {
            int processed = deepEvolveService.runDeepEvolve(kbId, 10);
            Map<String, Object> out = Map.of("processed", processed, "kbId", kbId);
            RESTResult<Map<String, Object>> r = RESTResult.getSuccess(out);
            r.setTraceId(MDC.get("traceId"));
            return r;
        } catch (Exception e) {
            log.error("pending-deepen/trigger 失败: {}", e.getMessage(), e);
            return RESTResult.error(ErrorCode.INTERNAL_ERROR, "触发失败: " + e.getMessage());
        }
    }

    @PostMapping("/quality-score/history")
    @Operation(summary = "质量分历史（按天，供热力图；与 score-trend 同源）")
    public RESTResult<List<Map<String, Object>>> qualityScoreHistory(
            HttpServletRequest request,
            @RequestBody(required = false) Map<String, Object> body) {
        if (AuthTokenFilter.getUserId(request) == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        int days = body != null && body.get("days") instanceof Number n ? n.intValue() : 90;
        if (days <= 0 || days > 365) {
            days = 90;
        }
        try {
            Long kbId = parseKbIdFromBody(body);
            List<Map<String, Object>> trend = buildDailyScoreTrend(days, kbId);
            RESTResult<List<Map<String, Object>>> r = RESTResult.getSuccess(trend);
            r.setTraceId(MDC.get("traceId"));
            return r;
        } catch (Exception e) {
            log.error("quality-score/history 失败: {}", e.getMessage(), e);
            return RESTResult.error(ErrorCode.INTERNAL_ERROR, "获取质量分历史失败: " + e.getMessage());
        }
    }
}
