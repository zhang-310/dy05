package cn.gaifan.douyinOperations.module.shortvideo.config;

import cn.gaifan.douyinOperations.module.shortvideo.service.impl.ViralAutoOrchestrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * LF-06：爆款二创全自动编排调度器。
 * <p>
 * 每日定时触发：采集→深度分析→人设融合→脚本生成→排入日历。
 * 默认关闭，由 {@code app.shortvideo.auto-orchestration.enabled=true} 开启。
 */
@Component
@ConditionalOnProperty(prefix = "app.shortvideo.auto-orchestration", name = "enabled", havingValue = "true")
public class ViralAutoOrchestrationScheduler {

    private static final Logger log = LoggerFactory.getLogger(ViralAutoOrchestrationScheduler.class);

    private final ViralAutoOrchestrationService orchestrationService;

    public ViralAutoOrchestrationScheduler(ViralAutoOrchestrationService orchestrationService) {
        this.orchestrationService = orchestrationService;
        log.info("[LF-06] 自动编排调度器已启用");
    }

    /**
     * 默认每日 6:00 执行（在垂类采集 5:00 之后），可通过配置覆盖。
     */
    @Scheduled(cron = "${app.shortvideo.auto-orchestration.cron:0 0 6 * * ?}",
            zone = "${app.shortvideo.auto-orchestration.zone:Asia/Shanghai}")
    public void run() {
        log.info("[LF-06] 定时编排开始");
        try {
            Map<String, Object> result = orchestrationService.runPipeline();
            log.info("[LF-06] 定时编排完成: {}", result);
        } catch (Exception e) {
            log.error("[LF-06] 定时编排异常: {}", e.getMessage(), e);
        }
    }
}
