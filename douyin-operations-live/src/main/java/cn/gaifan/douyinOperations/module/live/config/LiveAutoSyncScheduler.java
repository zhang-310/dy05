package cn.gaifan.douyinOperations.module.live.config;

import cn.gaifan.douyinOperations.module.live.entity.LiveSession;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
import cn.gaifan.douyinOperations.module.live.service.LiveSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Q3-4: 直播结束后自动数据同步调度器。
 * 每 5 分钟扫描一次，将已超过计划结束时间但状态仍为「直播中」的场次自动结束并触发数据同步。
 *
 * <p><b>架构约束</b>：状态更新必须通过 {@link LiveSessionService#updateStatus(Long, Integer)} 完成，
 * 以确保 {@link cn.gaifan.douyinOperations.module.live.event.LiveSessionEndedEvent} 正确发布，
 * 触发抖音数据同步和跨场次学习等下游逻辑。禁止直接调用 {@code session.setStatus()} + repository.save()。
 */
@Component
public class LiveAutoSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(LiveAutoSyncScheduler.class);

    /** 状态：直播中 */
    private static final int STATUS_LIVE = 1;
    /** 状态：已结束 */
    private static final int STATUS_ENDED = 2;

    @Resource
    private LiveSessionRepository sessionRepository;

    @Resource
    private LiveSessionService liveSessionService;

    @Value("${app.live.auto-sync.scheduler.enabled:true}")
    private boolean schedulerEnabled;

    /**
     * 定时任务：每 5 分钟检查一次已超时的直播场次。
     */
    @Scheduled(fixedDelayString = "${app.live.auto-sync.scheduler.fixed-delay-ms:300000}")
    @Transactional
    public void autoSyncEndedSessions() {
        if (!schedulerEnabled) {
            log.debug("LiveAutoSync: 调度已禁用，跳过");
            return;
        }
        Timestamp now = new Timestamp(System.currentTimeMillis());

        // 同时检查 scheduledEndTime 和 plannedEndTime
        List<LiveSession> byScheduled = sessionRepository
                .findByStatusAndScheduledEndTimeBeforeAndAutoSyncEnabledAndDeleted(
                        STATUS_LIVE, now, 1, 0);
        List<LiveSession> byPlanned = sessionRepository
                .findByStatusAndPlannedEndTimeBeforeAndAutoSyncEnabledAndDeleted(
                        STATUS_LIVE, now, 1, 0);

        // 合并去重
        Set<Long> seenIds = new HashSet<>();
        List<LiveSession> expiredSessions = new ArrayList<>();
        for (LiveSession s : byScheduled) {
            if (seenIds.add(s.getId())) expiredSessions.add(s);
        }
        for (LiveSession s : byPlanned) {
            if (seenIds.add(s.getId())) expiredSessions.add(s);
        }

        if (expiredSessions.isEmpty()) {
            log.debug("LiveAutoSync: 无超时场次需要处理");
            return;
        }

        log.info("LiveAutoSync: 发现 {} 个超时直播场次，开始自动同步", expiredSessions.size());

        for (LiveSession session : expiredSessions) {
            try {
                log.info("Auto-syncing session {}: {}", session.getId(), session.getLiveTitle());

                // 设置 endTime（若未设置）
                if (session.getEndTime() == null) {
                    session.setEndTime(now);
                    sessionRepository.save(session);
                }

                // 通过 LiveSessionService.updateStatus() 更新状态，确保发布 LiveSessionEndedEvent
                // 禁止直接 session.setStatus() + repository.save()，否则下游同步和跨场次学习会漏触发
                liveSessionService.updateStatus(session.getId(), STATUS_ENDED);

                log.info("Auto-sync completed for session {}: {}", session.getId(), session.getLiveTitle());
            } catch (Exception e) {
                log.error("Auto-sync failed for session {}: {}", session.getId(), e.getMessage(), e);
            }
        }

        log.info("LiveAutoSync: 本轮处理完成，共处理 {} 个场次", expiredSessions.size());
    }
}
