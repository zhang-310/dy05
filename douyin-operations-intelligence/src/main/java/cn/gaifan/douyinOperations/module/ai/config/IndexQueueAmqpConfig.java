package cn.gaifan.douyinOperations.module.ai.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 索引队列 RabbitMQ 配置：异步触发消费，降低入库延迟
 */
@Configuration
@EnableRabbit
public class IndexQueueAmqpConfig {

    public static final String QUEUE_INDEX = "ai.index.queue";

    @Bean
    public Queue indexQueue() {
        return new Queue(QUEUE_INDEX, true);
    }
}
