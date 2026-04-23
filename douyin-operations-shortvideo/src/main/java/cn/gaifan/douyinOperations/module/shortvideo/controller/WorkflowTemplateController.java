package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.common.annotation.CurrentUserId;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvWorkflowTemplate;
import cn.gaifan.douyinOperations.module.shortvideo.service.WorkflowTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 工作流模板 Controller（Phase 4.4）
 */
@RestController
@RequestMapping("/api/v1/short-video/workflow-template")
@Tag(name = "短视频 / 工作流模板", description = "工作流预设模板管理")
public class WorkflowTemplateController {

    @Resource
    private WorkflowTemplateService workflowTemplateService;

    @PostMapping("/list")
    @Operation(summary = "查询可用工作流模板")
    public RESTResult<List<Map<String, Object>>> list(@CurrentUserId Long userId) {
        RESTResult<List<Map<String, Object>>> r = RESTResult.getSuccess(workflowTemplateService.listForOwner(userId));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/get")
    @Operation(summary = "根据ID获取模板详情")
    public RESTResult<SvWorkflowTemplate> get(@RequestBody Map<String, Long> body, @CurrentUserId Long userId) {
        Long id = body != null ? body.get("id") : null;
        SvWorkflowTemplate t = workflowTemplateService.getById(id, userId);
        RESTResult<SvWorkflowTemplate> r = RESTResult.getSuccess(t);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
