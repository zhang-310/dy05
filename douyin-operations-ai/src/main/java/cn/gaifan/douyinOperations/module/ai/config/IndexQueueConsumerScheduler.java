package cn.gaifan.douyinOperations.module.ai.config;

import cn.gaifan.douyinOperations.module.ai.service.IndexQueueConsumerService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 索引队列消费者定时任务：定期消费 pending 任务，将进化报告/爆款拆解/直播复盘入库。
 * 支持管理员在「系统配置」中动态修改 ai.index-queue.consumer.interval-ms、batch-size。
 */
@Component
public class IndexQueueConsumerScheduler {

    private static final Logger log = LoggerFactory.getLogger(IndexQueueConsumerScheduler.class);

    @Value("${app.ai.index-queue.consumer.enabled:true}")
    private boolean consumerEnabled;

    @Resource
    private IndexQueueConsumerService indexQueueConsumerService;

    @Resource
    private AiRuntimeConfig aiRuntimeConfig;

    private volatile long lastRunTime = 0;

    /** 每 30 秒检查一次，按 sys_config 中的 interval 决定是否执行 */
    @Scheduled(fixedRate = 30000)
    public void consumeIndexQueue() {
        if (!consumerEnabled) return;
        long now = System.currentTimeMillis();
        long interval = aiRuntimeConfig.getIndexQueueIntervalMs();
        if (lastRunTime > 0 && now - lastRunTime < interval) return;

        lastRunTime = now;
        int batchSize = aiRuntimeConfig.getIndexQueueBatchSize();
        try {
            int done = indexQueueConsumerService.consume(batchSize);
            if (done > 0) {
                log.info("索引队列消费完成: 入库 {} 条", done);
            }
        } catch (Exception e) {
            log.error("索引队列消费异常", e);
        }
    }
}
