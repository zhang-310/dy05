package cn.gaifan.douyinOperations.module.abtest.controller;

import cn.gaifan.douyinOperations.module.abtest.service.AbTestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * P2-2: A/B 实验自动收敛定时任务 — 每日 05:00 检测运行中实验是否达到统计显著。
 */
@Slf4j
@Component
public class AbTestAutoConvergeScheduler {

    @Autowired(required = false)
    private AbTestService abTestService;

    /**
     * 每日 05:00 扫描运行中实验，自动收敛显著实验
     */
    @Scheduled(cron = "0 0 5 * * ?")
    public void scheduledAutoConverge() {
        if (abTestService == null) return;
        log.info("[AbTestAutoConverge] 开始执行自动收敛扫描");
        try {
            int count = abTestService.autoConvergeAll();
            if (count > 0) {
                log.info("[AbTestAutoConverge] 本轮自动收敛 {} 个实验", count);
            }
        } catch (Exception e) {
            log.error("[AbTestAutoConverge] 自动收敛任务失败", e);
        }
    }
}
