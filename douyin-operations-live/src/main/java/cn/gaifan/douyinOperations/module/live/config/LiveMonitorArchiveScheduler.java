package cn.gaifan.douyinOperations.module.live.config;

import cn.gaifan.douyinOperations.module.live.repository.LiveMonitorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * LiveMonitor 归档：超过 90 天的监控数据迁移到 live_monitor_archive 后删除
 * 每月 1 日 03:00 执行
 */
@Component
public class LiveMonitorArchiveScheduler {

    private static final Logger log = LoggerFactory.getLogger(LiveMonitorArchiveScheduler.class);
    private static final int RETENTION_DAYS = 90;

    @Resource
    private LiveMonitorRepository liveMonitorRepository;
    @Resource
    private JdbcTemplate jdbcTemplate;

    @Scheduled(cron = "${app.live.monitor-archive.cron:0 0 3 1 * ?}")
    public void archive() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RETENTION_DAYS);
        Timestamp cutoffTs = Timestamp.valueOf(cutoff);
        try {
            // 1. 迁移到归档表
            int archived = jdbcTemplate.update(
                    "INSERT INTO live_monitor_archive (session_id, timestamp, viewers, likes, comments, shares, " +
                            "product_impressions, total_viewers, new_followers, online_count, gmv, orders, create_time) " +
                            "SELECT session_id, timestamp, viewers, likes, comments, shares, product_impressions, " +
                            "total_viewers, new_followers, online_count, gmv, orders, create_time " +
                            "FROM live_monitor WHERE timestamp < ?",
                    cutoffTs);
            // 2. 删除原表数据
            int deleted = liveMonitorRepository.deleteByTimestampBefore(cutoffTs);
            if (archived > 0 || deleted > 0) {
                log.info("LiveMonitor 归档完成，迁移 {} 条到 live_monitor_archive，删除 {} 条（保留 {} 天）",
                        archived, deleted, RETENTION_DAYS);
            }
        } catch (Exception e) {
            log.error("LiveMonitor 归档失败", e);
        }
    }
}
