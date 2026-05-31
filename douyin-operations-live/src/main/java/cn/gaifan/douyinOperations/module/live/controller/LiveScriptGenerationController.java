package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.ai.service.AiCallLogService;
import cn.gaifan.douyinOperations.module.ai.service.AiQuotaService;
import cn.gaifan.douyinOperations.module.live.config.LiveGenerationAmqpConfig;
import cn.gaifan.douyinOperations.module.live.entity.LiveGenerationTask;
import cn.gaifan.douyinOperations.module.live.entity.LiveProduct;
import cn.gaifan.douyinOperations.module.live.repository.LiveProductRepository;
import cn.gaifan.douyinOperations.module.live.service.LiveAiService;
import cn.gaifan.douyinOperations.module.live.service.LiveFullGenerationProgressTracker;
import cn.gaifan.douyinOperations.module.live.service.LiveGenerationTaskService;
import cn.gaifan.douyinOperations.module.live.vo.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * 直播话术生成 Controller（含单段/整轨/并行/骨架/异步/SSE 生成端点）
 */
@RestController
@RequestMapping("/api/v1/live/ai")
@Tag(name = "直播话术生成 / Script Generation", description = "AI 话术生成：单段、整轨、并行、骨架、异步队列（需登录）")
public class LiveScriptGenerationController {

    private static final Logger log = LoggerFactory.getLogger(LiveScriptGenerationController.class);
    private static final ScheduledExecutorService HEARTBEAT_SCHEDULER = Executors.newScheduledThreadPool(1, r -> {
        Thread t = new Thread(r, "generate-full-sse-heartbeat");
        t.setDaemon(true);
        return t;
    });

    @Resource
    private LiveFullGenerationProgressTracker fullGenerationProgressTracker;

    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;

    @Value("${app.live.generation.async.enabled:true}")
    private boolean liveAsyncGenerationEnabled;

    @Resource
    private LiveAiService liveAiService;

    @Resource
    private LiveGenerationTaskService generationTaskService;

    @Resource
    private AiQuotaService aiQuotaService;

    @Resource
    private AiCallLogService aiCallLogService;

    @Resource
    private LiveProductRepository liveProductRepository;

    @Resource
    private cn.gaifan.douyinOperations.module.live.service.LiveScriptGenerationService liveScriptGenerationService;

    @Resource
    private ObjectMapper objectMapper;

    // ==================== 单段生成（已废弃，请使用 generate-full-sse 或 generate-slot-sse）====================

    /**
     * @deprecated 请使用全场生成接口 {@code /generate-full-sse} 或单槽接口 {@code /generate-slot-sse}。
     *             此接口保留以兼容旧调用，后续版本将移除。
     */
    @PostMapping("/generate-opening")
    @Operation(summary = "生成开场话术（已废弃）/ Generate Opening Script (Deprecated)",
            description = "⚠️ 已废弃：请改用 /generate-full-sse（整场）或 /generate-slot-sse（单槽流式）")
    public RESTResult<LiveAiResultVO> generateOpening(HttpServletRequest request,
                                                      @Valid @RequestBody LiveAiGenerateVO vo) {
        Long userId = requireUserId(request);
        LiveAiResultVO data = withAiCall(userId, "live_script_opening", () -> liveAiService.generateOpening(vo));
        return withTraceId(RESTResult.getSuccess(data));
    }

    /**
     * @deprecated 请使用全场生成接口 {@code /generate-full-sse} 或单槽接口 {@code /generate-slot-sse}。
     */
    @PostMapping("/generate-product")
    @Operation(summary = "生成商品介绍话术（已废弃）/ Generate Product Script (Deprecated)",
            description = "⚠️ 已废弃：请改用 /generate-full-sse（整场）或 /generate-slot-sse（单槽流式）")
    public RESTResult<LiveAiResultVO> generateProduct(HttpServletRequest request,
                                                      @Valid @RequestBody LiveAiGenerateVO vo) {
        Long userId = requireUserId(request);
        LiveAiResultVO data = withAiCall(userId, "live_script_product", () -> liveAiService.generateProduct(vo));
        return withTraceId(RESTResult.getSuccess(data));
    }

    /**
     * @deprecated 请使用全场生成接口 {@code /generate-full-sse} 或单槽接口 {@code /generate-slot-sse}。
     */
    @PostMapping("/generate-transition")
    @Operation(summary = "生成过渡话术（已废弃）/ Generate Transition Script (Deprecated)",
            description = "⚠️ 已废弃：请改用 /generate-full-sse（整场）或 /generate-slot-sse（单槽流式）")
    public RESTResult<LiveAiResultVO> generateTransition(HttpServletRequest request,
                                                         @Valid @RequestBody LiveAiGenerateVO vo) {
        Long userId = requireUserId(request);
        LiveAiResultVO data = withAiCall(userId, "live_script_transition", () -> liveAiService.generateTransition(vo));
        return withTraceId(RESTResult.getSuccess(data));
    }

    /**
     * @deprecated 请使用全场生成接口 {@code /generate-full-sse} 或单槽接口 {@code /generate-slot-sse}。
     */
    @PostMapping("/generate-closing")
    @Operation(summary = "生成收尾话术（已废弃）/ Generate Closing Script (Deprecated)",
            description = "⚠️ 已废弃：请改用 /generate-full-sse（整场）或 /generate-slot-sse（单槽流式）")
    public RESTResult<LiveAiResultVO> generateClosing(HttpServletRequest request,
                                                      @Valid @RequestBody LiveAiGenerateVO vo) {
        Long userId = requireUserId(request);
        LiveAiResultVO data = withAiCall(userId, "live_script_closing", () -> liveAiService.generateClosing(vo));
        return withTraceId(RESTResult.getSuccess(data));
    }

    @PostMapping("/generate-slot")
    @Operation(summary = "按槽位需求生成单段话术（不保存，供 AI 分析师自动填充）")
    public RESTResult<String> generateForSlot(HttpServletRequest request,
                                              @Valid @RequestBody SlotGenerateVO vo) {
        Long userId = requireUserId(request);
        String content = withAiCall(userId, "live_script_slot",
                () -> liveAiService.generateForSlot(vo.getScriptId(), vo.getRequirement(), vo.getDurationLimitSec(), vo.getModelId()));
        return withTraceId(RESTResult.getSuccess(content));
    }

    @PostMapping(value = "/generate-slot-sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "按槽位需求生成单段话术（SSE 流式）")
    public void generateForSlotSse(HttpServletRequest request, HttpServletResponse response,
                                   @Valid @RequestBody SlotGenerateVO vo) throws java.io.IOException {
        Long userId = requireUserId(request);
        Long scriptId = vo.getScriptId();
        OutputStream out = buildSseOutputStream(response);
        aiQuotaService.ensureQuota(userId);
        long start = System.currentTimeMillis();
        try {
            liveAiService.generateForSlotStream(scriptId, vo.getRequirement(), vo.getDurationLimitSec(), vo.getModelId(), out);
            aiQuotaService.consume(userId);
            aiCallLogService.log(new AiCallLogService.LogEntry(userId, "live_script_slot", null, "llm",
                    null, null, null, null, System.currentTimeMillis() - start, 1, null, false, null, null));
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg == null || (!msg.contains("Broken pipe") && !msg.contains("Connection reset"))) {
                aiCallLogService.log(new AiCallLogService.LogEntry(userId, "live_script_slot", null, "llm",
                        null, null, null, null, System.currentTimeMillis() - start, 0, msg, false, null, null));
                log.warn("generate-slot-sse controller异常: {}", msg);
            }
        } finally {
            try { out.flush(); } catch (Exception e) { log.debug("SSE flush失败: {}", e.getMessage()); }
        }
    }

    // ==================== 整轨生成 ====================

    @PostMapping("/generate-full")
    @Operation(summary = "生成完整话术流程 / Generate Full Script Flow")
    public RESTResult<List<LiveAiResultVO>> generateFull(HttpServletRequest request,
                                                         @Valid @RequestBody LiveAiGenerateVO vo) {
        Long userId = requireUserId(request);
        LiveAiFullResultVO full = withAiCallFull(userId, "live_script_full", () -> liveAiService.generateFull(vo));
        return withTraceId(RESTResult.getSuccess(full.getResults()));
    }

    @PostMapping(value = "/generate-full-sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "生成完整话术流程（SSE 进度推送）/ Generate Full Script Flow with SSE Progress")
    public void generateFullSse(HttpServletRequest request, HttpServletResponse response,
                                @Valid @RequestBody LiveAiGenerateVO vo) throws java.io.IOException {
        log.info("generate-full-sse 收到请求: sessionId={}, modelId={}, style={}", vo.getSessionId(), vo.getModelId(), vo.getStyle());
        Long userId = requireUserId(request);
        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Connection", "keep-alive");
        response.setBufferSize(1);
        OutputStream out = new java.io.FilterOutputStream(response.getOutputStream()) {
            @Override
            public void flush() throws java.io.IOException {
                super.flush();
                response.flushBuffer();
            }
        };
        aiQuotaService.ensureQuota(userId);
        Long sessionId = vo.getSessionId();
        if (sessionId != null) {
            fullGenerationProgressTracker.markSession(sessionId);
        }
        Long taskId = null;
        try {
            LiveGenerationTask task = generationTaskService.createTask(
                    sessionId, userId, 0,
                    vo.getStyle(), vo.getModelId(),
                    vo.getUseKbRef() == null || vo.getUseKbRef(),
                    vo.getHotKeywords());
            taskId = task.getId();
            generationTaskService.startTask(taskId);
        } catch (Exception e) {
            log.warn("Failed to persist generation task for sessionId={}: {}", sessionId, e.getMessage());
        }
        final Long finalTaskId = taskId;
        long start = System.currentTimeMillis();
        Object writeLock = new Object();
        AtomicBoolean heartbeatStopped = new AtomicBoolean(false);
        ScheduledFuture<?> heartbeat = HEARTBEAT_SCHEDULER.scheduleAtFixedRate(() -> {
            if (heartbeatStopped.get()) return;
            try {
                synchronized (writeLock) {
                    out.write(": heartbeat\n\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    out.flush();
                }
            } catch (Exception e) { log.debug("心跳发送失败: {}", e.getMessage()); }
        }, 10, 10, TimeUnit.SECONDS);
        try {
            synchronized (writeLock) {
                writeSseEvent(out, "progress", Map.of("current", 0, "total", 1, "percent", 0, "slotType", "连接成功，准备生成"));
            }
            log.info("generate-full-sse [sessionId={}] SSE已发送: progress 0/1 连接成功 (首条)", vo.getSessionId());
            LiveAiFullResultVO full = liveScriptGenerationService.generateFullPipelined(vo, new LiveAiService.FullGenerateProgressCallback() {
                @Override
                public void onProgress(int current, int total, String slotType) {
                    if (finalTaskId != null && current == 0 && total > 0) {
                        try {
                            generationTaskService.getActiveTask(vo.getSessionId()).ifPresent(t -> {
                                if (t.getTotalSlots() == 0) {
                                    t.setTotalSlots(total);
                                }
                            });
                        } catch (Exception e) { log.warn("getActiveTask失败: {}", e.getMessage()); }
                    }
                    try {
                        synchronized (writeLock) {
                            writeSseEvent(out, "progress", Map.of("current", current, "total", total,
                                    "percent", total > 0 ? (int) (100.0 * current / total) : 0,
                                    "slotType", slotType != null ? slotType : ""));
                        }
                        log.debug("generate-full-sse [sessionId={}] SSE已发送: progress {}/{} {}",
                                vo.getSessionId(), current, total, slotType);
                    } catch (Exception e) {
                        log.warn("generate-full-sse progress send failed: {}", e.getMessage());
                    }
                }

                @Override
                public void onSlotDone(Long scriptId, String content, String scriptType, String slotLabel, Integer sequenceNo, Integer index) {
                    if (finalTaskId != null) {
                        try { generationTaskService.onSlotCompleted(finalTaskId); } catch (Exception e) {
                            log.warn("onSlotCompleted 失败 taskId={}: {}", finalTaskId, e.getMessage());
                        }
                    }
                    try {
                        var data = new LinkedHashMap<String, Object>();
                        data.put("scriptId", scriptId != null ? scriptId : 0);
                        data.put("content", content != null ? content : "");
                        data.put("scriptType", scriptType != null ? scriptType : "");
                        data.put("slotLabel", slotLabel != null ? slotLabel : "");
                        if (sequenceNo != null) data.put("sequenceNo", sequenceNo);
                        if (index != null) data.put("index", index);
                        synchronized (writeLock) {
                            writeSseEvent(out, "slot_done", data);
                        }
                        log.debug("generate-full-sse [sessionId={}] SSE已发送: slot_done {}", vo.getSessionId(), slotLabel);
                    } catch (Exception e) {
                        log.warn("generate-full-sse slot_done send failed: {}", e.getMessage());
                    }
                }

                @Override
                public void onSlotFailed(Long scriptId, String slotLabel, String errorMsg, Integer index) {
                    if (finalTaskId != null) {
                        try { generationTaskService.onSlotFailed(finalTaskId); } catch (Exception e) {
                            log.warn("onSlotFailed失败 taskId={}: {}", finalTaskId, e.getMessage());
                        }
                    }
                    try {
                        var data = new LinkedHashMap<String, Object>();
                        data.put("scriptId", scriptId != null ? scriptId : 0);
                        data.put("slotLabel", slotLabel != null ? slotLabel : "");
                        data.put("errorMsg", errorMsg != null ? errorMsg : "生成失败");
                        if (index != null) data.put("index", index);
                        synchronized (writeLock) {
                            writeSseEvent(out, "slot_failed", data);
                        }
                        log.debug("generate-full-sse [sessionId={}] SSE已发送: slot_failed {}", vo.getSessionId(), slotLabel);
                    } catch (Exception e) {
                        log.warn("generate-full-sse slot_failed send failed: {}", e.getMessage());
                    }
                }
            });
            aiQuotaService.consume(userId, full.getConsumption());
            aiCallLogService.log(new AiCallLogService.LogEntry(userId, "live_script_full", null, "llm",
                    null, null, null, null, System.currentTimeMillis() - start, 1, null, false, null, null));
            synchronized (writeLock) {
                writeSseEvent(out, "done", Map.of("status", "ok", "count", full.getResults().size()));
            }
            if (finalTaskId != null) {
                try { generationTaskService.completeTask(finalTaskId); } catch (Exception e) {
                    log.warn("completeTask失败 taskId={}: {}", finalTaskId, e.getMessage());
                }
            }
            log.info("generate-full-sse [sessionId={}] SSE已发送: done count={} 耗时{}ms",
                    vo.getSessionId(), full.getResults().size(), System.currentTimeMillis() - start);
        } catch (Throwable e) {
            log.warn("generate-full-sse 异常: sessionId={} {}", vo.getSessionId(), e.getMessage(), e);
            if (finalTaskId != null) {
                try { generationTaskService.failTask(finalTaskId, e.getMessage()); } catch (Exception ex) {
                    log.warn("failTask失败 taskId={}: {}", finalTaskId, ex.getMessage());
                }
            }
            try {
                synchronized (writeLock) {
                    String errMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                    writeSseEvent(out, "error", Map.of("error", errMsg));
                }
            } catch (Exception ex) { log.debug("SSE error事件发送失败: {}", ex.getMessage()); }
        } finally {
            if (sessionId != null) {
                fullGenerationProgressTracker.unmarkSession(sessionId);
            }
            heartbeatStopped.set(true);
            heartbeat.cancel(false);
            try { out.flush(); } catch (Exception e) { log.debug("SSE flush失败: {}", e.getMessage()); }
        }
    }

    // ==================== 流水线生成（分组并行优化版）====================

    @PostMapping(value = "/generate-full-pipelined-sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "分组流水线全场生成（SSE）/ Pipelined Full Generation with SSE",
            description = "开场串行→商品并行→过渡/收尾串行→成篇优化，相比串行全场生成提速约 3-5x")
    public void generateFullPipelinedSse(HttpServletRequest request, HttpServletResponse response,
                                          @Valid @RequestBody LiveAiGenerateVO vo) throws java.io.IOException {
        Long userId = requireUserId(request);
        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Connection", "keep-alive");
        response.setBufferSize(1);
        OutputStream out = new java.io.FilterOutputStream(response.getOutputStream()) {
            @Override
            public void flush() throws java.io.IOException {
                super.flush();
                response.flushBuffer();
            }
        };
        aiQuotaService.ensureQuota(userId);
        Long sessionId = vo.getSessionId();
        if (sessionId != null) fullGenerationProgressTracker.markSession(sessionId);

        Object writeLock = new Object();
        AtomicBoolean heartbeatStopped = new AtomicBoolean(false);
        ScheduledFuture<?> heartbeat = HEARTBEAT_SCHEDULER.scheduleAtFixedRate(() -> {
            if (heartbeatStopped.get()) return;
            try {
                synchronized (writeLock) {
                    out.write(": heartbeat\n\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    out.flush();
                }
            } catch (Exception e) { log.debug("心跳发送失败: {}", e.getMessage()); }
        }, 10, 10, TimeUnit.SECONDS);

        try {
            synchronized (writeLock) {
                writeSseEvent(out, "progress", Map.of("current", 0, "total", 1,
                        "percent", 0, "slotType", "连接成功，准备流水线生成"));
            }
            LiveAiFullResultVO full = liveAiService.generateFullPipelined(vo, new LiveAiService.FullGenerateProgressCallback() {
                @Override
                public void onProgress(int current, int total, String slotType) {
                    try {
                        synchronized (writeLock) {
                            writeSseEvent(out, "progress", Map.of("current", current, "total", total,
                                    "percent", total > 0 ? (int) (100.0 * current / total) : 0,
                                    "slotType", slotType != null ? slotType : ""));
                        }
                    } catch (Exception e) {
                        log.warn("generate-full-pipelined-sse progress send failed: {}", e.getMessage());
                    }
                }

                @Override
                public void onSlotDone(Long scriptId, String content, String scriptType, String slotLabel,
                                        Integer sequenceNo, Integer index) {
                    try {
                        var data = new LinkedHashMap<String, Object>();
                        data.put("scriptId", scriptId != null ? scriptId : 0);
                        data.put("content", content != null ? content : "");
                        data.put("scriptType", scriptType != null ? scriptType : "");
                        data.put("slotLabel", slotLabel != null ? slotLabel : "");
                        data.put("sequenceNo", sequenceNo != null ? sequenceNo : index);
                        data.put("index", index);
                        synchronized (writeLock) { writeSseEvent(out, "slot_done", data); }
                    } catch (Exception e) {
                        log.warn("generate-full-pipelined-sse slot_done send failed: {}", e.getMessage());
                    }
                }

                @Override
                public void onSlotFailed(Long scriptId, String slotLabel, String errorMsg, Integer index) {
                    try {
                        var data = new LinkedHashMap<String, Object>();
                        data.put("scriptId", scriptId != null ? scriptId : 0);
                        data.put("slotLabel", slotLabel != null ? slotLabel : "");
                        data.put("errorMsg", errorMsg != null ? errorMsg : "生成失败");
                        if (index != null) data.put("index", index);
                        synchronized (writeLock) {
                            writeSseEvent(out, "slot_failed", data);
                        }
                    } catch (Exception e) {
                        log.warn("generate-full-pipelined-sse slot_failed send failed: {}", e.getMessage());
                    }
                }
            });
            aiQuotaService.consume(userId);
            synchronized (writeLock) { writeSseEvent(out, "done", full); }
        } catch (Exception ex) {
            log.error("generate-full-pipelined-sse 生成失败 sessionId={}: {}", vo.getSessionId(), ex.getMessage());
            try {
                synchronized (writeLock) {
                    writeSseEvent(out, "error", Map.of("message", ex.getMessage() != null ? ex.getMessage() : "生成失败"));
                }
            } catch (Exception e) { log.debug("SSE error事件发送失败: {}", e.getMessage()); }
        } finally {
            heartbeatStopped.set(true);
            heartbeat.cancel(false);
            if (sessionId != null) fullGenerationProgressTracker.unmarkSession(sessionId);
            try { out.flush(); } catch (Exception e) { log.debug("SSE flush失败: {}", e.getMessage()); }
        }
    }

    // ==================== 异步生成 ====================

    @PostMapping("/generate-full-async")
    @Operation(summary = "一键生成完整话术（RabbitMQ 异步）/ Queue full script generation")
    public RESTResult<LiveGenerationTaskVO> generateFullAsync(HttpServletRequest request,
                                                              @Valid @RequestBody LiveAiGenerateVO vo) {
        if (!liveAsyncGenerationEnabled) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "异步一键生成已关闭（app.live.generation.async.enabled）");
        }
        if (rabbitTemplate == null) {
            throw new BusinessException(ErrorCode.SYSTEM_BUSY, "RabbitMQ 未配置，无法投递异步生成任务");
        }
        Long userId = requireUserId(request);
        aiQuotaService.ensureQuota(userId);
        final String payload;
        try {
            payload = objectMapper.writeValueAsString(vo);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "生成参数序列化失败");
        }
        LiveGenerationTask task = generationTaskService.createQueuedFullGenerationTask(userId, payload, vo);
        try {
            rabbitTemplate.convertAndSend("", LiveGenerationAmqpConfig.QUEUE_LIVE_SCRIPT_GENERATION, String.valueOf(task.getId()));
        } catch (Exception e) {
            log.warn("live generate-full-async enqueue failed taskId={}: {}", task.getId(), e.getMessage());
            generationTaskService.failTask(task.getId(), "RabbitMQ 投递失败: " + e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_BUSY, "异步任务投递失败，请稍后重试或使用 SSE 一键生成");
        }
        return withTraceId(RESTResult.getSuccess(LiveGenerationTaskVO.fromEntity(task)));
    }

    @PostMapping("/generate-full-in-progress")
    @Operation(summary = "查询场次是否正在执行一键生成")
    public RESTResult<Map<String, Object>> generateFullInProgress(@RequestBody(required = false) SessionIdVO vo) {
        Long sessionId = vo != null ? vo.getSessionId() : null;
        if (sessionId == null) {
            return withTraceId(RESTResult.getSuccess(Map.of("inProgress", false)));
        }
        boolean inProgress = fullGenerationProgressTracker.isMarked(sessionId);
        if (!inProgress) {
            inProgress = generationTaskService.getActiveTask(sessionId).isPresent();
        }
        return withTraceId(RESTResult.getSuccess(Map.of("inProgress", inProgress)));
    }

    @PostMapping("/generation-task/active")
    @Operation(summary = "查询场次当前活跃的生成任务（含进度详情）")
    public RESTResult<LiveGenerationTaskVO> getActiveGenerationTask(@RequestBody(required = false) SessionIdVO vo) {
        Long sessionId = vo != null ? vo.getSessionId() : null;
        if (sessionId == null) {
            return withTraceId(RESTResult.getSuccess(null));
        }
        LiveGenerationTaskVO taskVO = generationTaskService.getActiveTask(sessionId)
                .map(LiveGenerationTaskVO::fromEntity)
                .orElse(null);
        return withTraceId(RESTResult.getSuccess(taskVO));
    }

    // ==================== 并行生成 ====================

    @PostMapping("/generate-parallel")
    @Operation(summary = "G-1 多商品批量并行生成 / Parallel Generate Multiple Slots")
    @Bulkhead(name = "liveParallelGenerate", fallbackMethod = "generateParallelFallback")
    public RESTResult<java.util.Map<Long, cn.gaifan.douyinOperations.module.live.vo.ParallelGenerateResult>> generateParallel(
            HttpServletRequest request,
            @Valid @RequestBody ParallelGenerateVO vo) {
        Long userId = requireUserId(request);
        int batchSize = vo.getScriptIds().size();
        AiQuotaService.QuotaInfo quota = aiQuotaService.getQuota(userId);
        if (quota.remaining() < batchSize) {
            throw new BusinessException(ErrorCode.AI_QUOTA_EXCEEDED,
                    "当日 AI 额度不足以一次并行生成 " + batchSize + " 个槽位（剩余 " + quota.remaining() + "）");
        }
        long start = System.currentTimeMillis();
        try {
            java.util.Map<Long, cn.gaifan.douyinOperations.module.live.vo.ParallelGenerateResult> results =
                    liveAiService.generateParallel(vo.getScriptIds(), vo.getModelId());
            int successCount = (int) results.values().stream()
                    .filter(cn.gaifan.douyinOperations.module.live.vo.ParallelGenerateResult::isSuccess)
                    .count();
            long duration = System.currentTimeMillis() - start;
            aiQuotaService.consume(userId, successCount);
            aiCallLogService.log(new AiCallLogService.LogEntry(
                    userId, "live_script_parallel", null, "llm", null, null, null, null,
                    duration, successCount, null, false, null, null));
            log.info("批量并行生成完成: userId={}, 总数={}, 成功={}, 耗时={}ms",
                    userId, vo.getScriptIds().size(), successCount, duration);
            return withTraceId(RESTResult.getSuccess(results));
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            aiCallLogService.log(new AiCallLogService.LogEntry(
                    userId, "live_script_parallel", null, "llm", null, null, null, null,
                    duration, 0, e.getMessage(), false, null, null));
            log.error("批量并行生成失败: userId={}", userId, e);
            throw e;
        }
    }

    // ==================== 骨架生成 ====================

    @PostMapping("/generate-skeleton")
    @Operation(summary = "骨架生成 / Generate Script Skeleton")
    public RESTResult<List<SkeletonSlotVO>> generateSkeleton(HttpServletRequest request,
                                                             @Valid @RequestBody SkeletonGenerateVO vo) {
        Long userId = requireUserId(request);
        List<SkeletonSlotVO> data = withAiCall(userId, "live_script_skeleton",
                () -> liveAiService.generateSkeleton(vo.getSessionId(), userId, vo.getModelId()));
        return withTraceId(RESTResult.getSuccess(data));
    }

    @PostMapping(value = "/generate-skeleton-sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "骨架生成（SSE 流式）/ Generate Script Skeleton with SSE")
    public void generateSkeletonSse(HttpServletRequest request, HttpServletResponse response,
                                    @Valid @RequestBody SkeletonGenerateVO vo) throws java.io.IOException {
        Long userId = requireUserId(request);
        Long sessionId = vo.getSessionId();
        Long modelId = vo.getModelId();
        OutputStream out = buildSseOutputStream(response);
        aiQuotaService.ensureQuota(userId);
        long start = System.currentTimeMillis();
        try {
            liveAiService.generateSkeletonStream(sessionId, userId, out, modelId);
            aiQuotaService.consume(userId);
            aiCallLogService.log(new AiCallLogService.LogEntry(userId, "live_script_skeleton", null, "llm",
                    null, null, null, null, System.currentTimeMillis() - start, 1, null, false, null, null));
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg == null || (!msg.contains("Broken pipe") && !msg.contains("Connection reset"))) {
                aiCallLogService.log(new AiCallLogService.LogEntry(userId, "live_script_skeleton", null, "llm",
                        null, null, null, null, System.currentTimeMillis() - start, 0, msg, false, null, null));
                log.warn("generate-skeleton-sse controller异常: {}", msg);
            }
        } finally {
            try { out.flush(); } catch (Exception e) { log.debug("SSE flush失败: {}", e.getMessage()); }
        }
    }

    // ==================== 商品/情绪话术生成 ====================

    @PostMapping("/generate-product-script")
    @Operation(summary = "生成产品 AI 话术（按人设+风格）/ Generate Product AI Script")
    public RESTResult<ProductScriptResultVO> generateProductScript(HttpServletRequest request,
                                                                   @Valid @RequestBody ProductScriptGenerateVO vo) {
        Long userId = requireUserId(request);
        ProductScriptResultVO data = withAiCall(userId, "live_script_product_ai",
                () -> liveAiService.generateProductScript(vo, userId));
        return withTraceId(RESTResult.getSuccess(data));
    }

    @PostMapping("/generate-emotional")
    @Operation(summary = "生成情绪价值话术 / Generate Emotional Value Script")
    public RESTResult<LiveAiResultVO> generateEmotional(HttpServletRequest request,
                                                        @Valid @RequestBody EmotionalScriptGenerateVO vo) {
        Long userId = requireUserId(request);
        LiveAiResultVO data = withAiCall(userId, "live_script_emotional",
                () -> liveAiService.generateEmotionalScript(vo, userId));
        return withTraceId(RESTResult.getSuccess(data));
    }

    // ==================== 排序建议 ====================

    @PostMapping("/sort-suggest")
    @Operation(summary = "AI 商品排序建议（预设规则）/ Sort Suggestion for Products")
    public RESTResult<Map<String, Object>> sortSuggest(HttpServletRequest request,
                                                       @RequestBody Map<String, Object> body) {
        Long userId = requireUserId(request);
        Long sessionId = body.get("sessionId") != null ? ((Number) body.get("sessionId")).longValue() : null;
        if (sessionId == null) return withTraceId(RESTResult.error(ErrorCode.VALIDATION_FAIL, "sessionId 不能为空"));

        List<LiveProduct> products = liveProductRepository.findBySessionIdOrderByPositionAscIdAsc(sessionId);
        if (products.isEmpty()) return withTraceId(RESTResult.error(ErrorCode.DATA_NOT_FOUND, "该场次无商品"));

        Map<String, Integer> typeOrder = Map.of("hot", 1, "control", 2, "profit", 3, "loss", 4, "flat", 5);
        products.sort((a, b) -> {
            String ta = (a.getProductType() != null ? a.getProductType().split(",")[0].trim() : "");
            String tb = (b.getProductType() != null ? b.getProductType().split(",")[0].trim() : "");
            return Integer.compare(typeOrder.getOrDefault(ta, 99), typeOrder.getOrDefault(tb, 99));
        });
        List<Long> sortedIds = products.stream().map(LiveProduct::getId).toList();
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("productIds", sortedIds);
        result.put("reason", "推荐排序：爆品（深度讲解）→ 控单品（憋单）→ 利润品（重点推介）→ 亏品（引流）→ 平价品（日常）");
        return withTraceId(RESTResult.getSuccess(result));
    }

    // ==================== 私有辅助方法 ====================

    Long requireUserId(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        return userId;
    }

    <T> T withAiCall(Long userId, String callType, Supplier<T> supplier) {
        aiQuotaService.ensureQuota(userId);
        long start = System.currentTimeMillis();
        try {
            T result = supplier.get();
            aiQuotaService.consume(userId);
            AiCallLogService.LogEntry entry = new AiCallLogService.LogEntry(userId, callType, null, "llm",
                    null, null, null, null, System.currentTimeMillis() - start, 1, null, false, null, null);
            if (result instanceof LiveAiResultVO r && (r.getReferencedChunkIds() != null
                    || r.getSessionIdForAttribution() != null || r.getScriptIdForAttribution() != null)) {
                entry = new AiCallLogService.LogEntry(userId, callType, null, "llm", null, null, null, null,
                        System.currentTimeMillis() - start, 1, null, false, r.getReferencedChunkIds(), null);
                Long callLogId = aiCallLogService.log(entry);
                if (r.getSessionIdForAttribution() != null)
                    aiCallLogService.linkToPublish(callLogId, null, r.getSessionIdForAttribution(), userId);
                if (r.getScriptIdForAttribution() != null)
                    liveAiService.attachAiCallLogToScript(r.getScriptIdForAttribution(), callLogId);
            } else {
                aiCallLogService.log(entry);
            }
            return result;
        } catch (Exception e) {
            aiCallLogService.log(new AiCallLogService.LogEntry(userId, callType, null, "llm",
                    null, null, null, null, System.currentTimeMillis() - start, 0, e.getMessage(), false, null, null));
            throw e;
        }
    }

    LiveAiFullResultVO withAiCallFull(Long userId, String callType, Supplier<LiveAiFullResultVO> supplier) {
        aiQuotaService.ensureQuota(userId);
        long start = System.currentTimeMillis();
        try {
            LiveAiFullResultVO result = supplier.get();
            aiQuotaService.consume(userId, result.getConsumption());
            String refChunks = result.getReferencedChunkIds();
            AiCallLogService.LogEntry entry = new AiCallLogService.LogEntry(userId, callType, null, "llm",
                    null, null, null, null, System.currentTimeMillis() - start, 1, null, false, refChunks, null);
            Long callLogId = aiCallLogService.log(entry);
            if (result.getSessionIdForAttribution() != null)
                aiCallLogService.linkToPublish(callLogId, null, result.getSessionIdForAttribution(), userId);
            if (result.getScriptIdsForAttribution() != null) {
                for (Long scriptId : result.getScriptIdsForAttribution()) {
                    liveAiService.attachAiCallLogToScript(scriptId, callLogId);
                }
            }
            return result;
        } catch (Exception e) {
            aiCallLogService.log(new AiCallLogService.LogEntry(userId, callType, null, "llm",
                    null, null, null, null, System.currentTimeMillis() - start, 0, e.getMessage(), false, null, null));
            throw e;
        }
    }

    <T> RESTResult<T> withTraceId(RESTResult<T> r) {
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    void writeSseEvent(OutputStream out, String event, Object data) throws java.io.IOException {
        String json = objectMapper.writeValueAsString(data);
        out.write(("event: " + event + "\ndata: " + json + "\n\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        out.flush();
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

    /** Bulkhead 降级：并行生成并发超限时返回友好提示 */
    public RESTResult<java.util.Map<Long, cn.gaifan.douyinOperations.module.live.vo.ParallelGenerateResult>> generateParallelFallback(
            HttpServletRequest request, ParallelGenerateVO vo,
            io.github.resilience4j.bulkhead.BulkheadFullException ex) {
        throw new BusinessException(ErrorCode.SYSTEM_BUSY,
                "并行生成并发已达上限，请稍后重试（最大并发 5）");
    }
}
