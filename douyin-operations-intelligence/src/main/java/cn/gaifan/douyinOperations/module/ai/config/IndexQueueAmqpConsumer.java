package cn.gaifan.douyinOperations.module.ai.config;

import cn.gaifan.douyinOperations.module.ai.repository.AiIndexQueueRepository;
import cn.gaifan.douyinOperations.module.ai.service.IndexQueueConsumerService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 索引队列 RabbitMQ 消费者：收到消息后立即消费 1 条待处理任务
 */
@Component
public class IndexQueueAmqpConsumer {

    private static final Logger log = LoggerFactory.getLogger(IndexQueueAmqpConsumer.class);

    @Value("${app.ai.index-queue.amqp.enabled:true}")
    private boolean amqpEnabled;

    @Resource
    private IndexQueueConsumerService indexQueueConsumerService;

    @Resource
    private AiIndexQueueRepository indexQueueRepository;

    @RabbitListener(queues = IndexQueueAmqpConfig.QUEUE_INDEX)
    public void onMessage(String msg) {
        if (!amqpEnabled) return;
        try {
            int done = indexQueueConsumerService.consume(1);
            if (done > 0) {
                indexQueueRepository.cleanDone();
                log.debug("索引队列 AMQP 消费: 入库 {} 条", done);
            }
        } catch (Exception e) {
            log.warn("索引队列 AMQP 消费异常: {}", e.getMessage());
        }
    }
}
