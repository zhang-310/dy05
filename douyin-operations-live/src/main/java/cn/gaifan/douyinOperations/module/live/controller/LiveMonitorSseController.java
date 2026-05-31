package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.live.entity.LiveMonitor;
import cn.gaifan.douyinOperations.module.live.repository.LiveMonitorRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 直播实时监控 SSE 推送
 * 客户端通过 EventSource 连接，服务端每 5 秒推送最新监控数据
 */
@RestController
@RequestMapping("/api/v1/live/monitor")
@Tag(name = "直播实时监控 / Live Realtime Monitor", description = "SSE 实时推送直播监控数据")
public class LiveMonitorSseController {

    private static final Logger log = LoggerFactory.getLogger(LiveMonitorSseController.class);
    private static final long SSE_TIMEOUT = 30 * 60 * 1000L; // 30 分钟
    private static final long PUSH_INTERVAL_MS = 5000L;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<Long, List<SseEmitter>> sessionEmitters = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    @Resource
    private LiveMonitorRepository monitorRepository;
    @Resource
    private LiveSessionRepository sessionRepository;

    @GetMapping(value = "/stream/{sessionId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "SSE 实时监控流 / Realtime Monitor Stream")
    public SseEmitter stream(@PathVariable Long sessionId, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            SseEmitter emitter = new SseEmitter(0L);
            try {
                emitter.send(SseEmitter.event().name("error").data("{\"message\":\"未登录\"}"));
                emitter.complete();
            } catch (IOException ignored) {
                // SSE发送失败，客户端已断开
            }
            return emitter;
        }

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        List<SseEmitter> emitters = sessionEmitters.computeIfAbsent(sessionId, k -> new CopyOnWriteArrayList<>());
        emitters.add(emitter);

        emitter.onCompletion(() -> removeEmitter(sessionId, emitter));
        emitter.onTimeout(() -> removeEmitter(sessionId, emitter));
        emitter.onError(e -> removeEmitter(sessionId, emitter));

        // 立即推送一次当前数据
        pushLatestData(sessionId, emitter);

        // 启动定时推送（如果该 session 还没有定时任务）
        startPeriodicPush(sessionId);

        log.info("SSE 连接建立: sessionId={}, userId={}, 当前连接数={}", sessionId, userId, emitters.size());
        return emitter;
    }

    @PostMapping("/snapshot")
    @Operation(summary = "获取最新监控快照 / Get Latest Monitor Snapshot")
    public Map<String, Object> snapshot(HttpServletRequest request,
            @RequestBody(required = false) java.util.Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        Long sessionId = body != null && body.get("sessionId") != null ? ((Number) body.get("sessionId")).longValue() : null;
        if (sessionId == null) throw new BusinessException(ErrorCode.INVALID_PARAMS, "缺少 sessionId");
        List<LiveMonitor> monitors = monitorRepository.findBySessionId(sessionId);
        if (monitors.isEmpty()) {
            return Map.of("sessionId", sessionId, "viewers", 0, "likes", 0L,
                    "comments", 0, "shares", 0, "productImpressions", 0, "dataPoints", 0);
        }
        LiveMonitor latest = monitors.get(monitors.size() - 1);
        // 计算峰值
        int peakViewers = monitors.stream().mapToInt(m -> m.getViewers() != null ? m.getViewers() : 0).max().orElse(0);
        long totalLikes = monitors.stream().mapToLong(m -> m.getLikes() != null ? m.getLikes() : 0L).max().orElse(0L);
        int totalComments = monitors.stream().mapToInt(m -> m.getComments() != null ? m.getComments() : 0).sum();
        int totalShares = monitors.stream().mapToInt(m -> m.getShares() != null ? m.getShares() : 0).sum();

        return Map.ofEntries(
                Map.entry("sessionId", sessionId),
                Map.entry("viewers", latest.getViewers() != null ? latest.getViewers() : 0),
                Map.entry("peakViewers", peakViewers),
                Map.entry("likes", latest.getLikes() != null ? latest.getLikes() : 0L),
                Map.entry("totalLikes", totalLikes),
                Map.entry("comments", latest.getComments() != null ? latest.getComments() : 0),
                Map.entry("totalComments", totalComments),
                Map.entry("shares", latest.getShares() != null ? latest.getShares() : 0),
                Map.entry("totalShares", totalShares),
                Map.entry("productImpressions", latest.getProductImpressions() != null ? latest.getProductImpressions() : 0),
                Map.entry("dataPoints", monitors.size()),
                Map.entry("timestamp", latest.getTimestamp())
        );
    }

    /**
     * 外部系统/采集器调用此接口推送数据，同时广播给所有 SSE 客户端
     */
    @PostMapping("/push")
    @Operation(summary = "推送监控数据 / Push Monitor Data")
    public Map<String, Object> pushData(@RequestBody Map<String, Object> data) {
        Long sessionId = data.get("sessionId") != null ? ((Number) data.get("sessionId")).longValue() : null;
        if (sessionId == null) {
            return Map.of("success", false, "message", "sessionId 不能为空");
        }

        LiveMonitor monitor = new LiveMonitor();
        monitor.setSessionId(sessionId);
        monitor.setTimestamp(new Timestamp(System.currentTimeMillis()));
        monitor.setViewers(data.get("viewers") != null ? ((Number) data.get("viewers")).intValue() : 0);
        monitor.setLikes(data.get("likes") != null ? ((Number) data.get("likes")).longValue() : 0L);
        monitor.setComments(data.get("comments") != null ? ((Number) data.get("comments")).intValue() : 0);
        monitor.setShares(data.get("shares") != null ? ((Number) data.get("shares")).intValue() : 0);
        monitor.setProductImpressions(data.get("productImpressions") != null ? ((Number) data.get("productImpressions")).intValue() : 0);
        monitorRepository.save(monitor);

        // 广播给所有订阅该 session 的 SSE 客户端
        broadcastToSession(sessionId, monitor);

        return Map.of("success", true, "id", monitor.getId());
    }

    // ─── 内部方法 ──────────────────────────────────────

    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    private void startPeriodicPush(Long sessionId) {
        scheduledTasks.computeIfAbsent(sessionId, id ->
                scheduler.scheduleAtFixedRate(() -> {
                    List<SseEmitter> emitters = sessionEmitters.get(id);
                    if (emitters == null || emitters.isEmpty()) {
                        ScheduledFuture<?> task = scheduledTasks.remove(id);
                        if (task != null) task.cancel(false);
                        sessionEmitters.remove(id);
                        return;
                    }
                    List<LiveMonitor> monitors = monitorRepository.findBySessionId(id);
                    if (!monitors.isEmpty()) {
                        LiveMonitor latest = monitors.get(monitors.size() - 1);
                        broadcastToSession(id, latest);
                    }
                }, PUSH_INTERVAL_MS, PUSH_INTERVAL_MS, TimeUnit.MILLISECONDS)
        );
    }

    private void broadcastToSession(Long sessionId, LiveMonitor monitor) {
        List<SseEmitter> emitters = sessionEmitters.get(sessionId);
        if (emitters == null || emitters.isEmpty()) return;

        try {
            String json = objectMapper.writeValueAsString(Map.of(
                    "sessionId", monitor.getSessionId(),
                    "viewers", monitor.getViewers() != null ? monitor.getViewers() : 0,
                    "likes", monitor.getLikes() != null ? monitor.getLikes() : 0L,
                    "comments", monitor.getComments() != null ? monitor.getComments() : 0,
                    "shares", monitor.getShares() != null ? monitor.getShares() : 0,
                    "productImpressions", monitor.getProductImpressions() != null ? monitor.getProductImpressions() : 0,
                    "timestamp", monitor.getTimestamp() != null ? monitor.getTimestamp().getTime() : System.currentTimeMillis()
            ));

            List<SseEmitter> dead = new CopyOnWriteArrayList<>();
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().name("monitor").data(json));
                } catch (Exception e) {
                    dead.add(emitter);
                }
            }
            emitters.removeAll(dead);
        } catch (Exception e) {
            log.error("广播监控数据失败: sessionId={}", sessionId, e);
        }
    }

    private void pushLatestData(Long sessionId, SseEmitter emitter) {
        try {
            List<LiveMonitor> monitors = monitorRepository.findBySessionId(sessionId);
            if (!monitors.isEmpty()) {
                LiveMonitor latest = monitors.get(monitors.size() - 1);
                String json = objectMapper.writeValueAsString(Map.of(
                        "sessionId", latest.getSessionId(),
                        "viewers", latest.getViewers() != null ? latest.getViewers() : 0,
                        "likes", latest.getLikes() != null ? latest.getLikes() : 0L,
                        "comments", latest.getComments() != null ? latest.getComments() : 0,
                        "shares", latest.getShares() != null ? latest.getShares() : 0,
                        "productImpressions", latest.getProductImpressions() != null ? latest.getProductImpressions() : 0,
                        "timestamp", latest.getTimestamp() != null ? latest.getTimestamp().getTime() : System.currentTimeMillis()
                ));
                emitter.send(SseEmitter.event().name("monitor").data(json));
            }
        } catch (Exception e) {
            log.warn("推送初始数据失败: sessionId={}", sessionId, e);
        }
    }

    private void removeEmitter(Long sessionId, SseEmitter emitter) {
        List<SseEmitter> emitters = sessionEmitters.get(sessionId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                sessionEmitters.remove(sessionId);
                ScheduledFuture<?> task = scheduledTasks.remove(sessionId);
                if (task != null) task.cancel(false);
            }
        }
    }
}
