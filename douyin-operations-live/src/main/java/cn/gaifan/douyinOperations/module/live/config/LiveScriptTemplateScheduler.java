package cn.gaifan.douyinOperations.module.live.config;

import cn.gaifan.douyinOperations.module.live.service.LiveScriptTemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * 高效话术入库定时任务
 * 每日 04:00 扫描 effectiveness_score >= 80 的话术，自动入库 live_script_template
 */
@Component
public class LiveScriptTemplateScheduler {

    private static final Logger log = LoggerFactory.getLogger(LiveScriptTemplateScheduler.class);

    @Resource
    private LiveScriptTemplateService liveScriptTemplateService;

    @Scheduled(cron = "${app.live.script-template-import.cron:0 0 4 * * ?}")
    public void importHighEffectivenessScripts() {
        try {
            int imported = liveScriptTemplateService.importFromHighEffectivenessScripts();
            if (imported > 0) {
                log.info("高效话术入库完成，新增 {} 条模板", imported);
            }
        } catch (Exception e) {
            log.error("高效话术入库失败", e);
        }
    }
}
