package cn.gaifan.douyinOperations.module.shortvideo.config;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 图生视频队列监听：消费失败不重入队，由队列 x-dead-letter 进入 DLQ。
 */
@Configuration
public class VideoGenerationRabbitListenerConfig {

    public static final String VIDEO_GENERATION_LISTENER_FACTORY = "videoGenerationListenerContainerFactory";

    @Bean(name = VIDEO_GENERATION_LISTENER_FACTORY)
    public SimpleRabbitListenerContainerFactory videoGenerationListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
