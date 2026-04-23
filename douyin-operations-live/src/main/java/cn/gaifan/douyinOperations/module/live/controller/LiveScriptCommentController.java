package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.common.annotation.CurrentUserId;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.live.service.LiveScriptCommentService;
import cn.gaifan.douyinOperations.module.live.vo.LiveScriptCommentSaveVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveScriptCommentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 话术行内评论 Controller
 * Live Script Inline Comment Controller
 */
@RestController
@RequestMapping("/api/v1/live/script-comment")
@Tag(name = "话术行内评论 / Script Inline Comment", description = "话术行内评论管理（需登录）")
public class LiveScriptCommentController {

    @Resource
    private LiveScriptCommentService commentService;

    @PostMapping("/by-script")
    @Operation(summary = "获取话术的评论列表 / Get Comments by Script")
    public RESTResult<List<LiveScriptCommentVO>> getByScript(
            @CurrentUserId Long userId,
            @RequestBody(required = false) Map<String, Object> body) {
        Long scriptId = parseLong(body, "scriptId");
        if (scriptId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 scriptId");
        List<LiveScriptCommentVO> data = commentService.getByScript(scriptId);
        return withTraceId(RESTResult.getSuccess(data));
    }

    @PostMapping("/by-session")
    @Operation(summary = "获取场次的所有评论 / Get Comments by Session")
    public RESTResult<List<LiveScriptCommentVO>> getBySession(
            @CurrentUserId Long userId,
            @RequestBody(required = false) Map<String, Object> body) {
        Long sessionId = parseLong(body, "sessionId");
        if (sessionId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 sessionId");
        List<LiveScriptCommentVO> data = commentService.getBySession(sessionId);
        return withTraceId(RESTResult.getSuccess(data));
    }

    @PostMapping("/save")
    @Operation(summary = "添加评论 / Add Comment")
    public RESTResult<LiveScriptCommentVO> save(
            @CurrentUserId Long userId,
            @Valid @RequestBody LiveScriptCommentSaveVO vo) {
        LiveScriptCommentVO data = commentService.addComment(vo, userId);
        return withTraceId(RESTResult.addSuccess(data));
    }

    @PostMapping("/resolve")
    @Operation(summary = "标记评论已解决 / Resolve Comment")
    public RESTResult<Void> resolve(
            @CurrentUserId Long userId,
            @RequestBody(required = false) Map<String, Object> body) {
        Long commentId = parseLong(body, "commentId");
        if (commentId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 commentId");
        commentService.resolveComment(commentId, userId);
        return withTraceId(RESTResult.updateSuccess(null));
    }

    @PostMapping("/delete")
    @Operation(summary = "删除评论 / Delete Comment")
    public RESTResult<Integer> delete(
            @CurrentUserId Long userId,
            @RequestBody(required = false) Map<String, Object> body) {
        Long commentId = parseLong(body, "commentId");
        if (commentId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 commentId");
        commentService.deleteComment(commentId, userId);
        Integer data = 1;
        return withTraceId(RESTResult.deleteSuccess(data));
    }

    @PostMapping("/unresolved-count")
    @Operation(summary = "场次未解决评论数 / Unresolved Comment Count")
    public RESTResult<Long> unresolvedCount(
            @CurrentUserId Long userId,
            @RequestBody(required = false) Map<String, Object> body) {
        Long sessionId = parseLong(body, "sessionId");
        if (sessionId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 sessionId");
        long count = commentService.countUnresolved(sessionId);
        return withTraceId(RESTResult.getSuccess(count));
    }

    @PostMapping("/unresolved-by-script")
    @Operation(summary = "按话术统计未解决评论数 / Unresolved Count by Script")
    public RESTResult<Map<Long, Long>> unresolvedByScript(
            @CurrentUserId Long userId,
            @RequestBody(required = false) Map<String, Object> body) {
        Long sessionId = parseLong(body, "sessionId");
        if (sessionId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 sessionId");
        Map<Long, Long> data = commentService.countUnresolvedByScript(sessionId);
        return withTraceId(RESTResult.getSuccess(data));
    }

    private <T> RESTResult<T> withTraceId(RESTResult<T> r) {
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    private static Long parseLong(Map<String, Object> body, String key) {
        if (body == null) return null;
        Object v = body.get(key);
        if (v == null) return null;
        return v instanceof Number ? ((Number) v).longValue() : Long.parseLong(v.toString());
    }
}
