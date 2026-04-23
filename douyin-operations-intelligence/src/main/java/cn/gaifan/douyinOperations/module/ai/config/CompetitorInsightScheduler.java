package cn.gaifan.douyinOperations.module.ai.config;

import cn.gaifan.douyinOperations.module.ai.service.CompetitorInsightService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CompetitorInsightScheduler {

    @Autowired
    private CompetitorInsightService competitorInsightService;

    @Scheduled(cron = "0 0 3 * * ?") // 每日凌晨3点
    public void dailyCollect() {
        log.info("[CompetitorInsightScheduler] 开始每日竞品采集...");
        try {
            competitorInsightService.collectInsights();
            competitorInsightService.ingestHighQualityToKb();
        } catch (Exception e) {
            log.error("[CompetitorInsightScheduler] 每日竞品采集失败", e);
        }
    }
}
