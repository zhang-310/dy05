package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.live.service.LiveScriptApprovalService;
import cn.gaifan.douyinOperations.module.live.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/live/script-approval")
@Tag(name = "直播话术审核", description = "话术提交/审批/撤回/历史")
public class LiveScriptApprovalController {

    @Resource
    private LiveScriptApprovalService approvalService;

    @PostMapping("/submit")
    @Operation(summary = "提交话术审核")
    public RESTResult<LiveScriptApprovalVO> submit(
            @Valid @RequestBody LiveScriptApprovalSaveVO vo,
            HttpServletRequest request) {
        Long userId = requireUserId(request);
        return RESTResult.success(approvalService.submit(vo.getScriptId(), vo.getComments(), userId));
    }

    @PostMapping("/submit-by-session")
    @Operation(summary = "整场批量提交审核（统一入口，替代 /live/approval/submit）",
            description = "将场次下所有有内容且未在审核中的话术批量提交审核，支持多级风险自动通过")
    public RESTResult<Map<String, Object>> submitBySession(
            @Valid @RequestBody SubmitBySessionVO vo,
            HttpServletRequest request) {
        Long userId = requireUserId(request);
        List<LiveScriptApprovalVO> results = approvalService.submitBySession(vo.getSessionId(), vo.getComments(), userId);
        return RESTResult.success(Map.of(
                "sessionId", vo.getSessionId(),
                "submitted", results.size(),
                "list", results
        ));
    }

    @PostMapping("/review")
    @Operation(summary = "审批话术（通过/拒绝），仅管理员")
    public RESTResult<LiveScriptApprovalVO> review(
            @Valid @RequestBody ReviewApprovalVO vo,
            HttpServletRequest request) {
        Long reviewerId = requireUserId(request);
        String roleCode = AuthTokenFilter.getRoleCode(request);
        if (!"admin".equalsIgnoreCase(roleCode)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可执行审批");
        }
        return RESTResult.success(approvalService.review(vo.getApprovalId(), vo.getAction(), vo.getComments(), reviewerId));
    }

    @PostMapping("/revoke")
    @Operation(summary = "撤回审核（仅提交人可操作）")
    public RESTResult<Void> revoke(@Valid @RequestBody RevokeApprovalVO vo, HttpServletRequest request) {
        Long userId = requireUserId(request);
        approvalService.revoke(vo.getScriptId(), userId);
        return RESTResult.success(null);
    }

    @PostMapping("/search")
    @Operation(summary = "分页查询审核记录")
    public RESTResult<PageResultVO<LiveScriptApprovalVO>> search(
            @Valid @RequestBody LiveScriptApprovalSearchVO vo,
            HttpServletRequest request) {
        requireUserId(request);
        return RESTResult.success(approvalService.search(vo));
    }

    @PostMapping("/history")
    @Operation(summary = "查询话术审核历史")
    public RESTResult<List<LiveScriptApprovalVO>> history(
            @Valid @RequestBody ApprovalHistoryQueryVO vo,
            HttpServletRequest request) {
        requireUserId(request);
        return RESTResult.success(approvalService.history(vo.getScriptId()));
    }

    private Long requireUserId(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        return userId;
    }
}
