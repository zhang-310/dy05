package cn.gaifan.douyinOperations.module.live.config;

import cn.gaifan.douyinOperations.module.live.service.impl.LiveGenerationQueueProcessor;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 直播一键生成 Rabbit 消费者（G-2）
 */
@Component
public class LiveGenerationTaskAmqpConsumer {

    private static final Logger log = LoggerFactory.getLogger(LiveGenerationTaskAmqpConsumer.class);

    @Value("${app.live.generation.async.amqp.enabled:true}")
    private boolean amqpEnabled;

    @Resource
    private LiveGenerationQueueProcessor liveGenerationQueueProcessor;

    @RabbitListener(
            queues = LiveGenerationAmqpConfig.QUEUE_LIVE_SCRIPT_GENERATION,
            containerFactory = LiveGenerationRabbitListenerConfig.LIVE_GENERATION_LISTENER_FACTORY
    )
    public void onMessage(String taskIdStr) {
        if (!amqpEnabled) {
            return;
        }
        try {
            long taskId = Long.parseLong(taskIdStr.trim());
            liveGenerationQueueProcessor.processQueuedFullGeneration(taskId);
        } catch (Exception e) {
            log.warn("live script generation queue message failed: {}", e.getMessage());
            if (e instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException(e);
        }
    }
}
