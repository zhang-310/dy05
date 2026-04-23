package cn.gaifan.douyinOperations.module.shortvideo.config;

import cn.gaifan.douyinOperations.module.shortvideo.service.SvHotTopicSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * 抖音热搜 → sv_hot_topic 定时同步（鬼鬼鸭/TianAPI）
 */
@Component
@ConditionalOnProperty(prefix = "app.hot-topic", name = "sync-enabled", havingValue = "true")
public class SvHotTopicSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(SvHotTopicSyncScheduler.class);

    @Resource
    private SvHotTopicSyncService syncService;

    @Scheduled(cron = "${app.hot-topic.sync-cron:0 */30 * * * ?}")  // 每 30 分钟
    public void sync() {
        try {
            int n = syncService.syncFromDouyinHot();
            if (n > 0) log.debug("[HotTopicSync] 同步 {} 条热点", n);
        } catch (Exception e) {
            log.warn("[HotTopicSync] 同步失败: {}", e.getMessage());
        }
    }
}
