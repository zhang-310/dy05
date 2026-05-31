package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 账号采集异步入口（独立 Bean 确保 @Async 代理生效）。
 * 使用独立线程池 accountCollectExecutor，避免与 mediaTaskExecutor 竞争导致饥饿。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountCollectAsyncRunner {

    private final AccountVideoCollectServiceImpl collectService;

    @Async("accountCollectExecutor")
    public void runCollectAsync(Long taskId, Long userId) {
        collectService.runCollectPipeline(taskId, userId);
    }

    @Async("accountCollectExecutor")
    public void runCollectAsync(Long taskId, Long userId, Runnable onDone) {
        try {
            collectService.runCollectPipeline(taskId, userId);
        } finally {
            if (onDone != null) {
                onDone.run();
            }
        }
    }

    @Async("accountCollectExecutor")
    public void runAnalyzeAsync(Long taskId, List<Long> viralVideoIds, Long userId) {
        collectService.runAnalyzePipeline(taskId, viralVideoIds, userId);
    }
}
