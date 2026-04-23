package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.common.annotation.CurrentUserId;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.live.entity.LiveApprovalLog;
import cn.gaifan.douyinOperations.module.live.entity.LiveSession;
import cn.gaifan.douyinOperations.module.live.service.LiveApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 直播话术审批 Controller
 *
 * @deprecated 请使用统一审批入口 {@code /api/v1/live/script-approval} 代替：
 * <ul>
 *   <li>整场提交 → POST /api/v1/live/script-approval/submit-by-session</li>
 *   <li>单条提交 → POST /api/v1/live/script-approval/submit</li>
 *   <li>审批通过/拒绝 → POST /api/v1/live/script-approval/review</li>
 *   <li>待审批列表 → POST /api/v1/live/script-approval/search?status=1</li>
 * </ul>
 * 本 Controller 保留以兼容旧调用方，后续版本将移除。
 */
@Deprecated
@RestController
@RequestMapping("/api/v1/live/approval")
@Tag(name = "直播审批（已废弃）/ Live Approval (Deprecated)", description = "已废弃，请使用 /api/v1/live/script-approval")
public class LiveApprovalController {

    @Resource
    private LiveApprovalService liveApprovalService;

    @PostMapping("/submit")
    @Operation(summary = "提交审批 / Submit for Approval",
            description = "将场次下所有话术提交审批（需登录）")
    public RESTResult<Void> submit(@CurrentUserId Long userId,
                                   @RequestBody Map<String, Object> body) {
        Long sessionId = parseSessionId(body);
        if (sessionId == null) return withTraceId(RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 sessionId"));
        liveApprovalService.submitForApproval(sessionId, userId);
        return withTraceId(RESTResult.success("已提交审批", null));
    }

    @PostMapping("/approve")
    @Operation(summary = "审批通过 / Approve",
            description = "通过指定话术的审批（需登录）")
    public RESTResult<Void> approve(@CurrentUserId Long userId,
                                    @RequestBody Map<String, Object> body) {
        Long sessionId = parseSessionId(body);
        Long scriptId = parseLong(body, "scriptId");
        String comment = body != null && body.get("comment") instanceof String s ? s : null;
        if (sessionId == null) return withTraceId(RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 sessionId"));
        if (scriptId == null) return withTraceId(RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 scriptId"));
        liveApprovalService.approve(sessionId, scriptId, userId, comment);
        return withTraceId(RESTResult.success("审批通过", null));
    }

    @PostMapping("/reject")
    @Operation(summary = "审批拒绝 / Reject",
            description = "拒绝指定话术的审批（需登录）")
    public RESTResult<Void> reject(@CurrentUserId Long userId,
                                   @RequestBody Map<String, Object> body) {
        Long sessionId = parseSessionId(body);
        Long scriptId = parseLong(body, "scriptId");
        String comment = body != null && body.get("comment") instanceof String s ? s : null;
        if (sessionId == null) return withTraceId(RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 sessionId"));
        if (scriptId == null) return withTraceId(RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 scriptId"));
        liveApprovalService.reject(sessionId, scriptId, userId, comment);
        return withTraceId(RESTResult.success("审批已拒绝", null));
    }

    @PostMapping("/history")
    @Operation(summary = "审批历史 / Approval History",
            description = "查看场次的审批历史记录（需登录）")
    public RESTResult<List<LiveApprovalLog>> history(@CurrentUserId Long userId,
                                                     @RequestBody Map<String, Object> body) {
        Long sessionId = parseSessionId(body);
        if (sessionId == null) return withTraceId(RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 sessionId"));
        List<LiveApprovalLog> data = liveApprovalService.getApprovalHistory(sessionId);
        return withTraceId(RESTResult.getSuccess(data));
    }

    @PostMapping("/pending")
    @Operation(summary = "待审批列表 / Pending Approvals",
            description = "查看待审批的场次列表（需登录）")
    public RESTResult<List<LiveSession>> pending(@CurrentUserId Long userId) {
        List<LiveSession> data = liveApprovalService.getPendingApprovals(userId);
        return withTraceId(RESTResult.getSuccess(data));
    }

    private <T> RESTResult<T> withTraceId(RESTResult<T> r) {
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    private static Long parseSessionId(Map<String, Object> body) {
        return parseLong(body, "sessionId");
    }

    private static Long parseLong(Map<String, Object> body, String key) {
        if (body == null) return null;
        Object v = body.get(key);
        if (v == null) return null;
        return v instanceof Number n ? n.longValue() : Long.parseLong(v.toString());
    }
}
