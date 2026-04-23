package cn.gaifan.douyinOperations.module.ai.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.ai.service.AiCallLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "AI 调用日志")
@RestController
@RequestMapping("/api/v1/ai/call-log")
public class AiCallLogController {

    @Resource
    private AiCallLogService aiCallLogService;

    @SuppressWarnings("unused")
    @Operation(summary = "关联调用记录与发布内容（效果归因回填）")
    @PostMapping("/link")
    public RESTResult<Void> link(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request
    ) {
        Long userId = getUserId(request);
        Long callLogId = body.get("callLogId") != null
                ? Long.valueOf(body.get("callLogId").toString()) : null;
        Long videoId = body.get("videoId") != null
                ? Long.valueOf(body.get("videoId").toString()) : null;
        Long sessionId = body.get("sessionId") != null
                ? Long.valueOf(body.get("sessionId").toString()) : null;
        if (callLogId == null || (videoId == null && sessionId == null)) {
            return RESTResult.error(ErrorCode.INVALID_PARAMS, "callLogId 与 videoId/sessionId 至少各提供一个");
        }
        aiCallLogService.linkToPublish(callLogId, videoId, sessionId);
        return RESTResult.getSuccess(null);
    }

    private Long getUserId(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        return userId;
    }
}
