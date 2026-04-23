package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.live.service.LiveTemplateService;
import cn.gaifan.douyinOperations.module.live.vo.SaveSessionAsTemplateVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * 从场次创建话术模板 Controller（将场次话术快照保存为可复用模板）
 * <p>
 * 注意：此 Controller 路径迁移至 /api/v1/live/session-template，
 * 避免与 LiveScriptTemplateController 的 /api/v1/live/template 冲突。
 * 旧路径 /api/v1/live/template/save-from-session 的调用方请更新为新路径。
 */
@RestController
@RequestMapping("/api/v1/live/session-template")
@Tag(name = "场次模板 / Session Template", description = "从场次话术创建可复用模板（需登录）")
public class LiveTemplateController {

    @Resource
    private LiveTemplateService liveTemplateService;

    @PostMapping("/save-from-session")
    @Operation(
            summary = "从场次话术创建模板 / Save Session Scripts as Template",
            description = "将指定场次的话术保存为可复用模板，产品名替换为占位符（需登录）")
    public RESTResult<Long> saveFromSession(HttpServletRequest request,
            @Valid @RequestBody SaveSessionAsTemplateVO vo) {
        Long userId = requireUserId(request);
        Long templateId = liveTemplateService.saveSessionAsTemplate(
                vo.getSessionId(), vo.getTemplateName(), vo.getScriptTypes(), userId);
        return withTraceId(RESTResult.addSuccess(templateId));
    }

    private Long requireUserId(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) throw new cn.gaifan.douyinOperations.common.exception.BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        return userId;
    }

    private <T> RESTResult<T> withTraceId(RESTResult<T> r) {
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
