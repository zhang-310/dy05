package cn.gaifan.douyinOperations.module.storage.config;

import cn.gaifan.douyinOperations.module.storage.service.UploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import jakarta.annotation.Resource;

/**
 * 上传任务清理调度器
 * 定时清理 7 天未完成的上传任务
 */
@Slf4j
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "storage.upload.cleanup.enabled", havingValue = "true", matchIfMissing = false)  // P3-5: 默认禁用
public class UploadTaskCleanupScheduler {

    @Resource
    private UploadService uploadService;

    /**
     * 每天凌晨 2 点执行一次清理
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupExpiredTasks() {
        try {
            log.info("========== 开始清理过期上传任务 ==========");
            long deletedCount = uploadService.cleanupExpiredTasks();
            log.info("========== 清理完成，删除 {} 个过期任务 ==========", deletedCount);
        } catch (Exception e) {
            log.error("清理过期上传任务失败", e);
        }
    }
}
