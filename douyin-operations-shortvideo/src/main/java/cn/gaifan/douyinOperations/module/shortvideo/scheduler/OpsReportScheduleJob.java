package cn.gaifan.douyinOperations.module.shortvideo.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * N-6：运营报表可选定时占位（默认关闭）。开启后仅打日志，避免无 Webhook/邮件配置时 silent 误判。
 */
@Component
@ConditionalOnProperty(prefix = "app.shortvideo.ops-report", name = "schedule-enabled", havingValue = "true")
public class OpsReportScheduleJob {

    private static final Logger log = LoggerFactory.getLogger(OpsReportScheduleJob.class);

    @Scheduled(cron = "${app.shortvideo.ops-report.schedule-cron:0 0 8 ? * MON}")
    public void tick() {
        log.info("shortvideo ops-report schedule tick (export/Webhook 请由集成方在回调中实现)");
    }
}
