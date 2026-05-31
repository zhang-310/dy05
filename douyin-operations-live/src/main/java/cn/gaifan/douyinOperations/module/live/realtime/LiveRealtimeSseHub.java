package cn.gaifan.douyinOperations.module.live.realtime;

import cn.gaifan.douyinOperations.module.live.repository.LiveProductRepository;
import cn.gaifan.douyinOperations.module.live.vo.LiveSessionRealtimeDataVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveSessionScriptSlotVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 直播实时面板主 SSE（/stream/{sessionId}）订阅与广播，供 Controller 与抖音采集等后台任务共用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LiveRealtimeSseHub {

    private final LiveProductRepository liveProductRepository;

    private final ConcurrentHashMap<Long, ConcurrentHashMap<String, SseEmitter>> subscribers = new ConcurrentHashMap<>();

    public ConcurrentHashMap<String, SseEmitter> subscribersForSession(Long sessionId) {
        return subscribers.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>());
    }

    public void register(Long sessionId, String subscriberId, SseEmitter emitter) {
        subscribers.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>()).put(subscriberId, emitter);
        emitter.onCompletion(() -> unregister(sessionId, subscriberId));
        emitter.onTimeout(() -> unregister(sessionId, subscriberId));
        emitter.onError(error -> unregister(sessionId, subscriberId));
    }

    public void unregister(Long sessionId, String subscriberId) {
        ConcurrentHashMap<String, SseEmitter> m = subscribers.get(sessionId);
        if (m != null) {
            m.remove(subscriberId);
            if (m.isEmpty()) {
                subscribers.remove(sessionId, m);
            }
        }
    }

    public void broadcastDataUpdate(Long sessionId, LiveSessionRealtimeDataVO realtimeData) {
        if (realtimeData == null) {
            return;
        }
        ConcurrentHashMap<String, SseEmitter> emitters = subscribers.get(sessionId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        BigDecimal cumulativeGmv = BigDecimal.ZERO;
        try {
            BigDecimal sum = liveProductRepository.sumRevenueBySessionId(sessionId);
            cumulativeGmv = sum != null ? sum : BigDecimal.ZERO;
        } catch (Exception e) {
            log.debug("sumRevenueBySessionId skip: {}", e.getMessage());
        }
        BigDecimal purchaseAmt = realtimeData.getProductPurchaseAmount() != null
                ? realtimeData.getProductPurchaseAmount() : BigDecimal.ZERO;

        String eventData = String.format(Locale.US,
                "{\"type\":\"realtime-data\",\"likeCount\":%d,\"commentCount\":%d,\"viewerCount\":%d,\"watchedCount\":%d,"
                        + "\"cumulativeGmv\":%s,\"productPurchaseAmount\":%s,\"timestamp\":%d}",
                realtimeData.getLikeCount() != null ? realtimeData.getLikeCount() : 0,
                realtimeData.getCommentCount() != null ? realtimeData.getCommentCount() : 0,
                realtimeData.getViewerCount() != null ? realtimeData.getViewerCount() : 0,
                realtimeData.getWatchedCount() != null ? realtimeData.getWatchedCount() : 0,
                cumulativeGmv.stripTrailingZeros().toPlainString(),
                purchaseAmt.stripTrailingZeros().toPlainString(),
                System.currentTimeMillis());

        emitters.forEach((subscriberId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .id(String.valueOf(System.currentTimeMillis()))
                        .name("data-update")
                        .data(eventData)
                        .build());
            } catch (IOException e) {
                log.warn("SSE 推送数据更新失败: sessionId={}, subscriberId={}", sessionId, subscriberId);
                unregister(sessionId, subscriberId);
            }
        });
    }

    public void broadcastSlotChange(Long sessionId, LiveSessionScriptSlotVO slot) {
        if (slot == null) {
            return;
        }
        ConcurrentHashMap<String, SseEmitter> emitters = subscribers.get(sessionId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        String eventData = String.format(
                "{\"currentSlotIndex\":%d,\"content\":\"%s\",\"durationSeconds\":%d,\"startedAt\":\"%s\"}",
                slot.getSlotIndex(),
                escapeJson(slot.getContent()),
                slot.getDurationSeconds() != null ? slot.getDurationSeconds() : 0,
                slot.getStartedAt());

        emitters.forEach((subscriberId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .id(String.valueOf(System.currentTimeMillis()))
                        .name("slot-change")
                        .data(eventData)
                        .build());
            } catch (IOException e) {
                log.warn("SSE 推送话术变化失败: sessionId={}, subscriberId={}", sessionId, subscriberId);
                unregister(sessionId, subscriberId);
            }
        });
    }

    public void broadcastSlotCompleted(Long sessionId, Integer slotIndex) {
        if (slotIndex == null) {
            return;
        }
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
                unregister(sessionId, subscriberId);
            }
        });
    }

    /**
     * 广播互动话术提示（inject_interaction 自动建议执行后推送给前端浮窗）。
     */
    public void broadcastInteractionHint(Long sessionId, String hint, String reason) {
        if (sessionId == null) {
            return;
        }
        ConcurrentHashMap<String, SseEmitter> emitters = subscribers.get(sessionId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        String eventData = String.format(
                "{\"hint\":\"%s\",\"reason\":\"%s\",\"timestamp\":%d}",
                escapeJson(hint != null ? hint : ""),
                escapeJson(reason != null ? reason : ""),
                System.currentTimeMillis());

        emitters.forEach((subscriberId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .id(String.valueOf(System.currentTimeMillis()))
                        .name("interaction-hint")
                        .data(eventData)
                        .build());
            } catch (IOException e) {
                log.warn("SSE 推送互动提示失败: sessionId={}, subscriberId={}", sessionId, subscriberId);
                unregister(sessionId, subscriberId);
            }
        });
    }

    private static String escapeJson(String input) {
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
