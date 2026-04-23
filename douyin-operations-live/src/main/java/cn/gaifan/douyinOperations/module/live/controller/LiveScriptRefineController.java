package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.ai.service.AiCallLogService;
import cn.gaifan.douyinOperations.module.ai.service.AiQuotaService;
import cn.gaifan.douyinOperations.module.live.service.LiveAiService;
import cn.gaifan.douyinOperations.module.live.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.OutputStream;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 直播话术精修 Controller（精修、段内微调、写作助手、相似度检测、改进建议）
 */
@RestController
@RequestMapping("/api/v1/live/ai")
@Tag(name = "直播话术精修 / Script Refinement", description = "AI 话术精修：提问式修改、段内微调、SSE 流式（需登录）")
public class LiveScriptRefineController {

    private static final Logger log = LoggerFactory.getLogger(LiveScriptRefineController.class);

    @Resource
    private LiveAiService liveAiService;

    @Resource
    private AiQuotaService aiQuotaService;

    @Resource
    private AiCallLogService aiCallLogService;

    // ==================== 精修 ====================

    @PostMapping("/refine-script")
    @Operation(summary = "AI 提问式修改话术 / Refine Script by User Question")
    public RESTResult<String> refineScript(HttpServletRequest request,
                                           @Valid @RequestBody ScriptRefineVO vo) {
        Long userId = requireUserId(request);
        String content = withAiCall(userId, "live_script_refine",
                () -> liveAiService.refineScript(vo.getScriptId(), vo.getQuestion(), userId, vo.getModelId()));
        return withTraceId(RESTResult.getSuccess(content));
    }

    @PostMapping("/refine-segment")
    @Operation(summary = "AI 段内微调 / Refine Segment Only")
    public RESTResult<String> refineSegment(HttpServletRequest request,
                                            @Valid @RequestBody ScriptRefineSegmentVO vo) {
        Long userId = requireUserId(request);
        String content = withAiCall(userId, "live_script_refine_segment", () ->
                liveAiService.refineSegment(vo.getScriptId(), vo.getSegmentText(), vo.getInstruction(), userId, vo.getModelId()));
        return withTraceId(RESTResult.getSuccess(content));
    }

    @PostMapping(value = "/refine-script-sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "AI 提问式修改话术（SSE 流式）/ Refine Script with SSE")
    public void refineScriptSse(HttpServletRequest request, HttpServletResponse response,
                                @Valid @RequestBody ScriptRefineVO vo) throws java.io.IOException {
        Long userId = requireUserId(request);
        OutputStream out = buildSseOutputStream(response);
        aiQuotaService.ensureQuota(userId);
        long start = System.currentTimeMillis();
        try {
            liveAiService.refineScriptStream(vo.getScriptId(), vo.getQuestion(), userId, out, vo.getModelId());
            aiQuotaService.consume(userId);
            aiCallLogService.log(new AiCallLogService.LogEntry(userId, "live_script_refine", null, "llm",
                    null, null, null, null, System.currentTimeMillis() - start, 1, null, false, null, null));
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg == null || (!msg.contains("Broken pipe") && !msg.contains("Connection reset"))) {
                aiCallLogService.log(new AiCallLogService.LogEntry(userId, "live_script_refine", null, "llm",
                        null, null, null, null, System.currentTimeMillis() - start, 0, msg, false, null, null));
                log.warn("refine-script-sse controller异常: {}", msg);
            }
        } finally {
            try { out.flush(); } catch (Exception e) { log.debug("SSE flush失败: {}", e.getMessage()); }
        }
    }

    // ==================== 写作助手 ====================

    @PostMapping("/chat-for-script")
    @Operation(summary = "编辑时 AI 写作助手 / AI Chat for Script Editing")
    public RESTResult<String> chatForScript(HttpServletRequest request,
                                            @Valid @RequestBody ScriptChatVO vo) {
        Long userId = requireUserId(request);
        String content = withAiCall(userId, "live_script_chat",
                () -> liveAiService.chatForScript(vo.getScriptId(), vo.getMessage(), userId, vo.getModelId()));
        return withTraceId(RESTResult.getSuccess(content));
    }

    @PostMapping(value = "/chat-for-script-sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "编辑时 AI 写作助手（SSE 流式）/ AI Chat for Script Editing with SSE")
    public void chatForScriptSse(HttpServletRequest request, HttpServletResponse response,
                                 @Valid @RequestBody ScriptChatVO vo) throws java.io.IOException {
        Long userId = requireUserId(request);
        OutputStream out = buildSseOutputStream(response);
        aiQuotaService.ensureQuota(userId);
        long start = System.currentTimeMillis();
        try {
            liveAiService.chatForScriptStream(vo.getScriptId(), vo.getMessage(), userId, out, vo.getModelId());
            aiQuotaService.consume(userId);
            aiCallLogService.log(new AiCallLogService.LogEntry(userId, "live_script_chat", null, "llm",
                    null, null, null, null, System.currentTimeMillis() - start, 1, null, false, null, null));
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg == null || (!msg.contains("Broken pipe") && !msg.contains("Connection reset"))) {
                aiCallLogService.log(new AiCallLogService.LogEntry(userId, "live_script_chat", null, "llm",
                        null, null, null, null, System.currentTimeMillis() - start, 0, msg, false, null, null));
                log.warn("chat-for-script-sse controller异常: {}", msg);
            }
        } finally {
            try { out.flush(); } catch (Exception e) { log.debug("SSE flush失败: {}", e.getMessage()); }
        }
    }

    @PostMapping("/batch-chat-for-script")
    @Operation(summary = "批量应用同一指令到多段话术 / Batch AI Chat for Scripts")
    public RESTResult<java.util.Map<Long, String>> batchChatForScript(HttpServletRequest request,
                                                                      @Valid @RequestBody BatchChatVO vo) {
        Long userId = requireUserId(request);
        java.util.List<Long> scriptIds = vo.getScriptIds().stream().distinct().toList();
        String message = vo.getMessage();
        Long modelId = vo.getModelId();
        java.util.Map<Long, String> result = new java.util.LinkedHashMap<>();
        for (Long scriptId : scriptIds) {
            String content = withAiCall(userId, "live_script_chat",
                    () -> liveAiService.chatForScript(scriptId, message, userId, modelId));
            result.put(scriptId, content);
        }
        return withTraceId(RESTResult.getSuccess(result));
    }

    // ==================== 改进建议 ====================

    @PostMapping("/suggest-improvement")
    @Operation(summary = "根据效果生成改进版话术 / Suggest Improvement by Effectiveness")
    public RESTResult<Map<String, Object>> suggestImprovement(HttpServletRequest request,
                                                              @Valid @RequestBody ScriptIdVO vo) {
        Long userId = requireUserId(request);
        String content = withAiCall(userId, "live_script_improve",
                () -> liveAiService.suggestImprovement(vo.getScriptId(), userId, vo.getModelId()));
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        if (content != null) {
            result.put("improved", content);
            result.put("skipped", false);
        } else {
            result.put("improved", null);
            result.put("skipped", true);
            result.put("reason", "话术效果良好或暂无效果数据，无需改进");
        }
        return withTraceId(RESTResult.getSuccess(result));
    }

    // ==================== 相似度检测 ====================

    @PostMapping("/check-similarity")
    @Operation(summary = "相似度检测 / Check Script Similarity")
    public RESTResult<java.util.List<SimilarityItemVO>> checkSimilarity(HttpServletRequest request,
                                                                        @Valid @RequestBody SessionIdVO vo) {
        Long userId = requireUserId(request);
        java.util.List<SimilarityItemVO> data = withAiCall(userId, "live_script_similarity",
                () -> liveAiService.checkSimilarity(vo.getSessionId(), userId));
        return withTraceId(RESTResult.getSuccess(data));
    }

    // ==================== 私有辅助方法 ====================

    private Long requireUserId(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        return userId;
    }

    private <T> T withAiCall(Long userId, String callType, Supplier<T> supplier) {
        aiQuotaService.ensureQuota(userId);
        long start = System.currentTimeMillis();
        try {
            T result = supplier.get();
            aiQuotaService.consume(userId);
            aiCallLogService.log(new AiCallLogService.LogEntry(userId, callType, null, "llm",
                    null, null, null, null, System.currentTimeMillis() - start, 1, null, false, null, null));
            return result;
        } catch (Exception e) {
            aiCallLogService.log(new AiCallLogService.LogEntry(userId, callType, null, "llm",
                    null, null, null, null, System.currentTimeMillis() - start, 0, e.getMessage(), false, null, null));
            throw e;
        }
    }

    private <T> RESTResult<T> withTraceId(RESTResult<T> r) {
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    private OutputStream buildSseOutputStream(HttpServletResponse response) throws java.io.IOException {
        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Connection", "keep-alive");
        response.setBufferSize(1);
        return new java.io.FilterOutputStream(response.getOutputStream()) {
            @Override
            public void flush() throws java.io.IOException {
                super.flush();
                response.flushBuffer();
            }
        };
    }
}
