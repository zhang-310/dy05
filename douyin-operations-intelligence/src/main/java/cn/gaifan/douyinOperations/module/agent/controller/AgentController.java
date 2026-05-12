package cn.gaifan.douyinOperations.module.agent.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.agent.service.impl.AgentServiceImpl;
import cn.gaifan.douyinOperations.module.agent.service.AgentShareService;
import cn.gaifan.douyinOperations.module.agent.vo.AgentSearchVO;
import cn.gaifan.douyinOperations.module.agent.vo.AgentSaveVO;
import cn.gaifan.douyinOperations.module.agent.vo.AgentShareRequestVO;
import cn.gaifan.douyinOperations.module.agent.vo.AgentShareVO;
import cn.gaifan.douyinOperations.module.agent.vo.AgentVO;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

@RestController
@RequestMapping("/api/v1/agent")
@Tag(name = "智能体 / Agent", description = "智能体管理（需登录）")
public class AgentController {

    @Resource
    private AgentServiceImpl agentService;

    @Resource
    private AgentShareService agentShareService;

    // P0-7: 注入共享线程池（避免每次 SSE 请求创建新线程池）
    @Resource(name = "sseExecutor")
    private ExecutorService sseExecutor;

    // ==================== Agent ====================

    @PostMapping("/list")
    @Operation(summary = "智能体列表（分页）")
    public RESTResult<PageResultVO<AgentVO>> list(HttpServletRequest request, @Valid @RequestBody AgentSearchVO searchVO) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        RESTResult<PageResultVO<AgentVO>> r = RESTResult.getSuccess(
                agentService.searchAgents(userId, searchVO));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/get")
    @Operation(summary = "智能体详情")
    public RESTResult<AgentVO> get(HttpServletRequest request, @RequestBody(required = false) Map<String, Object> body) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long id = body != null && body.get("id") != null ? Long.parseLong(body.get("id").toString()) : null;
        if (id == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "id 不能为空");
        RESTResult<AgentVO> r = RESTResult.getSuccess(agentService.getAgentById(id));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/save")
    @Operation(summary = "新增/更新智能体")
    public RESTResult<Long> save(HttpServletRequest request, @Valid @RequestBody AgentSaveVO saveVO) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        RESTResult<Long> r = RESTResult.addSuccess(agentService.saveAgent(userId, saveVO));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/delete")
    @Operation(summary = "删除智能体")
    public RESTResult<Void> delete(HttpServletRequest request, @RequestParam Long id) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        agentService.deleteAgent(id, userId);
        RESTResult<Void> r = RESTResult.deleteSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/update-status")
    @Operation(summary = "启用/禁用智能体")
    public RESTResult<Void> updateStatus(HttpServletRequest request, @RequestParam Long id, @RequestParam Integer status) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        agentService.updateAgentStatus(id, status);
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    // ==================== Conversation ====================

    @PostMapping("/conversation/create")
    @Operation(summary = "创建对话")
    public RESTResult<Long> createConversation(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long agentId = Long.parseLong(body.get("agentId").toString());
        String topic = (String) body.getOrDefault("topic", "新对话");
        RESTResult<Long> r = RESTResult.addSuccess(agentService.createConversation(agentId, userId, topic));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/conversation/list")
    @Operation(summary = "对话列表")
    public RESTResult<List<Map<String, Object>>> listConversations(HttpServletRequest request,
            @RequestBody(required = false) Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long agentId = null;
        if (body != null && body.get("agentId") != null) {
            agentId = Long.parseLong(body.get("agentId").toString());
        }
        RESTResult<List<Map<String, Object>>> r = RESTResult.getSuccess(agentService.listConversations(userId, agentId));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/conversation/delete")
    @Operation(summary = "删除对话")
    public RESTResult<Void> deleteConversation(HttpServletRequest request, @RequestParam Long id) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        agentService.deleteConversation(id, userId);
        RESTResult<Void> r = RESTResult.deleteSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    // ==================== Message ====================

    @PostMapping("/message/send")
    @Operation(summary = "发送消息")
    public RESTResult<Long> sendMessage(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long conversationId = Long.parseLong(body.get("conversationId").toString());
        Integer senderType = Integer.parseInt(body.get("senderType").toString());
        String content = (String) body.get("content");
        Integer tokens = body.get("tokens") != null ? Integer.parseInt(body.get("tokens").toString()) : 0;
        RESTResult<Long> r = RESTResult.addSuccess(agentService.sendMessage(conversationId, senderType, content, tokens));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/message/list")
    @Operation(summary = "消息列表（分页）")
    public RESTResult<PageResultVO<Map<String, Object>>> listMessages(HttpServletRequest request,
            @RequestBody(required = false) Map<String, Object> body) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long conversationId = body != null && body.get("conversationId") != null
                ? Long.parseLong(body.get("conversationId").toString()) : null;
        if (conversationId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "conversationId 不能为空");

        // P0-4: 分页参数（默认第 0 页，每页 50 条）
        int page = body != null && body.get("page") != null ? Integer.parseInt(body.get("page").toString()) : 0;
        int rows = body != null && body.get("rows") != null ? Integer.parseInt(body.get("rows").toString()) : 50;

        RESTResult<PageResultVO<Map<String, Object>>> r = RESTResult.getSuccess(agentService.listMessages(conversationId, page, rows));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/message/rate")
    @Operation(summary = "评价消息（👍👎）")
    public RESTResult<Void> rateMessage(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long messageId = Long.parseLong(body.get("messageId").toString());
        String rating = (String) body.get("rating");
        if (!"up".equals(rating) && !"down".equals(rating)) {
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, "rating 只能是 up 或 down");
        }
        agentService.rateMessage(messageId, rating);
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/conversation/export")
    @Operation(summary = "导出会话为 Markdown")
    public RESTResult<Map<String, String>> exportConversation(HttpServletRequest request,
            @RequestBody Map<String, Object> body) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long conversationId = Long.parseLong(body.get("conversationId").toString());
        String downloadUrl = agentService.exportConversation(conversationId);
        Map<String, String> result = new java.util.HashMap<>();
        result.put("downloadUrl", downloadUrl);
        RESTResult<Map<String, String>> r = RESTResult.getSuccess(result);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    // ==================== Share ====================

    @PostMapping("/share/create")
    @Operation(summary = "创建分享")
    public RESTResult<AgentShareVO> createShare(HttpServletRequest request, @RequestBody AgentShareRequestVO shareRequest) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        AgentShareVO share = agentShareService.createShare(userId, shareRequest);
        RESTResult<AgentShareVO> res = RESTResult.addSuccess(share);
        res.setTraceId(MDC.get("traceId"));
        return res;
    }

    @PostMapping("/share/get")
    @Operation(summary = "通过分享码获取分享信息")
    public RESTResult<AgentShareVO> getShare(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        String shareCode = (String) body.get("shareCode");
        if (shareCode == null || shareCode.isBlank()) {
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, "shareCode 不能为空");
        }
        AgentShareVO share = agentShareService.getByShareCode(shareCode);
        RESTResult<AgentShareVO> res = RESTResult.getSuccess(share);
        res.setTraceId(MDC.get("traceId"));
        return res;
    }

    @PostMapping("/share/list")
    @Operation(summary = "我的分享列表")
    public RESTResult<List<AgentShareVO>> listMyShares(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        List<AgentShareVO> shares = agentShareService.listByUser(userId);
        RESTResult<List<AgentShareVO>> res = RESTResult.getSuccess(shares);
        res.setTraceId(MDC.get("traceId"));
        return res;
    }

    @PostMapping("/share/delete")
    @Operation(summary = "删除分享")
    public RESTResult<Void> deleteShare(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long shareId = Long.parseLong(body.get("shareId").toString());
        agentShareService.deleteShare(userId, shareId);
        RESTResult<Void> res = RESTResult.deleteSuccess(null);
        res.setTraceId(MDC.get("traceId"));
        return res;
    }

    @PostMapping("/share/data")
    @Operation(summary = "获取分享的对话数据（用于导入）")
    public RESTResult<Object> getShareData(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        String shareCode = (String) body.get("shareCode");
        if (shareCode == null || shareCode.isBlank()) {
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, "shareCode 不能为空");
        }
        Object data = agentShareService.getShareConversationData(shareCode);
        RESTResult<Object> res = RESTResult.getSuccess(data);
        res.setTraceId(MDC.get("traceId"));
        return res;
    }

    // ==================== SSE 流式对话 ====================

    @PostMapping(value = "/chat-stream", produces = "text/event-stream;charset=UTF-8")
    @Operation(summary = "SSE 流式对话")
    @RateLimiter(name = "aiChat", fallbackMethod = "chatStreamFallback")
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter chatStream(
            HttpServletRequest request,
            @RequestBody Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter = new org.springframework.web.servlet.mvc.method.annotation.SseEmitter(300_000L);
        if (userId == null) {
            try {
                emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                        .name("error")
                        .data("{\"message\":\"未登录\"}"));
                emitter.complete();
            } catch (Exception e) { emitter.completeWithError(e); }
            return emitter;
        }
        Long agentId = body.get("agentId") != null ? Long.parseLong(body.get("agentId").toString()) : null;
        Long conversationId = body.get("conversationId") != null ? Long.parseLong(body.get("conversationId").toString()) : null;
        String content = body.get("content") != null ? body.get("content").toString() : "";
        final com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

        // P0-7: 使用共享线程池（避免线程泄漏）
        sseExecutor.execute(() -> {
            try {
                emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                        .name("status")
                        .data("{\"status\":\"思考中...\"}"));

                // 发送 SSE 状态事件发射器（BiConsumer<eventName, eventData>）
                java.util.function.BiConsumer<String, String> statusEmitter = (eventName, eventData) -> {
                    try {
                        if ("skill_start".equals(eventName)) {
                            // skill_start: data=toolName|description
                            String[] parts = eventData.split("\\|", 2);
                            String json = mapper.writeValueAsString(java.util.Map.of(
                                "type", "skill_start",
                                "tool", parts[0],
                                "description", parts.length > 1 ? parts[1] : ""
                            ));
                            emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                                    .name("skill_start").data(json));
                        } else if ("skill_end".equals(eventName)) {
                            // skill_end: data=toolName|success 或 toolName|error|message
                            String[] parts = eventData.split("\\|", 3);
                            String toolName2 = parts[0];
                            boolean isSuccess = parts.length > 1 && "success".equals(parts[1]);
                            String errorMsg = !isSuccess && parts.length > 2 ? parts[2] : null;
                            String json = mapper.writeValueAsString(java.util.Map.of(
                                "type", "skill_end",
                                "tool", toolName2,
                                "status", isSuccess ? "success" : "error",
                                "error", errorMsg != null ? errorMsg : ""
                            ));
                            emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                                    .name("skill_end").data(json));
                        } else if ("tool_start".equals(eventName)) {
                            String json = mapper.writeValueAsString(java.util.Map.of(
                                "type", "tool_start",
                                "tool", eventData
                            ));
                            emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                                    .name("tool_start").data(json));
                        } else if ("tool_end".equals(eventName)) {
                            String json = mapper.writeValueAsString(java.util.Map.of(
                                "type", "tool_end",
                                "tool", eventData
                            ));
                            emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                                    .name("tool_end").data(json));
                        }
                    } catch (Exception ignored) {}
                };
                String reply = agentService.chatWithAgent(agentId, userId, content, conversationId, null, statusEmitter);
                if (reply == null) reply = "(无回复)";

                // 流式输出每个字符/词
                String[] words = reply.split("(?<=[\n，。、！？；：.!?,;:\n])");
                for (String word : words) {
                    if (word.isEmpty()) continue;
                    // P0-1: XSS 防护 - HTML 转义
                    String safeWord = StringEscapeUtils.escapeHtml4(word);
                    String json = mapper.writeValueAsString(java.util.Map.of("type", "chunk", "content", safeWord));
                    emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                            .name("chunk")
                            .data(json));
                    Thread.sleep(20);
                }

                // P0-1: XSS 防护 - HTML 转义
                String safeReply = StringEscapeUtils.escapeHtml4(reply);
                String doneJson = mapper.writeValueAsString(java.util.Map.of("type", "done", "content", safeReply));
                emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                        .name("done")
                        .data(doneJson));
                emitter.complete();
            } catch (Exception e) {
                try {
                    String errorJson = mapper.writeValueAsString(java.util.Map.of("type", "error", "message", e.getMessage()));
                    emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                            .name("error")
                            .data(errorJson));
                    emitter.complete();
                } catch (Exception ex) { emitter.completeWithError(ex); }
            }
        });
        return emitter;
    }

    // P1-1: AI 对话限流 fallback 方法
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter chatStreamFallback(
            HttpServletRequest request, Map<String, Object> body, Exception e) {
        org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter =
            new org.springframework.web.servlet.mvc.method.annotation.SseEmitter(5_000L);
        try {
            emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                    .name("error")
                    .data("{\"message\":\"请求频率过高，请稍后再试\"}"));
            emitter.complete();
        } catch (Exception ex) {
            emitter.completeWithError(ex);
        }
        return emitter;
    }

    @GetMapping(value = "/sse-diagnostic", produces = "text/event-stream;charset=UTF-8")
    @Operation(summary = "SSE 诊断端点（不调 LLM）")
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter sseDiagnostic() {
        org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter = new org.springframework.web.servlet.mvc.method.annotation.SseEmitter(30_000L);
        // P0-7: 使用共享线程池（避免线程泄漏）
        sseExecutor.execute(() -> {
            try {
                for (int i = 1; i <= 5; i++) {
                    Thread.sleep(1000);
                    emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                            .data("chunk " + i));
                }
                emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event().data("[DONE]"));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }
}
