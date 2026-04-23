package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.live.service.LiveRealtimePanelService;
import cn.gaifan.douyinOperations.module.live.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 直播实时辅助面板 Controller
 * 提供话术导航、实时数据管理、SSE 实时推送等功能
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/live/realtime-panel")
@Tag(name = "直播实时辅助面板", description = "提词器、倒计时、实时数据、话术导航")
@Validated
public class LiveRealtimePanelController {

    @Resource
    private LiveRealtimePanelService realtimePanelService;

    /**
     * SSE 推送的订阅者存储
     * key: liveSessionId, value: SseEmitter 列表
     */
    private static final ConcurrentHashMap<Long, ConcurrentHashMap<String, SseEmitter>> subscribers = new ConcurrentHashMap<>();

    /**
     * 初始化直播实时面板
     * 查询直播的所有话术段落和实时数据，返回初始化信息
     *
     * @param request 包含 liveSessionId 的请求
     * @return 面板初始化数据
     */
    @PostMapping("/init")
    @Operation(summary = "初始化面板", description = "查询直播的所有话术段落和实时数据")
    public RESTResult<PanelInitVO> initializePanel(HttpServletRequest request,
                                                   @Valid @RequestBody SlotOperationVO body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        log.info("初始化实时面板请求: sessionId={}", body.getLiveSessionId());

        try {
            PanelInitVO result = realtimePanelService.initializePanel(body.getLiveSessionId(), userId);
            RESTResult<PanelInitVO> resp = RESTResult.getSuccess(result);
            resp.setTraceId(MDC.get("traceId"));
            return resp;
        } catch (BusinessException e) {
            log.warn("初始化面板失败: {}", e.getMessage());
            return RESTResult.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("初始化面板异常", e);
            return RESTResult.error(ErrorCode.SYSTEM_ERROR, "初始化面板失败");
        }
    }

    /**
     * SSE 实时推送流端点
     * 建立与客户端的服务端推送连接，推送实时数据和话术变化
     *
     * @param sessionId 直播场次 ID
     * @return SSE 推送流
     */
    @GetMapping(value = "/stream/{sessionId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "SSE 实时推送", description = "建立服务端推送连接，推送实时数据和话术变化")
    public SseEmitter streamRealtimeData(HttpServletRequest request, @PathVariable Long sessionId) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            userId = -1L; // 匿名用户
        }
        log.info("SSE 推送连接: sessionId={}, userId={}", sessionId, userId);

        // 创建 SseEmitter，5 分钟超时
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);

        // 注册订阅者
        String subscriberId = userId + "-" + System.nanoTime();
        subscribers.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>()).put(subscriberId, emitter);

        // 处理超时和完成事件
        emitter.onTimeout(() -> {
            log.info("SSE 连接超时: sessionId={}, subscriberId={}", sessionId, subscriberId);
            subscribers.getOrDefault(sessionId, new ConcurrentHashMap<>()).remove(subscriberId);
        });

        emitter.onCompletion(() -> {
            log.info("SSE 连接完成: sessionId={}, subscriberId={}", sessionId, subscriberId);
            subscribers.getOrDefault(sessionId, new ConcurrentHashMap<>()).remove(subscriberId);
        });

        // 发送初始连接成功消息
        try {
            emitter.send(SseEmitter.event()
                    .id(String.valueOf(System.currentTimeMillis()))
                    .name("connected")
                    .data("SSE 连接成功")
                    .build());
        } catch (IOException e) {
            log.error("SSE 初始消息发送失败: sessionId={}", sessionId, e);
            subscribers.getOrDefault(sessionId, new ConcurrentHashMap<>()).remove(subscriberId);
        }

        return emitter;
    }

    /**
     * 跳转到下一话术段落
     *
     * @param request 包含 liveSessionId 的请求
     * @return 下一话术段落的详细信息
     */
    @PostMapping("/next-slot")
    @Operation(summary = "下一话术", description = "跳转到下一个话术段落")
    public RESTResult<LiveSessionScriptSlotVO> nextSlot(HttpServletRequest request,
                                                        @Valid @RequestBody SlotOperationVO body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        log.info("执行下一话术: sessionId={}", body.getLiveSessionId());

        try {
            LiveSessionScriptSlotVO result = realtimePanelService.nextSlot(body.getLiveSessionId(), userId);

            // 推送话术变化事件到所有订阅者
            broadcastSlotChange(body.getLiveSessionId(), result);

            RESTResult<LiveSessionScriptSlotVO> resp = RESTResult.getSuccess(result);
            resp.setTraceId(MDC.get("traceId"));
            return resp;
        } catch (BusinessException e) {
            log.warn("下一话术失败: {}", e.getMessage());
            return RESTResult.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("下一话术异常", e);
            return RESTResult.error(ErrorCode.SYSTEM_ERROR, "操作失败");
        }
    }

    /**
     * 返回到上一话术段落
     *
     * @param request 包含 liveSessionId 的请求
     * @return 上一话术段落的详细信息
     */
    @PostMapping("/prev-slot")
    @Operation(summary = "上一话术", description = "返回到上一个话术段落")
    public RESTResult<LiveSessionScriptSlotVO> prevSlot(HttpServletRequest request,
                                                        @Valid @RequestBody SlotOperationVO body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        log.info("执行上一话术: sessionId={}", body.getLiveSessionId());

        try {
            LiveSessionScriptSlotVO result = realtimePanelService.prevSlot(body.getLiveSessionId(), userId);

            // 推送话术变化事件到所有订阅者
            broadcastSlotChange(body.getLiveSessionId(), result);

            RESTResult<LiveSessionScriptSlotVO> resp = RESTResult.getSuccess(result);
            resp.setTraceId(MDC.get("traceId"));
            return resp;
        } catch (BusinessException e) {
            log.warn("上一话术失败: {}", e.getMessage());
            return RESTResult.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("上一话术异常", e);
            return RESTResult.error(ErrorCode.SYSTEM_ERROR, "操作失败");
        }
    }

    /**
     * 跳转到指定序号的话术段落
     *
     * @param request 包含 liveSessionId 和 slotIndex 的请求
     * @return 目标话术段落的详细信息
     */
    @PostMapping("/jump-slot")
    @Operation(summary = "跳转话术", description = "跳转到指定序号的话术段落")
    public RESTResult<LiveSessionScriptSlotVO> jumpSlot(HttpServletRequest request,
                                                        @Valid @RequestBody SlotOperationVO body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        log.info("执行跳转话术: sessionId={}, slotIndex={}", body.getLiveSessionId(), body.getSlotIndex());

        try {
            if (body.getSlotIndex() == null) {
                return RESTResult.error(ErrorCode.VALIDATION_FAIL, "段落序号不能为空");
            }

            LiveSessionScriptSlotVO result = realtimePanelService.jumpSlot(
                    body.getLiveSessionId(),
                    body.getSlotIndex(),
                    userId
            );

            // 推送话术变化事件到所有订阅者
            broadcastSlotChange(body.getLiveSessionId(), result);

            RESTResult<LiveSessionScriptSlotVO> resp = RESTResult.getSuccess(result);
            resp.setTraceId(MDC.get("traceId"));
            return resp;
        } catch (BusinessException e) {
            log.warn("跳转话术失败: {}", e.getMessage());
            return RESTResult.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("跳转话术异常", e);
            return RESTResult.error(ErrorCode.SYSTEM_ERROR, "操作失败");
        }
    }

    /**
     * 标记话术段落为已完成
     *
     * @param request 包含 liveSessionId 和 slotIndex 的请求
     * @return 已完成的话术段落详细信息
     */
    @PostMapping("/complete-slot")
    @Operation(summary = "标记完成", description = "标记话术段落为已完成")
    public RESTResult<LiveSessionScriptSlotVO> completeSlot(HttpServletRequest request,
                                                            @Valid @RequestBody SlotOperationVO body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        log.info("标记话术完成: sessionId={}, slotIndex={}", body.getLiveSessionId(), body.getSlotIndex());

        try {
            if (body.getSlotIndex() == null) {
                return RESTResult.error(ErrorCode.VALIDATION_FAIL, "段落序号不能为空");
            }

            LiveSessionScriptSlotVO result = realtimePanelService.completeSlot(
                    body.getLiveSessionId(),
                    body.getSlotIndex(),
                    userId
            );

            // 推送完成事件到所有订阅者
            broadcastSlotCompleted(body.getLiveSessionId(), body.getSlotIndex());

            RESTResult<LiveSessionScriptSlotVO> resp = RESTResult.getSuccess(result);
            resp.setTraceId(MDC.get("traceId"));
            return resp;
        } catch (BusinessException e) {
            log.warn("标记完成失败: {}", e.getMessage());
            return RESTResult.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("标记完成异常", e);
            return RESTResult.error(ErrorCode.SYSTEM_ERROR, "操作失败");
        }
    }

    /**
     * 更新实时数据
     * 保存或更新直播场次的实时统计数据
     *
     * @param request 实时数据保存参数
     * @return 更新后的实时数据
     */
    @PostMapping("/update-data")
    @Operation(summary = "更新实时数据", description = "保存或更新实时数据（观众数、点赞、评论等）")
    public RESTResult<LiveSessionRealtimeDataVO> updateRealtimeData(HttpServletRequest request,
                                                                    @Valid @RequestBody RealtimeDataSaveVO body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        log.info("更新实时数据: sessionId={}", body.getLiveSessionId());

        try {
            LiveSessionRealtimeDataVO result = realtimePanelService.updateRealtimeData(body, userId);

            // 推送数据更新事件到所有订阅者
            broadcastDataUpdate(body.getLiveSessionId(), result);

            RESTResult<LiveSessionRealtimeDataVO> resp = RESTResult.getSuccess(result);
            resp.setTraceId(MDC.get("traceId"));
            return resp;
        } catch (BusinessException e) {
            log.warn("更新实时数据失败: {}", e.getMessage());
            return RESTResult.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("更新实时数据异常", e);
            return RESTResult.error(ErrorCode.SYSTEM_ERROR, "更新失败");
        }
    }

    /**
     * 内部方法：广播话术变化事件
     * 推送给所有订阅该直播的客户端
     *
     * @param sessionId 直播场次 ID
     * @param slot      新的话术段落信息
     */
    private void broadcastSlotChange(Long sessionId, LiveSessionScriptSlotVO slot) {
        ConcurrentHashMap<String, SseEmitter> emitters = subscribers.get(sessionId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        String eventData = String.format(
                "{\"currentSlotIndex\":%d,\"content\":\"%s\",\"durationSeconds\":%d,\"startedAt\":\"%s\"}",
                slot.getSlotIndex(),
                escapeJson(slot.getContent()),
                slot.getDurationSeconds(),
                slot.getStartedAt()
        );

        emitters.forEach((subscriberId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .id(String.valueOf(System.currentTimeMillis()))
                        .name("slot-change")
                        .data(eventData)
                        .build());
            } catch (IOException e) {
                log.warn("SSE 推送话术变化失败: sessionId={}, subscriberId={}", sessionId, subscriberId);
                emitters.remove(subscriberId);
            }
        });
    }

    /**
     * 内部方法：广播话术完成事件
     *
     * @param sessionId 直播场次 ID
     * @param slotIndex 完成的话术段落序号
     */
    private void broadcastSlotCompleted(Long sessionId, Integer slotIndex) {
        ConcurrentHashMap<String, SseEmitter> emitters = subscribers.get(sessionId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        String eventData = String.format("{\"completedSlotIndex\":%d,\"completedAt\":\"%s\"}", slotIndex, System.currentTimeMillis());

        emitters.forEach((subscriberId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .id(String.valueOf(System.currentTimeMillis()))
                        .name("slot-completed")
                        .data(eventData)
                        .build());
            } catch (IOException e) {
                log.warn("SSE 推送完成事件失败: sessionId={}, subscriberId={}", sessionId, subscriberId);
                emitters.remove(subscriberId);
            }
        });
    }

    /**
     * 内部方法：广播实时数据更新事件
     *
     * @param sessionId   直播场次 ID
     * @param realtimeData 更新后的实时数据
     */
    private void broadcastDataUpdate(Long sessionId, LiveSessionRealtimeDataVO realtimeData) {
        ConcurrentHashMap<String, SseEmitter> emitters = subscribers.get(sessionId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        String eventData = String.format(
                "{\"type\":\"realtime-data\",\"likeCount\":%d,\"commentCount\":%d,\"viewerCount\":%d,\"watchedCount\":%d,\"timestamp\":\"%s\"}",
                realtimeData.getLikeCount(),
                realtimeData.getCommentCount(),
                realtimeData.getViewerCount(),
                realtimeData.getWatchedCount(),
                System.currentTimeMillis()
        );

        emitters.forEach((subscriberId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .id(String.valueOf(System.currentTimeMillis()))
                        .name("data-update")
                        .data(eventData)
                        .build());
            } catch (IOException e) {
                log.warn("SSE 推送数据更新失败: sessionId={}, subscriberId={}", sessionId, subscriberId);
                emitters.remove(subscriberId);
            }
        });
    }

    /**
     * 内部方法：转义 JSON 字符串中的特殊字符
     *
     * @param input 输入字符串
     * @return 转义后的字符串
     */
    private String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
