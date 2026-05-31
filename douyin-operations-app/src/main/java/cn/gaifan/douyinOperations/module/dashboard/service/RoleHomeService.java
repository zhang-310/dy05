package cn.gaifan.douyinOperations.module.dashboard.service;

import cn.gaifan.douyinOperations.common.tenant.TenantOrgResolutionHelper;
import cn.gaifan.douyinOperations.contract.auth.DataScopeResolver;
import cn.gaifan.douyinOperations.module.ai.repository.AiCallLogRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiIndexQueueRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiKbDocumentRepository;
import cn.gaifan.douyinOperations.module.ai.service.AiDashboardService;
import cn.gaifan.douyinOperations.module.benchmark.repository.BenchmarkVideoRepository;
import cn.gaifan.douyinOperations.module.live.entity.LiveSession;
import cn.gaifan.douyinOperations.module.live.service.LiveApprovalService;
import cn.gaifan.douyinOperations.module.shortvideo.service.ShortVideoDashboardService;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvProjectRepository;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvViralVideoRepository;
import cn.gaifan.douyinOperations.module.tianapi.entity.TianApiImportRun;
import cn.gaifan.douyinOperations.module.tianapi.repository.TianApiImportCategoryStatRepository;
import cn.gaifan.douyinOperations.module.tianapi.repository.TianApiImportRunRepository;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Role home BFF: aggregates role dashboard data and exposes the effective data boundary.
 */
@Service
public class RoleHomeService {

    @Resource
    private DashboardService dashboardService;

    @Resource
    private DataScopeResolver dataScopeResolver;

    @Resource
    private TenantOrgResolutionHelper tenantOrgResolutionHelper;

    @Resource(name = "shortVideoDashboardServiceImpl")
    private ShortVideoDashboardService shortVideoDashboardService;

    @Autowired(required = false)
    private AiDashboardService aiDashboardService;

    @Autowired(required = false)
    private LiveApprovalService liveApprovalService;

    @Autowired(required = false)
    private AiKbDocumentRepository aiKbDocumentRepository;

    @Autowired(required = false)
    private AiCallLogRepository aiCallLogRepository;

    @Autowired(required = false)
    private AiIndexQueueRepository aiIndexQueueRepository;

    @Autowired(required = false)
    private TianApiImportRunRepository tianApiImportRunRepository;

    @Autowired(required = false)
    private TianApiImportCategoryStatRepository tianApiImportCategoryStatRepository;

    @Autowired(required = false)
    private SvViralVideoRepository svViralVideoRepository;

    @Autowired(required = false)
    private BenchmarkVideoRepository benchmarkVideoRepository;

    @Autowired(required = false)
    private SvProjectRepository svProjectRepository;

    public Map<String, Object> buildHome(String role, Long userId, String roleCode, Long requestOrgId) {
        List<Long> visibleOwnerIds = dataScopeResolver.getVisibleUserIds(userId, roleCode);
        Long organizationId = requestOrgId != null ? requestOrgId : tenantOrgResolutionHelper.organizationIdForUser(userId);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("role", role);
        data.put("userId", userId);
        data.put("roleCode", roleCode);
        data.put("organizationId", organizationId);
        data.put("visibleOwnerIds", visibleOwnerIds);
        data.put("boundary", boundary(role, userId, roleCode, organizationId, visibleOwnerIds));
        data.put("metrics", metrics(role, userId));
        data.put("sections", sections(role, userId));
        data.put("businessChains", businessChains(role, userId));
        data.put("knowledgeGuard", knowledgeGuard(role));
        data.put("learningLoops", learningLoops(role, userId));
        data.put("readyEndpoints", readyEndpoints(role));
        data.put("generatedAt", OffsetDateTime.now().toString());
        return data;
    }

    private Map<String, Object> metrics(String role, Long userId) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        if ("admin".equals(role)) {
            metrics.putAll(safeMap("dashboardError", dashboardService::getAdminStats));
            if (aiDashboardService != null) {
                metrics.putAll(safeMap("aiDashboardError", aiDashboardService::getDashboardStats));
            }
            return metrics;
        }
        if ("org".equals(role)) {
            metrics.putAll(safeMap("dashboardError", () -> dashboardService.getOrgStats(userId)));
            if (liveApprovalService != null) {
                metrics.put("pendingApprovalCount", safeListSize("approvalError", metrics,
                        () -> liveApprovalService.getPendingApprovals(userId)));
            }
            return metrics;
        }
        if ("talent".equals(role)) {
            metrics.putAll(safeMap("dashboardError", () -> dashboardService.getOrgStats(userId)));
            metrics.putAll(safeMap("shortVideoDashboardError", () -> shortVideoDashboardService.getStats(userId)));
            List<Map<String, Object>> projects = recentProjects(userId);
            metrics.put("projectCount", projects.size());
            metrics.put("latestProjectTitle", latestTitle(projects));
            return metrics;
        }
        metrics.putAll(safeMap("shortVideoDashboardError", () -> shortVideoDashboardService.getStats(userId)));
        List<Map<String, Object>> projects = recentProjects(userId);
        metrics.put("projectCount", projects.size());
        metrics.put("latestProjectTitle", latestTitle(projects));
        return metrics;
    }

    private List<Map<String, Object>> sections(String role, Long userId) {
        List<Map<String, Object>> sections = new ArrayList<>();
        if ("talent".equals(role) || "user".equals(role)) {
            sections.add(section("recentProjects", "Recent Short Video Projects", recentProjects(userId)));
        }
        if ("admin".equals(role)) {
            sections.add(section("governance", "Platform Governance", List.of(
                    item("AI Dashboard", "/admin/ai/dashboard"),
                    item("Auth Resources", "/admin/auth/resources"),
                    item("TianAPI", "/admin/system/tianapi")
            )));
        }
        if ("org".equals(role)) {
            sections.add(section("operations", "Organization Operations", List.of(
                    item("Live Sessions", "/org/live/sessions"),
                    item("Products", "/org/product/list"),
                    item("Members", "/org/members")
            )));
        }
        return sections;
    }

    private List<Map<String, Object>> recentProjects(Long userId) {
        try {
            return shortVideoDashboardService.getProjectsWithProgress(userId, null, 0, 5);
        } catch (RuntimeException ex) {
            return List.of();
        }
    }

    private static String latestTitle(List<Map<String, Object>> projects) {
        if (projects.isEmpty()) {
            return "暂无项目";
        }
        Object title = projects.get(0).get("title");
        return title == null ? "暂无项目" : title.toString();
    }

    private static Map<String, Object> boundary(String role,
                                                Long userId,
                                                String roleCode,
                                                Long organizationId,
                                                List<Long> visibleOwnerIds) {
        Map<String, Object> boundary = new LinkedHashMap<>();
        boundary.put("role", role);
        boundary.put("roleCode", roleCode);
        boundary.put("ownerId", userId);
        boundary.put("organizationId", organizationId);
        boundary.put("unrestricted", visibleOwnerIds == null);
        boundary.put("scopeType", visibleOwnerIds == null ? "all" : "ownerIds");
        boundary.put("visibleOwnerIds", visibleOwnerIds);
        boundary.put("requiresOwnerFilter", visibleOwnerIds != null);
        return boundary;
    }

    private static List<String> readyEndpoints(String role) {
        if ("admin".equals(role)) {
            return List.of("/api/v1/admin/home", "/api/v1/dashboard/admin/stats", "/api/v1/ai/admin/dashboard/stats");
        }
        if ("org".equals(role)) {
            return List.of("/api/v1/org/home", "/api/v1/dashboard/org/stats", "/api/v1/live/session/search");
        }
        if ("talent".equals(role)) {
            return List.of("/api/v1/talent/home", "/api/v1/dashboard/org/stats", "/api/v1/short-video/dashboard/stats");
        }
        return List.of("/api/v1/user/home", "/api/v1/short-video/dashboard/stats", "/api/v1/short-video/project/list");
    }

    private List<Map<String, Object>> businessChains(String role, Long userId) {
        if ("admin".equals(role)) {
            return List.of(
                    chain("ai-governance", "AI 总控", "/admin/ai/dashboard", "douyin,douyin_weigui", true,
                            aiGovernanceMetrics()),
                    chain("knowledge-assets", "知识资产中心", "/admin/ai/knowledge", "douyin,douyin_weigui,viral_patterns", true,
                            knowledgeAssetMetrics()),
                    chain("tianapi-import", "TianAPI 采集额度", "/admin/system/tianapi", "tianapi_materials", false,
                            tianApiMetrics())
            );
        }
        if ("org".equals(role)) {
            return List.of(
                    chain("live-script", "直播话术生成/微调", "/org/live/sessions", "douyin,douyin_weigui,script_library", true,
                            aiReferenceMetrics(userId)),
                    chain("live-product-sort", "直播排品 AI", "/org/live/sessions", "viral_patterns,session_review", true,
                            viralAndReviewMetrics(userId)),
                    chain("product-readiness", "商品上播准备", "/org/product/list", "douyin_weigui,product_assets", true,
                            knowledgeAssetMetrics())
            );
        }
        if ("talent".equals(role)) {
            return List.of(
                    chain("shortvideo-script", "短视频脚本/分镜", "/talent/shortvideo", "douyin,douyin_weigui,viral_patterns", true,
                            aiReferenceMetrics(userId)),
                    chain("digital-human", "数字人口播成片", "/talent/shortvideo/create", "douyin,douyin_weigui,product_assets", true,
                            aiReferenceMetrics(userId)),
                    chain("benchmark-collect", "爆款采集学习", "/talent/shortvideo/collect", "viral_patterns", false,
                            viralAndBenchmarkMetrics(userId))
            );
        }
        return List.of(
                chain("personal-shortvideo", "个人短视频创作", "/user/shortvideo", "douyin,douyin_weigui", true,
                        aiReferenceMetrics(userId)),
                chain("publish-review", "发布前违规检查", "/user/shortvideo/publish", "douyin_weigui", true,
                        publishReviewMetrics(userId))
        );
    }

    private Map<String, Object> knowledgeGuard(String role) {
        Map<String, Object> guard = new LinkedHashMap<>();
        guard.put("requiredKbCodes", List.of("douyin", "douyin_weigui"));
        guard.put("officialReferenceRequired", true);
        guard.put("blockWithoutOfficialReference", !"admin".equals(role));
        guard.put("appliesTo", appliesTo(role));
        Map<String, Object> metrics = knowledgeGuardMetrics();
        guard.put("status", guardStatus(metrics));
        guard.put("realTime", true);
        guard.put("metrics", metrics);
        guard.put("message", "业务链路必须检索 douyin + douyin_weigui，并在 AI 输出中暴露官方规则引用。");
        return guard;
    }

    private List<Map<String, Object>> learningLoops(String role, Long userId) {
        List<Map<String, Object>> loops = new ArrayList<>();
        if ("admin".equals(role) || "talent".equals(role)) {
            loops.add(loop("viral-pattern-kb", "爆款采集结果结构化入库", "viral_patterns", "benchmark_collect_result",
                    viralPatternLoopMetrics(userId)));
        }
        if ("admin".equals(role) || "org".equals(role) || "talent".equals(role)) {
            loops.add(loop("live-review", "直播复盘回流", "session_review", "live_session_review",
                    liveReviewLoopMetrics(userId)));
            loops.add(loop("publish-review", "发布数据回流", "publish_review", "shortvideo_publish_feedback",
                    publishReviewMetrics(userId)));
        }
        if ("user".equals(role)) {
            loops.add(loop("personal-publish-review", "个人发布复盘回流", "publish_review", "shortvideo_publish_feedback",
                    publishReviewMetrics(userId)));
        }
        return loops;
    }

    private static List<String> appliesTo(String role) {
        if ("admin".equals(role)) {
            return List.of("ai-dashboard", "knowledge-assets", "governance");
        }
        if ("org".equals(role)) {
            return List.of("live-script", "live-product-sort", "product-readiness", "violation-review");
        }
        if ("talent".equals(role)) {
            return List.of("shortvideo-script", "shot-list", "digital-human", "benchmark-analysis", "publish-review");
        }
        return List.of("personal-shortvideo", "publish-review");
    }

    private static Map<String, Object> chain(String key,
                                             String title,
                                             String path,
                                             String requiredKnowledge,
                                             boolean guardRequired,
                                             Map<String, Object> metrics) {
        Map<String, Object> chain = new LinkedHashMap<>();
        chain.put("key", key);
        chain.put("title", title);
        chain.put("path", path);
        chain.put("requiredKnowledge", requiredKnowledge);
        chain.put("guardRequired", guardRequired);
        chain.put("realTime", true);
        chain.put("metrics", metrics != null ? metrics : Map.of());
        chain.put("status", chainStatus(metrics));
        return chain;
    }

    private static Map<String, Object> loop(String key, String title, String targetKb, String source,
                                            Map<String, Object> metrics) {
        Map<String, Object> loop = new LinkedHashMap<>();
        loop.put("key", key);
        loop.put("title", title);
        loop.put("targetKb", targetKb);
        loop.put("source", source);
        loop.put("realTime", true);
        loop.put("metrics", metrics != null ? metrics : Map.of());
        loop.put("status", chainStatus(metrics));
        return loop;
    }

    private Map<String, Object> knowledgeGuardMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("douyinDocs", safeLong(() -> aiKbDocumentRepository != null
                ? aiKbDocumentRepository.countByKbName("douyin") : 0L));
        metrics.put("douyinWeiguiDocs", safeLong(() -> aiKbDocumentRepository != null
                ? aiKbDocumentRepository.countByKbName("douyin_weigui") : 0L));
        metrics.put("officialSchoolDocs", safeLong(() -> aiKbDocumentRepository != null
                ? aiKbDocumentRepository.countBySourceTypeAndDeleted("douyin_school_official", 0) : 0L));
        Timestamp since = Timestamp.valueOf(LocalDateTime.now().minusDays(7));
        long calls = safeLong(() -> aiCallLogRepository != null ? aiCallLogRepository.countCallsSince(since) : 0L);
        long referenced = safeLong(() -> aiCallLogRepository != null ? aiCallLogRepository.countWithReferencedChunksSince(since) : 0L);
        metrics.put("aiCalls7d", calls);
        metrics.put("officialReferencedCalls7d", referenced);
        metrics.put("referenceCoverage7d", calls <= 0 ? 0.0 : Math.round((referenced * 10000.0 / calls)) / 100.0);
        metrics.put("lastReferencedAt", timestampToString(safeTimestamp(() -> aiCallLogRepository != null
                ? aiCallLogRepository.findLastReferencedAt() : null)));
        metrics.put("indexPending", safeLong(() -> aiIndexQueueRepository != null
                ? aiIndexQueueRepository.countPendingEligible(10) : 0L));
        metrics.put("indexRecoverableFailed", safeLong(() -> aiIndexQueueRepository != null
                ? aiIndexQueueRepository.countRecoverableFailedEligible(10, 10) : 0L));
        return metrics;
    }

    private String guardStatus(Map<String, Object> metrics) {
        long douyinDocs = number(metrics.get("douyinDocs"));
        long violationDocs = number(metrics.get("douyinWeiguiDocs"));
        long failed = number(metrics.get("indexRecoverableFailed"));
        if (douyinDocs <= 0 || violationDocs <= 0) {
            return "blocked";
        }
        return failed > 0 ? "degraded" : "active";
    }

    private Map<String, Object> knowledgeAssetMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("douyinDocs", safeLong(() -> aiKbDocumentRepository != null
                ? aiKbDocumentRepository.countByKbName("douyin") : 0L));
        metrics.put("douyinWeiguiDocs", safeLong(() -> aiKbDocumentRepository != null
                ? aiKbDocumentRepository.countByKbName("douyin_weigui") : 0L));
        metrics.put("viralPatternDocs", safeLong(() -> aiKbDocumentRepository != null
                ? aiKbDocumentRepository.countByKbName("douyin_viral_patterns") : 0L));
        metrics.put("performanceReflectionDocs", safeLong(() -> aiKbDocumentRepository != null
                ? aiKbDocumentRepository.countByKbName("douyin_performance_reflections") : 0L));
        metrics.put("lastDouyinDocAt", timestampToString(safeTimestamp(() -> aiKbDocumentRepository != null
                ? aiKbDocumentRepository.findLastCreateTimeByKbName("douyin") : null)));
        metrics.put("lastViolationDocAt", timestampToString(safeTimestamp(() -> aiKbDocumentRepository != null
                ? aiKbDocumentRepository.findLastCreateTimeByKbName("douyin_weigui") : null)));
        return metrics;
    }

    private Map<String, Object> aiGovernanceMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>(knowledgeGuardMetrics());
        Timestamp since = Timestamp.valueOf(LocalDateTime.now().minusDays(1));
        metrics.put("successCalls24h", safeLong(() -> aiCallLogRepository != null
                ? aiCallLogRepository.countSuccessCallsSince(since) : 0L));
        metrics.put("failedCalls24h", safeLong(() -> aiCallLogRepository != null
                ? aiCallLogRepository.countFailedCallsSince(since) : 0L));
        return metrics;
    }

    private Map<String, Object> aiReferenceMetrics(Long userId) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        Timestamp since = Timestamp.valueOf(LocalDateTime.now().minusDays(7));
        metrics.put("aiCalls7d", safeLong(() -> aiCallLogRepository != null
                ? scopedCount(userId,
                        () -> aiCallLogRepository.countByCreateTimeAfterAndStatus(since, 1),
                        () -> aiCallLogRepository.countByUserIdAndCreateTimeAfterAndStatus(userId, since, 1))
                : 0L));
        metrics.put("referencedCalls7d", safeLong(() -> aiCallLogRepository != null
                ? aiCallLogRepository.findWithReferencedChunksSince(since).stream()
                        .filter(log -> userId == null || userId.equals(log.getUserId()))
                        .count()
                : 0L));
        metrics.put("lastReferencedAt", timestampToString(safeTimestamp(() -> aiCallLogRepository != null
                ? aiCallLogRepository.findLastReferencedAt() : null)));
        return metrics;
    }

    private Map<String, Object> tianApiMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();
        if (tianApiImportRunRepository == null) {
            metrics.put("available", false);
            return metrics;
        }
        Optional<TianApiImportRun> latest = safeOptional(tianApiImportRunRepository::findTopByOrderByCreateTimeDesc);
        if (latest.isEmpty()) {
            metrics.put("hasRun", false);
            return metrics;
        }
        TianApiImportRun run = latest.get();
        metrics.put("hasRun", true);
        metrics.put("runId", run.getId());
        metrics.put("runStatus", run.getStatus());
        metrics.put("estimatedMaxHttpCalls", nz(run.getEstimatedMaxHttpCalls()));
        metrics.put("totalCalls", nz(run.getTotalCalls()));
        metrics.put("totalImported", nz(run.getTotalImported()));
        metrics.put("totalSkipped", nz(run.getTotalSkipped()));
        metrics.put("startedAt", timestampToString(run.getStartedAt()));
        metrics.put("finishedAt", timestampToString(run.getFinishedAt()));
        metrics.put("completedCategories", safeLong(() -> tianApiImportCategoryStatRepository != null
                ? tianApiImportCategoryStatRepository.countByRunId(run.getId()) : 0L));
        metrics.put("totalCategories", 27);
        metrics.put("progressPercent", percent(nz(run.getTotalCalls()), nz(run.getEstimatedMaxHttpCalls())));
        Object[] aggregate = safeObjectArray(() -> tianApiImportCategoryStatRepository != null
                ? tianApiImportCategoryStatRepository.aggregateByRunId(run.getId()) : null);
        if (aggregate != null) {
            metrics.put("categoryCalls", number(aggregate[0]));
            metrics.put("categoryImported", number(aggregate[1]));
            metrics.put("categorySkipped", number(aggregate[2]));
            metrics.put("categoryEmptyResponses", number(aggregate[3]));
            metrics.put("categoryFailedCalls", number(aggregate[4]));
        }
        return metrics;
    }

    private Map<String, Object> viralAndBenchmarkMetrics(Long userId) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.putAll(viralAndReviewMetrics(userId));
        metrics.put("benchmarkVideos", safeLong(() -> benchmarkVideoRepository != null
                ? scopedCount(userId,
                        () -> benchmarkVideoRepository.countByDeleted(0),
                        () -> benchmarkVideoRepository.countByOwnerIdAndDeleted(userId, 0))
                : 0L));
        metrics.put("benchmarkAnalyzed", safeLong(() -> benchmarkVideoRepository != null
                ? scopedCount(userId,
                        () -> benchmarkVideoRepository.countByAnalysisStatusAndDeleted("completed", 0),
                        () -> benchmarkVideoRepository.countByOwnerIdAndAnalysisStatusAndDeleted(userId, "completed", 0))
                : 0L));
        metrics.put("lastBenchmarkCollectedAt", objectToString(safeObject(() -> benchmarkVideoRepository != null
                ? (userId == null ? benchmarkVideoRepository.findLastCreateTime() : benchmarkVideoRepository.findLastCreateTimeByOwnerId(userId))
                : null)));
        return metrics;
    }

    private Map<String, Object> viralAndReviewMetrics(Long userId) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("viralVideos", safeLong(() -> svViralVideoRepository != null
                ? scopedCount(userId,
                        () -> svViralVideoRepository.countByDeleted(0),
                        () -> svViralVideoRepository.countByOwnerIdAndDeleted(userId, 0))
                : 0L));
        metrics.put("autoCollectedViralVideos", safeLong(() -> svViralVideoRepository != null
                ? scopedCount(userId,
                        () -> svViralVideoRepository.countByAutoCollectedAndDeleted(true, 0),
                        () -> svViralVideoRepository.countByOwnerIdAndAutoCollectedAndDeleted(userId, true, 0))
                : 0L));
        metrics.put("deepAnalyzedViralVideos", safeLong(() -> svViralVideoRepository != null
                ? scopedCount(userId,
                        () -> svViralVideoRepository.countByDeepAnalyzeStatusAndDeleted("completed", 0),
                        () -> svViralVideoRepository.countByOwnerIdAndDeepAnalyzeStatusAndDeleted(userId, "completed", 0))
                : 0L));
        metrics.put("lastViralCollectedAt", timestampToString(safeTimestamp(() -> svViralVideoRepository != null
                ? (userId == null ? svViralVideoRepository.findLastCreateTime() : svViralVideoRepository.findLastCreateTimeByOwnerId(userId))
                : null)));
        return metrics;
    }

    private Map<String, Object> viralPatternLoopMetrics(Long userId) {
        Map<String, Object> metrics = new LinkedHashMap<>(viralAndBenchmarkMetrics(userId));
        metrics.put("targetKbDocs", safeLong(() -> aiKbDocumentRepository != null
                ? aiKbDocumentRepository.countByKbName("douyin_viral_patterns") : 0L));
        metrics.put("targetKbLastDocAt", timestampToString(safeTimestamp(() -> aiKbDocumentRepository != null
                ? aiKbDocumentRepository.findLastCreateTimeByKbName("douyin_viral_patterns") : null)));
        return metrics;
    }

    private Map<String, Object> liveReviewLoopMetrics(Long userId) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("targetKbDocs", safeLong(() -> aiKbDocumentRepository != null
                ? aiKbDocumentRepository.countByKbName("douyin_performance_reflections") : 0L));
        metrics.putAll(aiReferenceMetrics(userId));
        return metrics;
    }

    private Map<String, Object> publishReviewMetrics(Long userId) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("publishedProjects", safeLong(() -> svProjectRepository != null
                ? scopedCount(userId,
                        () -> svProjectRepository.countByPublishTimeIsNotNullAndDeleted(0),
                        () -> svProjectRepository.countByOwnerIdAndPublishTimeIsNotNullAndDeleted(userId, 0))
                : 0L));
        metrics.put("reviewedAiCalls7d", aiReferenceMetrics(userId).get("referencedCalls7d"));
        metrics.put("targetKbDocs", safeLong(() -> aiKbDocumentRepository != null
                ? aiKbDocumentRepository.countByKbName("douyin_performance_reflections") : 0L));
        return metrics;
    }

    private static String chainStatus(Map<String, Object> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            return "unknown";
        }
        Object runStatus = metrics.get("runStatus");
        if ("failed".equals(runStatus)) {
            return "failed";
        }
        long failed = number(metrics.get("failedCalls24h")) + number(metrics.get("indexRecoverableFailed"));
        if (failed > 0) {
            return "degraded";
        }
        long signal = number(metrics.get("totalImported"))
                + number(metrics.get("douyinDocs"))
                + number(metrics.get("douyinWeiguiDocs"))
                + number(metrics.get("viralVideos"))
                + number(metrics.get("benchmarkVideos"))
                + number(metrics.get("referencedCalls7d"))
                + number(metrics.get("targetKbDocs"));
        return signal > 0 ? "active" : "empty";
    }

    private static long scopedCount(Long userId, Supplier<Long> global, Supplier<Long> scoped) {
        return userId == null || userId <= 0 ? global.get() : scoped.get();
    }

    private static long safeLong(Supplier<Long> supplier) {
        try {
            Long value = supplier.get();
            return value != null ? value : 0L;
        } catch (RuntimeException ex) {
            return 0L;
        }
    }

    private static Timestamp safeTimestamp(Supplier<Timestamp> supplier) {
        try {
            return supplier.get();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static Object safeObject(Supplier<Object> supplier) {
        try {
            return supplier.get();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static Object[] safeObjectArray(Supplier<Object[]> supplier) {
        try {
            return supplier.get();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static <T> Optional<T> safeOptional(Supplier<Optional<T>> supplier) {
        try {
            Optional<T> value = supplier.get();
            return value != null ? value : Optional.empty();
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    private static int nz(Integer value) {
        return value != null ? value : 0;
    }

    private static long number(Object value) {
        return value instanceof Number n ? n.longValue() : 0L;
    }

    private static double percent(long part, long total) {
        if (total <= 0) {
            return 0.0;
        }
        return Math.round(part * 10000.0 / total) / 100.0;
    }

    private static String timestampToString(Timestamp timestamp) {
        return timestamp != null ? timestamp.toInstant().toString() : null;
    }

    private static String objectToString(Object value) {
        return value != null ? value.toString() : null;
    }

    private static Map<String, Object> section(String key, String title, List<Map<String, Object>> items) {
        Map<String, Object> section = new LinkedHashMap<>();
        section.put("key", key);
        section.put("title", title);
        section.put("items", items);
        return section;
    }

    private static Map<String, Object> item(String title, String path) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("title", title);
        item.put("path", path);
        return item;
    }

    private static Map<String, Object> safeMap(String errorKey, Supplier<Map<String, Object>> supplier) {
        try {
            Map<String, Object> value = supplier.get();
            return value == null ? Map.of() : value;
        } catch (RuntimeException ex) {
            return Map.of(errorKey, ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
        }
    }

    private static int safeListSize(String errorKey, Map<String, Object> metrics, Supplier<List<LiveSession>> supplier) {
        try {
            List<LiveSession> value = supplier.get();
            return value == null ? 0 : value.size();
        } catch (RuntimeException ex) {
            metrics.put(errorKey, ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
            return 0;
        }
    }
}
