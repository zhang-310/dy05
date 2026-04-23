package cn.gaifan.douyinOperations.module.live.service;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 一键生成进行中场次集合：内存侧快速探测，与 {@code live_generation_task} 互补。
 */
@Component
public class LiveFullGenerationProgressTracker {

    private final Set<Long> sessionIds = ConcurrentHashMap.newKeySet();

    public void markSession(Long sessionId) {
        if (sessionId != null) {
            sessionIds.add(sessionId);
        }
    }

    public void unmarkSession(Long sessionId) {
        if (sessionId != null) {
            sessionIds.remove(sessionId);
        }
    }

    public boolean isMarked(Long sessionId) {
        return sessionId != null && sessionIds.contains(sessionId);
    }
}
