package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.shortvideo.service.WorkflowExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 工作流执行 API (Phase 5.1)
 * 路径：/api/v1/short-video/workflow
 */
@RestController
@RequestMapping("/api/v1/short-video/workflow")
@Tag(name = "工作流", description = "可视化工作流执行与 AI 微调")
public class WorkflowController {

    @Resource
    private WorkflowExecutionService workflowExecutionService;

    @PostMapping("/execute")
    @Operation(summary = "从指定节点开始执行工作流")
    public RESTResult<Map<String, Object>> execute(@RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        Long projectId = body.get("projectId") instanceof Number n ? n.longValue() : 0L;
        String startStep = body.get("startStep") instanceof String s ? s : "script";
        @SuppressWarnings("unchecked")
        Map<String, Object> params = body.get("params") instanceof Map ? (Map<String, Object>) body.get("params") : Map.of();

        String taskId = workflowExecutionService.executeFrom(projectId, startStep, params, userId);
        return RESTResult.success("已提交", Map.<String, Object>of("taskId", taskId));
    }

    @PostMapping("/status")
    @Operation(summary = "查询工作流任务状态")
    public RESTResult<Map<String, Object>> status(@RequestBody Map<String, Object> body) {
        String taskId = body.get("taskId") instanceof String s ? s : "";
        Map<String, Object> status = workflowExecutionService.getStatus(taskId);
        return RESTResult.success("查询成功", status);
    }

    @PostMapping("/ai-assist")
    @Operation(summary = "AI 微调节点参数")
    public RESTResult<Map<String, Object>> aiAssist(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        Long projectId = body.get("projectId") instanceof Number n ? n.longValue() : 0L;
        String nodeId = body.get("nodeId") instanceof String s ? s : "";
        String userInput = body.get("userInput") instanceof String s ? s : "";

        Map<String, Object> result = workflowExecutionService.aiAssistNode(projectId, nodeId, userInput, userId);
        return RESTResult.success("处理成功", result);
    }
}
