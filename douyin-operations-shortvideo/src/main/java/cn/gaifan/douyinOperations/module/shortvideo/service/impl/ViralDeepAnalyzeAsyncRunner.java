package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 爆款深度分析异步入口（使用 mediaTaskExecutor，与视频/媒体 IO 一致）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ViralDeepAnalyzeAsyncRunner {

    private final ViralDeepAnalyzeExecutor executor;

    @Async("mediaTaskExecutor")
    public void runDeepAnalyzeAsync(Long viralVideoId, Long userId) {
        executor.executeDeepAnalyze(viralVideoId, userId);
    }

    @Async("mediaTaskExecutor")
    public void runDeepAnalyzeAsync(Long viralVideoId, Long userId, SseEmitter sseEmitter) {
        executor.executeDeepAnalyze(viralVideoId, userId, sseEmitter);
    }
}
