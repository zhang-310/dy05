package cn.gaifan.douyinOperations.module.storage.config;

import cn.gaifan.douyinOperations.module.storage.service.BosCleanupService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * BOS 僵尸文件清理定时任务：每天凌晨 3 点执行
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.storage.bos-cleanup.enabled", havingValue = "true", matchIfMissing = false)  // P3-5: 默认禁用
public class BosCleanupScheduler {

    @Resource
    private BosCleanupService bosCleanupService;

    @Scheduled(cron = "${app.storage.bos-cleanup.cron:0 0 3 * * ?}")
    public void cleanupAbandonedFiles() {
        try {
            int deleted = bosCleanupService.cleanupAbandonedProjectFiles(30);
            if (deleted > 0) {
                log.info("BOS 僵尸文件清理: 删除 {} 个文件", deleted);
            }
        } catch (Exception e) {
            log.error("BOS 僵尸文件清理失败", e);
        }
    }
}
