package cn.gaifan.douyinOperations.module.workflow.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.workflow.service.WorkflowExecutor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController("workflowModuleController")
@RequestMapping("/api/v1/workflow")
@Tag(name = "工作流", description = "工作流编排执行")
public class WorkflowController {

    @Resource
    private WorkflowExecutor workflowExecutor;

    @PostMapping("/execute")
    @Operation(summary = "执行工作流")
    public RESTResult<WorkflowExecutor.WorkflowExecuteResult> execute(HttpServletRequest request,
            @RequestBody Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        String workflowCode = body != null && body.get("workflowCode") != null ? body.get("workflowCode").toString() : null;
        if (workflowCode == null || workflowCode.isBlank()) {
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, "workflowCode 不能为空");
        }
        Map<String, Object> params = body != null && body.get("params") instanceof Map ? (Map<String, Object>) body.get("params") : (body != null ? body : Map.of());
        if (!params.containsKey("userId")) params.put("userId", userId);
        WorkflowExecutor.WorkflowExecuteResult result = workflowExecutor.execute(workflowCode, params);
        RESTResult<WorkflowExecutor.WorkflowExecuteResult> r = RESTResult.getSuccess(result);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
