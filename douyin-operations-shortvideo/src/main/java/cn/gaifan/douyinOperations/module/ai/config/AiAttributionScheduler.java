package cn.gaifan.douyinOperations.module.ai.config;

import cn.gaifan.douyinOperations.module.ai.entity.AiCallLog;
import cn.gaifan.douyinOperations.module.ai.repository.AiCallLogRepository;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeBoostService;
import cn.gaifan.douyinOperations.module.live.entity.LiveSession;
import cn.gaifan.douyinOperations.module.live.entity.LiveSessionData;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionDataRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvVideo;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvVideoRepository;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.List;

/**
 * 效果归因定时任务：每日 02:00 扫描已关联视频的调用记录，计算 content_effect、effect_score
 */
@Component
public class AiAttributionScheduler {

    private static final Logger log = LoggerFactory.getLogger(AiAttributionScheduler.class);

    @Value("${app.ai.attribution.enabled:true}")
    private boolean attributionEnabled;

    @Value("${app.ai.attribution.high-threshold:1.5}")
    private double highThreshold;

    @Value("${app.ai.attribution.low-threshold:0.3}")
    private double lowThreshold;

    @Resource
    private AiCallLogRepository aiCallLogRepository;

    @Resource
    private SvVideoRepository svVideoRepository;

    @Resource
    private LiveSessionRepository liveSessionRepository;

    @Resource
    private LiveSessionDataRepository liveSessionDataRepository;

    @Resource
    private KnowledgeBoostService knowledgeBoostService;

    @Scheduled(cron = "${app.ai.attribution.cron:0 0 2 * * ?}")
    public void runAttribution() {
        if (!attributionEnabled) {
            log.debug("效果归因已禁用，跳过");
            return;
        }
        List<AiCallLog> pending = aiCallLogRepository.findPendingAttribution();
        int updated = 0;
        for (AiCallLog logEntry : pending) {
            try {
                if (logEntry.getLinkedVideoId() != null) {
                    if (attributeVideo(logEntry)) updated++;
                } else if (logEntry.getLinkedSessionId() != null) {
                    if (attributeSession(logEntry)) updated++;
                }
            } catch (Exception e) {
                log.warn("归因失败 callLogId={}: {}", logEntry.getId(), e.getMessage());
            }
        }
        if (updated > 0) log.info("效果归因完成，更新 {} 条", updated);
    }

    private boolean attributeVideo(AiCallLog logEntry) {
        Long videoId = logEntry.getLinkedVideoId();
        SvVideo video = svVideoRepository.findById(videoId).orElse(null);
        if (video == null) return false;

        long playCount = video.getViewCount() != null ? video.getViewCount() : 0L;
        List<Long> accountViews = svVideoRepository.findViewCountsByAccountId(video.getAccountId());
        if (accountViews == null || accountViews.isEmpty()) return false;

        double avg = accountViews.stream().mapToLong(Long::longValue).average().orElse(0);
        if (avg <= 0) return false;

        double score = (double) playCount / avg;
        BigDecimal effectScore = BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP);
        String contentEffect;
        if (score >= highThreshold) contentEffect = "high_perform";
        else if (score <= lowThreshold) contentEffect = "low_perform";
        else contentEffect = "normal";

        logEntry.setContentEffect(contentEffect);
        logEntry.setEffectScore(effectScore);
        aiCallLogRepository.save(logEntry);

        // BR-31 知识权重反向更新
        knowledgeBoostService.updateBoostForAttribution(logEntry);
        return true;
    }

    /** P0 直播归因：按 totalViewers 或 totalRevenue 与账号均值对比 */
    private boolean attributeSession(AiCallLog logEntry) {
        Long sessionId = logEntry.getLinkedSessionId();
        LiveSession session = liveSessionRepository.findById(sessionId).orElse(null);
        if (session == null || session.getAccountId() == null) return false;

        LiveSessionData data = liveSessionDataRepository.findBySessionId(sessionId).orElse(null);
        if (data == null) return false;

        // 使用 totalViewers 或 totalRevenue 作为效果指标（优先 viewers）
        boolean useRevenue = (data.getTotalViewers() == null || data.getTotalViewers() == 0)
                && data.getTotalRevenue() != null && data.getTotalRevenue().longValue() > 0;
        long metric = useRevenue ? data.getTotalRevenue().longValue()
                : (data.getTotalViewers() != null ? data.getTotalViewers() : 0);
        if (metric <= 0) return false;

        long ninetyDaysAgo = System.currentTimeMillis() - 90L * 24 * 60 * 60 * 1000;
        Timestamp start = new Timestamp(ninetyDaysAgo);
        Timestamp end = new Timestamp(System.currentTimeMillis());
        List<LiveSession> accountSessions = liveSessionRepository.findByAccountIdAndStartTimeBetweenAndDeleted(
                session.getAccountId(), start, end, 0);
        if (accountSessions == null || accountSessions.size() < 2) return false;

        List<Long> sessionIds = accountSessions.stream().map(LiveSession::getId).toList();
        List<LiveSessionData> accountDataList = liveSessionDataRepository.findBySessionIdIn(sessionIds);
        if (accountDataList == null || accountDataList.isEmpty()) return false;

        double avg = accountDataList.stream()
                .mapToLong(d -> useRevenue && d.getTotalRevenue() != null ? d.getTotalRevenue().longValue()
                        : (d.getTotalViewers() != null ? d.getTotalViewers() : 0))
                .filter(v -> v > 0)
                .average().orElse(0);
        if (avg <= 0) return false;

        double score = (double) metric / avg;
        BigDecimal effectScore = BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP);
        String contentEffect = score >= highThreshold ? "high_perform" : (score <= lowThreshold ? "low_perform" : "normal");

        logEntry.setContentEffect(contentEffect);
        logEntry.setEffectScore(effectScore);
        aiCallLogRepository.save(logEntry);

        knowledgeBoostService.updateBoostForAttribution(logEntry);
        return true;
    }
}
