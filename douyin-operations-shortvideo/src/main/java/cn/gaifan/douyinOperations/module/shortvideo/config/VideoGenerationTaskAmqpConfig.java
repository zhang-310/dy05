package cn.gaifan.douyinOperations.module.shortvideo.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 图生视频异步任务 RabbitMQ 配置 (Phase 2.2)
 */
@Configuration
public class VideoGenerationTaskAmqpConfig {

    public static final String QUEUE_VIDEO_GENERATION = "shortvideo.video.generation";

    @Bean
    public Queue videoGenerationQueue() {
        return new Queue(QUEUE_VIDEO_GENERATION, true);
    }
}
