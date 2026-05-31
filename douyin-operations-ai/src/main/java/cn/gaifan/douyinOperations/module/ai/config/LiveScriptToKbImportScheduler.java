package cn.gaifan.douyinOperations.module.ai.config;

import cn.gaifan.douyinOperations.module.ai.service.LiveScriptToKbImportService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 话术→知识库管道定时任务：每日将高效 live_script 入库 huashu。
 */
@Component
public class LiveScriptToKbImportScheduler {

    private static final Logger log = LoggerFactory.getLogger(LiveScriptToKbImportScheduler.class);

    @Value("${app.ai.live-script-to-kb.enabled:true}")
    private boolean enabled;

    @Value("${app.ai.live-script-to-kb.max-per-user:50}")
    private int maxPerUser;

    @Resource
    private LiveScriptToKbImportService liveScriptToKbImportService;

    @Scheduled(cron = "${app.ai.live-script-to-kb.cron:0 0 5 * * ?}")
    public void runImport() {
        if (!enabled) {
            log.debug("话术→知识库管道已禁用，跳过");
            return;
        }
        try {
            Map<String, Object> result = liveScriptToKbImportService.importForAllUsers(maxPerUser);
            int imported = (Integer) result.getOrDefault("totalImported", 0);
            int skipped = (Integer) result.getOrDefault("totalSkipped", 0);
            if (imported > 0 || skipped > 0) {
                log.info("话术→知识库入库完成: imported={}, skipped={}, users={}",
                        imported, skipped, result.getOrDefault("usersProcessed", 0));
            }
        } catch (Exception e) {
            log.error("话术→知识库入库失败", e);
        }
    }
}
