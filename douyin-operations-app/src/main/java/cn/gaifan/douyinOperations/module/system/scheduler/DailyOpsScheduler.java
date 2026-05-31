package cn.gaifan.douyinOperations.module.system.scheduler;

import cn.gaifan.douyinOperations.module.live.service.GmvTrackingService;
import cn.gaifan.douyinOperations.module.platform.service.GovernanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * 运营自动化调度器 — 接入真实数据
 */
@Component
public class DailyOpsScheduler {

    private static final Logger log = LoggerFactory.getLogger(DailyOpsScheduler.class);

    @Resource private GovernanceService governanceService;
    @Resource private GmvTrackingService gmvTrackingService;
    @Resource private cn.gaifan.douyinOperations.module.integration.wecom.WecomNotifier wecomNotifier;

    /** 每日 07:30 — 晨报 + 企微推送 */
    @Scheduled(cron = "0 30 7 * * ?")
    public void morningBriefing() {
        var top = gmvTrackingService.getTopSessionsByGmv(1);
        long yesterdayGmv = top.isEmpty() ? 0 : top.get(0).netGmv();
        double gmvWan = yesterdayGmv / 10000.0;
        log.info("[晨报] 昨日 GMV: {}万, 场次: {}", gmvWan, top.size());
        governanceService.recordEvent("scheduler", "briefing", "gmv=" + yesterdayGmv, true);

        String content = String.format(
                "📊 每日运营晨报\n昨日 GMV: %.1f万\n场次: %d\n今日待办: 审核话术、巡检场次",
                gmvWan, top.size());
        wecomNotifier.sendText(content);
    }

    /** 每 30 分钟 — GMV 快照 */
    @Scheduled(fixedRate = 1800000)
    public void gmvSnapshot() {
        var top = gmvTrackingService.getTopSessionsByGmv(3);
        log.info("[GMV快照] Top1={}万", top.isEmpty() ? 0 : top.get(0).netGmvInWan());
    }
}
