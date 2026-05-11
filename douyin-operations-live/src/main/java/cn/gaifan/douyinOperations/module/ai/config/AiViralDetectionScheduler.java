package cn.gaifan.douyinOperations.module.ai.config;

import cn.gaifan.douyinOperations.module.ai.entity.AiViralAnalysis;
import cn.gaifan.douyinOperations.module.ai.repository.AiViralAnalysisRepository;
import cn.gaifan.douyinOperations.module.ai.service.EvolutionService;
import cn.gaifan.douyinOperations.module.douyin.entity.DouyinAccount;
import cn.gaifan.douyinOperations.module.douyin.entity.DouyinVideo;
import cn.gaifan.douyinOperations.module.douyin.repository.DouyinAccountRepository;
import cn.gaifan.douyinOperations.module.douyin.repository.DouyinVideoRepository;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 爆款自动检测定时任务：每日 03:00 扫描播放量 > 30 天均值 × 3 的视频，自动触发拆解
 */
@Component
public class AiViralDetectionScheduler {

    private static final Logger log = LoggerFactory.getLogger(AiViralDetectionScheduler.class);

    @Value("${app.ai.viral-detection.enabled:true}")
    private boolean viralDetectionEnabled;

    @Value("${app.ai.viral-detection.threshold-multiplier:3.0}")
    private double thresholdMultiplier;

    @Value("${app.ai.viral-detection.max-daily:10}")
    private int maxDaily;

    @Resource
    private DouyinVideoRepository videoRepository;

    @Resource
    private DouyinAccountRepository accountRepository;

    @Resource
    private AiViralAnalysisRepository viralAnalysisRepository;

    @Resource
    private EvolutionService evolutionService;

    @Scheduled(cron = "${app.ai.viral-detection.cron:0 0 3 * * ?}")
    public void runViralDetection() {
        if (!viralDetectionEnabled) {
            log.debug("爆款自动检测已禁用，跳过");
            return;
        }

        Timestamp thirtyDaysAgo = Timestamp.from(LocalDateTime.now().minusDays(30).atZone(ZoneId.systemDefault()).toInstant());
        Set<Long> analyzedVideoIds = viralAnalysisRepository.findAll().stream()
                .map(AiViralAnalysis::getVideoId)
                .collect(Collectors.toSet());

        int triggered = 0;
        List<Long> accountIds = accountRepository.findAll().stream()
                .map(DouyinAccount::getId)
                .toList();

        for (Long accountId : accountIds) {
            if (triggered >= maxDaily) break;

            DouyinAccount account = accountRepository.findByIdAndDeleted(accountId, 0).orElse(null);
            if (account == null) continue;

            List<DouyinVideo> recentVideos = videoRepository.findByAccountIdAndDeleted(accountId, 0,
                    PageRequest.of(0, 500, Sort.by(Sort.Direction.DESC, "createTime")))
                    .getContent().stream()
                    .filter(v -> v.getCreateTime() != null && v.getCreateTime().after(thirtyDaysAgo))
                    .toList();

            if (recentVideos.size() < 3) continue;

            double avgViews = recentVideos.stream()
                    .mapToLong(v -> v.getViewCount() != null ? v.getViewCount() : 0L)
                    .average()
                    .orElse(0);
            if (avgViews <= 0) continue;

            double threshold = avgViews * thresholdMultiplier;
            List<DouyinVideo> viralCandidates = recentVideos.stream()
                    .filter(v -> !analyzedVideoIds.contains(v.getId()))
                    .filter(v -> (v.getViewCount() != null ? v.getViewCount() : 0L) > threshold)
                    .sorted((a, b) -> Long.compare(b.getViewCount() != null ? b.getViewCount() : 0,
                            a.getViewCount() != null ? a.getViewCount() : 0))
                    .toList();

            for (DouyinVideo video : viralCandidates) {
                if (triggered >= maxDaily) break;
                try {
                    evolutionService.triggerViralAnalysis(video.getId(), account.getOwnerId(), accountId);
                    analyzedVideoIds.add(video.getId());
                    triggered++;
                    log.info("爆款自动检测: 触发拆解 videoId={} viewCount={} avg={}", video.getId(), video.getViewCount(), (long) avgViews);
                } catch (Exception e) {
                    log.warn("爆款拆解触发失败 videoId={}: {}", video.getId(), e.getMessage());
                }
            }
        }
        if (triggered > 0) log.info("爆款自动检测完成，触发 {} 个拆解任务", triggered);
    }
}
