package cn.gaifan.douyinOperations.module.ai.controller;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.ai.entity.AiEvolveReport;
import cn.gaifan.douyinOperations.module.ai.entity.AiEvolveTask;
import cn.gaifan.douyinOperations.module.ai.entity.AiEvolveTopic;
import cn.gaifan.douyinOperations.module.ai.repository.AiEvolveReportRepository;
import cn.gaifan.douyinOperations.module.ai.service.EvolveEngineService;
import cn.gaifan.douyinOperations.module.ai.service.EvolveRoiService;
import cn.gaifan.douyinOperations.module.ai.service.EvolveTopicImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识进化引擎 - 管理端 API
 */
@Tag(name = "AI 进化引擎")
@RestController
@RequestMapping("/api/v1/ai/admin/evolve")
public class EvolveController {

    @Resource
    private EvolveEngineService evolveEngineService;

    @Resource
    private EvolveTopicImportService evolveTopicImportService;

    @Resource
    private EvolveRoiService evolveRoiService;

    @Resource
    private AiEvolveReportRepository reportRepository;

    @Operation(summary = "手动触发进化")
    @PostMapping("/trigger")
    public RESTResult<Map<String, Object>> trigger(
            @RequestBody(required = false) Map<String, Object> body
    ) {
        Long kbId = null;
        String evolveAngle = null;
        if (body != null) {
            if (body.get("kbId") instanceof Number n) kbId = n.longValue();
            if (body.get("evolveAngle") instanceof String s) evolveAngle = s;
        }
        evolveEngineService.runEvolution(kbId, evolveAngle);
        String taskNo = "evolve_" + (kbId != null ? kbId + "_" : "") +
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Map<String, Object> data = new HashMap<>();
        data.put("taskNo", taskNo);
        data.put("message", "进化任务已启动，可在任务历史中查看进度");
        return RESTResult.getSuccess(data);
    }

    @Operation(summary = "进化状态概览")
    @PostMapping("/status")
    public RESTResult<Map<String, Object>> status() {
        List<AiEvolveTask> recent = evolveEngineService.listRecentTasks(10);
        List<AiEvolveTopic> topics = evolveEngineService.listTopics(null, null, false);
        Map<String, Object> data = new HashMap<>();
        data.put("recentTasks", recent);
        data.put("topicCount", topics.size());
        data.put("lastTask", recent.isEmpty() ? null : recent.get(0));
        data.put("scoreTrend", evolveEngineService.getScoreTrend(7));
        data.put("topicDistribution", evolveEngineService.getTopicDistribution());
        data.put("roiMetrics", evolveRoiService.getRoiMetrics(10));
        return RESTResult.getSuccess(data);
    }

    @Operation(summary = "主题池列表（支持按知识库/抖音账号归属筛选）")
    @PostMapping("/topic/list")
    public RESTResult<List<AiEvolveTopic>> listTopics(@RequestBody(required = false) Map<String, Object> body) {
        Long kbId = body != null && body.get("kbId") instanceof Number n ? n.longValue() : null;
        Long accountId = body != null && body.get("accountId") instanceof Number n ? n.longValue() : null;
        boolean scopeGlobal = body != null && Boolean.TRUE.equals(body.get("scopeGlobal"));
        return RESTResult.getSuccess(evolveEngineService.listTopics(kbId, accountId, scopeGlobal));
    }

    @Operation(summary = "新增/编辑主题")
    @PostMapping("/topic/save")
    public RESTResult<AiEvolveTopic> saveTopic(@RequestBody AiEvolveTopic topic) {
        return RESTResult.addSuccess(evolveEngineService.saveTopic(topic));
    }

    @Operation(summary = "从老系统导入主题库")
    @PostMapping("/topic/import-from-file")
    public RESTResult<EvolveTopicImportService.TopicImportResult> importTopics(
            @RequestBody Map<String, Object> body
    ) {
        String sourcePath = body != null && body.get("sourcePath") instanceof String s ? s : null;
        Long kbId = body != null && body.get("kbId") instanceof Number n ? n.longValue() : null;
        if (sourcePath == null || sourcePath.isBlank()) {
            return RESTResult.error(1001, "sourcePath 不能为空");
        }
        EvolveTopicImportService.TopicImportResult result = evolveTopicImportService.importFromFile(sourcePath, kbId);
        return RESTResult.getSuccess(result);
    }

    @Operation(summary = "删除主题")
    @PostMapping("/topic/delete")
    public RESTResult<Void> deleteTopicPost(@RequestBody Map<String, Object> body) {
        Long id = body.get("id") instanceof Number n ? n.longValue() : null;
        if (id == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS, "id 不能为空");
        }
        evolveEngineService.deleteTopic(id);
        return RESTResult.success("删除成功", null);
    }

    @Operation(summary = "删除主题（兼容 DELETE 方法）")
    @DeleteMapping("/topic/{id}")
    public RESTResult<Void> deleteTopic(@PathVariable Long id) {
        evolveEngineService.deleteTopic(id);
        return RESTResult.success("删除成功", null);
    }

    @Operation(summary = "最近任务列表")
    @PostMapping("/task/list")
    public RESTResult<List<AiEvolveTask>> listTasks(@RequestBody(required = false) Map<String, Object> body) {
        int limit = body != null && body.get("limit") instanceof Number n ? n.intValue() : 10;
        if (limit <= 0 || limit > 100) limit = 10;
        return RESTResult.getSuccess(evolveEngineService.listRecentTasks(limit));
    }

    @Operation(summary = "删除进化任务")
    @DeleteMapping("/task/{id}")
    public RESTResult<Void> deleteTask(@PathVariable Long id) {
        evolveEngineService.deleteTask(id);
        return RESTResult.success("删除成功", null);
    }

    @Operation(summary = "获取进化报告（按任务ID）")
    @PostMapping("/report/by-task")
    public RESTResult<AiEvolveReport> getReport(@RequestBody Map<String, Object> body) {
        Object v = body != null ? body.get("taskId") : null;
        if (v == null) throw new BusinessException(ErrorCode.INVALID_PARAMS, "taskId 不能为空");
        Long taskId = Long.parseLong(v.toString());
        AiEvolveReport report = reportRepository.findByTaskId(taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "报告不存在"));
        return RESTResult.getSuccess(report);
    }
}
