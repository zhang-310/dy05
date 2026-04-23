package cn.gaifan.douyinOperations.module.system.config;

import cn.gaifan.douyinOperations.module.system.repository.SysApiCallLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * API 调用日志 30 天自动清理，每天凌晨 2:00 执行
 * 分批删除，每批 1000 条，避免长事务
 */
@Component
public class ApiCallLogCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(ApiCallLogCleanupScheduler.class);
    private static final int RETENTION_DAYS = 30;
    private static final int BATCH_SIZE = 1000;

    @Resource
    private SysApiCallLogRepository apiCallLogRepository;

    @Scheduled(cron = "${app.system.api-log-cleanup.cron:0 0 2 * * ?}")
    public void cleanup() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RETENTION_DAYS);
        Timestamp cutoffTs = Timestamp.valueOf(cutoff);
        int totalDeleted = 0;
        int deleted;
        do {
            deleted = apiCallLogRepository.deleteBatchByCreateTimeBefore(cutoffTs);
            totalDeleted += deleted;
        } while (deleted > 0);

        if (totalDeleted > 0) {
            log.info("API 调用日志清理完成，删除 {} 条（保留 {} 天）", totalDeleted, RETENTION_DAYS);
        }
    }
}
