package cn.gaifan.douyinOperations.module.agent.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.agent.service.AgentWorkflowService;
import cn.gaifan.douyinOperations.module.agent.vo.AgentWorkflowExecutionVO;
import cn.gaifan.douyinOperations.module.agent.vo.AgentWorkflowSaveVO;
import cn.gaifan.douyinOperations.module.agent.vo.AgentWorkflowVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;

/**
 * 智能体协作编排工作流管理
 */
@RestController
@RequestMapping("/api/v1/agent/workflow")
@Tag(name = "智能体工作流 / Agent Workflow", description = "多智能体协作编排")
public class AgentWorkflowController {

    @Resource
    private AgentWorkflowService workflowService;

    @PostMapping("/list")
    @Operation(summary = "工作流列表（分页）")
    public RESTResult<PageResultVO<AgentWorkflowVO>> list(HttpServletRequest request,
            @RequestBody Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Integer page = toInt(body.get("page"), 0);
        Integer rows = toInt(body.get("rows"), 10);
        return RESTResult.getSuccess(workflowService.list(userId, page, rows));
    }

    @PostMapping("/get")
    @Operation(summary = "获取工作流详情")
    public RESTResult<AgentWorkflowVO> get(@RequestBody Map<String, Object> body) {
        Long workflowId = toLong(body.get("id"));
        if (workflowId == null) return RESTResult.error(ErrorCode.INVALID_PARAMS, "工作流ID不能为空");
        return RESTResult.getSuccess(workflowService.getById(workflowId));
    }

    @PostMapping("/save")
    @Operation(summary = "保存工作流（新增或更新）")
    public RESTResult<Long> save(HttpServletRequest request, @Valid @RequestBody AgentWorkflowSaveVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        RESTResult<Long> r = RESTResult.getSuccess(workflowService.save(userId, vo));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/delete")
    @Operation(summary = "删除工作流")
    public RESTResult<Void> delete(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long workflowId = toLong(body.get("id"));
        if (workflowId == null) return RESTResult.error(ErrorCode.INVALID_PARAMS, "工作流ID不能为空");
        workflowService.delete(workflowId, userId);
        return RESTResult.success(null);
    }

    @PostMapping("/execution/list")
    @Operation(summary = "执行历史列表（用户维度）")
    public RESTResult<PageResultVO<AgentWorkflowExecutionVO>> executionList(HttpServletRequest request,
            @RequestBody Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Integer page = toInt(body.get("page"), 0);
        Integer rows = toInt(body.get("rows"), 10);
        return RESTResult.getSuccess(workflowService.getExecutionHistory(userId, page, rows));
    }

    @PostMapping("/execution/workflow-list")
    @Operation(summary = "执行历史列表（工作流维度）")
    public RESTResult<PageResultVO<AgentWorkflowExecutionVO>> workflowExecutionList(HttpServletRequest request,
            @RequestBody Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long workflowId = toLong(body.get("workflowId"));
        if (workflowId == null) return RESTResult.error(ErrorCode.INVALID_PARAMS, "工作流ID不能为空");
        Integer page = toInt(body.get("page"), 0);
        Integer rows = toInt(body.get("rows"), 10);
        return RESTResult.getSuccess(workflowService.getWorkflowExecutions(userId, workflowId, page, rows));
    }

    @PostMapping("/execution/get")
    @Operation(summary = "获取执行详情")
    public RESTResult<AgentWorkflowExecutionVO> getExecution(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long executionId = toLong(body.get("id"));
        if (executionId == null) return RESTResult.error(ErrorCode.INVALID_PARAMS, "执行记录ID不能为空");
        return RESTResult.getSuccess(workflowService.getExecutionById(executionId, userId));
    }

    @PostMapping("/execute")
    @Operation(summary = "执行工作流编排")
    public RESTResult<Map<String, Object>> execute(HttpServletRequest request,
            @RequestBody Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long workflowId = toLong(body.get("workflowId"));
        Long conversationId = toLong(body.get("conversationId"));
        String userInput = (String) body.get("userInput");
        if (workflowId == null) return RESTResult.error(ErrorCode.INVALID_PARAMS, "工作流ID不能为空");
        if (userInput == null || userInput.isBlank()) return RESTResult.error(ErrorCode.INVALID_PARAMS, "输入内容不能为空");
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(
                workflowService.execute(workflowId, userId, conversationId, userInput));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Long) return (Long) v;
        if (v instanceof Integer) return ((Integer) v).longValue();
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return null; }
    }

    private Integer toInt(Object v, int defaultVal) {
        if (v == null) return defaultVal;
        if (v instanceof Integer) return (Integer) v;
        try { return Integer.parseInt(v.toString()); } catch (Exception e) { return defaultVal; }
    }
}
