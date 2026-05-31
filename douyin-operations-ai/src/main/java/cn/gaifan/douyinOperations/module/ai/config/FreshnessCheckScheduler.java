package cn.gaifan.douyinOperations.module.ai.config;

import cn.gaifan.douyinOperations.module.ai.service.FreshnessCheckService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 时效性检测 Agent 定时任务：每周检测知识文档是否过期
 */
@Component
public class FreshnessCheckScheduler {

    private static final Logger log = LoggerFactory.getLogger(FreshnessCheckScheduler.class);

    @Value("${app.ai.freshness-check.enabled:true}")
    private boolean freshnessCheckEnabled;

    @Value("${app.ai.freshness-check.max-docs:20}")
    private int maxDocs;

    @Resource
    private FreshnessCheckService freshnessCheckService;

    @Scheduled(cron = "${app.ai.freshness-check.cron:0 0 4 * * SUN}")
    public void runFreshnessCheck() {
        if (!freshnessCheckEnabled) {
            log.debug("时效性检测已禁用，跳过");
            return;
        }
        try {
            int checked = freshnessCheckService.runFreshnessCheck(null, maxDocs);
            if (checked > 0) log.info("时效性检测定时任务完成，检查 {} 篇文档", checked);
        } catch (Exception e) {
            log.error("时效性检测定时任务失败", e);
        }
    }
}
