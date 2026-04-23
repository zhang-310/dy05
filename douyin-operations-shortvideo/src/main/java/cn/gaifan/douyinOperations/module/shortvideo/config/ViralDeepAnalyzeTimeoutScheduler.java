package cn.gaifan.douyinOperations.module.shortvideo.config;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvViralVideo;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvViralVideoRepository;
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
 * 将长时间停留在 {@code deep_analyze_status=processing} 的爆款记录标记为 failed，便于运营重跑深度分析。
 */
@Component
@ConditionalOnProperty(prefix = "app.shortvideo.deep-analyze-timeout", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ViralDeepAnalyzeTimeoutScheduler {

    private static final Logger log = LoggerFactory.getLogger(ViralDeepAnalyzeTimeoutScheduler.class);

    @Resource
    private SvViralVideoRepository viralVideoRepository;

    @Value("${app.shortvideo.deep-analyze-timeout.processing-timeout-minutes:120}")
    private long processingTimeoutMinutes;

    @Scheduled(fixedDelayString = "${app.shortvideo.deep-analyze-timeout.scan-ms:300000}")
    @Transactional
    public void markStaleProcessingAsFailed() {
        if (processingTimeoutMinutes <= 0) {
            return;
        }
        long cutoff = System.currentTimeMillis() - processingTimeoutMinutes * 60_000L;
        Timestamp before = new Timestamp(cutoff);
        List<SvViralVideo> stale = viralVideoRepository.findStaleDeepAnalyzeProcessing(before);
        if (stale.isEmpty()) {
            return;
        }
        for (SvViralVideo v : stale) {
            v.setDeepAnalyzeStatus("failed");
            v.setDeepAnalyzeError("深度分析超时（处理超过 " + processingTimeoutMinutes + " 分钟）");
            v.setDeepAnalyzeFinishedAt(new Timestamp(System.currentTimeMillis()));
            viralVideoRepository.save(v);
            log.warn("[深度分析超时] viralId={} ownerId={}", v.getId(), v.getOwnerId());
        }
    }
}
