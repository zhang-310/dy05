package cn.gaifan.douyinOperations.module.ai.service;

import cn.gaifan.douyinOperations.module.ai.config.IndexQueueAmqpConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 索引队列 RabbitMQ 发布者：入队时发送消息，触发异步消费
 */
@Service
public class IndexQueueAmqpPublisher {

    private static final Logger log = LoggerFactory.getLogger(IndexQueueAmqpPublisher.class);

    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送索引任务通知（消费者将拉取 pending 任务处理）
     */
    public void notifyIndexTask() {
        if (rabbitTemplate == null) return;
        try {
            rabbitTemplate.convertAndSend(IndexQueueAmqpConfig.QUEUE_INDEX, "1");
        } catch (Exception e) {
            log.warn("索引队列 RabbitMQ 发布失败: {}", e.getMessage());
        }
    }
}
