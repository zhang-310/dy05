package cn.gaifan.douyinOperations.module.live.config;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 直播异步生成队列监听：失败不重入队，交由 x-dead-letter 进 DLQ。
 */
@Configuration
public class LiveGenerationRabbitListenerConfig {

    public static final String LIVE_GENERATION_LISTENER_FACTORY = "liveGenerationListenerContainerFactory";

    @Bean(name = LIVE_GENERATION_LISTENER_FACTORY)
    public SimpleRabbitListenerContainerFactory liveGenerationListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
