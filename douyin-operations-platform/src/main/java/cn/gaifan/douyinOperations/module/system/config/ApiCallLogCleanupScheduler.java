package cn.gaifan.douyinOperations.module.system.config;

import cn.gaifan.douyinOperations.module.system.repository.SysApiCallLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
    private static final int BATCH_SLEEP_MS = 100; // P1-1: 批次间休眠避免持续占用资源

    @Resource
    private SysApiCallLogRepository apiCallLogRepository;

    @Value("${app.system.api-log-cleanup.enabled:true}")
    private boolean schedulerEnabled;

    @Scheduled(cron = "${app.system.api-log-cleanup.cron:0 0 2 * * ?}")
    public void cleanup() {
        if (!schedulerEnabled) {
            log.debug("API 调用日志清理定时任务已禁用，跳过");
            return;
        }
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RETENTION_DAYS);
        Timestamp cutoffTs = Timestamp.valueOf(cutoff);
        int totalDeleted = 0;
        int deleted;
        do {
            deleted = deleteBatchWithNewTransaction(cutoffTs);
            totalDeleted += deleted;

            // P1-1: 批次间休眠，避免持续占用数据库资源
            if (deleted > 0) {
                try {
                    Thread.sleep(BATCH_SLEEP_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("日志清理任务被中断");
                    break;
                }
            }
        } while (deleted > 0);

        if (totalDeleted > 0) {
            log.info("API 调用日志清理完成，删除 {} 条（保留 {} 天）", totalDeleted, RETENTION_DAYS);
        }
    }

    /**
     * P1-1: 每批使用独立事务（REQUIRES_NEW），避免长事务锁表
     * 设置 10 秒超时保护
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 10, rollbackFor = Exception.class)
    public int deleteBatchWithNewTransaction(Timestamp cutoff) {
        return apiCallLogRepository.deleteBatchByCreateTimeBefore(cutoff);
    }
}
