package cn.gaifan.douyinOperations.module.shortvideo.config;

import cn.gaifan.douyinOperations.module.shortvideo.service.VideoGenerationTaskService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 图生视频异步任务 RabbitMQ 消费者 (Phase 2.2)
 */
@Component
public class VideoGenerationTaskAmqpConsumer {

    private static final Logger log = LoggerFactory.getLogger(VideoGenerationTaskAmqpConsumer.class);

    @Value("${app.shortvideo.video-task.amqp.enabled:true}")
    private boolean amqpEnabled;

    @Resource
    private VideoGenerationTaskService taskService;

    @RabbitListener(queues = VideoGenerationTaskAmqpConfig.QUEUE_VIDEO_GENERATION)
    public void onMessage(String taskIdStr) {
        if (!amqpEnabled) return;
        try {
            Long taskId = Long.parseLong(taskIdStr.trim());
            taskService.processTask(taskId);
        } catch (Exception e) {
            log.warn("图生视频任务消费异常: {}", e.getMessage());
        }
    }
}
