package cn.gaifan.douyinOperations.module.douyin.config;

import cn.gaifan.douyinOperations.module.douyin.service.DouyinScriptLearningService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * P1-1: 抖音话术学习管线定时任务 — 每日凌晨 4:00 执行。
 */
@Slf4j
@Component
public class DouyinLearningScheduler {

    @Autowired(required = false)
    private DouyinScriptLearningService douyinScriptLearningService;

    /**
     * 每日 04:00 执行话术学习管线
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public void scheduledLearning() {
        if (douyinScriptLearningService == null) {
            log.debug("[DouyinLearningScheduler] 服务不可用，跳过");
            return;
        }
        log.info("[DouyinLearningScheduler] 开始执行每日话术学习任务");
        try {
            douyinScriptLearningService.runLearningPipeline();
        } catch (Exception e) {
            log.error("[DouyinLearningScheduler] 话术学习任务失败", e);
        }
    }
}
