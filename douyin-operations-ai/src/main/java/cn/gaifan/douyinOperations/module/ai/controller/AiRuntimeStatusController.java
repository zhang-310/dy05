package cn.gaifan.douyinOperations.module.ai.controller;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.ai.config.AiRuntimeConfig;
import cn.gaifan.douyinOperations.module.ai.config.official.DouyinSchoolCollectorProperties;
import cn.gaifan.douyinOperations.module.ai.entity.AiEvolveTask;
import cn.gaifan.douyinOperations.module.ai.entity.AiOfficialKnowledgeCollectItem;
import cn.gaifan.douyinOperations.module.ai.repository.AiEvolveTaskRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiIndexQueueRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiOfficialKnowledgeCollectItemRepository;
import cn.gaifan.douyinOperations.module.ai.service.EvolveCircuitBreakerService;
import cn.gaifan.douyinOperations.module.ai.service.EvolveEngineService;
import cn.gaifan.douyinOperations.module.ai.service.EvolveRoiService;
import cn.gaifan.douyinOperations.module.ai.service.OfficialKnowledgeCollectItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Tag(name = "AI 运行状态")
@RestController
@RequestMapping("/api/v1/ai/admin/runtime-status")
public class AiRuntimeStatusController {

    private static final List<String> RUNNING_EVOLVE_STATUSES = List.of(
            "gathering", "generating", "scoring", "expanding", "indexing");

    @Value("${app.ai.evolve.enabled:true}")
    private boolean evolveEnabled;

    @Value("${app.ai.deep-evolve.enabled:true}")
    private boolean deepEvolveEnabled;

    @Resource
    private AiRuntimeConfig aiRuntimeConfig;

    @Resource
    private EvolveEngineService evolveEngineService;

    @Resource
    private EvolveRoiService evolveRoiService;

    @Resource
    private EvolveCircuitBreakerService evolveCircuitBreakerService;

    @Resource
    private AiEvolveTaskRepository evolveTaskRepository;

    @Resource
    private AiIndexQueueRepository indexQueueRepository;

    @Resource
    private DouyinSchoolCollectorProperties douyinSchoolCollectorProperties;

    @Resource
    private OfficialKnowledgeCollectItemService officialCollectItemService;

    @Resource
    private AiOfficialKnowledgeCollectItemRepository officialCollectItemRepository;

    @GetMapping
    @Operation(summary = "采集与进化运行状态看板")
    public RESTResult<Map<String, Object>> get(HttpServletRequest request) {
        requireAdmin(request);
        return RESTResult.getSuccess(buildStatus());
    }

    @PostMapping
    @Operation(summary = "采集与进化运行状态看板")
    public RESTResult<Map<String, Object>> post(HttpServletRequest request) {
        requireAdmin(request);
        return RESTResult.getSuccess(buildStatus());
    }

    private Map<String, Object> buildStatus() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("checkedAt", LocalDateTime.now().toString());
        data.put("evolution", buildEvolutionStatus());
        data.put("officialCollect", buildOfficialCollectStatus());
        data.put("indexQueue", buildIndexQueueStatus());
        return data;
    }

    private Map<String, Object> buildEvolutionStatus() {
        Map<String, Object> data = new LinkedHashMap<>();
        List<AiEvolveTask> recent = evolveEngineService.listRecentTasks(10);
        boolean running = recent.stream().anyMatch(t -> RUNNING_EVOLVE_STATUSES.contains(normalize(t.getStatus())));
        AiEvolveTask latest = recent.isEmpty() ? null : recent.get(0);
        Map<String, Object> breaker = evolveCircuitBreakerService.status();
        int intervalMinutes = aiRuntimeConfig.getEvolveIntervalMinutes();
        LocalDateTime latestTime = latestTime(latest);

        data.put("enabled", evolveEnabled);
        data.put("deepEvolveEnabled", deepEvolveEnabled);
        data.put("running", running);
        data.put("intervalMinutes", intervalMinutes);
        data.put("lastTask", latest == null ? null : taskToMap(latest));
        data.put("lastTaskAt", latestTime != null ? latestTime.toString() : null);
        data.put("nextEstimatedRunAt", evolveEnabled ? nextIntervalRun(latestTime, intervalMinutes) : null);
        data.put("resolvedKbIds", evolveEngineService.resolveEvolveKbIds());
        data.put("taskCounts", Map.of(
                "completed", evolveTaskRepository.countByStatus("completed"),
                "failed", evolveTaskRepository.countByStatus("failed"),
                "pendingReview", evolveTaskRepository.countByStatus("pending_review"),
                "running", recent.stream().filter(t -> RUNNING_EVOLVE_STATUSES.contains(normalize(t.getStatus()))).count()
        ));
        data.put("circuitBreaker", breaker);
        data.put("roi", evolveRoiService.getRoiMetrics(10));
        data.put("health", !evolveEnabled ? "disabled"
                : Boolean.TRUE.equals(breaker.get("open")) ? "blocked"
                : running ? "running"
                : "waiting");
        return data;
    }

    private Map<String, Object> buildOfficialCollectStatus() {
        Map<String, Object> data = new LinkedHashMap<>();
        Map<String, Object> summary = officialCollectItemService.summary();
        AiOfficialKnowledgeCollectItem latest = officialCollectItemRepository
                .findFirstByDeletedOrderByUpdateTimeDesc(0)
                .orElse(null);
        data.put("enabled", douyinSchoolCollectorProperties.isEnabled());
        data.put("cron", douyinSchoolCollectorProperties.getCron());
        data.put("nextRunAt", douyinSchoolCollectorProperties.isEnabled()
                ? nextCronRun(douyinSchoolCollectorProperties.getCron())
                : null);
        data.put("importOnStartup", douyinSchoolCollectorProperties.isImportOnStartup());
        data.put("deepMediaExtractionEnabled", douyinSchoolCollectorProperties.isDeepMediaExtractionEnabled());
        data.put("imageOcrEnabled", douyinSchoolCollectorProperties.isImageOcrEnabled());
        data.put("videoAsrEnabled", douyinSchoolCollectorProperties.isVideoAsrEnabled());
        data.put("lastItem", latest == null ? null : officialItemToMap(latest));
        data.put("lastCollectedAt", timestamp(latest == null ? null : latest.getLastCollectedAt()));
        data.put("lastIndexedAt", timestamp(latest == null ? null : latest.getLastIndexedAt()));
        data.put("summary", summary);
        data.put("health", !douyinSchoolCollectorProperties.isEnabled() ? "disabled"
                : Number.class.isInstance(summary.get("failed")) && ((Number) summary.get("failed")).longValue() > 0 ? "degraded"
                : "waiting");
        return data;
    }

    private Map<String, Object> buildIndexQueueStatus() {
        long pending = indexQueueRepository.countByStatus("pending");
        long processing = indexQueueRepository.countByStatus("processing");
        long failed = indexQueueRepository.countByStatus("failed");
        long done = indexQueueRepository.countByStatus("done");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("pending", pending);
        data.put("processing", processing);
        data.put("failed", failed);
        data.put("done", done);
        data.put("health", failed > 0 ? "degraded" : processing > 0 ? "running" : pending > 0 ? "waiting" : "idle");
        return data;
    }

    private static Map<String, Object> taskToMap(AiEvolveTask task) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", task.getId());
        data.put("kbId", task.getKbId());
        data.put("taskNo", task.getTaskNo());
        data.put("status", task.getStatus());
        data.put("evolveAngle", task.getEvolveAngle());
        data.put("scoreTotal", task.getScoreTotal());
        data.put("errorMessage", task.getErrorMessage());
        data.put("createTime", timestamp(task.getCreateTime()));
        data.put("updateTime", timestamp(task.getUpdateTime()));
        return data;
    }

    private static Map<String, Object> officialItemToMap(AiOfficialKnowledgeCollectItem item) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", item.getId());
        data.put("title", item.getTitle());
        data.put("targetKbName", item.getTargetKbName());
        data.put("collectStatus", item.getCollectStatus());
        data.put("indexStatus", item.getIndexStatus());
        data.put("ocrStatus", item.getOcrStatus());
        data.put("asrStatus", item.getAsrStatus());
        data.put("lastError", item.getLastError());
        data.put("updateTime", timestamp(item.getUpdateTime()));
        return data;
    }

    private static LocalDateTime latestTime(AiEvolveTask task) {
        if (task == null) return null;
        Timestamp value = task.getUpdateTime() != null ? task.getUpdateTime() : task.getCreateTime();
        return value == null ? null : value.toLocalDateTime();
    }

    private static String nextIntervalRun(LocalDateTime latestTime, int intervalMinutes) {
        if (latestTime == null) {
            return LocalDateTime.now().plusMinutes(Math.max(1, intervalMinutes)).toString();
        }
        LocalDateTime next = latestTime.plusMinutes(Math.max(1, intervalMinutes));
        LocalDateTime now = LocalDateTime.now();
        while (next.isBefore(now)) {
            next = next.plusMinutes(Math.max(1, intervalMinutes));
        }
        return next.toString();
    }

    private static String nextCronRun(String cron) {
        try {
            CronExpression expression = CronExpression.parse(cron);
            LocalDateTime next = expression.next(LocalDateTime.now(ZoneId.systemDefault()));
            return next == null ? null : next.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static String timestamp(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime().toString();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private void requireAdmin(HttpServletRequest request) {
        String roleCode = (String) request.getAttribute("roleCode");
        if (roleCode == null || !"admin".equals(roleCode)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可访问");
        }
    }
}
