package cn.gaifan.douyinOperations.module.ai.config;

import cn.gaifan.douyinOperations.module.ai.service.CompetitorInsightService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CompetitorInsightScheduler {

    @Autowired
    private CompetitorInsightService competitorInsightService;

    @Value("${app.ai.competitor-insight.enabled:true}")
    private boolean enabled;

    @Scheduled(cron = "${app.ai.competitor-insight.cron:0 0 3 * * ?}") // 默认每日凌晨3点
    public void dailyCollect() {
        if (!enabled) {
            log.debug("[CompetitorInsightScheduler] 已禁用，跳过每日竞品采集");
            return;
        }
        log.info("[CompetitorInsightScheduler] 开始每日竞品采集...");
        try {
            competitorInsightService.collectInsights();
            competitorInsightService.ingestHighQualityToKb();
        } catch (Exception e) {
            log.error("[CompetitorInsightScheduler] 每日竞品采集失败", e);
        }
    }
}
