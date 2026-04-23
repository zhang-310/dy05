package cn.gaifan.douyinOperations.module.shortvideo.config;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvVideoGenerationTask;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvVideoGenerationTaskRepository;
import cn.gaifan.douyinOperations.module.shortvideo.service.VideoGenerationTaskWebhookNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.sql.Timestamp;
import java.util.List;

/**
 * 将长时间停留在 processing 的任务标记为失败，释放资源并便于人工排查。
 */
@Component
@ConditionalOnProperty(name = "app.shortvideo.video-task.timeout-scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class VideoGenerationTaskTimeoutScheduler {

    private static final Logger log = LoggerFactory.getLogger(VideoGenerationTaskTimeoutScheduler.class);

    @Resource
    private SvVideoGenerationTaskRepository taskRepository;
    @Resource
    private VideoGenerationTaskWebhookNotifier taskWebhookNotifier;

    @Value("${app.shortvideo.video-task.processing-timeout-minutes:30}")
    private long processingTimeoutMinutes;

    @Scheduled(fixedDelayString = "${app.shortvideo.video-task.timeout-scan-ms:120000}")
    @Transactional
    public void markStaleProcessingAsFailed() {
        if (processingTimeoutMinutes <= 0) {
            return;
        }
        long cutoff = System.currentTimeMillis() - processingTimeoutMinutes * 60_000L;
        Timestamp before = new Timestamp(cutoff);
        List<SvVideoGenerationTask> stale = taskRepository.findByStatusAndProcessingStartedAtBefore("processing", before);
        if (stale.isEmpty()) {
            return;
        }
        for (SvVideoGenerationTask t : stale) {
            t.setStatus("failed");
            t.setErrorMessage("任务超时（处理超过 " + processingTimeoutMinutes + " 分钟）");
            t.setProcessingStartedAt(null);
            t = taskRepository.save(t);
            taskWebhookNotifier.notifyTaskEnded(t);
            log.warn("图生视频任务超时标记失败 taskId={} ownerId={}", t.getId(), t.getOwnerId());
        }
    }
}
