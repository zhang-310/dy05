package cn.gaifan.douyinOperations.module.ai.config;

import cn.gaifan.douyinOperations.module.ai.event.EvolveTaskCompletedEvent;
import cn.gaifan.douyinOperations.module.ai.service.EvolveEngineService;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 事务提交后再异步解锁 blocked 任务，避免读不到已提交的 completed 状态。
 */
@Component
public class EvolveTaskCompletionEventListener {

    @Resource
    private EvolveEngineService evolveEngineService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("evolveTaskExecutor")
    public void onCompleted(EvolveTaskCompletedEvent event) {
        if (event == null || event.kbId() == null || event.completedTaskNo() == null) {
            return;
        }
        evolveEngineService.tryResumeBlockedTasksAfterCompletion(event.kbId(), event.completedTaskNo());
    }
}
