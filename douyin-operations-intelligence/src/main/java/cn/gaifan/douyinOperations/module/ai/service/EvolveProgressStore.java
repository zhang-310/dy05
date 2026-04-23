package cn.gaifan.douyinOperations.module.ai.service;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 进化任务进度推送：jobId → SseEmitter 列表，runEvolution 每完成一步推送事件
 */
@Component
public class EvolveProgressStore {

    private static final long SSE_TIMEOUT_MS = 30 * 60 * 1000L; // 30 分钟

    private final Map<String, List<SseEmitter>> emittersByJob = new ConcurrentHashMap<>();

    public SseEmitter register(String jobId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        List<SseEmitter> list = emittersByJob.computeIfAbsent(jobId, k -> new CopyOnWriteArrayList<>());
        list.add(emitter);
        emitter.onCompletion(() -> remove(jobId, emitter));
        emitter.onTimeout(() -> remove(jobId, emitter));
        emitter.onError(e -> remove(jobId, emitter));
        return emitter;
    }

    public void emit(String jobId, String eventName, Object data) {
        List<SseEmitter> list = emittersByJob.get(jobId);
        if (list == null || list.isEmpty()) return;
        List<SseEmitter> dead = new CopyOnWriteArrayList<>();
        for (SseEmitter e : list) {
            try {
                e.send(SseEmitter.event().name(eventName).data(data, MediaType.APPLICATION_JSON));
            } catch (Exception ex) {
                dead.add(e);
            }
        }
        dead.forEach(em -> remove(jobId, em));
    }

    public void complete(String jobId) {
        List<SseEmitter> list = emittersByJob.remove(jobId);
        if (list != null) {
            for (SseEmitter e : list) {
                try {
                    e.complete();
                } catch (Exception ignored) {}
            }
        }
    }

    private void remove(String jobId, SseEmitter emitter) {
        List<SseEmitter> list = emittersByJob.get(jobId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) emittersByJob.remove(jobId);
        }
    }
}
