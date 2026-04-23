package cn.gaifan.douyinOperations.module.live.config;

import cn.gaifan.douyinOperations.module.live.repository.LiveLearningMemoryRepository;
import cn.gaifan.douyinOperations.module.live.service.CrossSessionLearningService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * P1-3: 跨场次学习记忆运维调度器。
 * - 每日 02:00: 对所有记忆执行置信度衰减（*0.9）
 * - 每日 02:30: 归档置信度低于 0.3 的记忆
 */
@Slf4j
@Component
public class LiveLearningMemoryScheduler {

    private static final BigDecimal DECAY_THRESHOLD = new BigDecimal("0.30");

    @Autowired(required = false)
    private CrossSessionLearningService crossSessionLearningService;

    @Autowired(required = false)
    private LiveLearningMemoryRepository memoryRepository;

    /**
     * 每日 02:00: 对所有未归档记忆执行置信度衰减 (*0.9)
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void decayAllMemories() {
        if (memoryRepository == null) return;
        try {
            // 找到所有未归档、置信度高于阈值的记忆，逐条衰减
            var all = memoryRepository.findByConfidenceLessThanAndDeleted(BigDecimal.ONE, 0);
            int decayed = 0;
            for (var m : all) {
                if (m.getConfidence() != null && m.getConfidence().compareTo(DECAY_THRESHOLD) > 0) {
                    memoryRepository.decayConfidence(m.getId());
                    decayed++;
                }
            }
            log.info("[LearningMemory] 置信度衰减完成，处理 {} 条记忆", decayed);
        } catch (Exception e) {
            log.error("[LearningMemory] 置信度衰减任务失败", e);
        }
    }

    /**
     * 每日 02:30: 归档置信度 < 0.3 的失效记忆
     */
    @Scheduled(cron = "0 30 2 * * ?")
    public void archiveIneffectiveMemories() {
        if (crossSessionLearningService == null) return;
        try {
            crossSessionLearningService.decayIneffectiveMemories();
        } catch (Exception e) {
            log.error("[LearningMemory] 归档失效记忆任务失败", e);
        }
    }
}
