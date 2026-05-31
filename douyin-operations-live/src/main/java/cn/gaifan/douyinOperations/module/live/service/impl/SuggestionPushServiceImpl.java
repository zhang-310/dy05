package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.module.live.service.DanmakuSentimentService;
import cn.gaifan.douyinOperations.module.live.service.LiveRealtimePanelService;
import cn.gaifan.douyinOperations.module.live.service.SuggestionPushService;
import cn.gaifan.douyinOperations.module.live.vo.DanmakuSentimentSnapshotVO;
import cn.gaifan.douyinOperations.module.live.vo.RealtimeSuggestionVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实时建议 SSE 推送实现（P0-11）
 * 每 30 秒评估一次实时指标并推送（建议列表 + 弹幕情绪快照，便于面板刷新）
 */
@Service
public class SuggestionPushServiceImpl implements SuggestionPushService {

    private static final Logger log = LoggerFactory.getLogger(SuggestionPushServiceImpl.class);

    @Resource
    private LiveRealtimePanelService realtimePanelService;

    @Resource
    private DanmakuSentimentService danmakuSentimentService;

    @Value("${app.live.realtime.suggestion.push-enabled:true}")
    private boolean pushEnabled;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** sessionId -> (emitterId -> Subscriber) */
    private final ConcurrentHashMap<Long, ConcurrentHashMap<String, Subscriber>> registry = new ConcurrentHashMap<>();

    private static class Subscriber {
        final SseEmitter emitter;
        final Long userId;

        Subscriber(SseEmitter emitter, Long userId) {
            this.emitter = emitter;
            this.userId = userId;
        }
    }

    @Override
    public void register(Long sessionId, Long userId, SseEmitter emitter) {
        String id = userId + "-" + System.nanoTime();
        registry.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>()).put(id, new Subscriber(emitter, userId));
        emitter.onCompletion(() -> unregister(sessionId, emitter));
        emitter.onTimeout(() -> unregister(sessionId, emitter));
        emitter.onError(error -> unregister(sessionId, emitter));
        log.debug("实时建议 SSE 注册: sessionId={}, userId={}", sessionId, userId);
    }

    @Override
    public void unregister(Long sessionId, SseEmitter emitter) {
        ConcurrentHashMap<String, Subscriber> map = registry.get(sessionId);
        if (map != null) {
            map.entrySet().removeIf(e -> e.getValue().emitter == emitter);
            if (map.isEmpty()) registry.remove(sessionId);
        }
    }

    @Scheduled(fixedDelayString = "${app.live.realtime.suggestion.push-fixed-delay-ms:30000}")
    public void evaluateAndPush() {
        if (!pushEnabled) {
            log.debug("实时建议 SSE 推送已禁用，跳过");
            return;
        }
        registry.forEach((sessionId, map) -> {
            map.forEach((id, sub) -> {
                try {
                    List<RealtimeSuggestionVO> suggestions = realtimePanelService.getSuggestionsForSession(sessionId, sub.userId);
                    DanmakuSentimentSnapshotVO sentiment = danmakuSentimentService.getSnapshot(sessionId);
                    Map<String, Object> payload = new LinkedHashMap<>();
                    payload.put("type", "suggestion");
                    payload.put("suggestions", suggestions);
                    payload.put("danmakuSentiment", sentiment);
                    payload.put("timestamp", System.currentTimeMillis());
                    String json = objectMapper.writeValueAsString(payload);
                    sub.emitter.send(SseEmitter.event()
                            .id(String.valueOf(System.currentTimeMillis()))
                            .name("suggestion")
                            .data(json)
                            .build());
                    log.debug("实时建议已推送: sessionId={}, count={}, sentiment={}",
                            sessionId, suggestions.size(), sentiment.getDominant());
                } catch (IOException e) {
                    log.warn("推送建议失败 sessionId={}: {}", sessionId, e.getMessage());
                    unregister(sessionId, sub.emitter);
                } catch (Exception e) {
                    log.warn("评估建议异常 sessionId={}: {}", sessionId, e.getMessage());
                }
            });
        });
    }
}
