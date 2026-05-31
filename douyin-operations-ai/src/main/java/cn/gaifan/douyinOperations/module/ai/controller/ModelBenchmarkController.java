package cn.gaifan.douyinOperations.module.ai.controller;

import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.ai.service.ModelBenchmarkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 模型性能基准 Controller
 * 暴露 /api/v1/ai/model-benchmark/* 端点，供前端 ModelBenchmarkPage 调用
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai/model-benchmark")
public class ModelBenchmarkController {

    @Autowired(required = false)
    private ModelBenchmarkService modelBenchmarkService;

    /**
     * 模型性能对比
     * POST /api/v1/ai/model-benchmark/comparison
     * { "taskCode": "short_video_script" }
     */
    @PostMapping("/comparison")
    public RESTResult<List<Map<String, Object>>> getComparison(
            @RequestBody(required = false) Map<String, Object> request) {
        String taskCode = request != null && request.get("taskCode") instanceof String s ? s : null;
        if (modelBenchmarkService == null) {
            log.warn("[ModelBenchmark] ModelBenchmarkService 未注入，返回空数据");
            return RESTResult.success("OK", List.of());
        }
        String tc = (taskCode != null && !taskCode.isBlank()) ? taskCode : null;
        List<Map<String, Object>> data = modelBenchmarkService.getModelComparison(tc);
        return RESTResult.success("OK", data != null ? data : List.of());
    }

    /**
     * 为指定任务推荐最优模型
     * POST /api/v1/ai/model-benchmark/best-model
     * { "taskCode": "evolution", "priority": "latency" }
     */
    @PostMapping("/best-model")
    public RESTResult<Map<String, Object>> getBestModel(
            @RequestBody(required = false) Map<String, Object> request) {
        String taskCode = request != null && request.get("taskCode") instanceof String s ? s : "default";
        String priority = request != null && request.get("priority") instanceof String p ? p : "latency";
        if (modelBenchmarkService == null) {
            return RESTResult.success("OK", Map.of(
                    "modelId", 0L,
                    "modelName", "",
                    "taskCode", taskCode,
                    "priority", priority));
        }
        Map<String, Object> data = modelBenchmarkService.recommendBestModel(taskCode, priority);
        return RESTResult.success("OK", data != null ? data : Map.of());
    }

    /**
     * 手动记录基准数据（运维/测试用）
     * POST /api/v1/ai/model-benchmark/record
     */
    @PostMapping("/record")
    public RESTResult<Void> recordBenchmark(
            @RequestBody Map<String, Object> request) {
        Long modelId = request.get("modelId") != null ? ((Number) request.get("modelId")).longValue() : null;
        String taskCode = request.get("taskCode") instanceof String s ? s : "default";
        long latencyMs = request.get("latencyMs") != null ? ((Number) request.get("latencyMs")).longValue() : 0L;
        int tokensUsed = request.get("tokensUsed") != null ? ((Number) request.get("tokensUsed")).intValue() : 0;
        boolean success = request.get("success") instanceof Boolean b ? b : true;
        if (modelId == null) return RESTResult.error(400, "modelId 不能为空");
        if (modelBenchmarkService != null) {
            modelBenchmarkService.recordBenchmark(modelId, taskCode, latencyMs, tokensUsed, success);
        }
        return RESTResult.success("记录成功", null);
    }
}
