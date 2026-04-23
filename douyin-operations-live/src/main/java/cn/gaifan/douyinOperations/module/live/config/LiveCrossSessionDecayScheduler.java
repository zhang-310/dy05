package cn.gaifan.douyinOperations.module.live.config;

import cn.gaifan.douyinOperations.module.live.service.CrossSessionLearningService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 跨场次学习记忆衰减调度器
 * 每天凌晨 4:00 执行，清理低置信度的失效学习记忆
 * 对应 CrossSessionLearningService.decayIneffectiveMemories（原本无调度入口）
 */
@Component
public class LiveCrossSessionDecayScheduler {

    private static final Logger log = LoggerFactory.getLogger(LiveCrossSessionDecayScheduler.class);

    @Autowired(required = false)
    private CrossSessionLearningService crossSessionLearningService;

    /**
     * 每日 04:00 衰减低效记忆
     * 可通过 app.live.cross-session-decay.cron 配置
     */
    @Scheduled(cron = "${app.live.cross-session-decay.cron:0 0 4 * * ?}")
    public void decayIneffectiveMemories() {
        if (crossSessionLearningService == null) {
            log.debug("[CrossSessionDecay] CrossSessionLearningService 未注入，跳过");
            return;
        }
        try {
            crossSessionLearningService.decayIneffectiveMemories();
            log.info("[CrossSessionDecay] 跨场次低效记忆衰减完成");
        } catch (Exception e) {
            log.error("[CrossSessionDecay] 跨场次低效记忆衰减失败: {}", e.getMessage(), e);
        }
    }
}
